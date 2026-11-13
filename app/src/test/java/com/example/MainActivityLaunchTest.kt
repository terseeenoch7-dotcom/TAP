package com.example

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.TapRepository
import com.example.ui.TapViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.TAPTheme
import com.example.util.AnalyticsHelper
import com.example.util.AudioHapticHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityLaunchTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_composesAuthScreenWithoutCrashing() {
        composeTestRule.waitForIdle()
        assertNotNull(FirebaseApp.getInstance())
        composeTestRule.onNodeWithText("TAP").assertExists()
    }

    @Test
    fun homeScreen_composesAndTapsWithoutCrashing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val repository = TapRepository(firestore)
        val audioHapticHelper = AudioHapticHelper(context)
        val analyticsHelper = AnalyticsHelper(context)
        val vm = TapViewModel(
            repository = repository,
            analyticsHelper = analyticsHelper,
            currentUserId = auth.currentUser?.uid ?: "test_uid",
            userEmail = "test@example.com",
            displayName = "TestTapper",
            photoUrl = null
        )

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                TAPTheme {
                    HomeScreen(
                        uiState = vm.uiState.value,
                        viewModel = vm,
                        audioHapticHelper = audioHapticHelper,
                        onNavigateToTasks = {},
                        onNavigateToBoosters = {},
                        onOpenAiStrategist = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("coin_balance_text").assertExists()
        composeTestRule.onNodeWithTag("tap_button").performClick()
        composeTestRule.waitForIdle()
        audioHapticHelper.release()
    }
}
