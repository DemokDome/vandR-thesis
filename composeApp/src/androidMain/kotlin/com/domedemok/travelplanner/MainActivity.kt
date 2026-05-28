package com.domedemok.travelplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.domedemok.travelplanner.util.JOIN_DEEP_LINK_HOST
import com.domedemok.travelplanner.util.JOIN_DEEP_LINK_SCHEME

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Extract the 6-character join code from a `travelplanner://join/{code}` deep link.
        // The scheme and host are constants shared with AndroidManifest's intent-filter.
        val joinCode = intent?.data?.let { uri ->
            if (uri.scheme == JOIN_DEEP_LINK_SCHEME && uri.host == JOIN_DEEP_LINK_HOST) {
                uri.lastPathSegment?.uppercase()
            } else null
        }

        setContent {
            App(initialJoinCode = joinCode)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
