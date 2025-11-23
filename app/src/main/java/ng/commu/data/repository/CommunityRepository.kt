package ng.commu.data.repository

import ng.commu.data.model.Community
import ng.commu.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getUserCommunities(): Result<List<Community>> {
        return try {
            val response = apiService.getUserCommunities()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.communities)
            } else {
                Result.failure(Exception("Failed to fetch communities: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
