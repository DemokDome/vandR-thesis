package com.domedemok.travelplanner.config

import com.domedemok.travelplanner.BuildKonfig

/**
 * Web Firebase configuration.
 *
 * These values are public by design — the Firebase JS SDK ships them in the
 * client bundle, and access is secured by Firestore security rules plus
 * API-key restrictions configured in the Firebase/GCP console, not by hiding
 * them. They are nonetheless injected from `local.properties` via BuildKonfig
 * so they stay out of source control and are easy to rotate.
 */
object FirebaseConfig {
    val API_KEY        = BuildKonfig.FIREBASE_API_KEY
    val AUTH_DOMAIN    = BuildKonfig.FIREBASE_AUTH_DOMAIN
    val DATABASE_URL   = BuildKonfig.FIREBASE_DATABASE_URL
    val PROJECT_ID     = BuildKonfig.FIREBASE_PROJECT_ID
    val STORAGE_BUCKET = BuildKonfig.FIREBASE_STORAGE_BUCKET
    val GCM_SENDER_ID  = BuildKonfig.FIREBASE_GCM_SENDER_ID
    val APP_ID         = BuildKonfig.FIREBASE_APP_ID
}
