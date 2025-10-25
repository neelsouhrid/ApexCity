package com.example.apexcity.data.api

import com.example.apexcity.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth endpoints
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/google")
    suspend fun googleSignIn(@Body request: GoogleSignInRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<User>

    // Complaint endpoints
    @Multipart
    @POST("complaints")
    suspend fun createComplaint(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("location") location: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Response<Complaint>

    @GET("complaints/my-complaints")
    suspend fun getMyComplaints(): Response<List<Complaint>>

    @GET("complaints/{id}")
    suspend fun getComplaint(@Path("id") id: String): Response<Complaint>

    @GET("complaints/resolved")
    suspend fun getResolvedComplaints(): Response<List<Complaint>>

    @GET("complaints/stats")
    suspend fun getDashboardStats(): Response<Stats>

    // Chatbot
    @POST("chatbot")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>
}