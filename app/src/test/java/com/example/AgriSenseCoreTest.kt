package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionPreferences
import com.example.data.model.ChatMessageRecord
import com.example.data.model.CropRecommendationRecord
import com.example.data.model.DiseaseDetectionRecord
import com.example.data.model.FarmerProfile
import com.example.data.model.SoilAnalysisRecord
import com.example.util.AppLanguage
import com.example.util.AppStringsProvider
import com.example.util.IndianAgriData
import com.example.util.IndianSeason
import com.example.util.IndianSoilType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgriSenseCoreTest {

  @Test
  fun `firebase applet config resource defines named firestore database id`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbId = context.getString(R.string.firestore_database_id)
    assertTrue(dbId.isNotBlank())
  }

  @Test
  fun `session preferences persist language across EN HI MR`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = SessionPreferences(context)
    prefs.setLanguage(AppLanguage.MR)
    assertEquals(AppLanguage.MR, prefs.language.first())
    prefs.setLanguage(AppLanguage.HI)
    assertEquals(AppLanguage.HI, prefs.language.first())
  }

  @Test
  fun `localized strings support English Hindi and Marathi`() {
    val en = AppStringsProvider.forLanguage(AppLanguage.EN)
    val hi = AppStringsProvider.forLanguage(AppLanguage.HI)
    val mr = AppStringsProvider.forLanguage(AppLanguage.MR)
    assertEquals("AgriSense AI", en.appName)
    assertTrue(hi.navAssistant.contains("AI"))
    assertTrue(mr.diseaseDisclaimer.contains("KVK"))
  }

  @Test
  fun `firestore models provide safe defaults and no-arg constructors`() {
    val farmer = FarmerProfile()
    val soil = SoilAnalysisRecord()
    val disease = DiseaseDetectionRecord()
    val rec = CropRecommendationRecord()
    val chat = ChatMessageRecord()
    assertEquals("FARMER", farmer.role)
    assertTrue(farmer.active)
    assertEquals("", soil.id)
    assertEquals("", disease.id)
    assertEquals(0L, rec.topScore)
    assertTrue(chat.liveAi)
  }

  @Test
  fun `indian agri catalog covers Kharif Rabi Zaid and all six soil types`() {
    val seasons = IndianAgriData.crops.flatMap { it.seasons }.toSet()
    assertTrue(IndianSeason.KHARIF in seasons)
    assertTrue(IndianSeason.RABI in seasons)
    assertTrue(IndianSeason.ZAID in seasons)
    assertEquals(6, IndianSoilType.entries.size)
    assertTrue(IndianAgriData.knowledgeArticles.any { it.isGovtScheme && !it.officialUrl.isNullOrBlank() })
  }
}
