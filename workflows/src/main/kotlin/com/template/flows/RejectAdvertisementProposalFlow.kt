package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfoFlow
import com.template.contracts.AdInventoryContract
import com.template.states.AdInventoryState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*
import java.util.concurrent.atomic.AtomicReference


@StartableByRPC
@InitiatingFlow
class RejectAdvertisementProposalFlow(
    private val publisher: String,
    private val advertiser: String,
    private val linearId: UUID,
    private val rejectReason: String
) : FlowLogic<String>() {

    companion object {
        object GENERATING_KEYS : ProgressTracker.Step("Generating Keys for transactions.")
        object RETRIEVING_STATE : ProgressTracker.Step("Retrieving proposed AdInventoryState from the vault.")
        object UPDATING_STATE : ProgressTracker.Step("Updating the AdInventoryState.")
        object BUILDING_TRANSACTION : ProgressTracker.Step("Building the transaction.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying the transaction.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing the transaction.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
            GENERATING_KEYS,
            RETRIEVING_STATE,
            UPDATING_STATE,
            BUILDING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): String {

        //Generate key for transaction
        progressTracker.currentStep = GENERATING_KEYS

        // Retrieve the publisher and advertiser accounts from the account names
        val publisherAccount = accountService.accountInfo(publisher).single().state.data
        //val publisherKey = subFlow(NewKeyForAccount(publisherAccount.identifier.id)).owningKey
        val publisherKey = subFlow(RequestKeyForAccount(publisherAccount)).owningKey
        logger.info("publisher account ${publisherAccount.name}")

        val advertiserAccount = accountService.accountInfo(advertiser).single().state.data
        //val advertiserAccountAnonymousParty = subFlow(RequestKeyForAccount(advertiserAccount))
        logger.info("advertiser account ${advertiserAccount.name}")

        val advertiserSession = initiateFlow(advertiserAccount.host)
        // Share advertiser account info with the publisher
        logger.info("Share advertiser account info with the publisher")
        subFlow(ShareAccountInfoFlow(accountService.accountInfo(publisher).single(), listOf( advertiserSession)))
        val advertiserAccountAnonymousParty = subFlow(RequestKeyForAccount(advertiserAccount))
        val advertiserAccountKey = advertiserAccountAnonymousParty.owningKey


        progressTracker.currentStep = RETRIEVING_STATE

        val criteria = QueryCriteria.VaultQueryCriteria(
            externalIds = listOf(publisherAccount.identifier.id)
        )

        val adInventoryStateAndRef = serviceHub.vaultService
            .queryBy(AdInventoryState::class.java, criteria)
            .states
            .firstOrNull()
            ?: throw FlowException("Ad proposal with ID $linearId not found or already consumed")

        val inputAdInventoryState = adInventoryStateAndRef.state.data

        // Check if the current party is the publisher
        if (ourIdentity != publisherAccount.host) {
            throw IllegalArgumentException("Flow can only be initiated by the publisher.")
        }

        progressTracker.currentStep = UPDATING_STATE
        // Create a new AdInventoryState with the "agreed" status
        val outputAdInventoryState = inputAdInventoryState.copy(
            adStatus = "rejected",
            rejectReason = rejectReason
        )

        progressTracker.currentStep = BUILDING_TRANSACTION
        // Build the transaction
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)
            .addInputState(adInventoryStateAndRef)
            .addOutputState(outputAdInventoryState, AdInventoryContract.ID)
            .addCommand(
                AdInventoryContract.Commands.Reject(),
                listOf(ourIdentity.owningKey, advertiserAccountKey)
            )

        progressTracker.currentStep = VERIFYING_TRANSACTION
        // Verify the transaction
        transactionBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        // Sign the transaction
        val locallySignedTx = serviceHub.signInitialTransaction(transactionBuilder, listOfNotNull(ourIdentity.owningKey, publisherKey))

        // Initiate a session with the advertiser
        val sessionForAdvertiser = initiateFlow(advertiserAccountAnonymousParty)

        progressTracker.currentStep = GATHERING_SIGS
        // Collect the advertiser's signature
        val advertiserSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAdvertiser, advertiserAccountKey))
        val signedByBothParties = locallySignedTx.withAdditionalSignatures(advertiserSignature)

        progressTracker.currentStep = FINALISING_TRANSACTION
        // Finalize the transaction
        subFlow(FinalityFlow(signedByBothParties, listOf(sessionForAdvertiser).filter { it.counterparty != ourIdentity }))

        return "Advertisement proposal rejected with linearId: $linearId"
    }
}

@InitiatedBy(RejectAdvertisementProposalFlow::class)
class RejectAdvertisementProposalFlowResponderFlow(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val accountMovedTo = AtomicReference<AccountInfo>()
        val transactionSigner = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                val keyStateMovedTo =
                    stx.coreTransaction.outRefsOfType(AdInventoryState::class.java).first().state.data.advertiser
                keyStateMovedTo.let {
                    accountMovedTo.set(accountService.accountInfo(keyStateMovedTo.owningKey)?.state?.data)
                }
                if (accountMovedTo.get() == null) {
                    throw IllegalStateException("Account to move to was not found on this node")
                }

            }
        }
        val transaction = subFlow(transactionSigner)
        if (counterpartySession.counterparty != serviceHub.myInfo.legalIdentities.first()) {
            val recievedTx = subFlow(
                ReceiveFinalityFlow(
                    counterpartySession,
                    expectedTxId = transaction.id,
                    statesToRecord = StatesToRecord.ALL_VISIBLE
                )
            )
            val accountInfo = accountMovedTo.get()
            if (accountInfo != null) {
                subFlow(
                    BroadcastToCarbonCopyReceiversFlow(
                        accountInfo,
                        recievedTx.coreTransaction.outRefsOfType(AdInventoryState::class.java).first()
                    )
                )
            }


        }
    }
}

