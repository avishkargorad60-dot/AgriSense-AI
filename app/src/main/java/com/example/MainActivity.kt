package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.signOutFromFirebaseAndCredentials
import com.example.data.model.FarmerProfile
import com.example.di.AppContainer
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CompleteProfileScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FarmLabScreen
import com.example.ui.screens.KnowledgeCenterScreen
import com.example.ui.screens.MandiMarketScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RecordsHistoryScreen
import com.example.ui.screens.SessionStatusFallbackScreen
import com.example.ui.theme.AgriSenseTheme
import com.example.ui.viewmodel.AgriViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ProfileUiState
import com.example.ui.viewmodel.dataOrEmpty
import com.example.util.AppLanguage
import com.example.util.AppStringsProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

object AgriRoutes {
  const val HOME = "home"
  const val ASSISTANT = "assistant"
  const val FARM_LAB = "farm_lab"
  const val MARKET = "market"
  const val RECORDS = "records"
  const val KNOWLEDGE = "knowledge"
  const val PROFILE = "profile"
  const val ADMIN = "admin"
}

private data class NavTabItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

class MainActivity : ComponentActivity() {
  private val appContainer by lazy { AppContainer(applicationContext) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase App Check with launch intent before any UI or Firebase calls.
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    setContent {
      AgriSenseTheme {
        AgriSenseRoot(appContainer = appContainer)
      }
    }
  }
}

@Composable
fun AgriSenseRoot(appContainer: AppContainer) {
  val auth = remember { Firebase.auth }
  val currentFirebaseUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val selectedLanguage by appContainer.authRepository.selectedLanguage
    .collectAsStateWithLifecycle(initialValue = AppLanguage.EN)
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val firebaseUser = currentFirebaseUser
  if (firebaseUser == null) {
    // Unauthenticated gate: no Firestore listeners or feature ViewModels are created here.
    AuthScreen(
      selectedLanguage = selectedLanguage,
      onSelectLanguage = { lang ->
        scope.launch { appContainer.authRepository.setLanguage(lang) }
      },
      onAuthSuccess = { /* AuthStateListener updates currentFirebaseUser reactively */ },
    )
  } else {
    val uid = firebaseUser.uid
    key(uid) {
      val authViewModel: AuthViewModel = viewModel(
        key = "auth_$uid",
        factory = AuthViewModel.Factory(appContainer.authRepository, uid),
      )
      val profileState by authViewModel.profileState.collectAsStateWithLifecycle()
      val actionState by authViewModel.actionState.collectAsStateWithLifecycle()
      val vmLanguage by authViewModel.selectedLanguage.collectAsStateWithLifecycle()

      val performSignOut: () -> Unit = {
        signOutFromFirebaseAndCredentials(
          credentialManager = credentialManager,
          scope = scope,
        )
      }

      when (val state = profileState) {
        is ProfileUiState.Loading -> {
          SessionStatusFallbackScreen(
            title = "Loading Farm Profile",
            message = "Connecting to Cloud Firestore...",
            isLoading = true,
            onSignOut = performSignOut,
          )
        }
        is ProfileUiState.Error -> {
          SessionStatusFallbackScreen(
            title = "Profile Load Error",
            message = state.message,
            isLoading = false,
            onSignOut = performSignOut,
          )
        }
        is ProfileUiState.NeedsOnboarding -> {
          CompleteProfileScreen(
            defaultName = firebaseUser.displayName.orEmpty(),
            defaultEmail = firebaseUser.email.orEmpty(),
            selectedLanguage = vmLanguage,
            actionState = actionState,
            onSelectLanguage = authViewModel::setLanguage,
            onSubmitProfile = { fullName, mobile, email, stateName, district, village, lang, farmSize, mainCrops, soilType ->
              authViewModel.createProfile(
                fullName = fullName,
                mobile = mobile,
                email = email,
                state = stateName,
                district = district,
                village = village,
                language = lang,
                farmSizeAcres = farmSize,
                mainCrops = mainCrops,
                soilType = soilType,
              )
            },
            onSignOut = performSignOut,
          )
        }
        is ProfileUiState.Ready -> {
          val farmer = state.profile
          LaunchedEffect(farmer.userId, farmer.role) {
            authViewModel.syncBootstrappedAdminIfNeeded(farmer)
          }

          if (!farmer.active) {
            SessionStatusFallbackScreen(
              title = "Account Deactivated",
              message = "This farmer account has been deactivated by an administrator.",
              isLoading = false,
              onSignOut = performSignOut,
            )
          } else {
            val isAdminUser = farmer.role == "ADMIN" || appContainer.authRepository.isBootstrappedAdmin()
            val agriViewModel: AgriViewModel = viewModel(
              key = "agri_${uid}_$isAdminUser",
              factory = AgriViewModel.Factory(appContainer.agriDataRepository, uid, isAdminUser),
            )
            AuthenticatedAppShell(
              farmer = farmer,
              language = vmLanguage,
              authViewModel = authViewModel,
              agriViewModel = agriViewModel,
              onLogout = performSignOut,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
  farmer: FarmerProfile,
  language: AppLanguage,
  authViewModel: AuthViewModel,
  agriViewModel: AgriViewModel,
  onLogout: () -> Unit,
) {
  val strings = AppStringsProvider.forLanguage(language)
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: AgriRoutes.HOME
  var farmLabInitialTab by remember { mutableIntStateOf(0) }

  val authActionState by authViewModel.actionState.collectAsStateWithLifecycle()
  val allUsersState by authViewModel.allUsersState.collectAsStateWithLifecycle()

  val weatherState by agriViewModel.weatherState.collectAsStateWithLifecycle()
  val marketState by agriViewModel.marketState.collectAsStateWithLifecycle()
  val chatMessagesState by agriViewModel.chatMessagesState.collectAsStateWithLifecycle()
  val isAiThinking by agriViewModel.isAiThinking.collectAsStateWithLifecycle()
  val streamingText by agriViewModel.streamingAssistantText.collectAsStateWithLifecycle()
  val chatError by agriViewModel.chatError.collectAsStateWithLifecycle()

  val soilTaskState by agriViewModel.soilTaskState.collectAsStateWithLifecycle()
  val diseaseTaskState by agriViewModel.diseaseTaskState.collectAsStateWithLifecycle()
  val cropRecs by agriViewModel.cropRecommendations.collectAsStateWithLifecycle()
  val irrigationAdvice by agriViewModel.irrigationAdvice.collectAsStateWithLifecycle()

  val soilHistoryState by agriViewModel.soilHistoryState.collectAsStateWithLifecycle()
  val diseaseHistoryState by agriViewModel.diseaseHistoryState.collectAsStateWithLifecycle()
  val cropRecHistoryState by agriViewModel.cropRecHistoryState.collectAsStateWithLifecycle()
  val weatherAdvisoriesState by agriViewModel.weatherAdvisoriesState.collectAsStateWithLifecycle()
  val savedCropsState by agriViewModel.savedCropsState.collectAsStateWithLifecycle()
  val activityLogsState by agriViewModel.activityLogsState.collectAsStateWithLifecycle()
  val platformActivityState by agriViewModel.platformActivityState.collectAsStateWithLifecycle()
  val apiHealthStatus by agriViewModel.apiHealthStatus.collectAsStateWithLifecycle()

  LaunchedEffect(farmer.userId, farmer.state, farmer.district) {
    agriViewModel.loadWeatherForFarmer(farmer, language, saveAdvisory = false)
  }

  val navItems = remember(strings) {
    listOf(
      NavTabItem(AgriRoutes.HOME, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
      NavTabItem(AgriRoutes.ASSISTANT, strings.navAssistant, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_assistant"),
      NavTabItem(AgriRoutes.FARM_LAB, strings.navTools, Icons.Filled.Science, Icons.Outlined.Science, "nav_farmlab"),
      NavTabItem(AgriRoutes.MARKET, strings.navMarket, Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_market"),
      NavTabItem(AgriRoutes.RECORDS, strings.navHistory, Icons.Filled.History, Icons.Outlined.History, "nav_records"),
    )
  }

  val isSecondaryRoute = currentRoute in setOf(AgriRoutes.KNOWLEDGE, AgriRoutes.PROFILE, AgriRoutes.ADMIN)

  fun navigateTab(route: String) {
    navController.navigate(route) {
      popUpTo(AgriRoutes.HOME) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 840.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        if (isSecondaryRoute) {
          val title = when (currentRoute) {
            AgriRoutes.KNOWLEDGE -> strings.knowledgeCenterTitle
            AgriRoutes.PROFILE -> strings.profileTitle
            AgriRoutes.ADMIN -> strings.adminDashboardTitle
            else -> strings.appName
          }
          TopAppBar(
            title = { Text(title) },
            navigationIcon = {
              IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.testTag("secondary_back_button"),
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            },
          )
        }
      },
      bottomBar = {
        if (!useNavRail && !isSecondaryRoute) {
          NavigationBar {
            navItems.forEach { item ->
              val selected = currentRoute == item.route
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTab(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label, maxLines = 1) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useNavRail && !isSecondaryRoute) {
          NavigationRail {
            navItems.forEach { item ->
              val selected = currentRoute == item.route
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTab(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          NavHost(
            navController = navController,
            startDestination = AgriRoutes.HOME,
          ) {
            composable(AgriRoutes.HOME) {
              DashboardScreen(
                farmer = farmer,
                language = language,
                weatherState = weatherState,
                marketState = marketState,
                latestSoil = soilHistoryState.dataOrEmpty().firstOrNull(),
                latestDisease = diseaseHistoryState.dataOrEmpty().firstOrNull(),
                onRefreshWeather = {
                  agriViewModel.loadWeatherForFarmer(farmer, language, saveAdvisory = true)
                },
                onUseDeviceLocation = { lat, lon ->
                  agriViewModel.loadWeather("Current GPS Location", lat, lon, language, saveAdvisory = true)
                },
                onOpenAiAssistant = { navigateTab(AgriRoutes.ASSISTANT) },
                onOpenFarmLab = { tabIdx ->
                  farmLabInitialTab = tabIdx
                  navigateTab(AgriRoutes.FARM_LAB)
                },
                onOpenMarket = { navigateTab(AgriRoutes.MARKET) },
                onOpenKnowledge = { navController.navigate(AgriRoutes.KNOWLEDGE) },
                onOpenProfile = { navController.navigate(AgriRoutes.PROFILE) },
                onOpenAdmin = { navController.navigate(AgriRoutes.ADMIN) },
              )
            }

            composable(AgriRoutes.ASSISTANT) {
              AiAssistantScreen(
                messages = chatMessagesState.dataOrEmpty(),
                isThinking = isAiThinking,
                streamingText = streamingText,
                chatError = chatError,
                language = language,
                onSendQuestion = { q -> agriViewModel.sendAiQuestion(q, language, farmer) },
                onClearHistory = agriViewModel::clearChatHistory,
                onDismissError = agriViewModel::clearChatError,
              )
            }

            composable(AgriRoutes.FARM_LAB) {
              FarmLabScreen(
                initialTab = farmLabInitialTab,
                farmer = farmer,
                language = language,
                weatherState = weatherState,
                soilTaskState = soilTaskState,
                diseaseTaskState = diseaseTaskState,
                cropRecommendations = cropRecs,
                irrigationAdvice = irrigationAdvice,
                onAnalyzeSoil = { uri, hint, ph, moisture ->
                  agriViewModel.analyzeSoil(uri, hint, ph, moisture, language)
                },
                onDetectDisease = { uri, crop, symptoms ->
                  agriViewModel.detectDisease(uri, crop, symptoms, language)
                },
                onGenerateCropPlan = agriViewModel::generateCropRecommendations,
                onSaveCrop = agriViewModel::saveCrop,
                onComputeIrrigation = agriViewModel::computeIrrigation,
              )
            }

            composable(AgriRoutes.MARKET) {
              MandiMarketScreen(
                farmer = farmer,
                marketState = marketState,
                onFetchPrices = agriViewModel::fetchMarketPrices,
              )
            }

            composable(AgriRoutes.RECORDS) {
              RecordsHistoryScreen(
                soilHistoryState = soilHistoryState,
                diseaseHistoryState = diseaseHistoryState,
                cropRecHistoryState = cropRecHistoryState,
                weatherAdvisoriesState = weatherAdvisoriesState,
                savedCropsState = savedCropsState,
                activityLogsState = activityLogsState,
                onDeleteSoil = agriViewModel::deleteSoilRecord,
                onDeleteDisease = agriViewModel::deleteDiseaseRecord,
                onDeleteCropRec = agriViewModel::deleteCropRecRecord,
                onDeleteWeather = agriViewModel::deleteWeatherRecord,
                onDeleteSavedCrop = agriViewModel::deleteSavedCrop,
                onDeleteActivity = agriViewModel::deleteActivityLog,
              )
            }

            composable(AgriRoutes.KNOWLEDGE) {
              KnowledgeCenterScreen()
            }

            composable(AgriRoutes.PROFILE) {
              ProfileScreen(
                farmer = farmer,
                language = language,
                actionState = authActionState,
                onSelectLanguage = authViewModel::setLanguage,
                onUpdateProfile = authViewModel::updateProfile,
                onOpenKnowledge = { navController.navigate(AgriRoutes.KNOWLEDGE) },
                onOpenAdmin = { navController.navigate(AgriRoutes.ADMIN) },
                onLogout = onLogout,
              )
            }

            composable(AgriRoutes.ADMIN) {
              AdminDashboardScreen(
                currentFarmer = farmer,
                allUsersState = allUsersState,
                platformActivityState = platformActivityState,
                aiStatusLabel = agriViewModel.aiStatusLabel,
                apiHealthStatus = apiHealthStatus,
                onRunHealthCheck = agriViewModel::runApiHealthCheck,
                onToggleUserActive = authViewModel::toggleUserActive,
              )
            }
          }
        }
      }
    }
  }
}
