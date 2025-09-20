package io.middlepoint.tvsleep.model

sealed class AdbState {
    data object Connecting : AdbState()

    data object Ready : AdbState()

    data class Failed(
        val reason: String,
    ) : AdbState()
}
