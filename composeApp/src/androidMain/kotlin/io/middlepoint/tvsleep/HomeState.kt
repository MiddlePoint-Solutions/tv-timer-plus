package io.middlepoint.tvsleep

sealed class HomeState {
    data object Connecting : HomeState()

    data object TimeSelection : HomeState()

    data object Timer : HomeState()

    data object Failed : HomeState()
}

fun AdbState.mapToHomeState(isTimerActive: Boolean): HomeState =
    when (this) {
        AdbState.Connecting -> HomeState.Connecting
        AdbState.Ready ->
            if (isTimerActive) {
                HomeState.Timer
            } else {
                HomeState.TimeSelection
            }

        is AdbState.Failed -> HomeState.Failed
    }
