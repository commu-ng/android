package ng.commu.data.repository

import ng.commu.data.model.ApplicationReviewRequest
import ng.commu.data.model.ApplicationReviewResponse
import ng.commu.data.model.ApplyToCommunityRequest
import ng.commu.data.model.Community
import ng.commu.data.model.CommunityApplication
import ng.commu.data.model.CommunityApplicationDetail
import ng.commu.data.model.CommunityDetails
import ng.commu.data.model.CommunityLink
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

    suspend fun getRecruitingCommunities(): Result<List<Community>> {
        return try {
            val response = apiService.getRecruitingCommunities()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.communities)
            } else {
                Result.failure(Exception("Failed to fetch recruiting communities: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOngoingCommunities(): Result<List<Community>> {
        return try {
            val response = apiService.getOngoingCommunities()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.communities)
            } else {
                Result.failure(Exception("Failed to fetch ongoing communities: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommunityDetails(slug: String): Result<CommunityDetails> {
        return try {
            val response = apiService.getCommunityDetails(slug)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch community details: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommunityLinks(slug: String): Result<List<CommunityLink>> {
        return try {
            val response = apiService.getCommunityLinks(slug)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch community links: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyToCommunity(
        slug: String,
        profileName: String,
        profileUsername: String,
        message: String?
    ): Result<CommunityApplication> {
        return try {
            val request = ApplyToCommunityRequest(
                profileName = profileName,
                profileUsername = profileUsername,
                message = message
            )
            val response = apiService.applyToCommunity(slug, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to apply to community: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyApplications(slug: String): Result<List<CommunityApplication>> {
        return try {
            val response = apiService.getMyApplications(slug)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch applications: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommunityApplications(slug: String): Result<List<CommunityApplicationDetail>> {
        return try {
            val response = apiService.getCommunityApplications(slug)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch applications: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveApplication(slug: String, applicationId: String): Result<ApplicationReviewResponse> {
        return try {
            val request = ApplicationReviewRequest(status = "approved")
            val response = apiService.reviewApplication(slug, applicationId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to approve application: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectApplication(slug: String, applicationId: String, reason: String?): Result<ApplicationReviewResponse> {
        return try {
            val request = ApplicationReviewRequest(status = "rejected", rejectionReason = reason)
            val response = apiService.reviewApplication(slug, applicationId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to reject application: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
