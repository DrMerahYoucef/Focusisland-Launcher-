package com.example.data.repository

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.data.db.dao.AllowedAppDao
import com.example.data.db.entity.AllowedAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class LaunchableAppInfo(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?,
    val isDialer: Boolean = false
)

data class LauncherPrefsState(
    val strictFocusDrawer: Boolean = true,
    val hideNonAllowedDuringFocus: Boolean = false,
    val minimalHomeList: Boolean = false,
    val pauseOnAllowedApp: Boolean = false,
    val defaultPromptDismissed: Boolean = false
)

class LauncherRepository(
    context: Context,
    private val allowedAppDao: AllowedAppDao
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val packageManager = appContext.packageManager

    private val _installedApps = MutableStateFlow<List<LaunchableAppInfo>?>(null)
    val installedApps: StateFlow<List<LaunchableAppInfo>?> = _installedApps.asStateFlow()

    private val _prefsState = MutableStateFlow(readPrefs())
    val prefsState: StateFlow<LauncherPrefsState> = _prefsState.asStateFlow()

    @Volatile
    private var lastAllowedLaunchAtMs: Long = 0L

    @Volatile
    private var lastAllowedLaunchPkg: String? = null

    val allowedAppsFlow: Flow<List<AllowedAppEntity>> = allowedAppDao.getAllAllowed()

    private fun readPrefs(): LauncherPrefsState = LauncherPrefsState(
        strictFocusDrawer = prefs.getBoolean(KEY_STRICT_FOCUS_DRAWER, true),
        hideNonAllowedDuringFocus = prefs.getBoolean(KEY_HIDE_NON_ALLOWED, false),
        minimalHomeList = prefs.getBoolean(KEY_MINIMAL_LIST, false),
        pauseOnAllowedApp = prefs.getBoolean(KEY_PAUSE_ON_ALLOWED, false),
        defaultPromptDismissed = prefs.getBoolean(KEY_PROMPT_DISMISSED, false)
    )

    suspend fun refreshInstalledApps(seedEssentialsIfFirstRun: Boolean = true) {
        val loaded = withContext(Dispatchers.IO) {
            val dialerPkg = runCatching {
                appContext.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
            }.getOrNull()
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            runCatching {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
                    .map { it.activityInfo.applicationInfo }
                    .distinctBy { it.packageName }
                    .filter { it.packageName != appContext.packageName }
                    .map { info ->
                        LaunchableAppInfo(
                            packageName = info.packageName,
                            appName = packageManager.getApplicationLabel(info).toString(),
                            icon = runCatching {
                                packageManager.getApplicationIcon(info).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
                            }.getOrNull(),
                            isDialer = info.packageName == dialerPkg ||
                                info.packageName.contains("dialer", ignoreCase = true)
                        )
                    }
                    .sortedBy { it.appName.lowercase() }
            }.getOrDefault(emptyList())
        }
        _installedApps.value = loaded

        if (seedEssentialsIfFirstRun && !prefs.getBoolean(KEY_ESSENTIALS_SEEDED, false)) {
            prefs.edit().putBoolean(KEY_ESSENTIALS_SEEDED, true).apply()
            if (allowedAppDao.getAllAllowedOnce().isEmpty() && loaded.isNotEmpty()) {
                val essentials = pickEssentialApps(loaded)
                if (essentials.isNotEmpty()) {
                    allowedAppDao.upsertAll(
                        essentials.mapIndexed { idx, app ->
                            AllowedAppEntity(
                                packageName = app.packageName,
                                appName = app.appName,
                                sortOrder = idx,
                                isPinnedToDock = idx < 2
                            )
                        }
                    )
                }
            }
        }
    }

    suspend fun applyEssentialPreset() {
        val installed = _installedApps.value ?: run {
            refreshInstalledApps(seedEssentialsIfFirstRun = false)
            _installedApps.value.orEmpty()
        }
        val existing = allowedAppDao.getAllAllowedOnce().associateBy { it.packageName }
        val essentials = pickEssentialApps(installed)
        if (essentials.isEmpty()) return
        var nextOrder = (existing.values.maxOfOrNull { it.sortOrder } ?: -1) + 1
        var pinnedCount = existing.values.count { it.isPinnedToDock }
        val toInsert = essentials.filter { it.packageName !in existing }.map { app ->
            val pin = pinnedCount < MAX_DOCK_PINS && (app.isDialer || pinnedCount < 2)
            if (pin) pinnedCount++
            AllowedAppEntity(
                packageName = app.packageName,
                appName = app.appName,
                sortOrder = nextOrder++,
                isPinnedToDock = pin
            )
        }
        if (toInsert.isNotEmpty()) allowedAppDao.upsertAll(toInsert)
    }

    private fun pickEssentialApps(installed: List<LaunchableAppInfo>): List<LaunchableAppInfo> {
        val keywords = listOf("phone", "dialer", "messages", "sms", "camera", "calculator", "clock", "calendar", "notes")
        val matched = mutableListOf<LaunchableAppInfo>()
        installed.firstOrNull { it.isDialer }?.let { matched.add(it) }
        for (kw in keywords) {
            if (matched.size >= 6) break
            val candidate = installed.firstOrNull { app ->
                app !in matched && (
                    app.appName.contains(kw, ignoreCase = true) ||
                        app.packageName.contains(kw, ignoreCase = true)
                    )
            }
            if (candidate != null) matched.add(candidate)
        }
        if (matched.isEmpty()) {
            matched.addAll(installed.take(4))
        }
        return matched
    }

    suspend fun setAllowed(packageName: String, appName: String, allowed: Boolean) {
        if (!allowed) {
            allowedAppDao.deleteByPackage(packageName)
            return
        }
        val existing = allowedAppDao.getAllAllowedOnce()
        if (existing.any { it.packageName == packageName }) return
        val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        allowedAppDao.upsert(
            AllowedAppEntity(
                packageName = packageName,
                appName = appName,
                sortOrder = nextOrder,
                isPinnedToDock = false
            )
        )
    }

    suspend fun toggleDockPin(packageName: String, fallbackName: String): Boolean {
        val current = allowedAppDao.getAllAllowedOnce()
        val existing = current.find { it.packageName == packageName }
        if (existing == null) {
            val pinnedCount = current.count { it.isPinnedToDock }
            if (pinnedCount >= MAX_DOCK_PINS) return false
            val nextOrder = (current.maxOfOrNull { it.sortOrder } ?: -1) + 1
            allowedAppDao.upsert(
                AllowedAppEntity(
                    packageName = packageName,
                    appName = fallbackName,
                    sortOrder = nextOrder,
                    isPinnedToDock = true
                )
            )
            return true
        }
        if (!existing.isPinnedToDock && current.count { it.isPinnedToDock } >= MAX_DOCK_PINS) {
            return false
        }
        allowedAppDao.upsert(existing.copy(isPinnedToDock = !existing.isPinnedToDock))
        return true
    }

    suspend fun moveAllowed(packageName: String, delta: Int) {
        val current = allowedAppDao.getAllAllowedOnce().toMutableList()
        val index = current.indexOfFirst { it.packageName == packageName }
        if (index == -1) return
        val target = (index + delta).coerceIn(0, current.lastIndex)
        if (index == target) return
        val item = current.removeAt(index)
        current.add(target, item)
        allowedAppDao.upsertAll(current.mapIndexed { i, entity -> entity.copy(sortOrder = i) })
    }

    suspend fun clearAllAllowed() {
        allowedAppDao.deleteAll()
    }

    fun updateStrictFocusDrawer(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_FOCUS_DRAWER, enabled).apply()
        _prefsState.update { it.copy(strictFocusDrawer = enabled) }
    }

    fun updateHideNonAllowedDuringFocus(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_NON_ALLOWED, enabled).apply()
        _prefsState.update { it.copy(hideNonAllowedDuringFocus = enabled) }
    }

    fun updateMinimalHomeList(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MINIMAL_LIST, enabled).apply()
        _prefsState.update { it.copy(minimalHomeList = enabled) }
    }

    fun updatePauseOnAllowedApp(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSE_ON_ALLOWED, enabled).apply()
        _prefsState.update { it.copy(pauseOnAllowedApp = enabled) }
    }

    fun dismissDefaultPrompt() {
        prefs.edit().putBoolean(KEY_PROMPT_DISMISSED, true).apply()
        _prefsState.update { it.copy(defaultPromptDismissed = true) }
    }

    fun isPauseOnAllowedAppEnabled(): Boolean = prefs.getBoolean(KEY_PAUSE_ON_ALLOWED, false)

    fun wasRecentlyLaunchedAllowedApp(windowMs: Long = 3_000L): Boolean {
        val pkg = lastAllowedLaunchPkg ?: return false
        return pkg.isNotBlank() && (System.currentTimeMillis() - lastAllowedLaunchAtMs) <= windowMs
    }

    fun isDefaultLauncher(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = appContext.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        val resolved = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == appContext.packageName
    }

    fun createSetDefaultLauncherIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = appContext.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun launchApp(context: Context, packageName: String, isAllowed: Boolean): Boolean {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        } ?: return false
        return runCatching {
            if (isAllowed) {
                lastAllowedLaunchPkg = packageName
                lastAllowedLaunchAtMs = System.currentTimeMillis()
            } else {
                lastAllowedLaunchPkg = null
                lastAllowedLaunchAtMs = 0L
            }
            context.startActivity(launchIntent)
        }.isSuccess
    }

    fun launchDialer(context: Context): Boolean {
        val dialerPkg = _installedApps.value?.firstOrNull { it.isDialer }?.packageName
        if (dialerPkg != null && launchApp(context, dialerPkg, isAllowed = true)) return true
        return runCatching {
            lastAllowedLaunchPkg = "dialer"
            lastAllowedLaunchAtMs = System.currentTimeMillis()
            context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }

    fun openAppInfo(context: Context, packageName: String): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.isSuccess

    companion object {
        private const val PREFS = "focusflow_prefs"
        private const val ICON_PX = 96
        const val MAX_DOCK_PINS = 4
        private const val KEY_ESSENTIALS_SEEDED = "launcher_essentials_seeded"
        private const val KEY_STRICT_FOCUS_DRAWER = "launcher_strict_focus_drawer"
        private const val KEY_HIDE_NON_ALLOWED = "launcher_hide_non_allowed_during_focus"
        private const val KEY_MINIMAL_LIST = "launcher_minimal_home_list"
        private const val KEY_PAUSE_ON_ALLOWED = "launcher_pause_on_allowed_app"
        private const val KEY_PROMPT_DISMISSED = "launcher_default_prompt_dismissed"
    }
}
