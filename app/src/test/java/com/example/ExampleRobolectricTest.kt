package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.data.DemoLocalChatRepository
import com.example.ui.ChatViewModel
import com.example.ui.components.ChatWindow
import com.example.ui.components.Sidebar
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.GlyphChatTheme
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:test")
                    .setApiKey("test-api-key")
                    .setProjectId("demo-test")
                    .build()
            )
        }
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GlyphChat", appName)
    }

    @Test
    fun `auth screen renders liquid glass card, toggles peek preview and light dark mode`() {
        var darkTheme by mutableStateOf(false)

        composeTestRule.setContent {
            GlyphChatTheme(darkTheme = darkTheme) {
                AuthScreen(
                    isDarkTheme = darkTheme,
                    onToggleTheme = { darkTheme = !darkTheme },
                    onAuthSuccess = {},
                    onEnterDemoMode = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("enter_demo_mode_button").assertIsDisplayed()

        // Toggle Peek decode on compact preview bubble
        composeTestRule.onNodeWithTag("auth_peek_preview_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MEET AT ZERO HOUR").assertIsDisplayed()

        // Toggle Light -> Dark mode and verify clean re-render
        composeTestRule.onNodeWithTag("auth_toggle_theme_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, darkTheme)
        composeTestRule.onNodeWithText("MEET AT ZERO HOUR").assertIsDisplayed()
    }

    @Test
    fun `chat window and sidebar verify messages, groups, peek decode, cipher keyboard, and theme toggle`() {
        val repository = DemoLocalChatRepository()
        val viewModel = ChatViewModel(
            repository = repository,
            currentUserId = DemoLocalChatRepository.DEMO_UID,
            suggestedDisplayName = "Operator Zero",
        )
        var darkTheme by mutableStateOf(false)

        composeTestRule.setContent {
            val accountState by viewModel.accountState.collectAsStateWithLifecycle()
            val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
            val selectedChatId by viewModel.selectedChatId.collectAsStateWithLifecycle()
            val selectedChat by viewModel.selectedChat.collectAsStateWithLifecycle()
            val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
            val peerPresence by viewModel.activePeerPresence.collectAsStateWithLifecycle()
            val composerText by viewModel.composerText.collectAsStateWithLifecycle()
            val replyTarget by viewModel.replyTarget.collectAsStateWithLifecycle()
            val globalDecodeActive by viewModel.globalDecodeActive.collectAsStateWithLifecycle()
            val decodedIds by viewModel.decodedMessageIds.collectAsStateWithLifecycle()
            val useStandardKeyboard by viewModel.useStandardKeyboard.collectAsStateWithLifecycle()

            val account = (accountState as? com.example.ui.UiState.Success)?.data
            GlyphChatTheme(darkTheme = darkTheme) {
                if (account != null) {
                    val active = selectedChat
                    if (active == null) {
                        Sidebar(
                            account = account,
                            isDemoMode = true,
                            chatsState = chatsState,
                            selectedChatId = selectedChatId,
                            searchQuery = "",
                            isDarkTheme = darkTheme,
                            globalDecodeActive = globalDecodeActive,
                            onSearchQueryChange = {},
                            onSelectChat = viewModel::selectChat,
                            onNewDirectClick = {},
                            onNewGroupClick = {},
                            onToggleTheme = { darkTheme = !darkTheme },
                            onOpenCipherGuide = {},
                            onOpenProfile = {},
                        )
                    } else {
                        ChatWindow(
                            chatRoom = active,
                            currentUserId = account.userId,
                            peerPresence = peerPresence,
                            messagesState = messagesState,
                            composerText = composerText,
                            replyTarget = replyTarget,
                            globalDecodeActive = globalDecodeActive,
                            decodedMessageIds = decodedIds,
                            showKeyHints = true,
                            useStandardKeyboard = useStandardKeyboard,
                            showBackButton = true,
                            onBack = viewModel::clearSelection,
                            onToggleGlobalDecode = viewModel::toggleGlobalDecode,
                            onToggleMessageDecode = viewModel::toggleMessageDecode,
                            onOpenGroupInfo = {},
                            onReplyMessage = viewModel::setReplyTarget,
                            onClearReply = { viewModel.setReplyTarget(null) },
                            onDeleteMessage = viewModel::deleteMessage,
                            onAppendCipherText = viewModel::appendComposerText,
                            onSetComposerText = viewModel::setComposerText,
                            onBackspace = viewModel::backspaceComposer,
                            onSend = viewModel::sendMessage,
                            onToggleKeyHints = {},
                            onSwitchKeyboardMode = viewModel::setUseStandardKeyboard,
                            onMarkRead = viewModel::markActiveChatRead,
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify Direct Chat and Group Chat appear in Sidebar
        composeTestRule.onNodeWithTag("chat_row_dm_demo_elena").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_row_grp_swiss_grid").assertIsDisplayed()

        // Toggle Light -> Dark mode on Sidebar
        composeTestRule.onNodeWithTag("sidebar_toggle_theme_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, darkTheme)

        // 2. Open Group Chat and verify Group Info button + messages
        composeTestRule.onNodeWithTag("chat_row_grp_swiss_grid").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_group_info_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("message_bubble_g1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("message_bubble_g2").assertIsDisplayed()

        // 3. Toggle global Peek decode button in floating frosted-glass header
        composeTestRule.onNodeWithTag("header_peek_toggle_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("PROTOCOL V2 IS LIVE").assertIsDisplayed()

        // 4. Tap Cipher Keyboard glass keycaps ('H', 'I') and Send
        composeTestRule.onNodeWithTag("cipher_key_h").performClick()
        composeTestRule.onNodeWithTag("cipher_key_i").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("composer_english_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cipher_key_send").performClick()
        composeTestRule.waitForIdle()

        // 5. Collapse and re-expand Cipher Keyboard via floating composer pill toggle
        composeTestRule.onNodeWithTag("toggle_cipher_keyboard_expand").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("toggle_cipher_keyboard_expand").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("cipher_keyboard").assertIsDisplayed()

        // 6. Switch to System Keyboard, enter long 12-line multiline text, confirm fixed max height (no layout shift), and send
        composeTestRule.onNodeWithTag("switch_standard_keyboard_button").performClick()
        composeTestRule.waitForIdle()
        val multilinePayload = (1..12).joinToString("\n") { "LINE $it" }
        composeTestRule.onNodeWithTag("standard_message_input")
            .performTextInput(multilinePayload)
        composeTestRule.waitForIdle()
        val scrollContainerBounds = composeTestRule
            .onNodeWithTag("standard_message_scroll_container")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Expected scrollable input container height (${scrollContainerBounds.height}) to stay capped at <= 112.dp",
            scrollContainerBounds.height <= 112.5.dp
        )
        composeTestRule.onNodeWithTag("standard_send_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(multilinePayload).assertIsDisplayed()

        // 7. Switch back to Light mode inside active ChatWindow and verify clean render
        darkTheme = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_window").assertIsDisplayed()
    }
}
