package com.example.session_kotlin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.example.session_kotlin.R

// ---------- Navigation shell ----------

sealed class Screen {
    data object MainMenu : Screen()
    data object ChooseSport : Screen()
    data object NameTeams : Screen()
    data object History : Screen()
    data object Scoreboard : Screen()
}

@Composable
fun AppRoot(
    vm: ScoreViewModel,
    onQuit: () -> Unit
) {
    var screen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var pickedSport by rememberSaveable { mutableStateOf(SportOption.TENNIS) }
    var left by rememberSaveable { mutableStateOf("Left") }
    var right by rememberSaveable { mutableStateOf("Right") }

    when (screen) {
        Screen.MainMenu -> MainMenuScreen(
            onNewMatch = { screen = Screen.ChooseSport },
            onHistory = { screen = Screen.History },
            onQuit = onQuit
        )
        Screen.ChooseSport -> ChooseSportScreen(
            initial = pickedSport,
            onPick = { pickedSport = it; screen = Screen.NameTeams },
            onBack = { screen = Screen.MainMenu }
        )
        Screen.NameTeams -> NameTeamsScreen(
            leftInitial = left,
            rightInitial = right,
            onConfirm = { l, r ->
                left = l; right = r
                vm.startNewMatch(pickedSport, left, right)
                screen = Screen.Scoreboard
            },
            onBack = { screen = Screen.ChooseSport }
        )
        Screen.History -> HistoryScreen(
            vm = vm,
            onBack = { screen = Screen.MainMenu }
        )
        Screen.Scoreboard -> MainScreen(
            vm = vm,
            onBack = { screen = Screen.MainMenu }
        )
    }
}

// ---------- Simple screens ----------

@Composable
fun MainMenuScreen(
    onNewMatch: () -> Unit,
    onHistory: () -> Unit,
    onQuit: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Chip(onClick = onNewMatch, label = { Text("New Match") }) }
        item { Chip(onClick = onHistory, label = { Text("History") }) }
        item { Chip(onClick = onQuit, label = { Text("Quit") }) }
    }
}

@Composable
fun ChooseSportScreen(
    initial: SportOption,
    onPick: (SportOption) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { Text("Choose Sport", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            item { Chip(onClick = { onPick(SportOption.TENNIS) }, label = { Text("Tennis") }) }
            item { Chip(onClick = { onPick(SportOption.PADEL) }, label = { Text("Padel") }) }
            item { Chip(onClick = onBack, label = { Text("Back") }) }
        }
    }
}

@Composable
private fun WearTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            if (value.isEmpty()) Text(text = " ", fontSize = 14.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun NameTeamsScreen(
    leftInitial: String,
    rightInitial: String,
    onConfirm: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var left by rememberSaveable { mutableStateOf(leftInitial) }
    var right by rememberSaveable { mutableStateOf(rightInitial) }
    val listState = rememberScalingLazyListState()

    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { Text("Team Names", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            item { WearTextField(value = left, onValueChange = { left = it }, label = "Left team") }
            item { WearTextField(value = right, onValueChange = { right = it }, label = "Right team") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(onClick = { onConfirm(left.trim(), right.trim()) }, label = { Text("Continue") })
                    Chip(onClick = onBack, label = { Text("Back") })
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    vm: ScoreViewModel,
    onBack: () -> Unit
) {
    val history by vm.history.collectAsState()
    val listState = rememberScalingLazyListState()

    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { Text("Matches History", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            item { Chip(onClick = { vm.clearHistory() }, label = { Text("Clear History") }) }

            if (history.isEmpty()) {
                item { Text("No matches yet") }
            } else {
                items(history) { rec ->
                    val title = "${rec.leftName} vs ${rec.rightName}"
                    val sportLabel = when (rec.sport) {
                        SportOption.TENNIS -> "Tennis"
                        SportOption.PADEL -> "Padel"
                    }
                    val setsLine = buildString {
                        append("$sportLabel ${rec.leftSets}-${rec.rightSets}")
                        if (rec.setSummary.isNotBlank()) append(" (${rec.setSummary})")
                    }

                    Card(
                        onClick = { /* details later */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = setsLine,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }

            item { Chip(onClick = onBack, label = { Text("Back") }) }
        }
    }
}

// ---------- Shared bits ----------

@Composable
private fun CenteredDashLine(
    line: String,
    fontSize: TextUnit,
    fontWeight: FontWeight? = null
) {
    val idx = line.indexOf('–').let { if (it < 0) line.indexOf('-') else it }
    if (idx >= 0) {
        val left = line.substring(0, idx).trim()
        val right = line.substring(idx + 1).trim()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = left,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                fontSize = fontSize,
                fontWeight = fontWeight
            )
            Text(
                text = "–",
                modifier = Modifier.padding(horizontal = 4.dp),
                fontSize = fontSize,
                fontWeight = fontWeight
            )
            Text(
                text = right,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontSize = fontSize,
                fontWeight = fontWeight
            )
        }
    } else {
        Text(
            text = line,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = fontSize,
            fontWeight = fontWeight ?: FontWeight.Normal
        )
    }
}

@Composable
private fun BottomSetsArea(
    st: ScoreUiState,
    modifier: Modifier = Modifier
) {
    val sidePad = 12.dp
    val bottomSafe = 12.dp

    val tokens = remember(st.setSummary) {
        st.setSummary.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    val hasLiveSet = (st.leftGames != 0 || st.rightGames != 0 || st.inTieBreak)
    val finishedTokens = if (!st.finished && hasLiveSet && tokens.isNotEmpty()) {
        tokens.dropLast(1)
    } else tokens

    val liveLine = "${st.leftGames}–${st.rightGames}" +
            if (st.inTieBreak) " (${st.tbLeft}:${st.tbRight})" else ""

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = sidePad, end = sidePad, bottom = bottomSafe),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        finishedTokens.forEach { raw ->
            CenteredDashLine(raw.replace('-', '–'), fontSize = 14.sp)
        }
        if (!st.finished && hasLiveSet) {
            CenteredDashLine(liveLine, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- MAIN MATCH SCREEN (with icon controls) ----------

@Composable
fun MainScreen(vm: ScoreViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsState()

    fun fmtPoint(p: Int) = when (p) {
        0 -> "0"; 15 -> "15"; 30 -> "30"; 40 -> "40"; 50 -> "Ad"; 100 -> "Game"; else -> p.toString()
    }

    Scaffold(timeText = { TimeText() }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxH = this.maxHeight
            val sidePad = 12.dp
            val bigPoints = if (maxH < 220.dp) 36.sp else 44.sp

            // 1) tappable halves
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .background(Color(0xFF0F9D58))
                        .clickable(enabled = !st.finished) { vm.addPointLeft() }
                )
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .background(Color(0xFF4285F4))
                        .clickable(enabled = !st.finished) { vm.addPointRight() }
                )
            }

            // vertical splitter
            Box(
                Modifier.align(Alignment.Center)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.25f))
            )

            // 2) team labels
            Row(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = -(bigPoints.value.dp * 1.2f))
                    .fillMaxWidth()
                    .padding(horizontal = sidePad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (st.serverLeft) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Yellow))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = st.leftName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .widthIn(max = 96.dp)
                        )
                    }
                }
                Box(Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = st.rightName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .widthIn(max = 96.dp)
                        )
                        if (!st.serverLeft) {
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Yellow))
                        }
                    }
                }
            }

            // 3) center big points or winner banner
            if (st.finished) {
                Text(
                    text = "${inferWinnerName(st)} wins the match",
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = sidePad),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (maxH < 220.dp) 16.sp else 18.sp,
                    lineHeight = 20.sp
                )
            } else {
                Row(
                    Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = sidePad),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (st.inTieBreak) st.tbLeft.toString() else fmtPoint(st.leftPoints),
                        fontSize = bigPoints,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text("vs", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    Text(
                        text = if (st.inTieBreak) st.tbRight.toString() else fmtPoint(st.rightPoints),
                        fontSize = bigPoints,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4) per-set rows
            BottomSetsArea(st, modifier = Modifier.align(Alignment.BottomCenter))

            // --------- ICONS (TOP with clock, BOTTOM above bezel) ---------

            val iconSize = 20.dp
            val hitSize = 36.dp

            // TOP ICONS — next to the clock
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 2.dp)      // sits right by the TimeText
                    .zIndex(3f)
            ) {
                Row(
                    modifier = Modifier.width(100.dp), // how tightly they hug the clock
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-left = Home (Main Menu)
                    SimpleIconButton(
                        onClick = onBack,
                        painter = painterResource(R.drawable.ic_home),
                        contentDesc = "Main Menu",
                        iconSize = 20.dp,
                        hitSize = 36.dp
                    )
                    // Top-right = Save & Exit
                    SimpleIconButton(
                        onClick = {
                            vm.endMatchAndSave()
                            onBack()
                        },
                        painter = painterResource(R.drawable.ic_save_exit),
                        contentDesc = "Save & Exit",
                        iconSize = 20.dp,
                        hitSize = 36.dp
                    )
                }
            }



            // BOTTOM ICONS — away from the corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, bottom = 22.dp)
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bottom-left = Toggle Server
                    SimpleIconButton(
                        onClick = { vm.toggleServer() },
                        painter = painterResource(R.drawable.ic_toggle_server),
                        contentDesc = "Toggle Server",
                        iconSize = 20.dp,
                        hitSize = 36.dp
                    )
                    // Bottom-right = Undo/Reset
                    SimpleIconButton(
                        onClick = { vm.reset() }, // replace with vm.undo() when implemented
                        painter = painterResource(R.drawable.ic_undo),
                        contentDesc = "Undo",
                        iconSize = 20.dp,
                        hitSize = 36.dp
                    )
                }
            }

        }
    }
}

@Composable
private fun inferWinnerName(st: ScoreUiState): String =
    if (st.leftSets > st.rightSets) st.leftName else st.rightName

@Composable
private fun SimpleIconButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDesc: String,
    iconSize: Dp = 20.dp,
    hitSize: Dp = 36.dp
) {
    Box(
        modifier = Modifier
            .size(hitSize)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDesc,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

// (kept from your earlier file; used by some buttons/text)
@Composable
private fun AutoResizeText(
    text: String,
    maxFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 9.sp,
    fontWeight: FontWeight? = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    var size by remember(text) { mutableStateOf(maxFontSize) }
    Text(
        text = text,
        fontSize = size,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        fontWeight = fontWeight,
        modifier = modifier,
        onTextLayout = { r ->
            if (r.didOverflowWidth && size > minFontSize) {
                size = (size.value - 1f).coerceAtLeast(minFontSize.value).sp
            }
        }
    )
}
