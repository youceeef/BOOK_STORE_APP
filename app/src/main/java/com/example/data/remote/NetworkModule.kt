package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.SessionStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val GUEST_ROUTE_HEADER = "X-Guest-Route"

    fun createMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun createOkHttpClient(sessionStore: SessionStore): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HeadersAndAuthInterceptor(sessionStore))

        if (BuildConfig.DEBUG) {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
                redactHeader("Authorization")
            }
            builder.addInterceptor(logger)
        }
        return builder.build()
    }

    fun createApiService(moshi: Moshi, okHttpClient: OkHttpClient): BookStoreApiService {
        return Retrofit.Builder()
            .baseUrl(SessionStore.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BookStoreApiService::class.java)
    }

    /**
     * Applies global headers mandated by Section 1 & Section 10 of the API Spec:
     * - Accept: application/json (preserved as application/pdf for invoice downloads)
     * - Accept-Language: <en|fr|ar>
     * - Authorization: Bearer <token> (omitted on guest-only auth endpoints)
     * Note: Content-Type is left to Retrofit/OkHttp so multipart/form-data boundaries are intact.
     */
    private class HeadersAndAuthInterceptor(
        private val sessionStore: SessionStore
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val isGuestRoute = original.header(GUEST_ROUTE_HEADER) == "true"
            val builder = original.newBuilder()
                .removeHeader(GUEST_ROUTE_HEADER)

            if (original.header("Accept").isNullOrBlank()) {
                builder.header("Accept", "application/json")
            }

            builder.header("Accept-Language", sessionStore.locale.value)

            val token = sessionStore.token.value
            if (!isGuestRoute && !token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }

            return chain.proceed(builder.build())
        }
    }
}
