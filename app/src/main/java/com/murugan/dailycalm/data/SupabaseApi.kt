package com.murugan.dailycalm.data

import android.util.Log
import com.murugan.dailycalm.BuildConfig
import com.murugan.dailycalm.DailyContent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object SupabaseApi {

    private const val TAG = "SupabaseApi"

    /** A day counts as published only once its audio has been generated. */
    private const val PUBLISHED = "not.is.null"

    private interface SupabaseService {
        @GET("rest/v1/daily_content")
        suspend fun getDailyContent(
            @Query("day") day: String,
            @Query("audio_url") audioUrl: String = PUBLISHED,
            @Query("select") select: String = "title,body,audio_url"
        ): List<DailyContent>

        @GET("rest/v1/daily_content")
        suspend fun getLatestPublishedDay(
            @Query("audio_url") audioUrl: String = PUBLISHED,
            @Query("select") select: String = "day",
            @Query("order") order: String = "day.desc",
            @Query("limit") limit: Int = 1
        ): List<PublishedDay>
    }

    internal data class PublishedDay(val day: Int)

    private val service: SupabaseService? by lazy {
        val baseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) {
            Log.e(TAG, "Missing SUPABASE_URL or SUPABASE_ANON_KEY in local.properties")
            null
        } else {
            createService(baseUrl, anonKey)
        }
    }

    private fun createService(baseUrl: String, anonKey: String): SupabaseService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(SupabaseService::class.java)
    }

    suspend fun getDailyContent(day: Int): Result<DailyContent> {
        val api = service ?: return Result.failure(IllegalStateException("App is not configured"))

        return try {
            val content = api.getDailyContent(day = "eq.$day").firstOrNull()
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(ContentNotFoundException(day))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network Error", e)
            Result.failure(e)
        }
    }

    suspend fun getLatestPublishedDay(): Result<Int> {
        val api = service ?: return Result.failure(IllegalStateException("App is not configured"))

        return try {
            val latest = api.getLatestPublishedDay().firstOrNull()?.day
            if (latest != null) {
                Result.success(latest)
            } else {
                Result.failure(ContentNotFoundException(day = 0))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network Error", e)
            Result.failure(e)
        }
    }
}
