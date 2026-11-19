package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val current: OpenMeteoCurrent? = null,
  val daily: OpenMeteoDaily? = null,
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrent(
  val time: String = "",
  @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
  @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int = 0,
  val precipitation: Double = 0.0,
  val rain: Double = 0.0,
  @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
  @Json(name = "weather_code") val weatherCode: Int = 0,
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDaily(
  val time: List<String> = emptyList(),
  @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
  @Json(name = "temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
  @Json(name = "temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
  @Json(name = "precipitation_sum") val precipitationSum: List<Double> = emptyList(),
  @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int> = emptyList(),
)

interface OpenMeteoApi {
  @GET("v1/forecast")
  suspend fun getForecast(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("current") current: String = "temperature_2m,relative_humidity_2m,precipitation,rain,wind_speed_10m,weather_code",
    @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max",
    @Query("timezone") timezone: String = "Asia/Kolkata",
    @Query("forecast_days") forecastDays: Int = 7,
  ): OpenMeteoResponse
}
