package com.example.worktimetracker.domain.commute

import app.cash.turbine.test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlinx.coroutines.test.runTest

class CommutePhaseTrackerTest {

    private lateinit var tracker: CommutePhaseTracker

    @BeforeEach
    fun setup() {
        tracker = CommutePhaseTracker()
    }

    @Test
    fun `initial phase is null when no commute active`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `startCommute sets phase to OUTBOUND`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()

            assertEquals(CommutePhase.OUTBOUND, awaitItem())
        }
    }

    @Test
    fun `enterOffice transitions from OUTBOUND to IN_OFFICE`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            tracker.enterOffice()
            assertEquals(CommutePhase.IN_OFFICE, awaitItem())
        }
    }

    @Test
    fun `exitOffice transitions from IN_OFFICE to RETURN`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            tracker.enterOffice()
            assertEquals(CommutePhase.IN_OFFICE, awaitItem())

            tracker.exitOffice()
            assertEquals(CommutePhase.RETURN, awaitItem())
        }
    }

    @Test
    fun `completeCommute transitions from RETURN to COMPLETED`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            tracker.enterOffice()
            assertEquals(CommutePhase.IN_OFFICE, awaitItem())

            tracker.exitOffice()
            assertEquals(CommutePhase.RETURN, awaitItem())

            tracker.completeCommute()
            assertEquals(CommutePhase.COMPLETED, awaitItem())
        }
    }

    @Test
    fun `reset clears phase to null`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            tracker.reset()
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `exitOffice does nothing when in OUTBOUND phase`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            // exitOffice should be ignored in OUTBOUND phase
            tracker.exitOffice()
            expectNoEvents()
        }
    }

    @Test
    fun `enterOffice does nothing when no commute active`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.enterOffice()
            expectNoEvents()
        }
    }

    @Test
    fun `completeCommute from OUTBOUND transitions directly to COMPLETED`() = runTest {
        // Edge case: User never enters office but returns to station
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            tracker.completeCommute()
            assertEquals(CommutePhase.COMPLETED, awaitItem())
        }
    }

    @Test
    fun `short office exit and re-enter stays in IN_OFFICE`() = runTest {
        // Edge case: Lunch break outside - should not change tracking
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            tracker.enterOffice()
            assertEquals(CommutePhase.IN_OFFICE, awaitItem())

            // Short exit - transitions to RETURN
            tracker.exitOffice()
            assertEquals(CommutePhase.RETURN, awaitItem())

            // Re-enter - transitions back to IN_OFFICE
            tracker.enterOffice()
            assertEquals(CommutePhase.IN_OFFICE, awaitItem())
        }
    }

    @Test
    fun `COMPLETED phase ignores enterOffice`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            tracker.completeCommute()
            assertEquals(CommutePhase.COMPLETED, awaitItem())

            // Entering office after completion should be ignored
            tracker.enterOffice()
            expectNoEvents()
        }
    }

    @Test
    fun `COMPLETED phase ignores exitOffice`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            tracker.enterOffice()
            assertEquals(CommutePhase.IN_OFFICE, awaitItem())

            tracker.exitOffice()
            assertEquals(CommutePhase.RETURN, awaitItem())

            tracker.completeCommute()
            assertEquals(CommutePhase.COMPLETED, awaitItem())

            // Exiting office after completion should be ignored
            tracker.exitOffice()
            expectNoEvents()
        }
    }

    @Test
    fun `COMPLETED phase ignores completeCommute`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            tracker.completeCommute()
            assertEquals(CommutePhase.COMPLETED, awaitItem())

            // Completing again should be ignored
            tracker.completeCommute()
            expectNoEvents()
        }
    }

    @Test
    fun `IN_OFFICE phase ignores completeCommute`() = runTest {
        tracker.currentPhase.test {
            assertEquals(null, awaitItem())

            tracker.startCommute()
            assertEquals(CommutePhase.OUTBOUND, awaitItem())

            tracker.enterOffice()
            assertEquals(CommutePhase.IN_OFFICE, awaitItem())

            // Cannot complete from IN_OFFICE (must exit first)
            tracker.completeCommute()
            expectNoEvents()
        }
    }
}
