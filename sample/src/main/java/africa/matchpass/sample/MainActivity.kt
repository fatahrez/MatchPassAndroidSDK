package africa.matchpass.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import africa.matchpass.sdk.AccessResult
import africa.matchpass.sdk.MatchPassSDK

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { StreamingHomeScreen() }
    }
}

// ── Colour palette ─────────────────────────────────────────────────────────────
private val BgDark   = Color(0xFF0B0F14)
private val Surface  = Color(0xFF161B26)
private val DstvBlue = Color(0xFF0057FF)
private val Gold     = Color(0xFFFFC300)
private val LiveRed  = Color(0xFFE53935)
private val TextMain = Color(0xFFFFFFFF)
private val TextSub  = Color(0xFF8A9BB8)
private val Scrim    = Color(0x99000000)

@Composable
fun StreamingHomeScreen() {
    val context = LocalContext.current

    // Session state:
    //   null  = still checking stored phone (splash shown)
    //   ""    = user explicitly skipped login (guest)
    //   "..." = verified phone number
    var sessionPhone by remember { mutableStateOf<String?>(null) }
    var isChecking   by remember { mutableStateOf(true) }

    // On first launch read any phone stored from a previous session.
    // If found → skip login gate and go straight to home as logged-in user.
    LaunchedEffect(Unit) {
        sessionPhone = MatchPassSDK.getStoredPhone(context)  // null if never verified
        isChecking = false
    }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        when {
            // ── Brief splash while reading stored state ────────────────────────
            isChecking -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DstvBlue, strokeWidth = 3.dp)
                }
            }

            // ── Login gate — shown on first launch until signed in or skipped ──
            sessionPhone == null -> {
                MatchPassSDK.Login(
                    onLoggedIn = { phone -> sessionPhone = phone },
                    onSkip     = { sessionPhone = "" },  // "" = guest
                )
            }

            // ── Home screen ────────────────────────────────────────────────────
            else -> {
                HomeContent(
                    // "" means guest — pass null so paywall shows OTP
                    loggedInPhone = sessionPhone!!.ifBlank { null },
                    onSignOut     = {
                        MatchPassSDK.signOut(context)
                        sessionPhone = null
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    loggedInPhone: String?,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val accessState = remember { mutableStateMapOf<String, Boolean>() }

    var paywallContent    by remember { mutableStateOf<SampleContent?>(null) }
    var nowPlayingContent by remember { mutableStateOf<SampleContent?>(null) }

    // Re-check access state in a loop. After each pass the loop sleeps until
    // just after the nearest-expiring pass's expiry time, then wakes and
    // re-checks everything. Cards flip from unlocked → locked automatically.
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            var earliestExpiry = Long.MAX_VALUE

            ALL_CONTENT.forEach { item ->
                val result = MatchPassSDK.checkAccess(context, item.passContent)
                accessState[item.passContent.id] = result is AccessResult.Granted

                // Track the soonest upcoming expiry so we wake up at the right time
                MatchPassSDK.getExpiresAt(context, item.passContent.id)?.let { expiry ->
                    if (expiry > now && expiry < earliestExpiry) earliestExpiry = expiry
                }
            }

            val delayMs = if (earliestExpiry < Long.MAX_VALUE) {
                // Wake 1 second after the pass expires; never wait less than 5 s or more than 60 s
                (earliestExpiry - System.currentTimeMillis() + 1_000L).coerceIn(5_000L, 60_000L)
            } else {
                60_000L  // no active passes — idle check every minute
            }
            kotlinx.coroutines.delay(delayMs)
        }
    }

    val onContentClick: (SampleContent) -> Unit = { item ->
        if (accessState[item.passContent.id] == true) nowPlayingContent = item
        else paywallContent = item
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            TopBar(
                loggedInPhone = loggedInPhone,
                onSignOut = onSignOut,
            )
        }

        item {
            FeaturedHero(
                content = LIVE_SPORT.first(),
                hasAccess = accessState[LIVE_SPORT.first().passContent.id] == true,
                onClick = onContentClick,
            )
        }

        item {
            SectionHeader("Live Channels")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(CHANNELS) { item ->
                    ChannelCard(
                        content = item,
                        hasAccess = accessState[item.passContent.id] == true,
                        onClick = onContentClick,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            SectionHeader("Live Sport")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(LIVE_SPORT) { item ->
                    SportCard(
                        content = item,
                        hasAccess = accessState[item.passContent.id] == true,
                        onClick = onContentClick,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            SectionHeader("Movies")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MOVIES.forEach { item ->
                    WideCard(
                        content = item,
                        hasAccess = accessState[item.passContent.id] == true,
                        onClick = onContentClick,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            SectionHeader("Series")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SERIES.forEach { item ->
                    WideCard(
                        content = item,
                        hasAccess = accessState[item.passContent.id] == true,
                        onClick = onContentClick,
                    )
                }
            }
        }
    }

    // ── MatchPass Paywall ──────────────────────────────────────────────────────
    if (paywallContent != null) {
        val item = paywallContent!!
        MatchPassSDK.Paywall(
            content   = item.passContent,
            userPhone = loggedInPhone,
            onAccessGranted = { _ ->
                accessState[item.passContent.id] = true
                paywallContent = null
                nowPlayingContent = item
            },
            onDismiss = { paywallContent = null },
        )
    }

    // ── Now Playing ────────────────────────────────────────────────────────────
    if (nowPlayingContent != null) {
        NowPlayingScreen(
            content = nowPlayingContent!!,
            onBack  = { nowPlayingContent = null },
        )
    }
}

// ── Now Playing ────────────────────────────────────────────────────────────────

@Composable
private fun NowPlayingScreen(content: SampleContent, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(content.bgStart, Color(0xFF000000)))),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(content.emoji, fontSize = 80.sp)
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Playing", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text(text = content.passContent.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(text = content.subtitle, color = Color(0xAAffffff), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33FFC300))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Gold, modifier = Modifier.size(12.dp))
                    Text("MatchPass Active", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    loggedInPhone: String?,   // null = guest (skipped login)
    onSignOut: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(DstvBlue),
                contentAlignment = Alignment.Center,
            ) {
                Text("D", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.width(6.dp))
            Text("Stv Stream", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        if (loggedInPhone != null) {
            // Signed in — show truncated phone + avatar (tap to sign out)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSignOut) {
                    Text("···${loggedInPhone.takeLast(4)}", color = TextSub, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(DstvBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            // Guest — show chip indicating they're browsing without an account
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Surface)
                    .clickable(onClick = onSignOut)  // "sign out" returns to login gate
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("Guest  ·  Sign in", color = TextSub, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Content cards ──────────────────────────────────────────────────────────────

@Composable
private fun ChannelCard(content: SampleContent, hasAccess: Boolean, onClick: (SampleContent) -> Unit) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(content.bgStart, content.bgEnd)))
            .clickable { onClick(content) },
    ) {
        // Channel number top-left
        content.channelNumber?.let { num ->
            Text(
                text = num,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(9.dp),
            )
        }

        // LIVE badge top-right
        if (content.isLive) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                LiveBadge()
            }
        }

        // Big emoji centred (above the text area)
        Text(
            text = content.emoji,
            fontSize = 34.sp,
            modifier = Modifier.align(Alignment.Center).padding(bottom = 36.dp),
        )

        // Bottom info strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xCC000000))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Column {
                Text(
                    text = content.passContent.title,
                    color = TextMain,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                content.onNow?.let { programme ->
                    Text(
                        text = programme,
                        color = TextSub,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(5.dp))
                if (hasAccess) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2ECC71)),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Watching", color = Color(0xFF2ECC71), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    PriceBadge(content.passContent.currency, content.passContent.price)
                }
            }
        }
    }
}

@Composable
private fun FeaturedHero(content: SampleContent, hasAccess: Boolean, onClick: (SampleContent) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable { onClick(content) },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(content.bgStart, content.bgEnd))))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(content.emoji, fontSize = 80.sp, modifier = Modifier.padding(bottom = 40.dp))
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            if (content.isLive) { LiveBadge(); Spacer(Modifier.height(6.dp)) }
            Text(text = content.passContent.title, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = content.subtitle, color = TextSub, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onClick(content) },
                colors = ButtonDefaults.buttonColors(containerColor = if (hasAccess) DstvBlue else Gold),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                if (hasAccess) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Watch Now", fontWeight = FontWeight.Black, fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${content.passContent.currency} ${content.passContent.price} · Get Pass", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SportCard(content: SampleContent, hasAccess: Boolean, onClick: (SampleContent) -> Unit) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(content.bgStart, content.bgEnd)))
            .clickable { onClick(content) },
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(content.emoji, fontSize = 40.sp, modifier = Modifier.padding(bottom = 32.dp))
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000)))))
        if (content.isLive) { Box(modifier = Modifier.padding(8.dp)) { LiveBadge() } }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(text = content.passContent.title, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            if (hasAccess) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DstvBlue, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Watch", color = DstvBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                PriceBadge(content.passContent.currency, content.passContent.price)
            }
        }
    }
}

@Composable
private fun WideCard(content: SampleContent, hasAccess: Boolean, onClick: (SampleContent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .clickable { onClick(content) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(90.dp).fillMaxSize().background(Brush.linearGradient(listOf(content.bgStart, content.bgEnd))),
            contentAlignment = Alignment.Center,
        ) {
            Text(content.emoji, fontSize = 28.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(text = content.passContent.title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(text = content.subtitle, color = TextSub, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(modifier = Modifier.padding(end = 14.dp)) {
            if (hasAccess) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(DstvBlue), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Watch", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            } else {
                PriceBadge(content.passContent.currency, content.passContent.price)
            }
        }
    }
}

// ── Small components ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp, end = 16.dp))
}

@Composable
private fun LiveBadge() {
    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(LiveRed).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text("● LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun PriceBadge(currency: String, price: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Scrim).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = Gold, modifier = Modifier.size(10.dp))
        Text("$currency $price", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
