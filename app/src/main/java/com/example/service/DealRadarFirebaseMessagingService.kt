package com.example.service

import android.util.Log
import com.example.util.FcmPushManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DealRadarFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New Firebase Cloud Messaging Registration Token: $token")
        FcmPushManager.onNewToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Incoming FCM Push Notification received from: ${remoteMessage.from}")
        FcmPushManager.handleRemoteMessage(applicationContext, remoteMessage)
    }

    companion object {
        private const val TAG = "DealRadarFcmService"
    }
}
