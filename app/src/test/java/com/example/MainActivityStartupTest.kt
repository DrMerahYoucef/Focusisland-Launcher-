package com.example

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.challenge.ChallengeResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = FocusFlowApplication::class)
class MainActivityStartupTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fullAppFlowAndTabsAndSettingsAndDialogsDoNotCrash() {
        // 1. Verify "Continue without an account" option is removed from AuthScreen
        val guestNodes = composeRule.onAllNodes(hasText("Continue without an account", substring = true))
        org.junit.Assert.assertTrue(guestNodes.fetchSemanticsNodes().isEmpty())

        // Enter app for test via in-memory auth session and ensure clean challenge state
        composeRule.activityRule.scenario.onActivity {
            FocusFlowApplication.instance.authRepository.continueAsGuest()
            val store = FocusFlowApplication.instance.challengeStore
            if (store.state.value.active != null) {
                store.finish(ChallengeResult.DONE)
            }
            store.clearHistory()
        }
        composeRule.waitForIdle()

        // 2. Verify Launcher Home screen, Allowed Apps screen, and App Drawer
        composeRule.onNodeWithTag("launcher_pomodoro_card").assertExists()
        composeRule.onNodeWithTag("launcher_radio_card").assertExists()

        composeRule.onNodeWithTag("launcher_manage_allowed_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("allowed_apps_screen").assertExists()
        composeRule.onNodeWithTag("allowed_apps_back_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("launcher_open_drawer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_drawer_search_input").assertExists()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        // 3. Enter Focus Hub workspace and navigate to Challenges tab
        composeRule.onNodeWithTag("launcher_open_focus_hub_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Challenges").performClick()
        composeRule.waitForIdle()

        // Ensure Personal tab is selected before opening New Challenge bottom sheet
        val personalTabNodes = composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("challenges_tab_personal"))
        if (personalTabNodes.fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("challenges_tab_personal").performClick()
            composeRule.waitForIdle()
        }

        // 4. Open New Challenge bottom sheet and verify Lock Phone Permitted Apps selector
        composeRule.onNodeWithText("New challenge").performScrollTo()
        composeRule.onNodeWithText("New challenge").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("personal_mode_lock_phone").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lock_phone_permitted_apps_card").performScrollTo()
        composeRule.onNodeWithTag("lock_phone_choose_apps_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("permitted_picker_save_button").performClick()
        composeRule.waitForIdle()

        // 5. Close sheet and navigate to Study, Progress, Community tabs
        composeRule.onNodeWithTag("new_challenge_close_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Study").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Progress").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Community").performClick()
        composeRule.waitForIdle()

        // 6. Return to Focus tab where the Settings button lives, open Settings and switch language
        composeRule.onNodeWithTag("workspace_tab_focus").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("workspace_settings_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Français").performScrollTo()
        composeRule.onNodeWithText("Français").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("English").performScrollTo()
        composeRule.onNodeWithText("English").performClick()
        composeRule.waitForIdle()

        // 7. Return from Settings and press Back from Focus workspace to return to Launcher Home
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("launcher_pomodoro_card").assertExists()
    }
}
