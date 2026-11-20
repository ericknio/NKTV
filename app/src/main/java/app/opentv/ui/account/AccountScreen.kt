/*
 * This file is part of NKTV.
 */
package app.opentv.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.opentv.core.ServiceLocator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.data.model.Source
import app.opentv.ui.SourcesViewModel

/**
 * The "Minha Conta" tab: shows the single active NKTV login, lets it be edited, or removed to log
 * out back to the setup screen. Mirrors the single-provider model [app.opentv.ui.onboarding.AddSourceScreen]
 * uses — one account per install, not a multi-provider list like [app.opentv.ui.settings.ProvidersScreen].
 */
@Composable
fun AccountScreen(
    onEditSource: (Source) -> Unit,
    onLogout: () -> Unit,
    onAddSource: () -> Unit,
    viewModel: SourcesViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val source = ui.sources.firstOrNull()

    if (source == null) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Nenhuma conta configurada.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddSource) { Text("Adicionar Nova Conta") }
        }
        return
    }

    var showPassword by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    // Real status: authenticate against the server on open (and on demand), not a fixed label.
    val context = LocalContext.current
    val sourceRepository = remember { ServiceLocator.get(context).sourceRepository }
    var status by remember { mutableStateOf(AccountStatus.CHECKING) }
    var checkNonce by remember { mutableIntStateOf(0) }
    LaunchedEffect(source.id, source.username, source.password, checkNonce) {
        status = AccountStatus.CHECKING
        status = if (sourceRepository.test(source).isSuccess) AccountStatus.ONLINE else AccountStatus.OFFLINE
    }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Minha Conta",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        source.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(16.dp))

                when (status) {
                    AccountStatus.CHECKING -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Verificando status da conta...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AccountStatus.ONLINE -> StatusPill("● Online (Conta ativa e rodando)", Color(0xFF4CAF50))
                    AccountStatus.OFFLINE -> StatusPill(
                        "● Offline (Servidor indisponível ou falha na conexão)",
                        MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { checkNonce++ },
                    enabled = status != AccountStatus.CHECKING,
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Testar Conexão")
                }

                Spacer(Modifier.height(20.dp))

                AccountField(label = "Usuário", value = source.username ?: "—")

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccountField(
                        label = "Senha",
                        value = if (showPassword) (source.password ?: "—") else "••••••••",
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha",
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { onEditSource(source) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Editar Login e Senha") }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Sair / Excluir Conta") }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Sair da conta") },
            text = { Text("Deseja realmente sair e remover esta conta?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        viewModel.delete(source)
                        onLogout()
                    },
                ) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") }
            },
        )
    }
}

private enum class AccountStatus { CHECKING, ONLINE, OFFLINE }

@Composable
private fun StatusPill(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun AccountField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
