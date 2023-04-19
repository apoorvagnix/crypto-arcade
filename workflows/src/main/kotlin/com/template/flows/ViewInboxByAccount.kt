package com.template.flows


import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.template.states.AdInventoryState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable


@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewInboxByAccount(
        val acctname : String
) : FlowLogic<List<AdInventoryInfo>>() {

    @Suspendable
    override fun call(): List<AdInventoryInfo> {

        val myAccount = accountService.accountInfo(acctname).single().state.data
        val criteria = QueryCriteria.VaultQueryCriteria(
                externalIds = listOf(myAccount.identifier.id)
        )

        /*val AdPlaced = serviceHub.vaultService.queryBy(
                contractStateType = AdInventoryState::class.java,
                criteria = criteria
        ).states.map { "\n" + "Ad Proposal: " + it.state.data.linearId  + ", AdType: " + it.state.data.getAdType() }*/

        val adPlaced = serviceHub.vaultService.queryBy(
            contractStateType = AdInventoryState::class.java,
            criteria = criteria
        ).states.map {
            val data = it.state.data
            val publisherAccountInfo = accountService.accountInfo(data.publisher.owningKey)?.state?.data
            val advertiserAccountInfo = accountService.accountInfo(data.advertiser.owningKey)?.state?.data

            val publisherName = publisherAccountInfo?.name ?: "Unknown"
            val advertiserName = advertiserAccountInfo?.name ?: "Unknown"

            AdInventoryInfo(
                linearId = data.linearId.id.toString(),
                adType = data.getAdType(),
                publisher = publisherName,
                advertiser = advertiserName
            )
        }

        return adPlaced
    }
}

@CordaSerializable
data class AdInventoryInfo(
    val linearId: String,
    val adType: String,
    val publisher: String,
    val advertiser: String
)



