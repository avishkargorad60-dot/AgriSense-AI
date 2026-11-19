package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
  private val moshi: Moshi by lazy {
    Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

  private val okHttpClient: OkHttpClient by lazy {
    val logger = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .addInterceptor(logger)
      .build()
  }

  val openMeteoApi: OpenMeteoApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoApi::class.java)
  }

  val agmarknetApi: AgmarknetApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.data.gov.in/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(AgmarknetApi::class.java)
  }
}
