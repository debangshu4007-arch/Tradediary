package com.example.data

import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class FeedbackRepository {

    private val db = FirebaseFirestore.getInstance()

    data class FeedbackPayload(
        val rating: Int,
        val feedbackText: String,
        val userId: String,
        val timestamp: Any,
        val deviceInfo: String,
        val appVersion: String,
        val androidVersion: String,
        val deviceModel: String
    )

    suspend fun submitFeedback(rating: Int, feedbackText: String, userId: String): Result<Unit> {
        return try {
            val payload = FeedbackPayload(
                rating = rating,
                feedbackText = feedbackText,
                userId = userId,
                timestamp = FieldValue.serverTimestamp(),
                deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}",
                appVersion = "2.0",
                androidVersion = Build.VERSION.RELEASE,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            )
            db.collection("feedback").add(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
