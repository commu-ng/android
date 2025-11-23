package ng.commu.data.repository

import ng.commu.data.model.ChangeEmailRequest
import ng.commu.data.model.ChangePasswordRequest
import ng.commu.data.model.User
import ng.commu.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getCurrentUser() = apiService.getCurrentUser()

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ) = apiService.changePassword(
        ChangePasswordRequest(
            currentPassword = currentPassword,
            newPassword = newPassword
        )
    )

    suspend fun changeEmail(newEmail: String, password: String) = apiService.changeEmail(
        ChangeEmailRequest(newEmail = newEmail, password = password)
    )

    suspend fun requestDataExport() = apiService.requestDataExport()

    suspend fun deleteAccount() = apiService.deleteAccount()
}
