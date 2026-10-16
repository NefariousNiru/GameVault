package com.gamevault.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamevault.app.R
import com.gamevault.app.ui.components.VaultBrand
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes

/** Brighter, cleaner branded sign-in; existing Firebase sign-in behavior is unchanged. */
@Composable
fun SignInScreen(state: AuthUiState, onGoogleSignIn: () -> Unit) {
    val palette = VaultPalettes.pc
    val background = Color(0xFF080B13)

    // Increase image luminance at render time without changing the bundled artwork.
    val heroBrightness = remember {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.15f, 0f, 0f, 0f, 12f,
                    0f, 1.15f, 0f, 0f, 12f,
                    0f, 0f, 1.18f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    Box(Modifier.fillMaxSize().background(background)) {
        Image(
            painter = painterResource(R.drawable.gv_hero_pc),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(475.dp),
            contentScale = ContentScale.Crop,
            colorFilter = heroBrightness
        )

        // Reveal more of the artwork while retaining a dark, legible sign-in area.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color(0x10080B13),
                    0.35f to Color(0x20080B13),
                    0.61f to Color(0xB8080B13),
                    0.78f to background,
                    1.0f to background
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(26.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            VaultBrand(iconColor = palette.accent)

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Your next adventure\nstarts here.",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 39.sp,
                    lineHeight = 44.sp
                )
                Text(
                    "Your PC and PS5 games, all in one place.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = VaultMuted
                )
                Button(
                    onClick = onGoogleSignIn,
                    enabled = !state.loading && !state.busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = Color(0xFF0B0B18)
                    )
                ) {
                    if (state.loading || state.busy) {
                        CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                state.error?.let { error ->
                    Text(error, color = Color(0xFFFFA6B4), fontSize = 13.sp)
                }
            }
        }
    }
}
