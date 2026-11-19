package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.FarmerProfile
import com.example.data.repository.AuthRepository
import com.example.util.AppLanguage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

private const val TAG = "AuthVM"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface ProfileUiState {
  data object Loading : ProfileUiState
  data object NeedsOnboarding : ProfileUiState
  data class Ready(val profile: FarmerProfile) : ProfileUiState
  data class Error(val message: String) : ProfileUiState
}

sealed interface AuthActionState {
  data object Idle : AuthActionState
  data object Loading : AuthActionState
  data class Success(val message: String) : AuthActionState
  data class Error(val message: String) : AuthActionState
}

class AuthViewModel(
  private val authRepository: AuthRepository,
  private val currentUserId: String,
) : ViewModel() {

  val selectedLanguage: StateFlow<AppLanguage> = authRepository.selectedLanguage
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppLanguage.EN)

  val profileState: StateFlow<ProfileUiState> = authRepository.observeProfile(currentUserId)
    .map<FarmerProfile?, ProfileUiState> { profile ->
      if (profile == null) {
        ProfileUiState.NeedsOnboarding
      } else {
        ProfileUiState.Ready(profile)
      }
    }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing profile", rootError)
      emit(ProfileUiState.Error(rootError.message ?: "Failed to load farmer profile."))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = ProfileUiState.Loading,
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  val allUsersState: StateFlow<FirestoreUiState<List<FarmerProfile>>> = profileState
    .flatMapLatest { state ->
      val profile = (state as? ProfileUiState.Ready)?.profile
      val isAdmin = profile?.role == "ADMIN" || authRepository.isBootstrappedAdmin()
      if (!isAdmin) {
        flowOf(FirestoreUiState.Success(emptyList()))
      } else {
        authRepository.observeAllUsers()
          .map<List<FarmerProfile>, FirestoreUiState<List<FarmerProfile>>> { FirestoreUiState.Success(it) }
          .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing all users", rootError)
            emit(FirestoreUiState.Error(rootError.message ?: "Failed to load user directory."))
          }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = FirestoreUiState.Loading,
    )

  private val _actionState = MutableStateFlow<AuthActionState>(AuthActionState.Idle)
  val actionState: StateFlow<AuthActionState> = _actionState.asStateFlow()

  fun clearActionState() {
    _actionState.value = AuthActionState.Idle
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch {
      authRepository.setLanguage(language)
    }
  }

  fun syncBootstrappedAdminIfNeeded(profile: FarmerProfile) {
    if (authRepository.isBootstrappedAdmin() && profile.role != "ADMIN") {
      viewModelScope.launch {
        authRepository.syncBootstrappedAdminRoleIfNeeded(profile)
      }
    }
  }

  fun createProfile(
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
  ) {
    viewModelScope.launch {
      _actionState.value = AuthActionState.Loading
      val result = authRepository.createProfile(
        fullName = fullName,
        mobile = mobile,
        email = email,
        state = state,
        district = district,
        village = village,
        language = language,
        farmSizeAcres = farmSizeAcres,
        mainCrops = mainCrops,
        soilType = soilType,
      )
      _actionState.value = result.fold(
        onSuccess = { AuthActionState.Success("Farmer profile created!") },
        onFailure = { AuthActionState.Error(it.localizedMessage ?: "Failed to save profile.") },
      )
    }
  }

  fun updateProfile(updated: FarmerProfile) {
    viewModelScope.launch {
      _actionState.value = AuthActionState.Loading
      val result = authRepository.updateProfile(updated)
      _actionState.value = result.fold(
        onSuccess = { AuthActionState.Success("Profile saved to Cloud Firestore.") },
        onFailure = { AuthActionState.Error(it.localizedMessage ?: "Failed to update profile.") },
      )
    }
  }

  fun toggleUserActive(targetUserId: String, active: Boolean) {
    viewModelScope.launch {
      val result = authRepository.toggleUserActive(targetUserId, active)
      result.onFailure { err ->
        _actionState.value = AuthActionState.Error(err.localizedMessage ?: "Failed to update user status.")
      }
    }
  }

  class Factory(
    private val authRepository: AuthRepository,
    private val currentUserId: String,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      AuthViewModel(authRepository, currentUserId) as T
  }
}
