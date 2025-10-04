package io.middlepoint.tvsleep.ui.screens

import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Serializable
data class TimeOptionItem(
    val time: String,
    val label: String,
    val timeInMillis: Long,
)

val timeOptions =
    buildList {
        addAll(
            listOf(
                TimeOptionItem("15 Minutes", "Short distraction", 15.minutes.inWholeMilliseconds),
                TimeOptionItem("30 Minutes", "Short distraction", 30.minutes.inWholeMilliseconds),
                TimeOptionItem("45 Minutes", "Short distraction", 45.minutes.inWholeMilliseconds),
                TimeOptionItem("1 Hour", "1 hour of peace", 1.hours.inWholeMilliseconds),
                TimeOptionItem("1.5 Hours", "Movie", (1.hours + 30.minutes).inWholeMilliseconds),
                TimeOptionItem("2 Hours", "Long Movie", 2.hours.inWholeMilliseconds),
                TimeOptionItem("3 Hours", "Long Movie", 3.hours.inWholeMilliseconds),
            ),
        )
    }
