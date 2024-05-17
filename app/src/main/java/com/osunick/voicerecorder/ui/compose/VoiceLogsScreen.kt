package com.osunick.voicerecorder.ui.compose

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
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
import com.osunick.voicerecorder.viewmodel.NavEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun VoiceMessageListDetailScaffold(
    uiState: StateFlow<LogsUiState>,
    messageFlow: Flow<PagingData<VoiceMessage>>,
    labelsFlow: StateFlow<LogLabels>,
    eventsFlow: MutableStateFlow<LogEvent>,
    navEventsFlow: SharedFlow<NavEvent>
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<VoiceMessage>()

    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    LaunchedEffect(navigator) {
        navEventsFlow.collectLatest {
            if (it == NavEvent.Back) {
                navigator.navigateBack()
            }
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                LogListScaffold(
                    uiState = uiState,
                    messageFlow = messageFlow,
                    labelsFlow = labelsFlow,
                    eventsFlow = eventsFlow,
                    navigator = navigator
                )
            }
        },
        detailPane = {
            AnimatedPane {
                EditLogScaffold(
                    labelsFlow = labelsFlow,
                    eventsFlow = eventsFlow,
                    navigator = navigator)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun LogListScaffold(
    uiState: StateFlow<LogsUiState>,
    messageFlow: Flow<PagingData<VoiceMessage>>,
    labelsFlow: StateFlow<LogLabels>,
    eventsFlow: MutableStateFlow<LogEvent>,
    navigator: ThreePaneScaffoldNavigator<VoiceMessage>
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LogListAppBar(eventsFlow) },
        bottomBar = { AddLogBar(uiState, eventsFlow) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            LogLabelSelector(
                labelsFlow,
                eventsFlow
            )
            VoiceLogList(
                messageFlow,
                onItemClick = { item ->
                    // Navigate to the detail pane with the passed item
                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EditLogScaffold(
    labelsFlow: StateFlow<LogLabels>,
    eventsFlow: MutableStateFlow<LogEvent>,
    navigator: ThreePaneScaffoldNavigator<VoiceMessage>
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { EditLogTopBar(navigator, eventsFlow) }
    ) { innerPadding ->
        navigator.currentDestination?.content?.let {
            EditVoiceLog(it, labelsFlow, eventsFlow, Modifier.padding(innerPadding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun topAppBarColors() =
    TopAppBarDefaults.mediumTopAppBarColors(
    containerColor = MaterialTheme.colorScheme.primary,
    titleContentColor = MaterialTheme.colorScheme.onPrimary,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EditLogTopBar(navigator: ThreePaneScaffoldNavigator<VoiceMessage>, eventsFlow: MutableStateFlow<LogEvent>) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        colors = topAppBarColors(),
        title = { Text(stringResource(id = R.string.edit_log)) },
        actions = {
            navigator.currentDestination?.content?.id?.let {
                DeleteLogActionBarButton(it, eventsFlow)
            }
        })
}

@Composable
fun DeleteLogActionBarButton(id: Int, eventsFlow: MutableStateFlow<LogEvent>) {
    val coroutineScope = rememberCoroutineScope()
    IconButton(onClick = {
        coroutineScope.launch {
            eventsFlow.emit(LogEvent.DeleteLog(id))
        }
    }) {
        Icon(Icons.Filled.Delete, stringResource(id = R.string.deleteall))
    }
}

@Composable
fun LogLabelSelector(
    labelsFlow: StateFlow<LogLabels>,
    eventsFlow: MutableStateFlow<LogEvent>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val labels = labelsFlow.collectAsState()
    val allLabels = labels.value.allLabels
    val selectedLabel = labels.value.selectedLabel
    val context = LocalContext.current
    val initialSelection = selectedLabel.ifEmpty { context.getString(R.string.all) }
    val labelSelectionEventFlow = MutableStateFlow<LabelSelectEvent>(LabelSelectEvent.None)
    LaunchedEffect(labelSelectionEventFlow) {
        labelSelectionEventFlow.collect {
            when (it) {
                LabelSelectEvent.SelectAll ->
                    coroutineScope.launch {
                        eventsFlow.emit(LogEvent.SelectAllLabels)
                    }
                is LabelSelectEvent.CreateLabel ->
                    coroutineScope.launch {
                        eventsFlow.emit(LogEvent.CreateLabel(it.label))
                    }
                is LabelSelectEvent.RenameLabel ->
                    coroutineScope.launch {
                        eventsFlow.emit(LogEvent.RenameLabel(it.oldLabel, it.newLabel))
                    }
                is LabelSelectEvent.SelectLabel ->
                    coroutineScope.launch {
                        eventsFlow.emit(LogEvent.SelectLabel(it.label))
                    }
                LabelSelectEvent.None -> {}
            }
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        LabelSelectionDropDown(
            allLabels,
            initialSelection,
            labelSelectionEventFlow,
            includeAll = true,
            includeCreate = true,
            includeRename = true,
        )
    }
}

sealed interface LabelSelectEvent {
    data object None : LabelSelectEvent
    data object SelectAll : LabelSelectEvent
    data class SelectLabel(val label: String): LabelSelectEvent
    data class CreateLabel(val label: String): LabelSelectEvent
    data class RenameLabel(val oldLabel: String, val newLabel: String): LabelSelectEvent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSelectionDropDown(
    allLabels: List<String> = listOf(),
    initialSelection: String = "",
    labelSelectEventsFlow: MutableStateFlow<LabelSelectEvent>,
    includeAll: Boolean = false,
    includeCreate: Boolean = false,
    includeRename: Boolean = false,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(expanded) {
        if (!expanded) {
            focusManager.clearFocus(true)
        }
    }
    var isTextFieldFocused by remember {
        mutableStateOf(false)
    }
    var typingValue by remember { mutableStateOf("") }
    var currentSelection by remember { mutableStateOf(initialSelection) }
    val isAllSelected by remember { derivedStateOf { currentSelection == context.getString(R.string.all) } }
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
            if (includeAll) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.all)) },
                    onClick = {
                        coroutineScope.launch {
                            labelSelectEventsFlow.emit(LabelSelectEvent.SelectAll)
                        }
                        currentSelection = context.getString(R.string.all)
                        expanded = false
                    }
                )
            }
            allLabels.filter { it.isNotBlank() && it.startsWith(typingValue) }.forEach { label ->
                DropdownMenuItem(
                    text = { Text(text = label) },
                    onClick = {
                        coroutineScope.launch {
                            labelSelectEventsFlow.emit(LabelSelectEvent.SelectLabel(label))
                        }
                        currentSelection = label
                        expanded = false
                    }
                )
            }
            if (includeCreate && typingValue.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text(context.getString(R.string.add_label, typingValue)) },
                    onClick = {
                        coroutineScope.launch {
                            labelSelectEventsFlow.emit(LabelSelectEvent.CreateLabel(typingValue))
                        }
                        currentSelection = typingValue
                        expanded = false
                    }
                )
            }
            if (includeRename && !isAllSelected && typingValue.isNotBlank()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            context.getString(R.string.rename_label, initialSelection, typingValue)
                        )
                    },
                    onClick = {
                        coroutineScope.launch {
                            labelSelectEventsFlow.emit(
                                LabelSelectEvent.RenameLabel(initialSelection, typingValue)
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

@Composable
fun EditVoiceLog(
    message: VoiceMessage,
    labelsFlow: StateFlow<LogLabels>,
    eventsFlow: MutableStateFlow<LogEvent>,
    modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    var editedText by remember { mutableStateOf(message.text) }
    var editedLabel by remember { mutableStateOf(message.label) }
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start) {
        Text(
            text = formatDateTime(message.dateTime),
            color = onSurfaceVariantLight,
            style = Typography.labelMedium
        )
        OutlinedTextField(
            value = editedText,
            onValueChange = { newText ->
                editedText = newText
            },
            shape = RoundedCornerShape(40.dp)
        )
        val labelState = labelsFlow.collectAsState()
        val labelSelectEventsFlow = remember { MutableStateFlow<LabelSelectEvent>(LabelSelectEvent.None) }
        LaunchedEffect(labelSelectEventsFlow) {
            labelSelectEventsFlow.collectLatest {
                when(it) {
                    is LabelSelectEvent.SelectLabel -> {
                        editedLabel = it.label
                    }
                    is LabelSelectEvent.CreateLabel -> {
                        editedLabel = it.label
                    }
                    else -> {}

                }
            }
        }
        LabelSelectionDropDown(
            labelState.value.allLabels,
            message.label ?: "",
            labelSelectEventsFlow,
            includeAll = false,
            includeCreate = true,
            includeRename = false,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    eventsFlow.emit(LogEvent.UpdateLog(
                        message.copy(text = editedText, label = editedLabel))
                    )
                }
            }
        ) {
            Text(stringResource(id = R.string.update))
        }
    }
}

@Composable
fun VoiceLogList(
    messageFlow: Flow<PagingData<VoiceMessage>>,
    onItemClick: (VoiceMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    var previousDate: LocalDate? = null
    val lazyPagerItems = messageFlow.collectAsLazyPagingItems()
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

                    Row(modifier = Modifier.padding(4.dp)) {
                        Column() {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 0.dp)
                                ,
                                text = formatDate(message.dateTime),
                                color = onSurfaceVariantLight,
                                style = Typography.labelMedium
                            )
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                ,
                                text = formatTime(message.dateTime),
                                color = onSurfaceVariantLight,
                                style = Typography.labelMedium
                            )
                        }


                        Text(
                            modifier = Modifier
                                .background(
                                    color = primaryContainerLight,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable {
                                    onItemClick(message)
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
fun LogListAppBar(eventsFlow: MutableStateFlow<LogEvent>) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        colors = topAppBarColors(),
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
fun AddLogBar(uiState: StateFlow<LogsUiState>, eventsFlow: MutableStateFlow<LogEvent>) {
    val coroutineScope = rememberCoroutineScope()
    val messageState = uiState.collectAsState()

    val requester = remember { FocusRequester() }
    Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 12.dp)
    )
    {
        OutlinedTextField(
            messageState.value.currentMessage ?: "",
            onValueChange = {
                coroutineScope.launch {
                    eventsFlow.emit(LogEvent.UpdateLogMessage(it))
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
                .padding(start = 0.dp, end = 10.dp)
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


fun formatDateTime(zonedDateTime: ZonedDateTime): String =
    zonedDateTime
        .withZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeConstants.PrettyDateFormatter)

fun formatTime(zonedDateTime: ZonedDateTime): String =
    zonedDateTime
        .withZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

fun formatDate(zonedDateTime: ZonedDateTime): String =
    zonedDateTime
        .withZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))


@Preview(widthDp = 320, heightDp = 640)
@Composable
fun VRAppPreview() {
    VoiceRecorderTheme {
        VoiceMessageListDetailScaffold(
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
            eventsFlow = MutableStateFlow(LogEvent.None),
            navEventsFlow = MutableSharedFlow()
        )
    }
}