package com.murugan.dailycalm.data.info

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Read-only client for the public informationneeds.com API.
 *
 * Unlike [com.murugan.dailycalm.data.SupabaseApi] this needs no key — every endpoint used here is
 * public and unauthenticated. It is served over HTTPS, so no cleartext-traffic exception is needed.
 *
 * Supabase stays the source of truth for the daily practice content, where audio is attached and
 * offline playback matters. This API supplies the live reference data — festivals, temples,
 * panchangam — which changes daily and carries no audio.
 */
object InfoNeedsApi : InfoNeedsDataSource {

    private const val TAG = "InfoNeedsApi"

    const val BASE_URL = "https://api.informationneeds.com/api/"

    /** The portal, for "read more" links that earn AdSense on the web side. */
    const val PORTAL_URL = "https://informationneeds.com"

    /** Only 30 temples exist in total, so one page fetches everything. */
    private const val ALL_TEMPLES_PAGE_SIZE = 50

    private interface InfoNeedsService {

        @GET("spiritual/festivals/masters")
        suspend fun getFestivalMasters(): ApiResponse<List<FestivalMaster>>

        @GET("spiritual/festivals/upcoming")
        suspend fun getUpcomingFestivals(
            @Query("count") count: Int = 10
        ): ApiResponse<List<FestivalOccurrence>>

        @GET("spiritual/festivals/year/{year}")
        suspend fun getFestivalsByYear(
            @Path("year") year: Int
        ): ApiResponse<List<FestivalOccurrence>>

        @GET("spiritual/festivals/month/{month}/{year}")
        suspend fun getFestivalsByMonth(
            @Path("month") month: Int,
            @Path("year") year: Int
        ): ApiResponse<List<FestivalOccurrence>>

        @GET("spiritual/festivals/{slug}")
        suspend fun getFestival(
            @Path("slug") slug: String
        ): ApiResponse<FestivalDetail>

        @GET("temples")
        suspend fun getTemples(
            @Query("page") page: Int = 1,
            @Query("pageSize") pageSize: Int = ALL_TEMPLES_PAGE_SIZE,
            @Query("city") city: String? = null,
            @Query("deity") deity: String? = null
        ): ApiResponse<PaginatedResponse<Temple>>

        @GET("temples/{slug}")
        suspend fun getTemple(
            @Path("slug") slug: String
        ): ApiResponse<Temple>
    }

    private val service: InfoNeedsService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(InfoNeedsService::class.java)
    }

    /** Unwraps the `{success, message, data}` envelope, failing when the server reports an error. */
    private inline fun <T> call(label: String, block: () -> ApiResponse<T>): Result<T> = try {
        val response = block()
        val payload = response.data
        if (response.success && payload != null) {
            Result.success(payload)
        } else {
            val message = response.message ?: "Request failed"
            Log.e(TAG, "$label: $message")
            Result.failure(InfoNeedsException(message))
        }
    } catch (e: Exception) {
        Log.e(TAG, "$label: network error", e)
        Result.failure(e)
    }

    override suspend fun getFestivalMasters(): Result<List<FestivalMaster>> =
        call("getFestivalMasters") { service.getFestivalMasters() }

    override suspend fun getUpcomingFestivals(count: Int): Result<List<FestivalOccurrence>> =
        call("getUpcomingFestivals") { service.getUpcomingFestivals(count) }

    override suspend fun getFestivalsByYear(year: Int): Result<List<FestivalOccurrence>> =
        call("getFestivalsByYear") { service.getFestivalsByYear(year) }

    override suspend fun getFestivalsByMonth(month: Int, year: Int): Result<List<FestivalOccurrence>> =
        call("getFestivalsByMonth") { service.getFestivalsByMonth(month, year) }

    override suspend fun getFestival(slug: String): Result<FestivalDetail> =
        call("getFestival($slug)") { service.getFestival(slug) }

    /**
     * Every temple in one call.
     *
     * The API exposes only `city` and `deity` filters, and `deity` is unusable for finding Murugan
     * temples: the six Arupadai Veedu carry deity names like "Lord Dhandayuthapani" and
     * "Lord Senthilnathan", so `deity=Murugan` matches only two of them. Filtering happens
     * client-side on `templeType` instead — see [TempleRepository].
     */
    override suspend fun getAllTemples(): Result<List<Temple>> =
        call("getAllTemples") { service.getTemples() }.map { it.items.orEmpty() }

    override suspend fun getTemple(slug: String): Result<Temple> =
        call("getTemple($slug)") { service.getTemple(slug) }
}

class InfoNeedsException(message: String) : Exception(message)
