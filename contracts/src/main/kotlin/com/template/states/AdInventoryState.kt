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
    private val adURL: String,
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
                adURL: String,
                rejectReason: String?,
                publisher: AnonymousParty,
                advertiser: AnonymousParty
    ) : this (
        adType,
        adPlacement,
        adCost,
        adExpiry,
        adStatus,
        adURL,
        rejectReason,
        publisher,
        advertiser,
        UniqueIdentifier(),
        participants = listOf(publisher, advertiser)
    )

    fun getAdStatus() : String = adStatus

    fun getCost() : Amount<Currency> = adCost

    fun getAdType() : String = adType

    fun getAdURL() : String = adURL

    fun getAdPlacement() : String = adPlacement

    fun getRejectReason() : String? = rejectReason

    fun getAdExpiry() : Date? = adExpiry

    fun getAdCost() : Amount<Currency> = adCost

    fun getLinerId() : UniqueIdentifier = linearId
}
