package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.local.SessionPreferences
import com.example.data.model.FarmerProfile
import com.example.util.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val BOOTSTRAPPED_ADMIN_EMAIL = "avishkargorad60@gmail.com"

class AuthRepository(
  private val db: FirebaseFirestore,
  private val prefs: SessionPreferences,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  val selectedLanguage: Flow<AppLanguage> = prefs.language

  suspend fun setLanguage(language: AppLanguage) {
    prefs.setLanguage(language)
  }

  private fun requireUserId(): String =
    auth.currentUser?.uid?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun isBootstrappedAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    return user.isEmailVerified && user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)
  }

  fun observeProfile(userId: String): Flow<FarmerProfile?> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
    val docRef = db.collection("users").document(userId)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (!snapshot.exists()) {
            null
          } else {
            snapshot.toObject(FarmerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        }
    )
  }

  fun observeAllUsers(): Flow<List<FarmerProfile>> = flow {
    val uid = requireUserId()
    val colRef = db.collection("users")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(FarmerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
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

  suspend fun createProfile(
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
  ): Result<Unit> {
    var docPath = "users/unauthenticated"
    return try {
      val uid = requireUserId()
      val cleanName = fullName.trim()
      require(cleanName.isNotBlank()) { "Full name is required." }
      val cleanState = state.trim().ifBlank { "Maharashtra" }
      val cleanDistrict = district.trim().ifBlank { "Pune" }
      val cleanMobile = mobile.trim().take(24)
      val cleanEmail = email.trim().ifBlank { auth.currentUser?.email.orEmpty() }.take(160)
      val cleanVillage = village.trim().ifBlank { "Gram Panchayat" }.take(120)
      val cleanCrops = mainCrops.trim().ifBlank { "Soybean, Wheat" }.take(300)
      val cleanSoil = soilType.trim().ifBlank { "Black Soil (Regur)" }.take(80)
      val cleanSize = farmSizeAcres.coerceIn(0.1, 100000.0)

      val docRef = db.collection("users").document(uid)
      docPath = docRef.path

      val payload = mapOf(
        "userId" to uid,
        "fullName" to cleanName.take(120),
        "mobile" to cleanMobile,
        "email" to cleanEmail,
        "state" to cleanState.take(80),
        "district" to cleanDistrict.take(80),
        "village" to cleanVillage,
        "preferredLanguage" to language.code,
        "farmSizeAcres" to cleanSize,
        "mainCrops" to cleanCrops,
        "soilType" to cleanSoil,
        "role" to "FARMER",
        "active" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()

      if (isBootstrappedAdmin()) {
        docRef.update(
          mapOf(
            "role" to "ADMIN",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }

      prefs.setLanguage(language)
      logAuthActivity(
        uid = uid,
        actionTitle = "Profile Created",
        details = "$cleanName joined from $cleanDistrict, $cleanState",
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateProfile(profile: FarmerProfile): Result<Unit> {
    var docPath = "users/${profile.userId.ifBlank { "unauthenticated" }}"
    return try {
      val uid = requireUserId()
      check(uid == profile.userId || profile.userId.isBlank()) {
        "Signed-in user changed; refusing cross-user write."
      }
      val cleanName = profile.fullName.trim()
      require(cleanName.isNotBlank()) { "Name cannot be empty." }

      val docRef = db.collection("users").document(uid)
      docPath = docRef.path
      val updates = mapOf(
        "fullName" to cleanName.take(120),
        "mobile" to profile.mobile.trim().take(24),
        "email" to profile.email.trim().take(160),
        "state" to profile.state.trim().ifBlank { "Maharashtra" }.take(80),
        "district" to profile.district.trim().ifBlank { "Pune" }.take(80),
        "village" to profile.village.trim().take(120),
        "preferredLanguage" to AppLanguage.fromCode(profile.preferredLanguage).code,
        "farmSizeAcres" to profile.farmSizeAcres.coerceIn(0.0, 100000.0),
        "mainCrops" to profile.mainCrops.trim().take(300),
        "soilType" to profile.soilType.trim().ifBlank { "Black Soil (Regur)" }.take(80),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      prefs.setLanguage(AppLanguage.fromCode(profile.preferredLanguage))
      logAuthActivity(
        uid = uid,
        actionTitle = "Profile Updated",
        details = "${profile.district}, ${profile.state} • ${profile.farmSizeAcres} acres",
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun syncBootstrappedAdminRoleIfNeeded(profile: FarmerProfile): Result<Unit> {
    if (!isBootstrappedAdmin() || profile.role == "ADMIN") return Result.success(Unit)
    var docPath = "users/${profile.userId}"
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid)
      docPath = docRef.path
      docRef.update(
        mapOf(
          "role" to "ADMIN",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun toggleUserActive(targetUserId: String, active: Boolean): Result<Unit> {
    val docPath = "users/$targetUserId"
    return try {
      requireUserId()
      require(targetUserId.isNotBlank()) { "Target user ID is required." }
      db.collection("users").document(targetUserId)
        .update(
          mapOf(
            "active" to active,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  private suspend fun logAuthActivity(uid: String, actionTitle: String, details: String) {
    val payload = mapOf(
      "userId" to uid,
      "module" to "Profile",
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
}
