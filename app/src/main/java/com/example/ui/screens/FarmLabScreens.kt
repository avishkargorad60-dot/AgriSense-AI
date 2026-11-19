package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ai.DiseaseDetectionResult
import com.example.data.ai.SoilAnalysisResult
import com.example.data.model.FarmerProfile
import com.example.data.repository.CropScoreResult
import com.example.data.repository.IrrigationAdviceResult
import com.example.ui.components.AiNoticeBanner
import com.example.ui.components.SimpleDropdownSelector
import com.example.ui.components.StatusPill
import com.example.ui.theme.FreshEmerald
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.HarvestGoldContainer
import com.example.ui.viewmodel.AiTaskState
import com.example.ui.viewmodel.WeatherUiState
import com.example.util.AppLanguage
import com.example.util.AppStringsProvider
import com.example.util.IndianAgriData
import com.example.util.IndianSeason
import com.example.util.IndianSoilType
import com.example.util.IrrigationType
import java.io.File
import kotlin.math.roundToInt

@Composable
fun FarmLabScreen(
  initialTab: Int,
  farmer: FarmerProfile,
  language: AppLanguage,
  weatherState: WeatherUiState,
  soilTaskState: AiTaskState<SoilAnalysisResult>,
  diseaseTaskState: AiTaskState<DiseaseDetectionResult>,
  cropRecommendations: List<CropScoreResult>,
  irrigationAdvice: IrrigationAdviceResult?,
  onAnalyzeSoil: (String?, IndianSoilType, Double?, String) -> Unit,
  onDetectDisease: (String?, String, String) -> Unit,
  onGenerateCropPlan: (IndianSoilType, Double?, Double, Double, IndianSeason, String, String, IrrigationType, Double, String) -> Unit,
  onSaveCrop: (String, String, String) -> Unit,
  onComputeIrrigation: (String, IndianSoilType, String, Double, Int, Double, Double?) -> Unit,
) {
  var activeTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
  LaunchedEffect(initialTab) {
    activeTab = initialTab.coerceIn(0, 3)
  }

  val strings = AppStringsProvider.forLanguage(language)
  val tabs = listOf(
    strings.soilAnalysisTitle,
    strings.diseaseDetectionTitle,
    strings.cropAdvisorTitle,
    strings.irrigationAdvisorTitle,
  )

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = 840.dp)
        .fillMaxSize(),
    ) {
      ScrollableTabRow(
        selectedTabIndex = activeTab,
        edgePadding = 12.dp,
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = activeTab == index,
            onClick = { activeTab = index },
            text = { Text(title, maxLines = 1) },
            modifier = Modifier.testTag("farmlab_tab_$index"),
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        when (activeTab) {
          0 -> SoilAnalysisTab(
            farmer = farmer,
            language = language,
            strings = strings,
            taskState = soilTaskState,
            onAnalyze = onAnalyzeSoil,
          )
          1 -> DiseaseDetectionTab(
            farmer = farmer,
            strings = strings,
            taskState = diseaseTaskState,
            onDetect = onDetectDisease,
          )
          2 -> CropAdvisorTab(
            farmer = farmer,
            language = language,
            strings = strings,
            weatherState = weatherState,
            recommendations = cropRecommendations,
            onRecommend = onGenerateCropPlan,
            onSaveCrop = onSaveCrop,
          )
          3 -> IrrigationAdvisorTab(
            farmer = farmer,
            weatherState = weatherState,
            advice = irrigationAdvice,
            onCompute = onComputeIrrigation,
          )
        }
      }
    }
  }
}

@Composable
private fun SoilAnalysisTab(
  farmer: FarmerProfile,
  language: AppLanguage,
  strings: com.example.util.AgriStrings,
  taskState: AiTaskState<SoilAnalysisResult>,
  onAnalyze: (String?, IndianSoilType, Double?, String) -> Unit,
) {
  val isLoading = taskState is AiTaskState.Loading
  val result = (taskState as? AiTaskState.Success)?.data
  val errorMsg = (taskState as? AiTaskState.Error)?.message
  val context = LocalContext.current
  var selectedUri by remember { mutableStateOf<Uri?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var selectedSoil by remember {
    mutableStateOf(
      IndianSoilType.entries.firstOrNull { it.labelEn.equals(farmer.soilType, true) } ?: IndianSoilType.BLACK
    )
  }
  var phInput by remember { mutableStateOf("7.1") }
  var moistureNote by remember { mutableStateOf("Moist / Medium") }

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri -> if (uri != null) selectedUri = uri }

  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success -> if (success) selectedUri = pendingCameraUri }

  val cameraPermLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      val uri = createTempImageUri(context)
      pendingCameraUri = uri
      cameraLauncher.launch(uri)
    } else {
      Toast.makeText(context, "Camera permission denied. Use Photo Picker instead.", Toast.LENGTH_SHORT).show()
    }
  }

  AiNoticeBanner(message = strings.aiNoticeBanner)

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp)
          .clip(RoundedCornerShape(14.dp)),
      ) {
        if (selectedUri != null) {
          AsyncImage(
            model = selectedUri,
            contentDescription = "Selected soil photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        } else {
          Image(
            painter = painterResource(id = R.drawable.img_soil_analysis_1795086583671),
            contentDescription = "Soil sample preview placeholder",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("soil_pick_photo_button"),
        ) {
          Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.uploadFromGallery)
        }
        OutlinedButton(
          onClick = {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
              PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
              val uri = createTempImageUri(context)
              pendingCameraUri = uri
              cameraLauncher.launch(uri)
            } else {
              cameraPermLauncher.launch(Manifest.permission.CAMERA)
            }
          },
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("soil_camera_button"),
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.capturePhoto)
        }
      }

      SimpleDropdownSelector(
        label = "Soil Category Hint",
        selectedValue = selectedSoil.localizedLabel(language),
        options = IndianSoilType.entries.map { it.labelEn },
        onSelect = { chosen ->
          selectedSoil = IndianSoilType.entries.firstOrNull { it.labelEn == chosen } ?: IndianSoilType.BLACK
        },
        testTag = "soil_type_dropdown",
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = phInput,
          onValueChange = { phInput = it },
          label = { Text("Soil pH (optional)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("soil_ph_input"),
        )
        OutlinedTextField(
          value = moistureNote,
          onValueChange = { moistureNote = it },
          label = { Text("Moisture / Texture") },
          singleLine = true,
          modifier = Modifier.weight(1.4f).testTag("soil_moisture_input"),
        )
      }

      Button(
        onClick = {
          onAnalyze(selectedUri?.toString(), selectedSoil, phInput.toDoubleOrNull(), moistureNote)
        },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("soil_analyze_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        } else {
          Text(strings.analyzeNow)
        }
      }
    }
  }

  if (!errorMsg.isNullOrBlank()) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
      Text(
        text = errorMsg,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.padding(14.dp),
      )
    }
  }

  if (result != null) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = if (result.isLiveAi) "GEMINI AI • FIREBASE AI LOGIC" else "AGRISENSE ANALYSIS")
        Text(
          text = result.predictedCategory.localizedLabel(language),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(result.confidenceLabel, style = MaterialTheme.typography.bodySmall)
        Text(result.suitabilitySummary, style = MaterialTheme.typography.bodyMedium)
        Text("Suitable Crops:", style = MaterialTheme.typography.titleSmall)
        Text(result.cropRecommendations.joinToString(" • "), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text("Soil Improvement Suggestions:", style = MaterialTheme.typography.titleSmall)
        result.improvementSuggestions.forEach { tip ->
          Text("• $tip", style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}

@Composable
private fun DiseaseDetectionTab(
  farmer: FarmerProfile,
  strings: com.example.util.AgriStrings,
  taskState: AiTaskState<DiseaseDetectionResult>,
  onDetect: (String?, String, String) -> Unit,
) {
  val isLoading = taskState is AiTaskState.Loading
  val result = (taskState as? AiTaskState.Success)?.data
  val errorMsg = (taskState as? AiTaskState.Error)?.message
  val context = LocalContext.current
  var selectedUri by remember { mutableStateOf<Uri?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var cropName by remember { mutableStateOf(farmer.mainCrops.substringBefore(",").trim().ifBlank { "Soybean" }) }
  var symptomsNote by remember { mutableStateOf("") }

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri -> if (uri != null) selectedUri = uri }

  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success -> if (success) selectedUri = pendingCameraUri }

  val cameraPermLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      val uri = createTempImageUri(context)
      pendingCameraUri = uri
      cameraLauncher.launch(uri)
    } else {
      Toast.makeText(context, "Camera permission denied. Choose a photo from gallery.", Toast.LENGTH_SHORT).show()
    }
  }

  AiNoticeBanner(message = strings.aiNoticeBanner)

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp)
          .clip(RoundedCornerShape(14.dp)),
      ) {
        if (selectedUri != null) {
          AsyncImage(
            model = selectedUri,
            contentDescription = "Selected crop leaf photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        } else {
          Image(
            painter = painterResource(id = R.drawable.img_crop_health_1795086583591),
            contentDescription = "Crop leaf preview placeholder",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("disease_pick_photo_button"),
        ) {
          Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.uploadFromGallery)
        }
        OutlinedButton(
          onClick = {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
              PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
              val uri = createTempImageUri(context)
              pendingCameraUri = uri
              cameraLauncher.launch(uri)
            } else {
              cameraPermLauncher.launch(Manifest.permission.CAMERA)
            }
          },
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("disease_camera_button"),
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.capturePhoto)
        }
      }

      SimpleDropdownSelector(
        label = "Select Crop",
        selectedValue = cropName,
        options = IndianAgriData.crops.map { it.name },
        onSelect = { cropName = it },
        testTag = "disease_crop_dropdown",
      )

      OutlinedTextField(
        value = symptomsNote,
        onValueChange = { symptomsNote = it },
        label = { Text("Visible Symptoms (e.g., yellow spots, leaf curl, rust)") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth().testTag("disease_symptoms_input"),
      )

      Button(
        onClick = { onDetect(selectedUri?.toString(), cropName, symptomsNote) },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("disease_analyze_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        } else {
          Text(strings.analyzeNow)
        }
      }
    }
  }

  // Mandatory Expert Confirmation Disclaimer
  Card(
    colors = CardDefaults.cardColors(containerColor = HarvestGoldContainer),
    shape = RoundedCornerShape(14.dp),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(Icons.Default.Warning, contentDescription = null, tint = HarvestGold)
      Text(strings.diseaseDisclaimer, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A3000))
    }
  }

  if (!errorMsg.isNullOrBlank()) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
      Text(
        text = errorMsg,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.padding(14.dp),
      )
    }
  }

  if (result != null) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = if (result.isLiveAi) "GEMINI AI • PRELIMINARY ASSESSMENT" else "PRELIMINARY ASSESSMENT")
        Text(result.predictedDisease, style = MaterialTheme.typography.titleLarge)
        Text(result.confidenceLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Symptoms & Causes", style = MaterialTheme.typography.titleSmall)
        Text(result.symptomsAndCauses, style = MaterialTheme.typography.bodyMedium)
        Text("Prevention Guidance", style = MaterialTheme.typography.titleSmall)
        Text(result.preventionGuidance, style = MaterialTheme.typography.bodyMedium)
        Text("General Treatment Information", style = MaterialTheme.typography.titleSmall)
        Text(result.treatmentInformation, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

@Composable
private fun CropAdvisorTab(
  farmer: FarmerProfile,
  language: AppLanguage,
  strings: com.example.util.AgriStrings,
  weatherState: WeatherUiState,
  recommendations: List<CropScoreResult>,
  onRecommend: (IndianSoilType, Double?, Double, Double, IndianSeason, String, String, IrrigationType, Double, String) -> Unit,
  onSaveCrop: (String, String, String) -> Unit,
) {
  val context = LocalContext.current
  val liveTemp = (weatherState as? WeatherUiState.Success)?.data?.tempC ?: 28.0
  var selectedSoil by remember {
    mutableStateOf(
      IndianSoilType.entries.firstOrNull { it.labelEn.equals(farmer.soilType, true) } ?: IndianSoilType.BLACK
    )
  }
  var selectedSeason by remember { mutableStateOf(IndianSeason.KHARIF) }
  var selectedIrrigation by remember { mutableStateOf(IrrigationType.BOREWELL) }
  var phInput by remember { mutableStateOf("6.8") }
  var tempInput by remember { mutableStateOf("${liveTemp.roundToInt()}") }
  var rainfallInput by remember { mutableStateOf("650") }
  var preferenceInput by remember { mutableStateOf("") }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Transparent Agronomy Scoring Engine",
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text = "Scores crops against Indian Kharif/Rabi/Zaid windows, soil compatibility, temperature, rainfall, pH, and irrigation.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Text("Farming Season", style = MaterialTheme.typography.labelLarge)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IndianSeason.entries.forEach { season ->
          FilterChip(
            selected = selectedSeason == season,
            onClick = { selectedSeason = season },
            label = { Text(season.name) },
            modifier = Modifier.weight(1f).testTag("season_chip_${season.name}"),
          )
        }
      }

      SimpleDropdownSelector(
        label = strings.soilTypeLabel,
        selectedValue = selectedSoil.localizedLabel(language),
        options = IndianSoilType.entries.map { it.labelEn },
        onSelect = { chosen ->
          selectedSoil = IndianSoilType.entries.firstOrNull { it.labelEn == chosen } ?: IndianSoilType.BLACK
        },
        testTag = "advisor_soil_dropdown",
      )

      SimpleDropdownSelector(
        label = "Irrigation Availability",
        selectedValue = selectedIrrigation.localizedLabel(language),
        options = IrrigationType.entries.map { it.labelEn },
        onSelect = { chosen ->
          selectedIrrigation = IrrigationType.entries.firstOrNull { it.labelEn == chosen } ?: IrrigationType.RAINFED
        },
        testTag = "advisor_irrigation_dropdown",
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = tempInput,
          onValueChange = { tempInput = it },
          label = { Text("Temp (°C)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = rainfallInput,
          onValueChange = { rainfallInput = it },
          label = { Text("Rainfall (mm)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = phInput,
          onValueChange = { phInput = it },
          label = { Text("pH") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(0.8f),
        )
      }

      OutlinedTextField(
        value = preferenceInput,
        onValueChange = { preferenceInput = it },
        label = { Text("Crop Preference (optional, e.g., Soybean, Onion)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("advisor_preference_input"),
      )

      Button(
        onClick = {
          onRecommend(
            selectedSoil,
            phInput.toDoubleOrNull(),
            tempInput.toDoubleOrNull() ?: 28.0,
            rainfallInput.toDoubleOrNull() ?: 600.0,
            selectedSeason,
            farmer.state,
            farmer.district,
            selectedIrrigation,
            farmer.farmSizeAcres,
            preferenceInput,
          )
        },
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("advisor_generate_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Text(strings.getRecommendations)
      }
    }
  }

  recommendations.forEach { rec ->
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${rec.crop.name} (${rec.crop.hindiName})",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "Growing period: ${rec.crop.durationDays} • Soil match: ${rec.soilCompatibility}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = "${rec.score}%",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }

        LinearProgressIndicator(
          progress = { rec.score / 100f },
          modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
          color = FreshEmerald,
        )

        rec.reasons.forEach { reason ->
          Text("• $reason", style = MaterialTheme.typography.bodySmall)
        }
        Text(
          text = "Water: ${rec.crop.waterRequirement}",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = "Guidance: ${rec.crop.guidance}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
          onClick = {
            onSaveCrop(rec.crop.name, selectedSeason.labelEn, "Score ${rec.score}% • ${rec.crop.durationDays}")
            Toast.makeText(context, "Saved ${rec.crop.name} to Records", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.align(Alignment.End).minimumInteractiveComponentSize(),
        ) {
          Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Save Crop")
        }
      }
    }
  }
}

@Composable
private fun IrrigationAdvisorTab(
  farmer: FarmerProfile,
  weatherState: WeatherUiState,
  advice: IrrigationAdviceResult?,
  onCompute: (String, IndianSoilType, String, Double, Int, Double, Double?) -> Unit,
) {
  val live = (weatherState as? WeatherUiState.Success)?.data
  val next3DaysRain = live?.dailyForecast?.take(3)?.sumOf { it.rainMm } ?: 0.0
  var cropName by remember { mutableStateOf(farmer.mainCrops.substringBefore(",").trim().ifBlank { "Wheat" }) }
  var soilType by remember {
    mutableStateOf(
      IndianSoilType.entries.firstOrNull { it.labelEn.equals(farmer.soilType, true) } ?: IndianSoilType.BLACK
    )
  }
  val stages = listOf("Sowing / Germination", "Vegetative Growth", "Flowering / Pod Init", "Grain Filling", "Maturity")
  var selectedStage by remember { mutableStateOf(stages[2]) }
  var moistureInput by remember { mutableStateOf("") }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Smart Irrigation Advisor", style = MaterialTheme.typography.titleMedium)
      Text(
        text = if (live != null) {
          "Using live Open-Meteo conditions (${live.tempC.roundToInt()}°C, ${live.humidityPct}% RH, ${"%.1f".format(next3DaysRain)} mm 3-day rain forecast)"
        } else {
          "Using baseline conditions (refresh live weather on Home for local forecast integration)"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
      )

      SimpleDropdownSelector(
        label = "Crop",
        selectedValue = cropName,
        options = IndianAgriData.crops.map { it.name },
        onSelect = { cropName = it },
        testTag = "irrigation_crop_dropdown",
      )

      SimpleDropdownSelector(
        label = "Soil Type",
        selectedValue = soilType.labelEn,
        options = IndianSoilType.entries.map { it.labelEn },
        onSelect = { chosen ->
          soilType = IndianSoilType.entries.firstOrNull { it.labelEn == chosen } ?: IndianSoilType.BLACK
        },
        testTag = "irrigation_soil_dropdown",
      )

      SimpleDropdownSelector(
        label = "Crop Growth Stage",
        selectedValue = selectedStage,
        options = stages,
        onSelect = { selectedStage = it },
        testTag = "irrigation_stage_dropdown",
      )

      OutlinedTextField(
        value = moistureInput,
        onValueChange = { moistureInput = it },
        label = { Text("Field Soil Moisture % (optional, leave blank if unmeasured)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("irrigation_moisture_input"),
      )

      Button(
        onClick = {
          onCompute(
            cropName,
            soilType,
            selectedStage,
            live?.tempC ?: 29.0,
            live?.humidityPct ?: 55,
            next3DaysRain,
            moistureInput.toDoubleOrNull(),
          )
        },
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("irrigation_compute_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Text("Generate Irrigation Plan")
      }
    }
  }

  if (advice != null) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(advice.recommendationTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(advice.irrigationGuidance, style = MaterialTheme.typography.bodyMedium)
        Text("Monitoring Frequency:", style = MaterialTheme.typography.titleSmall)
        Text(advice.monitoringFrequency, style = MaterialTheme.typography.bodySmall)
        Text("Rainfall Precautions:", style = MaterialTheme.typography.titleSmall)
        Text(advice.rainfallPrecautions, style = MaterialTheme.typography.bodySmall)
        Text("Water Conservation Suggestions:", style = MaterialTheme.typography.titleSmall)
        advice.conservationTips.forEach { tip ->
          Text("• $tip", style = MaterialTheme.typography.bodySmall)
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = HarvestGoldContainer,
        ) {
          Text(
            text = advice.precisionCaveat,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4A3000),
            modifier = Modifier.padding(10.dp),
          )
        }
      }
    }
  }
}

private fun createTempImageUri(context: Context): Uri {
  val dir = File(context.cacheDir, "camera_images").apply { mkdirs() }
  val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
