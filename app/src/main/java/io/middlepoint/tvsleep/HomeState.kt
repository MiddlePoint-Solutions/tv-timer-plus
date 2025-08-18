package io.middlepoint.tvsleep

sealed class HomeState {
    data object Idle : HomeState()

    data object Connecting : HomeState()

    data object Ready : HomeState()

    data object Failed : HomeState()
}

fun AdbState.mapToHomeState(): HomeState =
    when (this) {
        AdbState.Idle -> HomeState.Idle
        AdbState.Connecting -> HomeState.Connecting
        AdbState.Ready -> HomeState.Ready
        is AdbState.Failed -> HomeState.Failed
    }
