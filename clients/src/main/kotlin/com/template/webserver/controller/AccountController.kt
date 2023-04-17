package com.template.webserver.controller

import com.template.flows.CreateNewAccount
import com.template.flows.ProposeAdvertisementFlow
import com.template.flows.ShareAccountTo
import com.template.webserver.AdData
import com.template.webserver.NodeRPCConnection
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
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
@RequestMapping("/api/v1/advertisement")
class AccountController(nodeRPCConnection: NodeRPCConnection) {

    val proxy = nodeRPCConnection.proxy

    @PostMapping("/publisher/create-account")
    fun createPublisher(@RequestBody acctName: String): ResponseEntity<String> {
        return try {

            val flowHandle = proxy.startFlow(::CreateNewAccount, acctName)
            val result = flowHandle.returnValue.get()

            val shareToX500Name = CordaX500Name("advertiser", "New York", "US")
            val shareTo = proxy.wellKnownPartyFromX500Name(shareToX500Name) ?: throw IllegalArgumentException("Party not found: $shareToX500Name")
            val flowHandle2 = proxy.startFlow(::ShareAccountTo, acctName, shareTo)
            val result2 = flowHandle.returnValue.get()

            ResponseEntity.status(HttpStatus.CREATED).body("Successfully created and share account: $acctName")
        } catch (e: Exception) {
            log.error("Error proposing advertisement", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create account: ${e.message}")
        }
    }

    @PostMapping("/advertiser/create-account")
    fun createAdvertiser(@RequestBody acctName: String): ResponseEntity<String> {
        return try {

            val flowHandle = proxy.startFlow(::CreateNewAccount, acctName)
            val result = flowHandle.returnValue.get()

            val shareToX500Name = CordaX500Name("publisher", "London", "GB")
            val shareTo = proxy.wellKnownPartyFromX500Name(shareToX500Name) ?: throw IllegalArgumentException("Party not found: $shareToX500Name")
            val flowHandle2 = proxy.startFlow(::ShareAccountTo, acctName, shareTo)
            val result2 = flowHandle.returnValue.get()

            ResponseEntity.status(HttpStatus.CREATED).body("Successfully created and share account: $acctName")
        } catch (e: Exception) {
            log.error("Error proposing advertisement", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create account: ${e.message}")
        }
    }

}