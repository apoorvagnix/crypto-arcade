package com.template.webserver

import com.template.flows.AcceptAdvertisementProposalFlow
import com.template.flows.ProposeAdvertisementFlow
import com.template.flows.GetActiveProposal
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

            val flowHandle = proxy.startFlow(::ProposeAdvertisementFlow, adData.advertiser, adData.publisher,
                adData.adType, adData.adPlacement, hundredDollars, Date())
            val result = flowHandle.returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Successfully proposed advertisement with ID: $result")
        } catch (e: Exception) {
            log.error("Error proposing advertisement", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to propose advertisement: ${e.message}")
        }
    }

    @GetMapping("/accept")
    fun acceptAdvertisement(@RequestParam publisher: String,
                            @RequestParam advertiser: String,
                            @RequestParam linearId: UUID
    ): ResponseEntity<String> {
        return try {

            val flowHandle = proxy.startFlow(::AcceptAdvertisementProposalFlow, publisher, advertiser, linearId)
            val result = flowHandle.returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Successfully accepted advertisement with ID: $linearId")
        } catch (e: Exception) {
            log.error("Error accepting advertisement", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to accept advertisement: ${e.message}")
        }
    }

    @GetMapping("/showAdvertisement")
    fun showAdvertisement(@RequestParam advertiser: String): ResponseEntity<String> {
        return try {
            val flowHandle = proxy.startFlow(::GetActiveProposal, advertiser)
            val result = flowHandle.returnValue.get()
            ResponseEntity.status(HttpStatus.OK).body(result)
        } catch (e: Exception) {
            log.error("Error accepting advertisement", e)
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
    val adExpiry: String
)
