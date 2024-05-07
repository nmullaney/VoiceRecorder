package com.osunick.voicerecorder.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.osunick.voicerecorder.R
import com.osunick.voicerecorder.model.VoiceMessage
import com.osunick.voicerecorder.ui.theme.Typography
import com.osunick.voicerecorder.ui.theme.VoiceRecorderTheme
import com.osunick.voicerecorder.viewmodel.LogEvent
import com.osunick.voicerecorder.viewmodel.LogsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Composable
fun VRScaffold(
    uiState: StateFlow<LogsUiState>,
    messageFlow: Flow<PagingData<VoiceMessage>>,
    eventsFlow: MutableStateFlow<LogEvent>
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { VRTopAppBar(eventsFlow) },
        bottomBar = { VRAddLogBar(uiState, eventsFlow) },
        floatingActionButton = { VRFab(uiState, eventsFlow) }
    ) { innerPadding ->
        VoiceLogList(
            messageFlow,
            Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun VoiceLogList(messageFlow: Flow<PagingData<VoiceMessage>>, modifier: Modifier = Modifier) {
    val lazyPagerItems = messageFlow.collectAsLazyPagingItems()
    Box(modifier = modifier
        .fillMaxWidth()
        .fillMaxHeight()) {


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .align(alignment = Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
            reverseLayout = true
        ) {
            items(count = lazyPagerItems.itemCount) { index ->
                lazyPagerItems[index]?.let { message ->

                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(
                            modifier = Modifier,
                            text = formatDateTime(message.dateTime),
                            color = Color.Gray,
                            style = Typography.labelMedium
                        )
                        Text(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            text = message.text,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = Typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VRTopAppBar(eventsFlow: MutableStateFlow<LogEvent>) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        title = { Text(stringResource(id = R.string.voice_logs)) },
        actions = {
            ShareActionBarButton(eventsFlow)
        })
}

@Composable
fun ShareActionBarButton(eventsFlow: MutableStateFlow<LogEvent>) {
    val coroutineScope = rememberCoroutineScope()
    IconButton(onClick = {
        coroutineScope.launch {
            eventsFlow.emit(LogEvent.Share)
        }
    }) {
        Icon(Icons.Filled.Share, stringResource(id = R.string.share))
    }
}


@Composable
fun VRAddLogBar(uiState: StateFlow<LogsUiState>, eventsFlow: MutableStateFlow<LogEvent>) {
    val coroutineScope = rememberCoroutineScope()
    val messageState = uiState.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextField(
            messageState.value.currentMessage ?: "",
            onValueChange = {
                coroutineScope.launch {
                    eventsFlow.emit(LogEvent.UpdateLog(it))
                }
            },
            modifier = Modifier.weight(1f)
        )
        Button(
            {
                coroutineScope.launch {
                    eventsFlow.emit(LogEvent.Save)
                }
            },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .height(48.dp)
                .aspectRatio(1f),
            colors = ButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContentColor = disabledColor(MaterialTheme.colorScheme.onPrimary),
                disabledContainerColor = disabledColor(MaterialTheme.colorScheme.primary)
            ),
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Filled.Add, stringResource(id = R.string.add_log))
        }
    }
}

@Composable
fun VRFab(uiState: StateFlow<LogsUiState>, eventsFlow: MutableStateFlow<LogEvent>) {
    val coroutineScope = rememberCoroutineScope()
    val state = uiState.collectAsState()
    FloatingActionButton(
        onClick = {
            if (!state.value.isRecording) {
                coroutineScope.launch {
                    eventsFlow.emit(LogEvent.StartRecording)
                }
            }
        },

    ) {
        if (state.value.isRecording) {
            CircularProgressIndicator()
        } else {
            Icon(
                painter = painterResource(id = R.drawable.baseline_voice_24),
                contentDescription = stringResource(id = R.string.add_voice_log),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun formatDateTime(localDateTime: LocalDateTime): String =
    localDateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))

fun disabledColor(color: Color): Color =
    color.copy(alpha = 0.5f)


@Preview(widthDp = 320, heightDp = 640)
@Composable
fun VRAppPreview() {
    VoiceRecorderTheme {
        VRScaffold(
            uiState = MutableStateFlow(
                LogsUiState(
                    "current message",
                    false
                )
            ),
            messageFlow = flowOf(
                PagingData.from(
                    listOf(
                        VoiceMessage("Hello", LocalDateTime.now()),
                        VoiceMessage("There", LocalDateTime.now()),
                        VoiceMessage(
                            "Let's try a super long message too, to see what it looks like",
                            LocalDateTime.now()
                        )
                    )
                )
            ),
            eventsFlow = MutableStateFlow(LogEvent.None)
        )
    }
}