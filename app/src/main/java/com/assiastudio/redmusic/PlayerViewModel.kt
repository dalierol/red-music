package com.assiastudio.redmusic

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _currentSong = mutableStateOf<Song?>(null)
    val currentSong: State<Song?> = _currentSong

    private val _songs = mutableStateOf<List<Song>>(emptyList())
    val songs: State<List<Song>> = _songs

    private val _currentPosition = mutableLongStateOf(0L)
    val currentPosition: State<Long> = _currentPosition

    private val _duration = mutableLongStateOf(0L)
    val duration: State<Long> = _duration

    private val PREFS_NAME = "RedMusicPrefs"
    private val KEY_LAST_SONG_ID = "last_song_id"

    fun setupController(context: Context) {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            val player = controller ?: return@addListener
            setupPlayerListener(player)
            // Servis bağlandığında eğer liste varsa son şarkıyı player'a hazırla
            if (player.mediaItemCount == 0 && _songs.value.isNotEmpty()) {
                prepareLastPlayedSong(player)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                _isPlaying.value = isPlayingNow
                if (isPlayingNow) startProgressUpdate()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) _duration.longValue = player.duration
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentMediaUri = mediaItem?.localConfiguration?.uri
                val song = _songs.value.find { it.contentUri == currentMediaUri }
                _currentSong.value = song
                song?.let { saveLastSong(it.id) }
            }
        })
    }

    fun loadSongs(context: Context) {
        _songs.value = getSongsFromDevice(context)
    }

    fun playSong(song: Song) {
        val player = controller ?: return

        // Playlist oluşturma (İleri/Geri butonları için)
        val mediaItems = _songs.value.map { s ->
            MediaItem.Builder()
                .setUri(s.contentUri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).setArtworkUri(s.artworkUri).build())
                .build()
        }
        player.setMediaItems(mediaItems)
        val index = _songs.value.indexOf(song)
        player.seekTo(index, 0L)
        player.prepare()
        player.play()
    }

    private fun prepareLastPlayedSong(player: Player) {
        val sharedPref = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastId = sharedPref.getLong(KEY_LAST_SONG_ID, -1L)
        val lastSong = _songs.value.find { it.id == lastId }

        if (lastSong != null) {
            val mediaItems = _songs.value.map { s ->
                MediaItem.Builder()
                    .setUri(s.contentUri)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).setArtworkUri(s.artworkUri).build())
                    .build()
            }
            player.setMediaItems(mediaItems)
            player.seekTo(_songs.value.indexOf(lastSong), 0L)
            player.prepare()
            _currentSong.value = lastSong
        }
    }

    private fun saveLastSong(songId: Long) {
        val sharedPref = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putLong(KEY_LAST_SONG_ID, songId).apply()
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (_isPlaying.value) {
                _currentPosition.longValue = controller?.currentPosition ?: 0L
                delay(1000)
            }
        }
    }

    fun togglePlayPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(position: Float) { controller?.seekTo(position.toLong()) }
    fun playNextSong() { controller?.seekToNext() }
    fun playPreviousSong() { controller?.seekToPrevious() }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}