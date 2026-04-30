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

    private interface SupabaseService {
        @GET("rest/v1/daily_content")
        suspend fun getDailyContent(
            @Query("day") day: String,
            @Query("select") select: String = "title,body,audio_url"
        ): List<DailyContent>
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

    suspend fun getDailyContent(day: Int): DailyContent? {
        val baseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (baseUrl.isBlank() || anonKey.isBlank()) {
            Log.e("SupabaseApi", "Missing SUPABASE_URL or SUPABASE_ANON_KEY in local.properties")
            return null
        }

        return try {
            val service = createService(baseUrl, anonKey)
            service.getDailyContent(day = "eq.$day").firstOrNull()
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Network Error", e)
            null
        }
    }
}
