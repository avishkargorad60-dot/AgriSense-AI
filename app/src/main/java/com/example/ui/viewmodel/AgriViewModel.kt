package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.DiseaseDetectionResult
import com.example.data.ai.SoilAnalysisResult
import com.example.data.model.ActivityLogRecord
import com.example.data.model.ChatMessageRecord
import com.example.data.model.CropRecommendationRecord
import com.example.data.model.DiseaseDetectionRecord
import com.example.data.model.FarmerProfile
import com.example.data.model.SavedCropRecord
import com.example.data.model.SoilAnalysisRecord
import com.example.data.model.WeatherAdvisoryRecord
import com.example.data.repository.AgriDataRepository
import com.example.data.repository.CropScoreResult
import com.example.data.repository.IrrigationAdviceResult
import com.example.data.repository.LiveWeatherUi
import com.example.data.repository.MarketPricesResult
import com.example.util.AppLanguage
import com.example.util.IndianAgriData
import com.example.util.IndianSeason
import com.example.util.IndianSoilType
import com.example.util.IrrigationType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "AgriVM"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface FirestoreUiState<out T> {
  data object Loading : FirestoreUiState<Nothing>
  data class Success<T>(val data: T) : FirestoreUiState<T>
  data class Error(val message: String) : FirestoreUiState<Nothing>
}

fun <T> FirestoreUiState<List<T>>.dataOrEmpty(): List<T> =
  (this as? FirestoreUiState.Success)?.data ?: emptyList()

sealed interface WeatherUiState {
  data object Loading : WeatherUiState
  data class Success(val data: LiveWeatherUi) : WeatherUiState
  data class Error(val message: String) : WeatherUiState
}

sealed interface MarketUiState {
  data object Idle : MarketUiState
  data object Loading : MarketUiState
  data class Result(val data: MarketPricesResult) : MarketUiState
}

sealed interface AiTaskState<out T> {
  data object Idle : AiTaskState<Nothing>
  data object Loading : AiTaskState<Nothing>
  data class Success<T>(val data: T) : AiTaskState<T>
  data class Error(val message: String) : AiTaskState<Nothing>
}

class AgriViewModel(
  private val repository: AgriDataRepository,
  private val currentUserId: String,
  private val isAdminUser: Boolean,
) : ViewModel() {

  val isLiveAiConfigured: Boolean = repository.isLiveAiConfigured
  val aiStatusLabel: String = repository.aiStatusLabel

  private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
  val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

  private val _marketState = MutableStateFlow<MarketUiState>(MarketUiState.Idle)
  val marketState: StateFlow<MarketUiState> = _marketState.asStateFlow()

  private val _isAiThinking = MutableStateFlow(false)
  val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

  private val _streamingAssistantText = MutableStateFlow<String?>(null)
  val streamingAssistantText: StateFlow<String?> = _streamingAssistantText.asStateFlow()

  private val _chatError = MutableStateFlow<String?>(null)
  val chatError: StateFlow<String?> = _chatError.asStateFlow()

  private val _soilTaskState = MutableStateFlow<AiTaskState<SoilAnalysisResult>>(AiTaskState.Idle)
  val soilTaskState: StateFlow<AiTaskState<SoilAnalysisResult>> = _soilTaskState.asStateFlow()

  private val _diseaseTaskState = MutableStateFlow<AiTaskState<DiseaseDetectionResult>>(AiTaskState.Idle)
  val diseaseTaskState: StateFlow<AiTaskState<DiseaseDetectionResult>> = _diseaseTaskState.asStateFlow()

  private val _cropRecommendations = MutableStateFlow<List<CropScoreResult>>(emptyList())
  val cropRecommendations: StateFlow<List<CropScoreResult>> = _cropRecommendations.asStateFlow()

  private val _irrigationAdvice = MutableStateFlow<IrrigationAdviceResult?>(null)
  val irrigationAdvice: StateFlow<IrrigationAdviceResult?> = _irrigationAdvice.asStateFlow()

  private val _apiHealthStatus = MutableStateFlow("Not checked yet")
  val apiHealthStatus: StateFlow<String> = _apiHealthStatus.asStateFlow()

  private val _actionError = MutableStateFlow<String?>(null)
  val actionError: StateFlow<String?> = _actionError.asStateFlow()

  private fun <T> Flow<List<T>>.toFirestoreStateFlow(label: String): StateFlow<FirestoreUiState<List<T>>> =
    this.map<List<T>, FirestoreUiState<List<T>>> { FirestoreUiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        emit(FirestoreUiState.Error(rootError.message ?: "Failed to observe $label"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FirestoreUiState.Loading,
      )

  val chatMessagesState: StateFlow<FirestoreUiState<List<ChatMessageRecord>>> =
    repository.observeChatMessages(currentUserId).toFirestoreStateFlow("chat_messages")

  val soilHistoryState: StateFlow<FirestoreUiState<List<SoilAnalysisRecord>>> =
    repository.observeSoilHistory(currentUserId).toFirestoreStateFlow("soil_analyses")

  val diseaseHistoryState: StateFlow<FirestoreUiState<List<DiseaseDetectionRecord>>> =
    repository.observeDiseaseHistory(currentUserId).toFirestoreStateFlow("disease_detections")

  val cropRecHistoryState: StateFlow<FirestoreUiState<List<CropRecommendationRecord>>> =
    repository.observeCropRecHistory(currentUserId).toFirestoreStateFlow("crop_recommendations")

  val weatherAdvisoriesState: StateFlow<FirestoreUiState<List<WeatherAdvisoryRecord>>> =
    repository.observeWeatherAdvisories(currentUserId).toFirestoreStateFlow("weather_advisories")

  val savedCropsState: StateFlow<FirestoreUiState<List<SavedCropRecord>>> =
    repository.observeSavedCrops(currentUserId).toFirestoreStateFlow("saved_crops")

  val activityLogsState: StateFlow<FirestoreUiState<List<ActivityLogRecord>>> =
    repository.observeActivityLogs(currentUserId).toFirestoreStateFlow("activity_logs")

  @OptIn(ExperimentalCoroutinesApi::class)
  val platformActivityState: StateFlow<FirestoreUiState<List<ActivityLogRecord>>> =
    flowOf(isAdminUser)
      .flatMapLatest { admin ->
        if (!admin) {
          flowOf(FirestoreUiState.Success(emptyList()))
        } else {
          repository.observePlatformActivity()
            .map<List<ActivityLogRecord>, FirestoreUiState<List<ActivityLogRecord>>> { FirestoreUiState.Success(it) }
            .catch { error ->
              val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
              if (firestoreError == null && error is CancellationException) throw error
              val rootError = firestoreError ?: error
              Log.w(TAG, "Error observing platform_activity", rootError)
              emit(FirestoreUiState.Error(rootError.message ?: "Failed to observe platform activity"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FirestoreUiState.Loading,
      )

  fun clearChatError() {
    _chatError.value = null
  }

  fun clearActionError() {
    _actionError.value = null
  }

  fun loadWeatherForFarmer(farmer: FarmerProfile, language: AppLanguage, saveAdvisory: Boolean = false) {
    val (lat, lon) = IndianAgriData.findCoordinates(farmer.state, farmer.district)
    loadWeather(
      locationName = "${farmer.district}, ${farmer.state}",
      latitude = lat,
      longitude = lon,
      language = language,
      saveAdvisory = saveAdvisory,
    )
  }

  fun loadWeather(
    locationName: String,
    latitude: Double,
    longitude: Double,
    language: AppLanguage,
    saveAdvisory: Boolean = true,
  ) {
    viewModelScope.launch {
      _weatherState.value = WeatherUiState.Loading
      val res = repository.fetchLiveWeather(
        userId = currentUserId,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        language = language,
        saveAdvisory = saveAdvisory,
      )
      _weatherState.value = res.fold(
        onSuccess = { WeatherUiState.Success(it) },
        onFailure = { WeatherUiState.Error(it.localizedMessage ?: "Unable to fetch live weather from Open-Meteo.") },
      )
    }
  }

  fun fetchMarketPrices(state: String?, district: String?, commodity: String?) {
    viewModelScope.launch {
      _marketState.value = MarketUiState.Loading
      val result = repository.fetchMarketPrices(state, district, commodity)
      _marketState.value = MarketUiState.Result(result)
    }
  }

  fun sendAiQuestion(question: String, language: AppLanguage, farmer: FarmerProfile?) {
    if (question.isBlank() || _isAiThinking.value) return
    val weatherLine = (weatherState.value as? WeatherUiState.Success)?.data?.let {
      "${it.tempC}°C, ${it.humidityPct}% humidity, ${it.rainMm} mm rain"
    }
    val history = chatMessagesState.value.dataOrEmpty()
    viewModelScope.launch {
      _isAiThinking.value = true
      _chatError.value = null
      _streamingAssistantText.value = ""
      try {
        val result = repository.askAiAssistantStream(
          userId = currentUserId,
          question = question.trim(),
          history = history,
          language = language,
          farmer = farmer,
          weatherSummary = weatherLine,
          onPartialText = { partial ->
            _streamingAssistantText.value = partial
          },
        )
        result.onFailure { err ->
          Log.e(TAG, "AI Assistant error", err)
          _chatError.value = err.localizedMessage ?: "Failed to get AI response."
        }
      } finally {
        _streamingAssistantText.value = null
        _isAiThinking.value = false
      }
    }
  }

  fun clearChatHistory() {
    viewModelScope.launch {
      repository.clearChatHistory(currentUserId).onFailure {
        _chatError.value = it.localizedMessage ?: "Failed to clear chat history."
      }
    }
  }

  fun analyzeSoil(
    imageUri: String?,
    soilHint: IndianSoilType,
    phValue: Double?,
    moistureNote: String,
    language: AppLanguage,
  ) {
    viewModelScope.launch {
      _soilTaskState.value = AiTaskState.Loading
      val res = repository.runSoilAnalysis(
        userId = currentUserId,
        imageUri = imageUri,
        soilHint = soilHint,
        phValue = phValue,
        moistureNote = moistureNote,
        language = language,
      )
      _soilTaskState.value = res.fold(
        onSuccess = { AiTaskState.Success(it) },
        onFailure = {
          Log.e(TAG, "Soil analysis error", it)
          AiTaskState.Error(it.localizedMessage ?: "Soil analysis failed.")
        },
      )
    }
  }

  fun detectDisease(
    imageUri: String?,
    cropName: String,
    symptomsNote: String,
    language: AppLanguage,
  ) {
    viewModelScope.launch {
      _diseaseTaskState.value = AiTaskState.Loading
      val res = repository.runDiseaseDetection(
        userId = currentUserId,
        imageUri = imageUri,
        cropName = cropName,
        symptomsNote = symptomsNote,
        language = language,
      )
      _diseaseTaskState.value = res.fold(
        onSuccess = { AiTaskState.Success(it) },
        onFailure = {
          Log.e(TAG, "Disease detection error", it)
          AiTaskState.Error(it.localizedMessage ?: "Disease detection failed.")
        },
      )
    }
  }

  fun generateCropRecommendations(
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
  ) {
    viewModelScope.launch {
      val res = repository.recommendCrops(
        userId = currentUserId,
        soilType = soilType,
        ph = ph,
        temperatureC = temperatureC,
        rainfallMm = rainfallMm,
        season = season,
        state = state,
        district = district,
        irrigationType = irrigationType,
        farmSizeAcres = farmSizeAcres,
        preferenceKeyword = preferenceKeyword,
      )
      res.fold(
        onSuccess = { _cropRecommendations.value = it },
        onFailure = { _actionError.value = it.localizedMessage ?: "Failed to save crop plan." },
      )
    }
  }

  fun computeIrrigation(
    cropName: String,
    soilType: IndianSoilType,
    growthStage: String,
    tempC: Double,
    humidityPct: Int,
    next3DaysRainMm: Double,
    soilMoisturePct: Double?,
  ) {
    _irrigationAdvice.value = repository.computeIrrigationAdvice(
      cropName = cropName,
      soilType = soilType,
      growthStage = growthStage,
      tempC = tempC,
      humidityPct = humidityPct,
      next3DaysRainMm = next3DaysRainMm,
      soilMoisturePct = soilMoisturePct,
    )
  }

  fun saveCrop(cropName: String, season: String, notes: String) {
    viewModelScope.launch {
      repository.saveCrop(currentUserId, cropName, season, notes).onFailure {
        _actionError.value = it.localizedMessage ?: "Failed to save crop."
      }
    }
  }

  fun deleteSoilRecord(id: String) {
    viewModelScope.launch { repository.deleteSoilRecord(id, currentUserId) }
  }

  fun deleteDiseaseRecord(id: String) {
    viewModelScope.launch { repository.deleteDiseaseRecord(id, currentUserId) }
  }

  fun deleteCropRecRecord(id: String) {
    viewModelScope.launch { repository.deleteCropRecRecord(id, currentUserId) }
  }

  fun deleteWeatherRecord(id: String) {
    viewModelScope.launch { repository.deleteWeatherRecord(id, currentUserId) }
  }

  fun deleteSavedCrop(id: String) {
    viewModelScope.launch { repository.deleteSavedCrop(id, currentUserId) }
  }

  fun deleteActivityLog(id: String) {
    viewModelScope.launch { repository.deleteActivityLog(id, currentUserId) }
  }

  fun runApiHealthCheck() {
    viewModelScope.launch {
      _apiHealthStatus.value = "Checking Open-Meteo, Agmarknet & Firebase AI..."
      val weatherCheck = repository.fetchLiveWeather(
        userId = currentUserId,
        locationName = "Pune, Maharashtra",
        latitude = 18.5204,
        longitude = 73.8567,
        language = AppLanguage.EN,
        saveAdvisory = false,
      )
      val weatherStr = if (weatherCheck.isSuccess) "Open-Meteo: OK" else "Open-Meteo: Error"
      val marketCheck = repository.fetchMarketPrices("Maharashtra", null, null)
      val marketStr = when (marketCheck) {
        is MarketPricesResult.Success -> "Agmarknet: OK"
        is MarketPricesResult.Unavailable -> if (marketCheck.missingKey) "Agmarknet: Missing DATA_GOV_API_KEY" else "Agmarknet: Unavailable"
      }
      val aiStr = if (isLiveAiConfigured) "AI: Gemini ($aiStatusLabel)" else "AI: Not Configured"
      _apiHealthStatus.value = "$weatherStr • $marketStr • $aiStr"
    }
  }

  class Factory(
    private val repository: AgriDataRepository,
    private val currentUserId: String,
    private val isAdminUser: Boolean,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      AgriViewModel(repository, currentUserId, isAdminUser) as T
  }
}
