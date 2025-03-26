package android.saswat.claimsense.data.model

data class Vehicle(
    val id: String = "",
    val userId: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 0,
    val licensePlate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)