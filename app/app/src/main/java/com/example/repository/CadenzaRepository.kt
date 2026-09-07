package com.example.repository

import com.example.models.CadenzaAnalysisResponse
import com.example.models.CadenzaCompareResponse
import com.example.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class CadenzaRepository {

    suspend fun analyzeAudioFile(audioFile: File): Result<CadenzaAnalysisResponse> {
        return try {
            if (!audioFile.exists()) {
                return Result.failure(
                    Exception("Audio file does not exist: ${audioFile.absolutePath}")
                )
            }

            if (audioFile.length() == 0L) {
                return Result.failure(
                    Exception("Audio file is empty.")
                )
            }

            val mimeType = getMimeType(audioFile)

            val requestBody = audioFile.asRequestBody(
                mimeType.toMediaTypeOrNull()
            )

            val multipartFile = MultipartBody.Part.createFormData(
                name = "file",
                filename = audioFile.name,
                body = requestBody
            )

            val response = RetrofitClient.cadenzaService.analyzeMobileAudio(
                file = multipartFile
            )

            if (response.isSuccessful) {
                val responseBody = response.body()

                if (responseBody != null) {
                    Result.success(responseBody)
                } else {
                    Result.failure(
                        Exception("Backend returned an empty response.")
                    )
                }
            } else {
                val errorMessage = response.errorBody()?.string()
                    ?: "Unknown backend error"

                Result.failure(
                    Exception("Backend error ${response.code()}: $errorMessage")
                )
            }

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun uploadReferenceAudio(audioFile: File): Result<com.example.models.UploadReferenceResponse> {
        return try {
            if (!audioFile.exists()) return Result.failure(Exception("File not found"))
            
            val mimeType = getMimeType(audioFile)
            val requestBody = audioFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val multipartFile = MultipartBody.Part.createFormData("reference", audioFile.name, requestBody)

            val response = RetrofitClient.cadenzaService.uploadReference(multipartFile)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Backend error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun comparePerformance(sessionId: String, audioFile: File): Result<CadenzaCompareResponse> {
        return try {
            if (!audioFile.exists()) return Result.failure(Exception("File not found"))
            
            val mimeType = getMimeType(audioFile)
            val requestBody = audioFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val multipartFile = MultipartBody.Part.createFormData("user_recording", audioFile.name, requestBody)
            
            val sessionIdBody = sessionId.toRequestBody(okhttp3.MultipartBody.FORM)

            val response = RetrofitClient.cadenzaService.compareVocals(sessionIdBody, multipartFile)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Backend error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            else -> "application/octet-stream"
        }
    }
}