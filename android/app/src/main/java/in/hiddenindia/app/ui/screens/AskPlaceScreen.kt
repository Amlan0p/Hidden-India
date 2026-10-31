package `in`.hiddenindia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.hiddenindia.app.data.ChatHistoryRepository
import `in`.hiddenindia.app.data.LocalConversation
import `in`.hiddenindia.app.data.MessageSender
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.LightTertiary
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel

private val ALL_INDIA_SUGGESTIONS = listOf(
    "What should I see in Jaipur?",
    "Tell me about street food in Kolkata.",
    "What are some hidden places in Kerala?",
    "What should I do in Port Blair?",
    "What are the most tranquil stepwells across India?"
)

/**
 * Port of web src/pages/AskThePlacePage.tsx. This client only keeps one flat chat stream
 * (see MainViewModel.chatMessages) rather than the web app's per-destination conversation
 * history store, so "History" is a stub for now — the destination selector and "New Chat"
 * are real and wired to actual view-model state.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AskPlaceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val destination by viewModel.selectedDestination.collectAsState()
    val allDestinations by viewModel.allDestinations.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    val chatError by viewModel.chatError.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val currentConvId by viewModel.currentConversationId.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var lastQuestion by remember { mutableStateOf("") }
    var locationMenuOpen by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<LocalConversation?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val density = LocalDensity.current
    val isImeOpen = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(messages.size, isImeOpen) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val suggestions = if (destination != null) {
        listOf(
            "What's the hidden legend of ${destination?.name}?",
            "Where can I eat traditional food under ₹300 in ${destination?.name}?",
            "What should I visit before sunset in ${destination?.name}?",
            "What local customs and etiquette should I know?",
            "I only have 3 hours. What should I do in ${destination?.name}?"
        )
    } else ALL_INDIA_SUGGESTIONS

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        // Sticky sub-bar: Back, History, Location selector, New Chat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("btn_ask_place_back")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.loadChatHistory()
                            showHistorySheet = true
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("btn_chat_history"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(" History", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                }

                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable { locationMenuOpen = true }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("chat_location_selector"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = destination?.let { "${it.name}, ${it.state}" } ?: "\uD83C\uDDEE\uD83C\uDDF3 All India",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(
                        expanded = locationMenuOpen,
                        onDismissRequest = { locationMenuOpen = false },
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("\uD83C\uDDEE\uD83C\uDDF3 All India (Pan-India Travel)") },
                            onClick = { viewModel.selectedDestination.value = null; locationMenuOpen = false }
                        )
                        allDestinations.forEach { dest ->
                            DropdownMenuItem(
                                text = { Text("${dest.name}, ${dest.state}") },
                                onClick = { viewModel.selectedDestination.value = dest; locationMenuOpen = false }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { viewModel.startNewChat() }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                Text(" New Chat", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1, softWrap = false)
            }
        }

        // Conversation title
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = destination?.let { "${it.name}, ${it.state}" } ?: "India Travel & Heritage Assistant",
                fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = destination?.tagline?.ifBlank { null } ?: "Ask about architecture, regional culinary traditions, stepwells, or verified route tips.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 2.dp)
            )
        }

        // Chat Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender == MessageSender.USER
                Column(
                    modifier = Modifier.fillMaxWidth(0.88f),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    if (!isUser) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                            Text("  Guide AI", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp, topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                )
                            )
                            .background(if (isUser) LightTertiary else MaterialTheme.colorScheme.surface)
                            .then(if (!isUser) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)) else Modifier)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (isUser) msg.text else remember(msg.text) { cleanChatText(msg.text) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 21.sp
                        )
                    }
                }
            }

            if (messages.size <= 2) {
                item {
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(50))
                                    .clickable(enabled = !isLoading) {
                                        lastQuestion = chip
                                        viewModel.sendChatMessage(chip)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(chip, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("chat_typing_indicator"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            Text("  Thinking…", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (chatError != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(14.dp)
                    ) {
                        Text(chatError ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        if (lastQuestion.isNotBlank()) {
                            Text(
                                "Retry",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable { viewModel.sendChatMessage(lastQuestion) }
                                    .testTag("btn_retry_chat")
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isLoading) {
                        val chip = suggestions.random()
                        lastQuestion = chip
                        viewModel.sendChatMessage(chip)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add suggested prompt", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }

            Box(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    enabled = !isLoading,
                    textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().testTag("chat_input_field")
                )
                if (inputText.isEmpty()) {
                    Text(
                        text = destination?.let { "Ask about ${it.name}..." } ?: "Ask about any place, food, or history in India...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            val canSend = inputText.isNotBlank() && !isLoading
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = canSend) {
                        val text = inputText
                        inputText = ""
                        lastQuestion = text
                        viewModel.sendChatMessage(text)
                    }
                    .testTag("btn_send_chat"),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showHistorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Conversation History",
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AI travel conversations on this device",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showHistorySheet = false },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (chatHistory.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No conversations yet",
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your AI travel conversations will appear here.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatHistory, key = { it.id }) { conv ->
                            val isCurrent = conv.id == currentConvId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        viewModel.selectConversation(conv.id)
                                        showHistorySheet = false
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isCurrent) {
                                            Text(
                                                text = "ACTIVE • ",
                                                fontFamily = JetBrainsMono,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = conv.title,
                                            fontFamily = PlayfairDisplay,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = conv.destinationName,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${ChatHistoryRepository.formatTimestamp(conv.updatedAt)} · ${conv.messages.size} messages",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { conversationToDelete = conv },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (conversationToDelete != null) {
        val target = conversationToDelete!!
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = {
                Text(
                    text = "Delete Conversation?",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${target.title}\"? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(target.id)
                        conversationToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * The chat bubble shows plain text, so strip any markdown a model may still emit
 * (**bold**, *italic*, # headings, `code`, links) and normalise list markers to "• ".
 */
internal fun cleanChatText(input: String): String {
    var t = input.replace("\r", "")
    t = Regex("```[a-zA-Z]*\\n?([\\s\\S]*?)```").replace(t, "$1")
    t = Regex("(?m)^\\s{0,3}#{1,6}\\s*").replace(t, "")
    t = Regex("!\\[[^\\]]*\\]\\([^)]*\\)").replace(t, "")
    t = Regex("\\[([^\\]]+)\\]\\([^)]*\\)").replace(t, "$1")
    t = Regex("\\*\\*\\*([^*\\n]+?)\\*\\*\\*").replace(t, "$1")
    t = Regex("\\*\\*([^*\\n]+?)\\*\\*").replace(t, "$1")
    t = Regex("__([^_\\n]+?)__").replace(t, "$1")
    t = Regex("(^|[\\s(\\[])\\*(?!\\s)([^*\\n]+?)\\*(?=[\\s).,;:!?\\]]|$)", RegexOption.MULTILINE).replace(t, "$1$2")
    t = Regex("`([^`\\n]+)`").replace(t, "$1")
    t = Regex("(?m)^\\s*[-*+\u2022]\\s+").replace(t, "\u2022 ")
    t = Regex("(?m)^\\s*[-_*]{3,}\\s*$").replace(t, "")
    t = Regex("(?m)^\\s*>\\s?").replace(t, "")
    t = t.replace(Regex("\\*{2,}"), "")
    t = Regex("\\n{3,}").replace(t, "\n\n")
    return t.trim()
}
