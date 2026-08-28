package com.murugan.dailycalm

/**
 * Outbound destinations, in one place so the app, the share card and the More tab cannot drift.
 */
object Links {

    /**
     * Addressed by channel id rather than the @handle: a handle can be renamed by its owner, which
     * would break the link in every installed copy until an update shipped. Ids are permanent.
     */
    const val YOUTUBE_CHANNEL_ID = "UCrCQJA4nBDpnmE3KXRezt0Q"

    const val YOUTUBE_CHANNEL_URL = "https://www.youtube.com/channel/$YOUTUBE_CHANNEL_ID"

    /** What people read. Shown on cards and in the More tab; never used as the target. */
    const val YOUTUBE_HANDLE = "@murugandevotee"

    const val PORTAL_URL = "https://informationneeds.com"

    /**
     * Festival masters that have a dedicated portal page, keyed by `festivalMasterID`.
     *
     * Someone reading a vehicle-purchase muhurat wants the vehicle-purchase page, not the generic
     * festival entry — and those two masters alone account for 157 of the year's rows, so this is
     * the difference between a "read more" that matches the screen and one that does not.
     */
    private val PORTAL_PATH_BY_MASTER_ID = mapOf(
        13 to "/auspicious-dates/property-purchase",
        14 to "/auspicious-dates/vehicle-purchase"
    )

    /**
     * Where "read more" should go for a festival. Falls back to the festival's own portal page,
     * which exists for every slug.
     */
    fun portalUrlForFestival(masterId: Int?, slug: String?): String? {
        PORTAL_PATH_BY_MASTER_ID[masterId]?.let { return "$PORTAL_URL$it" }
        return slug?.takeIf { it.isNotBlank() }?.let { "$PORTAL_URL/festivals/$it" }
    }
}
