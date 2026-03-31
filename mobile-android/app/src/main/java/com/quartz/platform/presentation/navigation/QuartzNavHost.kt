package com.quartz.platform.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quartz.platform.presentation.home.map.HomeMapRoute
import com.quartz.platform.presentation.performance.session.PerformanceSessionRoute
import com.quartz.platform.presentation.ret.session.RetGuidedSessionRoute
import com.quartz.platform.presentation.report.draft.ReportDraftRoute
import com.quartz.platform.presentation.report.list.ReportListRoute
import com.quartz.platform.presentation.site.detail.SiteDetailRoute
import com.quartz.platform.presentation.site.list.SiteListRoute
import com.quartz.platform.presentation.xfeeder.session.XfeederGuidedSessionRoute

@Composable
fun QuartzNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = QuartzDestination.HomeMap.route
    ) {
        composable(QuartzDestination.HomeMap.route) {
            HomeMapRoute(
                onSiteSelected = { siteId ->
                    navController.navigate(QuartzDestination.SiteDetail.routeFor(siteId))
                }
            )
        }

        composable(QuartzDestination.SiteList.route) {
            SiteListRoute(
                onSiteSelected = { siteId ->
                    navController.navigate(QuartzDestination.SiteDetail.routeFor(siteId))
                }
            )
        }

        composable(
            route = QuartzDestination.SiteDetail.route,
            arguments = listOf(navArgument(QuartzDestination.SiteDetail.ARG_SITE_ID) { type = NavType.StringType })
        ) {
            SiteDetailRoute(
                onBack = { navController.popBackStack() },
                onOpenDraft = { draftId ->
                    navController.navigate(QuartzDestination.ReportDraft.routeFor(draftId))
                },
                onOpenReportList = { siteId ->
                    navController.navigate(QuartzDestination.ReportList.routeFor(siteId))
                },
                onOpenXfeederSession = { siteId, sectorId ->
                    navController.navigate(
                        QuartzDestination.XfeederGuidedSession.routeFor(
                            siteId = siteId,
                            sectorId = sectorId
                        )
                    )
                },
                onOpenRetSession = { siteId, sectorId ->
                    navController.navigate(
                        QuartzDestination.RetGuidedSession.routeFor(
                            siteId = siteId,
                            sectorId = sectorId
                        )
                    )
                },
                onOpenPerformanceSession = { siteId ->
                    navController.navigate(QuartzDestination.PerformanceSession.routeFor(siteId))
                }
            )
        }

        composable(
            route = QuartzDestination.ReportDraft.route,
            arguments = listOf(navArgument(QuartzDestination.ReportDraft.ARG_DRAFT_ID) { type = NavType.StringType })
        ) {
            ReportDraftRoute(
                onBack = { navController.popBackStack() },
                onOpenReportList = { siteId ->
                    navController.navigate(QuartzDestination.ReportList.routeFor(siteId))
                }
            )
        }

        composable(
            route = QuartzDestination.ReportList.route,
            arguments = listOf(navArgument(QuartzDestination.ReportList.ARG_SITE_ID) { type = NavType.StringType })
        ) {
            ReportListRoute(
                onBack = { navController.popBackStack() },
                onOpenDraft = { draftId ->
                    navController.navigate(QuartzDestination.ReportDraft.routeFor(draftId))
                }
            )
        }

        composable(
            route = QuartzDestination.XfeederGuidedSession.route,
            arguments = listOf(
                navArgument(QuartzDestination.XfeederGuidedSession.ARG_SITE_ID) { type = NavType.StringType },
                navArgument(QuartzDestination.XfeederGuidedSession.ARG_SECTOR_ID) { type = NavType.StringType }
            )
        ) {
            XfeederGuidedSessionRoute(
                onBack = { navController.popBackStack() },
                onOpenDraft = { draftId ->
                    navController.navigate(QuartzDestination.ReportDraft.routeFor(draftId))
                }
            )
        }

        composable(
            route = QuartzDestination.RetGuidedSession.route,
            arguments = listOf(
                navArgument(QuartzDestination.RetGuidedSession.ARG_SITE_ID) { type = NavType.StringType },
                navArgument(QuartzDestination.RetGuidedSession.ARG_SECTOR_ID) { type = NavType.StringType }
            )
        ) {
            RetGuidedSessionRoute(
                onBack = { navController.popBackStack() },
                onOpenDraft = { draftId ->
                    navController.navigate(QuartzDestination.ReportDraft.routeFor(draftId))
                }
            )
        }

        composable(
            route = QuartzDestination.PerformanceSession.route,
            arguments = listOf(
                navArgument(QuartzDestination.PerformanceSession.ARG_SITE_ID) { type = NavType.StringType }
            )
        ) {
            PerformanceSessionRoute(
                onBack = { navController.popBackStack() },
                onOpenDraft = { draftId ->
                    navController.navigate(QuartzDestination.ReportDraft.routeFor(draftId))
                }
            )
        }
    }
}
