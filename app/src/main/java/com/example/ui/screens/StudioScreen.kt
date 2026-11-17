package com.example.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.theme.BooyahGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.MythicMagenta
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SafeZoneEmerald
import com.example.ui.theme.StormCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MusicUiState
import com.example.ui.viewmodel.StudioViewModel
import com.example.ui.viewmodel.VideoUiState

@Composable
fun StudioScreen(studioViewModel: StudioViewModel) {
    var activeTab by remember { mutableIntStateOf(0) }
    val musicState by studioViewModel.musicState.collectAsStateWithLifecycle()
    val isPlayingMusic by studioViewModel.isPlayingMusic.collectAsStateWithLifecycle()
    val videoState by studioViewModel.videoState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SOUNDTRACK & HIGHLIGHT STUDIO",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Create custom battle tracks with Lyria 3 or animate squad photos into highlight reels.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        modifier = Modifier.testTag("studio_tab_music")
                    ) {
                        Text("Soundtrack Lab")
                    }
                    SegmentedButton(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Default.Movie, contentDescription = null) },
                        modifier = Modifier.testTag("studio_tab_video")
                    ) {
                        Text("Photo Animator")
                    }
                }
            }
        }

        if (activeTab == 0) {
            item {
                MusicStudioCard(
                    musicState = musicState,
                    isPlaying = isPlayingMusic,
                    onGenerate = { prompt, usePro ->
                        studioViewModel.generateMusicTrack(prompt, usePro)
                    },
                    onTogglePlay = { studioViewModel.toggleMusicPlayback() },
                    onStop = { studioViewModel.stopAudio() }
                )
            }
        } else {
            item {
                VideoAnimatorCard(
                    videoState = videoState,
                    onGenerate = { uri, prompt, aspect ->
                        studioViewModel.animatePhotoToVideo(uri, prompt, aspect)
                    }
                )
            }
        }
    }
}

@Composable
private fun MusicStudioCard(
    musicState: MusicUiState,
    isPlaying: Boolean,
    onGenerate: (String, Boolean) -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit
) {
    var useProModel by remember { mutableStateOf(false) }
    var prompt by remember {
        mutableStateOf("High-energy battle royale phonk & synthwave drop beat with punchy 808 bass")
    }

    val presets = remember {
        listOf(
            "Booyah Phonk" to "Aggressive esports phonk beat with distorted cowbell and heavy 808 drop",
            "Cyber Lobby" to "Futuristic cyber-samurai synthwave instrumental with pulsing arpeggios",
            "Final Zone" to "Tense cinematic orchestral hybrid trailer music with taiko war drums",
            "Victory Anthem" to "Uplifting EDM trap festival drop celebrating a battle royale victory"
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "LYRIA 3 MUSIC GENERATOR",
                style = MaterialTheme.typography.titleLarge,
                color = ElectricCyan
            )

            // Model Selector: lyria-3-clip-preview vs lyria-3-pro-preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !useProModel,
                    onClick = { useProModel = false },
                    label = { Text("Clip <=30s (lyria-3-clip-preview)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = ObsidianBg
                    ),
                    modifier = Modifier.weight(1f).testTag("model_lyria_clip")
                )
                FilterChip(
                    selected = useProModel,
                    onClick = { useProModel = true },
                    label = { Text("Full Track (lyria-3-pro-preview)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MythicMagenta,
                        selectedLabelColor = ObsidianBg
                    ),
                    modifier = Modifier.weight(1f).testTag("model_lyria_pro")
                )
            }

            // Quick Vibes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.take(2).forEach { (label, fullPrompt) ->
                    OutlinedButton(
                        onClick = { prompt = fullPrompt },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.drop(2).forEach { (label, fullPrompt) ->
                    OutlinedButton(
                        onClick = { prompt = fullPrompt },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Track Prompt (mood, tempo, instruments)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("music_prompt_input")
            )

            Button(
                onClick = { onGenerate(prompt, useProModel) },
                enabled = musicState !is MusicUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_music_button")
            ) {
                if (musicState is MusicUiState.Loading) {
                    CircularProgressIndicator(
                        color = ObsidianBg,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("COMPOSING TRACK...", color = ObsidianBg, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = ObsidianBg)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("GENERATE SOUNDTRACK", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            }

            when (musicState) {
                is MusicUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StormCrimson.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StormCrimson)
                    ) {
                        Text(
                            text = musicState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is MusicUiState.Ready -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SafeZoneEmerald.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TRACK READY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SafeZoneEmerald
                                    )
                                    Text(
                                        text = musicState.result.modelUsed,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = onTogglePlay,
                                        modifier = Modifier.size(48.dp).testTag("music_play_pause_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause track" else "Play track",
                                            tint = SafeZoneEmerald
                                        )
                                    }
                                    IconButton(
                                        onClick = onStop,
                                        modifier = Modifier.size(48.dp).testTag("music_stop_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Stop track",
                                            tint = StormCrimson
                                        )
                                    }
                                }
                            }
                            if (musicState.result.lyricsOrStructure.isNotBlank()) {
                                Text(
                                    text = musicState.result.lyricsOrStructure,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun VideoAnimatorCard(
    videoState: VideoUiState,
    onGenerate: (Uri?, String, String) -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var aspectRatio by remember { mutableStateOf("16:9") }
    var prompt by remember {
        mutableStateOf("Cinematic battle royale intro animation with glowing embers, dynamic camera push-in, and dramatic lighting")
    }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "HIGHLIGHT PHOTO ANIMATOR",
                style = MaterialTheme.typography.titleLarge,
                color = BooyahGold
            )
            Text(
                text = "Upload any squad or avatar photo and animate it into a video using gemini-omni-1.2-flash.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            // Upload Photo Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .testTag("upload_photo_button")
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            ) {
                if (selectedImageUri != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected photo for video animation",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianBg.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        ) {
                            Text(
                                text = "Tap to change photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload photo",
                            tint = ElectricCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "TAP TO UPLOAD PHOTO",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Uses Android Photo Picker (no storage permission needed)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Aspect Ratio Selector: 16:9 Landscape or 9:16 Portrait
            Text("OUTPUT ASPECT RATIO", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = aspectRatio == "16:9",
                    onClick = { aspectRatio = "16:9" },
                    label = { Text("16:9 Landscape") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BooyahGold,
                        selectedLabelColor = ObsidianBg
                    ),
                    modifier = Modifier.weight(1f).testTag("aspect_16_9")
                )
                FilterChip(
                    selected = aspectRatio == "9:16",
                    onClick = { aspectRatio = "9:16" },
                    label = { Text("9:16 Portrait") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BooyahGold,
                        selectedLabelColor = ObsidianBg
                    ),
                    modifier = Modifier.weight(1f).testTag("aspect_9_16")
                )
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Animation Motion Prompt") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("video_prompt_input")
            )

            Button(
                onClick = { onGenerate(selectedImageUri, prompt, aspectRatio) },
                enabled = videoState !is VideoUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = BooyahGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_video_button")
            ) {
                if (videoState is VideoUiState.Loading) {
                    CircularProgressIndicator(
                        color = ObsidianBg,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(videoState.statusMessage, color = ObsidianBg, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = ObsidianBg)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("ANIMATE INTO $aspectRatio VIDEO", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            }

            when (videoState) {
                is VideoUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StormCrimson.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StormCrimson)
                    ) {
                        Text(
                            text = videoState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is VideoUiState.Ready -> {
                    val ratioFloat = if (videoState.result.aspectRatio == "9:16") 9f / 16f else 16f / 9f
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "GENERATED ${videoState.result.aspectRatio} VIDEO READY",
                            style = MaterialTheme.typography.labelMedium,
                            color = SafeZoneEmerald
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratioFloat)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianBg)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        val controller = MediaController(ctx)
                                        controller.setAnchorView(this)
                                        setMediaController(controller)
                                        setVideoPath(videoState.result.videoFile.absolutePath)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}
