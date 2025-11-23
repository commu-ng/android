package ng.commu.data.model

import com.squareup.moshi.Json

data class RegisterDeviceRequest(
    @Json(name = "push_token") val pushToken: String,
    val platform: String = "android",
    @Json(name = "device_model") val deviceModel: String? = null,
    @Json(name = "os_version") val osVersion: String? = null,
    @Json(name = "app_version") val appVersion: String? = null
)

data class DeviceResponseData(
    @Json(name = "push_token") val pushToken: String,
    val registered: Boolean,
    @Json(name = "registered_at") val registeredAt: String
)

data class DeviceResponse(
    val data: DeviceResponseData
)
