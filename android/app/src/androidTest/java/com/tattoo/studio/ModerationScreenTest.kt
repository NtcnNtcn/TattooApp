package com.tattoo.studio

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsEnabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tattoo.studio.data.remote.dto.TattooWorkOutDto
import com.tattoo.studio.data.remote.dto.TagDto
import com.tattoo.studio.presentation.moderation.PendingWorkCard
import com.tattoo.studio.presentation.theme.TattooStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModerationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPendingWorkCardElementsAreDisplayed() {
        val mockWork = TattooWorkOutDto(
            id = 1,
            imageUrl = "/uploads/works/test.jpg",
            description = "Test tattoo work",
            status = "pending",
            likeCount = 0,
            rejectionReason = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            tags = listOf(TagDto(1, "realism"), TagDto(2, "blackwork"))
        )

        var approveCalled = false
        var rejectCalled = false

        composeTestRule.setContent {
            TattooStudioTheme {
                PendingWorkCard(
                    work = mockWork,
                    onApprove = { approveCalled = true },
                    onReject = { rejectCalled = true }
                )
            }
        }

        // Verify tags are displayed
        composeTestRule.onNodeWithText("realism / blackwork").assertIsDisplayed()

        // Verify action buttons are displayed
        composeTestRule.onNodeWithText("ОДОБРИТЬ").assertIsDisplayed()
        composeTestRule.onNodeWithText("ОТКЛОНИТЬ").assertIsDisplayed()
    }

    @Test
    fun testApproveButtonClick() {
        val mockWork = TattooWorkOutDto(
            id = 1,
            imageUrl = "/uploads/works/test.jpg",
            description = "Test tattoo work",
            status = "pending",
            likeCount = 0,
            rejectionReason = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            tags = listOf(TagDto(1, "realism"))
        )

        var approveCalled = false

        composeTestRule.setContent {
            TattooStudioTheme {
                PendingWorkCard(
                    work = mockWork,
                    onApprove = { approveCalled = true },
                    onReject = {}
                )
            }
        }

        // Click approve button
        composeTestRule.onNodeWithText("ОДОБРИТЬ").performClick()

        // Verify callback was called
        assert(approveCalled)
    }

    @Test
    fun testRejectButtonClick() {
        val mockWork = TattooWorkOutDto(
            id = 1,
            imageUrl = "/uploads/works/test.jpg",
            description = "Test tattoo work",
            status = "pending",
            likeCount = 0,
            rejectionReason = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            tags = listOf(TagDto(1, "realism"))
        )

        var rejectCalled = false

        composeTestRule.setContent {
            TattooStudioTheme {
                PendingWorkCard(
                    work = mockWork,
                    onApprove = {},
                    onReject = { rejectCalled = true }
                )
            }
        }

        // Click reject button
        composeTestRule.onNodeWithText("ОТКЛОНИТЬ").performClick()

        // Verify callback was called
        assert(rejectCalled)
    }

    @Test
    fun testWorkWithLongDescription() {
        val mockWork = TattooWorkOutDto(
            id = 1,
            imageUrl = "/uploads/works/test.jpg",
            description = "This is a very long description that should be truncated in the display",
            status = "pending",
            likeCount = 0,
            rejectionReason = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            tags = listOf(TagDto(1, "realism"))
        )

        composeTestRule.setContent {
            TattooStudioTheme {
                PendingWorkCard(
                    work = mockWork,
                    onApprove = {},
                    onReject = {}
                )
            }
        }

        // Verify that the description is truncated (first 20 chars)
        composeTestRule.onNodeWithText("THIS IS A VERY LONG").assertIsDisplayed()
    }

    @Test
    fun testWorkWithMultipleTags() {
        val mockWork = TattooWorkOutDto(
            id = 1,
            imageUrl = "/uploads/works/test.jpg",
            description = "Test work",
            status = "pending",
            likeCount = 0,
            rejectionReason = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            tags = listOf(
                TagDto(1, "realism"),
                TagDto(2, "blackwork"),
                TagDto(3, "geometric"),
                TagDto(4, "dotwork")
            )
        )

        composeTestRule.setContent {
            TattooStudioTheme {
                PendingWorkCard(
                    work = mockWork,
                    onApprove = {},
                    onReject = {}
                )
            }
        }

        // Verify all tags are displayed
        composeTestRule.onNodeWithText("realism / blackwork / geometric / dotwork").assertIsDisplayed()
    }

    @Test
    fun testWorkWithNoDescription() {
        val mockWork = TattooWorkOutDto(
            id = 1,
            imageUrl = "/uploads/works/test.jpg",
            description = null,
            status = "pending",
            likeCount = 0,
            rejectionReason = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            tags = listOf(TagDto(1, "realism"))
        )

        composeTestRule.setContent {
            TattooStudioTheme {
                PendingWorkCard(
                    work = mockWork,
                    onApprove = {},
                    onReject = {}
                )
            }
        }

        // Verify "НЕИЗВЕСТЕН" is shown when description is null
        composeTestRule.onNodeWithText("МАСТЕР: НЕИЗВЕСТЕН").assertIsDisplayed()
    }
}
