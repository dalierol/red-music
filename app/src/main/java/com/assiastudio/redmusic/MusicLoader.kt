package com.assiastudio.redmusic

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.net.Uri

fun getSongsFromDevice(context: Context): List<Song> {
    val songs = mutableListOf<Song>()

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"

    val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        sortOrder
    )
    cursor?.use { c ->
        val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        while (c.moveToNext()) {
            val id = c.getLong(idCol)
            val title = c.getString(titleCol)
            val artist = c.getString(artistCol)
            val duration = c.getInt(durationCol)
            val albumId = c.getLong(albumIdCol)


            // Albüm kapağı URI'sini oluşturma
            val artworkUri = ContentUris.withAppendedId(
                android.net.Uri.parse("content://media/external/audio/albumart"),
                albumId
            )

            // Şarkının gerçek dosya yolunu (URI) oluşturuyoruz
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id
            )
            songs.add(Song(id, title, artist, contentUri, duration, artworkUri))
        }



    }
    return songs
}