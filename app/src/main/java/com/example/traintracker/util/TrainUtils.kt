package com.example.traintracker.util

import android.content.Context
import com.example.traintracker.R
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Suppress("unused")
object TrainUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun calculateDelay(expected: String?, scheduled: String?, context: Context): String {
        if (expected.isNullOrBlank() || scheduled.isNullOrBlank() || scheduled == "--:--" || expected == "--:--") return ""

        return try {
            val scheduledTime = LocalTime.parse(scheduled, timeFormatter)
            val expectedTime = LocalTime.parse(expected, timeFormatter)

            val diff = Duration.between(scheduledTime, expectedTime).toMinutes()

            when {
                diff > 0 -> "+$diff"
                diff < 0 -> diff.toString()
                else -> context.getString(R.string.on_time)
            }
        } catch (_: Exception) {
            ""
        }
    }
}
