package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread

class CadenzaAudioRecorder(
    private val context: Context
) {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false

    private var currentOutputFile: File? = null

    fun hasRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startRecording(): Result<File> {
        return try {
            if (!hasRecordingPermission()) {
                return Result.failure(
                    Exception("Microphone permission is not granted.")
                )
            }

            if (isRecording) {
                return Result.failure(
                    Exception("Recording is already in progress.")
                )
            }

            val outputFile = createOutputFile()
            currentOutputFile = outputFile

            val minimumBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                channelConfig,
                audioFormat
            )

            if (minimumBufferSize == AudioRecord.ERROR ||
                minimumBufferSize == AudioRecord.ERROR_BAD_VALUE
            ) {
                return Result.failure(
                    Exception("Invalid audio recording buffer size.")
                )
            }

            val bufferSize = minimumBufferSize * 2

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return Result.failure(
                    Exception("Audio recorder could not be initialized.")
                )
            }

            isRecording = true

            audioRecord?.startRecording()

            recordingThread = thread(start = true) {
                writeWavAudioDataToFile(
                    outputFile = outputFile,
                    bufferSize = bufferSize
                )
            }

            Result.success(outputFile)

        } catch (exception: Exception) {
            isRecording = false
            Result.failure(exception)
        }
    }

    fun stopRecording(): Result<File> {
        return try {
            if (!isRecording) {
                return Result.failure(
                    Exception("Recording is not currently active.")
                )
            }

            isRecording = false

            try {
                audioRecord?.stop()
            } catch (_: Exception) {
            }

            try {
                audioRecord?.release()
            } catch (_: Exception) {
            }

            audioRecord = null

            recordingThread?.join()
            recordingThread = null

            val outputFile = currentOutputFile

            if (outputFile == null || !outputFile.exists()) {
                return Result.failure(
                    Exception("Recorded audio file was not created.")
                )
            }

            updateWavHeader(outputFile)

            Result.success(outputFile)

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun isCurrentlyRecording(): Boolean {
        return isRecording
    }

    private fun createOutputFile(): File {
        val recordingsFolder = File(
            context.cacheDir,
            "cadenza_recordings"
        )

        if (!recordingsFolder.exists()) {
            recordingsFolder.mkdirs()
        }

        val fileName = "cadenza_recording_${System.currentTimeMillis()}.wav"

        return File(recordingsFolder, fileName)
    }

    private fun writeWavAudioDataToFile(
        outputFile: File,
        bufferSize: Int
    ) {
        val audioData = ByteArray(bufferSize)

        try {
            FileOutputStream(outputFile).use { fileOutputStream ->

                writeEmptyWavHeader(fileOutputStream)

                while (isRecording) {
                    val readBytes = audioRecord?.read(
                        audioData,
                        0,
                        audioData.size
                    ) ?: 0

                    if (readBytes > 0) {
                        fileOutputStream.write(
                            audioData,
                            0,
                            readBytes
                        )
                    }
                }
            }
        } catch (_: IOException) {
        }
    }

    private fun writeEmptyWavHeader(fileOutputStream: FileOutputStream) {
        val emptyHeader = ByteArray(44)
        fileOutputStream.write(emptyHeader)
    }

    private fun updateWavHeader(wavFile: File) {
        val totalAudioLen = wavFile.length() - 44
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 16 / 8

        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        writeIntToHeader(header, 4, totalDataLen.toInt())

        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        writeIntToHeader(header, 16, 16)

        writeShortToHeader(header, 20, 1.toShort())
        writeShortToHeader(header, 22, channels.toShort())

        writeIntToHeader(header, 24, sampleRate)
        writeIntToHeader(header, 28, byteRate)

        writeShortToHeader(header, 32, (channels * 16 / 8).toShort())
        writeShortToHeader(header, 34, 16.toShort())

        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        writeIntToHeader(header, 40, totalAudioLen.toInt())

        // Fix: Use RandomAccessFile to write header without truncating the file
        java.io.RandomAccessFile(wavFile, "rw").use { raf ->
            raf.seek(0)
            raf.write(header)
        }
    }

    private fun writeIntToHeader(
        header: ByteArray,
        offset: Int,
        value: Int
    ) {
        header[offset] = (value and 0xff).toByte()
        header[offset + 1] = ((value shr 8) and 0xff).toByte()
        header[offset + 2] = ((value shr 16) and 0xff).toByte()
        header[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    private fun writeShortToHeader(
        header: ByteArray,
        offset: Int,
        value: Short
    ) {
        header[offset] = (value.toInt() and 0xff).toByte()
        header[offset + 1] = ((value.toInt() shr 8) and 0xff).toByte()
    }
}