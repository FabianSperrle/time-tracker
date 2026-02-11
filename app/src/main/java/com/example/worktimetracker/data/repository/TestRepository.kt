package com.example.worktimetracker.data.repository

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test repository to verify Hilt injection works correctly.
 * This will be removed in later features as real repositories are implemented.
 */
@Singleton
class TestRepository @Inject constructor() {
    fun isInjected(): Boolean = true
}
