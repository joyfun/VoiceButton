package com.lizhidan.voicebutton.utils

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.lizhidan.voicebutton.interfaces.MediaRecorderStateListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.log10


class RecordManager(
    //音频文件存储目录
    private val recordFileDir: String
) {

    //音频文件绝对存储路径
    var recordAbsoluteFileDir: String? = null
        private set

    //通过MediaRecorder实现录音功能
    private var mediaRecorder: MediaRecorder? = null

    private var audioRecorder: AudioRecord? = null
    private var bufferSize = 0
    private lateinit var buffer: ByteArray
    var fmt = "amr"
    var Db = 0
    var sampleRate= 16000

    // 最大录音时长,默认1000*60（一分钟）
    var maxRecordLength = 1000 * 60

    //录音控件初始化状态标志
    private var isPrepared = false

    //音量分级标准
    private val volumeBase: Long = 600
    private var mediaRecorderStateListener: MediaRecorderStateListener? = null
    fun setMediaRecorderStateListener(mediaRecorderStateListener: MediaRecorderStateListener?) {
        this.mediaRecorderStateListener = mediaRecorderStateListener
    }

    /**
     * 准备录音控件
     */
    @SuppressLint("MissingPermission")
    fun prepareAudio() {
        try {
            val dir = File(recordFileDir)
            if (!dir.exists()) {
                dir.mkdir()
            }
            //生成随机文件名
            val file =
                File(dir, UUID.randomUUID().toString() + "."+fmt)
            recordAbsoluteFileDir = file.absolutePath
            // 需要每次使用前重新构造，不然在调用setOutputFile()时会报重用异常
            mediaRecorder?.let {
                mediaRecorder!!.release()
                mediaRecorder = null
            }
            if(fmt.equals("amr")) {
                mediaRecorder = MediaRecorder()
                //设置输出文件
                mediaRecorder!!.setOutputFile(recordAbsoluteFileDir)
                mediaRecorder!!.setMaxDuration(maxRecordLength)
                //设置MediaRecorder的音频源为麦克风
                mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                //设置音频格式
                mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                //设置音频的格式为amr
                mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                mediaRecorder!!.setOnInfoListener { mr, what, extra ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        mediaRecorderStateListener?.run {
                            //到达最大录音时间
                            onReachMaxRecordTime(recordAbsoluteFileDir)
                        }
                    }
                }
                mediaRecorder!!.setOnErrorListener { mr, what, extra ->
                    mediaRecorderStateListener?.run {
                        //录音发生错误
                        onError(what, extra)
                    }
                }
                mediaRecorder!!.prepare()
                mediaRecorder!!.start()
                isPrepared = true

            }else{
                bufferSize = AudioRecord.getMinBufferSize(
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                audioRecorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize
                )
                audioRecorder!!.startRecording()
                // 用于读取的 buffer
                // 用于读取的 buffer
                buffer = ByteArray(bufferSize)
                isPrepared = true

            //准备结束,可以开始录音了
                Thread {
                    var fileOutputStream: FileOutputStream? = null
                    try {
                        fileOutputStream = FileOutputStream(recordAbsoluteFileDir)
                        if (fileOutputStream != null) {
                            while (isPrepared) {
                                val readStatus: Int = audioRecorder!!.read(buffer, 0, bufferSize)
                                if(readStatus>0) {
                                    var v = 0
                                    // 将 buffer 内容取出，进行平方和运算
                                    // 将 buffer 内容取出，进行平方和运算
                                    for (i in 0 until buffer.size) {
                                        // 这里没有做运算的优化，为了更加清晰的展示代码
                                        v += buffer[i] * buffer[i]
                                    }
                                    // 平方和除以数据总长度，得到音量大小。可以获取白噪声值，然后对实际采样进行标准化。
                                    // 如果想利用这个数值进行操作，建议用 sendMessage 将其抛出，在 Handler 里进行处理。
                                    // 平方和除以数据总长度，得到音量大小。可以获取白噪声值，然后对实际采样进行标准化。
                                    // 如果想利用这个数值进行操作，建议用 sendMessage 将其抛出，在 Handler 里进行处理。
                                    Db =
                                        (20 * Math.log10(v / readStatus.toDouble() / volumeBase)).toInt()
                                    Log.d("RecordManager", "run: readStatus=$readStatus DB:$Db")
                                    fileOutputStream.write(buffer)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e("RecordManager", "run: ", e)
                    } finally {
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }.start()

            }
            mediaRecorderStateListener?.run {
                wellPrepared()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    //正常录音结束释放资源
    fun release() {
        if(fmt.equals("amr")){
        mediaRecorder?.let {
            try {
                mediaRecorder!!.stop()
                mediaRecorder!!.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaRecorder = null
            isPrepared = false
        }}else{
            try {
                audioRecorder!!.stop()
                audioRecorder!!.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isPrepared = false
            audioRecorder = null
        }
    }

    //中途取消录音,删除音频文件
    fun cancel() {
        release()
        recordAbsoluteFileDir?.let {
            val file = File(it)
            file.delete()
            recordAbsoluteFileDir = null
        }
    }

    /**
     * 根据音量分级更新麦克状态
     */
    fun getVoiceLevel(maxLevel: Int): Int {
        if (isPrepared && mediaRecorder != null) {
            val ratio = mediaRecorder!!.maxAmplitude.toDouble() / volumeBase
            var db = 0.0 // 分贝
            if (ratio > 1) {
                db = 20 * log10(ratio)
                return if (db / 4 > maxLevel) {
                    maxLevel
                } else (db / 4).toInt()
            }
        }
        if (isPrepared && audioRecorder != null) {
            return if (Db / 4 > maxLevel) {
                maxLevel
            } else (Db / 4).toInt()
        }
        return 0
    }

}
