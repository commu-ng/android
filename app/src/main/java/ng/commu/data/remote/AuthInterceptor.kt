package ng.commu.data.remote

import android.util.Log
import ng.commu.data.local.CommunityContextManager
import ng.commu.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val communityContextManager: CommunityContextManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = sessionManager.getSessionToken()
        val requestBuilder = originalRequest.newBuilder()

        // Add Authorization header if token exists
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        // Add Origin header for app endpoints (community context)
        val path = originalRequest.url.encodedPath
        if (path.startsWith("/app/")) {
            val origin = communityContextManager.buildOriginHeader()
            if (origin != null) {
                requestBuilder.header("Origin", origin)
                Log.d("AuthInterceptor", "Added Origin header: $origin for $path")
            } else {
                Log.w("AuthInterceptor", "No community context set for app endpoint: $path")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
