package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.model.ChatMessageRecord
import com.example.data.model.FarmerProfile
import com.example.util.AppLanguage
import com.example.util.IndianSoilType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiRepository"
private const val GEMINI_MODEL = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

class GeminiAiRepository(private val context: Context) : AiRepository {

  override val isLiveModelConfigured: Boolean = true
  override val statusLabel: String = "Firebase AI Logic ($GEMINI_MODEL)"

  private val chatModel by lazy {
    Firebase.ai.generativeModel(modelName = GEMINI_MODEL)
  }

  private val soilSchema = Schema.obj(
    properties = mapOf(
      "predictedCategory" to Schema.enumeration(
        values = listOf("BLACK", "RED", "ALLUVIAL", "LATERITE", "ARID", "MOUNTAIN"),
        description = "Closest matching Indian soil category key",
      ),
      "confidenceLabel" to Schema.string(
        description = "Short confidence assessment and visual/input basis",
      ),
      "suitabilitySummary" to Schema.string(
        description = "Clear summary of fertility, drainage, pH, and moisture status",
      ),
      "cropRecommendations" to Schema.array(
        items = Schema.string(),
        description = "4 to 6 suitable Indian crops for this soil",
      ),
      "improvementSuggestions" to Schema.array(
        items = Schema.string(),
        description = "3 to 5 practical soil improvement recommendations",
      ),
    ),
  )

  private val soilModel by lazy {
    Firebase.ai.generativeModel(
      modelName = GEMINI_MODEL,
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = soilSchema
      },
    )
  }

  private val diseaseSchema = Schema.obj(
    properties = mapOf(
      "predictedDisease" to Schema.string(
        description = "Most likely crop disease, pest, or stress condition name",
      ),
      "confidenceLabel" to Schema.string(
        description = "Preliminary confidence level and diagnostic basis",
      ),
      "symptomsAndCauses" to Schema.string(
        description = "Observed symptoms and likely fungal/bacterial/viral/nutrient cause",
      ),
      "preventionGuidance" to Schema.string(
        description = "Preventive field hygiene and cultural practices",
      ),
      "treatmentInformation" to Schema.string(
        description = "General IPM and agronomic treatment guidance with KVK confirmation advice",
      ),
    ),
  )

  private val diseaseModel by lazy {
    Firebase.ai.generativeModel(
      modelName = GEMINI_MODEL,
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = diseaseSchema
      },
    )
  }

  override fun askAssistantStream(
    question: String,
    history: List<ChatMessageRecord>,
    language: AppLanguage,
    farmer: FarmerProfile?,
    weatherSummary: String?,
  ): Flow<AssistantStreamEvent> = flow {
    val systemContext = buildString {
      append("You are AgriSense AI Mitra, a practical, empathetic digital farming advisor for Indian farmers. ")
      append("Respond strictly in ${languageName(language)} using simple, clear farmer-friendly wording. ")
      if (farmer != null) {
        append("Farmer context: Name=${farmer.fullName}, Location=${farmer.village}, ${farmer.district}, ${farmer.state}, ")
        append("FarmSize=${farmer.farmSizeAcres} acres, Soil=${farmer.soilType}, MainCrops=${farmer.mainCrops}. ")
      }
      if (!weatherSummary.isNullOrBlank()) {
        append("Current local weather: $weatherSummary. ")
      }
      append("Provide actionable agronomic advice tailored to Indian Kharif/Rabi/Zaid seasons. ")
      append("Do not invent live market prices. ")
      append("At the very end on a new line, include exactly 3 short follow-up questions in ${languageName(language)} formatted as: FOLLOWUPS: q1 || q2 || q3")
    }

    val recentHistory: List<Content> = history
      .takeLast(6)
      .filter { it.message.isNotBlank() }
      .map { msg ->
        content(role = if (msg.role == "user") "user" else "model") {
          text(msg.message)
        }
      }

    val fullPrompt = "$systemContext\n\nFarmer question: $question"
    val accumulated = StringBuilder()

    try {
      withTimeout(AI_TIMEOUT_MS) {
        val chat = chatModel.startChat(history = recentHistory)
        chat.sendMessageStream(fullPrompt).collect { chunk ->
          val part = chunk.text.orEmpty()
          if (part.isNotEmpty()) {
            accumulated.append(part)
            val displayText = accumulated.toString().substringBefore("FOLLOWUPS:").trim()
            emit(AssistantStreamEvent.PartialText(displayText))
          }
        }
      }

      val rawText = accumulated.toString().trim()
      if (rawText.isBlank()) {
        throw IllegalStateException("Gemini returned an empty response.")
      }

      val answer = rawText.substringBefore("FOLLOWUPS:").trim()
      val followUpsPart = rawText.substringAfter("FOLLOWUPS:", "").trim()
      val parsedFollowUps = followUpsPart
        .split("||")
        .map { it.trim().trimStart('-', '*', '•', ' ') }
        .filter { it.isNotBlank() }
        .take(3)
        .ifEmpty { defaultFollowUps(language) }

      emit(
        AssistantStreamEvent.Completed(
          AiAssistantResponse(
            answer = answer,
            followUpSuggestions = parsedFollowUps,
            isLiveAi = true,
            providerNotice = "Powered by Gemini ($GEMINI_MODEL) via Firebase AI Logic",
          )
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Assistant generation timed out", e)
      throw IllegalStateException("AI response timed out after 60 seconds. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Assistant generation failed", e)
      throw IllegalStateException(e.localizedMessage ?: "Failed to generate AI guidance.", e)
    }
  }

  override suspend fun analyzeSoil(
    imageUri: String?,
    soilHint: IndianSoilType,
    phValue: Double?,
    moistureNote: String,
    language: AppLanguage,
  ): SoilAnalysisResult {
    val bitmap = loadScaledBitmap(imageUri)
    val prompt = buildString {
      append("Analyze this Indian agricultural soil sample and respond strictly in ${languageName(language)} for the text fields. ")
      append("For predictedCategory, pick the closest enum from [BLACK, RED, ALLUVIAL, LATERITE, ARID, MOUNTAIN]. ")
      append("Farmer hint: ${soilHint.labelEn}. ")
      append("Measured pH: ${phValue?.toString() ?: "not provided"}. ")
      append("Observed moisture/texture: ${moistureNote.ifBlank { "not specified" }}. ")
      if (bitmap == null) {
        append("No photo attached; evaluate carefully using the provided pH, moisture, and soil category hint and note that in confidenceLabel.")
      } else {
        append("Inspect the attached soil photograph for color, texture, organic matter, and clod/crust clues alongside pH and moisture.")
      }
    }

    try {
      val response = withTimeout(AI_TIMEOUT_MS) {
        soilModel.generateContent(
          content {
            if (bitmap != null) image(bitmap)
            text(prompt)
          }
        )
      }
      val jsonText = response.text ?: throw IllegalStateException("Gemini returned an empty soil analysis response.")
      val json = JSONObject(jsonText)
      val catRaw = json.optString("predictedCategory", soilHint.name)
      val predictedCat = IndianSoilType.entries.firstOrNull { it.name.equals(catRaw, ignoreCase = true) } ?: soilHint
      val cropsArray = json.optJSONArray("cropRecommendations")
      val crops = (0 until (cropsArray?.length() ?: 0)).mapNotNull { i ->
        cropsArray?.optString(i)?.takeIf { it.isNotBlank() }
      }
      val tipsArray = json.optJSONArray("improvementSuggestions")
      val tips = (0 until (tipsArray?.length() ?: 0)).mapNotNull { i ->
        tipsArray?.optString(i)?.takeIf { it.isNotBlank() }
      }

      return SoilAnalysisResult(
        predictedCategory = predictedCat,
        confidenceLabel = json.optString("confidenceLabel", "Analyzed by Gemini AI"),
        suitabilitySummary = json.optString("suitabilitySummary", ""),
        cropRecommendations = crops.ifEmpty { listOf("Soybean", "Wheat", "Chickpea") },
        improvementSuggestions = tips.ifEmpty { listOf("Add well-decomposed farmyard manure and test NPK at your local soil lab.") },
        isLiveAi = true,
        providerNotice = "Powered by Gemini ($GEMINI_MODEL) via Firebase AI Logic",
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Soil analysis timed out", e)
      throw IllegalStateException("Soil analysis timed out after 60 seconds. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Soil analysis failed", e)
      throw IllegalStateException(e.localizedMessage ?: "Failed to analyze soil with Firebase AI Logic.", e)
    }
  }

  override suspend fun detectCropDisease(
    imageUri: String?,
    cropName: String,
    symptomsNote: String,
    language: AppLanguage,
  ): DiseaseDetectionResult {
    val bitmap = loadScaledBitmap(imageUri)
    val prompt = buildString {
      append("Perform a preliminary agricultural disease assessment for Indian crop '$cropName'. ")
      append("Respond strictly in ${languageName(language)}. ")
      append("Farmer-reported symptoms: ${symptomsNote.ifBlank { "none typed" }}. ")
      if (bitmap == null) {
        append("No leaf photo was provided; base assessment on the crop and symptoms and clearly note this limitation in confidenceLabel.")
      } else {
        append("Inspect the attached crop/leaf photo closely for lesions, chlorosis, rust pustules, curling, or fungal/pest damage.")
      }
      append(" Include balanced IPM prevention and general treatment guidance and advise confirming with a Krishi Vigyan Kendra (KVK) before spraying.")
    }

    try {
      val response = withTimeout(AI_TIMEOUT_MS) {
        diseaseModel.generateContent(
          content {
            if (bitmap != null) image(bitmap)
            text(prompt)
          }
        )
      }
      val jsonText = response.text ?: throw IllegalStateException("Gemini returned an empty disease assessment response.")
      val json = JSONObject(jsonText)

      return DiseaseDetectionResult(
        predictedDisease = json.optString("predictedDisease", "Preliminary Stress / Disease Assessment"),
        confidenceLabel = json.optString("confidenceLabel", "Preliminary Gemini Vision Assessment"),
        symptomsAndCauses = json.optString("symptomsAndCauses", ""),
        preventionGuidance = json.optString("preventionGuidance", ""),
        treatmentInformation = json.optString("treatmentInformation", ""),
        isLiveAi = true,
        providerNotice = "Powered by Gemini ($GEMINI_MODEL) via Firebase AI Logic",
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Disease detection timed out", e)
      throw IllegalStateException("Disease detection timed out after 60 seconds. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Disease detection failed", e)
      throw IllegalStateException(e.localizedMessage ?: "Failed to run disease detection with Firebase AI Logic.", e)
    }
  }

  private suspend fun loadScaledBitmap(imageUriString: String?): Bitmap? = withContext(Dispatchers.IO) {
    if (imageUriString.isNullOrBlank()) return@withContext null
    try {
      val uri = Uri.parse(imageUriString)
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
      }
      val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
      var sampleSize = 1
      while (maxDim > 0 && maxDim / sampleSize > 1024) {
        sampleSize *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOpts)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not decode image URI for multimodal inference: $imageUriString", e)
      null
    }
  }

  private fun languageName(language: AppLanguage): String = when (language) {
    AppLanguage.EN -> "English"
    AppLanguage.HI -> "Hindi (Devanagari script)"
    AppLanguage.MR -> "Marathi (Devanagari script)"
  }

  private fun defaultFollowUps(language: AppLanguage): List<String> = when (language) {
    AppLanguage.HI -> listOf(
      "मेरी फसल के लिए सही खाद की मात्रा क्या है?",
      "सिंचाई का सही समय कैसे तय करें?",
      "रोग से बचाव के जैविक उपाय बताएं",
    )
    AppLanguage.MR -> listOf(
      "माझ्या पिकासाठी खताचे योग्य प्रमाण किती असावे?",
      "शेताला पाणी कधी द्यावे?",
      "रोग नियंत्रणासाठी सेंद्रिय उपाय सांगा",
    )
    AppLanguage.EN -> listOf(
      "What fertilizer schedule fits this crop stage?",
      "When should I irrigate next?",
      "What preventive IPM steps should I take?",
    )
  }
}
