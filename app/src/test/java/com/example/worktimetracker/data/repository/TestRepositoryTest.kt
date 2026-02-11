package com.example.worktimetracker.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test for TestRepository to verify dependency injection works.
 */
class TestRepositoryTest {

    @Test
    fun `isInjected returns true`() {
        val repository = TestRepository()
        assertTrue("TestRepository should indicate it is injected", repository.isInjected())
    }
}
