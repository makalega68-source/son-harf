package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact Kelime Tahtı header lockup.
 *
 * The full supplied logo is square and belongs on roomy screens. The home header is only
 * 205x61dp, so it uses the dedicated app icon as the compact emblem plus a readable wordmark.
 * The legacy function name remains for source compatibility.
 */
@Composable
fun SonHarfOfficialLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.kelime_tahti_app_icon),
            contentDescription = sh("Kelime Tahtı ikonu", "Kelime Tahtı icon"),
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(13.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = sh("KELİME\nTAHTI", "WORD\nTHRONE"),
            color = Color(0xFFF1D071),
            fontSize = 17.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
