package com.domedemok.travelplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.components.LocalIsOnline
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.viewmodel.AiChatViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AiChatScreen(
    tripId: String,
    modifier: Modifier = Modifier,
    viewModel: AiChatViewModel = koinViewModel()
) {
    val s = LocalStrings.current
    val isOnline = LocalIsOnline.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(tripId) {
        viewModel.loadMessages(tripId)
    }

    // Index 0 maps to the bottom of the list (LazyColumn uses reverseLayout) —
    // auto-scrolls to the newest bubble or to the typing indicator while the AI works.
    LaunchedEffect(uiState.messages.size, uiState.isAiThinking) {
        if (uiState.messages.isNotEmpty() || uiState.isAiThinking) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Messages list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading && uiState.messages.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.messages.isEmpty() && !uiState.isAiThinking -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(AppSpacing.xxxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    ) {
                        Surface(
                            shape    = AppShape.xl,
                            color    = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector        = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier           = Modifier.size(40.dp),
                                    tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Text(
                            text  = s.aiChatTitle,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text  = s.aiChatSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                else -> {
                    // reverseLayout puts the newest item at the bottom (index 0).
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Typing indicator. Listed first so reverseLayout
                        // renders it at the very bottom of the conversation.
                        if (uiState.isAiThinking) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 16.dp
                                        ),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.widthIn(min = 60.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            items = uiState.messages.reversed(),
                            key   = { it.id },
                        ) { message ->
                            MessageBubble(
                                message      = message,
                                isOwnMessage = message.senderId == uiState.currentUserId,
                            )
                        }
                    }
                }
            }

            // Error snackbar at the bottom of the screen
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(s.generalDismiss)
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }

        // Reuses the MessageInput composable from ChatScreen.kt.
        // `isSending = isAiThinking` disables the Send button while a response is in flight.
        MessageInput(
            text         = uiState.inputText,
            onTextChange = { viewModel.updateInputText(it) },
            onSend       = { viewModel.sendAiMessage(tripId) },
            isSending    = uiState.isAiThinking,
            isOnline     = isOnline,
        )
    }
}