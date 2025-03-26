package android.saswat.claimsense.data.model

data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val driverLicense: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)