package com.kilovativedesigns.parkaware.ui.auth

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kilovativedesigns.parkaware.util.FcmTokenManager

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save only when user is signed in (method internally checks this)
        FcmTokenManager.storeToken(token)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        // If you use "notification" payloads, the system tray handles UI.
        // Handle "data" payloads here if needed.
    }
}