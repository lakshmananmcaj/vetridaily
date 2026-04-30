package com.murugan.dailycalm

import android.media.MediaPlayer

object AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null

    fun play(url: String) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener { start() }
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
