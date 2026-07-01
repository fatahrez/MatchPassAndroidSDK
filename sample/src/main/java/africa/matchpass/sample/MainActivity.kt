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
        setContent { StreamingApp() }
    }
}

// ── Colour palette ─────────────────────────────────────────────────────────────
private val BgDark   = Color(0xFF0B0F14)
private val Surface  = Color(0xFF161B26)
private val Brand    = Color(0xFF0057FF)
private val Gold     = Color(0xFFFFC300)
private val LiveRed  = Color(0xFFE53935)
private val TextMain = Color(0xFFFFFFFF)
private val TextSub  = Color(0xFF8A9BB8)
private val Scrim    = Color(0x99000000)

// ── Root composable ────────────────────────────────────────────────────────────
//
// Browse-first flow: users land on the home screen immediately, no mandatory
// login gate. If a phone was verified in a previous session it's restored so
// pass-restore and "already-purchased" detection work automatically.
// The MatchPass paywall handles phone + OTP inline when a user taps locked
// content — operators never need to build their own payment or auth UI.

@Composable
fun StreamingApp() {
    val context = LocalContext.current

    // null = still reading storage, "" = no verified phone, "..." = verified phone
    var sessionPhone by remember { mutableStateOf<String?>(null) }
    var isChecking   by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        sessionPhone = MatchPassSDK.getStoredPhone(context)
        isChecking = false
    }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        when {
            isChecking -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Brand, strokeWidth = 3.dp)
                }
            }
            else -> {
                // Go straight to home — no login gate
                HomeContent(
                    loggedInPhone = sessionPhone?.ifBlank { null },
                    onPhoneVerified = { phone -> sessionPhone = phone },
                    onSignOut = {
                        MatchPassSDK.signOut(context)
                        sessionPhone = ""
                    },
                )
            }
        }
    }
}

// ── Home ───────────────────────────────────────────────────────────────────────

@Composable
private fun HomeContent(
    loggedInPhone: String?,
    onPhoneVerified: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val accessState = remember { mutableStateMapOf<String, Boolean>() }

    var paywallContent    by remember { mutableStateOf<SampleContent?>(null) }
    var nowPlayingContent by remember { mutableStateOf<SampleContent?>(null) }

    // Refresh access state; wake up when the nearest pass expires
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            var earliestExpiry = Long.MAX_VALUE

            ALL_CONTENT.forEach { item ->
                val result = MatchPassSDK.checkAccess(context, item.passContent)
                accessState[item.passContent.id] = result is AccessResult.Granted
                MatchPassSDK.getExpiresAt(context, item.passContent.id)?.let { exp ->
                    if (exp > now && exp < earliestExpiry) earliestExpiry = exp
                }
            }

            val delay = if (earliestExpiry < Long.MAX_VALUE)
                (earliestExpiry - System.currentTimeMillis() + 1_000L).coerceIn(5_000L, 60_000L)
            else 60_000L
            kotlinx.coroutines.delay(delay)
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
            SectionHeader("Upcoming Fixtures")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(LIVE_SPORT) { item ->
                    FixtureCard(
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

    // ── MatchPass Paywall — the only integration point ─────────────────────────
    // The operator just calls MatchPassSDK.Paywall(). Everything else — phone
    // entry, OTP, M-Pesa payment, pass issuing — is handled by the SDK.
    if (paywallContent != null) {
        val item = paywallContent!!
        MatchPassSDK.Paywall(
            content   = item.passContent,
            userPhone = loggedInPhone,
            onAccessGranted = { _ ->
                accessState[item.passContent.id] = true
                paywallContent = null
                nowPlayingContent = item
                // If the user just verified their phone inside the paywall,
                // update our session so top-bar and future paywalls skip OTP
                MatchPassSDK.getStoredPhone(context)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { onPhoneVerified(it) }
            },
            onDismiss = { paywallContent = null },
        )
    }

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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
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
                Text(
                    text = content.passContent.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(text = content.subtitle, color = Color(0xAAffffff), fontSize = 13.sp)
                content.kickoffLabel?.let { label ->
                    Spacer(Modifier.height(4.dp))
                    Text(text = label, color = Color(0xAAffffff), fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
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
private fun TopBar(loggedInPhone: String?, onSignOut: () -> Unit) {
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
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(Brand),
                contentAlignment = Alignment.Center,
            ) {
                Text("S", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.width(6.dp))
            Text("StreamPlay", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        if (loggedInPhone != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "···${loggedInPhone.takeLast(4)}",
                    color = TextSub,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Surface)
                        .clickable(onClick = onSignOut)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Brand),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        // No phone yet — top bar stays clean. MatchPass prompts for phone
        // inline when the user taps locked content.
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
        content.channelNumber?.let { num ->
            Text(
                text = num,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(9.dp),
            )
        }
        if (content.isLive) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) { LiveBadge() }
        }
        Text(
            text = content.emoji,
            fontSize = 34.sp,
            modifier = Modifier.align(Alignment.Center).padding(bottom = 36.dp),
        )
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
                content.onNow?.let {
                    Text(text = it, color = TextSub, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(5.dp))
                if (hasAccess) WatchingBadge() else PriceBadge(content.passContent.currency, content.passContent.price)
            }
        }
    }
}

@Composable
private fun FeaturedHero(content: SampleContent, hasAccess: Boolean, onClick: (SampleContent) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clickable { onClick(content) },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(content.bgStart, content.bgEnd))))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(content.emoji, fontSize = 90.sp, modifier = Modifier.padding(bottom = 40.dp))
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            if (content.isLive) { LiveBadge(); Spacer(Modifier.height(6.dp)) }
            Text(text = content.passContent.title, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = content.subtitle, color = TextSub, fontSize = 13.sp)
            content.kickoffLabel?.let {
                Text(text = it, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onClick(content) },
                colors = ButtonDefaults.buttonColors(containerColor = if (hasAccess) Brand else Gold),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                if (hasAccess) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Watch Now", fontWeight = FontWeight.Black, fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0d0d0d))
                    Spacer(Modifier.width(4.dp))
                    Text("ZAR ${content.passContent.price} · Get Pass", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF0d0d0d))
                }
            }
        }
    }
}

@Composable
private fun FixtureCard(content: SampleContent, hasAccess: Boolean, onClick: (SampleContent) -> Unit) {
    Box(
        modifier = Modifier
            .width(170.dp)
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(content.bgStart, content.bgEnd)))
            .clickable { onClick(content) },
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(content.emoji, fontSize = 42.sp, modifier = Modifier.padding(bottom = 40.dp))
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE000000)))))
        if (content.isLive) { Box(modifier = Modifier.padding(8.dp)) { LiveBadge() } }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(
                text = content.passContent.title,
                color = TextMain,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = content.subtitle, color = TextSub, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            content.kickoffLabel?.let {
                Text(text = it, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            if (hasAccess) WatchingBadge() else PriceBadge(content.passContent.currency, content.passContent.price)
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
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Brand), contentAlignment = Alignment.Center) {
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
    Text(
        text = title,
        color = TextMain,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp, end = 16.dp),
    )
}

@Composable
private fun LiveBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(LiveRed)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text("● LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun PriceBadge(currency: String, price: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Scrim)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = Gold, modifier = Modifier.size(10.dp))
        Text("$currency $price", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WatchingBadge() {
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
}
