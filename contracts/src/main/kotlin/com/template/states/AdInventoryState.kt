package com.template.states

import com.template.contracts.AdInventoryContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.serialization.ConstructorForDeserialization
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(AdInventoryContract::class)
data class AdInventoryState(
    //private val adInventoryId: UUID,
    private val adType: String,
    private val adPlacement: String,
    private val adCost: Amount<Currency>,
    private val adExpiry: Date?,
    private val adStatus: String,
    private val rejectReason: String?,
    val publisher: AnonymousParty,
    val advertiser: AnonymousParty,
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
    override val participants: List<AbstractParty> = listOf(publisher, advertiser)

) : LinearState {
    @ConstructorForDeserialization
    constructor(adType: String,
                adPlacement: String,
                adCost: Amount<Currency>,
                adExpiry: Date?,
                adStatus: String,
                rejectReason: String?,
                publisher: AnonymousParty,
                advertiser: AnonymousParty
    ) : this (
        adType,
        adPlacement,
        adCost,
        adExpiry,
        adStatus,
        rejectReason,
        publisher,
        advertiser,
        UniqueIdentifier(),
        participants = listOf(publisher, advertiser)
    )

    fun getAdStatus() : String = adStatus

    fun getCost() : Amount<Currency> = adCost

    fun getAdType() : String = adType

    fun getLinerId() : UniqueIdentifier = linearId
}
