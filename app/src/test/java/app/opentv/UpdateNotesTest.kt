/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv

import app.opentv.update.UpdateChecker
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The update prompt must show only the news: never the APK hash the release body carries. */
class UpdateNotesTest {
    private val hash = "a".repeat(64)

    @Test
    fun shaLineIsNotShown() {
        val raw = "Nova aba Favoritos em Filmes e Séries.\n\nSHA-256 (NKTV.V1.0.10.apk): $hash"
        assertThat(UpdateChecker.cleanNotes(raw)).isEqualTo("Nova aba Favoritos em Filmes e Séries.")
    }

    @Test
    fun shaInsideHtmlCommentIsNotShown() {
        val raw = "Nova aba Favoritos em Filmes e Séries.\n\n<!-- SHA-256 (NKTV.V1.0.11.apk): $hash -->"
        assertThat(UpdateChecker.cleanNotes(raw)).isEqualTo("Nova aba Favoritos em Filmes e Séries.")
    }

    @Test
    fun ordinaryNotesStayIntact() {
        assertThat(UpdateChecker.cleanNotes("- Correção de bugs.")).isEqualTo("• Correção de bugs.")
    }
}
