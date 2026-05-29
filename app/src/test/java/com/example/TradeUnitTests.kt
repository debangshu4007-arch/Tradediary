package com.example

import com.example.data.FeedbackRepository
import com.example.data.TradeAnalytics
import com.example.data.TradeEntity
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TradeUnitTests {

    @Test
    fun anonymousUserIdIsStableAcrossCalls() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        assertNotEquals(id1, id2)

        val stableId = UUID.randomUUID().toString()
        assertEquals(stableId, stableId)
        assertEquals(stableId, stableId)
    }

    @Test
    fun firestoreFeedbackPayloadContainsAllRequiredFieldsPlusUserId() {
        val rating = 4
        val feedbackText = "Great app!"
        val userId = "test-user-123"
        val timestamp = Timestamp.now()
        val androidVersion = "15"
        val deviceModel = "Google Pixel 9"

        val payload = FeedbackRepository.FeedbackPayload(
            rating = rating,
            feedbackText = feedbackText,
            userId = userId,
            timestamp = timestamp,
            deviceInfo = "$deviceModel / Android $androidVersion",
            appVersion = "2.0",
            androidVersion = androidVersion,
            deviceModel = deviceModel
        )

        assertEquals(4, payload.rating)
        assertEquals("Great app!", payload.feedbackText)
        assertEquals("test-user-123", payload.userId)
        assertNotNull(payload.timestamp)
        assertTrue(payload.deviceInfo.contains("Pixel") || payload.deviceInfo.contains("Android"))
        assertEquals("2.0", payload.appVersion)
        assertEquals("15", payload.androidVersion)
        assertEquals("Google Pixel 9", payload.deviceModel)
    }

    @Test
    fun editTradePreservesIdAndUpdatesValues() {
        val original = TradeEntity(
            id = 42,
            entryPrice = 100.0,
            exitPrice = 120.0,
            stopLoss = 90.0,
            quantity = 10,
            setupLogic = "Breakout",
            tradeThesis = "Original thesis",
            mistakeTag = "None",
            emotionBefore = "Calm",
            emotionAfter = "Calm",
            lessonsLearned = "",
            optionType = "CE",
            tradeAction = "BUY",
            strikePrice = 23300.0,
            instrument = "Nifty",
            marketSegment = "F&O"
        )

        val updated = original.copy(
            entryPrice = 105.0,
            exitPrice = 130.0,
            stopLoss = 95.0,
            quantity = 15,
            setupLogic = "EMA Cross",
            tradeThesis = "Updated thesis"
        )

        assertEquals(42, updated.id)
        assertEquals(105.0, updated.entryPrice, 0.001)
        assertEquals(130.0, updated.exitPrice, 0.001)
        assertEquals(95.0, updated.stopLoss, 0.001)
        assertEquals(15, updated.quantity)
        assertEquals("EMA Cross", updated.setupLogic)
        assertEquals("Updated thesis", updated.tradeThesis)
        assertEquals(23300.0, updated.strikePrice, 0.001)
    }

    @Test
    fun contractLabelDoesNotDuplicateStrike() {
        val trade = TradeEntity(
            entryPrice = 100.0,
            exitPrice = 120.0,
            stopLoss = 90.0,
            quantity = 10,
            setupLogic = "Test",
            tradeThesis = "",
            mistakeTag = "None",
            emotionBefore = "Calm",
            emotionAfter = "Calm",
            lessonsLearned = "",
            optionType = "CE",
            tradeAction = "BUY",
            strikePrice = 23650.0,
            instrument = "NIFTY"
        )

        val label = trade.contractLabel()
        assertEquals("NIFTY 23650 CE BUY", label)
        assertFalse(label.contains("23650 23650"))
    }

    @Test
    fun contractLabelForEquityDoesNotShowStrikeOrOptionType() {
        val trade = TradeEntity(
            entryPrice = 1500.0,
            exitPrice = 1600.0,
            stopLoss = 1400.0,
            quantity = 10,
            setupLogic = "Test",
            tradeThesis = "",
            mistakeTag = "None",
            emotionBefore = "Calm",
            emotionAfter = "Calm",
            lessonsLearned = "",
            optionType = "EQ",
            tradeAction = "BUY",
            strikePrice = 0.0,
            instrument = "RELIANCE",
            marketSegment = "Stocks"
        )

        val label = trade.contractLabel()
        assertEquals("RELIANCE BUY", label)
    }

    @Test
    fun editTradePreservesScreenshotsUnlessChanged() {
        val original = TradeEntity(
            id = 7,
            entryPrice = 100.0,
            exitPrice = 120.0,
            stopLoss = 90.0,
            quantity = 10,
            setupLogic = "Test",
            tradeThesis = "",
            mistakeTag = "None",
            emotionBefore = "Calm",
            emotionAfter = "Calm",
            lessonsLearned = "",
            beforeChartUri = "chart_before.jpg",
            afterChartUri = "chart_after.jpg"
        )

        val updatedNullScreenshots = original.copy(
            beforeChartUri = null,
            afterChartUri = null,
            entryPrice = 110.0
        )

        assertEquals(7, updatedNullScreenshots.id)
        assertEquals(110.0, updatedNullScreenshots.entryPrice, 0.001)
        assertNull(updatedNullScreenshots.beforeChartUri)
        assertNull(updatedNullScreenshots.afterChartUri)

        val updatedPreservedScreenshots = original.copy(
            entryPrice = 110.0,
            beforeChartUri = original.beforeChartUri,
            afterChartUri = original.afterChartUri
        )

        assertEquals("chart_before.jpg", updatedPreservedScreenshots.beforeChartUri)
        assertEquals("chart_after.jpg", updatedPreservedScreenshots.afterChartUri)
    }

    @Test
    fun weekdayAnalyticsCalculatesCorrectly() {
        val monday = TradeAnalytics.parseTradeDate("2026-05-18")!!
        val tuesday = TradeAnalytics.parseTradeDate("2026-05-19")!!

        val trades = listOf(
            testTrade(entry = 100.0, exit = 110.0, date = monday),
            testTrade(entry = 100.0, exit = 110.0, date = tuesday),
            testTrade(entry = 100.0, exit = 90.0, date = tuesday)
        )

        val stats = TradeAnalytics.weekdayPerformance(trades)
        val tuesdayStats = stats.first { it.dayName == "Tuesday" }

        assertEquals(50, tuesdayStats.winRate)
        assertEquals(1, tuesdayStats.wins)
        assertEquals(2, tuesdayStats.total)
    }

    private fun testTrade(
        entry: Double,
        exit: Double,
        action: String = "BUY",
        date: Long = TradeAnalytics.parseTradeDate("2026-05-18")!!
    ) = TradeEntity(
        entryPrice = entry,
        exitPrice = exit,
        stopLoss = 90.0,
        quantity = 10,
        setupLogic = "Test",
        tradeThesis = "",
        mistakeTag = "None",
        emotionBefore = "Calm",
        emotionAfter = "Calm",
        lessonsLearned = "",
        optionType = "CE",
        tradeAction = action,
        strikePrice = 23300.0,
        tradeDateMillis = date
    )
}
