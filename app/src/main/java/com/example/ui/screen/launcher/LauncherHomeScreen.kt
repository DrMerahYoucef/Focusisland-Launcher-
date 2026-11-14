package com.example.ui.screen.launcher

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.LastSound
import com.example.service.Phase
import com.example.service.PomodoroTimerService
import com.example.service.TimerState
import com.example.ui.designsystem.BannerTone
import com.example.ui.designsystem.EmptyState
import com.example.ui.designsystem.InfoBanner
import com.example.ui.designsystem.LoadingState
import com.example.ui.screen.radio.PlaybackStatus
import com.example.ui.screen.radio.RadioViewModel
import com.example.ui.screen.radio.StationArt
import com.example.ui.screen.timer.FocusToday
import com.example.ui.screen.timer.TimerViewModel
import com.example.ui.screen.timer.ambientLabel
import com.example.ui.screen.timer.formatClock
import com.example.ui.screen.timer.phaseLabel
import com.example.ui.theme.PhaseTexturedBackground
import com.example.ui.theme.Spacing
import com.example.ui.theme.phaseColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherHomeScreen(
    launcherViewModel: LauncherViewModel,
    timerViewModel: TimerViewModel,
    radioViewModel: RadioViewModel = viewModel(),
    isHomeTabActive: Boolean = true,
    drawerOpen: Boolean,
    onDrawerOpenChange: (Boolean) -> Unit,
    onOpenAllowedApps: () -> Unit,
    onOpenTimerTab: () -> Unit,
    onOpenRadio: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hostView = LocalView.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by launcherViewModel.uiState.collectAsStateWithLifecycle()
    val today by timerViewModel.today.collectAsStateWithLifecycle()
    val lastAmbient by timerViewModel.lastAmbient.collectAsState()
    val lastSoundKind by timerViewModel.lastSoundKind.collectAsState()
    val radioPlaying by radioViewModel.isPlaying.collectAsState()
    val currentStation by radioViewModel.currentStation.collectAsState()
    val radioStatus by radioViewModel.status.collectAsState()

    var selectedMenuApp by remember { mutableStateOf<LauncherAppItem?>(null) }
    var guardPendingApp by remember { mutableStateOf<LauncherAppItem?>(null) }

    val dockLimitMsg = stringResource(R.string.launcher_dock_limit_hint)

    val homeRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        launcherViewModel.refreshDefaultLauncherStatus()
    }

    LifecycleResumeEffect(Unit) {
        launcherViewModel.refreshDefaultLauncherStatus()
        onPauseOrDispose { }
    }

    // Close modals on Back, and consume Back on the root Home Launcher so pressing Back never exits Home.
    BackHandler(enabled = isHomeTabActive) {
        when {
            guardPendingApp != null -> guardPendingApp = null
            selectedMenuApp != null -> selectedMenuApp = null
            drawerOpen -> onDrawerOpenChange(false)
            else -> Unit
        }
    }

    fun handleLaunch(app: LauncherAppItem, bypassGuard: Boolean = false) {
        when (val result = launcherViewModel.attemptLaunch(context, app, bypassGuard)) {
            LaunchResult.Launched -> onDrawerOpenChange(false)
            is LaunchResult.NeedsFocusGuard -> guardPendingApp = result.app
            is LaunchResult.Failed -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.launcher_app_launch_error, result.appName))
            }
        }
    }

    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            val msToNextSecond = 1_000L - (System.currentTimeMillis() % 1_000L)
            delay(msToNextSecond)
        }
    }
    val timeText = remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val dateText = remember(now) {
        now.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    PhaseTexturedBackground(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(drawerOpen) {
                    detectVerticalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            dragAccumulator += dragAmount
                            if (!drawerOpen && dragAccumulator < -96f) {
                                dragAccumulator = 0f
                                onDrawerOpenChange(true)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = Spacing.contentMaxWidth)
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val columns = if (maxWidth >= 560.dp) 6 else 4
                    val appRows = remember(uiState.allowedApps, columns) {
                        uiState.allowedApps.chunked(columns)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = Spacing.xs, bottom = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        item(key = "header") {
                            LauncherTopHeader(
                                timeText = timeText,
                                dateText = dateText,
                                today = today,
                                points = today.minutes,
                                minimalMode = uiState.prefs.minimalHomeList,
                                onToggleLayout = {
                                    launcherViewModel.updateMinimalHomeList(!uiState.prefs.minimalHomeList)
                                },
                                onOpenAllowedApps = onOpenAllowedApps,
                                onOpenSettings = onOpenSettings
                            )
                        }

                        if (!uiState.isDefaultLauncher && !uiState.prefs.defaultPromptDismissed) {
                            item(key = "default_launcher_card") {
                                DefaultLauncherCard(
                                    onSetDefault = {
                                        runCatching {
                                            homeRoleLauncher.launch(launcherViewModel.createSetDefaultLauncherIntent())
                                        }
                                    },
                                    onDismiss = launcherViewModel::dismissDefaultPrompt
                                )
                            }
                        }

                        item(key = "pomodoro_card") {
                            PomodoroLauncherCard(
                                state = uiState.timerState,
                                onOpenTimer = onOpenTimerTab,
                                onToggleTimer = {
                                    hostView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    when {
                                        uiState.timerState.isRunning -> timerViewModel.pauseTimer()
                                        uiState.timerState.isUntouched -> timerViewModel.startTimer()
                                        else -> timerViewModel.resumeTimer()
                                    }
                                }
                            )
                        }

                        item(key = "radio_pill") {
                            RadioLauncherPill(
                                timerState = uiState.timerState,
                                lastAmbient = lastAmbient,
                                lastSoundKind = lastSoundKind,
                                radioPlaying = radioPlaying,
                                station = currentStation,
                                radioStatus = radioStatus,
                                onOpenRadio = onOpenRadio,
                                onToggleSound = {
                                    val ambientOn = uiState.timerState.currentAmbientId != PomodoroTimerService.AMBIENT_NONE
                                    when {
                                        radioPlaying -> radioViewModel.togglePlayback(context)
                                        ambientOn -> timerViewModel.setAmbientSound(PomodoroTimerService.AMBIENT_NONE)
                                        lastSoundKind == LastSound.KIND_AMBIENT && lastAmbient != null -> {
                                            timerViewModel.setAmbientSound(lastAmbient!!)
                                        }
                                        currentStation != null -> radioViewModel.togglePlayback(context)
                                        lastAmbient != null -> timerViewModel.setAmbientSound(lastAmbient!!)
                                        else -> onOpenRadio()
                                    }
                                }
                            )
                        }

                        item(key = "allowed_apps_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${stringResource(R.string.launcher_allowed_apps_title)} (${uiState.allowedApps.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TextButton(
                                    onClick = onOpenAllowedApps,
                                    modifier = Modifier.testTag("launcher_edit_allowed_button")
                                ) {
                                    Icon(Icons.Rounded.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(Spacing.xxs))
                                    Text(stringResource(R.string.launcher_manage_allowed))
                                }
                            }
                        }

                        when {
                            uiState.isLoadingApps -> {
                                item(key = "apps_loading") {
                                    LoadingState(Modifier.fillMaxWidth().height(160.dp))
                                }
                            }
                            uiState.allowedApps.isEmpty() -> {
                                item(key = "apps_empty") {
                                    Surface(
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        EmptyState(
                                            icon = Icons.Rounded.Apps,
                                            title = stringResource(R.string.launcher_allowed_empty_title),
                                            body = stringResource(R.string.launcher_allowed_empty_body),
                                            actionLabel = stringResource(R.string.launcher_choose_allowed_cta),
                                            onAction = onOpenAllowedApps,
                                            modifier = Modifier.padding(vertical = Spacing.md)
                                        )
                                    }
                                }
                            }
                            uiState.prefs.minimalHomeList -> {
                                items(uiState.allowedApps, key = { it.packageName }) { app ->
                                    MinimalAllowedAppRow(
                                        app = app,
                                        onClick = { handleLaunch(app) },
                                        onLongClick = {
                                            hostView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            selectedMenuApp = app
                                        }
                                    )
                                }
                            }
                            else -> {
                                items(
                                    items = appRows,
                                    key = { row -> row.joinToString(",") { it.packageName } }
                                ) { rowApps ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                    ) {
                                        rowApps.forEach { app ->
                                            LauncherAppGridTile(
                                                app = app,
                                                onClick = { handleLaunch(app) },
                                                onLongClick = {
                                                    hostView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                    selectedMenuApp = app
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(columns - rowApps.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Subtle swipe hint + Bottom Launcher Dock
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onDrawerOpenChange(true) }
                            .padding(horizontal = Spacing.sm, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.launcher_swipe_up_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LauncherBottomDock(
                        dockApps = uiState.dockApps,
                        onLaunchDockApp = { handleLaunch(it) },
                        onLaunchDialerFallback = { launcherViewModel.launchDialer(context) },
                        onOpenDrawer = { onDrawerOpenChange(true) },
                        onOpenFocusHub = onOpenTimerTab
                    )
                }
            }

            AnimatedVisibility(
                visible = drawerOpen,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                AppDrawerOverlay(
                    uiState = uiState,
                    onQueryChange = launcherViewModel::updateDrawerQuery,
                    onFilterChange = launcherViewModel::updateDrawerFilter,
                    onLaunchApp = { handleLaunch(it) },
                    onLongPressApp = {
                        hostView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        selectedMenuApp = it
                    },
                    onManageAllowed = {
                        onDrawerOpenChange(false)
                        onOpenAllowedApps()
                    },
                    onDismiss = { onDrawerOpenChange(false) }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
            )
        }
    }

    selectedMenuApp?.let { app ->
        AppActionsBottomSheet(
            app = app,
            onOpen = {
                selectedMenuApp = null
                handleLaunch(app)
            },
            onToggleAllowed = {
                launcherViewModel.toggleAllowed(app)
                selectedMenuApp = null
            },
            onToggleDockPin = {
                launcherViewModel.toggleDockPin(app) {
                    scope.launch { snackbarHostState.showSnackbar(dockLimitMsg) }
                }
                selectedMenuApp = null
            },
            onAppInfo = {
                selectedMenuApp = null
                launcherViewModel.openAppInfo(context, app.packageName)
            },
            onDismiss = { selectedMenuApp = null }
        )
    }

    guardPendingApp?.let { guarded ->
        AlertDialog(
            onDismissRequest = { guardPendingApp = null },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            title = { Text(stringResource(R.string.launcher_focus_guard_title)) },
            text = { Text(stringResource(R.string.launcher_focus_guard_body, guarded.appName)) },
            confirmButton = {
                Button(onClick = { guardPendingApp = null }) {
                    Text(stringResource(R.string.launcher_focus_guard_stay))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    TextButton(
                        onClick = {
                            guardPendingApp = null
                            handleLaunch(guarded, bypassGuard = true)
                        }
                    ) {
                        Text(stringResource(R.string.launcher_focus_guard_open_once))
                    }
                    FilledTonalButton(
                        onClick = {
                            guardPendingApp = null
                            when (val res = launcherViewModel.allowAndLaunch(context, guarded)) {
                                LaunchResult.Launched -> onDrawerOpenChange(false)
                                is LaunchResult.Failed -> scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.launcher_app_launch_error, res.appName)
                                    )
                                }
                                is LaunchResult.NeedsFocusGuard -> Unit
                            }
                        }
                    ) {
                        Text(stringResource(R.string.launcher_focus_guard_allow_open))
                    }
                }
            }
        )
    }
}

@Composable
private fun LauncherTopHeader(
    timeText: String,
    dateText: String,
    today: FocusToday,
    points: Int,
    minimalMode: Boolean,
    onToggleLayout: () -> Unit,
    onOpenAllowedApps: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xxs))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.launcher_streak_badge, today.streakDays),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.launcher_points_badge, points),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            FilledTonalIconButton(
                onClick = onToggleLayout,
                modifier = Modifier
                    .size(Spacing.minTouchTarget)
                    .testTag("launcher_toggle_layout_button")
            ) {
                Icon(
                    imageVector = if (minimalMode) Icons.Rounded.ViewModule else Icons.AutoMirrored.Rounded.ViewList,
                    contentDescription = stringResource(R.string.launcher_settings_minimal_layout)
                )
            }
            FilledTonalIconButton(
                onClick = onOpenAllowedApps,
                modifier = Modifier
                    .size(Spacing.minTouchTarget)
                    .testTag("launcher_manage_allowed_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Checklist,
                    contentDescription = stringResource(R.string.launcher_allowed_apps_title)
                )
            }
            FilledTonalIconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(Spacing.minTouchTarget)
                    .testTag("launcher_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.nav_settings)
                )
            }
        }
    }
}

@Composable
private fun DefaultLauncherCard(
    onSetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(Icons.Rounded.Home, contentDescription = null)
                Text(
                    text = stringResource(R.string.launcher_default_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = stringResource(R.string.launcher_default_banner_body),
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.launcher_dismiss_cta))
                }
                Spacer(Modifier.width(Spacing.xs))
                Button(
                    onClick = onSetDefault,
                    modifier = Modifier.testTag("launcher_set_default_button")
                ) {
                    Text(stringResource(R.string.launcher_set_default_cta))
                }
            }
        }
    }
}

@Composable
private fun PomodoroLauncherCard(
    state: TimerState,
    onOpenTimer: () -> Unit,
    onToggleTimer: () -> Unit
) {
    val accent = phaseColor(state.phase)
    val duration = state.phaseDurationMs.coerceAtLeast(1L)
    val progress = (1f - state.remainingMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    Surface(
        onClick = onOpenTimer,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (state.isRunning) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.large
            )
            .testTag("launcher_pomodoro_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.18f),
                    contentColor = accent,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Timer, contentDescription = null)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.sm)
                ) {
                    Text(
                        text = stringResource(phaseLabel(state.phase)),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatClock(state.remainingMs),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFeatureSettings = "tnum",
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.launcher_timer_open_full),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = Spacing.xs)
                )
                FilledIconButton(
                    onClick = onToggleTimer,
                    modifier = Modifier
                        .size(Spacing.minTouchTarget)
                        .testTag("launcher_pomodoro_toggle_button")
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(
                            if (state.isRunning) R.string.focus_pause else R.string.focus_start
                        )
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                color = accent,
                trackColor = accent.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun RadioLauncherPill(
    timerState: TimerState,
    lastAmbient: String?,
    lastSoundKind: String?,
    radioPlaying: Boolean,
    station: com.example.data.RadioStation?,
    radioStatus: PlaybackStatus,
    onOpenRadio: () -> Unit,
    onToggleSound: () -> Unit
) {
    val ambientOn = timerState.currentAmbientId != PomodoroTimerService.AMBIENT_NONE
    val activeStation = when {
        radioPlaying && station != null -> station
        !ambientOn && lastSoundKind != LastSound.KIND_AMBIENT && station != null -> station
        !ambientOn && lastAmbient == null && station != null -> station
        else -> null
    }
    val activeAmbientId = when {
        ambientOn -> timerState.currentAmbientId
        lastSoundKind == LastSound.KIND_AMBIENT && lastAmbient != null -> lastAmbient
        activeStation == null && lastAmbient != null -> lastAmbient
        else -> null
    }

    val playing = radioPlaying || (ambientOn && timerState.isRunning)
    val title = when {
        activeStation != null -> activeStation.name
        activeAmbientId != null -> stringResource(ambientLabel(activeAmbientId))
        else -> stringResource(R.string.launcher_radio_choose_station)
    }
    val subtitle = when {
        activeStation != null -> when (radioStatus) {
            PlaybackStatus.PLAYING -> activeStation.country.ifBlank { stringResource(R.string.sound_radio) }
            PlaybackStatus.BUFFERING -> stringResource(R.string.radio_status_connecting)
            PlaybackStatus.ERROR -> stringResource(R.string.sound_radio_error_short)
            else -> stringResource(R.string.radio_status_paused)
        }
        activeAmbientId != null -> stringResource(
            if (ambientOn) R.string.sound_ambient_playing else R.string.radio_status_paused
        )
        else -> stringResource(R.string.launcher_radio_open_full)
    }

    Surface(
        onClick = onOpenRadio,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("launcher_radio_card")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeStation != null) {
                StationArt(activeStation, playing = radioPlaying, size = 44.dp)
            } else {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (activeAmbientId != null) Icons.Rounded.GraphicEq else Icons.Rounded.Radio,
                            contentDescription = null
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.sm)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalIconButton(
                onClick = onToggleSound,
                modifier = Modifier
                    .size(Spacing.minTouchTarget)
                    .testTag("launcher_radio_toggle_button")
            ) {
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (playing) R.string.radio_pause else R.string.radio_play)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherAppGridTile(
    app: LauncherAppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.78f),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("allowed_app_${app.packageName}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppIconAvatar(app = app, size = 48.dp)
            Text(
                text = app.appName,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalAllowedAppRow(
    app: LauncherAppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.84f),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("allowed_app_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            AppIconAvatar(app = app, size = 34.dp)
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (app.isPinnedToDock) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = stringResource(R.string.launcher_badge_dock),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LauncherBottomDock(
    dockApps: List<LauncherAppItem>,
    onLaunchDockApp: (LauncherAppItem) -> Unit,
    onLaunchDialerFallback: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenFocusHub: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (dockApps.isEmpty()) {
                IconButton(
                    onClick = onLaunchDialerFallback,
                    modifier = Modifier.size(Spacing.minTouchTarget)
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = stringResource(R.string.launcher_dock_dialer))
                }
            } else {
                dockApps.take(4).forEach { app ->
                    IconButton(
                        onClick = { onLaunchDockApp(app) },
                        modifier = Modifier
                            .size(Spacing.minTouchTarget)
                            .testTag("dock_app_${app.packageName}")
                    ) {
                        AppIconAvatar(app = app, size = 34.dp)
                    }
                }
            }

            FilledTonalIconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(Spacing.minTouchTarget)
                    .testTag("launcher_open_drawer_button")
            ) {
                Icon(
                    Icons.Rounded.Apps,
                    contentDescription = stringResource(R.string.launcher_all_apps)
                )
            }

            FilledTonalButton(
                onClick = onOpenFocusHub,
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 8.dp),
                modifier = Modifier
                    .heightIn(min = Spacing.minTouchTarget)
                    .testTag("launcher_open_focus_hub_button")
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.launcher_focus_hub_cta),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppDrawerOverlay(
    uiState: LauncherUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (DrawerFilter) -> Unit,
    onLaunchApp: (LauncherAppItem) -> Unit,
    onLongPressApp: (LauncherAppItem) -> Unit,
    onManageAllowed: () -> Unit,
    onDismiss: () -> Unit
) {
    val visibleApps = uiState.filteredDrawerApps
    var dragDown by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.97f),
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragDown = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                dragDown += dragAmount
                                if (dragDown > 84f) {
                                    dragDown = 0f
                                    onDismiss()
                                }
                            }
                        )
                    }
                    .padding(top = Spacing.sm, bottom = Spacing.xxs),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.launcher_all_apps),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onManageAllowed) {
                        Icon(Icons.Rounded.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(Spacing.xxs))
                        Text(stringResource(R.string.launcher_manage_allowed))
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("app_drawer_close_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
            }

            if (uiState.isFocusing && uiState.prefs.strictFocusDrawer) {
                InfoBanner(
                    text = stringResource(R.string.launcher_drawer_focus_active_banner),
                    tone = BannerTone.Info
                )
            }

            OutlinedTextField(
                value = uiState.drawerQuery,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.launcher_drawer_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.drawerQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_drawer_search_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = uiState.drawerFilter == DrawerFilter.ALL,
                    onClick = { onFilterChange(DrawerFilter.ALL) },
                    label = { Text(stringResource(R.string.launcher_drawer_filter_all, uiState.allApps.size)) }
                )
                FilterChip(
                    selected = uiState.drawerFilter == DrawerFilter.ALLOWED,
                    onClick = { onFilterChange(DrawerFilter.ALLOWED) },
                    label = { Text(stringResource(R.string.launcher_drawer_filter_allowed, uiState.allowedApps.size)) }
                )
            }

            if (uiState.isLoadingApps) {
                LoadingState(Modifier.fillMaxSize())
            } else if (visibleApps.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = stringResource(R.string.launcher_drawer_empty),
                    body = stringResource(R.string.launcher_allowed_empty_body),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.lg)
                ) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        val guarded = uiState.isFocusing && uiState.prefs.strictFocusDrawer && !app.isAllowed
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = app.appName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (guarded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                    if (app.isAllowed) {
                                        Text(
                                            text = stringResource(R.string.launcher_badge_allowed),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (app.isBlocked) {
                                        Text(
                                            text = stringResource(R.string.launcher_badge_blocked),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            leadingContent = { AppIconAvatar(app = app, size = 40.dp) },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    if (app.isAllowed) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = stringResource(R.string.launcher_badge_allowed),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (guarded) {
                                        Icon(
                                            Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (app.isBlocked) {
                                        Icon(
                                            Icons.Rounded.Block,
                                            contentDescription = stringResource(R.string.launcher_badge_blocked),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .combinedClickable(
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onLongPressApp(app) }
                                )
                                .testTag("drawer_app_${app.packageName}")
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppActionsBottomSheet(
    app: LauncherAppItem,
    onOpen: () -> Unit,
    onToggleAllowed: () -> Unit,
    onToggleDockPin: () -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            ) {
                AppIconAvatar(app = app, size = 40.dp)
                Column {
                    Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = Spacing.xs))
            ListItem(
                headlineContent = { Text(stringResource(R.string.launcher_action_open)) },
                leadingContent = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpen)
            )
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            if (app.isAllowed) R.string.launcher_action_remove_allowed
                            else R.string.launcher_action_allow
                        )
                    )
                },
                leadingContent = { Icon(Icons.Rounded.Checklist, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onToggleAllowed)
            )
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            if (app.isPinnedToDock) R.string.launcher_action_unpin_dock
                            else R.string.launcher_action_pin_dock
                        )
                    )
                },
                leadingContent = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onToggleDockPin)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.launcher_action_app_info)) },
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onAppInfo)
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
internal fun AppIconAvatar(
    app: LauncherAppItem,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val icon = app.icon
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = app.appName,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Android, contentDescription = app.appName)
            }
        }
    }
}
