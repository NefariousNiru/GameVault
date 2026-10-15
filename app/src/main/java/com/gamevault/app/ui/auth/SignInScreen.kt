package com.gamevault.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamevault.app.R
import com.gamevault.app.ui.components.VaultBrand
import com.gamevault.app.ui.theme.VaultMuted
import com.gamevault.app.ui.theme.VaultPalettes

/** Dedicated branded sign-in; does not mutate or bypass the working Firebase flow. */
@Composable
fun SignInScreen(state: AuthUiState, onGoogleSignIn: () -> Unit) {
    val palette = VaultPalettes.pc
    Box(Modifier.fillMaxSize().background(Color(0xFF080B13))) {
        Image(painterResource(R.drawable.gv_hero_pc), null, Modifier.fillMaxWidth().height(475.dp),
            contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color(0x55090B14), Color(0xAA090B14), Color(0xFF080B13), Color(0xFF080B13))
        )))
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(26.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            VaultBrand(iconColor = palette.accent)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("EVERY GAME. ONE VAULT.", color = palette.accent,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.3.sp, fontSize = 11.sp)
                Text("Your next adventure\nstarts here.", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 45.sp)
                Text("A beautiful home for your PC and PS5 collections, your backlog, and what's next.",
                    fontSize = 15.sp, lineHeight = 24.sp, color = VaultMuted)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onGoogleSignIn,
                    enabled = !state.loading && !state.busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent,
                        contentColor = Color(0xFF0B0B18))
                ) {
                    if (state.loading || state.busy) {
                        CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                state.error?.let { Text(it, color = Color(0xFFFFA6B4), fontSize = 13.sp) }
                Text("Private by default · Synced to your Google account",
                    color = VaultMuted, fontSize = 11.sp)
            }
        }
    }
}
