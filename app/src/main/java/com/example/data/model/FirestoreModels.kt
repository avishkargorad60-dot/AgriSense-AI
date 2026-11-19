package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FarmerProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val mobile: String = "",
  val email: String = "",
  val state: String = "Maharashtra",
  val district: String = "Pune",
  val village: String = "",
  val preferredLanguage: String = "en",
  val farmSizeAcres: Double = 2.0,
  val mainCrops: String = "Soybean, Wheat",
  val soilType: String = "Black Soil (Regur)",
  val role: String = "FARMER",
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SoilAnalysisRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val imageUri: String? = null,
  val soilTypeInput: String = "",
  val phValue: Double? = null,
  val moistureNote: String = "",
  val predictedCategory: String = "",
  val confidenceNote: String = "",
  val cropRecommendations: String = "",
  val improvementTips: String = "",
  val liveAi: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DiseaseDetectionRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val imageUri: String? = null,
  val cropName: String = "",
  val symptomsNote: String = "",
  val predictedDisease: String = "",
  val confidenceNote: String = "",
  val symptomsCauses: String = "",
  val preventionSteps: String = "",
  val treatmentGuidance: String = "",
  val liveAi: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CropRecommendationRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val soilType: String = "",
  val ph: Double? = null,
  val temperatureC: Double = 0.0,
  val rainfallMm: Double = 0.0,
  val season: String = "",
  val state: String = "",
  val district: String = "",
  val irrigationType: String = "",
  val farmSizeAcres: Double = 0.0,
  val topCropName: String = "",
  val topScore: Long = 0L,
  val recommendedCropsSummary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatMessageRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val role: String = "user",
  val message: String = "",
  val followUps: String = "",
  val liveAi: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class WeatherAdvisoryRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val locationName: String = "",
  val temperatureC: Double = 0.0,
  val humidityPct: Long = 0L,
  val windKmh: Double = 0.0,
  val rainfallMm: Double = 0.0,
  val advisorySummary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SavedCropRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val cropName: String = "",
  val season: String = "",
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ActivityLogRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val module: String = "",
  val actionTitle: String = "",
  val details: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)
