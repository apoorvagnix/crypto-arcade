package com.template.webserver

import com.template.flows.ProposeAdvertisementFlow
import net.corda.core.contracts.Amount
import net.corda.core.messaging.startFlow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

private val log = LoggerFactory.getLogger(RestController::class.java)

@RestController
@RequestMapping("/api/v1/advertisement") // Base URL for the API
class AdvertisementController(nodeRPCConnection: NodeRPCConnection) {

    val proxy = nodeRPCConnection.proxy

    @PostMapping("/propose")
    fun proposeAdvertisement(adData: AdData): ResponseEntity<String> {
        return try {
            val tenDollars = Amount(10_000L, Currency.getInstance("USD"))

            val flowHandle = proxy.startFlow(::ProposeAdvertisementFlow, "NIKE", "EA-SPORTS",
                "Banner", "Main Screen", tenDollars, Date())
            val result = flowHandle.returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Successfully proposed advertisement with ID: $result")
        } catch (e: Exception) {
            log.error("Error proposing advertisement", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to propose advertisement: ${e.message}")
        }
    }
}

data class AdData(
    val adType: String
    // Add other fields here
)
