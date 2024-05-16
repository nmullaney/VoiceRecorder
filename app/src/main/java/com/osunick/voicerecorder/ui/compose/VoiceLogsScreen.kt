package com.osunick.voicerecorder.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.osunick.voicerecorder.R
import com.osunick.voicerecorder.date.DateTimeConstants
import com.osunick.voicerecorder.model.LogLabels
import com.osunick.voicerecorder.model.VoiceMessage
import com.osunick.voicerecorder.ui.theme.Typography
import com.osunick.voicerecorder.ui.theme.VoiceRecorderTheme
import com.osunick.voicerecorder.ui.theme.onPrimaryContainerLight
import com.osunick.voicerecorder.ui.theme.onSurfaceVariantLight
import com.osunick.voicerecorder.ui.theme.primaryContainerLight
import com.osunick.voicerecorder.viewmodel.LogEvent
import com.osunick.voicerecorder.viewmodel.LogsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime


@Composable
fun VRScaffold(
    uiState: StateFlow<LogsUiState>,
    messageFlow: Flow<PagingData<VoiceMessage>>,
    labelsFlow: StateFlow<LogLabels>,
    eventsFlow: MutableStateFlow<LogEvent>
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { VRTopAppBar(eventsFlow) },
        bottomBar = { VRAddLogBar(uiState, eventsFlow) },
        floatingActionButton = {  }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val labels = labelsFlow.collectAsState()
            LogLabelSelector(
                labelsFlow,
                eventsFlow
            )
            VoiceLogList(
                messageFlow,
                eventsFlow
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogLabelSelector(
    labelsFlow: StateFlow<LogLabels>,
    eventsFlow: MutableStateFlow<LogEvent>,
    modifier: Modifier = Modifier
) {
    val labels = labelsFlow.collectAsState()
    val allLabels = labels.value.allLabels
    val selectedLabel = labels.value.selectedLabel
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var currentSelection by remember {
        mutableStateOf(
            selectedLabel.ifEmpty { context.getString(R.string.all) })
    }
    var typingValue by remember { mutableStateOf("") }
    val isAllSelected by remember { derivedStateOf { currentSelection == context.getString(R.string.all) } }
    var isTextFieldFocused by remember {
        mutableStateOf(false)
    }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(expanded) {
        if (!expanded) {
            focusManager.clearFocus(true)
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        ExposedDropdownMenuBox(
            modifier = Modifier.padding(4.dp),
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .onFocusChanged {
                        isTextFieldFocused = it.isFocused
                        typingValue = ""
                    },
                value = if (isTextFieldFocused) typingValue else currentSelection,
                onValueChange = {
                    typingValue = it
                },
                label = { Text(stringResource(id = R.string.label)) }
            )

            ExposedDropdownMenu(
                // Max height prevents the TextField from being covered
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .exposedDropdownSize(matchTextFieldWidth = true),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.all)) },
                    onClick = {
                        coroutineScope.launch {
                            eventsFlow.emit(LogEvent.SelectAllLabels)
                        }
                        currentSelection = context.getString(R.string.all)
                        expanded = false
                    }
                )
                allLabels.filter { it.isNotBlank() && it.startsWith(typingValue) }.forEach { label ->
                    DropdownMenuItem(
                        text = { Text(text = label) },
                        onClick = {
                            coroutineScope.launch {
                                eventsFlow.emit(LogEvent.SelectLabel(label))
                            }
                            currentSelection = label
                            expanded = false
                        }
                    )
                }
                if (typingValue.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text(context.getString(R.string.add_label, typingValue)) },
                        onClick = {
                            coroutineScope.launch {
                                eventsFlow.emit(LogEvent.CreateLabel(typingValue))
                            }
                            currentSelection = typingValue
                            expanded = false
                        }
                    )
                }
                if (!isAllSelected && typingValue.isNotBlank()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                context.getString(R.string.rename_label, selectedLabel, typingValue)
                            )
                        },
                        onClick = {
                            coroutineScope.launch {
                                eventsFlow.emit(
                                    LogEvent.RenameLabel(selectedLabel, typingValue)
                                )
                                currentSelection = typingValue
                                expanded = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceLogList(
    messageFlow: Flow<PagingData<VoiceMessage>>,
    eventsFlow: MutableStateFlow<LogEvent>,
    modifier: Modifier = Modifier
) {
    val lazyPagerItems = messageFlow.collectAsLazyPagingItems()
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .align(alignment = Alignment.BottomCenter)
                .padding(vertical = 8.dp),
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
                            color = onSurfaceVariantLight,
                            style = Typography.labelMedium
                        )
                        Text(
                            modifier = Modifier
                                .background(
                                    color = primaryContainerLight,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        message.id?.let {
                                            eventsFlow.emit(LogEvent.DeleteLog(it))
                                        }
                                    }
                                },
                            text = message.text,
                            color = onPrimaryContainerLight,
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
            DeleteActionBarButton(eventsFlow)
            ShareActionBarButton(eventsFlow)
        })
}

@Composable
fun DeleteActionBarButton(eventsFlow: MutableStateFlow<LogEvent>) {
    val coroutineScope = rememberCoroutineScope()
    IconButton(onClick = {
        coroutineScope.launch {
            eventsFlow.emit(LogEvent.DeleteAllLogs)
        }
    }) {
        Icon(Icons.Filled.Delete, stringResource(id = R.string.deleteall))
    }
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

    val requester = remember { FocusRequester() }
    Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 10.dp)
    )
    {
        OutlinedTextField(
            messageState.value.currentMessage ?: "",
            onValueChange = {
                coroutineScope.launch {
                    eventsFlow.emit(LogEvent.UpdateLog(it))
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                coroutineScope.launch {
                    eventsFlow.emit(LogEvent.Save)
                }
            }),
            modifier = Modifier
                .weight(1f)
                .padding(start = 0.dp, end=10.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_ENTER) {
                        coroutineScope.launch {
                            eventsFlow.emit(LogEvent.Save)
                        }
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                }
                .focusRequester(requester)
                .focusable(),
            shape = RoundedCornerShape(40.dp)

        )
        IconButton(
            onClick = {
                if (!messageState.value.isRecording) {
                    coroutineScope.launch {
                        eventsFlow.emit(LogEvent.StartRecording)
                    }
                }
            },
            Modifier
                .background(MaterialTheme.colorScheme.secondary, CircleShape)
        ) {
            if (messageState.value.isRecording) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_talking_24),
                    contentDescription = stringResource(id = R.string.recording),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_voice_24),
                    contentDescription = stringResource(id = R.string.add_voice_log),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
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
        containerColor = MaterialTheme.colorScheme.secondary
    ) {
        if (state.value.isRecording) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_talking_24),
                contentDescription = stringResource(id = R.string.recording),
                tint = MaterialTheme.colorScheme.onSecondary
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.baseline_voice_24),
                contentDescription = stringResource(id = R.string.add_voice_log),
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

fun formatDateTime(zonedDateTime: ZonedDateTime): String =
    zonedDateTime
        .withZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeConstants.PrettyDateFormatter)


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
                        VoiceMessage(text = "Hello", dateTime = ZonedDateTime.now()),
                        VoiceMessage(text = "There", dateTime = ZonedDateTime.now()),
                        VoiceMessage(
                            text = "Let's try a super long message too, to see what it looks like",
                            dateTime = ZonedDateTime.now()
                        )
                    )
                )
            ),
            labelsFlow = MutableStateFlow(
                LogLabels(
                    "Selected",
                    listOf("Selected", "Not Selected")
                )
            ),
            eventsFlow = MutableStateFlow(LogEvent.None)
        )
    }
}