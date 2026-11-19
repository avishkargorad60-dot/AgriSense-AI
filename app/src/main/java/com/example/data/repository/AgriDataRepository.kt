package com.example.data.repository

import com.example.BuildConfig
import com.example.data.ai.AiAssistantResponse
import com.example.data.ai.AiRepository
import com.example.data.ai.AssistantStreamEvent
import com.example.data.ai.DiseaseDetectionResult
import com.example.data.ai.SoilAnalysisResult
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ActivityLogRecord
import com.example.data.model.ChatMessageRecord
import com.example.data.model.CropRecommendationRecord
import com.example.data.model.DiseaseDetectionRecord
import com.example.data.model.FarmerProfile
import com.example.data.model.SavedCropRecord
import com.example.data.model.SoilAnalysisRecord
import com.example.data.model.WeatherAdvisoryRecord
import com.example.data.remote.AgmarknetApi
import com.example.data.remote.AgmarknetRecord
import com.example.data.remote.OpenMeteoApi
import com.example.util.AppLanguage
import com.example.util.IndianAgriData
import com.example.util.IndianCropProfile
import com.example.util.IndianSeason
import com.example.util.IndianSoilType
import com.example.util.IrrigationType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

data class DailyForecastUi(
  val date: String,
  val tempMaxC: Double,
  val tempMinC: Double,
  val rainMm: Double,
  val rainProbPct: Int,
  val weatherCode: Int,
)

data class LiveWeatherUi(
  val locationName: String,
  val latitude: Double,
  val longitude: Double,
  val tempC: Double,
  val humidityPct: Int,
  val rainMm: Double,
  val windKmh: Double,
  val weatherCode: Int,
  val dailyForecast: List<DailyForecastUi>,
  val generatedAdvisories: List<String>,
  val alerts: List<String>,
  val fetchedAtMillis: Long = System.currentTimeMillis(),
)

sealed interface MarketPricesResult {
  data class Success(val records: List<AgmarknetRecord>, val source: String) : MarketPricesResult
  data class Unavailable(val reason: String, val missingKey: Boolean) : MarketPricesResult
}

data class CropScoreResult(
  val crop: IndianCropProfile,
  val score: Int,
  val reasons: List<String>,
  val soilCompatibility: String,
)

data class IrrigationAdviceResult(
  val recommendationTitle: String,
  val irrigationGuidance: String,
  val monitoringFrequency: String,
  val rainfallPrecautions: String,
  val conservationTips: List<String>,
  val precisionCaveat: String,
)

class AgriDataRepository(
  private val openMeteoApi: OpenMeteoApi,
  private val agmarknetApi: AgmarknetApi,
  private val aiRepository: AiRepository,
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  val isLiveAiConfigured: Boolean get() = aiRepository.isLiveModelConfigured
  val aiStatusLabel: String get() = aiRepository.statusLabel

  private fun requireUserId(): String =
    auth.currentUser?.uid?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun <T> observeUserSubcollection(
    userId: String,
    collectionName: String,
    clazz: Class<T>,
    ascending: Boolean = false,
    limit: Long = 100L,
  ): Flow<List<T>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
    val path = "users/$userId/$collectionName"
    val direction = if (ascending) Query.Direction.ASCENDING else Query.Direction.DESCENDING
    emitAll(
      db.collection("users").document(userId).collection(collectionName)
        .orderBy("createdAt", direction)
        .limit(limit)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeSoilHistory(userId: String): Flow<List<SoilAnalysisRecord>> =
    observeUserSubcollection(userId, "soil_analyses", SoilAnalysisRecord::class.java)

  fun observeDiseaseHistory(userId: String): Flow<List<DiseaseDetectionRecord>> =
    observeUserSubcollection(userId, "disease_detections", DiseaseDetectionRecord::class.java)

  fun observeCropRecHistory(userId: String): Flow<List<CropRecommendationRecord>> =
    observeUserSubcollection(userId, "crop_recommendations", CropRecommendationRecord::class.java)

  fun observeChatMessages(userId: String): Flow<List<ChatMessageRecord>> =
    observeUserSubcollection(userId, "chat_messages", ChatMessageRecord::class.java, ascending = true)

  fun observeWeatherAdvisories(userId: String): Flow<List<WeatherAdvisoryRecord>> =
    observeUserSubcollection(userId, "weather_advisories", WeatherAdvisoryRecord::class.java)

  fun observeSavedCrops(userId: String): Flow<List<SavedCropRecord>> =
    observeUserSubcollection(userId, "saved_crops", SavedCropRecord::class.java)

  fun observeActivityLogs(userId: String): Flow<List<ActivityLogRecord>> =
    observeUserSubcollection(userId, "activity_logs", ActivityLogRecord::class.java)

  fun observePlatformActivity(): Flow<List<ActivityLogRecord>> = flow {
    val uid = requireUserId()
    val colRef = db.collection("platform_activity")
    emitAll(
      colRef.orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(30L)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(ActivityLogRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
          }
          throw error
        }
    )
  }

  suspend fun fetchLiveWeather(
    userId: String,
    locationName: String,
    latitude: Double,
    longitude: Double,
    language: AppLanguage,
    saveAdvisory: Boolean = true,
  ): Result<LiveWeatherUi> {
    return try {
      val response = openMeteoApi.getForecast(latitude = latitude, longitude = longitude)
      val current = response.current ?: return Result.failure(IllegalStateException("Weather payload missing current conditions."))
      val daily = response.daily
      val days = mutableListOf<DailyForecastUi>()
      if (daily != null) {
        val count = listOf(
          daily.time.size,
          daily.temperature2mMax.size,
          daily.temperature2mMin.size,
          daily.precipitationSum.size,
          daily.precipitationProbabilityMax.size,
          daily.weatherCode.size,
        ).minOrNull() ?: 0
        for (i in 0 until count) {
          days.add(
            DailyForecastUi(
              date = daily.time[i],
              tempMaxC = daily.temperature2mMax[i],
              tempMinC = daily.temperature2mMin[i],
              rainMm = daily.precipitationSum[i],
              rainProbPct = daily.precipitationProbabilityMax[i],
              weatherCode = daily.weatherCode[i],
            )
          )
        }
      }

      val (advisories, alerts) = buildWeatherAdvisories(
        tempC = current.temperature2m,
        humidity = current.relativeHumidity2m,
        rainMm = current.rain,
        windKmh = current.windSpeed10m,
        forecast = days,
        language = language,
      )

      val ui = LiveWeatherUi(
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        tempC = current.temperature2m,
        humidityPct = current.relativeHumidity2m,
        rainMm = current.rain.coerceAtLeast(current.precipitation),
        windKmh = current.windSpeed10m,
        weatherCode = current.weatherCode,
        dailyForecast = days,
        generatedAdvisories = advisories,
        alerts = alerts,
      )

      if (saveAdvisory) {
        val uid = requireUserId()
        if (uid == userId) {
          val docRef = db.collection("users").document(uid).collection("weather_advisories").document()
          val payload = mapOf(
            "userId" to uid,
            "locationName" to locationName.take(160).ifBlank { "Farm Location" },
            "temperatureC" to ui.tempC.coerceIn(-20.0, 65.0),
            "humidityPct" to ui.humidityPct.toLong().coerceIn(0L, 100L),
            "windKmh" to ui.windKmh.coerceIn(0.0, 300.0),
            "rainfallMm" to ui.rainMm.coerceIn(0.0, 2000.0),
            "advisorySummary" to advisories.joinToString(" | ").take(2000),
            "createdAt" to FieldValue.serverTimestamp(),
          )
          try {
            docRef.set(payload).await()
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
          }
        }
      }
      Result.success(ui)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun buildWeatherAdvisories(
    tempC: Double,
    humidity: Int,
    rainMm: Double,
    windKmh: Double,
    forecast: List<DailyForecastUi>,
    language: AppLanguage,
  ): Pair<List<String>, List<String>> {
    val upcomingRainMm = forecast.take(3).sumOf { it.rainMm }
    val maxRainProb = forecast.take(3).maxOfOrNull { it.rainProbPct } ?: 0
    val advisories = mutableListOf<String>()
    val alerts = mutableListOf<String>()

    if (upcomingRainMm >= 15.0 || maxRainProb >= 65) {
      alerts.add(
        when (language) {
          AppLanguage.HI -> "अगले 3 दिनों में बारिश की संभावना ($maxRainProb%, ~${upcomingRainMm.roundToInt()} mm) — जल निकासी तैयार रखें।"
          AppLanguage.MR -> "पुढील ३ दिवसांत पावसाची शक्यता ($maxRainProb%, ~${upcomingRainMm.roundToInt()} mm) — निचरा सज्ज ठेवा."
          AppLanguage.EN -> "Rain alert in next 3 days ($maxRainProb% chance, ~${upcomingRainMm.roundToInt()} mm) — clear field drainage channels."
        }
      )
      advisories.add("Postpone scheduled irrigation and avoid fertilizer top-dressing until rain passes.")
    } else {
      advisories.add("Low near-term rainfall expected — plan light morning/evening irrigation as per crop stage.")
    }

    if (tempC >= 36.0) {
      alerts.add("High heat (${tempC.roundToInt()}°C) — apply straw mulching to reduce root-zone evaporation.")
      advisories.add("Avoid foliar chemical sprays during peak afternoon heat; spray before 9 AM or after 5 PM.")
    }

    if (windKmh >= 18.0) {
      alerts.add("Strong winds (${windKmh.roundToInt()} km/h) — pause foliar spraying to prevent spray drift.")
    } else {
      advisories.add("Wind speed (${windKmh.roundToInt()} km/h) is suitable for calm-morning field operations.")
    }

    if (humidity >= 80 && rainMm > 0.0) {
      advisories.add("High humidity ($humidity%) increases fungal leaf-spot pressure; scout lower canopy regularly.")
    }

    return advisories to alerts
  }

  suspend fun fetchMarketPrices(
    state: String?,
    district: String?,
    commodity: String?,
  ): MarketPricesResult {
    val apiKey = BuildConfig.DATA_GOV_API_KEY
    if (apiKey.isBlank() || apiKey == "MY_DATA_GOV_API_KEY") {
      return MarketPricesResult.Unavailable(
        reason = "Live Agmarknet market prices could not be retrieved because DATA_GOV_API_KEY is not configured in the AI Studio Secrets panel.",
        missingKey = true,
      )
    }

    return try {
      val response = agmarknetApi.getCommodityPrices(
        apiKey = apiKey,
        state = state?.takeIf { it.isNotBlank() },
        district = district?.takeIf { it.isNotBlank() },
        commodity = commodity?.takeIf { it.isNotBlank() },
      )
      if (response.records.isEmpty()) {
        MarketPricesResult.Unavailable(
          reason = "No Agmarknet mandi records matched your current filter. Try clearing district or commodity filters.",
          missingKey = false,
        )
      } else {
        MarketPricesResult.Success(
          records = response.records,
          source = "Official Agmarknet / data.gov.in Live Feed",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      MarketPricesResult.Unavailable(
        reason = "Could not retrieve latest live mandi prices: ${e.localizedMessage ?: "Network error"}",
        missingKey = false,
      )
    }
  }

  suspend fun askAiAssistantStream(
    userId: String,
    question: String,
    history: List<ChatMessageRecord>,
    language: AppLanguage,
    farmer: FarmerProfile?,
    weatherSummary: String?,
    onPartialText: (String) -> Unit,
  ): Result<AiAssistantResponse> {
    var docPath = "users/$userId/chat_messages"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      val cleanQ = question.trim().take(8000)
      require(cleanQ.isNotBlank()) { "Question cannot be empty." }

      val userMsgRef = db.collection("users").document(uid).collection("chat_messages").document()
      docPath = userMsgRef.path
      userMsgRef.set(
        mapOf(
          "userId" to uid,
          "role" to "user",
          "message" to cleanQ,
          "followUps" to "",
          "liveAi" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      var finalReply: AiAssistantResponse? = null
      aiRepository.askAssistantStream(cleanQ, history, language, farmer, weatherSummary).collect { event ->
        when (event) {
          is AssistantStreamEvent.PartialText -> onPartialText(event.text)
          is AssistantStreamEvent.Completed -> finalReply = event.response
        }
      }

      val reply = finalReply ?: throw IllegalStateException("AI stream ended without a final response.")
      val assistantMsgRef = db.collection("users").document(uid).collection("chat_messages").document()
      docPath = assistantMsgRef.path
      assistantMsgRef.set(
        mapOf(
          "userId" to uid,
          "role" to "assistant",
          "message" to reply.answer.take(8000).ifBlank { "No answer generated." },
          "followUps" to reply.followUpSuggestions.joinToString("||").take(1000),
          "liveAi" to reply.isLiveAi,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      recordActivity(uid, "AI Assistant", "Asked AI Mitra", cleanQ.take(80))
      Result.success(reply)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (e is FirebaseFirestoreException) {
        handleFirestoreError(e, OperationType.CREATE, docPath)
      }
      Result.failure(e)
    }
  }

  suspend fun clearChatHistory(userId: String): Result<Unit> {
    val path = "users/$userId/chat_messages"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
      val docs = db.collection("users").document(uid).collection("chat_messages").get().await()
      val batch = db.batch()
      docs.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun runSoilAnalysis(
    userId: String,
    imageUri: String?,
    soilHint: IndianSoilType,
    phValue: Double?,
    moistureNote: String,
    language: AppLanguage,
  ): Result<SoilAnalysisResult> {
    var docPath = "users/$userId/soil_analyses"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      val result = aiRepository.analyzeSoil(imageUri, soilHint, phValue, moistureNote, language)

      val docRef = db.collection("users").document(uid).collection("soil_analyses").document()
      docPath = docRef.path
      val cleanPh = phValue?.coerceIn(0.0, 14.0)
      val payload = mapOf(
        "userId" to uid,
        "imageUri" to imageUri?.take(600),
        "soilTypeInput" to soilHint.labelEn.take(100),
        "phValue" to cleanPh,
        "moistureNote" to moistureNote.take(200),
        "predictedCategory" to result.predictedCategory.labelEn.take(100),
        "confidenceNote" to result.confidenceLabel.take(400),
        "cropRecommendations" to result.cropRecommendations.joinToString(", ").take(800),
        "improvementTips" to result.improvementSuggestions.joinToString("\n").take(2500),
        "liveAi" to result.isLiveAi,
        "createdAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }

      docRef.set(payload).await()
      recordActivity(uid, "Soil Analysis", "Analyzed ${soilHint.labelEn}", result.cropRecommendations.take(3).joinToString(", "))
      Result.success(result)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (e is FirebaseFirestoreException) {
        handleFirestoreError(e, OperationType.CREATE, docPath)
      }
      Result.failure(e)
    }
  }

  suspend fun runDiseaseDetection(
    userId: String,
    imageUri: String?,
    cropName: String,
    symptomsNote: String,
    language: AppLanguage,
  ): Result<DiseaseDetectionResult> {
    var docPath = "users/$userId/disease_detections"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      val cleanCrop = cropName.trim().ifBlank { "Soybean" }.take(120)
      val result = aiRepository.detectCropDisease(imageUri, cleanCrop, symptomsNote, language)

      val docRef = db.collection("users").document(uid).collection("disease_detections").document()
      docPath = docRef.path
      val payload = mapOf(
        "userId" to uid,
        "imageUri" to imageUri?.take(600),
        "cropName" to cleanCrop,
        "symptomsNote" to symptomsNote.take(500),
        "predictedDisease" to result.predictedDisease.take(200),
        "confidenceNote" to result.confidenceLabel.take(400),
        "symptomsCauses" to result.symptomsAndCauses.take(2500),
        "preventionSteps" to result.preventionGuidance.take(2500),
        "treatmentGuidance" to result.treatmentInformation.take(2500),
        "liveAi" to result.isLiveAi,
        "createdAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }

      docRef.set(payload).await()
      recordActivity(uid, "Disease Detection", "Checked $cleanCrop", result.predictedDisease)
      Result.success(result)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (e is FirebaseFirestoreException) {
        handleFirestoreError(e, OperationType.CREATE, docPath)
      }
      Result.failure(e)
    }
  }

  suspend fun recommendCrops(
    userId: String,
    soilType: IndianSoilType,
    ph: Double?,
    temperatureC: Double,
    rainfallMm: Double,
    season: IndianSeason,
    state: String,
    district: String,
    irrigationType: IrrigationType,
    farmSizeAcres: Double,
    preferenceKeyword: String,
  ): Result<List<CropScoreResult>> {
    var docPath = "users/$userId/crop_recommendations"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      val results = IndianAgriData.crops.map { crop ->
        var score = 0
        val reasons = mutableListOf<String>()

        if (season in crop.seasons) {
          score += 30
          reasons.add("Matches ${season.labelEn} sowing window")
        } else {
          reasons.add("Off-season for ${season.labelEn} (preferred: ${crop.seasons.joinToString { it.name }})")
        }

        val soilCompat = when {
          crop.compatibleSoils.firstOrNull() == soilType -> {
            score += 25
            reasons.add("Primary match for ${soilType.labelEn}")
            "Optimal"
          }
          soilType in crop.compatibleSoils -> {
            score += 20
            reasons.add("Compatible with ${soilType.labelEn}")
            "Good"
          }
          else -> {
            score += 6
            "Low"
          }
        }

        if (temperatureC in crop.tempRangeC) {
          score += 20
          reasons.add("Temperature (${temperatureC.roundToInt()}°C) inside ideal band")
        } else {
          val diff = minOf(
            kotlin.math.abs(temperatureC - crop.tempRangeC.start),
            kotlin.math.abs(temperatureC - crop.tempRangeC.endInclusive)
          )
          score += (20 - (diff * 2).roundToInt()).coerceAtLeast(4)
        }

        val hasIrrigation = irrigationType != IrrigationType.RAINFED
        if (rainfallMm in crop.rainfallRangeMm) {
          score += 15
          reasons.add("Rainfall (${rainfallMm.roundToInt()} mm) fits water profile")
        } else if (hasIrrigation && !crop.requiresAssuredIrrigation) {
          score += 12
          reasons.add("Supported via ${irrigationType.labelEn}")
        } else if (hasIrrigation) {
          score += 11
          reasons.add("Requires supplemental ${irrigationType.labelEn}")
        } else if (!crop.requiresAssuredIrrigation && rainfallMm >= crop.rainfallRangeMm.start * 0.7) {
          score += 10
          reasons.add("Resilient under rainfed conditions")
        } else {
          score += 3
          reasons.add("Water deficit risk under rainfed setup")
        }

        if (ph != null) {
          if (ph in crop.phRange) {
            score += 10
            reasons.add("Soil pH ${"%.1f".format(ph)} is ideal")
          } else {
            score += 4
          }
        } else {
          score += 6
        }

        if (preferenceKeyword.isNotBlank() && crop.name.contains(preferenceKeyword.trim(), ignoreCase = true)) {
          score = (score + 5).coerceAtMost(100)
          reasons.add("Matches your crop preference")
        }

        CropScoreResult(
          crop = crop,
          score = score.coerceIn(10, 100),
          reasons = reasons,
          soilCompatibility = soilCompat,
        )
      }.sortedByDescending { it.score }.take(6)

      val top = results.firstOrNull()
      if (top != null) {
        val docRef = db.collection("users").document(uid).collection("crop_recommendations").document()
        docPath = docRef.path
        val payload = mapOf(
          "userId" to uid,
          "soilType" to soilType.labelEn.take(100),
          "ph" to ph?.coerceIn(0.0, 14.0),
          "temperatureC" to temperatureC.coerceIn(-20.0, 65.0),
          "rainfallMm" to rainfallMm.coerceIn(0.0, 10000.0),
          "season" to season.labelEn.take(80),
          "state" to state.take(80),
          "district" to district.take(80),
          "irrigationType" to irrigationType.labelEn.take(100),
          "farmSizeAcres" to farmSizeAcres.coerceIn(0.0, 100000.0),
          "topCropName" to top.crop.name.take(120),
          "topScore" to top.score.toLong().coerceIn(0L, 100L),
          "recommendedCropsSummary" to results.take(3).joinToString(", ") { "${it.crop.name} (${it.score}%)" }.take(800),
          "createdAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
        docRef.set(payload).await()
        recordActivity(uid, "Crop Advisor", "Generated ${season.name} Plan", "Top match: ${top.crop.name} (${top.score}%)")
      }
      Result.success(results)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  fun computeIrrigationAdvice(
    cropName: String,
    soilType: IndianSoilType,
    growthStage: String,
    tempC: Double,
    humidityPct: Int,
    next3DaysRainMm: Double,
    soilMoisturePct: Double?,
  ): IrrigationAdviceResult {
    val highRainComing = next3DaysRainMm >= 12.0
    val hotDry = tempC >= 35.0 && humidityPct < 45
    val criticalStage = growthStage.contains("Flowering", true) ||
      growthStage.contains("Pod", true) ||
      growthStage.contains("Grain", true)

    val title = when {
      highRainComing -> "Hold Irrigation — Significant Rainfall Forecast"
      soilMoisturePct != null && soilMoisturePct >= 65.0 -> "Delay Irrigation — Adequate Soil Moisture"
      criticalStage && hotDry -> "Prioritize Scheduled Light Irrigation (Critical Stage)"
      soilType == IndianSoilType.BLACK -> "Check Root-Zone Cracks Before Irrigating"
      else -> "Follow Standard Stage-Based Moisture Monitoring"
    }

    val guidance = buildString {
      append("For $cropName in ${soilType.labelEn} at the $growthStage stage: ")
      when {
        highRainComing ->
          append("With ~${next3DaysRainMm.roundToInt()} mm rain forecast over the next 3 days, skip irrigation and inspect field drainage bunds.")
        soilMoisturePct != null && soilMoisturePct < 35.0 ->
          append("Measured moisture (${soilMoisturePct.roundToInt()}%) is low; schedule irrigation during early morning or late evening.")
        soilMoisturePct != null ->
          append("Current moisture (${soilMoisturePct.roundToInt()}%) is moderate; re-evaluate before applying water.")
        criticalStage ->
          append("This reproductive stage is moisture-sensitive; ensure the top 15 cm root zone does not dry out.")
        else ->
          append("Test soil 10–15 cm deep by hand feel (ball test) before turning on pump or drip.")
      }
    }

    val frequency = when (soilType) {
      IndianSoilType.BLACK -> "Inspect every 4–6 days (high moisture retention; avoid over-saturation)."
      IndianSoilType.ALLUVIAL -> "Inspect every 3–5 days depending on crop canopy cover."
      IndianSoilType.RED, IndianSoilType.ARID -> "Inspect every 2–3 days; frequent light cycles outperform flood irrigation."
      IndianSoilType.LATERITE, IndianSoilType.MOUNTAIN -> "Inspect every 3–4 days with organic mulch cover."
    }

    val rainPrecaution = if (highRainComing) {
      "Keep surface drainage furrows clear, especially in Black Regur soil where waterlogging triggers root rot."
    } else {
      "Monitor 48-hour forecast updates before irrigating to avoid fertilizer leaching."
    }

    val caveat = if (soilMoisturePct == null) {
      "Note: Field soil moisture sensor reading was not provided. Guidance is qualitative and should be verified on-site before scheduling exact pump hours."
    } else {
      "Guidance incorporates your entered ${soilMoisturePct.roundToInt()}% moisture reading and live weather forecast; confirm root-zone depth across multiple field spots."
    }

    return IrrigationAdviceResult(
      recommendationTitle = title,
      irrigationGuidance = guidance,
      monitoringFrequency = frequency,
      rainfallPrecautions = rainPrecaution,
      conservationTips = listOf(
        "Use drip or micro-sprinklers to cut evaporation losses by 35–50%.",
        "Apply 5–7 cm straw or crop-residue mulch between rows.",
        "Irrigate between 6:00–9:00 AM or after 5:30 PM to minimize wind/heat loss.",
      ),
      precisionCaveat = caveat,
    )
  }

  suspend fun saveCrop(userId: String, cropName: String, season: String, notes: String): Result<Unit> {
    var docPath = "users/$userId/saved_crops"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      val cleanCrop = cropName.trim().ifBlank { "Crop" }.take(120)
      val docRef = db.collection("users").document(uid).collection("saved_crops").document()
      docPath = docRef.path
      docRef.set(
        mapOf(
          "userId" to uid,
          "cropName" to cleanCrop,
          "season" to season.take(80),
          "notes" to notes.take(500),
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      recordActivity(uid, "Saved Crops", "Saved $cleanCrop", "$season season")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  private suspend fun deleteSubcollectionDoc(userId: String, collectionName: String, recordId: String): Result<Unit> {
    val path = "users/$userId/$collectionName/$recordId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
      require(recordId.isNotBlank()) { "Record ID cannot be blank." }
      db.collection("users").document(uid).collection(collectionName).document(recordId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteSoilRecord(id: String, userId: String) = deleteSubcollectionDoc(userId, "soil_analyses", id)
  suspend fun deleteDiseaseRecord(id: String, userId: String) = deleteSubcollectionDoc(userId, "disease_detections", id)
  suspend fun deleteCropRecRecord(id: String, userId: String) = deleteSubcollectionDoc(userId, "crop_recommendations", id)
  suspend fun deleteWeatherRecord(id: String, userId: String) = deleteSubcollectionDoc(userId, "weather_advisories", id)
  suspend fun deleteSavedCrop(id: String, userId: String) = deleteSubcollectionDoc(userId, "saved_crops", id)
  suspend fun deleteActivityLog(id: String, userId: String) = deleteSubcollectionDoc(userId, "activity_logs", id)

  private suspend fun recordActivity(uid: String, module: String, actionTitle: String, details: String) {
    val payload = mapOf(
      "userId" to uid,
      "module" to module.take(80),
      "actionTitle" to actionTitle.take(160),
      "details" to details.take(500),
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      db.collection("users").document(uid).collection("activity_logs").document().set(payload).await()
      db.collection("platform_activity").document().set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/$uid/activity_logs")
    }
  }

  companion object {
    fun formatTimestampMillis(ts: Timestamp?): Long = (ts ?: Timestamp.now()).toDate().time
  }
}
