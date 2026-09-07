package com.example.network

import com.example.models.CadenzaAnalysisResponse
import com.example.models.UploadReferenceResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

import com.example.models.CadenzaCompareResponse

interface CadenzaApiService {

    @Multipart
    @POST("analyze-mobile")
    suspend fun analyzeMobileAudio(
        @Part file: MultipartBody.Part
    ): Response<CadenzaAnalysisResponse>

    @Multipart
    @POST("upload-reference")
    suspend fun uploadReference(
        @Part file: MultipartBody.Part
    ): Response<UploadReferenceResponse>

    @Multipart
    @POST("compare")
    suspend fun compareVocals(
        @Part("session_id") sessionId: okhttp3.RequestBody,
        @Part userRecording: MultipartBody.Part
    ): Response<CadenzaCompareResponse>
}