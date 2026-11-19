package com.example.data.ai

import com.example.data.model.ChatMessageRecord
import com.example.data.model.FarmerProfile
import com.example.util.AppLanguage
import com.example.util.IndianSoilType
import kotlinx.coroutines.flow.Flow

data class AiAssistantResponse(
  val answer: String,
  val followUpSuggestions: List<String>,
  val isLiveAi: Boolean = true,
  val providerNotice: String,
)

data class SoilAnalysisResult(
  val predictedCategory: IndianSoilType,
  val confidenceLabel: String,
  val suitabilitySummary: String,
  val cropRecommendations: List<String>,
  val improvementSuggestions: List<String>,
  val isLiveAi: Boolean = true,
  val providerNotice: String,
)

data class DiseaseDetectionResult(
  val predictedDisease: String,
  val confidenceLabel: String,
  val symptomsAndCauses: String,
  val preventionGuidance: String,
  val treatmentInformation: String,
  val isLiveAi: Boolean = true,
  val providerNotice: String,
)

sealed interface AssistantStreamEvent {
  data class PartialText(val text: String) : AssistantStreamEvent
  data class Completed(val response: AiAssistantResponse) : AssistantStreamEvent
}

interface AiRepository {
  val isLiveModelConfigured: Boolean
  val statusLabel: String

  fun askAssistantStream(
    question: String,
    history: List<ChatMessageRecord>,
    language: AppLanguage,
    farmer: FarmerProfile?,
    weatherSummary: String?,
  ): Flow<AssistantStreamEvent>

  suspend fun analyzeSoil(
    imageUri: String?,
    soilHint: IndianSoilType,
    phValue: Double?,
    moistureNote: String,
    language: AppLanguage,
  ): SoilAnalysisResult

  suspend fun detectCropDisease(
    imageUri: String?,
    cropName: String,
    symptomsNote: String,
    language: AppLanguage,
  ): DiseaseDetectionResult
}
