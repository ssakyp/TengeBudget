package com.tenge.budget.ui

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** A one-shot message for the user, optionally with an action (e.g. "Undo"). */
data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (suspend () -> Unit)? = null
)

/**
 * ViewModels post user feedback here; the single SnackbarHost in the
 * navigation scaffold shows it. This keeps feedback visible even when the
 * screen that caused it has already been popped (e.g. "Deleted — Undo").
 */
class SnackbarMessenger {
    private val channel = Channel<SnackbarEvent>(Channel.BUFFERED)
    val events: Flow<SnackbarEvent> = channel.receiveAsFlow()

    fun post(message: String, actionLabel: String? = null, onAction: (suspend () -> Unit)? = null) {
        channel.trySend(SnackbarEvent(message, actionLabel, onAction))
    }
}
