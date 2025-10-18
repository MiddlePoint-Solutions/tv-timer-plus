package io.middlepoint.tvsleep.ui.screens

import io.middlepoint.tvsleep.model.HomeState
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen

@Serializable
data object TimeSelection : Screen()

@Serializable
data object CustomTime : Screen()

@Serializable
data object Timer : Screen()

@Serializable
data object SetupADB : Screen()

@Serializable
data object Connecting : Screen()

@Serializable
data object Debug : Screen()

fun HomeState.mapToScreen(): Screen =
    when (this) {
        HomeState.Connecting -> Connecting
        HomeState.Failed -> SetupADB
        HomeState.TimeSelection -> TimeSelection
        HomeState.Timer -> Timer
    }
