package com.ostarosto.app.feature.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ostarosto.app.core.designsystem.PrimaryButton
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.PaymentPoll
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaymentWaitingScreen(
    reference: String,
    onPaid: (Long) -> Unit,
    onFailed: () -> Unit,
    viewModel: PaymentViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(reference) { viewModel.startPolling(reference) }
    LaunchedEffect(state.poll, state.order) {
        if (state.poll == PaymentPoll.Paid) {
            state.order?.let { onPaid(it.id) }
        }
    }

    // The Paymob checkout opens as an in-app browser tab over this screen, not a
    // separate app — so coming back from it (Custom Tab / SFSafariViewController
    // dismissed) resumes this screen's lifecycle rather than restarting the app.
    // Re-check immediately on that return instead of waiting for the next poll
    // tick. Skip the very first resume, which just means the screen appeared.
    val sawFirstResume = remember(reference) { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (sawFirstResume.value) {
            viewModel.check(reference)
        }
        sawFirstResume.value = true
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.poll) {
                PaymentPoll.Failed -> {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(Ar.paymentFailed, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(32.dp))
                    PrimaryButton(text = Ar.backToCart, onClick = onFailed)
                }

                else -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(20.dp))
                    Text(
                        Ar.waitingForPayment,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Ar.waitingForPaymentHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(28.dp))
                    PrimaryButton(
                        text = Ar.iHavePaid,
                        onClick = { viewModel.check(reference) },
                        loading = state.checking,
                    )
                }
            }
        }
    }
}
