package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ai.AiMoodViewModel
import com.example.ai.configureAppCheck
import com.example.domain.ToymoteDate
import com.example.ui.ToymoteViewModel
import com.example.ui.components.DayDetailSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.AiSupportScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.EmotionPickerScreen
import com.example.ui.screens.RecommendationsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.ToymoteTheme

const val ROUTE_CALENDAR = "calendar"
const val ROUTE_STATS = "stats"
const val ROUTE_ADVICE = "advice"
const val ROUTE_AI = "ai_support"
const val ROUTE_PICKER = "picker"

private data class TopTab(
  val route: String,
  val labelRes: Int,
  val icon: ImageVector,
)

private val TOP_TABS =
  listOf(
    TopTab(
      route = ROUTE_CALENDAR,
      labelRes = R.string.nav_calendar,
      icon = Icons.Rounded.CalendarMonth,
    ),
    TopTab(
      route = ROUTE_STATS,
      labelRes = R.string.nav_stats,
      icon = Icons.Rounded.BarChart,
    ),
    TopTab(
      route = ROUTE_ADVICE,
      labelRes = R.string.nav_advice,
      icon = Icons.Rounded.AutoAwesome,
    ),
    TopTab(
      route = ROUTE_AI,
      labelRes = R.string.nav_ai,
      icon = Icons.Rounded.GraphicEq,
    ),
  )

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    setContent { ToymoteApp() }
  }
}

@Composable
fun ToymoteApp(
  viewModel: ToymoteViewModel = viewModel(),
  aiViewModel: AiMoodViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val aiState by aiViewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val context = LocalContext.current

  var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
  var askedNotificationOnce by rememberSaveable { mutableStateOf(false) }
  var editingEntryId by rememberSaveable { mutableLongStateOf(-1L) }
  var editingEmotionId by rememberSaveable { mutableStateOf<String?>(null) }
  var editingComment by rememberSaveable { mutableStateOf("") }

  LaunchedEffect(uiState.entriesByDay) {
    aiViewModel.updateMoodContext(uiState.entriesByDay.values.flatten())
  }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (!granted) {
        viewModel.updateReminderEnabled(false)
      }
    }

  LaunchedEffect(Unit) {
    if (!askedNotificationOnce && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      askedNotificationOnce = true
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted && uiState.settings.reminderEnabled) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  val openPicker: (ToymoteDate, Long?, String?, String) -> Unit =
    { date, entryId, emotionId, comment ->
      editingEntryId = entryId ?: -1L
      editingEmotionId = emotionId
      editingComment = comment
      navController.navigate("$ROUTE_PICKER/${date.epochDay}")
    }

  ToymoteTheme(themeMode = uiState.settings.themeMode) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isPickerScreen = currentDestination?.route?.startsWith(ROUTE_PICKER) == true
    val isAiScreen = currentDestination?.route == ROUTE_AI

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
          if (!isPickerScreen && !useNavRail) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 4.dp,
            ) {
              TOP_TABS.forEach { tab ->
                val selected =
                  currentDestination?.hierarchy?.any { it.route == tab.route } == true
                NavigationBarItem(
                  selected = selected,
                  onClick = {
                    navController.navigate(tab.route) {
                      popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                      }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = {
                    Icon(
                      imageVector = tab.icon,
                      contentDescription = stringResource(id = tab.labelRes),
                    )
                  },
                  label = { Text(text = stringResource(id = tab.labelRes)) },
                  modifier = Modifier.testTag("nav_tab_${tab.route}"),
                )
              }
            }
          }
        },
        floatingActionButton = {
          if (!isPickerScreen && !isAiScreen) {
            FloatingActionButton(
              onClick = { openPicker(ToymoteDate.today(), null, null, "") },
              shape = CircleShape,
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
              modifier = Modifier.size(64.dp).testTag("add_mood_fab"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(id = R.string.fab_add_mood),
                modifier = Modifier.size(30.dp),
              )
            }
          }
        },
      ) { innerPadding ->
        Row(
          modifier =
            Modifier.fillMaxSize()
              .padding(innerPadding)
              .consumeWindowInsets(innerPadding)
        ) {
          if (!isPickerScreen && useNavRail) {
            NavigationRail(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxHeight(),
            ) {
              TOP_TABS.forEach { tab ->
                val selected =
                  currentDestination?.hierarchy?.any { it.route == tab.route } == true
                NavigationRailItem(
                  selected = selected,
                  onClick = {
                    navController.navigate(tab.route) {
                      popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                      }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = {
                    Icon(
                      imageVector = tab.icon,
                      contentDescription = stringResource(id = tab.labelRes),
                    )
                  },
                  label = { Text(text = stringResource(id = tab.labelRes)) },
                  modifier = Modifier.testTag("rail_tab_${tab.route}"),
                )
              }
            }
          }

          Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            NavHost(
              navController = navController,
              startDestination = ROUTE_CALENDAR,
              modifier = Modifier.fillMaxSize(),
            ) {
              composable(ROUTE_CALENDAR) {
                CalendarScreen(
                  uiState = uiState,
                  onPrevMonth = { viewModel.shiftMonth(-1) },
                  onNextMonth = { viewModel.shiftMonth(1) },
                  onJumpToTodayMonth = { viewModel.jumpToCurrentMonth() },
                  onDayClick = { date, dayEntries ->
                    if (dayEntries.isNotEmpty()) {
                      viewModel.openDaySheet(date)
                    } else {
                      openPicker(date, null, null, "")
                    }
                  },
                  onOpenSettings = { showSettingsDialog = true },
                  onMarkWidgetPromptShown = { viewModel.markWidgetPromptShown() },
                )
              }

              composable(ROUTE_STATS) {
                StatsScreen(
                  uiState = uiState,
                  onSelectPeriod = { viewModel.setStatsPeriod(it) },
                )
              }

              composable(ROUTE_ADVICE) {
                RecommendationsScreen(
                  uiState = uiState,
                  onNextQuote = { viewModel.nextQuote() },
                )
              }

              composable(ROUTE_AI) {
                AiSupportScreen(
                  aiState = aiState,
                  onConnect = { aiViewModel.connectLive() },
                  onDisconnect = { aiViewModel.disconnectLive() },
                  onToggleMute = { aiViewModel.toggleMicMute() },
                  onToggleSpeakerphone = { aiViewModel.toggleSpeakerphone() },
                  onMicPermissionDenied = { permanentlyDenied ->
                    aiViewModel.onMicPermissionDenied(permanentlyDenied)
                  },
                )
              }

              composable(
                route = "$ROUTE_PICKER/{epochDay}",
                arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
              ) { backStackEntry ->
                val epochDay =
                  backStackEntry.arguments?.getLong("epochDay") ?: ToymoteDate.today().epochDay
                val targetDate = ToymoteDate.fromEpochDay(epochDay)
                EmotionPickerScreen(
                  targetDate = targetDate,
                  initialEmotionId = editingEmotionId,
                  initialComment = editingComment,
                  isEditing = editingEntryId > 0L,
                  onNavigateBack = { navController.popBackStack() },
                  onSave = { emotionId, comment ->
                    viewModel.saveMoodEntry(
                      date = targetDate,
                      emotionId = emotionId,
                      comment = comment,
                      editingEntryId = editingEntryId.takeIf { it > 0L },
                    )
                    navController.popBackStack()
                  },
                )
              }
            }
          }
        }
      }
    }

    val sheetDay = uiState.selectedDayForSheet
    if (sheetDay != null) {
      val dayEntries = uiState.entriesByDay[sheetDay.epochDay].orEmpty()
      DayDetailSheet(
        date = sheetDay,
        entries = dayEntries,
        onDismiss = { viewModel.closeDaySheet() },
        onAddAnother = { date ->
          viewModel.closeDaySheet()
          openPicker(date, null, null, "")
        },
        onEditEntry = { entry ->
          viewModel.closeDaySheet()
          openPicker(
            ToymoteDate.fromEpochDay(entry.dateEpochDay),
            entry.id,
            entry.emotionId,
            entry.comment,
          )
        },
        onDeleteEntry = { entryId -> viewModel.deleteEntry(entryId) },
      )
    }

    if (showSettingsDialog) {
      SettingsDialog(
        settings = uiState.settings,
        onDismiss = { showSettingsDialog = false },
        onToggleReminder = { viewModel.updateReminderEnabled(it) },
        onSelectReminderTime = { h, m -> viewModel.updateReminderTime(h, m) },
        onSelectThemeMode = { viewModel.updateThemeMode(it) },
      )
    }
  }
}
