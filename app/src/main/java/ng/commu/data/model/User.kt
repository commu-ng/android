package ng.commu.data.model

import com.squareup.moshi.Json

data class User(
    val id: String,
    @Json(name = "login_name") val loginName: String,
    val email: String?,
    @Json(name = "email_verified_at") val emailVerifiedAt: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "is_admin") val isAdmin: Boolean,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

data class LoginRequest(
    @Json(name = "login_name") val loginName: String,
    val password: String
)

data class LoginResponseData(
    val id: String,
    @Json(name = "login_name") val loginName: String,
    val email: String?,
    @Json(name = "email_verified_at") val emailVerifiedAt: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "is_admin") val isAdmin: Boolean,
    @Json(name = "session_token") val sessionToken: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

data class LoginResponse(
    val data: LoginResponseData
)

data class ChangePasswordRequest(
    @Json(name = "current_password") val currentPassword: String,
    @Json(name = "new_password") val newPassword: String
)

data class ChangeEmailRequest(
    @Json(name = "new_email") val newEmail: String,
    val password: String
)
