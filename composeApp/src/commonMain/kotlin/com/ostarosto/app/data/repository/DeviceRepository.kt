package com.ostarosto.app.data.repository

import com.ostarosto.app.core.network.ApiClient
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.data.dto.RegisterDeviceBody
import com.ostarosto.app.data.dto.TokenBody

class DeviceRepository(private val api: ApiClient) {

    suspend fun register(token: String, platform: String, appVersion: String?): ApiResult<Unit> =
        api.postUnit("devices", RegisterDeviceBody(token, platform, appVersion))

    suspend fun unregister(token: String): ApiResult<Unit> =
        api.deleteUnit("devices", TokenBody(token))
}
