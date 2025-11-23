package ng.commu.data.repository

import ng.commu.data.local.SessionManager
import ng.commu.data.model.LoginRequest
import ng.commu.data.model.User
import ng.commu.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    suspend fun login(loginName: String, password: String): Result<User> {
        return try {
            val response = apiService.login(LoginRequest(loginName, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!.data
                sessionManager.saveSessionToken(loginResponse.sessionToken)

                val user = User(
                    id = loginResponse.id,
                    loginName = loginResponse.loginName,
                    email = loginResponse.email,
                    emailVerifiedAt = loginResponse.emailVerifiedAt,
                    createdAt = loginResponse.createdAt,
                    isAdmin = loginResponse.isAdmin,
                    avatarUrl = loginResponse.avatarUrl
                )
                Result.success(user)
            } else {
                Result.failure(Exception(response.message() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(loginName: String, password: String): Result<User> {
        return try {
            val response = apiService.signup(LoginRequest(loginName, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!.data
                sessionManager.saveSessionToken(loginResponse.sessionToken)

                val user = User(
                    id = loginResponse.id,
                    loginName = loginResponse.loginName,
                    email = loginResponse.email,
                    emailVerifiedAt = loginResponse.emailVerifiedAt,
                    createdAt = loginResponse.createdAt,
                    isAdmin = loginResponse.isAdmin,
                    avatarUrl = loginResponse.avatarUrl
                )
                Result.success(user)
            } else {
                Result.failure(Exception(response.message() ?: "Signup failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<User> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to get user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            apiService.logout()
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.success(Unit)
        }
    }

    fun hasStoredSession(): Boolean {
        return sessionManager.hasSession()
    }
}
