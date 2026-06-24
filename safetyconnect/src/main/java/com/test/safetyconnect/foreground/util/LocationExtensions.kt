package com.test.safetyconnect.foreground.util

import android.location.Location
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Extension function to round all location values to 2 decimal places
 * This improves data consistency and readability
 *
 * @return New Location object with rounded values
 */
fun Location.roundToTwoDecimals(): Location {
    return Location(this).apply {
        latitude = this@roundToTwoDecimals.latitude.roundToTwoDecimalPlaces()
        longitude = this@roundToTwoDecimals.longitude.roundToTwoDecimalPlaces()

        if (this@roundToTwoDecimals.hasAltitude()) {
            altitude = this@roundToTwoDecimals.altitude.roundToTwoDecimalPlaces()
        }

        if (this@roundToTwoDecimals.hasSpeed()) {
            speed = this@roundToTwoDecimals.speed.toDouble().roundToTwoDecimalPlaces().toFloat()
        }

        if (this@roundToTwoDecimals.hasBearing()) {
            bearing = this@roundToTwoDecimals.bearing.toDouble().roundToTwoDecimalPlaces().toFloat()
        }

        if (this@roundToTwoDecimals.hasAccuracy()) {
            accuracy = this@roundToTwoDecimals.accuracy.toDouble().roundToTwoDecimalPlaces().toFloat()
        }
    }
}

/**
 * Round Double to exactly 2 decimal places using BigDecimal for precision
 */
private fun Double.roundToTwoDecimalPlaces(): Double {
    return BigDecimal(this)
        .setScale(2, RoundingMode.HALF_UP)
        .toDouble()
}

