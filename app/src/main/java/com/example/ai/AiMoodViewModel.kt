package com.example.ai

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MoodEntryEntity
import com.example.domain.AnalyticsEngine
import com.example.domain.EmotionCatalog
import com.example.domain.ToymoteDate
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerGoAway
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLException
import kotlin.math.max
import kotlin.math.sqrt

private const val TAG = "AiMoodViewModel"
internal const val PRIMARY_LIVE_MODEL_NAME = "gemini-3.1-flash-live-preview"
internal const val FALLBACK_LIVE_MODEL_NAME = "gemini-2.5-flash-native-audio-preview-12-2025"
private const val INPUT_SAMPLE_RATE = 16_000
private const val OUTPUT_SAMPLE_RATE = 24_000

enum class LiveVoiceStatus(val label: String) {
  DISCONNECTED("Отключено"),
  CONNECTING("Подключаюсь..."),
  CONNECTED("В эфире"),
  ERROR("Ошибка подключения"),
}

data class AiSupportUiState(
  val status: LiveVoiceStatus = LiveVoiceStatus.DISCONNECTED,
  val isMicMuted: Boolean = false,
  val isSpeakerphoneOn: Boolean = true,
  val isAiSpeaking: Boolean = false,
  val aiSpeakingAmplitude: Float = 0f,
  val errorMessage: String? = null,
  val needsSettingsForPermission: Boolean = false,
)

@OptIn(PublicPreviewAPI::class)
class AiMoodViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(AiSupportUiState())
  val uiState: StateFlow<AiSupportUiState> = _uiState.asStateFlow()

  private var latestMoodContextSummary: String = "Пользователь только начал вести дневник."

  private val audioManager: AudioManager by lazy {
    getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }
  private var audioFocusRequest: AudioFocusRequest? = null

  private var liveSession: LiveSession? = null
  private var audioRecord: AudioRecord? = null
  private var audioTrack: AudioTrack? = null
  private var echoCanceler: AcousticEchoCanceler? = null

  private var playbackQueue: Channel<ByteArray>? = null

  private var connectJob: Job? = null
  private var recordJob: Job? = null
  private var receiveJob: Job? = null
  private var playbackJob: Job? = null
  private var speakingResetJob: Job? = null

  private val handlingStreamFailure = AtomicBoolean(false)

  @Volatile private var isAudioWriting: Boolean = false
  @Volatile private var lastPlaybackFinishMs: Long = 0L

  fun updateMoodContext(entries: List<MoodEntryEntity>) {
    val today = ToymoteDate.today()
    val streak = AnalyticsEngine.computeStreaks(entries, today)
    val recent = entries.sortedByDescending { it.timestampMillis }.take(7)
    latestMoodContextSummary =
      if (recent.isEmpty()) {
        "Записей настроения пока нет. Текущая серия: 0 дней."
      } else {
        val recentDesc =
          recent.joinToString("; ") { entry ->
            val emotion = EmotionCatalog.findById(entry.emotionId)
            val date = ToymoteDate.fromEpochDay(entry.dateEpochDay).shortDayMonthRu()
            val note = if (entry.comment.isNotBlank()) " («${entry.comment}»)" else ""
            "$date — ${emotion.name}$note"
          }
        "Текущая серия: ${streak.currentStreak} дн., всего записей: ${entries.size}. Недавние отметки: $recentDesc."
      }
  }

  fun onMicPermissionDenied(permanentlyDenied: Boolean) {
    _uiState.update {
      it.copy(
        status = LiveVoiceStatus.ERROR,
        isAiSpeaking = false,
        aiSpeakingAmplitude = 0f,
        needsSettingsForPermission = permanentlyDenied,
        errorMessage =
          if (permanentlyDenied) {
            "Доступ к микрофону запрещён в настройках телефона. Разрешите микрофон в настройках приложения, чтобы говорить с ГАЗелью."
          } else {
            "Для разговора с ГАЗелью нужно разрешение на микрофон."
          },
      )
    }
  }

  private fun buildSystemInstructionText(): String =
    """
    Ты — ГАЗель, тёплый, живой и эмпатичный голосовой собеседник в дневнике настроения «toymote» (от Matto Inc.).
    Общайся исключительно на русском языке, естественно, бережно и с лёгкой дружеской теплотой.
    Выслушивай пользователя, поддерживай его настроение, помогай мягко осмыслить переживания и предлагай 1–2 простых, тёплых шага заботы о себе.
    Не ставь медицинских диагнозов. Отвечай лаконично и живо (2–4 предложения), чтобы разговор звучал как естественный диалог.
    Контекст дневника пользователя: $latestMoodContextSummary
    """.trimIndent()

  private fun isNetworkAvailable(): Boolean {
    return try {
      val cm =
        getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE)
          as? ConnectivityManager ?: return true
      val network = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
      true
    }
  }

  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
  fun connectLive() {
    val hasPermission =
      ContextCompat.checkSelfPermission(
        getApplication(),
        Manifest.permission.RECORD_AUDIO,
      ) == PackageManager.PERMISSION_GRANTED
    if (!hasPermission) {
      onMicPermissionDenied(permanentlyDenied = false)
      return
    }

    if (!isNetworkAvailable()) {
      _uiState.update {
        it.copy(
          status = LiveVoiceStatus.ERROR,
          isAiSpeaking = false,
          aiSpeakingAmplitude = 0f,
          needsSettingsForPermission = false,
          errorMessage = "Нет подключения к интернету. Проверьте Wi‑Fi или мобильную связь.",
        )
      }
      return
    }

    val currentStatus = _uiState.value.status
    if (currentStatus == LiveVoiceStatus.CONNECTING || currentStatus == LiveVoiceStatus.CONNECTED) {
      return
    }

    connectJob?.cancel()
    handlingStreamFailure.set(false)
    _uiState.update {
      it.copy(
        status = LiveVoiceStatus.CONNECTING,
        isAiSpeaking = false,
        aiSpeakingAmplitude = 0f,
        errorMessage = null,
        needsSettingsForPermission = false,
      )
    }

    connectJob =
      viewModelScope.launch {
        try {
          applyAudioRouting(_uiState.value.isSpeakerphoneOn)

          val session = connectSessionWithFallback()
          liveSession = session

          val queue = Channel<ByteArray>(Channel.UNLIMITED)
          playbackQueue = queue

          val (record, track) = createAudioHardware()
          audioRecord = record
          audioTrack = track

          // Re-apply routing after AudioTrack/AudioRecord creation so Samsung HAL binds output cleanly
          applyAudioRouting(_uiState.value.isSpeakerphoneOn)

          track.play()
          if (!_uiState.value.isMicMuted) {
            record.startRecording()
          }

          _uiState.update {
            it.copy(
              status = LiveVoiceStatus.CONNECTED,
              errorMessage = null,
              needsSettingsForPermission = false,
            )
          }

          startAudioPipelines(session, record, track, queue)
        } catch (e: TimeoutCancellationException) {
          Log.e(TAG, "Live connection timed out", e)
          cleanupResources()
          _uiState.update {
            it.copy(
              status = LiveVoiceStatus.ERROR,
              isAiSpeaking = false,
              aiSpeakingAmplitude = 0f,
              errorMessage = "Сервер не ответил вовремя (таймаут). Проверьте интернет и попробуйте снова.",
            )
          }
        } catch (e: CancellationException) {
          cleanupResources()
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Live connection failed", e)
          val detailedError = diagnoseLiveConnectionFailure(e)
          cleanupResources()
          _uiState.update {
            it.copy(
              status = LiveVoiceStatus.ERROR,
              isAiSpeaking = false,
              aiSpeakingAmplitude = 0f,
              errorMessage = detailedError,
            )
          }
        }
      }
  }

  private suspend fun diagnoseLiveConnectionFailure(e: Exception): String {
    val initErr = AppCheckDiagnostics.initError
    if (!initErr.isNullOrBlank()) {
      return "Ошибка инициализации App Check ($initErr). Переустановите релизную сборку приложения."
    }

    val appCheckError = fetchAppCheckErrorIfAny()
    if (appCheckError != null) {
      return "Не пройдена проверка App Check / reCAPTCHA ($appCheckError). Проверьте интернет, Google Play Сервисы и точное время на телефоне."
    }

    val combined = generateSequence<Throwable>(e) { it.cause }.mapNotNull { it.message }.joinToString(" | ").lowercase()
    if (AppCheckDiagnostics.isDebugBuild &&
      !AppCheckDiagnostics.hasInjectedDebugToken &&
      ("unable to connect" in combined || "403" in combined || "handshake" in combined || "closed" in combined)
    ) {
      return "Это Debug-сборка без зарегистрированного токена для этого телефона — после передачи APK с другого устройства сервер отклоняет её (403). Установите Release APK или подключите это устройство с debug-токеном."
    }

    if ("unable to connect to the server" in combined) {
      return "Не удалось подключиться к серверу Gemini Live. На этом телефоне сервер может быть недоступен без VPN или другого Wi‑Fi (либо используется старая/Debug-сборка без App Check)."
    }

    return mapLiveErrorToRussian(e)
  }

  private suspend fun fetchAppCheckErrorIfAny(): String? {
    return try {
      withTimeoutOrNull(4_000L) {
        suspendCancellableCoroutine { cont ->
          FirebaseAppCheck.getInstance()
            .getAppCheckToken(false)
            .addOnSuccessListener { token ->
              if (cont.isActive) {
                val empty = token?.token.isNullOrBlank()
                cont.resume(if (empty) "Пустой токен App Check" else null)
              }
            }
            .addOnFailureListener { err ->
              if (cont.isActive) {
                cont.resume(err.localizedMessage ?: err::class.simpleName ?: "AppCheck failure")
              }
            }
        }
      }
    } catch (_: Exception) {
      null
    }
  }

  private suspend fun connectSessionWithFallback(): LiveSession {
    val instruction = content { text(buildSystemInstructionText()) }
    val attempts: List<Pair<String, Boolean>> =
      listOf(
        PRIMARY_LIVE_MODEL_NAME to true,
        FALLBACK_LIVE_MODEL_NAME to true,
        FALLBACK_LIVE_MODEL_NAME to false,
      )

    var lastError: Exception? = null
    for ((index, attempt) in attempts.withIndex()) {
      val (modelName, useVoiceConfig) = attempt
      try {
        val generationConfig =
          liveGenerationConfig {
            responseModality = ResponseModality.AUDIO
            if (useVoiceConfig) {
              speechConfig = SpeechConfig(voice = Voice("Aoede"))
            }
          }
        val liveModel =
          Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
            modelName = modelName,
            generationConfig = generationConfig,
            systemInstruction = instruction,
          )
        return withTimeout(25_000L) { liveModel.connect() }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Live connect attempt #$index ($modelName, voice=$useVoiceConfig) failed", e)
        lastError = e
        if (isNonRetryableAuthOrNetworkError(e)) {
          throw e
        }
      }
    }
    throw lastError ?: IllegalStateException("Не удалось установить Live-сессию.")
  }

  private fun isNonRetryableAuthOrNetworkError(e: Throwable): Boolean {
    var cursor: Throwable? = e
    while (cursor != null) {
      if (cursor is UnknownHostException ||
        cursor is ConnectException ||
        cursor is UnresolvedAddressException ||
        cursor is SSLException
      ) {
        return true
      }
      val msg = cursor.message.orEmpty().lowercase()
      if ("403" in msg || "appcheck" in msg || "app check" in msg || "attestation" in msg) {
        return true
      }
      cursor = cursor.cause
    }
    return false
  }

  private fun requestAudioFocus() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val req =
          AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            )
            .build()
        audioFocusRequest = req
        audioManager.requestAudioFocus(req)
      } else {
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(
          null,
          AudioManager.STREAM_VOICE_CALL,
          AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
        )
      }
    } catch (e: Exception) {
      Log.w(TAG, "requestAudioFocus warning", e)
    }
  }

  private fun abandonAudioFocus() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
      } else {
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(null)
      }
    } catch (_: Exception) {}
  }

  private fun applyAudioRouting(speakerOn: Boolean) {
    try {
      requestAudioFocus()
      audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val devices = audioManager.availableCommunicationDevices
        val externalDevice =
          devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
              it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
              it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
              it.type == AudioDeviceInfo.TYPE_USB_HEADSET
          }
        val targetDevice =
          if (speakerOn) {
            devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
              ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE }
          } else {
            externalDevice ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
          }
        if (targetDevice != null) {
          val ok = audioManager.setCommunicationDevice(targetDevice)
          if (!ok) {
            Log.w(TAG, "setCommunicationDevice(${targetDevice.type}) returned false")
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = speakerOn
          }
        } else {
          audioManager.clearCommunicationDevice()
          @Suppress("DEPRECATION")
          audioManager.isSpeakerphoneOn = speakerOn
        }
      } else {
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = speakerOn
      }
    } catch (e: Exception) {
      Log.w(TAG, "applyAudioRouting warning", e)
    }
  }

  private fun restoreAudioRouting() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        audioManager.clearCommunicationDevice()
      }
      @Suppress("DEPRECATION")
      audioManager.isSpeakerphoneOn = false
      audioManager.mode = AudioManager.MODE_NORMAL
      abandonAudioFocus()
    } catch (e: Exception) {
      Log.w(TAG, "restoreAudioRouting warning", e)
    }
  }

  private fun isExternalHeadsetActive(): Boolean {
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val currentType = audioManager.communicationDevice?.type
        currentType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
          currentType == AudioDeviceInfo.TYPE_BLE_HEADSET ||
          currentType == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
          currentType == AudioDeviceInfo.TYPE_USB_HEADSET
      } else {
        @Suppress("DEPRECATION")
        audioManager.isWiredHeadsetOn || audioManager.isBluetoothScoOn
      }
    } catch (_: Exception) {
      false
    }
  }

  @SuppressLint("MissingPermission")
  private fun createAudioHardware(): Pair<AudioRecord, AudioTrack> {
    val recMin =
      AudioRecord.getMinBufferSize(
        INPUT_SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      )
    val safeRecMin = if (recMin > 0) recMin else INPUT_SAMPLE_RATE / 5 * 2
    val recBuf = max(safeRecMin * 2, INPUT_SAMPLE_RATE / 4 * 2)

    val record =
      buildAudioRecordWithFallback(recBuf)
        ?: throw IllegalStateException(
          "Не удалось инициализировать микрофон. Проверьте, не занят ли он другим приложением."
        )

    echoCanceler =
      if (AcousticEchoCanceler.isAvailable()) {
        try {
          AcousticEchoCanceler.create(record.audioSessionId)?.apply { enabled = true }
        } catch (e: Exception) {
          Log.w(TAG, "AcousticEchoCanceler init warning", e)
          null
        }
      } else {
        null
      }

    val playMin =
      AudioTrack.getMinBufferSize(
        OUTPUT_SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      )
    val safePlayMin = if (playMin > 0) playMin else OUTPUT_SAMPLE_RATE / 5 * 2
    // Large ~1 second PCM buffer prevents underrun stutters between WebSocket chunks
    val playBuf = max(safePlayMin * 4, OUTPUT_SAMPLE_RATE * 2)

    val track =
      buildAudioTrackWithFallback(playBuf)
        ?: run {
          try {
            record.release()
          } catch (_: Exception) {}
          throw IllegalStateException("Не удалось инициализировать аудиовыход устройства.")
        }

    return record to track
  }

  @SuppressLint("MissingPermission")
  private fun buildAudioRecordWithFallback(bufferBytes: Int): AudioRecord? {
    val sources =
      listOf(
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.MIC,
      )
    for (source in sources) {
      var candidate: AudioRecord? = null
      try {
        candidate =
          AudioRecord.Builder()
            .setAudioSource(source)
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(INPUT_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .build()
        if (candidate.state == AudioRecord.STATE_INITIALIZED) {
          return candidate
        }
        Log.w(TAG, "AudioRecord source $source uninitialized (state=${candidate.state})")
        candidate.release()
      } catch (e: Exception) {
        Log.w(TAG, "AudioRecord source $source failed", e)
        try {
          candidate?.release()
        } catch (_: Exception) {}
      }
    }
    return null
  }

  private fun buildAudioTrackWithFallback(bufferBytes: Int): AudioTrack? {
    val usages =
      listOf(
        AudioAttributes.USAGE_VOICE_COMMUNICATION,
        AudioAttributes.USAGE_MEDIA,
      )
    for (usage in usages) {
      var candidate: AudioTrack? = null
      try {
        candidate =
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(OUTPUT_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        if (candidate.state == AudioTrack.STATE_INITIALIZED) {
          return candidate
        }
        Log.w(TAG, "AudioTrack usage $usage uninitialized (state=${candidate.state})")
        candidate.release()
      } catch (e: Exception) {
        Log.w(TAG, "AudioTrack usage $usage failed", e)
        try {
          candidate?.release()
        } catch (_: Exception) {}
      }
    }
    return null
  }

  private fun startAudioPipelines(
    session: LiveSession,
    record: AudioRecord,
    track: AudioTrack,
    queue: Channel<ByteArray>,
  ) {
    recordJob?.cancel()
    receiveJob?.cancel()
    playbackJob?.cancel()

    // 1. Microphone Capture -> LiveSession
    recordJob =
      viewModelScope.launch(Dispatchers.IO) {
        val chunkBytes = 1024 // ~32ms of 16kHz 16-bit mono PCM
        val buffer = ByteArray(chunkBytes)
        var consecutiveReadErrors = 0
        try {
          while (isActive && !session.isClosed()) {
            if (_uiState.value.isMicMuted) {
              delay(40L)
              continue
            }
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
              try {
                record.startRecording()
              } catch (e: Exception) {
                Log.w(TAG, "AudioRecord startRecording retry warning", e)
                delay(80L)
                continue
              }
            }
            val read = record.read(buffer, 0, buffer.size)
            if (read > 0) {
              consecutiveReadErrors = 0
              // Prevent speakerphone output from echoing back into mic & causing false Gemini VAD interrupts
              val now = System.currentTimeMillis()
              val speakerEchoWindow =
                _uiState.value.isSpeakerphoneOn &&
                  !isExternalHeadsetActive() &&
                  (isAudioWriting || (now - lastPlaybackFinishMs) < 320L)

              if (!speakerEchoWindow) {
                val payload = if (read == buffer.size) buffer.copyOf() else buffer.copyOf(read)
                session.sendAudioRealtime(InlineData(payload, "audio/pcm"))
              }
            } else if (read < 0) {
              consecutiveReadErrors++
              Log.w(TAG, "AudioRecord.read returned error $read (#$consecutiveReadErrors)")
              if (consecutiveReadErrors >= 8) {
                reportStreamFailure("Микрофон перестал отвечать (код $read). Попробуйте подключиться снова.")
                break
              }
              delay(40L)
            } else {
              delay(15L)
            }
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Audio recording stream error", e)
          reportStreamFailure(mapLiveErrorToRussian(e))
        }
      }

    // 2. Buffered Playback Loop (decoupled from WebSocket collector to eliminate stutters)
    playbackJob =
      viewModelScope.launch(Dispatchers.IO) {
        try {
          for (firstChunk in queue) {
            if (!isActive) break
            // Briefly coalesce up to ~4800 bytes (~100ms) at turn start so AudioTrack never starves
            val batch = ArrayList<ByteArray>(4)
            batch.add(firstChunk)
            var accumulated = firstChunk.size
            while (accumulated < 4800) {
              val nextImmediate = queue.tryReceive().getOrNull()
              if (nextImmediate != null) {
                batch.add(nextImmediate)
                accumulated += nextImmediate.size
              } else {
                val awaited = withTimeoutOrNull(45L) { queue.receive() }
                if (awaited != null) {
                  batch.add(awaited)
                  accumulated += awaited.size
                } else {
                  break
                }
              }
            }

            isAudioWriting = true
            for (chunk in batch) {
              if (!isActive) break
              writeChunkToTrack(track, chunk)
            }

            // Continue draining consecutive chunks seamlessly without extra prebuffer pauses
            while (isActive) {
              val nextChunk = queue.tryReceive().getOrNull() ?: withTimeoutOrNull(140L) { queue.receive() }
              if (nextChunk == null) break
              writeChunkToTrack(track, nextChunk)
            }

            isAudioWriting = false
            lastPlaybackFinishMs = System.currentTimeMillis()
            scheduleSpeakingDecay(delayMs = 180L)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Audio playback error", e)
          reportStreamFailure("Ошибка воспроизведения звука: ${e.localizedMessage ?: "неизвестная ошибка"}")
        } finally {
          isAudioWriting = false
        }
      }

    // 3. WebSocket Receive -> Queue
    receiveJob =
      viewModelScope.launch(Dispatchers.IO) {
        try {
          session.receive().collect { message ->
            when (message) {
              is LiveServerContent -> {
                if (message.interrupted) {
                  // Only flush buffered audio on genuine external interrupts
                  if (!_uiState.value.isSpeakerphoneOn || isExternalHeadsetActive()) {
                    while (queue.tryReceive().isSuccess) {
                      // drain stale buffered frames
                    }
                    try {
                      track.pause()
                      track.flush()
                      track.play()
                    } catch (_: Exception) {}
                    speakingResetJob?.cancel()
                    isAudioWriting = false
                    _uiState.update { it.copy(isAiSpeaking = false, aiSpeakingAmplitude = 0f) }
                  }
                }

                val parts = message.content?.parts.orEmpty()
                for (part in parts) {
                  if (part is InlineDataPart && part.mimeType.startsWith("audio/")) {
                    val audioBytes = part.inlineData
                    if (audioBytes.isNotEmpty()) {
                      queue.trySend(audioBytes)
                    }
                  }
                }
              }
              is LiveServerGoAway -> {
                Log.w(TAG, "LiveServerGoAway received: timeLeft=${message.timeLeft}")
              }
              else -> Unit
            }
          }
          // Flow completed (server closed WebSocket) while UI still believed it was CONNECTED
          if (isActive && _uiState.value.status == LiveVoiceStatus.CONNECTED) {
            reportStreamFailure("Соединение с ГАЗелью завершено сервером. Нажмите «Подключиться», чтобы продолжить.")
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Live receive stream error", e)
          reportStreamFailure(mapLiveErrorToRussian(e))
        }
      }
  }

  private fun reportStreamFailure(message: String) {
    if (_uiState.value.status != LiveVoiceStatus.CONNECTED &&
      _uiState.value.status != LiveVoiceStatus.CONNECTING
    ) {
      return
    }
    if (!handlingStreamFailure.compareAndSet(false, true)) return
    viewModelScope.launch {
      cleanupResources()
      _uiState.update {
        it.copy(
          status = LiveVoiceStatus.ERROR,
          isAiSpeaking = false,
          aiSpeakingAmplitude = 0f,
          errorMessage = message,
        )
      }
    }
  }

  private fun writeChunkToTrack(track: AudioTrack, audioBytes: ByteArray) {
    val amp = computePcm16Amplitude(audioBytes)
    speakingResetJob?.cancel()
    _uiState.update {
      it.copy(
        isAiSpeaking = amp > 0.025f,
        aiSpeakingAmplitude = amp,
      )
    }

    var offset = 0
    while (offset < audioBytes.size) {
      val written =
        track.write(
          audioBytes,
          offset,
          audioBytes.size - offset,
          AudioTrack.WRITE_BLOCKING,
        )
      if (written < 0) {
        throw IllegalStateException("AudioTrack.write failed with code $written")
      }
      if (written == 0) break
      offset += written
    }
    lastPlaybackFinishMs = System.currentTimeMillis()
  }

  private fun scheduleSpeakingDecay(delayMs: Long = 200L) {
    speakingResetJob?.cancel()
    speakingResetJob =
      viewModelScope.launch {
        delay(delayMs)
        _uiState.update { it.copy(isAiSpeaking = false, aiSpeakingAmplitude = 0f) }
      }
  }

  @SuppressLint("MissingPermission")
  fun toggleMicMute() {
    val nextMuted = !_uiState.value.isMicMuted
    _uiState.update { it.copy(isMicMuted = nextMuted) }
    val record = audioRecord ?: return
    try {
      if (nextMuted) {
        if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
          record.stop()
        }
      } else {
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
          record.startRecording()
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "toggleMicMute warning", e)
    }
  }

  fun toggleSpeakerphone() {
    val nextSpeaker = !_uiState.value.isSpeakerphoneOn
    _uiState.update { it.copy(isSpeakerphoneOn = nextSpeaker) }
    if (_uiState.value.status == LiveVoiceStatus.CONNECTED ||
      _uiState.value.status == LiveVoiceStatus.CONNECTING
    ) {
      applyAudioRouting(nextSpeaker)
    }
  }

  fun disconnectLive() {
    connectJob?.cancel()
    handlingStreamFailure.set(false)
    viewModelScope.launch {
      cleanupResources()
      _uiState.update {
        it.copy(
          status = LiveVoiceStatus.DISCONNECTED,
          isAiSpeaking = false,
          aiSpeakingAmplitude = 0f,
          errorMessage = null,
          needsSettingsForPermission = false,
        )
      }
    }
  }

  private suspend fun cleanupResources() {
    recordJob?.cancel()
    recordJob = null
    receiveJob?.cancel()
    receiveJob = null
    playbackJob?.cancel()
    playbackJob = null
    speakingResetJob?.cancel()
    speakingResetJob = null
    isAudioWriting = false

    playbackQueue?.close()
    playbackQueue = null

    try {
      echoCanceler?.release()
    } catch (_: Exception) {}
    echoCanceler = null

    val record = audioRecord
    audioRecord = null
    if (record != null) {
      try {
        if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
          record.stop()
        }
      } catch (_: Exception) {}
      try {
        record.release()
      } catch (_: Exception) {}
    }

    val track = audioTrack
    audioTrack = null
    if (track != null) {
      try {
        track.pause()
        track.flush()
      } catch (_: Exception) {}
      try {
        track.release()
      } catch (_: Exception) {}
    }

    val session = liveSession
    liveSession = null
    if (session != null) {
      try {
        session.stopReceiving()
      } catch (_: Exception) {}
      try {
        session.close()
      } catch (e: Exception) {
        Log.w(TAG, "LiveSession close warning", e)
      }
    }

    restoreAudioRouting()
  }

  private fun computePcm16Amplitude(bytes: ByteArray): Float {
    if (bytes.size < 2) return 0f
    var sumSquares = 0.0
    var sampleCount = 0
    var i = 0
    while (i + 1 < bytes.size) {
      val low = bytes[i].toInt() and 0xFF
      val high = bytes[i + 1].toInt() shl 8
      val sample = (high or low).toShort().toDouble()
      sumSquares += sample * sample
      sampleCount++
      i += 4
    }
    if (sampleCount == 0) return 0f
    val rms = sqrt(sumSquares / sampleCount) / 32768.0
    return (rms.toFloat() * 4.4f).coerceIn(0f, 1f)
  }

  override fun onCleared() {
    super.onCleared()
    connectJob?.cancel()
    recordJob?.cancel()
    receiveJob?.cancel()
    playbackJob?.cancel()
    speakingResetJob?.cancel()
    playbackQueue?.close()
    try {
      echoCanceler?.release()
    } catch (_: Exception) {}
    try {
      audioRecord?.release()
    } catch (_: Exception) {}
    try {
      audioTrack?.release()
    } catch (_: Exception) {}
    echoCanceler = null
    audioRecord = null
    audioTrack = null
    liveSession = null
    restoreAudioRouting()
  }
}

internal fun mapLiveErrorToRussian(e: Throwable): String {
  val chain = generateSequence(e) { it.cause }.toList()
  val combinedMsg = chain.mapNotNull { it.message }.joinToString(" | ")
  val lower = combinedMsg.lowercase()

  return when {
    chain.any {
      it is UnknownHostException ||
        it is ConnectException ||
        it is SocketException ||
        it is UnresolvedAddressException ||
        it is SSLException
    } ->
      "Нет стабильного соединения с сервером. Проверьте интернет, Wi‑Fi или VPN и попробуйте снова."

    "403" in lower || "appcheck" in lower || "app check" in lower || "attestation" in lower ->
      "Сервер отклонил проверку приложения (App Check / 403). На физическом устройстве нужна релизная сборка с reCAPTCHA либо зарегистрированный debug-токен."

    "429" in lower || "quota" in lower || "resource_exhausted" in lower ->
      "Превышен лимит запросов к серверу. Попробуйте подключиться через пару минут."

    "404" in lower || "not found" in lower || "unsupported" in lower || "invalid argument" in lower ->
      "Модель голоса отклонила подключение (${combinedMsg.take(120)})."

    "микрофон" in lower || "audiorecord" in lower ->
      combinedMsg.ifBlank { "Не удалось запустить микрофон на устройстве." }

    combinedMsg.isNotBlank() ->
      "Не удалось подключиться к ГАЗели: ${combinedMsg.take(160)}"

    else ->
      "Не удалось подключиться к ГАЗели (${e::class.simpleName ?: "ошибка"})."
  }
}
