package com.ostarosto.app.feature.location

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.dataToJsonString
import com.multiplatform.webview.jsbridge.processParams
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.ostarosto.app.core.designsystem.BackTopBar
import com.ostarosto.app.core.designsystem.PrimaryButton
import com.ostarosto.app.core.designsystem.money
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.domain.model.DeliveryAvailability
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
private data class PinMovedParams(val lat: Double, val lng: Double)

@Serializable
private data class Ack(val ok: Boolean = true)

@Composable
fun LocationPickerScreen(
    onBack: () -> Unit,
    onConfirm: (latitude: Double, longitude: Double, availability: DeliveryAvailability) -> Unit,
    viewModel: LocationPickerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val html = remember { leafletMapHtml(state.centerLatitude, state.centerLongitude) }
    val webViewState = rememberWebViewStateWithHTMLData(data = html, mimeType = "text/html")
    val webViewNavigator = rememberWebViewNavigator()
    val jsBridge = rememberWebViewJsBridge(webViewNavigator)

    LaunchedEffect(webViewState) {
        webViewState.webSettings.isJavaScriptEnabled = true
    }

    LaunchedEffect(jsBridge) {
        jsBridge.register(object : IJsMessageHandler {
            override fun methodName() = "PinMoved"
            override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
                val params = processParams<PinMovedParams>(message)
                viewModel.onPinMoved(params.lat, params.lng)
                callback(dataToJsonString(Ack()))
            }
        })
    }

    Scaffold(topBar = { BackTopBar(Ar.selectLocationOnMap, onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            WebView(
                state = webViewState,
                modifier = Modifier.fillMaxSize(),
                captureBackPresses = false,
                navigator = webViewNavigator,
                webViewJsBridge = jsBridge,
            )

            Card(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    val availability = state.availability
                    when {
                        !state.hasPinned -> Text(Ar.tapMapToPlacePin, style = MaterialTheme.typography.bodyMedium)

                        state.checking -> Row {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(Ar.checkingAvailability, style = MaterialTheme.typography.bodyMedium)
                        }

                        state.error != null -> Text(
                            state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        availability != null && availability.available -> Column {
                            Text(
                                availability.zoneName ?: Ar.deliveryAvailableHere,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${Ar.deliveryFee}: ${money(availability.deliveryFee)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            availability.estimatedTime?.let {
                                Text(
                                    "${Ar.estimatedTimeLabel}: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        availability != null -> Text(
                            Ar.deliveryNotAvailableHere,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        text = Ar.confirmLocation,
                        enabled = availability?.available == true && state.latitude != null && state.longitude != null,
                        onClick = {
                            val lat = state.latitude
                            val lng = state.longitude
                            if (lat != null && lng != null && availability != null) onConfirm(lat, lng, availability)
                        },
                    )
                }
            }
        }
    }
}
