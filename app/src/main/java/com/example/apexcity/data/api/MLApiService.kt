package com.example.apexcity.data.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

// ML Response Model
data class MLResponse(
    val title: String,
    val description: String,
    val category: String
)

interface MLApiService {
    @Multipart
    @POST("api/process-image")
    suspend fun processImage(
        @Part image: MultipartBody.Part
    ): Response<MLResponse>
}

object MLApiClient {
    private const val BASE_URL = "https://rAdvirtua-apex-city-api.hf.space/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: MLApiService = retrofit.create(MLApiService::class.java)
}