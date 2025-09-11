package com.kilovativedesigns.parkaware.push

import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Saves the current device's FCM token ONLY if a Firebase user is signed in.
 * Tokens are stored per device under: users/{uid}/tokens/{token}
 */
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    /** Fetch device token and store it if a user is signed in. Safe to call anytime. */
    fun storeCurrentTokenIfSignedIn() {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Log.d(TAG, "No user; not fetching/saving token.")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "Empty token; nothing to save.")
                    return@addOnSuccessListener
                }
                storeToken(token)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch FCM token", e)
            }
    }

    /** Persist a specific token for the signed-in user. */
    fun storeToken(token: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Log.d(TAG, "No user; skipping storeToken().")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val tokenDoc = db.collection("users")
            .document(user.uid)
            .collection("tokens")
            .document(token)

        val data = mapOf(
            "token" to token,
            "platform" to "android",
            "updatedAt" to FieldValue.serverTimestamp(),
            "device" to Build.MANUFACTURER + " " + Build.MODEL,
            "sdk" to Build.VERSION.SDK_INT
        )

        tokenDoc.set(data)
            .addOnSuccessListener { Log.d(TAG, "Token saved for ${user.uid}") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save token", e) }
    }

    /** Optional: remove this device’s token when the user signs out. */
    fun removeTokenIfSignedIn(token: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(user.uid)
            .collection("tokens").document(token)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "Token removed") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to remove token", e) }
    }
}