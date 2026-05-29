package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.crashlytics

object AnalyticsHelper {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }
    private val crashlytics by lazy { Firebase.crashlytics }

    fun setUserId(userId: String) {
        analytics.setUserId(userId)
        crashlytics.setUserId(userId)
    }

    fun logTabOpen(tabName: String) {
        analytics.logEvent("tab_open") {
            param("tab_name", tabName)
        }
    }

    fun logAddTrade(params: Map<String, Any>) {
        analytics.logEvent("add_trade") {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Double -> param(key, value)
                    is Long -> param(key, value)
                    is Int -> param(key, value.toLong())
                }
            }
        }
    }

    fun logEditTrade(params: Map<String, Any>) {
        analytics.logEvent("edit_trade") {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Double -> param(key, value)
                    is Long -> param(key, value)
                    is Int -> param(key, value.toLong())
                }
            }
        }
    }

    fun logDeleteTrade() {
        analytics.logEvent("delete_trade") {}
    }

    fun logFeedbackSubmit(success: Boolean) {
        analytics.logEvent("feedback_submit") {
            param("success", if (success) "1" else "0")
        }
    }

    fun logScreenshotExpand() {
        analytics.logEvent("screenshot_expand") {}
    }

    fun logAiCoachRun(success: Boolean) {
        analytics.logEvent("ai_coach_run") {
            param("success", if (success) "1" else "0")
        }
    }
}
