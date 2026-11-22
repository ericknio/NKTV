/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import app.opentv.pairing.QrCodes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.opentv.BuildConfig
import app.opentv.R
import app.opentv.core.ServiceLocator
import app.opentv.core.findActivity
import app.opentv.update.UpdateChecker
import app.opentv.update.UpdateHub
import app.opentv.update.UpdateUiState
import app.opentv.update.UpdateViewModel
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/**
 * About: what this is, what version it is, and how to get involved. The update check here is the
 * manual counterpart to the automatic one — someone who just heard a fix landed can pull it now
 * instead of waiting for the periodic check.
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateLine by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var showPatchNotes by remember { mutableStateOf(false) }
    val pendingUpdate by UpdateHub.pending.collectAsState()
    // Same instance UpdateGate uses, so the download dialog it draws reflects this install.
    val updateViewModel: UpdateViewModel = (context.findActivity() as? ComponentActivity)
        ?.let { viewModel(viewModelStoreOwner = it) } ?: viewModel()

    if (showPatchNotes) {
        AlertDialog(
            onDismissRequest = { showPatchNotes = false },
            title = {
                Text(
                    stringResource(R.string.about_patch_notes_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PatchNotesData.notes.forEach { note ->
                        PatchNoteCard(note)
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showPatchNotes = false }) {
                    Text(stringResource(R.string.common_close))
                }
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.about_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_done)) }
        }

        Spacer(Modifier.height(20.dp))

        Section(stringResource(R.string.about_version)) {
            pendingUpdate?.let { update ->
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(update.versionName) { runCatching { focusRequester.requestFocus() } }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "Nova atualização disponível: v${update.versionName.removePrefix("v")}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            update.notes.takeIf { it.isNotBlank() }
                                ?: "Atualize para receber as últimas melhorias e correções.",
                            modifier = Modifier.padding(top = 6.dp),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Button(
                            onClick = {
                                UpdateHub.state.value = UpdateUiState.Available(update)
                                updateViewModel.install()
                            },
                            modifier = Modifier.padding(top = 12.dp).focusRequester(focusRequester),
                        ) { Text("Atualizar Agora") }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Text("NKTV Player v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = !checking,
                    onClick = {
                        checking = true
                        updateLine = null
                        scope.launch {
                            val graph = ServiceLocator.get(context)
                            val update = runCatching {
                                UpdateChecker(graph.httpClient, BuildConfig.VERSION_NAME).check()
                            }.getOrNull()
                            updateLine = when {
                                update != null -> context.getString(R.string.about_update_available, update.versionName)
                                else -> context.getString(R.string.about_up_to_date)
                            }
                            // Raise the shared update prompt right here — UpdateGate overlays every
                            // screen, so the install dialog appears over About immediately.
                            UpdateHub.pending.value = update
                            if (update != null) UpdateHub.state.value = UpdateUiState.Available(update)
                            checking = false
                        }
                    },
                ) { Text(if (checking) stringResource(R.string.about_checking) else stringResource(R.string.about_check_updates)) }

                Spacer(Modifier.width(12.dp))

                OutlinedButton(onClick = { showPatchNotes = true }) {
                    Text(stringResource(R.string.about_patch_notes_btn))
                }
            }
            updateLine?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    Column(Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
}

/**
 * A tappable link row. Focusable on purpose: on a TV this is what lets the d-pad travel down the
 * About screen (and so scroll it) — a screen of plain Text has nothing to carry the scroll. Clicking
 * opens the URL in a browser, or shows it as a toast where there is no browser (some TV boxes).
 */
@Composable
private fun LinkLine(label: String, value: String, url: String = "https://$value") {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (focused) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                else Modifier,
            )
            .clickable {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure { Toast.makeText(context, url, Toast.LENGTH_LONG).show() }
            }
            .padding(vertical = 6.dp, horizontal = 6.dp),
    ) {
        Text(
            "$label:  ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private data class PatchNote(
    val version: String,
    val date: String,
    val isLatest: Boolean = false,
    val changes: List<String>,
)

private object PatchNotesData {
    val notes = listOf(
        PatchNote(
            version = "1.0.11",
            date = "Novembro 2026",
            isLatest = true,
            changes = listOf(
                "Próximo episódio automático em séries com contagem regressiva.",
                "Marcação de episódios assistidos.",
                "Melhorias de foco e navegação no controle remoto.",
            ),
        ),
        PatchNote(
            version = "1.0.10",
            date = "Novembro 2026",
            changes = listOf("Nova aba Favoritos em Filmes e Séries."),
        ),
        PatchNote(
            version = "1.0.9",
            date = "Novembro 2026",
            changes = listOf(
                "Correção de bugs e melhorias.",
                "Melhorias nas resoluções dos celulares.",
            ),
        ),
        PatchNote(
            version = "1.0.8",
            date = "Novembro 2026",
            changes = listOf(
                "Status da Conta: Verificação de status online/offline na aba Minha Conta.",
                "Cadeado em Conteúdo Adulto: Categorias protegidas visíveis com ícone de cadeado.",
                "Desbloqueio por PIN: Exigência de senha ao clicar em qualquer categoria com cadeado.",
            ),
        ),
        PatchNote(
            version = "1.0.7",
            date = "Novembro 2026",
            changes = listOf(
                "Login Seguro: Validação antes de salvar e tela de carregamento de conteúdo.",
                "Aba Minha Conta: Gestão completa de usuário e senha na barra lateral.",
                "Notificação Discreta: Alerta com badge '1' em Configurações e Sobre.",
                "Controle dos Pais Total: Bloqueio por PIN em Ao Vivo, Filmes e Séries.",
            ),
        ),
        PatchNote(
            version = "1.0.6",
            date = "Novembro 2026",
            changes = listOf(
                "Desempenho otimizado para TVs e Mi Stick (sem engasgos).",
                "Tela cheia imersiva (sem barras do sistema atrapalhando).",
                "Controles aprimorados: pular ±10s nas setas do controle.",
                "Continuar Assistindo em Filmes e Séries (retoma de onde parou).",
                "Navegação e busca mais fluidas e organizadas.",
            ),
        ),
        PatchNote(
            version = "1.0.5",
            date = "Novembro 2026",
            changes = listOf(
                "Sincronização geral de canais, filmes, séries e EPG.",
                "Interface e fontes 20% maiores para melhor leitura.",
            ),
        ),
        PatchNote(
            version = "1.0.4",
            date = "Novembro 2026",
            changes = listOf(
                "Segurança reforçada: Bloqueio de exportação de dados via backup (allowBackup=false).",
                "Gerenciador Web seguro: Proteção por código PIN de 6 dígitos e limite de tentativas.",
                "Validação de integridade: Checagem SHA-256 e assinatura no instalador de atualizações.",
                "Preservação de variantes de qualidade (SD/HD/FHD) em listas M3U.",
            ),
        ),
        PatchNote(
            version = "1.0.3",
            date = "Novembro 2026",
            changes = listOf(
                "Painel remoto: Integração com gestão dinâmica de DNS do servidor.",
                "Otimizações de conexão e reconexão automática em transmissões ao vivo.",
            ),
        ),
        PatchNote(
            version = "1.0.2",
            date = "Novembro 2026",
            changes = listOf(
                "Melhorias de desempenho no catálogo de filmes e séries (VOD).",
                "Ajustes de reprodução e estabilidade de áudio.",
            ),
        ),
        PatchNote(
            version = "1.0.1",
            date = "Novembro 2026",
            changes = listOf(
                "Lançamento inicial do NKTV Player.",
                "Suporte a listas Xtream Codes e M3U com Guia de Programação (EPG).",
            ),
        ),
    )
}

@Composable
private fun PatchNoteCard(note: PatchNote) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .border(
                width = if (note.isLatest || focused) 1.5.dp else 1.dp,
                color = when {
                    focused -> MaterialTheme.colorScheme.primary
                    note.isLatest -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(8.dp),
            )
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "v${note.version}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (note.isLatest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                if (note.isLatest) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "NOVA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                note.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        note.changes.forEach { change ->
            Row(
                Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    "• ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    change,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
