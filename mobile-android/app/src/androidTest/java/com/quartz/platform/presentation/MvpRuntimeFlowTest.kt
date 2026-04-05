package com.quartz.platform.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quartz.platform.MainActivity
import com.quartz.platform.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MvpRuntimeFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun home_map_shows_bootstrap_ready_or_cached_sites() {
        val title = string(R.string.title_home_map)
        val emptyMessage = string(R.string.empty_site_snapshot_cache)
        val loadDemoAction = string(R.string.action_load_demo_snapshot)
        val siteCodePrefix = siteCodePrefix()

        composeRule.onNodeWithText(title).assertIsDisplayed()

        if (nodeExists(emptyMessage)) {
            composeRule.onNodeWithText(loadDemoAction).assertIsDisplayed()
            composeRule.onNodeWithText(string(R.string.empty_site_snapshot_cache_hint)).assertIsDisplayed()
            return
        }

        composeRule.onAllNodesWithText(siteCodePrefix, substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun navigation_to_report_draft_shows_reviewer_runtime_structure() {
        ensureDemoSitesLoaded()
        openDemoSiteDetail()

        composeRule.onNodeWithText(string(R.string.title_site_detail)).assertIsDisplayed()
        scrollToTextIfNeeded(string(R.string.action_create_local_draft))
        composeRule.waitUntilExists(string(R.string.action_create_local_draft))
        composeRule.onNodeWithText(string(R.string.action_create_local_draft)).assertIsDisplayed()
        scrollToTextIfNeeded(string(R.string.action_open_site_local_reports))
        composeRule.waitUntilExists(string(R.string.action_open_site_local_reports))
        composeRule.onNodeWithText(string(R.string.action_open_site_local_reports)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.action_create_local_draft)).performClick()

        composeRule.waitUntilExists(string(R.string.title_report_draft))
        composeRule.onNodeWithText(string(R.string.title_report_draft)).assertIsDisplayed()
        composeRule.waitUntilExists(string(R.string.report_draft_runtime_state_title))
        composeRule.onNodeWithText(string(R.string.report_draft_runtime_state_title)).assertIsDisplayed()
        scrollToTextIfNeeded(string(R.string.report_draft_section_actions_title))
        composeRule.waitUntilExists(string(R.string.report_draft_section_actions_title))
        composeRule.onNodeWithText(string(R.string.report_draft_section_actions_title)).assertIsDisplayed()
        scrollToTextIfNeeded(string(R.string.report_draft_action_show_technical_evidence))
        composeRule.waitUntilExists(string(R.string.report_draft_action_show_technical_evidence))
        composeRule.onNodeWithText(string(R.string.report_draft_action_show_technical_evidence)).assertIsDisplayed()
    }

    @Test
    fun navigation_to_report_list_shows_local_draft_sync_state() {
        ensureDemoSitesLoaded()
        openDemoSiteDetail()

        scrollToTextIfNeeded(string(R.string.action_create_local_draft))
        composeRule.waitUntilExists(string(R.string.action_create_local_draft))
        composeRule.onNodeWithText(string(R.string.action_create_local_draft)).performClick()
        composeRule.waitUntilExists(string(R.string.title_report_draft))
        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntilExists(string(R.string.title_site_detail))
        scrollToTextIfNeeded(string(R.string.action_open_site_local_reports))
        composeRule.waitUntilExists(string(R.string.action_open_site_local_reports))
        composeRule.onNodeWithText(string(R.string.action_open_site_local_reports)).performClick()

        composeRule.waitUntilExists(string(R.string.title_local_reports))
        composeRule.onNodeWithText(string(R.string.title_local_reports)).assertIsDisplayed()
        composeRule.waitUntilExistsSubstring(syncStatePrefix())
        composeRule
            .onAllNodesWithText(syncStatePrefix(), substring = true)
            .onFirst()
            .assertIsDisplayed()
        composeRule
            .onAllNodesWithText(string(R.string.action_open_draft))
            .onFirst()
            .assertIsDisplayed()
    }

    private fun ensureDemoSitesLoaded() {
        if (nodeExistsSubstring(siteCodePrefix())) return

        val loadDemoAction = string(R.string.action_load_demo_snapshot)
        if (nodeExists(loadDemoAction)) {
            composeRule.onAllNodesWithText(loadDemoAction)[0].performClick()
        }

        composeRule.waitUntilExistsSubstring(siteCodePrefix())
    }

    private fun openDemoSiteDetail() {
        composeRule.waitUntilExists(string(R.string.home_action_open_site_intelligence))
        composeRule.onNodeWithText(string(R.string.home_action_open_site_intelligence)).performClick()
        composeRule.waitUntilExists(string(R.string.title_site_detail))
    }

    private fun scrollToTextIfNeeded(text: String) {
        if (nodeExists(text)) return
        val scrollContainers = composeRule.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes()
        if (scrollContainers.isEmpty()) return
        composeRule
            .onAllNodes(hasScrollToNodeAction())
            .onFirst()
            .performScrollToNode(hasText(text))
        composeRule.waitForIdle()
    }

    private fun syncStateText(stateLabel: String): String {
        return string(R.string.label_sync_state, stateLabel)
    }

    private fun syncStatePrefix(): String {
        val marker = "__sync_state__"
        return syncStateText(marker).substringBefore(marker)
    }

    private fun string(resId: Int, vararg args: Any): String {
        return composeRule.activity.getString(resId, *args)
    }

    private fun nodeExists(text: String): Boolean {
        return composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun nodeExistsSubstring(text: String): Boolean {
        return composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun siteCodePrefix(): String {
        val marker = "__site_code__"
        return string(R.string.label_site_code, marker).substringBefore(marker)
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitUntilExists(
        text: String,
        timeoutMillis: Long = 15_000L
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitUntilExistsSubstring(
        text: String,
        timeoutMillis: Long = 15_000L
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
