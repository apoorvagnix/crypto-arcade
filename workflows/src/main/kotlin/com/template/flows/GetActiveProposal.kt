package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.template.states.AdInventoryState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.node.services.vault.QueryCriteria

@StartableByRPC
@StartableByService
@InitiatingFlow
class GetActiveProposal(val advertiserName: String) : FlowLogic<String>()  {

    @Suspendable
    override fun call(): String {
        val myAccount = accountService.accountInfo(advertiserName).single().state.data
        val criteria = QueryCriteria.VaultQueryCriteria(
            externalIds = listOf(myAccount.identifier.id)
        )

        val adInventoryStates = serviceHub.vaultService.queryBy(
            contractStateType = AdInventoryState::class.java,
            criteria = criteria
        ).states

        for (stateAndRef in adInventoryStates) {
            val adInventoryState = stateAndRef.state.data
            if (adInventoryState.getAdStatus() == "proposed") {
                return "proposed"
            } else if (adInventoryState.getAdStatus() == "agreed") {
                return "agreed"
            }
        }

        return "none" // Return "none" if no matching state is found
    }


}