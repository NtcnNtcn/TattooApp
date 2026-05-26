package com.tattoo.studio

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tattoo.studio.presentation.auth.LoginScreen
import com.tattoo.studio.presentation.theme.TattooStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLoginScreenElementsAreDisplayed() {
        var navigateToRegisterCalled = false
        var navigateToForgotPasswordCalled = false
        var navigateToVerificationCalled: String? = null
        var loginSuccessCalled = false

        composeTestRule.setContent {
            TattooStudioTheme {
                LoginScreen(
                    onNavigateToRegister = { navigateToRegisterCalled = true },
                    onNavigateToForgotPassword = { navigateToForgotPasswordCalled = true },
                    onNavigateToVerification = { navigateToVerificationCalled = it },
                    onLoginSuccess = { loginSuccessCalled = true }
                )
            }
        }

        // Verify title elements are displayed
        composeTestRule.onNodeWithText("MAGNUM").assertIsDisplayed()
        composeTestRule.onNodeWithText("STUDIO").assertIsDisplayed()

        // Verify input fields are displayed
        composeTestRule.onNodeWithText("EMAIL").assertIsDisplayed()
        composeTestRule.onNodeWithText("ПАРОЛЬ").assertIsDisplayed()

        // Verify buttons are displayed
        composeTestRule.onNodeWithText("ВОЙТИ").assertIsDisplayed()
        composeTestRule.onNodeWithText("РЕГИСТРАЦИЯ").assertIsDisplayed()
        composeTestRule.onNodeWithText("ЗАБЫЛИ ПАРОЛЬ?").assertIsDisplayed()
    }

    @Test
    fun testEmailInput() {
        composeTestRule.setContent {
            TattooStudioTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {},
                    onNavigateToVerification = {},
                    onLoginSuccess = {}
                )
            }
        }

        // Find email field and input text
        composeTestRule.onNodeWithText("EMAIL")
            .performTextInput("test@example.com")

        // Verify the text was entered
        composeTestRule.onNode(hasText("test@example.com")).assertIsDisplayed()
    }

    @Test
    fun testPasswordInput() {
        composeTestRule.setContent {
            TattooStudioTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {},
                    onNavigateToVerification = {},
                    onLoginSuccess = {}
                )
            }
        }

        // Find password field and input text
        composeTestRule.onNodeWithText("ПАРОЛЬ")
            .performTextInput("password123")

        // Verify the text was entered (password field should not show the actual text)
        // But we can verify the field exists and is interactable
        composeTestRule.onNodeWithText("ПАРОЛЬ").assertIsDisplayed()
    }

    @Test
    fun testNavigateToRegister() {
        var navigateToRegisterCalled = false

        composeTestRule.setContent {
            TattooStudioTheme {
                LoginScreen(
                    onNavigateToRegister = { navigateToRegisterCalled = true },
                    onNavigateToForgotPassword = {},
                    onNavigateToVerification = {},
                    onLoginSuccess = {}
                )
            }
        }

        // Click on registration button
        composeTestRule.onNodeWithText("РЕГИСТРАЦИЯ").performClick()

        // Verify navigation callback was called
        assert(navigateToRegisterCalled)
    }

    @Test
    fun testNavigateToForgotPassword() {
        var navigateToForgotPasswordCalled = false

        composeTestRule.setContent {
            TattooStudioTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = { navigateToForgotPasswordCalled = true },
                    onNavigateToVerification = {},
                    onLoginSuccess = {}
                )
            }
        }

        // Click on forgot password button
        composeTestRule.onNodeWithText("ЗАБЫЛИ ПАРОЛЬ?").performClick()

        // Verify navigation callback was called
        assert(navigateToForgotPasswordCalled)
    }
}
