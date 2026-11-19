package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.DiseaseDetectionRecord
import com.example.data.model.FarmerProfile
import com.example.data.model.SoilAnalysisRecord
import com.example.data.repository.LiveWeatherUi
import com.example.data.repository.MarketPricesResult
import com.example.ui.components.ForecastBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.FreshEmerald
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.HarvestGoldContainer
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SkyBlueContainer
import com.example.ui.viewmodel.MarketUiState
import com.example.ui.viewmodel.WeatherUiState
import com.example.util.AppLanguage
import com.example.util.AppStringsProvider
import com.google.android.gms.location.LocationServices
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
  farmer: FarmerProfile,
  language: AppLanguage,
  weatherState: WeatherUiState,
  marketState: MarketUiState,
  latestSoil: SoilAnalysisRecord?,
  latestDisease: DiseaseDetectionRecord?,
  onRefreshWeather: () -> Unit,
  onUseDeviceLocation: (Double, Double) -> Unit,
  onOpenAiAssistant: () -> Unit,
  onOpenFarmLab: (Int) -> Unit,
  onOpenMarket: () -> Unit,
  onOpenKnowledge: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenAdmin: () -> Unit,
) {
  val strings = AppStringsProvider.forLanguage(language)
  val context = LocalContext.current
  val greeting = remember(language) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    when {
      hour < 12 -> strings.greetingMorning
      hour < 17 -> strings.greetingAfternoon
      else -> strings.greetingEvening
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      fetchCoarseLocation(context, onUseDeviceLocation, onRefreshWeather)
    }
  }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 860.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Top Greeting & Profile Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "$greeting, ${farmer.fullName.substringBefore(" ")}!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Farm location",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "${farmer.village}, ${farmer.district}, ${farmer.state} • ${farmer.farmSizeAcres} ac",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (farmer.role == "ADMIN") {
            IconButton(
              onClick = onOpenAdmin,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .background(HarvestGoldContainer)
                .testTag("dashboard_admin_button"),
            ) {
              Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Open Admin Dashboard",
                tint = HarvestGold,
              )
            }
          }
          IconButton(
            onClick = onOpenProfile,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer)
              .testTag("dashboard_profile_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Open Farmer Profile",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      // Hero AI Assistant Shortcut Card
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(168.dp)
          .clip(RoundedCornerShape(24.dp))
          .clickable(onClick = onOpenAiAssistant)
          .testTag("dashboard_ai_hero_card"),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_farm_hero_1795086583228),
          contentDescription = "AgriSense AI Companion",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.horizontalGradient(
                listOf(ForestGreen.copy(alpha = 0.92f), ForestGreen.copy(alpha = 0.50f), Color.Transparent)
              )
            )
            .padding(18.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            StatusPill(
              text = "${farmer.soilType} • ${farmer.mainCrops}",
              containerColor = Color.White.copy(alpha = 0.18f),
              contentColor = Color.White,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = strings.askAiShortcut,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
              )
              FilledTonalButton(
                onClick = onOpenAiAssistant,
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = HarvestGold,
                  contentColor = Color.White,
                ),
                modifier = Modifier.testTag("dashboard_ask_ai_button"),
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Chat / Voice Ask")
              }
            }
          }
        }
      }

      // Live Weather Summary Card
      SectionHeader(
        title = strings.weatherSummary,
        actionText = "Refresh",
        onActionClick = onRefreshWeather,
      )

      WeatherDashboardCard(
        weatherState = weatherState,
        strings = strings,
        onRefresh = onRefreshWeather,
        onGpsClick = {
          val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
          ) == PackageManager.PERMISSION_GRANTED
          if (granted) {
            fetchCoarseLocation(context, onUseDeviceLocation, onRefreshWeather)
          } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
          }
        },
      )

      // Important Alerts & Advisories (when weather loaded)
      if (weatherState is WeatherUiState.Success) {
        val w = weatherState.data
        if (w.alerts.isNotEmpty()) {
          Card(
            colors = CardDefaults.cardColors(containerColor = HarvestGoldContainer),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = "Weather alert", tint = HarvestGold)
                Text(
                  text = strings.importantAlerts,
                  style = MaterialTheme.typography.titleSmall,
                  color = Color(0xFF4A3000),
                )
              }
              w.alerts.forEach { alert ->
                Text("• $alert", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A3000))
              }
            }
          }
        }

        // 7-Day Forecast Chart
        ForecastBarChart(forecast = w.dailyForecast)

        // Today's Farming Insights & Recommended Activities
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(strings.todayInsights, style = MaterialTheme.typography.titleMedium)
              StatusPill(
                text = strings.advisoryBadge,
                containerColor = SkyBlueContainer,
                contentColor = SkyBlue,
              )
            }
            w.generatedAdvisories.forEach { tip ->
              Text("• $tip", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
              text = strings.recommendedActivities,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "1. Scout ${farmer.mainCrops.substringBefore(",")} leaves early morning.\n2. Verify moisture at 10 cm root depth before irrigation.\n3. Keep field bunds and drainage outlets clear.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Soil Health & Crop Health Overview Row
      SectionHeader(title = "Farm Diagnostics & Lab")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        VisualFeatureCard(
          title = strings.soilHealthOverview,
          subtitle = latestSoil?.let { "${it.predictedCategory} • ${it.cropRecommendations.substringBefore(",")}" }
            ?: "Analyze soil image & pH",
          imageRes = R.drawable.img_soil_analysis_1795086583671,
          icon = Icons.Default.Science,
          onClick = { onOpenFarmLab(0) },
          modifier = Modifier.weight(1f).testTag("card_soil_overview"),
        )
        VisualFeatureCard(
          title = strings.cropHealthInfo,
          subtitle = latestDisease?.predictedDisease ?: "Scan leaves for diseases",
          imageRes = R.drawable.img_crop_health_1795086583591,
          icon = Icons.Default.Biotech,
          onClick = { onOpenFarmLab(1) },
          modifier = Modifier.weight(1f).testTag("card_crop_health"),
        )
      }

      // Quick Action Pills for Crop Advisor, Irrigation, Knowledge Center
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        QuickModuleTile(
          title = strings.cropAdvisorTitle,
          icon = Icons.Default.Agriculture,
          onClick = { onOpenFarmLab(2) },
          modifier = Modifier.testTag("quick_crop_advisor"),
        )
        QuickModuleTile(
          title = strings.irrigationAdvisorTitle,
          icon = Icons.Default.WaterDrop,
          onClick = { onOpenFarmLab(3) },
          modifier = Modifier.testTag("quick_irrigation_advisor"),
        )
        QuickModuleTile(
          title = strings.knowledgeCenterTitle,
          icon = Icons.AutoMirrored.Filled.MenuBook,
          onClick = onOpenKnowledge,
          modifier = Modifier.testTag("quick_knowledge_center"),
        )
      }

      // Market Price Highlights Card
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onOpenMarket)
          .testTag("dashboard_market_card"),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(strings.marketHighlights, style = MaterialTheme.typography.titleMedium)
            }
            Text("Open Mandi", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
          }
          when (marketState) {
            is MarketUiState.Result -> {
              when (val res = marketState.data) {
                is MarketPricesResult.Success -> {
                  res.records.take(3).forEach { rec ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                      Text("${rec.commodity} (${rec.market})", style = MaterialTheme.typography.bodySmall)
                      Text(
                        "₹${rec.modalPrice}/qtl",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FreshEmerald,
                      )
                    }
                  }
                }
                is MarketPricesResult.Unavailable -> {
                  Text(
                    text = res.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
            is MarketUiState.Loading -> Text("Checking Agmarknet mandi rates...", style = MaterialTheme.typography.bodySmall)
            is MarketUiState.Idle -> Text(
              "Tap to search live Agmarknet mandi prices by state, district, and commodity.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(Modifier.height(12.dp))
    }
  }
}

@Composable
private fun WeatherDashboardCard(
  weatherState: WeatherUiState,
  strings: com.example.util.AgriStrings,
  onRefresh: () -> Unit,
  onGpsClick: () -> Unit,
) {
  ElevatedCard(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    modifier = Modifier.fillMaxWidth(),
  ) {
    when (weatherState) {
      is WeatherUiState.Loading -> {
        Row(
          modifier = Modifier.fillMaxWidth().padding(22.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
          Spacer(Modifier.width(12.dp))
          Text("Fetching live weather from Open-Meteo...", style = MaterialTheme.typography.bodyMedium)
        }
      }
      is WeatherUiState.Error -> {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(weatherState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
          OutlinedButton(onClick = onRefresh) { Text("Retry Live Weather") }
        }
      }
      is WeatherUiState.Success -> {
        val w: LiveWeatherUi = weatherState.data
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              StatusPill(text = strings.liveWeatherBadge)
              Spacer(Modifier.height(4.dp))
              Text(w.locationName, style = MaterialTheme.typography.titleMedium)
            }
            Row {
              IconButton(
                onClick = onGpsClick,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("weather_gps_button"),
              ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Use device location")
              }
              IconButton(
                onClick = onRefresh,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("weather_refresh_button"),
              ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh weather")
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = HarvestGold,
                modifier = Modifier.size(40.dp),
              )
              Text(
                text = "${w.tempC.roundToInt()}°C",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              WeatherMetricRow(Icons.Default.WaterDrop, "Humidity: ${w.humidityPct}%")
              WeatherMetricRow(Icons.Default.Air, "Wind: ${w.windKmh.roundToInt()} km/h")
              WeatherMetricRow(Icons.Default.Thermostat, "Rain: ${"%.1f".format(w.rainMm)} mm")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WeatherMetricRow(icon: ImageVector, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun VisualFeatureCard(
  title: String,
  subtitle: String,
  imageRes: Int,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .height(175.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.fillMaxWidth().height(92.dp)) {
        Image(
          painter = painterResource(id = imageRes),
          contentDescription = title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
        Surface(
          shape = CircleShape,
          color = ForestGreen.copy(alpha = 0.85f),
          modifier = Modifier.padding(8.dp).align(Alignment.TopStart),
        ) {
          Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(6.dp).size(16.dp))
        }
      }
      Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun QuickModuleTile(
  title: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .width(155.dp)
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@SuppressLint("MissingPermission")
private fun fetchCoarseLocation(
  context: android.content.Context,
  onLocation: (Double, Double) -> Unit,
  onFallback: () -> Unit,
) {
  try {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.lastLocation
      .addOnSuccessListener { loc ->
        if (loc != null) {
          onLocation(loc.latitude, loc.longitude)
        } else {
          onFallback()
        }
      }
      .addOnFailureListener { onFallback() }
  } catch (_: Exception) {
    onFallback()
  }
}
