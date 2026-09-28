package com.ostarosto.app.feature.address

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.dataToJsonString
import com.multiplatform.webview.jsbridge.processParams
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.ostarosto.app.core.designsystem.OstaColors
import com.ostarosto.app.core.designsystem.PrimaryButton
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.SavedAddress
import com.ostarosto.app.feature.location.leafletMapHtml
import kotlinx.serialization.Serializable

@Serializable
private data class PinMovedParams(val lat: Double, val lng: Double)

@Serializable
private data class Ack(val ok: Boolean = true)

/**
 * Add/edit a named delivery location. The map is embedded directly (rather than
 * pushed as a separate route) so its picked pin never has to survive a
 * navigate-away-and-back round trip — same [leafletMapHtml] + JS bridge the
 * full-screen [com.ostarosto.app.feature.location.LocationPickerScreen] uses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddressSheet(
    existing: SavedAddress?,
    centerLatitude: Double,
    centerLongitude: Double,
    onDismiss: () -> Unit,
    onSave: (label: String, latitude: Double, longitude: Double, addressText: String?, makeDefault: Boolean) -> Unit,
) {
    var label by remember { mutableStateOf(existing?.label.orEmpty()) }
    var addressText by remember { mutableStateOf(existing?.addressText.orEmpty()) }
    var makeDefault by remember { mutableStateOf(existing?.isDefault ?: false) }
    var latitude by remember { mutableStateOf(existing?.latitude ?: centerLatitude) }
    var longitude by remember { mutableStateOf(existing?.longitude ?: centerLongitude) }

    val html = remember { leafletMapHtml(latitude, longitude) }
    val webViewState = rememberWebViewStateWithHTMLData(data = html, mimeType = "text/html")
    val webViewNavigator = rememberWebViewNavigator()
    val jsBridge = rememberWebViewJsBridge(webViewNavigator)

    LaunchedEffect(webViewState) { webViewState.webSettings.isJavaScriptEnabled = true }
    LaunchedEffect(jsBridge) {
        jsBridge.register(object : IJsMessageHandler {
            override fun methodName() = "PinMoved"
            override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
                val params = processParams<PinMovedParams>(message)
                latitude = params.lat
                longitude = params.lng
                callback(dataToJsonString(Ack()))
            }
        })
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            if (existing == null) Ar.addAddress else Ar.editAddress,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OstaColors.Ink,
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Ar.addressLabelHome, Ar.addressLabelWork, Ar.addressLabelOther).forEach { preset ->
                FilterChip(selected = label == preset, onClick = { label = preset }, label = { Text(preset) })
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text(Ar.addressLabelHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))) {
            WebView(
                state = webViewState,
                modifier = Modifier.fillMaxSize(),
                captureBackPresses = false,
                navigator = webViewNavigator,
                webViewJsBridge = jsBridge,
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = addressText,
            onValueChange = { addressText = it },
            label = { Text(Ar.addressDetailsOptional) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(Ar.setAsDefault, style = MaterialTheme.typography.bodyMedium)
            Switch(checked = makeDefault, onCheckedChange = { makeDefault = it })
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(Ar.cancel) }
            PrimaryButton(
                text = Ar.save,
                enabled = label.isNotBlank(),
                onClick = { onSave(label.trim(), latitude, longitude, addressText.ifBlank { null }, makeDefault) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}
