package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class AgmarknetResponse(
  val total: Int = 0,
  val count: Int = 0,
  val status: String? = null,
  val message: String? = null,
  val records: List<AgmarknetRecord> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AgmarknetRecord(
  val state: String = "",
  val district: String = "",
  val market: String = "",
  val commodity: String = "",
  val variety: String = "",
  val grade: String = "",
  @Json(name = "arrival_date") val arrivalDate: String = "",
  @Json(name = "min_price") val minPrice: String = "",
  @Json(name = "max_price") val maxPrice: String = "",
  @Json(name = "modal_price") val modalPrice: String = "",
)

interface AgmarknetApi {
  @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
  suspend fun getCommodityPrices(
    @Query("api-key") apiKey: String,
    @Query("format") format: String = "json",
    @Query("limit") limit: Int = 40,
    @Query("filters[state]") state: String? = null,
    @Query("filters[district]") district: String? = null,
    @Query("filters[commodity]") commodity: String? = null,
  ): AgmarknetResponse
}
