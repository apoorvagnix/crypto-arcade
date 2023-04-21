package com.template.contracts

import com.template.states.AdInventoryState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat
// ************
// * Contract *
// ************
class AdInventoryContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.AdInventoryContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands>()
        //val output = tx.outputsOfType<AdInventoryState>().first()

        when (command.value) {
            is Commands.Propose -> {
                requireThat {
                    "No input states should be consumed when proposing an AdInventory." using (tx.inputs.isEmpty())
                    "Only one output state should be created when proposing an AdInventory." using (tx.outputs.size == 1)

                    val outputState = tx.outputsOfType<AdInventoryState>().single()

                    "The output AdInventoryState must have 'proposed' status." using (outputState.getAdStatus() == "proposed")
                }
            }

            is Commands.Reject -> {
                requireThat {
                    "Only one input state should be consumed when rejecting an AdInventory." using (tx.inputs.size == 1)
                    "Only one output state should be created when rejecting an AdInventory." using (tx.outputs.size == 1)

                    val inputState = tx.inputsOfType<AdInventoryState>().single()
                    val outputState = tx.outputsOfType<AdInventoryState>().single()

                    "The input AdInventoryState must have 'proposed' status." using (inputState.getAdStatus() == "proposed")
                    "The output AdInventoryState must have 'rejected' status." using (outputState.getAdStatus() == "rejected")
                }
            }
            is Commands.Agree -> {
                requireThat {
                    "Only one input state should be consumed when agreeing to an AdInventory." using (tx.inputs.size == 1)
                    "Only one output state should be created when agreeing to an AdInventory." using (tx.outputs.size == 1)

                    val inputState = tx.inputsOfType<AdInventoryState>().single()
                    val outputState = tx.outputsOfType<AdInventoryState>().single()
                    "The input AdInventoryState must have 'proposed' status." using (inputState.getAdStatus() == "proposed")
                    "The output AdInventoryState must have 'agreed' status." using (outputState.getAdStatus() == "agreed")
                }
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Propose : Commands
        class Reject : Commands
        class Agree : Commands
    }
}