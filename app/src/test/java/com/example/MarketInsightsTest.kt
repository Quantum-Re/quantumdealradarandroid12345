package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.MarketInsightsViewModel
import com.example.util.GeminiMarketInsightsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarketInsightsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCuratedBaselineReportIntegrity() {
        val report = GeminiMarketInsightsService.getCuratedBaselineReport(MarketInsightTopic.ALL)

        assertNotNull("Report must not be null", report)
        assertTrue("Baseline report should have headline items", report.headlineItems.isNotEmpty())
        assertTrue("Baseline report should have macro indicators", report.macroIndicators.isNotEmpty())
        assertTrue("Baseline report should have grounding sources", report.groundingSources.isNotEmpty())
        assertFalse("Baseline report should NOT be marked as live grounded", report.isGroundedWithGoogleSearch)

        // Verify headline properties
        for (item in report.headlineItems) {
            assertTrue("Title should not be blank", item.title.isNotBlank())
            assertTrue("Source should not be blank", item.sourcePublication.isNotBlank())
            assertTrue("Summary should not be blank", item.summary.isNotBlank())
            assertTrue("Impact rating should be between 1 and 10", item.impactRating in 1..10)
            assertNotNull("Sentiment should be defined", item.sentiment)
            assertNotNull("Topic should be defined", item.topic)
        }
    }

    @Test
    fun testTopicFiltering() {
        val ratesReport = GeminiMarketInsightsService.getCuratedBaselineReport(MarketInsightTopic.MORTGAGE_RATES)
        assertEquals(MarketInsightTopic.MORTGAGE_RATES, ratesReport.activeTopic)
        assertTrue(ratesReport.headlineItems.isNotEmpty())

        val auctionReport = GeminiMarketInsightsService.getCuratedBaselineReport(MarketInsightTopic.AUCTIONS_NPL)
        assertEquals(MarketInsightTopic.AUCTIONS_NPL, auctionReport.activeTopic)
        assertTrue(auctionReport.headlineItems.isNotEmpty())
    }

    @Test
    fun testSentimentMapping() {
        assertEquals(MarketSentiment.BULLISH, MarketSentiment.fromKey("BULLISH"))
        assertEquals(MarketSentiment.BEARISH, MarketSentiment.fromKey("BEARISH"))
        assertEquals(MarketSentiment.REGULATORY_ALERT, MarketSentiment.fromKey("REGULATORY"))
        assertEquals(MarketSentiment.NEUTRAL, MarketSentiment.fromKey("UNKNOWN"))
    }

    @Test
    fun testViewModelStateAndBookmarking() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MarketInsightsViewModel(app)

        testScheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.report)
        assertTrue(state.report.headlineItems.isNotEmpty())

        // Test bookmarking
        val firstId = state.report.headlineItems.first().id
        vm.toggleBookmark(firstId)
        assertTrue(vm.uiState.value.bookmarkedInsightIds.contains(firstId))

        vm.toggleBookmark(firstId)
        assertFalse(vm.uiState.value.bookmarkedInsightIds.contains(firstId))

        // Test sentiment filtering
        vm.setSentimentFilter(MarketSentiment.BULLISH)
        assertEquals(MarketSentiment.BULLISH, vm.uiState.value.selectedSentimentFilter)

        vm.setSentimentFilter(MarketSentiment.BULLISH)
        assertNull(vm.uiState.value.selectedSentimentFilter)

        // Test topic selection
        vm.selectTopic(MarketInsightTopic.HOTSPOTS_CITIES)
        assertEquals(MarketInsightTopic.HOTSPOTS_CITIES, vm.uiState.value.selectedTopic)

        testScheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.report)
    }
}
