package io.middlepoint.tvsleep

sealed class AdbState {
    data object Idle : AdbState()

    data object Connecting : AdbState()

    data object Ready : AdbState()

    data class Failed(
        val reason: String,
    ) : AdbState()
}
