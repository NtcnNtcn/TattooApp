package com.tattoo.studio.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class TokenPairDto(
    @SerializedName("access_token")  val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type")    val tokenType: String,
)

data class RefreshRequestDto(
    @SerializedName("refresh_token") val refreshToken: String,
)

data class FCMTokenDto(
    @SerializedName("fcm_token") val fcmToken: String,
)

data class VerifyCodeRequestDto(
    val email: String,
    val code: String,
    val type: String
)

data class ResendCodeRequestDto(
    val email: String,
    val type: String
)

data class ForgotPasswordRequestDto(
    val email: String
)

data class ResetPasswordRequestDto(
    val email: String,
    val code: String,
    @SerializedName("new_password") val newPassword: String
)

// ── Role & User ───────────────────────────────────────────────────────────────

data class RoleDto(
    val id: Int,
    val name: String,
    val level: Int,
)

data class UserStatsDto(
    @SerializedName("likes_given")     val likesGiven: Int,
    @SerializedName("likes_received")  val likesReceived: Int,
    @SerializedName("favorites_count") val favoritesCount: Int,
    @SerializedName("works_count")     val worksCount: Int,
    @SerializedName("following_count") val followingCount: Int,
    @SerializedName("followers_count") val followersCount: Int,
    @SerializedName("applications_count") val applicationsCount: Int,
    @SerializedName("favorites_received") val favoritesReceived: Int,
)

data class UserDto(
    val id: Int,
    val email: String,
    @SerializedName("full_name")  val fullName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("is_active")  val isActive: Boolean,
    @SerializedName("is_verified") val isVerified: Boolean,
    val status: String? = null,
    val description: String? = null,
    @SerializedName("created_at") val createdAt: String,
    val role: RoleDto,
    val stats: UserStatsDto? = null,
)

data class UserPublicDto(
    val id: Int,
    @SerializedName("full_name")  val fullName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val status: String? = null,
    val description: String? = null,
    val role: RoleDto,
)

// ── Tags & Works ──────────────────────────────────────────────────────────────

data class TagDto(
    val id: Int,
    val name: String,
)

data class TattooWorkFeedDto(
    val id: Int,
    @SerializedName("image_url")   val imageUrl: String,
    val status: String,
    @SerializedName("like_count")  val likeCount: Int,
    @SerializedName("created_at")  val createdAt: String,
    val tags: List<TagDto>,
    @SerializedName("is_liked")    val isLiked: Boolean = false,
    @SerializedName("is_favorited") val isFavorited: Boolean = false,
    @SerializedName("master_name")  val masterName: String = "МАСТЕР",
    @SerializedName("master_avatar") val masterAvatar: String? = null,
    @SerializedName("master_id")     val masterId: Int = 0
)

data class TattooWorkDetailDto(
    val id: Int,
    @SerializedName("image_url")         val imageUrl: String,
    val description: String?,
    val status: String,
    @SerializedName("like_count")        val likeCount: Int,
    @SerializedName("rejection_reason")  val rejectionReason: String?,
    @SerializedName("created_at")        val createdAt: String,
    @SerializedName("updated_at")        val updatedAt: String,
    @SerializedName("is_liked")          val isLiked: Boolean,
    @SerializedName("is_favorited")      val isFavorited: Boolean,
    val tags: List<TagDto>,
    val master: UserPublicDto,
)

data class TattooWorkOutDto(
    val id: Int,
    @SerializedName("image_url")         val imageUrl: String,
    val description: String?,
    val status: String,
    @SerializedName("like_count")        val likeCount: Int,
    @SerializedName("rejection_reason")  val rejectionReason: String?,
    @SerializedName("created_at")        val createdAt: String,
    @SerializedName("updated_at")        val updatedAt: String,
    val tags: List<TagDto>,
)

data class WorksPageDto(
    val items: List<TattooWorkFeedDto>,
    val total: Int,
    val page: Int,
    val size: Int,
    val pages: Int,
)

data class ModerationWorksPageDto(
    val items: List<TattooWorkOutDto>,
    val total: Int,
    val page: Int,
    val size: Int,
    val pages: Int,
)

// ── Social ────────────────────────────────────────────────────────────────────

data class ToggleResponseDto(
    val active: Boolean,
    val message: String,
)

data class SubscriptionDto(
    val id: Int,
    @SerializedName("master_id")   val masterId: Int,
    @SerializedName("created_at")  val createdAt: String,
)

// ── Moderation ────────────────────────────────────────────────────────────────

data class ReviewActionDto(
    val comment: String? = null,
)

data class WorkReviewDto(
    val id: Int,
    @SerializedName("work_id")      val workId: Int,
    @SerializedName("reviewer_id")  val reviewerId: Int?,
    @SerializedName("old_status")   val oldStatus: String,
    @SerializedName("new_status")   val newStatus: String,
    val comment: String?,
    @SerializedName("created_at")   val createdAt: String,
)

// ── Notifications ─────────────────────────────────────────────────────────────

data class NotificationDto(
    val id: Int,
    val type: String,
    val title: String,
    val message: String,
    @SerializedName("is_read")     val isRead: Boolean,
    @SerializedName("created_at")  val createdAt: String,
)

// ── Reports ───────────────────────────────────────────────────────────────────

data class TopMasterDto(
    @SerializedName("master_id")      val masterId: Int,
    @SerializedName("full_name")      val fullName: String,
    @SerializedName("total_likes")    val totalLikes: Int,
    @SerializedName("approved_works") val approvedWorks: Int,
)

data class ReportSummaryDto(
    @SerializedName("period_start")           val periodStart: String,
    @SerializedName("period_end")             val periodEnd: String,
    @SerializedName("total_works_uploaded")   val totalWorksUploaded: Int,
    @SerializedName("total_approved")         val totalApproved: Int,
    @SerializedName("total_rejected")         val totalRejected: Int,
    @SerializedName("total_pending")          val totalPending: Int,
    @SerializedName("total_likes")            val totalLikes: Int,
    @SerializedName("total_subscriptions")    val totalSubscriptions: Int,
    @SerializedName("new_users")              val newUsers: Int,
    @SerializedName("top_masters")            val topMasters: List<TopMasterDto>,
)

data class UsersPageDto(
    val items: List<UserDto>,
    val total: Int,
    val page: Int,
    val size: Int,
    val pages: Int,
)

// ── Admin ───────────────────────────────────────────────────────────────────

data class UserSimpleDto(
    val id: Int,
    val email: String,
    @SerializedName("full_name")  val fullName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
)

data class AdminStatsDto(
    @SerializedName("works_approved")          val worksApproved: Int,
    @SerializedName("works_rejected")          val worksRejected: Int,
    @SerializedName("consultations_processed") val consultationsProcessed: Int,
)

data class MasterApplicationDto(
    val id: Int,
    @SerializedName("master_id")   val masterId: Int,
    val status: String,
    @SerializedName("created_at")  val createdAt: String,
    val master: UserSimpleDto,
)

data class ConsultationApplicationDto(
    val id: Int,
    @SerializedName("client_id")   val clientId: Int,
    @SerializedName("master_id")   val masterId: Int,
    @SerializedName("contact_info") val contactInfo: String,
    val message: String?,
    val status: String,
    @SerializedName("created_at")  val createdAt: String,
    val client: UserSimpleDto,
    val master: UserSimpleDto,
)

// ── Applications ─────────────────────────────────────────────────────────────

data class ApplicationCreateDto(
    @SerializedName("master_id")    val masterId: Int,
    @SerializedName("contact_info") val contactInfo: String,
    val message: String?,
)

data class ApplicationOutDto(
    val id: Int,
    @SerializedName("client_id")    val clientId: Int,
    @SerializedName("master_id")    val masterId: Int,
    @SerializedName("contact_info") val contactInfo: String,
    val message: String?,
    val status: String,
    @SerializedName("created_at")   val createdAt: String,
)
