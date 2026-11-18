/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv

import app.opentv.recording.RecordingEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordingRescheduleTest {

    @Test
    fun `overdue recording whose broadcast window ended while device was off is marked missed`() {
        val now = 100_000L
        val startMillis = 80_000L
        val endMillis = 90_000L // Already ended

        val action = RecordingEngine.resolveRescheduleAction(startMillis, endMillis, now)

        assertThat(action).isEqualTo(RecordingEngine.RescheduleAction.MISSED)
    }

    @Test
    fun `ongoing recording whose broadcast window is partially underway starts immediately`() {
        val now = 100_000L
        val startMillis = 95_000L // Started 5s ago
        val endMillis = 120_000L // Ends in 20s

        val action = RecordingEngine.resolveRescheduleAction(startMillis, endMillis, now)

        assertThat(action).isEqualTo(RecordingEngine.RescheduleAction.START_NOW)
    }

    @Test
    fun `future recording is scheduled normally`() {
        val now = 100_000L
        val startMillis = 150_000L // Future
        val endMillis = 180_000L

        val action = RecordingEngine.resolveRescheduleAction(startMillis, endMillis, now)

        assertThat(action).isEqualTo(RecordingEngine.RescheduleAction.SCHEDULE)
    }
}
