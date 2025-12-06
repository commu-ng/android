package ng.commu.data.model

import com.squareup.moshi.Json

data class BlockedUser(
    val id: String,
    @Json(name = "login_name")
    val loginName: String,
    @Json(name = "blocked_at")
    val blockedAt: String
)

data class BlockedUsersResponse(
    val data: List<BlockedUser>
)

data class BlockResponse(
    val data: BlockResponseData
)

data class BlockResponseData(
    val blocked: Boolean? = null,
    val unblocked: Boolean? = null
)
