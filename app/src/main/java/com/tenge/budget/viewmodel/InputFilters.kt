package com.tenge.budget.viewmodel

/** Keeps digits and at most one decimal point — used by every amount field. */
internal fun cleanAmountInput(value: String): String {
    var seenDot = false
    return buildString {
        for (c in value) {
            if (c.isDigit()) append(c)
            else if (c == '.' && !seenDot) { seenDot = true; append(c) }
        }
    }.take(12)
}

/** Shows a Double the way a person would type it back into a field. */
internal fun Double.toInputString(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
