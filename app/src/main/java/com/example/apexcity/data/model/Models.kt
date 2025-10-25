package com.example.apexcity.data.model

import com.google.gson.annotations.SerializedName

// Auth Models
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class GoogleSignInRequest(
    val idToken: String
)

data class AuthResponse(
    @SerializedName("_id") val id: String,
    val name: String,
    val email: String,
    val role: String,
    val token: String
)

data class User(
    @SerializedName("_id") val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val role: String,
    val createdAt: String
)

// Complaint Models
data class Location(
    val address: String,
    val coordinates: Coordinates
)

data class Coordinates(
    val lat: Double,
    val lng: Double
)

data class ComplaintImage(
    val url: String,
    val publicId: String
)

data class Complaint(
    @SerializedName("_id") val id: String,
    val title: String,
    val description: String,
    val category: String,
    val location: Location,
    val images: List<ComplaintImage>,
    val status: String,
    val priority: String,
    val userId: User?,
    val adminNotes: String?,
    val createdAt: String,
    val updatedAt: String,
    val resolvedAt: String?
)

data class Stats(
    val totalComplaints: Int,
    val pending: Int,
    val inProgress: Int,
    val resolved: Int,
    val categoryCounts: List<CategoryCount>
)

data class CategoryCount(
    @SerializedName("_id") val category: String,
    val count: Int
)

// Chat Models
data class ChatRequest(
    val message: String,
    val userId: String
)

data class ChatResponse(
    val response: String
)