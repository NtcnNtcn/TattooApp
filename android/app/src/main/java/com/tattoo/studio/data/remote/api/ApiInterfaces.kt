package com.tattoo.studio.data.remote.api

import com.tattoo.studio.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body body: Map<String, String>): UserDto

    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String,
    ): TokenPairDto

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenPairDto

    @POST("api/auth/fcm-token")
    suspend fun updateFcmToken(@Body body: FCMTokenDto)

    @POST("api/auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyCodeRequestDto): Response<TokenPairDto?>

    @POST("api/auth/resend-code")
    suspend fun resendCode(@Body body: ResendCodeRequestDto)

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto)

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto)
}

interface WorksApi {
    @GET("api/works")
    suspend fun listWorks(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("tags") tags: String? = null,
        @Query("sort") sort: String = "date",
        @Query("master_id") masterId: Int? = null,
    ): WorksPageDto

    @GET("api/works/{id}")
    suspend fun getWork(@Path("id") id: Int): TattooWorkDetailDto

    @Multipart
    @POST("api/works")
    suspend fun uploadWork(
        @Part image: MultipartBody.Part,
        @Part("description") description: RequestBody?,
        @Part("tags") tags: RequestBody,
    ): TattooWorkOutDto

    @DELETE("api/works/{id}")
    suspend fun deleteWork(@Path("id") id: Int)
}

interface TagsApi {
    @GET("api/tags")
    suspend fun listTags(): List<TagDto>

    @GET("api/tags/search")
    suspend fun searchTags(@Query("q") query: String): List<TagDto>
}

interface SocialApi {
    @POST("api/works/{id}/like")
    suspend fun toggleLike(@Path("id") workId: Int): ToggleResponseDto

    @POST("api/works/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") workId: Int): ToggleResponseDto

    @GET("api/likes")
    suspend fun listLikes(): List<TattooWorkFeedDto>

    @GET("api/favorites")
    suspend fun listFavorites(): List<TattooWorkFeedDto>

    @POST("api/users/{id}/subscribe")
    suspend fun toggleSubscription(@Path("id") masterId: Int): ToggleResponseDto

    @GET("api/subscriptions")
    suspend fun listSubscriptions(): List<SubscriptionDto>
}

interface ModerationApi {
    @GET("api/moderation/pending")
    suspend fun listPending(): ModerationWorksPageDto

    @POST("api/moderation/{id}/approve")
    suspend fun approveWork(
        @Path("id") workId: Int,
        @Body body: ReviewActionDto,
    ): WorkReviewDto

    @POST("api/moderation/{id}/reject")
    suspend fun rejectWork(
        @Path("id") workId: Int,
        @Body body: ReviewActionDto,
    ): WorkReviewDto
}

interface ReportsApi {
    @GET("api/reports/summary")
    suspend fun getSummary(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
    ): ReportSummaryDto

    @GET("api/reports/export")
    @Streaming
    suspend fun exportReport(
        @Query("format") format: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
    ): okhttp3.ResponseBody
}

interface UsersApi {
    @GET("api/users/me")
    suspend fun getMe(): UserDto

    @GET("api/users")
    suspend fun listUsers(@Query("role") role: String? = null): UsersPageDto

    @PATCH("api/users/{userId}/status")
    suspend fun updateUserStatus(
        @Path("userId") userId: Int,
        @Query("status_val") status: String
    ): UserDto

    @PATCH("api/users/me")
    suspend fun updateMe(@Body body: Map<String, String?>): UserDto

    @Multipart
    @POST("api/users/me/avatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part
    ): UserDto

    @GET("api/users/me/notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @POST("api/users/me/notifications/read-all")
    suspend fun markAllRead()

    @POST("api/users/me/verify-email-change")
    suspend fun verifyEmailChange(@Body body: VerifyCodeRequestDto)

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: Int): UserDto

    @DELETE("api/users/me")
    suspend fun deleteMe()
}

interface ApplicationsApi {
    @POST("api/applications")
    suspend fun createApplication(@Body body: ApplicationCreateDto): ApplicationOutDto

    @GET("api/applications/my")
    suspend fun listMyApplications(): List<ApplicationOutDto>
}

interface AdminApi {
    @GET("api/admin/applications/stats")
    suspend fun getStats(): AdminStatsDto

    @GET("api/admin/applications/masters")
    suspend fun listMasterApplications(): List<MasterApplicationDto>

    @POST("api/admin/applications/masters/{id}/approve")
    suspend fun approveMasterApplication(@Path("id") appId: Int)

    @POST("api/admin/applications/masters/{id}/reject")
    suspend fun rejectMasterApplication(@Path("id") appId: Int)

    @GET("api/admin/applications/consultations")
    suspend fun listConsultationApplications(): List<ConsultationApplicationDto>

    @PATCH("api/admin/applications/consultations/{id}/close")
    suspend fun closeConsultationApplication(@Path("id") appId: Int)
}
