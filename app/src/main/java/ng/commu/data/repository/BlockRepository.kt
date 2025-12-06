package ng.commu.data.repository

import ng.commu.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getBlockedUsers() = apiService.getBlockedUsers()

    suspend fun blockUser(userId: String) = apiService.blockUser(userId)

    suspend fun unblockUser(userId: String) = apiService.unblockUser(userId)
}
