package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.template.contracts.AdInventoryContract
import com.template.states.AdInventoryState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class ProposeAdvertisementFlow(
    private val adType: String,
    private val adPlacement: String,
    private val adCost: Amount<Currency>,
    private val adExpiry: Date?,
    private val publisher: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        if (ourIdentity == publisher) {
            throw IllegalArgumentException("The advertiser and publisher must be different parties.")
        }

        //build the output state
        val uniqueID = UniqueIdentifier()

        // Create a new AdInventoryState with the "proposed" status
        val adInventoryState = AdInventoryState(
            adType = adType,
            adPlacement = adPlacement,
            adCost = adCost,
            adExpiry = adExpiry,
            adStatus = "proposed",
            rejectReason = null,
            publisher = publisher,
            advertiser = ourIdentity,
            linearId = uniqueID
        )


        // Build the transaction
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val txBuilder = TransactionBuilder(notary)
            .addOutputState(adInventoryState)
            .addCommand(AdInventoryContract.Commands.Propose(), listOf(ourIdentity.owningKey, publisher.owningKey))

        // Verify and sign the transaction
        txBuilder.verify(serviceHub)

        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // Collect the publisher's signature
        val publisherSession = initiateFlow(publisher)
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(publisherSession)))

        val sessions = if (publisher != ourIdentity) listOf(publisherSession) else emptyList()
        // Finalize the transaction and update the ledger
        //return subFlow(FinalityFlow(fullySignedTx, listOf(publisherSession)))
        return subFlow(FinalityFlow(fullySignedTx, sessions))
    }
}

@InitiatedBy(ProposeAdvertisementFlow::class)
class ProposeAdvertisementResponderFlow(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Add any additional checks if required
            }
        }

        val signedTx = subFlow(signedTransactionFlow)

        // Call the ReceiveFinalityFlow to record the transaction on the publisher's side
        subFlow(ReceiveFinalityFlow(counterpartySession, signedTx.id))
    }
}
*/


// *********
// * NEW Flows *
// *********

@StartableByRPC
@StartableByService
@InitiatingFlow
class ProposeAdvertisementFlow(
    val whoAmI: String,
    val whereTo:String,
    private val adType: String,
    private val adPlacement: String,
    private val adCost: Amount<Currency>,
    private val adExpiry: Date?
) : FlowLogic<String>(){

    companion object {
        object GENERATING_KEYS : ProgressTracker.Step("Generating Keys for transactions.")
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction for between accounts")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
            GENERATING_KEYS,
            GENERATING_TRANSACTION,
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
        val myAccount = accountService.accountInfo(whoAmI).single().state.data
        val myKey = subFlow(NewKeyForAccount(myAccount.identifier.id)).owningKey

        val targetAccount = accountService.accountInfo(whereTo).single().state.data
        val targetAcctAnonymousParty = subFlow(RequestKeyForAccount(targetAccount))

        //LOGIC
        /*if (ourIdentity == publisher) {
            throw IllegalArgumentException("The advertiser and publisher must be different parties.")
        }*/

        //build the output state
        val uniqueID = UniqueIdentifier()

        // Create a new AdInventoryState with the "proposed" status
        val adInventoryState = AdInventoryState(
            adType = adType,
            adPlacement = adPlacement,
            adCost = adCost,
            adExpiry = adExpiry,
            adStatus = "proposed",
            rejectReason = null,
            publisher = targetAcctAnonymousParty,
            advertiser = AnonymousParty(myKey),
            linearId = uniqueID
        )

        //generating State for transfer
        progressTracker.currentStep = GENERATING_TRANSACTION
        val output = adInventoryState
        val transactionBuilder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
        transactionBuilder.addOutputState(output)
            .addCommand(AdInventoryContract.Commands.Propose(), listOf(targetAcctAnonymousParty.owningKey,myKey))

        //Pass along Transaction
        progressTracker.currentStep = SIGNING_TRANSACTION
        val locallySignedTx = serviceHub.signInitialTransaction(transactionBuilder, listOfNotNull(ourIdentity.owningKey,myKey))


        //Collect sigs
        progressTracker.currentStep =GATHERING_SIGS
        val sessionForAccountToSendTo = initiateFlow(targetAccount.host)
        val accountToMoveToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAccountToSendTo, targetAcctAnonymousParty.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature)

        progressTracker.currentStep =FINALISING_TRANSACTION
        subFlow(FinalityFlow(signedByCounterParty, listOf(sessionForAccountToSendTo).filter { it.counterparty != ourIdentity }))
        return "Proposal send to " + targetAccount.host.name.organisation + "'s "+ targetAccount.name + " team."
    }
}

@InitiatedBy(ProposeAdvertisementFlow::class)
class ProposeAdvertisementResponderFlow(val counterpartySession: FlowSession) : FlowLogic<Unit>(){
    @Suspendable
    override fun call() {
        //placeholder to record account information for later use
        val accountMovedTo = AtomicReference<AccountInfo>()

        //extract account information from transaction
        val transactionSigner = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                val keyStateMovedTo = stx.coreTransaction.outRefsOfType(AdInventoryState::class.java).first().state.data.getPublisher()
                keyStateMovedTo.let {
                    accountMovedTo.set(accountService.accountInfo(keyStateMovedTo.owningKey)?.state?.data)
                }
                if (accountMovedTo.get() == null) {
                    throw IllegalStateException("Account to move to was not found on this node")
                }
            }
        }
        //record and finalize transaction
        val transaction = subFlow(transactionSigner)
        if (counterpartySession.counterparty != serviceHub.myInfo.legalIdentities.first()) {
            val recievedTx = subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = transaction.id, statesToRecord = StatesToRecord.ALL_VISIBLE))
            val accountInfo = accountMovedTo.get()
            if (accountInfo != null) {
                subFlow(BroadcastToCarbonCopyReceiversFlow(accountInfo, recievedTx.coreTransaction.outRefsOfType(AdInventoryState::class.java).first()))
            }
        }
    }

}