package android.saswat.claimsense.data.model

data class Claim(
    val id: String = "",
    val userId: String = "",
    val vehicleId: String = "",
    val description: String = "",
    val status: ClaimStatus = ClaimStatus.PENDING,
    val amount: Double = 0.0,
    val photos: List<String> = emptyList(), // URLs to photos
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ClaimStatus {
    PENDING,
    IN_REVIEW,
    APPROVED,
    DENIED
}