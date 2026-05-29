package com.example.data

sealed class FeedbackSubmitState {
    object Idle : FeedbackSubmitState()
    object Loading : FeedbackSubmitState()
    object Success : FeedbackSubmitState()
    data class Error(val message: String) : FeedbackSubmitState()
}
