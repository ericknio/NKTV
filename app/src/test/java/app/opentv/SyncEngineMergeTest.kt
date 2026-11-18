/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv

import app.opentv.sync.SyncEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SyncEngineMergeTest {

    @Test
    fun `new incoming watch position wins when no existing position exists`() {
        val update = SyncEngine.shouldUpdatePosition(
            existingUpdatedAtMillis = null,
            incomingUpdatedAtMillis = 5_000L,
        )

        assertThat(update).isTrue()
    }

    @Test
    fun `newer incoming watch position wins over older local position`() {
        val update = SyncEngine.shouldUpdatePosition(
            existingUpdatedAtMillis = 10_000L,
            incomingUpdatedAtMillis = 15_000L,
        )

        assertThat(update).isTrue()
    }

    @Test
    fun `older incoming watch position loses against newer local position`() {
        val update = SyncEngine.shouldUpdatePosition(
            existingUpdatedAtMillis = 20_000L,
            incomingUpdatedAtMillis = 15_000L,
        )

        assertThat(update).isFalse()
    }

    @Test
    fun `equal timestamp preserves existing position`() {
        val update = SyncEngine.shouldUpdatePosition(
            existingUpdatedAtMillis = 20_000L,
            incomingUpdatedAtMillis = 20_000L,
        )

        assertThat(update).isFalse()
    }
}
