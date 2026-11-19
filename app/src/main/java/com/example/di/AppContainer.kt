package com.example.di

import android.content.Context
import com.example.R
import com.example.data.ai.AiRepository
import com.example.data.ai.GeminiAiRepository
import com.example.data.local.SessionPreferences
import com.example.data.remote.NetworkModule
import com.example.data.repository.AgriDataRepository
import com.example.data.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
  private val appContext = context.applicationContext
  private val firestoreDatabaseId: String by lazy {
    appContext.getString(R.string.firestore_database_id)
  }
  val firestore: FirebaseFirestore by lazy {
    FirebaseFirestore.getInstance(firestoreDatabaseId)
  }
  private val prefs: SessionPreferences by lazy { SessionPreferences(appContext) }
  private val aiRepository: AiRepository by lazy { GeminiAiRepository(appContext) }

  val authRepository: AuthRepository by lazy {
    AuthRepository(
      db = firestore,
      prefs = prefs,
    )
  }

  val agriDataRepository: AgriDataRepository by lazy {
    AgriDataRepository(
      openMeteoApi = NetworkModule.openMeteoApi,
      agmarknetApi = NetworkModule.agmarknetApi,
      aiRepository = aiRepository,
      db = firestore,
    )
  }
}
