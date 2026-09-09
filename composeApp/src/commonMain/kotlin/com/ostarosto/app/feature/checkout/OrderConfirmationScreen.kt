package com.ostarosto.app.feature.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ostarosto.app.core.designsystem.PrimaryButton
import com.ostarosto.app.core.l10n.Ar

@Composable
fun OrderConfirmationScreen(
    orderId: Long,
    onTrack: (Long) -> Unit,
    onBackToMenu: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(Ar.orderPlaced, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(32.dp))
        PrimaryButton(text = Ar.trackOrder, onClick = { onTrack(orderId) })
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(Ar.menu)
        }
    }
}
