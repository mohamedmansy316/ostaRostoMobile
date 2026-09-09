package com.ostarosto.app.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.ostarosto.app.core.l10n.Ar
import ostarostomobile.composeapp.generated.resources.Res
import ostarostomobile.composeapp.generated.resources.logo_osta
import org.jetbrains.compose.resources.painterResource

/** The Osta Rosto wordmark. Bundled from the web project's `logo-osta.png`. */
@Composable
fun OstaRostoLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.logo_osta),
        contentDescription = Ar.appName,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
