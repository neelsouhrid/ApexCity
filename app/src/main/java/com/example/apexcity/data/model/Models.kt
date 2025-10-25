package com.example.apexcity.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

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

// Issue Model (Simplified for Fragment Usage)
@Parcelize
data class Issue(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable

// Extension function to convert Complaint to Issue
fun Complaint.toIssue(): Issue {
    return Issue(
        id = this.id,
        title = this.title,
        category = this.category,
        description = this.description,
        location = this.location.address,
        imageUrl = this.images.firstOrNull()?.url ?: "",
        status = this.status,
        createdAt = parseDate(this.createdAt),
        userId = this.userId?.id ?: "",
        latitude = this.location.coordinates.lat,
        longitude = this.location.coordinates.lng
    )
}

// Helper function to parse date string to timestamp
private fun parseDate(dateString: String): Long {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        sdf.parse(dateString)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}