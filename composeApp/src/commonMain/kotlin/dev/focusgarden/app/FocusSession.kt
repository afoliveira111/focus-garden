package dev.focusgarden.app

data class FocusSession(
    val durationMinutes: Int = 25,
    val remainingSeconds: Int = durationMinutes * 60,
    val status: SessionStatus = SessionStatus.Ready,
) {
    val progress: Float
        get() = 1f - remainingSeconds.toFloat() / (durationMinutes * 60)

    fun start() = copy(status = SessionStatus.Running)
    fun pause() = copy(status = SessionStatus.Paused)

    fun tick(): FocusSession = when {
        status != SessionStatus.Running -> this
        remainingSeconds <= 1 -> copy(remainingSeconds = 0, status = SessionStatus.Completed)
        else -> copy(remainingSeconds = remainingSeconds - 1)
    }

    fun withDuration(minutes: Int) = FocusSession(durationMinutes = minutes)

    fun seekTo(progress: Float): FocusSession {
        val totalSeconds = durationMinutes * 60
        val remaining = (totalSeconds * (1f - progress.coerceIn(0f, 1f))).toInt()
        return copy(
            remainingSeconds = remaining,
            status = when {
                remaining == 0 -> SessionStatus.Completed
                status == SessionStatus.Completed -> SessionStatus.Paused
                else -> status
            },
        )
    }
}

enum class SessionStatus { Ready, Running, Paused, Completed }
