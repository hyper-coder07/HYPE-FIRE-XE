package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GraphicsConfig
import com.example.data.model.GraphicsPreset
import com.example.data.model.PlayerProfile
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SafeZoneEmerald
import com.example.ui.theme.StormCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    profile: PlayerProfile,
    onSaveSettings: (GraphicsConfig, String) -> Unit,
    onSignOut: () -> Unit
) {
    var config by remember(profile.graphicsPreset, profile.targetFps, profile.shadowsEnabled) {
        mutableStateOf(GraphicsConfig.fromProfile(profile))
    }
    var callsign by remember(profile.callsign) { mutableStateOf(profile.callsign) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                Text(
                    text = "LOW-END DEVICE OPTIMIZER",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Tune FPS cap, viewport render scale, shadows, and particles for zero frame drops.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Preset Profiles
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = SafeZoneEmerald)
                        Text(
                            text = "PERFORMANCE PRESETS",
                            style = MaterialTheme.typography.titleLarge,
                            color = SafeZoneEmerald
                        )
                    }

                    GraphicsPreset.entries.forEach { preset ->
                        val selected = config.preset == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) SafeZoneEmerald.copy(alpha = 0.16f) else ObsidianBg,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) SafeZoneEmerald else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preset_${preset.name}")
                                .clickable {
                                    config = config.copy(
                                        preset = preset,
                                        targetFps = preset.defaultFps,
                                        resolutionScale = preset.resolutionScale.toFloat(),
                                        shadowsEnabled = preset.shadows,
                                        particlesEnabled = preset.particles
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selected) SafeZoneEmerald else TextPrimary
                                    )
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected preset",
                                        tint = SafeZoneEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manual Fine-Tuning
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ENGINE & CONTROLS TUNING",
                        style = MaterialTheme.typography.titleLarge,
                        color = ElectricCyan
                    )

                    Text("TARGET FPS CAP", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 45, 60).forEach { fps ->
                            FilterChip(
                                selected = config.targetFps == fps,
                                onClick = { config = config.copy(targetFps = fps) },
                                label = { Text("$fps FPS") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan,
                                    selectedLabelColor = ObsidianBg
                                ),
                                modifier = Modifier.weight(1f).testTag("fps_chip_$fps")
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "TERRAIN & DETAIL SCALE: ${(config.resolutionScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Slider(
                            value = config.resolutionScale,
                            onValueChange = { config = config.copy(resolutionScale = it) },
                            valueRange = 0.6f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = FlameOrange,
                                activeTrackColor = FlameOrange
                            )
                        )
                    }

                    ToggleRow(
                        title = "Dynamic Drop Shadows",
                        subtitle = "Disable on older GPUs to save fillrate",
                        checked = config.shadowsEnabled,
                        onCheckedChange = { config = config.copy(shadowsEnabled = it) }
                    )
                    ToggleRow(
                        title = "Combat & Muzzle Particles",
                        subtitle = "Disable to keep 0-drop thermals in final zone",
                        checked = config.particlesEnabled,
                        onCheckedChange = { config = config.copy(particlesEnabled = it) }
                    )
                    ToggleRow(
                        title = "Smart Mobile Aim-Assist",
                        subtitle = "Snaps fire toward closest visible enemy",
                        checked = config.aimAssistEnabled,
                        onCheckedChange = { config = config.copy(aimAssistEnabled = it) }
                    )

                    OutlinedTextField(
                        value = callsign,
                        onValueChange = { if (it.length <= 24) callsign = it },
                        label = { Text("Survivor Callsign") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("callsign_input")
                    )

                    Button(
                        onClick = { onSaveSettings(config, callsign) },
                        colors = ButtonDefaults.buttonColors(containerColor = SafeZoneEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_optimizer_button")
                    ) {
                        Text("SAVE OPTIMIZER PROFILE", color = ObsidianBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Account Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StormCrimson),
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("SIGN OUT")
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
