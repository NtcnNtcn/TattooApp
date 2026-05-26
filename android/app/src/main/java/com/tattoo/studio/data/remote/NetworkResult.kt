package com.tattoo.studio.data.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Structured error detail returned by the FastAPI backend.
 * The backend uses {"detail": "..."} for most errors.
 */
data class ApiErrorBody(
    @SerializedName("detail") val detail: String? = null,
    @SerializedName("message") val message: String? = null,
)

/**
 * A domain-level error type that gives the UI enough context
 * to display localised, actionable messages.
 */
sealed class AppError(
    open val userMessage: String,
    override val cause: Throwable? = null
) : Throwable(userMessage, cause) {
    /** No internet / DNS failure / server unreachable */
    data class Network(
        override val userMessage: String = "Нет подключения к серверу. Проверьте интернет-соединение.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    /** Request reached the server but timed out */
    data class Timeout(
        override val userMessage: String = "Превышено время ожидания. Попробуйте позже.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    /** 401 — token expired or invalid */
    data class Unauthorized(
        override val userMessage: String = "Сессия истекла. Войдите снова.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    /** 403 — insufficient permissions */
    data class Forbidden(
        override val userMessage: String = "Недостаточно прав для этого действия.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    /** 403 with specific message — email not verified */
    data class Unverified(
        override val userMessage: String = "Email не подтвержден.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    /** 404 — resource not found */
    data class NotFound(
        override val userMessage: String = "Запрашиваемый ресурс не найден.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    /** 409 — conflict (e.g. duplicate email) */
    data class Conflict(
        override val userMessage: String = "Конфликт данных. Возможно, запись уже существует.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    /** 422 — validation error */
    data class Validation(
        override val userMessage: String = "Ошибка валидации. Проверьте введённые данные.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    /** 500+ server errors */
    data class Server(
        override val userMessage: String = "Ошибка сервера. Попробуйте позже.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    /** Anything else */
    data class Unknown(
        override val userMessage: String = "Произошла неизвестная ошибка.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)
}

/**
 * Maps a raw [Throwable] into a structured [AppError].
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is ConnectException,
        is UnknownHostException -> AppError.Network(cause = this)

        is SocketTimeoutException -> AppError.Timeout(cause = this)

        is IOException -> AppError.Network(
            userMessage = "Ошибка сети: ${this.localizedMessage ?: "неизвестная ошибка"}",
            cause = this
        )

        is HttpException -> {
            val serverMessage = parseErrorBody(this)
            when (code()) {
                401 -> AppError.Unauthorized(
                    userMessage = serverMessage ?: "Сессия истекла. Войдите снова.",
                    cause = this
                )
                403 -> {
                    if (serverMessage?.contains("verified", ignoreCase = true) == true) {
                        AppError.Unverified(userMessage = serverMessage, cause = this)
                    } else {
                        AppError.Forbidden(userMessage = serverMessage ?: "Недостаточно прав для этого действия.", cause = this)
                    }
                }
                404 -> AppError.NotFound(
                    userMessage = serverMessage ?: "Запрашиваемый ресурс не найден.",
                    cause = this
                )
                409 -> AppError.Conflict(
                    userMessage = serverMessage ?: "Конфликт данных.",
                    cause = this
                )
                422 -> AppError.Validation(
                    userMessage = serverMessage ?: "Ошибка валидации. Проверьте данные.",
                    cause = this
                )
                in 500..599 -> AppError.Server(
                    userMessage = serverMessage ?: "Ошибка сервера (${ code() }). Попробуйте позже.",
                    cause = this
                )
                else -> AppError.Unknown(
                    userMessage = serverMessage ?: "Ошибка запроса (${code()}).",
                    cause = this
                )
            }
        }

        else -> AppError.Unknown(
            userMessage = this.localizedMessage ?: "Произошла неизвестная ошибка.",
            cause = this
        )
    }
}

/**
 * Tries to extract a human-readable message from the HTTP error body.
 */
private fun parseErrorBody(exception: HttpException): String? {
    return try {
        val errorBody = exception.response()?.errorBody()?.string() ?: return null
        val parsed = Gson().fromJson(errorBody, ApiErrorBody::class.java)
        parsed.detail ?: parsed.message
    } catch (e: Exception) {
        null
    }
}

/**
 * Wraps a suspend API call into a [Result] with proper [AppError] mapping.
 */
suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
    return try {
        Result.success(call())
    } catch (e: Exception) {
        Result.failure(e.toAppError())
    }
}
