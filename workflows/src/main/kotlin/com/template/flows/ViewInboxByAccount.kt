package com.template.flows


import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.template.states.AdInventoryState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.vault.QueryCriteria


@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewInboxByAccount(
        val acctname : String
) : FlowLogic<List<String>>() {

    @Suspendable
    override fun call(): List<String> {

        val myAccount = accountService.accountInfo(acctname).single().state.data
        val criteria = QueryCriteria.VaultQueryCriteria(
                externalIds = listOf(myAccount.identifier.id)
        )

        val AdPlaced = serviceHub.vaultService.queryBy(
                contractStateType = AdInventoryState::class.java,
                criteria = criteria
        ).states.map { "\n" + "Ad Proposal: " + it.state.data.linearId }

        return AdPlaced
    }
}



