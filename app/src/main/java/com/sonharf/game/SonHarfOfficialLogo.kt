package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
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
 * The supplied brand artwork is square, while the home header is a wide 205x61dp slot.
 * Rendering the square artwork with Fit made the entire brand only ~61dp wide. The compact
 * lockup keeps the approved artwork visible and adds a readable product wordmark beside it.
 * The legacy function name remains for source compatibility.
 */
@Composable
fun SonHarfOfficialLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.kelime_tahti_logo),
            contentDescription = sh("Kelime Tahtı logosu", "Kelime Tahtı logo"),
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = sh("KELİME\nTAHTI", "WORD\nTHRONE"),
            color = Color(0xFFF1D071),
            fontSize = 17.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
