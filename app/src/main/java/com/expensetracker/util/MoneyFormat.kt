package com.expensetracker.util

import java.text.NumberFormat
import java.util.Locale

// Indian rupee formatting (₹) with Indian digit grouping, regardless of device locale.
private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

/** Formats an amount as Indian rupees, e.g. ₹1,200.50. */
fun Double.asCurrency(): String = currencyFormat.format(this)
