package africa.matchpass.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import africa.matchpass.sdk.MatchPassContent
import africa.matchpass.sdk.MatchPassSDK

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = MatchPassContent(
            id = "epl-match-001",
            title = "Arsenal vs Manchester City",
            price = "29.00",
            currency = "ZAR",
            durationHours = 4,
            thumbnailUrl = null,
        )

        setContent {
            var showPaywall by remember { mutableStateOf(false) }

            if (showPaywall) {
                // ── This is the entire MatchPass integration surface ──────────
                MatchPassSDK.Paywall(
                    content = content,
                    onAccessGranted = { grant ->
                        showPaywall = false
                        Toast.makeText(this, "Access granted! Token: ${grant.token.take(12)}…", Toast.LENGTH_LONG).show()
                        // → pass grant.token to your streaming backend here
                    },
                    onDismiss = { showPaywall = false },
                )
                // ─────────────────────────────────────────────────────────────
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { showPaywall = true }) {
                        Text("Watch Arsenal vs Manchester City — ZAR 29.00")
                    }
                }
            }
        }
    }
}
