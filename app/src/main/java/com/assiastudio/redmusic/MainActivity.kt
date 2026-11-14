package com.assiastudio.redmusic

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

// --- ÖZEL KIRMIZI RENK PALETİ ---
val DarkRedBackground = Color(0xFF140000)
val PrimaryRed = Color(0xFFE53935)
val SurfaceRed = Color(0xFF330A0A)
val TextLightRed = Color(0xFFFFEBEE)
val TextMutedRed = Color(0xFFEF9A9A)

class MainActivity : ComponentActivity() {

    // ViewModel'i burada tanımlıyoruz ki izin sonrası erişebilelim
    private val playerViewModel: PlayerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // İzin verildiği an şarkıları yükle (Boş ekran sorununu çözer)
            playerViewModel.loadSongs(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // İzin Kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = DarkRedBackground,
                    primary = PrimaryRed,
                    surface = SurfaceRed,
                    onBackground = TextLightRed
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ViewModel'i parametre olarak gönderiyoruz
                    PlayerScreen(playerViewModel)
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(viewModel: PlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying
    val songs by viewModel.songs
    val currentSong by viewModel.currentSong
    val currentPosition by viewModel.currentPosition
    val totalDuration by viewModel.duration

    // İlk açılışta (eğer izin zaten varsa) yükle
    LaunchedEffect(Unit) {
        viewModel.setupController(context)
        viewModel.loadSongs(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // --- ALBÜM KAPAĞI BÖLÜMÜ ---
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceRed),
            contentAlignment = Alignment.Center
        ) {
            if (currentSong?.artworkUri != null) {
                AsyncImage(
                    model = currentSong?.artworkUri,
                    contentDescription = "Albüm Kapağı",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = TextMutedRed
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Şarkı ve Sanatçı İsmi
        Text(
            text = currentSong?.title ?: "Red Music'e Hoş Geldin",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextLightRed,
            maxLines = 1
        )
        Text(
            text = currentSong?.artist ?: "Bir şarkı seçin",
            fontSize = 16.sp,
            color = TextMutedRed,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Slider (İlerleme Çubuğu)
        Slider(
            value = if (totalDuration > 0) currentPosition.toFloat() else 0f,
            valueRange = 0f..(if (totalDuration > 0) totalDuration.toFloat() else 1f),
            onValueChange = { viewModel.seekTo(it) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = PrimaryRed,
                activeTrackColor = PrimaryRed,
                inactiveTrackColor = SurfaceRed
            )
        )

        // Süre Bilgisi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(currentPosition), color = TextMutedRed, fontSize = 12.sp)
            Text(text = formatTime(totalDuration), color = TextMutedRed, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Kontrol Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.playPreviousSong() }) {
                Icon(Icons.Filled.SkipPrevious, "Geri", Modifier.size(45.dp), TextLightRed)
            }

            FloatingActionButton(
                onClick = { viewModel.togglePlayPause() },
                shape = CircleShape,
                containerColor = PrimaryRed,
                contentColor = Color.White,
                modifier = Modifier.size(65.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Oynat/Duraklat",
                    modifier = Modifier.size(35.dp)
                )
            }

            IconButton(onClick = { viewModel.playNextSong() }) {
                Icon(Icons.Filled.SkipNext, "İleri", Modifier.size(45.dp), TextLightRed)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Liste Başlığı
        Text(
            text = "Kitaplığın",
            modifier = Modifier.align(Alignment.Start),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextLightRed
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Şarkı Listesi
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(songs) { song ->
                SongItem(song = song) {
                    viewModel.playSong(song)
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Küçük Liste İkonu/Kapağı
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceRed),
            contentAlignment = Alignment.Center
        ) {
            if (song.artworkUri != null) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Audiotrack, null, tint = PrimaryRed, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(text = song.title, color = TextLightRed, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(text = song.artist, color = TextMutedRed, fontSize = 14.sp, maxLines = 1)
        }
    }
}

// Zaman Formatlayıcı (00:00)
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}