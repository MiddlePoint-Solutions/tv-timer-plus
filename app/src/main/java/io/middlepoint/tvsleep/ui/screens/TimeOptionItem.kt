package io.middlepoint.tvsleep.ui.screens

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class TimeOptionItem(
    val time: String,
    val label: String,
    val timeInMillis: Long,
)

val timeOptions =
    listOf(
//        TimeOptionItem("15 Min", "Short distraction", 15.minutes.inWholeMilliseconds),
        TimeOptionItem("10 Seconds", "Short distraction", 10.seconds.inWholeMilliseconds),
        TimeOptionItem("1 Hour", "1 hour of peace", 1.hours.inWholeMilliseconds),
        TimeOptionItem("1.5 Hours", "Movie", (1.hours + 30.minutes).inWholeMilliseconds),
        TimeOptionItem("2 Hours", "Long Movie", 2.hours.inWholeMilliseconds),
    )
