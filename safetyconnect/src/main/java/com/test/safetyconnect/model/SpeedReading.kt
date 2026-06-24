package com.test.safetyconnect.model
    data class SpeedReading(
        val speed: Float,           // in km/h
        val timestamp: Long,
        val accuracy: Float,
        val latitude: Double,
        val longitude: Double
    )
    