package com.quartz.platform.presentation.navigation

import android.net.Uri

sealed class QuartzDestination(val route: String) {
    data object HomeMap : QuartzDestination("home_map")

    data object SiteList : QuartzDestination("site_list")

    data object SiteDetail : QuartzDestination("site_detail/{siteId}") {
        const val ARG_SITE_ID = "siteId"
        fun routeFor(siteId: String): String = "site_detail/${Uri.encode(siteId)}"
    }

    data object ReportDraft : QuartzDestination("report_draft/{draftId}") {
        const val ARG_DRAFT_ID = "draftId"
        fun routeFor(draftId: String): String = "report_draft/${Uri.encode(draftId)}"
    }

    data object ReportList : QuartzDestination("report_list/{siteId}") {
        const val ARG_SITE_ID = "siteId"
        fun routeFor(siteId: String): String = "report_list/${Uri.encode(siteId)}"
    }

    data object XfeederGuidedSession : QuartzDestination("xfeeder_session/{siteId}/{sectorId}") {
        const val ARG_SITE_ID = "siteId"
        const val ARG_SECTOR_ID = "sectorId"
        fun routeFor(siteId: String, sectorId: String): String {
            return "xfeeder_session/${Uri.encode(siteId)}/${Uri.encode(sectorId)}"
        }
    }

    data object RetGuidedSession : QuartzDestination("ret_session/{siteId}/{sectorId}") {
        const val ARG_SITE_ID = "siteId"
        const val ARG_SECTOR_ID = "sectorId"
        fun routeFor(siteId: String, sectorId: String): String {
            return "ret_session/${Uri.encode(siteId)}/${Uri.encode(sectorId)}"
        }
    }
}
