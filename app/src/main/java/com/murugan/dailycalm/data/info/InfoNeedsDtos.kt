package com.murugan.dailycalm.data.info

import com.google.gson.annotations.SerializedName

/**
 * Every informationneeds.com endpoint wraps its payload in this envelope.
 * The DTO is never at the top level of the response.
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
)

data class PaginatedResponse<T>(
    @SerializedName("items") val items: List<T>? = null,
    @SerializedName("pageNumber") val pageNumber: Int = 1,
    @SerializedName("pageSize") val pageSize: Int = 10,
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("hasPreviousPage") val hasPreviousPage: Boolean = false,
    @SerializedName("hasNextPage") val hasNextPage: Boolean = false
)

/** One of the 25 festival types returned by `festivals/masters`. Carries the slug. */
data class FestivalMaster(
    @SerializedName("festivalMasterID") val masterId: Int = 0,
    @SerializedName("festival") val name: String? = null,
    @SerializedName("festivalNameTamil") val nameTamil: String? = null,
    @SerializedName("festivalShortName") val shortName: String? = null,
    @SerializedName("festivalShortDesc") val shortDesc: String? = null,
    @SerializedName("festivalShortDescTamil") val shortDescTamil: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("featuredImage") val featuredImage: String? = null
)

/**
 * A dated festival occurrence.
 *
 * `festivals/year/{year}` returns a slimmer shape than `festivals/upcoming` — notably with
 * **no slug** — so every field here is nullable and callers must not assume [slug] is present.
 */
data class FestivalOccurrence(
    @SerializedName("festivalID") val festivalId: Int = 0,
    @SerializedName("festivalMasterID") val masterId: Int? = null,
    @SerializedName("festivalName") val name: String? = null,
    @SerializedName("festivalNameTamil") val nameTamil: String? = null,
    @SerializedName("masterFestivalName") val masterName: String? = null,
    @SerializedName("festivalShortDesc") val shortDesc: String? = null,
    @SerializedName("festivalShortDescTamil") val shortDescTamil: String? = null,
    @SerializedName("festivalDate") val date: String? = null,
    @SerializedName("startingTime") val startingTime: String? = null,
    @SerializedName("endTime") val endTime: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("featuredImage") val featuredImage: String? = null
)

data class FestivalDate(
    @SerializedName("festivalID") val festivalId: Int = 0,
    @SerializedName("festivalDate") val date: String? = null,
    @SerializedName("startingTime") val startingTime: String? = null,
    @SerializedName("endTime") val endTime: String? = null,
    @SerializedName("year") val year: String? = null
)

/**
 * Detail for one festival type.
 *
 * Two live-API caveats, verified 2026-08-27:
 *  - [upcomingDates] is **not** filtered to the future; it includes dates back to 2023.
 *  - [detail] is HTML (`<p>…`), 10-12 KB, not plain prose.
 *  - [featuredImage] comes back empty for the festivals checked. Never depend on it.
 */
data class FestivalDetail(
    @SerializedName("festivalMasterID") val masterId: Int = 0,
    @SerializedName("festival") val name: String? = null,
    @SerializedName("festivalNameTamil") val nameTamil: String? = null,
    @SerializedName("festivalShortName") val shortName: String? = null,
    @SerializedName("festivalDetail") val detail: String? = null,
    @SerializedName("festivalShortDesc") val shortDesc: String? = null,
    @SerializedName("festivalShortDescTamil") val shortDescTamil: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("featuredImage") val featuredImage: String? = null,
    @SerializedName("festivalKeywords") val keywords: String? = null,
    @SerializedName("festivalKeywordsTamil") val keywordsTamil: String? = null,
    @SerializedName("upcomingDates") val upcomingDates: List<FestivalDate>? = null
)

data class Temple(
    @SerializedName("templeID") val templeId: Int = 0,
    @SerializedName("templeName") val name: String? = null,
    @SerializedName("nameTamil") val nameTamil: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("deity") val deity: String? = null,
    @SerializedName("deityTamil") val deityTamil: String? = null,
    @SerializedName("imageURL") val imageUrl: String? = null,
    @SerializedName("templeType") val templeType: String? = null,
    @SerializedName("views") val views: Int? = null
)
