package dev.focusgarden.app

import kotlin.test.Test
import kotlin.test.assertEquals

class FocusSessionTest {
    @Test
    fun runningSessionCountsDown() {
        val session = FocusSession(durationMinutes = 1, remainingSeconds = 2).start().tick()
        assertEquals(1, session.remainingSeconds)
        assertEquals(SessionStatus.Running, session.status)
    }

    @Test
    fun sessionCompletesAtZero() {
        val session = FocusSession(durationMinutes = 1, remainingSeconds = 1).start().tick()
        assertEquals(0, session.remainingSeconds)
        assertEquals(SessionStatus.Completed, session.status)
    }

    @Test
    fun pausedSessionDoesNotCountDown() {
        val session = FocusSession(durationMinutes = 1, remainingSeconds = 30).pause().tick()
        assertEquals(30, session.remainingSeconds)
    }

    @Test
    fun previewCanJumpToAnyGrowthStage() {
        val session = FocusSession(durationMinutes = 1).seekTo(.75f)
        assertEquals(15, session.remainingSeconds)
    }

    @Test
    fun previewCanReturnFromCompletedStage() {
        val session = FocusSession(durationMinutes = 1, remainingSeconds = 0, status = SessionStatus.Completed)
            .seekTo(.5f)
        assertEquals(30, session.remainingSeconds)
        assertEquals(SessionStatus.Paused, session.status)
    }
}
