package com.example.ui.screen.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.data.db.entity.AllowedAppEntity
import com.example.data.repository.LaunchableAppInfo
import com.example.data.repository.LauncherPrefsState
import com.example.data.repository.LauncherRepository
import com.example.service.Phase
import com.example.service.PomodoroTimerService
import com.example.service.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LauncherAppItem(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?,
    val isAllowed: Boolean,
    val isPinnedToDock: Boolean,
    val isBlocked: Boolean,
    val isDialer: Boolean,
    val sortOrder: Int
)

enum class DrawerFilter { ALL, ALLOWED }
enum class AllowedFilter { ALL, ALLOWED, PINNED }

sealed interface LaunchResult {
    data object Launched : LaunchResult
    data class NeedsFocusGuard(val app: LauncherAppItem) : LaunchResult
    data class Failed(val appName: String) : LaunchResult
}

data class LauncherUiState(
    val isLoadingApps: Boolean = true,
    val allApps: List<LauncherAppItem> = emptyList(),
    val allowedApps: List<LauncherAppItem> = emptyList(),
    val dockApps: List<LauncherAppItem> = emptyList(),
    val drawerQuery: String = "",
    val drawerFilter: DrawerFilter = DrawerFilter.ALL,
    val allowedQuery: String = "",
    val allowedFilter: AllowedFilter = AllowedFilter.ALL,
    val isDefaultLauncher: Boolean = false,
    val prefs: LauncherPrefsState = LauncherPrefsState(),
    val timerState: TimerState = TimerState()
) {
    val isFocusing: Boolean
        get() = timerState.isRunning && timerState.phase == Phase.FOCUS

    val filteredDrawerApps: List<LauncherAppItem>
        get() {
            val q = drawerQuery.trim()
            return allApps.filter { app ->
                val matchesFilter = when {
                    drawerFilter == DrawerFilter.ALLOWED -> app.isAllowed
                    prefs.hideNonAllowedDuringFocus && isFocusing -> app.isAllowed
                    else -> true
                }
                val matchesQuery = q.isBlank() ||
                    app.appName.contains(q, ignoreCase = true) ||
                    app.packageName.contains(q, ignoreCase = true)
                matchesFilter && matchesQuery
            }
        }

    val filteredAllowedManagerApps: List<LauncherAppItem>
        get() {
            val q = allowedQuery.trim()
            val base = when (allowedFilter) {
                AllowedFilter.ALL -> allApps.sortedWith(
                    compareByDescending<LauncherAppItem> { it.isAllowed }
                        .thenBy { if (it.isAllowed) it.sortOrder else Int.MAX_VALUE }
                        .thenBy { it.appName.lowercase() }
                )
                AllowedFilter.ALLOWED -> allowedApps
                AllowedFilter.PINNED -> allApps.filter { it.isPinnedToDock }
                    .sortedWith(compareBy<LauncherAppItem> { it.sortOrder }.thenBy { it.appName.lowercase() })
            }
            return if (q.isBlank()) base else base.filter {
                it.appName.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
            }
        }
}

private data class LauncherCombinedInputs(
    val installed: List<LaunchableAppInfo>?,
    val allowed: List<AllowedAppEntity>,
    val blockedPackages: Set<String>,
    val prefs: LauncherPrefsState,
    val timer: TimerState
)

private data class LauncherSearchInputs(
    val drawerQuery: String,
    val drawerFilter: DrawerFilter,
    val allowedQuery: String,
    val allowedFilter: AllowedFilter,
    val isDefault: Boolean
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val app = FocusFlowApplication.instance
    private val launcherRepo: LauncherRepository = app.launcherRepository
    private val blockedRepo = app.blockedAppRepository

    private val _drawerQuery = MutableStateFlow("")
    private val _drawerFilter = MutableStateFlow(DrawerFilter.ALL)
    private val _allowedQuery = MutableStateFlow("")
    private val _allowedFilter = MutableStateFlow(AllowedFilter.ALL)
    private val _isDefaultLauncher = MutableStateFlow(launcherRepo.isDefaultLauncher())

    init {
        refreshInstalledApps()
    }

    private val dataInputs = combine(
        launcherRepo.installedApps,
        launcherRepo.allowedAppsFlow,
        blockedRepo.getAllBlocked(),
        launcherRepo.prefsState,
        PomodoroTimerService.state
    ) { installed, allowed, blocked, prefs, timer ->
        LauncherCombinedInputs(
            installed = installed,
            allowed = allowed,
            blockedPackages = blocked.filter { it.blockLaunch }.map { it.packageName }.toSet(),
            prefs = prefs,
            timer = timer
        )
    }

    private val searchInputs = combine(
        _drawerQuery,
        _drawerFilter,
        _allowedQuery,
        _allowedFilter,
        _isDefaultLauncher
    ) { dq, df, aq, af, def ->
        LauncherSearchInputs(dq, df, aq, af, def)
    }

    val uiState: StateFlow<LauncherUiState> = combine(dataInputs, searchInputs) { data, search ->
        val allowedByPkg = data.allowed.associateBy { it.packageName }
        val installed = data.installed
        val mergedAll = installed.orEmpty().map { info ->
            val allowedEntity = allowedByPkg[info.packageName]
            LauncherAppItem(
                packageName = info.packageName,
                appName = info.appName,
                icon = info.icon,
                isAllowed = allowedEntity != null,
                isPinnedToDock = allowedEntity?.isPinnedToDock == true,
                isBlocked = info.packageName in data.blockedPackages,
                isDialer = info.isDialer,
                sortOrder = allowedEntity?.sortOrder ?: Int.MAX_VALUE
            )
        }
        val allowedOrdered = mergedAll
            .filter { it.isAllowed }
            .sortedWith(compareBy<LauncherAppItem> { it.sortOrder }.thenBy { it.appName.lowercase() })

        val pinned = allowedOrdered.filter { it.isPinnedToDock }.take(LauncherRepository.MAX_DOCK_PINS)
        val dockApps = if (pinned.isNotEmpty()) {
            pinned
        } else {
            val dialer = mergedAll.firstOrNull { it.isDialer }
            (listOfNotNull(dialer) + allowedOrdered.filter { it.packageName != dialer?.packageName }).take(2)
        }

        LauncherUiState(
            isLoadingApps = installed == null,
            allApps = mergedAll,
            allowedApps = allowedOrdered,
            dockApps = dockApps,
            drawerQuery = search.drawerQuery,
            drawerFilter = search.drawerFilter,
            allowedQuery = search.allowedQuery,
            allowedFilter = search.allowedFilter,
            isDefaultLauncher = search.isDefault,
            prefs = data.prefs,
            timerState = data.timer
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherUiState())

    fun refreshInstalledApps() {
        viewModelScope.launch {
            launcherRepo.refreshInstalledApps(seedEssentialsIfFirstRun = true)
        }
    }

    fun refreshDefaultLauncherStatus() {
        _isDefaultLauncher.value = launcherRepo.isDefaultLauncher()
    }

    fun createSetDefaultLauncherIntent(): Intent = launcherRepo.createSetDefaultLauncherIntent()

    fun dismissDefaultPrompt() = launcherRepo.dismissDefaultPrompt()

    fun updateDrawerQuery(query: String) {
        _drawerQuery.value = query
    }

    fun updateDrawerFilter(filter: DrawerFilter) {
        _drawerFilter.value = filter
    }

    fun updateAllowedQuery(query: String) {
        _allowedQuery.value = query
    }

    fun updateAllowedFilter(filter: AllowedFilter) {
        _allowedFilter.value = filter
    }

    fun setAllowed(app: LauncherAppItem, allowed: Boolean) {
        viewModelScope.launch {
            launcherRepo.setAllowed(app.packageName, app.appName, allowed)
        }
    }

    fun toggleAllowed(app: LauncherAppItem) = setAllowed(app, !app.isAllowed)

    fun toggleDockPin(app: LauncherAppItem, onLimitReached: () -> Unit = {}) {
        viewModelScope.launch {
            val ok = launcherRepo.toggleDockPin(app.packageName, app.appName)
            if (!ok) onLimitReached()
        }
    }

    fun moveAllowed(packageName: String, delta: Int) {
        viewModelScope.launch {
            launcherRepo.moveAllowed(packageName, delta)
        }
    }

    fun applyEssentialPreset() {
        viewModelScope.launch {
            launcherRepo.applyEssentialPreset()
        }
    }

    fun clearAllAllowed() {
        viewModelScope.launch {
            launcherRepo.clearAllAllowed()
        }
    }

    fun updateStrictFocusDrawer(enabled: Boolean) = launcherRepo.updateStrictFocusDrawer(enabled)
    fun updateHideNonAllowedDuringFocus(enabled: Boolean) = launcherRepo.updateHideNonAllowedDuringFocus(enabled)
    fun updateMinimalHomeList(enabled: Boolean) = launcherRepo.updateMinimalHomeList(enabled)
    fun updatePauseOnAllowedApp(enabled: Boolean) = launcherRepo.updatePauseOnAllowedApp(enabled)

    fun attemptLaunch(context: Context, app: LauncherAppItem, bypassGuard: Boolean = false): LaunchResult {
        val state = uiState.value
        if (state.isFocusing && state.prefs.strictFocusDrawer && !app.isAllowed && !bypassGuard) {
            return LaunchResult.NeedsFocusGuard(app)
        }
        val launched = launcherRepo.launchApp(context, app.packageName, isAllowed = app.isAllowed)
        return if (launched) LaunchResult.Launched else LaunchResult.Failed(app.appName)
    }

    fun allowAndLaunch(context: Context, app: LauncherAppItem): LaunchResult {
        viewModelScope.launch {
            launcherRepo.setAllowed(app.packageName, app.appName, allowed = true)
            blockedRepo.deleteByPackage(app.packageName)
        }
        val launched = launcherRepo.launchApp(context, app.packageName, isAllowed = true)
        return if (launched) LaunchResult.Launched else LaunchResult.Failed(app.appName)
    }

    fun launchDialer(context: Context): Boolean = launcherRepo.launchDialer(context)

    fun openAppInfo(context: Context, packageName: String): Boolean = launcherRepo.openAppInfo(context, packageName)
}
