package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatMessageEntity
import com.example.viewmodel.SslcViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslcAppScreen(viewModel: SslcViewModel, onNavigateBack: () -> Unit = {}) {
    val messages by viewModel.allMessages.collectAsStateWithLifecycle()
    val bookmarkedMessages by viewModel.bookmarkedMessages.collectAsStateWithLifecycle()
    val currentSubject by viewModel.currentSubject.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorType by viewModel.errorType.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Robot Chat, 1 = Bookmarks

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🤖",
                                    fontSize = 22.sp
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "SSLC AI ROBOT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "Kerala State Syllabus Companion",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != {}) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (activeTab == 0 && messages.size > 1) {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Selector (Chat vs Bookmarks)
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Robot Chat", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_chat")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Saved Notes (${bookmarkedMessages.size})", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_bookmarks")
                )
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    slideInHorizontally(initialOffsetX = { if (targetState > initialState) it else -it }) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { if (targetState > initialState) -it else it }) + fadeOut()
                },
                modifier = Modifier.weight(1f),
                label = "MainContentTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> {
                        // CHAT VIEW
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Subject Horizontal Selection Row
                            SubjectQuickSelectRow(
                                selectedSubject = currentSubject,
                                onSubjectSelected = { viewModel.onSubjectChanged(it) }
                            )

                            ModeSelectionRow(
                                currentMode = viewModel.currentMode,
                                onModeChanged = { viewModel.updateMode(it) }
                            )

                            // Error Bar
                            if (errorType != null) {
                                ErrorBanner(
                                    errorType = errorType,
                                    onDismiss = { viewModel.dismissError() }
                                )
                            }

                            // Chat Messages List
                            Box(modifier = Modifier.weight(1f)) {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(messages) { message ->
                                        ChatMessageBubble(
                                            message = message,
                                            viewModel = viewModel
                                        )
                                    }
                                }

                                if (isLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                                Text(
                                                    "Robot is thinking...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Quick suggestion chips based on selected subject
                            QuickSuggestionRow(
                                selectedSubject = currentSubject,
                                onSuggestedClick = { prompt ->
                                    viewModel.onInputTextChanged(prompt)
                                }
                            )

                            // Input Box Area
                            InputSection(
                                text = viewModel.inputText,
                                onTextChanged = { viewModel.onInputTextChanged(it) },
                                onSend = { viewModel.sendMessage() },
                                currentSubject = currentSubject,
                                isLoading = isLoading
                            )
                        }
                    }
                    1 -> {
                        // SAVED / BOOKMARKED NOTES VIEW
                        SavedNotesList(
                            notes = bookmarkedMessages,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectQuickSelectRow(
    selectedSubject: String,
    onSubjectSelected: (String) -> Unit
) {
    val subjects = listOf("General", "Mathematics", "Science", "Social Science", "English", "Malayalam")
    val icons = listOf(
        Icons.Default.School,
        Icons.Default.Calculate,
        Icons.Default.Science,
        Icons.Default.Public,
        Icons.Default.Translate,
        Icons.Default.HistoryEdu
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(subjects.size) { index ->
            val subName = subjects[index]
            val subIcon = icons[index]
            val isSelected = selectedSubject == subName

            FilterChip(
                selected = isSelected,
                onClick = { onSubjectSelected(subName) },
                label = { Text(subName, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = subIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("subject_chip_$subName")
            )
        }
    }
}

@Composable
fun QuickSuggestionRow(
    selectedSubject: String,
    onSuggestedClick: (String) -> Unit
) {
    val options = when (selectedSubject) {
        "Mathematics" -> listOf("Explain Arithmetic Sequences", "What is Tangents formula?", "Solve: x² - 5x + 6 = 0")
        "Science" -> listOf("How AC generator works", "State Ohm's Law with formula", "What is raw materials of Photosynthesis?")
        "Social Science" -> listOf("Wayanad landslides facts", "What is the Revolt of 1857?", "Kerala Renaissance leaders list")
        "English" -> listOf("Explain active and passive voice", "Format of an Official Letter", "Identify figure of speech")
        "Malayalam" -> listOf("ലഘു വിവരണം: കാവ്യഭംഗി", "സംവർണ്ണ പദങ്ങൾ എന്നാൽ എന്ത്?", "വാക്യപ്രയോഗം ശരിയാക്കുക")
        else -> listOf("What is SSLC Exam?", "Give me a study plan", "Tension relief tips")
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { opt ->
            SuggestionChip(
                onClick = { onSuggestedClick(opt) },
                label = { Text(opt, fontSize = 12.sp, maxLines = 1) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("suggestion_chip")
            )
        }
    }
}

@Composable
fun ErrorBanner(
    errorType: String?,
    onDismiss: () -> Unit
) {
    Surface(
        color = if (errorType == "APIKEY_MISSING") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (errorType == "APIKEY_MISSING") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = if (errorType == "APIKEY_MISSING") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                if (errorType == "APIKEY_MISSING") {
                    Text(
                        "Gemini API Key is Missing!",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "Please go to Google AI Studio's Secrets Manager, add the key 'GEMINI_API_KEY' with your valid token, then reload.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                } else {
                    Text(
                        "API network alert",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        errorType ?: "Unknown response error occurred",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = if (errorType == "APIKEY_MISSING") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    viewModel: SslcViewModel
) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Top)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🤖", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                // USER MESSAGE BUBBLE
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = getSubjectIcon(message.subject),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = message.subject,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.userQuery,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                // ROBOT STRUCTURED EDUCATIONAL ANSWER
                RobotLessonCard(
                    message = message,
                    viewModel = viewModel,
                    isBookmarkView = false
                )
            }

            // Message timestamp & quick delete tag on long tap
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier
                        .size(12.dp)
                        .clickable { viewModel.deleteMessage(message.id) },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Top)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🧑‍🎓", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun RobotLessonCard(
    message: ChatMessageEntity,
    viewModel: SslcViewModel,
    isBookmarkView: Boolean
) {
    var isRawExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Subject Tag & Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = getSubjectIcon(message.subject),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = message.subject,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Bookmark Toggle
                IconButton(
                    onClick = { viewModel.toggleBookmark(message.id, !message.isBookmarked) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("bookmark_button_${message.id}")
                ) {
                    Icon(
                        imageVector = if (message.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Lesson",
                        tint = if (message.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (message.isCasual) {
                Text(
                    text = message.shortExplanation,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 20.sp
                )
            } else {
                if (message.mode == "Tutor") {
                    // --- TUTOR MODE UI ---
                    message.topicName?.let { topic ->
                        Text(
                            text = "📖 Topic: $topic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    SectionExpandable(title = "1. Easy Explanation / ലഘുവിവരണം", content = message.shortExplanation, color = MaterialTheme.colorScheme.primary)
                    SectionExpandable(title = "2. Detailed Explanation / വിശദീകരണം", content = message.detailedExplanation, color = MaterialTheme.colorScheme.secondary)
                    
                    message.realLifeExample?.let {
                        SectionExpandable(title = "3. Real-Life Example / ജീവിത ഉദാഹരണം", content = it, color = MaterialTheme.colorScheme.tertiary)
                    }
                    
                    val points = viewModel.getPointsList(message)
                    if (points.isNotEmpty()) {
                        BulletsSection(title = "4. Important Points / പ്രധാന കാര്യങ്ങൾ", points = points, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    val examQ = viewModel.getExamQuestionsList(message)
                    if (examQ.isNotEmpty()) {
                        BulletsSection(title = "5. Common Exam Questions / പരീക്ഷാ ചോദ്യങ്ങൾ", points = examQ, color = MaterialTheme.colorScheme.error)
                    }
                    
                    message.revisionNotes?.let {
                        SectionExpandable(title = "6. Quick Revision Notes / റിവിഷൻ നോട്ട്", content = it, color = MaterialTheme.colorScheme.secondary)
                    }
                    
                    message.practiceQuestion?.let {
                        SectionExpandable(title = "7. Practice Question / പരിശീലന ചോദ്യം", content = it, color = MaterialTheme.colorScheme.tertiary)
                    }
                    
                } else if (message.mode == "Quiz") {
                    // --- QUIZ MODE UI ---
                    message.fullQuizTopic?.let { topic ->
                        Text(
                            text = "📝 Topic: $topic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    val mcqs = viewModel.getMcqQuestionsList(message)
                    val shortAns = viewModel.getShortAnswerQuestionsList(message)
                    val longAns = viewModel.getLongAnswerQuestionsList(message)
                    val summary = viewModel.getQuizSummary(message)

                    if (mcqs.isNotEmpty()) {
                        Text(
                            text = "Section A: Multiple Choice Questions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                        )
                        mcqs.forEachIndexed { index, q ->
                            QuizQuestionCard(
                                index = index + 1,
                                question = q.question,
                                options = q.options ?: emptyList(),
                                answer = q.correctAnswer ?: "",
                                explanation = q.explanation,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    if (shortAns.isNotEmpty()) {
                        Text(
                            text = "Section B: Short Answer Questions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                        )
                        shortAns.forEachIndexed { index, q ->
                            QuizModelAnswerCard(
                                index = index + 1,
                                question = q.question,
                                answer = q.answer ?: "",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    if (longAns.isNotEmpty()) {
                        Text(
                            text = "Section C: Long Answer Questions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                        )
                        longAns.forEachIndexed { index, q ->
                            QuizModelAnswerCard(
                                index = index + 1,
                                question = q.question,
                                answer = q.modelAnswer ?: "",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (summary != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("📊 Quiz Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Total Questions: ${summary.totalQuestions}", style = MaterialTheme.typography.bodyMedium)
                                Text("Difficulty: ${summary.difficultyLevel}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Key Concepts:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                summary.keyConceptsCovered.forEach { concept ->
                                    Text("• $concept", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                } else {
                    // --- ASSISTANT MODE UI ---
                    SectionExpandable(title = "1. Short Explanation / ലഘുവിവരണം", content = message.shortExplanation, color = MaterialTheme.colorScheme.primary)
                    SectionExpandable(title = "2. Lessons & Proof / വിശദമായ വിവരണം", content = message.detailedExplanation, color = MaterialTheme.colorScheme.secondary)
                    
                    val points = viewModel.getPointsList(message)
                    if (points.isNotEmpty()) {
                        BulletsSection(title = "3. Study Facts / പ്രധാനപ്പെട്ട കാര്യങ്ങൾ", points = points, color = MaterialTheme.colorScheme.tertiary)
                    }
                    
                    if (message.examTip.isNotEmpty()) {
                        ExamTipWidget(message.examTip)
                    }
                    
                    // AI interactive Quiz Section!
                    if (!message.quizQuestion.isNullOrEmpty()) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        InteractiveQuizWidget(
                            messageId = message.id,
                            question = message.quizQuestion,
                            options = viewModel.getQuizOptionsList(message),
                            correctIndex = message.quizCorrectIndex ?: 0,
                            explanation = message.quizExplanation ?: "",
                            selectedAnswerIndex = viewModel.quizAnswers[message.id],
                            onAnswerSelected = { index ->
                                viewModel.submitQuizAnswer(message.id, index)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveQuizWidget(
    messageId: Long,
    question: String,
    options: List<String>,
    correctIndex: Int,
    explanation: String,
    selectedAnswerIndex: Int?,
    onAnswerSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Quiz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Would you like a quiz on this topic?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        val hasSelected = selectedAnswerIndex != null

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { idx, opt ->
                val isSelectedOpt = selectedAnswerIndex == idx
                val isCorrectOpt = idx == correctIndex

                val cardBgColor = when {
                    !hasSelected -> MaterialTheme.colorScheme.surface
                    isSelectedOpt && isCorrectOpt -> Color(0xFFE8F5E9) // Success green tint
                    isSelectedOpt && !isCorrectOpt -> Color(0xFFFFEBEE) // Failure red tint
                    isCorrectOpt -> Color(0xFFE8F5E9) // Show correct answer as green anyway
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                }

                val borderStrokeColor = when {
                    !hasSelected -> MaterialTheme.colorScheme.outlineVariant
                    isSelectedOpt && isCorrectOpt -> Color(0xFF4CAF50)
                    isSelectedOpt && !isCorrectOpt -> Color(0xFFF44336)
                    isCorrectOpt -> Color(0xFF4CAF50)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                }

                Surface(
                    color = cardBgColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !hasSelected) {
                            onAnswerSelected(idx)
                        }
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(8.dp))
                        .testTag("quiz_option_${messageId}_$idx")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = isSelectedOpt,
                            onClick = { if (!hasSelected) onAnswerSelected(idx) },
                            enabled = !hasSelected,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = if (isCorrectOpt) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        )
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelectedOpt) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )

                        // Helper Icon indicators
                        if (hasSelected) {
                            if (isCorrectOpt) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = Color(0xFF4CAF50))
                            } else if (isSelectedOpt) {
                                Icon(Icons.Default.Cancel, contentDescription = "Incorrect", tint = Color(0xFFF44336))
                            }
                        }
                    }
                }
            }
        }

        if (hasSelected) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    val answeredCorrectly = selectedAnswerIndex == correctIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = if (answeredCorrectly) "🎉 Correct Answer!" else "❌ Try again in next search!",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (answeredCorrectly) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                    Text(
                        text = "Explanation: $explanation",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InputSection(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    currentSubject: String,
    isLoading: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val placeholderText = when (currentSubject) {
        "Mathematics" -> "Solve: 8th term of AP 3, 5, 7..."
        "Science" -> "Ask: Ohm's law, photosynthesis..."
        "Social Science" -> "Ask: Important points on Revolt of 1857..."
        "English" -> "Ask: Passive voice of 'She wrote a book'..."
        "Malayalam" -> "മലയാളം വ്യാകരണം അല്ലെങ്കിൽ ചോദ്യങ്ങൾ..."
        else -> "Ask SSLC Robot anything..."
    }

    Surface(
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = { Text(placeholderText, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("chat_input_text_field"),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank() && !isLoading) {
                            onSend()
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }
                ),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            FloatingActionButton(
                onClick = {
                    if (text.isNotBlank() && !isLoading) {
                        onSend()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SavedNotesList(
    notes: List<ChatMessageEntity>,
    viewModel: SslcViewModel
) {
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Text(
                    text = "No saved lessons yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "When reading explanations with SSLC Robot, tap the bookmark icon at the top of the answer card to save it here for reference!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = notes,
                key = { it.id }
            ) { note ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Student asked: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = if (note.userQuery.isNotEmpty()) "\"${note.userQuery}\"" else "Greetings Introduction",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    RobotLessonCard(
                        message = note,
                        viewModel = viewModel,
                        isBookmarkView = true
                    )
                }
            }
        }
    }
}

fun getSubjectIcon(subject: String): ImageVector {
    return when (subject) {
        "Mathematics" -> Icons.Default.Calculate
        "Science" -> Icons.Default.Science
        "Social Science" -> Icons.Default.Public
        "English" -> Icons.Default.Translate
        "Malayalam" -> Icons.Default.HistoryEdu
        else -> Icons.Default.School
    }
}

@Composable
fun ModeSelectionRow(currentMode: String, onModeChanged: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.height(40.dp)
        ) {
            Row(modifier = Modifier.padding(2.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (currentMode == "Assistant") MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (currentMode == "Assistant") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onModeChanged("Assistant") }.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🤖", fontSize = 14.sp)
                        Text("Assistant", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (currentMode == "Tutor") MaterialTheme.colorScheme.secondary else Color.Transparent,
                    contentColor = if (currentMode == "Tutor") MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onModeChanged("Tutor") }.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("👨‍🏫", fontSize = 14.sp)
                        Text("AI Tutor", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (currentMode == "Quiz") MaterialTheme.colorScheme.tertiary else Color.Transparent,
                    contentColor = if (currentMode == "Quiz") MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onModeChanged("Quiz") }.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("✍️", fontSize = 14.sp)
                        Text("Quiz", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionExpandable(title: String, content: String, color: Color) {
    if (content.isBlank()) return
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(10.dp),
            lineHeight = 20.sp
        )
    }
}

@Composable
fun BulletsSection(title: String, points: List<String>, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        points.forEach { pt ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text("✨", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp, top = 2.dp))
                Text(text = pt, style = MaterialTheme.typography.bodyMedium, lineHeight = 18.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun QuizQuestionCard(index: Int, question: String, options: List<String>, answer: String, explanation: String?, color: Color) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("Q$index. $question", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            options.forEach { opt ->
                Text("○ $opt", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Correct Answer: $answer", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            if (!explanation.isNullOrBlank()) {
                Text("Reason: $explanation", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun QuizModelAnswerCard(index: Int, question: String, answer: String, color: Color) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("Q$index. $question", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Answer:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = color)
            Text(answer, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ExamTipWidget(tip: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "4. Pareeksha Exam Tip / പരീക്ഷാ ടിപ്പ്",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )
        }
    }
}
