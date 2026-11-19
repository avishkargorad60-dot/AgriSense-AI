package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.onGoogleSignInClicked
import com.example.ui.components.LanguageSelectorRow
import com.example.ui.components.SimpleDropdownSelector
import com.example.ui.theme.ForestGreen
import com.example.ui.viewmodel.AuthActionState
import com.example.util.AppLanguage
import com.example.util.AppStringsProvider
import com.example.util.IndianAgriData
import com.example.util.IndianSoilType

@Composable
fun AuthScreen(
  selectedLanguage: AppLanguage,
  onSelectLanguage: (AppLanguage) -> Unit,
  onAuthSuccess: () -> Unit,
) {
  val strings = AppStringsProvider.forLanguage(selectedLanguage)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isCheckingSilent by remember { mutableStateOf(true) }
  var isSigningIn by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isCheckingSilent = false
        onAuthSuccess()
      },
      onUnauthenticated = {
        isCheckingSilent = false
      },
      scope = scope,
    )
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 600.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Hero Banner
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(24.dp)),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_farm_hero_1795086583228),
            contentDescription = "AgriSense AI farmland sunrise",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color.Black.copy(alpha = 0.15f), ForestGreen.copy(alpha = 0.90f))
                )
              )
              .padding(20.dp),
            contentAlignment = Alignment.BottomStart,
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = strings.appName,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
              )
              Text(
                text = strings.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
              )
            }
          }
        }

        LanguageSelectorRow(selected = selectedLanguage, onSelect = onSelectLanguage)

        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
          Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.loginTitle, style = MaterialTheme.typography.headlineSmall)
            Text(
              text = "Sign in securely with your Google account to sync your farm profile, soil diagnostics, crop disease scans, and AI Mitra chats across devices.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AuthFeatureRow(Icons.Default.AutoAwesome, "Gemini AI Mitra in English, Hindi & Marathi")
            AuthFeatureRow(Icons.Default.Science, "Multimodal Soil & Crop Disease Vision Analysis")
            AuthFeatureRow(Icons.Default.CloudDone, "Cloud Firestore sync + Live Weather & Agmarknet")

            if (!errorMessage.isNullOrBlank()) {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
              ) {
                Text(
                  text = errorMessage!!,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }

            Spacer(Modifier.height(4.dp))

            Button(
              onClick = {
                errorMessage = null
                isSigningIn = true
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    isSigningIn = false
                    onAuthSuccess()
                  },
                  onAuthError = { msg ->
                    isSigningIn = false
                    errorMessage = msg
                  },
                  scope = scope,
                  onAuthCancelled = {
                    isSigningIn = false
                  },
                )
              },
              enabled = !isSigningIn && !isCheckingSilent,
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .minimumInteractiveComponentSize()
                .testTag("google_signin_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              if (isSigningIn || isCheckingSilent) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.5.dp,
                )
                Spacer(Modifier.width(10.dp))
                Text(if (isCheckingSilent) "Checking session..." else "Signing in...")
              } else {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AuthFeatureRow(icon: ImageVector, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun CompleteProfileScreen(
  defaultName: String,
  defaultEmail: String,
  selectedLanguage: AppLanguage,
  actionState: AuthActionState,
  onSelectLanguage: (AppLanguage) -> Unit,
  onSubmitProfile: (
    fullName: String,
    mobile: String,
    email: String,
    state: String,
    district: String,
    village: String,
    language: AppLanguage,
    farmSizeAcres: Double,
    mainCrops: String,
    soilType: String,
  ) -> Unit,
  onSignOut: () -> Unit,
) {
  val strings = AppStringsProvider.forLanguage(selectedLanguage)
  var fullName by remember(defaultName) { mutableStateOf(defaultName.ifBlank { "Farmer" }) }
  var mobile by remember { mutableStateOf("") }
  var email by remember(defaultEmail) { mutableStateOf(defaultEmail) }
  var selectedState by remember { mutableStateOf(IndianAgriData.states.first()) }
  val districts = remember(selectedState) { IndianAgriData.districtsForState(selectedState).map { it.district } }
  var selectedDistrict by remember(selectedState) { mutableStateOf(districts.firstOrNull() ?: "Pune") }
  var village by remember { mutableStateOf("") }
  var farmSize by remember { mutableStateOf("3.5") }
  var mainCrops by remember { mutableStateOf("Soybean, Wheat") }
  var soilType by remember { mutableStateOf(IndianSoilType.BLACK.labelEn) }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 620.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(strings.registerTitle, style = MaterialTheme.typography.headlineSmall)
            Text(
              text = "Tell AgriSense AI about your farm for localized recommendations.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          TextButton(onClick = onSignOut, modifier = Modifier.testTag("onboarding_signout_button")) {
            Text(strings.logoutButton)
          }
        }

        LanguageSelectorRow(selected = selectedLanguage, onSelect = onSelectLanguage)

        if (actionState is AuthActionState.Error) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text(
              text = actionState.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        val isLoading = actionState is AuthActionState.Loading

        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = fullName,
              onValueChange = { fullName = it },
              label = { Text(strings.fullNameLabel) },
              leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("register_name_input"),
            )
            OutlinedTextField(
              value = mobile,
              onValueChange = { mobile = it },
              label = { Text(strings.mobileLabel) },
              leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("register_mobile_input"),
            )
            OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text(strings.emailLabel) },
              leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("register_email_input"),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              SimpleDropdownSelector(
                label = strings.stateLabel,
                selectedValue = selectedState,
                options = IndianAgriData.states,
                onSelect = { selectedState = it },
                testTag = "register_state_dropdown",
                modifier = Modifier.weight(1f),
              )
              SimpleDropdownSelector(
                label = strings.districtLabel,
                selectedValue = selectedDistrict,
                options = districts,
                onSelect = { selectedDistrict = it },
                testTag = "register_district_dropdown",
                modifier = Modifier.weight(1f),
              )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = village,
                onValueChange = { village = it },
                label = { Text(strings.villageLabel) },
                singleLine = true,
                modifier = Modifier.weight(1.3f).testTag("register_village_input"),
              )
              OutlinedTextField(
                value = farmSize,
                onValueChange = { farmSize = it },
                label = { Text(strings.farmSizeLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("register_farmsize_input"),
              )
            }

            OutlinedTextField(
              value = mainCrops,
              onValueChange = { mainCrops = it },
              label = { Text(strings.mainCropsLabel) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("register_crops_input"),
            )

            SimpleDropdownSelector(
              label = strings.soilTypeLabel,
              selectedValue = soilType,
              options = IndianSoilType.entries.map { it.labelEn },
              onSelect = { soilType = it },
              testTag = "register_soil_dropdown",
            )

            Button(
              onClick = {
                onSubmitProfile(
                  fullName,
                  mobile,
                  email,
                  selectedState,
                  selectedDistrict,
                  village,
                  selectedLanguage,
                  farmSize.toDoubleOrNull() ?: 2.0,
                  mainCrops,
                  soilType,
                )
              },
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .minimumInteractiveComponentSize()
                .testTag("complete_profile_submit_button"),
              shape = RoundedCornerShape(14.dp),
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp,
                )
              } else {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save & Enter AgriSense AI")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun SessionStatusFallbackScreen(
  title: String,
  message: String,
  isLoading: Boolean = false,
  onSignOut: (() -> Unit)? = null,
) {
  Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.widthIn(max = 460.dp),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (isLoading) {
            CircularProgressIndicator()
          }
          Text(title, style = MaterialTheme.typography.titleLarge)
          Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (onSignOut != null) {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.minimumInteractiveComponentSize()) {
              Text("Sign Out")
            }
          }
        }
      }
    }
  }
}
