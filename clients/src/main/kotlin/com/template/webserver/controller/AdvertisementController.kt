package com.template.webserver

import com.template.flows.*
import com.template.flows.AcceptAdvertisementProposalFlow
import net.corda.core.contracts.Amount
import net.corda.core.messaging.startFlow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

private val log = LoggerFactory.getLogger(RestController::class.java)

@RestController
@RequestMapping("/api/v1/advertisement") // Base URL for the API
class AdvertisementController(nodeRPCConnection: NodeRPCConnection) {

    val proxy = nodeRPCConnection.proxy

    @PostMapping("/propose")
    fun proposeAdvertisement(@RequestBody adData: AdData): ResponseEntity<String> {
        return try {
            val hundredDollars = Amount(100_000L, Currency.getInstance("USD"))

            val proposeAdvertisementData = ProposeAdvertisementData(
                advertiser = adData.advertiser,
                publisher = adData.publisher,
                adType = adData.adType,
                adPlacement = adData.adPlacement,
                adCost = hundredDollars,
                adExpiry = Date(),
                adURL = adData.adURL
            )

            val flowHandle = proxy.startFlow(::ProposeAdvertisementFlow, proposeAdvertisementData)
            val result = flowHandle.returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Successfully proposed advertisement with ID: $result")
        } catch (e: Exception) {
            log.error("Error proposing advertisement", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to propose advertisement: ${e.message}")
        }
    }

    @PostMapping("/accept")
    fun acceptAdvertisement(@RequestBody request: AcceptAdvertisementRequest): ResponseEntity<String> {
        return try {

            val flowHandle = proxy.startFlow(::AcceptAdvertisementProposalFlow, request.publisher, request.advertiser,
                request.linearId)
            val result = flowHandle.returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Successfully accepted advertisement")
        } catch (e: Exception) {
            log.error("Error accepting advertisement", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to accept advertisement: ${e.message}")
        }
    }

    @PostMapping("/reject")
    fun rejectAdvertisement(@RequestBody request: rejectAdvertisementRequest): ResponseEntity<String> {
        return try {

            val flowHandle = proxy.startFlow(::RejectAdvertisementProposalFlow, request.publisher, request.advertiser,
                request.linearId, request.rejectReason)
            val result = flowHandle.returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Successfully rejected advertisement")
        } catch (e: Exception) {
            log.error("Error rejecting advertisement", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to reject advertisement: ${e.message}")
        }
    }

    @GetMapping("/showAdvertisement")
    fun showAdvertisement(@RequestParam advertiser: String): ResponseEntity<String> {
        return try {
            val flowHandle = proxy.startFlow(::GetActiveProposal, advertiser)
            val result = flowHandle.returnValue.get()
            ResponseEntity.status(HttpStatus.OK).body(result)
        } catch (e: Exception) {
            log.error("Error showing advertisement", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error")
        }
    }

}

data class AdData(
    val advertiser: String,
    val publisher: String,
    val adType: String,
    val adPlacement: String,
    val adCost: String,
    val adExpiry: String,
    val adURL: String
)

data class AcceptAdvertisementRequest(
    val publisher: String,
    val advertiser: String,
    val linearId: UUID
)

data class rejectAdvertisementRequest(
    val publisher: String,
    val advertiser: String,
    val linearId: UUID,
    val rejectReason: String
)
