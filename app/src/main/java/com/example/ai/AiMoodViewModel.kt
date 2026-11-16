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
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
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
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.sqrt

private const val TAG = "AiMoodViewModel"
private const val LIVE_MODEL_NAME = "gemini-3.8-live"
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
  private var noiseSuppressor: NoiseSuppressor? = null
  private var gainControl: AutomaticGainControl? = null

  private var playbackQueue: Channel<ByteArray>? = null

  private var connectJob: Job? = null
  private var recordJob: Job? = null
  private var receiveJob: Job? = null
  private var playbackJob: Job? = null
  private var speakingResetJob: Job? = null

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

  private fun buildSystemInstructionText(): String =
    """
    Ты — ГАЗель, тёплый, живой и эмпатичный голосовой собеседник в дневнике настроения «toymote» (от Matto Inc.).
    Общайся исключительно на русском языке, естественно, бережно и с лёгкой дружеской теплотой.
    Выслушивай пользователя, поддерживай его настроение, помогай мягко осмыслить переживания и предлагай 1–2 простых, тёплых шага заботы о себе.
    Не ставь медицинских диагнозов. Отвечай лаконично и живо (2–4 предложения), чтобы разговор звучал как естественный диалог.
    Контекст дневника пользователя: $latestMoodContextSummary
    """.trimIndent()

  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
  fun connectLive() {
    val hasPermission =
      ContextCompat.checkSelfPermission(
        getApplication(),
        Manifest.permission.RECORD_AUDIO,
      ) == PackageManager.PERMISSION_GRANTED
    if (!hasPermission) {
      _uiState.update {
        it.copy(
          status = LiveVoiceStatus.ERROR,
          errorMessage = "Нужно разрешение на микрофон для разговора с ГАЗелью.",
        )
      }
      return
    }

    val currentStatus = _uiState.value.status
    if (currentStatus == LiveVoiceStatus.CONNECTING || currentStatus == LiveVoiceStatus.CONNECTED) {
      return
    }

    connectJob?.cancel()
    _uiState.update {
      it.copy(
        status = LiveVoiceStatus.CONNECTING,
        isAiSpeaking = false,
        aiSpeakingAmplitude = 0f,
        errorMessage = null,
      )
    }

    connectJob =
      viewModelScope.launch {
        try {
          applyAudioRouting(_uiState.value.isSpeakerphoneOn)

          val liveModel =
            Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
              modelName = LIVE_MODEL_NAME,
              generationConfig =
                liveGenerationConfig {
                  responseModality = ResponseModality.AUDIO
                  speechConfig = SpeechConfig(voice = Voice("AOEDE"))
                },
              systemInstruction = content { text(buildSystemInstructionText()) },
            )

          val session = withTimeout(30_000L) { liveModel.connect() }
          liveSession = session

          val queue = Channel<ByteArray>(Channel.UNLIMITED)
          playbackQueue = queue

          val (record, track) = createAudioHardware()
          audioRecord = record
          audioTrack = track

          // Re-apply routing after AudioTrack creation so HAL binds output to speaker cleanly
          applyAudioRouting(_uiState.value.isSpeakerphoneOn)

          track.play()
          if (!_uiState.value.isMicMuted) {
            record.startRecording()
          }

          _uiState.update {
            it.copy(
              status = LiveVoiceStatus.CONNECTED,
              errorMessage = null,
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
              errorMessage = "Таймаут подключения. Проверьте интернет и попробуйте снова.",
            )
          }
        } catch (e: CancellationException) {
          cleanupResources()
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Live connection failed", e)
          cleanupResources()
          _uiState.update {
            it.copy(
              status = LiveVoiceStatus.ERROR,
              isAiSpeaking = false,
              aiSpeakingAmplitude = 0f,
              errorMessage = e.localizedMessage ?: "Не удалось подключиться к ГАЗели.",
            )
          }
        }
      }
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
        // Respect external headset/BT if connected; otherwise switch between speaker and earpiece
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
          } else {
            externalDevice ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
          }
        if (targetDevice != null) {
          audioManager.setCommunicationDevice(targetDevice)
        } else {
          audioManager.clearCommunicationDevice()
        }
      }

      @Suppress("DEPRECATION")
      audioManager.isSpeakerphoneOn = speakerOn
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
    val recBuf = max(recMin * 2, INPUT_SAMPLE_RATE / 4 * 2)
    val record =
      AudioRecord(
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        INPUT_SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        recBuf,
      )

    echoCanceler =
      if (AcousticEchoCanceler.isAvailable()) {
        try {
          AcousticEchoCanceler.create(record.audioSessionId)?.apply { enabled = true }
        } catch (_: Exception) {
          null
        }
      } else {
        null
      }

    noiseSuppressor =
      if (NoiseSuppressor.isAvailable()) {
        try {
          NoiseSuppressor.create(record.audioSessionId)?.apply { enabled = true }
        } catch (_: Exception) {
          null
        }
      } else {
        null
      }

    gainControl =
      if (AutomaticGainControl.isAvailable()) {
        try {
          AutomaticGainControl.create(record.audioSessionId)?.apply { enabled = true }
        } catch (_: Exception) {
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
    // Large ~1 second PCM buffer prevents underrun stutters between WebSocket chunks
    val playBuf = max(playMin * 4, OUTPUT_SAMPLE_RATE * 2)
    val track =
      AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
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
        .setBufferSizeInBytes(playBuf)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    return record to track
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
                delay(60L)
                continue
              }
            }
            val read = record.read(buffer, 0, buffer.size)
            if (read > 0) {
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
            } else {
              delay(15L)
            }
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Audio recording stream error", e)
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
        } finally {
          isAudioWriting = false
        }
      }

    // 3. WebSocket Receive -> Queue
    receiveJob =
      viewModelScope.launch(Dispatchers.IO) {
        try {
          session.receive().collect { message ->
            if (message is LiveServerContent) {
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
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Live receive stream error", e)
          if (_uiState.value.status == LiveVoiceStatus.CONNECTED) {
            cleanupResources()
            _uiState.update {
              it.copy(
                status = LiveVoiceStatus.ERROR,
                isAiSpeaking = false,
                aiSpeakingAmplitude = 0f,
                errorMessage = e.localizedMessage ?: "Соединение с ГАЗелью прервалось.",
              )
            }
          }
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
      if (written <= 0) break
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
    viewModelScope.launch {
      cleanupResources()
      _uiState.update {
        it.copy(
          status = LiveVoiceStatus.DISCONNECTED,
          isAiSpeaking = false,
          aiSpeakingAmplitude = 0f,
          errorMessage = null,
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

    try {
      noiseSuppressor?.release()
    } catch (_: Exception) {}
    noiseSuppressor = null

    try {
      gainControl?.release()
    } catch (_: Exception) {}
    gainControl = null

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
      noiseSuppressor?.release()
    } catch (_: Exception) {}
    try {
      gainControl?.release()
    } catch (_: Exception) {}
    try {
      audioRecord?.release()
    } catch (_: Exception) {}
    try {
      audioTrack?.release()
    } catch (_: Exception) {}
    echoCanceler = null
    noiseSuppressor = null
    gainControl = null
    audioRecord = null
    audioTrack = null
    liveSession = null
    restoreAudioRouting()
  }
}
