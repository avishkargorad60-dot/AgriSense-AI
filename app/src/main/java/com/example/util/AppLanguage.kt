package com.example.util

enum class AppLanguage(
  val code: String,
  val displayName: String,
  val nativeLabel: String,
  val localeTag: String,
) {
  EN("en", "English", "English", "en-IN"),
  HI("hi", "Hindi", "हिन्दी", "hi-IN"),
  MR("mr", "Marathi", "मराठी", "mr-IN");

  companion object {
    fun fromCode(code: String?): AppLanguage =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
  }
}
