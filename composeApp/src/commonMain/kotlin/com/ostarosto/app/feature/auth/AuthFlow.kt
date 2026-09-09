package com.ostarosto.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ostarosto.app.core.designsystem.OstaRostoLogo
import com.ostarosto.app.core.l10n.Ar
import org.koin.compose.viewmodel.koinViewModel

/**
 * Arabic phone → OTP → name sign-in flow, shown full-screen while logged out.
 */
@Composable
fun AuthFlow(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.step) {
        if (state.step == AuthStep.Done) onAuthenticated()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OstaRostoLogo(modifier = Modifier.fillMaxWidth(0.55f).height(84.dp))
        Spacer(Modifier.height(24.dp))

        when (state.step) {
            AuthStep.Phone -> {
                Text(Ar.enterPhone, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhone,
                    label = { Text(Ar.phoneHint) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
                Button(onClick = viewModel::requestOtp, enabled = !state.loading && state.phone.length >= 8) {
                    Text(Ar.sendCode)
                }
            }

            AuthStep.Otp -> {
                Text("${Ar.codeSentTo} ${state.phone}", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = state.otp,
                    onValueChange = viewModel::onOtp,
                    label = { Text(Ar.enterCode) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
                Button(onClick = viewModel::verifyOtp, enabled = !state.loading && state.otp.length == 6) {
                    Text(Ar.verify)
                }
            }

            AuthStep.Profile -> {
                Text(Ar.yourName, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onName,
                    label = { Text(Ar.yourName) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
                Button(onClick = viewModel::completeProfile, enabled = !state.loading && state.name.trim().length >= 2) {
                    Text(Ar.saveAndContinue)
                }
            }

            AuthStep.Done -> Unit
        }

        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
