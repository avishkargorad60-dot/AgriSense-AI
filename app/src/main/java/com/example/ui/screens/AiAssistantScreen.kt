package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatMessageRecord
import com.example.ui.components.AiNoticeBanner
import com.example.ui.components.StatusPill
import com.example.util.AppLanguage
import com.example.util.AppStringsProvider
import java.util.Locale

@Composable
fun AiAssistantScreen(
  messages: List<ChatMessageRecord>,
  isThinking: Boolean,
  streamingText: String?,
  chatError: String?,
  language: AppLanguage,
  onSendQuestion: (String) -> Unit,
  onClearHistory: () -> Unit,
  onDismissError: () -> Unit,
) {
  val strings = AppStringsProvider.forLanguage(language)
  val context = LocalContext.current
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  var ttsReady by remember { mutableStateOf(false) }
  val tts = remember {
    var engine: TextToSpeech? = null
    engine = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        ttsReady = true
        engine?.language = Locale.forLanguageTag(language.localeTag)
      }
    }
    engine
  }

  LaunchedEffect(language, ttsReady) {
    if (ttsReady) {
      tts.language = Locale.forLanguageTag(language.localeTag)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      tts.stop()
      tts.shutdown()
    }
  }

  val voiceLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
      if (!spoken.isNullOrBlank()) {
        inputText = spoken
      }
    }
  }

  LaunchedEffect(messages.size, streamingText) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  val samplePrompts = remember(language) {
    when (language) {
      AppLanguage.HI -> listOf(
        "कौन सी फसल उगाऊं?",
        "पत्तियां पीली क्यों हो रही हैं?",
        "सिंचाई कब करनी चाहिए?",
        "भारी बारिश में क्या करें?",
      )
      AppLanguage.MR -> listOf(
        "कोणते पीक घ्यावे?",
        "पाने पिवळी का पडत आहेत?",
        "शेताला पाणी कधी द्यावे?",
        "जास्त पावसात काय काळजी घ्यावी?",
      )
      AppLanguage.EN -> listOf(
        "Which crop should I grow?",
        "Why are my leaves turning yellow?",
        "What fertilizer is suitable?",
        "When should I irrigate?",
        "What to do during heavy rain?",
      )
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = 820.dp)
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(strings.navAssistant, style = MaterialTheme.typography.headlineSmall)
          Text(
            text = "Multilingual Farming Q&A (${language.nativeLabel})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (messages.isNotEmpty()) {
          IconButton(
            onClick = onClearHistory,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_chat_button"),
          ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear conversation history")
          }
        }
      }

      AiNoticeBanner(message = strings.aiNoticeBanner, isLiveAi = true)

      if (!chatError.isNullOrBlank()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = chatError,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismissError, modifier = Modifier.minimumInteractiveComponentSize()) {
              Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
          }
        }
      }

      // Quick Suggestions Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        samplePrompts.forEach { prompt ->
          AssistChip(
            onClick = { onSendQuestion(prompt) },
            label = { Text(prompt) },
          )
        }
      }

      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (messages.isEmpty() && streamingText == null) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
              Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ask AgriSense AI Mitra", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "Tap any suggestion chip above, type your farming question, or use the microphone button for voice input.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        items(messages, key = { it.id.ifBlank { "${it.role}_${it.hashCode()}" } }) { msg ->
          val isUser = msg.role == "user"
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
          ) {
            Surface(
              shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp,
              ),
              color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
              tonalElevation = if (isUser) 0.dp else 2.dp,
              modifier = Modifier.widthIn(max = 560.dp),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isUser) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    StatusPill(text = if (msg.liveAi) "GEMINI AI" else "AGRISENSE AI")
                    IconButton(
                      onClick = {
                        if (ttsReady) {
                          tts.speak(msg.message, TextToSpeech.QUEUE_FLUSH, null, "msg_${msg.id}")
                        }
                      },
                      modifier = Modifier.size(36.dp),
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Read answer aloud",
                        tint = MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                }
                Text(
                  text = msg.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
                if (!isUser && msg.followUps.isNotBlank()) {
                  val chips = msg.followUps.split("||").filter { it.isNotBlank() }
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = "Suggested follow-ups:",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontWeight = FontWeight.SemiBold,
                    )
                    chips.forEach { follow ->
                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { onSendQuestion(follow) },
                      ) {
                        Text(
                          text = follow,
                          style = MaterialTheme.typography.labelMedium,
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        if (!streamingText.isNullOrBlank()) {
          item(key = "streaming_bubble") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
              Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 2.dp,
                modifier = Modifier.widthIn(max = 560.dp),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  StatusPill(text = "GEMINI AI • STREAMING")
                  Text(text = streamingText, style = MaterialTheme.typography.bodyMedium)
                }
              }
            }
          }
        } else if (isThinking) {
          item(key = "thinking_row") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              Text("Consulting Gemini AI...", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }

      // Composer Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        IconButton(
          onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
              putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
              putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
              putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your farming question")
            }
            try {
              voiceLauncher.launch(intent)
            } catch (_: Exception) {
              Toast.makeText(context, "Voice recognition not available on this device.", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("ai_voice_input_button"),
        ) {
          Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = MaterialTheme.colorScheme.primary)
        }

        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text("Ask about crops, soil, pests, rain...") },
          singleLine = false,
          maxLines = 3,
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("ai_chat_input"),
        )

        FilledIconButton(
          onClick = {
            if (inputText.isNotBlank()) {
              onSendQuestion(inputText)
              inputText = ""
            }
          },
          enabled = inputText.isNotBlank() && !isThinking,
          shape = CircleShape,
          modifier = Modifier
            .size(48.dp)
            .testTag("ai_send_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send question")
        }
      }
      Spacer(Modifier.height(2.dp))
    }
  }
}
