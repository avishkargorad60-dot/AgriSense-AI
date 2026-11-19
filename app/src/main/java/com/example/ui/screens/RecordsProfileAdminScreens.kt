package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityLogRecord
import com.example.data.model.CropRecommendationRecord
import com.example.data.model.DiseaseDetectionRecord
import com.example.data.model.FarmerProfile
import com.example.data.model.SavedCropRecord
import com.example.data.model.SoilAnalysisRecord
import com.example.data.model.WeatherAdvisoryRecord
import com.example.ui.components.LanguageSelectorRow
import com.example.ui.components.SimpleDropdownSelector
import com.example.ui.components.StatusPill
import com.example.ui.components.UsageAnalyticsBarChart
import com.example.ui.viewmodel.AuthActionState
import com.example.ui.viewmodel.FirestoreUiState
import com.example.ui.viewmodel.dataOrEmpty
import com.example.util.AppLanguage
import com.example.util.AppStringsProvider
import com.example.util.IndianAgriData
import com.example.util.IndianSoilType
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private fun formatFirestoreTime(fmt: SimpleDateFormat, ts: Timestamp?): String =
  fmt.format((ts ?: Timestamp.now()).toDate())

@Composable
fun RecordsHistoryScreen(
  soilHistoryState: FirestoreUiState<List<SoilAnalysisRecord>>,
  diseaseHistoryState: FirestoreUiState<List<DiseaseDetectionRecord>>,
  cropRecHistoryState: FirestoreUiState<List<CropRecommendationRecord>>,
  weatherAdvisoriesState: FirestoreUiState<List<WeatherAdvisoryRecord>>,
  savedCropsState: FirestoreUiState<List<SavedCropRecord>>,
  activityLogsState: FirestoreUiState<List<ActivityLogRecord>>,
  onDeleteSoil: (String) -> Unit,
  onDeleteDisease: (String) -> Unit,
  onDeleteCropRec: (String) -> Unit,
  onDeleteWeather: (String) -> Unit,
  onDeleteSavedCrop: (String) -> Unit,
  onDeleteActivity: (String) -> Unit,
) {
  val categories = listOf("Soil", "Disease", "Crop Plans", "Saved Crops", "Weather", "Activity")
  var selectedCategory by remember { mutableStateOf("Soil") }
  var searchQuery by remember { mutableStateOf("") }
  val fmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

  val soilHistory = soilHistoryState.dataOrEmpty()
  val diseaseHistory = diseaseHistoryState.dataOrEmpty()
  val cropRecHistory = cropRecHistoryState.dataOrEmpty()
  val weatherAdvisories = weatherAdvisoriesState.dataOrEmpty()
  val savedCrops = savedCropsState.dataOrEmpty()
  val activityLogs = activityLogsState.dataOrEmpty()

  val activeError = when (selectedCategory) {
    "Soil" -> (soilHistoryState as? FirestoreUiState.Error)?.message
    "Disease" -> (diseaseHistoryState as? FirestoreUiState.Error)?.message
    "Crop Plans" -> (cropRecHistoryState as? FirestoreUiState.Error)?.message
    "Saved Crops" -> (savedCropsState as? FirestoreUiState.Error)?.message
    "Weather" -> (weatherAdvisoriesState as? FirestoreUiState.Error)?.message
    "Activity" -> (activityLogsState as? FirestoreUiState.Error)?.message
    else -> null
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 820.dp)
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text("Farmer Records & History", style = MaterialTheme.typography.headlineSmall)
        Text(
          text = "Search and manage your cloud-synced soil reports, disease checks, crop plans, and weather advisories.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = { Text("Search records...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("records_search_input"),
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          categories.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat) },
              modifier = Modifier.testTag("records_chip_$cat"),
            )
          }
        }
      }

      if (!activeError.isNullOrBlank()) {
        item {
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(
              text = activeError,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }

      when (selectedCategory) {
        "Soil" -> {
          val filtered = soilHistory.filter {
            searchQuery.isBlank() || it.predictedCategory.contains(searchQuery, true) ||
              it.cropRecommendations.contains(searchQuery, true)
          }
          if (soilHistoryState is FirestoreUiState.Loading) {
            item { LoadingRecordsRow() }
          } else if (filtered.isEmpty()) {
            item { EmptyRecordHint("No soil analysis records yet.") }
          }
          items(filtered, key = { it.id.ifBlank { it.hashCode().toString() } }) { item ->
            RecordCard(
              title = item.predictedCategory,
              subtitle = "${formatFirestoreTime(fmt, item.createdAt)} • Crops: ${item.cropRecommendations}",
              body = item.improvementTips,
              badge = if (item.liveAi) "Gemini AI" else null,
              onDelete = { onDeleteSoil(item.id) },
            )
          }
        }

        "Disease" -> {
          val filtered = diseaseHistory.filter {
            searchQuery.isBlank() || it.cropName.contains(searchQuery, true) ||
              it.predictedDisease.contains(searchQuery, true)
          }
          if (diseaseHistoryState is FirestoreUiState.Loading) {
            item { LoadingRecordsRow() }
          } else if (filtered.isEmpty()) {
            item { EmptyRecordHint("No crop disease records yet.") }
          }
          items(filtered, key = { it.id.ifBlank { it.hashCode().toString() } }) { item ->
            RecordCard(
              title = "${item.cropName}: ${item.predictedDisease}",
              subtitle = formatFirestoreTime(fmt, item.createdAt),
              body = item.treatmentGuidance,
              badge = if (item.liveAi) "Gemini AI" else null,
              onDelete = { onDeleteDisease(item.id) },
            )
          }
        }

        "Crop Plans" -> {
          val filtered = cropRecHistory.filter {
            searchQuery.isBlank() || it.topCropName.contains(searchQuery, true) ||
              it.season.contains(searchQuery, true)
          }
          if (cropRecHistoryState is FirestoreUiState.Loading) {
            item { LoadingRecordsRow() }
          } else if (filtered.isEmpty()) {
            item { EmptyRecordHint("No crop recommendation history yet.") }
          }
          items(filtered, key = { it.id.ifBlank { it.hashCode().toString() } }) { item ->
            RecordCard(
              title = "${item.season} • Top: ${item.topCropName} (${item.topScore}%)",
              subtitle = "${formatFirestoreTime(fmt, item.createdAt)} • ${item.soilType}",
              body = item.recommendedCropsSummary,
              onDelete = { onDeleteCropRec(item.id) },
            )
          }
        }

        "Saved Crops" -> {
          val filtered = savedCrops.filter {
            searchQuery.isBlank() || it.cropName.contains(searchQuery, true)
          }
          if (savedCropsState is FirestoreUiState.Loading) {
            item { LoadingRecordsRow() }
          } else if (filtered.isEmpty()) {
            item { EmptyRecordHint("No saved crops yet. Save crops from Smart Crop Advisor.") }
          }
          items(filtered, key = { it.id.ifBlank { it.hashCode().toString() } }) { item ->
            RecordCard(
              title = item.cropName,
              subtitle = "${item.season} • ${formatFirestoreTime(fmt, item.createdAt)}",
              body = item.notes,
              onDelete = { onDeleteSavedCrop(item.id) },
            )
          }
        }

        "Weather" -> {
          val filtered = weatherAdvisories.filter {
            searchQuery.isBlank() || it.locationName.contains(searchQuery, true) ||
              it.advisorySummary.contains(searchQuery, true)
          }
          if (weatherAdvisoriesState is FirestoreUiState.Loading) {
            item { LoadingRecordsRow() }
          } else if (filtered.isEmpty()) {
            item { EmptyRecordHint("No weather advisories logged yet.") }
          }
          items(filtered, key = { it.id.ifBlank { it.hashCode().toString() } }) { item ->
            RecordCard(
              title = "${item.locationName} (${item.temperatureC}°C)",
              subtitle = "${formatFirestoreTime(fmt, item.createdAt)} • RH ${item.humidityPct}%",
              body = item.advisorySummary,
              onDelete = { onDeleteWeather(item.id) },
            )
          }
        }

        "Activity" -> {
          val filtered = activityLogs.filter {
            searchQuery.isBlank() || it.actionTitle.contains(searchQuery, true) ||
              it.module.contains(searchQuery, true)
          }
          if (activityLogsState is FirestoreUiState.Loading) {
            item { LoadingRecordsRow() }
          } else if (filtered.isEmpty()) {
            item { EmptyRecordHint("No activity logs recorded yet.") }
          }
          items(filtered, key = { it.id.ifBlank { it.hashCode().toString() } }) { item ->
            RecordCard(
              title = "${item.module}: ${item.actionTitle}",
              subtitle = formatFirestoreTime(fmt, item.createdAt),
              body = item.details,
              onDelete = { onDeleteActivity(item.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LoadingRecordsRow() {
  Row(
    modifier = Modifier.fillMaxWidth().padding(20.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CircularProgressIndicator(modifier = Modifier.size(22.dp))
    Spacer(Modifier.width(10.dp))
    Text("Loading Cloud Firestore records...", style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun EmptyRecordHint(message: String) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(18.dp),
    )
  }
}

@Composable
private fun RecordCard(
  title: String,
  subtitle: String,
  body: String,
  badge: String? = null,
  onDelete: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          if (badge != null) {
            StatusPill(text = badge)
            Spacer(Modifier.height(4.dp))
          }
          Text(title, style = MaterialTheme.typography.titleMedium)
          Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(Icons.Default.DeleteOutline, contentDescription = "Delete record", tint = MaterialTheme.colorScheme.error)
        }
      }
      if (body.isNotBlank()) {
        Text(body, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
fun ProfileScreen(
  farmer: FarmerProfile,
  language: AppLanguage,
  actionState: AuthActionState,
  onSelectLanguage: (AppLanguage) -> Unit,
  onUpdateProfile: (FarmerProfile) -> Unit,
  onOpenKnowledge: () -> Unit,
  onOpenAdmin: () -> Unit,
  onLogout: () -> Unit,
) {
  val strings = AppStringsProvider.forLanguage(language)
  var fullName by remember(farmer.userId, farmer.fullName) { mutableStateOf(farmer.fullName) }
  var mobile by remember(farmer.userId, farmer.mobile) { mutableStateOf(farmer.mobile) }
  var state by remember(farmer.userId, farmer.state) { mutableStateOf(farmer.state) }
  val districts = remember(state) { IndianAgriData.districtsForState(state).map { it.district } }
  var district by remember(farmer.userId, state, farmer.district) {
    mutableStateOf(if (farmer.district in districts) farmer.district else (districts.firstOrNull() ?: farmer.district))
  }
  var village by remember(farmer.userId, farmer.village) { mutableStateOf(farmer.village) }
  var farmSize by remember(farmer.userId, farmer.farmSizeAcres) { mutableStateOf(farmer.farmSizeAcres.toString()) }
  var mainCrops by remember(farmer.userId, farmer.mainCrops) { mutableStateOf(farmer.mainCrops) }
  var soilType by remember(farmer.userId, farmer.soilType) { mutableStateOf(farmer.soilType) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(strings.profileTitle, style = MaterialTheme.typography.headlineSmall)
          Text(farmer.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusPill(text = farmer.role)
      }

      LanguageSelectorRow(selected = language, onSelect = onSelectLanguage)

      if (actionState is AuthActionState.Success) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
          Text(actionState.message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
      } else if (actionState is AuthActionState.Error) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
          Text(actionState.message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
      }

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(strings.fullNameLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text(strings.mobileLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SimpleDropdownSelector(
              label = strings.stateLabel,
              selectedValue = state,
              options = IndianAgriData.states,
              onSelect = { state = it },
              testTag = "profile_state_dropdown",
              modifier = Modifier.weight(1f),
            )
            SimpleDropdownSelector(
              label = strings.districtLabel,
              selectedValue = district,
              options = districts.ifEmpty { listOf(district) },
              onSelect = { district = it },
              testTag = "profile_district_dropdown",
              modifier = Modifier.weight(1f),
            )
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = village,
              onValueChange = { village = it },
              label = { Text(strings.villageLabel) },
              singleLine = true,
              modifier = Modifier.weight(1.3f),
            )
            OutlinedTextField(
              value = farmSize,
              onValueChange = { farmSize = it },
              label = { Text(strings.farmSizeLabel) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          OutlinedTextField(
            value = mainCrops,
            onValueChange = { mainCrops = it },
            label = { Text(strings.mainCropsLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          SimpleDropdownSelector(
            label = strings.soilTypeLabel,
            selectedValue = soilType,
            options = IndianSoilType.entries.map { it.labelEn },
            onSelect = { soilType = it },
            testTag = "profile_soil_dropdown",
          )
          Button(
            onClick = {
              onUpdateProfile(
                farmer.copy(
                  fullName = fullName.trim(),
                  mobile = mobile.trim(),
                  state = state,
                  district = district,
                  village = village.trim(),
                  preferredLanguage = language.code,
                  farmSizeAcres = farmSize.toDoubleOrNull() ?: farmer.farmSizeAcres,
                  mainCrops = mainCrops.trim(),
                  soilType = soilType,
                )
              )
            },
            enabled = actionState !is AuthActionState.Loading,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("profile_save_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text("Save Profile Changes")
          }
        }
      }

      OutlinedButton(
        onClick = onOpenKnowledge,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_knowledge_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(strings.knowledgeCenterTitle)
      }

      if (farmer.role == "ADMIN") {
        OutlinedButton(
          onClick = onOpenAdmin,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_admin_button"),
        ) {
          Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(strings.adminDashboardTitle)
        }
      }

      Button(
        onClick = onLogout,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("profile_logout_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(strings.logoutButton)
      }
    }
  }
}

@Composable
fun AdminDashboardScreen(
  currentFarmer: FarmerProfile,
  allUsersState: FirestoreUiState<List<FarmerProfile>>,
  platformActivityState: FirestoreUiState<List<ActivityLogRecord>>,
  aiStatusLabel: String,
  apiHealthStatus: String,
  onRunHealthCheck: () -> Unit,
  onToggleUserActive: (String, Boolean) -> Unit,
) {
  if (currentFarmer.role != "ADMIN") {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Unauthorized: Administrator role required.", color = MaterialTheme.colorScheme.error)
    }
    return
  }

  val allUsers = allUsersState.dataOrEmpty()
  val recentActivities = platformActivityState.dataOrEmpty()
  val totalFarmers = allUsers.count { it.role == "FARMER" }
  val activeUsers = allUsers.count { it.active }
  val fmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

  val chatCount = recentActivities.count { it.module == "AI Assistant" }
  val soilCount = recentActivities.count { it.module == "Soil Analysis" }
  val diseaseCount = recentActivities.count { it.module == "Disease Detection" }
  val cropCount = recentActivities.count { it.module == "Crop Advisor" }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 860.dp)
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text("Admin Console", style = MaterialTheme.typography.headlineSmall)
        Text(
          text = "Platform health, Cloud Firestore registrations, usage analytics, and user access control.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (allUsersState is FirestoreUiState.Error) {
        item {
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(allUsersState.message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          StatMetricCard("Registered Farmers", "$totalFarmers", Modifier.weight(1f))
          StatMetricCard("Active Users", "$activeUsers", Modifier.weight(1f))
          StatMetricCard("Total Accounts", "${allUsers.size}", Modifier.weight(1f))
        }
      }

      item {
        UsageAnalyticsBarChart(
          items = listOf(
            "AI Mitra Questions" to chatCount,
            "Soil Analyses" to soilCount,
            "Disease Checks" to diseaseCount,
            "Crop Plans" to cropCount,
          ),
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("AI & API Service Health", style = MaterialTheme.typography.titleMedium)
            }
            Text("AI Provider: $aiStatusLabel", style = MaterialTheme.typography.bodySmall)
            Text("Live API Probe: $apiHealthStatus", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Button(
              onClick = onRunHealthCheck,
              modifier = Modifier.testTag("admin_health_check_button"),
            ) {
              Text("Run Live API Health Check")
            }
          }
        }
      }

      item {
        Text("User Management (${allUsers.size})", style = MaterialTheme.typography.titleLarge)
      }

      items(allUsers, key = { it.id.ifBlank { it.userId } }) { user ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(user.fullName, style = MaterialTheme.typography.titleSmall)
                StatusPill(text = user.role)
              }
              Text(
                text = "${user.email} • ${user.district}, ${user.state}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "Joined: ${formatFirestoreTime(fmt, user.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (user.userId != currentFarmer.userId) {
              Switch(
                checked = user.active,
                onCheckedChange = { onToggleUserActive(user.userId, it) },
              )
            }
          }
        }
      }

      item {
        Text("Recent Platform Activity", style = MaterialTheme.typography.titleLarge)
      }

      if (platformActivityState is FirestoreUiState.Error) {
        item {
          Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(platformActivityState.message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      items(recentActivities.take(15), key = { it.id.ifBlank { it.hashCode().toString() } }) { log ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text("${log.module} • ${log.actionTitle}", style = MaterialTheme.typography.labelLarge)
            Text(
              "${log.details} (${formatFirestoreTime(fmt, log.createdAt)})",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StatMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
  }
}
