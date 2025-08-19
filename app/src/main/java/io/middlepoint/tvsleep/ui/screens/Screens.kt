package io.middlepoint.tvsleep.ui.screens

import io.middlepoint.tvsleep.HomeState
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen

@Serializable
data object Start : Screen()

@Serializable
data object Home : Screen()

@Serializable
data object Setup : Screen()

@Serializable
data object Connecting : Screen()

@Serializable
data object Debug : Screen()

fun HomeState.mapToScreen(): Screen =
    when (this) {
        HomeState.Connecting -> Connecting
        HomeState.Failed -> Setup
        HomeState.Idle -> Start
        HomeState.Ready -> Home
    }
