package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiMoodViewModel
import com.example.ai.AiSupportUiState
import com.example.ai.FALLBACK_LIVE_MODEL_NAME
import com.example.ai.LiveVoiceStatus
import com.example.ai.PRIMARY_LIVE_MODEL_NAME
import com.example.ai.mapLiveErrorToRussian
import com.example.data.MoodEntryEntity
import com.example.data.UserSettings
import com.example.domain.ToymoteDate
import com.example.widgets.ToymoteWidgetRenderer
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name and gazelle defaults match requirements`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("toymote", context.getString(R.string.app_name))
    assertEquals("ГАЗель", context.getString(R.string.nav_ai))

    val defaultAiState = AiSupportUiState()
    assertEquals(LiveVoiceStatus.DISCONNECTED, defaultAiState.status)
    assertTrue(defaultAiState.isSpeakerphoneOn)
    assertFalse(defaultAiState.needsSettingsForPermission)
    assertFalse(UserSettings().widgetPromptShown)
  }

  @Test
  fun `gazelle uses supported live models and maps errors cleanly`() {
    assertEquals("gemini-3.1-flash-live-preview", PRIMARY_LIVE_MODEL_NAME)
    assertEquals("gemini-2.5-flash-native-audio-preview-12-2025", FALLBACK_LIVE_MODEL_NAME)

    val netErr = mapLiveErrorToRussian(UnknownHostException("generativelanguage.googleapis.com"))
    assertTrue(netErr.contains("интернет"))

    val appCheckErr = mapLiveErrorToRussian(RuntimeException("HTTP 403 AppCheck token rejected"))
    assertTrue(appCheckErr.contains("App Check") && appCheckErr.contains("403"))

    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = AiMoodViewModel(app)
    vm.onMicPermissionDenied(permanentlyDenied = true)
    assertEquals(LiveVoiceStatus.ERROR, vm.uiState.value.status)
    assertTrue(vm.uiState.value.needsSettingsForPermission)
  }

  @Test
  fun `all three widgets render safely across 2x1 and expanded sizes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val today = ToymoteDate.today().epochDay
    val sampleEntries =
      listOf(
        MoodEntryEntity(id = 1L, dateEpochDay = today, timestampMillis = 1000L, emotionId = "spokush", comment = ""),
        MoodEntryEntity(id = 2L, dateEpochDay = today - 1, timestampMillis = 1000L, emotionId = "kaifyush", comment = ""),
        MoodEntryEntity(id = 3L, dateEpochDay = today - 2, timestampMillis = 1000L, emotionId = "veselyi", comment = ""),
      )

    val sizes = listOf(128 to 54, 160 to 68, 200 to 160, 320 to 110, 320 to 250)
    for ((w, h) in sizes) {
      val b1 = ToymoteWidgetRenderer.renderStreakWidget(context, sampleEntries, w, h)
      val b2 = ToymoteWidgetRenderer.renderCalendarWidget(context, sampleEntries, w, h)
      val b3 = ToymoteWidgetRenderer.renderComboWidget(context, sampleEntries, w, h)

      assertTrue(b1.width > 0 && b1.height > 0 && b1.byteCount < 900_000)
      assertTrue(b2.width > 0 && b2.height > 0 && b2.byteCount < 900_000)
      assertTrue(b3.width > 0 && b3.height > 0 && b3.byteCount < 900_000)
    }
  }
}
