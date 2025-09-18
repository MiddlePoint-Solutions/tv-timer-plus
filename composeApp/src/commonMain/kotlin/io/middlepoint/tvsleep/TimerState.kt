package io.middlepoint.tvsleep

sealed class TimerState {
    data class Start(
        val duration: Long,
    ) : TimerState()

    object Started : TimerState()

    data class Running(
        val duration: Long,
        val tick: Long,
    ) : TimerState()

    object Stopped : TimerState()

    object Paused : TimerState()

    object Finish : TimerState()

    object Finished : TimerState()
}
