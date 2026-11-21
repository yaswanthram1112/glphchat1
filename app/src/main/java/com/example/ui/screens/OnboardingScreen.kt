package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.generateRandomPin
import com.example.data.normalizeHandleInput
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalMonochromeTokens
import com.example.ui.theme.liquidGlassChrome
import com.example.ui.theme.monochromeGlassCanvas

@Composable
fun OnboardingScreen(
    suggestedName: String,
    isBusy: Boolean,
    errorMessage: String?,
    onSubmit: (displayName: String, handle: String, pin: String) -> Unit,
    onSignOut: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    var displayName by remember(suggestedName) { mutableStateOf(suggestedName.take(40)) }
    var handle by remember(suggestedName) {
        mutableStateOf(
            normalizeHandleInput(suggestedName.replace(" ", "_")).ifBlank { "" }
        )
    }
    var pin by remember { mutableStateOf(generateRandomPin()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .monochromeGlassCanvas(tokens)
            .safeDrawingPadding()
    ) {
        // Floating Minimal Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LiquidGlassSurface(shape = CircleShape) {
                Text(
                    text = "PROFILE SETUP",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.mutedInk,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            LiquidGlassSurface(
                shape = CircleShape,
                modifier = Modifier
                    .clickable(onClick = onSignOut)
                    .testTag("onboarding_sign_out_button")
            ) {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.ink,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 72.dp),
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(28.dp),
                fillColor = tokens.glassFill,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.onboarding_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = tokens.ink,
                        )
                        Text(
                            text = stringResource(R.string.onboarding_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.mutedInk,
                        )
                    }

                    GlassLabeledField(
                        label = "Display Name",
                        value = displayName,
                        onValueChange = { displayName = it.take(40) },
                        placeholder = "e.g. Elena Solis",
                        prefix = null,
                        mono = false,
                        testTag = "onboarding_name_input"
                    )

                    GlassLabeledField(
                        label = "Unique Handle (3–24 chars)",
                        value = handle,
                        onValueChange = { handle = normalizeHandleInput(it) },
                        placeholder = "elena_s",
                        prefix = "@",
                        mono = true,
                        testTag = "onboarding_handle_input"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "4-Digit Identity PIN",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.mutedInk,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .liquidGlassChrome(
                                    shape = RoundedCornerShape(16.dp),
                                    fillColor = tokens.glassStrongFill,
                                    borderColor = tokens.hairline,
                                    topHighlightColor = tokens.specularTop,
                                    sheenColor = tokens.specularSheen,
                                )
                                .padding(start = 14.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "#",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 15.sp,
                                    color = tokens.mutedInk,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                BasicTextField(
                                    value = pin,
                                    onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(4) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tokens.ink,
                                        letterSpacing = 2.sp,
                                    ),
                                    cursorBrush = SolidColor(tokens.ink),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_pin_input")
                                )
                            }
                            IconButton(
                                onClick = { pin = generateRandomPin() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("onboarding_regen_pin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Regenerate PIN",
                                    tint = tokens.ink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
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
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.ink,
                            )
                        }
                    }

                    val canSubmit = displayName.trim().length >= 2 && handle.length >= 3 && pin.length == 4 && !isBusy

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .liquidGlassChrome(
                                shape = RoundedCornerShape(16.dp),
                                fillColor = if (canSubmit) tokens.ink else tokens.glassSoftFill,
                                borderColor = if (canSubmit) tokens.ink else tokens.hairline,
                                topHighlightColor = if (canSubmit) {
                                    if (tokens.isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.22f)
                                } else {
                                    tokens.specularTop
                                },
                                sheenColor = if (canSubmit && !tokens.isDark) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                            )
                            .clickable(enabled = canSubmit) {
                                onSubmit(displayName, handle, pin)
                            }
                            .testTag("onboarding_submit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = tokens.canvas,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Activate @${handle.ifBlank { "..." }}#$pin",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (canSubmit) tokens.canvas else tokens.mutedInk
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    prefix: String?,
    mono: Boolean,
    testTag: String,
) {
    val tokens = LocalMonochromeTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.mutedInk,
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .liquidGlassChrome(
                    shape = RoundedCornerShape(16.dp),
                    fillColor = tokens.glassStrongFill,
                    borderColor = tokens.hairline,
                    topHighlightColor = tokens.specularTop,
                    sheenColor = tokens.specularSheen,
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (prefix != null) {
                Text(
                    text = prefix,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 15.sp,
                    color = tokens.mutedInk,
                    modifier = Modifier.padding(end = 3.dp)
                )
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = if (mono) JetBrainsMonoFamily else InterFontFamily,
                        fontSize = 14.sp,
                        color = tokens.mutedInk,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = if (mono) JetBrainsMonoFamily else InterFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = tokens.ink,
                    ),
                    cursorBrush = SolidColor(tokens.ink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                )
            }
        }
    }
}
