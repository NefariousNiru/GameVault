package com.gamevault.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamevault.app.R

/** Branded identity used on splash, login, and every main screen. */
@Composable
fun VaultBrand(modifier: Modifier = Modifier, iconColor: Color = Color(0xFFA897FF)) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painterResource(R.drawable.gv_brand_mark),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = "GAMEVAULT",
            color = Color(0xFFF8FAFF),
            fontSize = 17.sp,
            letterSpacing = 2.7.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
