package com.tattoo.studio

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tattoo.studio.data.remote.dto.UserDto
import com.tattoo.studio.data.remote.dto.RoleDto
import com.tattoo.studio.presentation.masters.MasterItem
import com.tattoo.studio.presentation.masters.StatusBadge
import com.tattoo.studio.presentation.theme.TattooStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MastersListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMasterItemElementsAreDisplayed() {
        val mockMaster = UserDto(
            id = 1,
            email = "john@example.com",
            fullName = "John Doe",
            avatarUrl = "/uploads/avatars/avatar1.jpg",
            isActive = true,
            isVerified = true,
            status = "active",
            createdAt = "2024-01-01T00:00:00Z",
            role = RoleDto(1, "master", 2)
        )

        var onClickCalled = false

        composeTestRule.setContent {
            TattooStudioTheme {
                MasterItem(
                    master = mockMaster,
                    onClick = { onClickCalled = true }
                )
            }
        }

        // Verify master name is displayed
        composeTestRule.onNodeWithText("JOHN DOE").assertIsDisplayed()

        // Verify email is displayed
        composeTestRule.onNodeWithText("john@example.com").assertIsDisplayed()

        // Verify status badge is displayed
        composeTestRule.onNodeWithText("АКТИВЕН").assertIsDisplayed()

        // Verify ID badge is displayed
        composeTestRule.onNodeWithText("ID: 1").assertIsDisplayed()
    }

    @Test
    fun testMasterItemClick() {
        val mockMaster = UserDto(
            id = 1,
            email = "john@example.com",
            fullName = "John Doe",
            avatarUrl = null,
            isActive = true,
            isVerified = true,
            status = "active",
            createdAt = "2024-01-01T00:00:00Z",
            role = RoleDto(1, "master", 2)
        )

        var onClickCalled = false

        composeTestRule.setContent {
            TattooStudioTheme {
                MasterItem(
                    master = mockMaster,
                    onClick = { onClickCalled = true }
                )
            }
        }

        // Click on the master item
        composeTestRule.onNodeWithText("JOHN DOE").performClick()

        // Verify callback was called
        assert(onClickCalled)
    }

    @Test
    fun testMasterWithNoAvatar() {
        val mockMaster = UserDto(
            id = 1,
            email = "jane@example.com",
            fullName = "Jane Smith",
            avatarUrl = null,
            isActive = true,
            isVerified = true,
            status = "active",
            createdAt = "2024-01-01T00:00:00Z",
            role = RoleDto(1, "master", 2)
        )

        composeTestRule.setContent {
            TattooStudioTheme {
                MasterItem(
                    master = mockMaster,
                    onClick = {}
                )
            }
        }

        // Verify master info is displayed even without avatar
        composeTestRule.onNodeWithText("JANE SMITH").assertIsDisplayed()
        composeTestRule.onNodeWithText("jane@example.com").assertIsDisplayed()
    }

    @Test
    fun testActiveStatusBadge() {
        composeTestRule.setContent {
            TattooStudioTheme {
                StatusBadge(status = "active", isActive = true)
            }
        }

        // Verify active status is displayed
        composeTestRule.onNodeWithText("АКТИВЕН").assertIsDisplayed()
    }

    @Test
    fun testFrozenStatusBadge() {
        composeTestRule.setContent {
            TattooStudioTheme {
                StatusBadge(status = "frozen", isActive = true)
            }
        }

        // Verify frozen status is displayed
        composeTestRule.onNodeWithText("ЗАМОРОЖЕН").assertIsDisplayed()
    }

    @Test
    fun testPendingStatusBadge() {
        composeTestRule.setContent {
            TattooStudioTheme {
                StatusBadge(status = "active", isActive = false)
            }
        }

        // Verify pending status is displayed
        composeTestRule.onNodeWithText("ОЖИДАЕТ").assertIsDisplayed()
    }

    @Test
    fun testPendingDeletionStatusBadge() {
        composeTestRule.setContent {
            TattooStudioTheme {
                StatusBadge(status = "pending_deletion", isActive = true)
            }
        }

        // Verify pending deletion status is displayed
        composeTestRule.onNodeWithText("УДАЛЕНИЕ").assertIsDisplayed()
    }

    @Test
    fun testUnknownStatusBadge() {
        composeTestRule.setContent {
            TattooStudioTheme {
                StatusBadge(status = "unknown", isActive = true)
            }
        }

        // Verify unknown status is displayed
        composeTestRule.onNodeWithText("НЕИЗВЕСТНО").assertIsDisplayed()
    }

    @Test
    fun testMasterWithFrozenStatus() {
        val mockMaster = UserDto(
            id = 2,
            email = "bob@example.com",
            fullName = "Bob Johnson",
            avatarUrl = null,
            isActive = true,
            isVerified = true,
            status = "frozen",
            createdAt = "2024-01-01T00:00:00Z",
            role = RoleDto(1, "master", 2)
        )

        composeTestRule.setContent {
            TattooStudioTheme {
                MasterItem(
                    master = mockMaster,
                    onClick = {}
                )
            }
        }

        // Verify frozen status is displayed
        composeTestRule.onNodeWithText("BOB JOHNSON").assertIsDisplayed()
        composeTestRule.onNodeWithText("ЗАМОРОЖЕН").assertIsDisplayed()
    }

    @Test
    fun testMasterWithPendingStatus() {
        val mockMaster = UserDto(
            id = 3,
            email = "alice@example.com",
            fullName = "Alice Williams",
            avatarUrl = null,
            isActive = false,
            isVerified = false,
            status = "active",
            createdAt = "2024-01-01T00:00:00Z",
            role = RoleDto(1, "client", 1)
        )

        composeTestRule.setContent {
            TattooStudioTheme {
                MasterItem(
                    master = mockMaster,
                    onClick = {}
                )
            }
        }

        // Verify pending status is displayed
        composeTestRule.onNodeWithText("ALICE WILLIAMS").assertIsDisplayed()
        composeTestRule.onNodeWithText("ОЖИДАЕТ").assertIsDisplayed()
    }

    @Test
    fun testMasterWithEmailDisplay() {
        val mockMaster = UserDto(
            id = 1,
            email = "test.master@studio.com",
            fullName = "Test Master",
            avatarUrl = null,
            isActive = true,
            isVerified = true,
            status = "active",
            createdAt = "2024-01-01T00:00:00Z",
            role = RoleDto(1, "master", 2)
        )

        composeTestRule.setContent {
            TattooStudioTheme {
                MasterItem(
                    master = mockMaster,
                    onClick = {}
                )
            }
        }

        // Verify email is displayed correctly
        composeTestRule.onNodeWithText("test.master@studio.com").assertIsDisplayed()
    }

    @Test
    fun testMasterIdDisplay() {
        val mockMaster = UserDto(
            id = 42,
            email = "test@example.com",
            fullName = "Test Master",
            avatarUrl = null,
            isActive = true,
            isVerified = true,
            status = "active",
            createdAt = "2024-01-01T00:00:00Z",
            role = RoleDto(1, "master", 2)
        )

        composeTestRule.setContent {
            TattooStudioTheme {
                MasterItem(
                    master = mockMaster,
                    onClick = {}
                )
            }
        }

        // Verify ID is displayed correctly
        composeTestRule.onNodeWithText("ID: 42").assertIsDisplayed()
    }

    @Test
    fun testMasterNameUppercase() {
        val mockMaster = UserDto(
            id = 1,
            email = "john@example.com",
            fullName = "john doe",
            avatarUrl = null,
            isActive = true,
            isVerified = true,
            status = "active",
            createdAt = "2024-01-01T00:00:00Z",
            role = RoleDto(1, "master", 2)
        )

        composeTestRule.setContent {
            TattooStudioTheme {
                MasterItem(
                    master = mockMaster,
                    onClick = {}
                )
            }
        }

        // Verify name is displayed in uppercase
        composeTestRule.onNodeWithText("JOHN DOE").assertIsDisplayed()
    }
}
