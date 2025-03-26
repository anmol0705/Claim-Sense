package android.saswat.claimsense.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "RiskViewModel"

data class RiskRecord(
    val timestamp: Long = 0L,
    val risk: Float = 0f,
    val formattedDate: String = "",
    val avgRisk: Float? = null,
    val maxRisk: Float? = null,
    val minRisk: Float? = null,
    val sampleCount: Int? = null
)

data class RiskSummary(
    val averageRisk: Float = 0f,
    val maxRisk: Float = 0f,
    val recordCount: Int = 0,
    val lastRecordDate: String = "",
    val riskTrend: Float = 0f  // Positive: increasing risk, Negative: decreasing risk
)

sealed class RiskViewState {
    object Initial : RiskViewState()
    object Loading : RiskViewState()
    data class Success(
        val riskHistory: List<RiskRecord>,
        val summary: RiskSummary,
        val dailyAverages: Map<String, Float>
    ) : RiskViewState()
    data class Error(val message: String) : RiskViewState()
}

class RiskViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _riskViewState = MutableStateFlow<RiskViewState>(RiskViewState.Initial)
    val riskViewState: StateFlow<RiskViewState> = _riskViewState

    init {
        // Check if user is already signed in and fetch their risk data
        auth.currentUser?.let { user ->
            fetchRiskData()
        }
    }

    fun fetchRiskData(timeRange: TimeRange = TimeRange.WEEK) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _riskViewState.value = RiskViewState.Error("User not authenticated")
            return
        }

        _riskViewState.value = RiskViewState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val thresholdTime = calculateThresholdTime(timeRange)

                val riskSnapshot = firestore.collection("risk_scores")
                    .document(userId)
                    .collection("transactions")
                    .whereGreaterThan("timestamp", thresholdTime)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1000)  // Reasonable limit
                    .get()
                    .await()

                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                val riskRecords = riskSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null

                    val timestamp = (data["timestamp"] as? Number)?.toLong() ?: return@mapNotNull null
                    val risk = (data["risk"] as? Number)?.toFloat()
                    val avgRisk = (data["avgRisk"] as? Number)?.toFloat()
                    val maxRisk = (data["maxRisk"] as? Number)?.toFloat()
                    val minRisk = (data["minRisk"] as? Number)?.toFloat()
                    val sampleCount = (data["sampleCount"] as? Number)?.toInt()

                    // If no risk values are available, skip this record
                    if (risk == null && avgRisk == null) return@mapNotNull null

                    RiskRecord(
                        timestamp = timestamp,
                        risk = risk ?: (avgRisk ?: 0f),  // Fallback to avgRisk if risk is null
                        formattedDate = dateFormat.format(Date(timestamp)),
                        avgRisk = avgRisk,
                        maxRisk = maxRisk,
                        minRisk = minRisk,
                        sampleCount = sampleCount
                    )
                }

                if (riskRecords.isEmpty()) {
                    _riskViewState.value = RiskViewState.Error("No risk data available for this time period")
                    return@launch
                }

                // Calculate risk summary
                val riskSummary = calculateRiskSummary(riskRecords)

                // Calculate daily averages for charts
                val dailyAverages = calculateDailyAverages(riskRecords)

                _riskViewState.value = RiskViewState.Success(
                    riskHistory = riskRecords,
                    summary = riskSummary,
                    dailyAverages = dailyAverages
                )

                Log.d(TAG, "Fetched ${riskRecords.size} risk records")

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching risk data", e)
                _riskViewState.value = RiskViewState.Error(e.message ?: "Failed to fetch risk data")
            }
        }
    }

    fun fetchRiskDataForUser(userId: String, timeRange: TimeRange = TimeRange.WEEK) {
        if (userId.isBlank()) {
            _riskViewState.value = RiskViewState.Error("Invalid user ID")
            return
        }

        _riskViewState.value = RiskViewState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val thresholdTime = calculateThresholdTime(timeRange)

                val riskSnapshot = firestore.collection("risk_scores")
                    .document(userId)
                    .collection("transactions")
                    .whereGreaterThan("timestamp", thresholdTime)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1000)
                    .get()
                    .await()

                // Process data same as above
                // ... (same code as in fetchRiskData)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching risk data for user $userId", e)
                _riskViewState.value = RiskViewState.Error(e.message ?: "Failed to fetch risk data")
            }
        }
    }

    fun fetchRiskDataForDateRange(startDate: Long, endDate: Long) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _riskViewState.value = RiskViewState.Error("User not authenticated")
            return
        }

        _riskViewState.value = RiskViewState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val riskSnapshot = firestore.collection("risk_scores")
                    .document(userId)
                    .collection("transactions")
                    .whereGreaterThanOrEqualTo("timestamp", startDate)
                    .whereLessThanOrEqualTo("timestamp", endDate)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                // Process data same as above
                // ... (similar code as in fetchRiskData)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching risk data for date range", e)
                _riskViewState.value = RiskViewState.Error(e.message ?: "Failed to fetch risk data")
            }
        }
    }

    private fun calculateRiskSummary(riskRecords: List<RiskRecord>): RiskSummary {
        if (riskRecords.isEmpty()) return RiskSummary()

        // Calculate average risk
        val riskValues = riskRecords.map {
            it.avgRisk ?: it.risk
        }
        val averageRisk = riskValues.average().toFloat()

        // Find max risk
        val maxRisk = riskValues.maxOrNull() ?: 0f

        // Calculate trend (comparing first half vs second half of data)
        val midPoint = riskRecords.size / 2
        val riskTrend = if (riskRecords.size >= 4) {
            val recentHalf = riskRecords.subList(0, midPoint).map { it.avgRisk ?: it.risk }
            val olderHalf = riskRecords.subList(midPoint, riskRecords.size).map { it.avgRisk ?: it.risk }

            recentHalf.average().toFloat() - olderHalf.average().toFloat()
        } else {
            0f
        }

        // Format the last record date
        val lastRecordDate = if (riskRecords.isNotEmpty()) {
            riskRecords.first().formattedDate
        } else {
            "N/A"
        }

        return RiskSummary(
            averageRisk = averageRisk,
            maxRisk = maxRisk,
            recordCount = riskRecords.size,
            lastRecordDate = lastRecordDate,
            riskTrend = riskTrend
        )
    }

    private fun calculateDailyAverages(riskRecords: List<RiskRecord>): Map<String, Float> {
        val dailyData = mutableMapOf<String, MutableList<Float>>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Group risk records by day
        riskRecords.forEach { record ->
            val dayString = dateFormat.format(Date(record.timestamp))
            val riskValue = record.avgRisk ?: record.risk

            if (!dailyData.containsKey(dayString)) {
                dailyData[dayString] = mutableListOf()
            }
            dailyData[dayString]?.add(riskValue)
        }

        // Calculate average risk for each day
        return dailyData.mapValues { (_, values) -> values.average().toFloat() }
    }

    private fun calculateThresholdTime(timeRange: TimeRange): Long {
        val calendar = Calendar.getInstance()

        when (timeRange) {
            TimeRange.DAY -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            TimeRange.WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
            TimeRange.MONTH -> calendar.add(Calendar.MONTH, -1)
            TimeRange.YEAR -> calendar.add(Calendar.YEAR, -1)
        }

        return calendar.timeInMillis
    }

    enum class TimeRange {
        DAY, WEEK, MONTH, YEAR
    }
}