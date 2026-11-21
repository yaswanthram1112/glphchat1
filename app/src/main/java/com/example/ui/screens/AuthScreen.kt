package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.attemptAutoSignIn
import com.example.data.onGoogleSignInClicked
import com.example.ui.cipher.CipherGlyph
import com.example.ui.cipher.CipherTextDisplay
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalMonochromeTokens
import com.example.ui.theme.liquidGlassChrome
import com.example.ui.theme.monochromeGlassCanvas

@Composable
fun AuthScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAuthSuccess: () -> Unit,
    onEnterDemoMode: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var previewDecoded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Stay on AuthScreen with interactive fallback */ },
            scope = scope,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .monochromeGlassCanvas(tokens)
            .safeDrawingPadding()
    ) {
        // Floating Minimal Glass Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LiquidGlassSurface(
                shape = CircleShape,
                fillColor = tokens.glassFill,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CipherGlyph(
                        letter = 'G',
                        size = 15.dp,
                        color = tokens.ink,
                        strokeWidthUnits = 2.2f
                    )
                    Text(
                        text = "GLYPHCHAT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp,
                        color = tokens.ink,
                    )
                }
            }

            LiquidGlassSurface(
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onToggleTheme)
                    .testTag("auth_toggle_theme_button")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = stringResource(R.string.cd_toggle_theme),
                        tint = tokens.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Spacious Centered Frosted Glass Card (rounded-3xl)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(28.dp),
                fillColor = tokens.glassFill,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Minimalist Monochrome Emblem + Title
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .liquidGlassChrome(
                                    shape = RoundedCornerShape(18.dp),
                                    fillColor = tokens.ink,
                                    borderColor = tokens.ink,
                                    topHighlightColor = Color.White.copy(alpha = if (tokens.isDark) 0.4f else 0.25f),
                                    sheenColor = Color.White.copy(alpha = 0.14f),
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CipherGlyph(
                                letter = 'C',
                                size = 28.dp,
                                color = tokens.canvas,
                                strokeWidthUnits = 2.2f
                            )
                        }

                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displaySmall,
                            color = tokens.ink,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Private monochrome cipher messaging with live 26-glyph rendering and instant decode peek.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.mutedInk,
                            textAlign = TextAlign.Center,
                        )
                    }

                    // Compact Interactive Messenger Preview Bubble
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlassChrome(
                                shape = RoundedCornerShape(20.dp),
                                fillColor = tokens.glassSoftFill,
                                borderColor = tokens.hairline,
                                topHighlightColor = tokens.specularTop,
                                sheenColor = tokens.specularSheen,
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (previewDecoded) "Decoded · English" else "Cipher · Live Script",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.mutedInk,
                            )
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (previewDecoded) tokens.ink else tokens.glassStrongFill, CircleShape)
                                    .border(1.dp, if (previewDecoded) tokens.ink else tokens.hairline, CircleShape)
                                    .clickable { previewDecoded = !previewDecoded }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("auth_peek_preview_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (previewDecoded) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = stringResource(R.string.cd_peek_bubble),
                                    tint = if (previewDecoded) tokens.canvas else tokens.ink,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (previewDecoded) "Glyph" else "Peek",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (previewDecoded) tokens.canvas else tokens.ink,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        // Compact incoming-style message bubble
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassChrome(
                                    shape = RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomEnd = 18.dp,
                                        bottomStart = 6.dp
                                    ),
                                    fillColor = tokens.incomingBubble,
                                    borderColor = tokens.hairline,
                                    topHighlightColor = tokens.specularTop,
                                    sheenColor = tokens.specularSheen,
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnimatedContent(
                                targetState = previewDecoded,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "auth_preview_decode"
                            ) { decoded ->
                                if (decoded) {
                                    Text(
                                        text = "MEET AT ZERO HOUR",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = tokens.ink,
                                    )
                                } else {
                                    CipherTextDisplay(
                                        text = "MEET AT ZERO HOUR",
                                        glyphSize = 18.dp,
                                        color = tokens.ink,
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "09:41",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.mutedInk,
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = null,
                                    tint = tokens.ink,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    if (authError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassChrome(
                                    shape = RoundedCornerShape(14.dp),
                                    fillColor = tokens.glassSoftFill,
                                    borderColor = tokens.hairlineStrong,
                                    topHighlightColor = tokens.specularTop,
                                    sheenColor = Color.Transparent,
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = authError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.ink,
                            )
                        }
                    }

                    // Gentle Rounded Glass Action Buttons (rounded-2xl)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .liquidGlassChrome(
                                    shape = RoundedCornerShape(16.dp),
                                    fillColor = tokens.ink,
                                    borderColor = tokens.ink,
                                    topHighlightColor = Color.White.copy(alpha = if (tokens.isDark) 0.42f else 0.24f),
                                    sheenColor = Color.White.copy(alpha = 0.12f),
                                )
                                .clickable(enabled = !isLoading) {
                                    isLoading = true
                                    authError = null
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg ->
                                            isLoading = false
                                            authError = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = {
                                            isLoading = false
                                        }
                                    )
                                }
                                .testTag("google_sign_in_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = tokens.canvas,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Sign in with Google",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = tokens.canvas
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .liquidGlassChrome(
                                    shape = RoundedCornerShape(16.dp),
                                    fillColor = tokens.glassStrongFill,
                                    borderColor = tokens.hairlineStrong,
                                    topHighlightColor = tokens.specularTop,
                                    sheenColor = tokens.specularSheen,
                                )
                                .clickable(enabled = !isLoading, onClick = onEnterDemoMode)
                                .testTag("enter_demo_mode_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Try Demo / Local Sync",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = tokens.ink
                            )
                        }
                    }

                    Text(
                        text = "Cloud sync uses Google Sign-In & Firestore. Demo mode runs locally with simulated cipher peers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.mutedInk,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
