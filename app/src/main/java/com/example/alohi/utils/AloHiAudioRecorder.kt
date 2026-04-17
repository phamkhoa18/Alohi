package com.example.alohi.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AloHiAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0

    fun startRecording(): File? {
        val fileName = "alohi_record_${System.currentTimeMillis()}.m4a"
        outputFile = File(context.cacheDir, fileName)

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
                startTime = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return outputFile
    }

    fun stopRecording(): Long {
        val duration = System.currentTimeMillis() - startTime
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        return duration
    }

    fun cancelRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            outputFile?.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        outputFile = null
    }
}
