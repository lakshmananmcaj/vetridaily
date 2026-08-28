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
}
