package com.example.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class FcmPushType {
    PRICE_DROP,
    STATUS_CHANGE,
    GRAVE_DANCER_DISTRESS,
    NEW_AUCTION,
    BRIEF_MATCH,
    SYSTEM_ANNOUNCEMENT
}

data class FcmPushAlert(
    val id: String = System.currentTimeMillis().toString(),
    val dealId: Long? = null,
    val type: FcmPushType = FcmPushType.PRICE_DROP,
    val title: String,
    val body: String,
    val propertyTitle: String = "",
    val address: String = "",
    val city: String = "",
    val oldPrice: Double? = null,
    val newPrice: Double? = null,
    val discountPercent: Double? = null,
    val oldStatus: String? = null,
    val newStatus: String? = null,
    val receivedTimestamp: Long = System.currentTimeMillis(),
    val deepLink: String? = null,
    val isRead: Boolean = false
)

data class FcmTopicItem(
    val topicId: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val isSubscribed: Boolean = true,
    val channelId: String
)
