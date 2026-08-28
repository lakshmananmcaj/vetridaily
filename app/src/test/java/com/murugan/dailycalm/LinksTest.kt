package com.murugan.dailycalm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinksTest {

    @Test
    fun channelUrlIsBuiltFromTheIdNotTheHandle() {
        // A handle can be renamed by its owner, which would break the link in every installed copy.
        assertTrue(Links.YOUTUBE_CHANNEL_URL.endsWith(Links.YOUTUBE_CHANNEL_ID))
        assertTrue(Links.YOUTUBE_CHANNEL_URL.startsWith("https://"))
    }

    @Test
    fun purchaseMastersGetTheirDedicatedPortalPage() {
        assertEquals(
            "https://informationneeds.com/auspicious-dates/property-purchase",
            Links.portalUrlForFestival(masterId = 13, slug = "property-purchase-auspicious-days-13")
        )
        assertEquals(
            "https://informationneeds.com/auspicious-dates/vehicle-purchase",
            Links.portalUrlForFestival(masterId = 14, slug = "vehicle-purchase-days-14")
        )
    }

    @Test
    fun otherFestivalsFallBackToTheirOwnPortalPage() {
        assertEquals(
            "https://informationneeds.com/festivals/skanda-shashti-25",
            Links.portalUrlForFestival(masterId = 25, slug = "skanda-shashti-25")
        )
    }

    @Test
    fun aDedicatedPageWinsEvenWithoutASlug() {
        assertEquals(
            "https://informationneeds.com/auspicious-dates/vehicle-purchase",
            Links.portalUrlForFestival(masterId = 14, slug = null)
        )
    }

    @Test
    fun withoutMasterOrSlugThereIsNoLink() {
        // Better no button than a button that 404s.
        assertNull(Links.portalUrlForFestival(masterId = null, slug = null))
        assertNull(Links.portalUrlForFestival(masterId = 99, slug = null))
        assertNull(Links.portalUrlForFestival(masterId = null, slug = "   "))
    }

    @Test
    fun anUnmappedMasterStillUsesItsSlug() {
        assertEquals(
            "https://informationneeds.com/festivals/pournami-1",
            Links.portalUrlForFestival(masterId = 1, slug = "pournami-1")
        )
    }
}
