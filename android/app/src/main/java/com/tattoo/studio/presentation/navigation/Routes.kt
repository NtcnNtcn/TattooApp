package com.tattoo.studio.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object LoginRoute

@Serializable
object RegisterRoute

@Serializable
object MainRoute

@Serializable
object UploadWorkRoute

@Serializable
object ModerationRoute

@Serializable
object ReportsRoute

@Serializable
object ClientApplicationsRoute

@Serializable
object MastersListRoute

@Serializable
object EditProfileRoute

@Serializable
data class VerificationRoute(val email: String, val type: String)

@Serializable
object ForgotPasswordRoute

@Serializable
data class ResetPasswordRoute(val email: String)

@Serializable
data class MasterDetailRoute(val id: Int)

@Serializable
data class WorkDetailRoute(val id: Int)
