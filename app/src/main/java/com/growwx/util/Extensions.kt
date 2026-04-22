package com.growwx.util

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ─── Number formatting ────────────────────────────────────────────────────────

fun Double.toRupees(): String = when {
    this >= 10_000_000 -> "₹${String.format("%.2f", this / 10_000_000)}Cr"
    this >= 100_000    -> "₹${String.format("%.2f", this / 100_000)}L"
    this >= 1_000      -> "₹${String.format("%,.2f", this)}"
    else               -> "₹${String.format("%.2f", this)}"
}

fun Double.toRupeesShort(): String = when {
    abs(this) >= 10_000_000 -> "₹${String.format("%.1f", this / 10_000_000)}Cr"
    abs(this) >= 100_000    -> "₹${String.format("%.1f", this / 100_000)}L"
    abs(this) >= 1_000      -> "₹${String.format("%.1f", this / 1_000)}K"
    else                    -> "₹${String.format("%.2f", this)}"
}

fun Double.toPctString(includeSign: Boolean = true): String {
    val pct = String.format("%.2f", abs(this)) + "%"
    return if (!includeSign) pct
    else if (this >= 0) "+$pct" else "-$pct"
}

fun Double.toPnlColor(isPositive: Boolean = this >= 0): androidx.compose.ui.graphics.Color =
    if (isPositive) com.growwx.ui.theme.GrowwXColor.Green else com.growwx.ui.theme.GrowwXColor.Red

// ─── Date formatting ──────────────────────────────────────────────────────────

fun Long.toReadableDate(): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))

fun Long.toReadableDateTime(): String =
    SimpleDateFormat("dd MMM · hh:mm a", Locale.getDefault()).format(Date(this))

fun Long.toShortDate(): String =
    SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(this))

// ─── Input validation ─────────────────────────────────────────────────────────

fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isValidPassword(): Boolean = this.length >= 6

fun String.isPositiveNumber(): Boolean = this.toDoubleOrNull()?.let { it > 0 } ?: false
