package com.domedemok.travelplanner.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.domedemok.travelplanner.MainActivity
import com.domedemok.travelplanner.R
import com.domedemok.travelplanner.data.remote.FirestoreCollections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM service that handles:
 * 1. Token refresh → stored in Firestore under users/{uid}/fcmToken
 * 2. Push message received → shows a system notification
 *
 * Registration in AndroidManifest.xml (inside <application>):
 *
 *   <service
 *       android:name=".service.TravelPlannerMessagingService"
 *       android:exported="false">
 *       <intent-filter>
 *           <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *       </intent-filter>
 *   </service>
 *
 * Required permission (Android 13+):
 *   <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
 */
class TravelPlannerMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "TRIP_UPDATES"
    }

    // ── Token management ──────────────────────────────────────────────────────

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection(FirestoreCollections.USERS)
            .document(uid)
            .update("fcmToken", token)
            .addOnFailureListener { /* non-critical — best effort */ }
    }

    // ── Foreground / data message handling ────────────────────────────────────

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Support both notification payloads (sent from FCM console / Cloud Functions)
        // and data-only messages (sent programmatically).
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "TravelPlanner"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return  // nothing to show

        showNotification(title = title, body = body)
    }

    // ── Notification helper ───────────────────────────────────────────────────

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel once on Android 8+ — name and description come from
        // Android string resources so the system Settings screen is locale-aware.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            manager.createNotificationChannel(channel)
        }

        // Tap → open the app at MainActivity (deep-linking into specific trip
        // can be added later via the "tripId" data key).
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)  // replace with R.drawable.ic_notification when available
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
