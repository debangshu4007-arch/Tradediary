package com.example

import com.example.data.TradeAnalytics
import com.example.data.TradeEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun buyAndSellTradesCalculateProfitCorrectly() {
    val buyWinner = testTrade(entry = 100.0, exit = 120.0, action = "BUY")
    val buyLoser = testTrade(entry = 100.0, exit = 80.0, action = "BUY")
    val sellWinner = testTrade(entry = 100.0, exit = 80.0, action = "SELL")
    val sellLoser = testTrade(entry = 100.0, exit = 120.0, action = "SELL")

    assertEquals(200.0, buyWinner.getPL(), 0.001)
    assertTrue(buyWinner.isWin())
    assertEquals(-200.0, buyLoser.getPL(), 0.001)
    assertFalse(buyLoser.isWin())
    assertEquals(200.0, sellWinner.getPL(), 0.001)
    assertTrue(sellWinner.isWin())
    assertEquals(-200.0, sellLoser.getPL(), 0.001)
    assertFalse(sellLoser.isWin())
  }

  @Test
  fun weekdayAnalyticsCalculatesWinRatesAndAveragePL() {
    val monday = TradeAnalytics.parseTradeDate("2026-05-18")!!
    val tuesday = TradeAnalytics.parseTradeDate("2026-05-19")!!
    val wednesday = TradeAnalytics.parseTradeDate("2026-05-20")!!

    val trades = listOf(
      testTrade(entry = 100.0, exit = 110.0, date = monday),
      testTrade(entry = 100.0, exit = 110.0, date = tuesday),
      testTrade(entry = 100.0, exit = 110.0, date = tuesday),
      testTrade(entry = 100.0, exit = 110.0, date = tuesday),
      testTrade(entry = 100.0, exit = 110.0, date = tuesday),
      testTrade(entry = 100.0, exit = 90.0, date = tuesday),
      testTrade(entry = 100.0, exit = 110.0, date = wednesday),
      testTrade(entry = 100.0, exit = 90.0, date = wednesday),
      testTrade(entry = 100.0, exit = 90.0, date = wednesday),
      testTrade(entry = 100.0, exit = 90.0, date = wednesday),
      testTrade(entry = 100.0, exit = 90.0, date = wednesday)
    )

    val stats = TradeAnalytics.weekdayPerformance(trades)
    val mondayStats = stats.first { it.dayName == "Monday" }
    val tuesdayStats = stats.first { it.dayName == "Tuesday" }
    val wednesdayStats = stats.first { it.dayName == "Wednesday" }
    val thursdayStats = stats.first { it.dayName == "Thursday" }

    assertEquals(100, mondayStats.winRate)
    assertEquals(1, mondayStats.wins)
    assertEquals(1, mondayStats.total)
    assertEquals(80, tuesdayStats.winRate)
    assertEquals(4, tuesdayStats.wins)
    assertEquals(5, tuesdayStats.total)
    assertEquals(20, wednesdayStats.winRate)
    assertEquals(1, wednesdayStats.wins)
    assertEquals(5, wednesdayStats.total)
    assertFalse(thursdayStats.hasTrades)
    assertEquals(0, thursdayStats.winRate)
    assertEquals("Monday", TradeAnalytics.bestWeekday(stats)?.dayName)
    assertEquals("Wednesday", TradeAnalytics.worstWeekday(stats)?.dayName)
    assertEquals(60.0, tuesdayStats.averagePL, 0.001)
  }

  @Test
  fun partialExitsCalculateWeightedProfitAndReward() {
    val scaledTrade = testTrade(entry = 100.0, exit = 130.0).copy(
      quantity = 300,
      stopLoss = 90.0,
      partialExits = "150@120, 150@140"
    )

    assertEquals(9000.0, scaledTrade.getPL(), 0.001)
    assertEquals(130.0, scaledTrade.effectiveExitPrice(), 0.001)
    assertEquals("1:3.0", scaledTrade.getRiskRewardRatioString())
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
