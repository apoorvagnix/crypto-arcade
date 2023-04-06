package com.template.flows

import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.template.states.AdInventoryState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party


@StartableByRPC
@StartableByService
@InitiatingFlow
class ShareAccountTo(
        private val acctNameShared: String,
        private val shareTo: Party
        ) : FlowLogic<String>(){

    @Suspendable
    override fun call(): String {

        //Create a new account
        val AllmyAccounts = accountService.ourAccounts()
        val SharedAccount = AllmyAccounts.single { it.state.data.name == acctNameShared }.state.data.identifier.id
        accountService.shareAccountInfoWithParty(SharedAccount,shareTo)

        return "Shared " + acctNameShared + " with " + shareTo.name.organisation
    }
}

@StartableByRPC
@StartableByService
@InitiatingFlow
class ShareStateAndSyncAccountsInitiatorFlow<T : LinearState>(
    private val stateType: Class<T>,
    private val stateAndRefToShare: StateAndRef<T>,
    private val counterparty: Party
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Establish a session with the counterparty
        val counterpartySession = initiateFlow(counterparty)

        // Share the state and sync accounts with the counterparty
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val shareStateAndSyncAccountsFuture = accountService.shareStateAndSyncAccounts(stateAndRefToShare, counterparty)
        shareStateAndSyncAccountsFuture.get()
    }
}




