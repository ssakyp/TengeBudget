package com.tenge.budget.data.repository

/** Outcome of a write that can be refused by a business rule (the reason is meant for the user). */
sealed interface OpResult {
    data object Success : OpResult
    data class Rejected(val reason: String) : OpResult
}
