package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfoFlow
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccountsFlow
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlow
import com.template.contracts.AdInventoryContract
import com.template.states.AdInventoryState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*
import java.util.concurrent.atomic.AtomicReference


@StartableByRPC
@InitiatingFlow
class AcceptAdvertisementProposalFlow(
    private val publisher: String,
    private val advertiser: String,
    private val linearId: UUID
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


        // Retrieve the proposed AdInventoryState from the vault
        val myAccount = accountService.accountInfo(publisher).single().state.data


        //CHECKS ---------- ----------
        if (myAccount.name != "EA-SPORTS") {
            throw IllegalArgumentException("Could not find EA-SPORTS")
        }
        if (advertiserAccount.name != "NIKE") {
            throw IllegalArgumentException("Could not find NIKE")
        }
        //CHECKS ---------- ----------

        val criteria = QueryCriteria.VaultQueryCriteria(
            externalIds = listOf(myAccount.identifier.id)
        )

        val adInventoryStateAndRef = serviceHub.vaultService.queryBy(
            contractStateType = AdInventoryState::class.java,
            criteria = criteria
        ).states.firstOrNull()?: throw FlowException("Ad proposal with ID $linearId not found or already consumed")

        val inputAdInventoryState = adInventoryStateAndRef.state.data

        // Check if the current party is the publisher
        if (ourIdentity != publisherAccount.host) {
            throw IllegalArgumentException("Flow can only be initiated by the publisher.")
        }

        // Create a new AdInventoryState with the "agreed" status
        val outputAdInventoryState = inputAdInventoryState.copy(adStatus = "agreed") // Only update the adStatus field


        //CHECKS ---------- ----------
        /*val publisherCheck = accountService.accountInfo(inputAdInventoryState.publisher.owningKey)?.state?.data
            ?: throw FlowException("Publisher account not found")
        val publisherCheckName = publisherCheck.name

        if (publisherCheckName == "EA-SPORTS") {
            throw IllegalArgumentException("FOUND Party EA-SPORTS")
        }

        val advertiserCheck = accountService.accountInfo(inputAdInventoryState.advertiser.owningKey)?.state?.data
            ?: throw FlowException("Advertiser account not found")
        val advertiserCheckName = advertiserCheck.name
        */

        val allAccounts = accountService.ourAccounts()
        allAccounts.forEach { accountStateAndRef ->
            val account = accountStateAndRef.state.data
            logger.info("Account: ${account.name} with owningKey: ${account.host}")
        }
        //CHECKS ---------- ----------

        // Build the transaction
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)
            .addInputState(adInventoryStateAndRef)
            .addOutputState(outputAdInventoryState, AdInventoryContract.ID)
            .addCommand(
                AdInventoryContract.Commands.Agree(),
                listOf(ourIdentity.owningKey, advertiserAccountKey)
            )

        // Verify the transaction
        transactionBuilder.verify(serviceHub)

        // Sign the transaction
        val locallySignedTx = serviceHub.signInitialTransaction(transactionBuilder, listOfNotNull(ourIdentity.owningKey, publisherKey))

        // Initiate a session with the advertiser
        val sessionForAdvertiser = initiateFlow(advertiserAccountAnonymousParty)

        // Collect the advertiser's signature
        val advertiserSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAdvertiser, advertiserAccountKey))
        val signedByBothParties = locallySignedTx.withAdditionalSignatures(advertiserSignature)

        // Finalize the transaction
        subFlow(FinalityFlow(signedByBothParties, listOf(sessionForAdvertiser).filter { it.counterparty != ourIdentity }))

        return "Advertisement proposal accepted with linearId: $linearId"
    }
}

@InitiatedBy(AcceptAdvertisementProposalFlow::class)
class AcceptAdvertisementProposalResponderFlow(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
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



/*

@Suspendable
override fun call(): String {
    // Step 1. Get the AdInventoryState from the vault
    */
/*val adInventoryStateAndRef = serviceHub.vaultService.queryBy<AdInventoryState>(
        QueryCriteria.LinearStateQueryCriteria(linearId = listOf(adInventoryLinearId))
    ).states.singleOrNull() ?: throw FlowException("AdInventoryState not found")*//*


    //START
    // Retrieve the proposed AdInventoryState from the vault
    val myAccount = accountService.accountInfo(publisherName).single().state.data
    val criteria = QueryCriteria.VaultQueryCriteria(
        externalIds = listOf(myAccount.identifier.id)
    )
    val adInventoryStateAndRef = serviceHub.vaultService.queryBy(
        contractStateType = AdInventoryState::class.java,
        criteria = criteria
    ).states.firstOrNull()?: throw FlowException("Ad proposal with ID $adInventoryLinearId not found or already consumed")
    //END

    val inputAdInventory = adInventoryStateAndRef.state.data

    // Step 2. Verify if the publisher account is the node's account
    val publisherAccount = accountService.accountInfo(inputAdInventory.publisher.owningKey)?.state?.data
        ?: throw FlowException("Publisher account not found")
    if (publisherAccount.host != ourIdentity) throw FlowException("The node is not the publisher")

    // Step 3. Verify if the advertiser account matches the input account name
    val advertiserAccount = accountService.accountInfo(inputAdInventory.advertiser.owningKey)?.state?.data
        ?: throw FlowException("Advertiser account not found")
    if (advertiserAccount.name != advertiserName) throw FlowException("The account name does not match the advertiser")

    // Step 4. Create a new AdInventoryState with the updated status
    val updatedAdInventory = inputAdInventory.copy(adStatus = "agreed")

    // Step 5. Create a command and a transaction builder
    val command = Command(
        AdInventoryContract.Commands.Agree(),
        listOf(inputAdInventory.publisher.owningKey, inputAdInventory.advertiser.owningKey)
    )
    val transactionBuilder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
        .addInputState(adInventoryStateAndRef)
        .addOutputState(updatedAdInventory, AdInventoryContract.ID)
        .addCommand(command)

    // Step 6. Verify and sign the transaction
    transactionBuilder.verify(serviceHub)
    val signedTransaction =
        serviceHub.signInitialTransaction(transactionBuilder, inputAdInventory.publisher.owningKey)

    // Step 7. Collect the counterparty's signature
    val counterpartySession = initiateFlow(advertiserAccount.host)
    val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, listOf(counterpartySession)))

    // Step 8. Finalize the transaction
    subFlow(FinalityFlow(fullySignedTransaction, listOf(counterpartySession)))

    return "DONE TRANSACTION"

}

@InitiatedBy(AcceptAdvertisementProposalFlow::class)
class AcceptAdvertisementProposalResponderFlow(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Implement any additional checks here
            }
        }
        subFlow(signedTransactionFlow)
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
}
*/

