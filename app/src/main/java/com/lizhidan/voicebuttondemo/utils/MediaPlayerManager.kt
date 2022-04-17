package com.lizhidan.voicebuttondemo.utils

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.util.Log
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.IOException

object MediaPlayerManager {
    //播放音频API类：MediaPlayer
    private var mMediaPlayer: MediaPlayer? = null
    private var audioplayer: AudioTrack? = null
    //是否暂停
    private var isPause = false

    /**
     * @param filePath：文件路径 onCompletionListener：播放完成监听
     * @description 播放声音
     */
    fun playSound(
        filePath: String?,
        onCompletionListener: OnCompletionListener?,sampleRate :Int=16000
    ) {
        Log.w("VoiceButton","play file"+filePath)
        if(filePath!!.endsWith("amr")){
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            //设置一个error监听器
            mMediaPlayer!!.setOnErrorListener { _, _, _ ->
                mMediaPlayer!!.reset()
                false
            }
        } else {
            mMediaPlayer!!.reset()
        }
        try {
            mMediaPlayer!!.setOnCompletionListener(onCompletionListener)
            mMediaPlayer!!.setDataSource(filePath)
            mMediaPlayer!!.prepare()
            mMediaPlayer!!.start()
        } catch (e: Exception) {
        }}else{
            if (mMediaPlayer == null) {
                val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT

                val minBufferSize =
                    AudioTrack.getMinBufferSize(sampleRate, android.media.AudioFormat.CHANNEL_OUT_MONO, audioFormat)
                audioplayer=AudioTrack(
                    AudioManager.STREAM_MUSIC, sampleRate, android.media.AudioFormat.CHANNEL_OUT_MONO, audioFormat,
                    minBufferSize, AudioTrack.MODE_STREAM
                );
                audioplayer!!.play();
                val fileInputStream = FileInputStream(filePath)
                val dataInputStream = DataInputStream(BufferedInputStream(fileInputStream))
                Thread {
                    try {
                        val tempBuffer = ByteArray(minBufferSize)
                        while (dataInputStream.available() > 0) {
                            val readCount: Int = dataInputStream.read(tempBuffer)
                            if (readCount == AudioTrack.ERROR_INVALID_OPERATION ||
                                readCount == AudioTrack.ERROR_BAD_VALUE
                            ) {
                                continue
                            }
                            if (readCount != 0 && readCount != -1) {
                                audioplayer!!.write(tempBuffer, 0, readCount)
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        try {
                            if (dataInputStream != null) {
                                dataInputStream.close()
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }.start()

            }

        }
    }

    /**
     * @param
     * @description 暂停播放
     */
    fun pause() {
        if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) { //正在播放的时候
            mMediaPlayer!!.pause()
            isPause = true
        }
        if (audioplayer != null && audioplayer!!.playState== AudioTrack.PLAYSTATE_PLAYING) { //正在播放的时候
            audioplayer!!.pause()
            isPause = true
        }
    }

    /**
     * @param
     * @description 重新播放
     */
    fun resume() {
        if (mMediaPlayer != null && isPause) {
            mMediaPlayer!!.start()
            isPause = false
        }
        if (audioplayer != null && isPause) {
            audioplayer!!.play()
            isPause = false
        }
    }

    /**
     * @param
     * @description 是否在播放
     */
    val isPlaying: Boolean
        get() = if (mMediaPlayer != null) {
            mMediaPlayer!!.isPlaying
        }else if (audioplayer != null) {
            audioplayer!!.playState==AudioTrack.PLAYSTATE_PLAYING
    } else false

    /**
     * @param
     * @description 停止操作
     */
    fun stop() {
        if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.stop()
        }
        if (audioplayer != null && audioplayer!!.playState==AudioTrack.PLAYSTATE_PLAYING) {
            audioplayer!!.stop()
        }
    }

    /**
     * @param
     * @description 释放操作
     */
    fun release() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
        if (audioplayer != null) {
            audioplayer!!.stop()
            audioplayer!!.release()
            audioplayer = null
        }
    }
}