package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CadenzaAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    var isAudioPlaying = false
        private set

    suspend fun play(url: String, onCompletion: () -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, Uri.parse(url))
                    setOnCompletionListener {
                        isAudioPlaying = false
                        onCompletion()
                    }
                    prepare()
                    start()
                }
                isAudioPlaying = true
            } catch (e: Exception) {
                e.printStackTrace()
                isAudioPlaying = false
                onCompletion()
            }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.takeIf { it.isPlaying }?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isAudioPlaying = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
