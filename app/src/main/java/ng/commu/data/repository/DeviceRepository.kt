package ng.commu.data.repository

import android.content.Context
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import ng.commu.BuildConfig
import ng.commu.data.local.SessionManager
import ng.commu.data.model.RegisterDeviceRequest
import ng.commu.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) {
    suspend fun registerDevice(pushToken: String? = null): Result<Unit> {
        return try {
            // If no token provided, get it from Firebase
            val token = pushToken ?: FirebaseMessaging.getInstance().token.await()

            val request = RegisterDeviceRequest(
                pushToken = token,
                platform = "android",
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                appVersion = BuildConfig.VERSION_NAME
            )

            val response = apiService.registerDevice(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to register device"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDevice(pushToken: String): Result<Unit> {
        return try {
            val response = apiService.deleteDevice(pushToken)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to delete device"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentPushToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            null
        }
    }
}
