package africa.matchpass.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
    val accessState = remember { mutableStateMapOf<String, Boolean>() }
    var selectedContent by remember { mutableStateOf<SampleContent?>(null) }

    // Silently check which content the user already has valid passes for.
    // AccessChecker respects PassPolicy cache TTLs — MOVIE/SEASON won't hit the server.
    LaunchedEffect(Unit) {
        ALL_CONTENT.forEach { item ->
            val result = MatchPassSDK.checkAccess(context, item.passContent)
            accessState[item.passContent.id] = result is AccessResult.Granted
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item { TopBar() }

            item {
                FeaturedHero(
                    content = LIVE_SPORT.first(),
                    hasAccess = accessState[LIVE_SPORT.first().passContent.id] == true,
                    onClick = { selectedContent = it },
                )
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
                            onClick = { selectedContent = it },
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
                            onClick = { selectedContent = it },
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
                            onClick = { selectedContent = it },
                        )
                    }
                }
            }
        }

        // ── MatchPassSDK.Paywall overlaid on top when content is selected ──────
        AnimatedVisibility(visible = selectedContent != null, enter = fadeIn(), exit = fadeOut()) {
            selectedContent?.let { item ->
                MatchPassSDK.Paywall(
                    content = item.passContent,
                    onAccessGranted = { _ ->
                        // Update lock indicator immediately — user can resume instantly next open
                        accessState[item.passContent.id] = true
                        selectedContent = null
                    },
                    onDismiss = { selectedContent = null },
                )
            }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun TopBar() {
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
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DstvBlue),
                contentAlignment = Alignment.Center,
            ) {
                Text("D", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.width(6.dp))
            Text("Stv Stream", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Surface),
            contentAlignment = Alignment.Center,
        ) {
            Text("M", color = DstvBlue, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

@Composable
private fun FeaturedHero(
    content: SampleContent,
    hasAccess: Boolean,
    onClick: (SampleContent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable { onClick(content) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(content.bgStart, content.bgEnd))),
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(content.emoji, fontSize = 80.sp, modifier = Modifier.padding(bottom = 40.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            if (content.isLive) { LiveBadge(); Spacer(Modifier.height(6.dp)) }
            Text(
                text = content.passContent.title,
                color = TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
                    Text(
                        "${content.passContent.currency} ${content.passContent.price} · Get Pass",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SportCard(
    content: SampleContent,
    hasAccess: Boolean,
    onClick: (SampleContent) -> Unit,
) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000)))),
        )
        if (content.isLive) {
            Box(modifier = Modifier.padding(8.dp)) { LiveBadge() }
        }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(
                text = content.passContent.title,
                color = TextMain,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
private fun WideCard(
    content: SampleContent,
    hasAccess: Boolean,
    onClick: (SampleContent) -> Unit,
) {
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
            modifier = Modifier
                .width(90.dp)
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(content.bgStart, content.bgEnd))),
            contentAlignment = Alignment.Center,
        ) {
            Text(content.emoji, fontSize = 28.sp)
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        ) {
            Text(
                text = content.passContent.title,
                color = TextMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = content.subtitle, color = TextSub, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(modifier = Modifier.padding(end = 14.dp)) {
            if (hasAccess) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DstvBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Watch", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            } else {
                PriceBadge(content.passContent.currency, content.passContent.price)
            }
        }
    }
}

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
