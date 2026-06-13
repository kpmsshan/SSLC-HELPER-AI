package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessageEntity
import com.example.data.SslcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SslcViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SslcRepository(application)

    // UI state streams
    val allMessages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookmarkedMessages: StateFlow<List<ChatMessageEntity>> = repository.bookmarkedMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current subject state flow
    private val _currentSubject = MutableStateFlow("General")
    val currentSubject: StateFlow<String> = _currentSubject.asStateFlow()

    // State for inputs
    var inputText by mutableStateOf("")
        private set

    // Loading & error state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorType = MutableStateFlow<String?>(null)
    val errorType: StateFlow<String?> = _errorType.asStateFlow()

    // Key is message id, value is student selected option index
    val quizAnswers = mutableStateMapOf<Long, Int>()

    var currentMode by mutableStateOf("Assistant")
        private set

    var language by mutableStateOf("English")
        private set

    fun updateMode(mode: String) {
        currentMode = mode
    }

    fun toggleLanguage() {
        language = if (language == "English") "Malayalam" else "English"
    }

    init {
        // Automatically insert a warm, custom Malayalam/English greetings welcome from SSLC Robot if empty
        viewModelScope.launch {
            repository.allMessages.collect { list ->
                if (list.isEmpty()) {
                    val welcomeMsg = ChatMessageEntity(
                        sender = "robot",
                        userQuery = "",
                        isCasual = true,
                        mode = "Assistant",
                        shortExplanation = "Hi, I am SSLC AI ROBOT! 🤖\n\nI am your companion helper to study and excel in your Kerala SSLC exams! Select a subject below or ask me any question. I can reply in English and Malayalam!",
                        detailedExplanation = "Change to Tutor Mode 👨‍🏫 for step-by-step teaching, or use Assistant Mode 🤖 for quick exam tips!",
                        importantPointsJson = "[\"Support for Malayalam & English\",\"Mathematics formulas & steps explained\",\"Science concepts with live examples\",\"Interactive multiple choice quiz for testing yourself\"]",
                        examTip = "Check out the Exam Tip section on every topic to learn exactly what questions are frequently asked by the Pareeksha Bhavan!",
                        subject = "General"
                    )
                    repository.insertMessage(welcomeMsg)
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        inputText = text
    }

    fun onSubjectChanged(subject: String) {
        _currentSubject.value = subject
    }

    fun sendMessage() {
        val query = inputText.trim()
        if (query.isEmpty() || _isLoading.value) return

        inputText = ""
        _isLoading.value = true
        _errorType.value = null

        viewModelScope.launch {
            try {
                // 1. Insert User Query Message
                val userMsg = ChatMessageEntity(
                    sender = "user",
                    userQuery = query,
                    subject = _currentSubject.value
                )
                repository.insertMessage(userMsg)

                // 2. Fetch Reply from SSLC AI Robot
                val robotMsg = repository.askSslcRobot(query, _currentSubject.value, currentMode, language)
                repository.insertMessage(robotMsg)
            } catch (e: IllegalStateException) {
                if (e.message == "APIKEY_MISSING") {
                    _errorType.value = "APIKEY_MISSING"
                } else {
                    _errorType.value = "ERROR: ${e.localizedMessage ?: "Unknown network error"}"
                }
            } catch (e: Exception) {
                _errorType.value = e.localizedMessage ?: "Something went wrong! Check your internet connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleBookmark(id: Long, isBookmarked: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(id, isBookmarked)
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun submitQuizAnswer(messageId: Long, selectedIndex: Int) {
        quizAnswers[messageId] = selectedIndex
    }

    fun dismissError() {
        _errorType.value = null
    }

    fun getPointsList(message: ChatMessageEntity): List<String> {
        return repository.parseStringList(message.importantPointsJson)
    }

    fun getQuizOptionsList(message: ChatMessageEntity): List<String> {
        val json = message.quizOptionsJson ?: return emptyList()
        return repository.parseStringList(json)
    }

    fun getExamQuestionsList(message: ChatMessageEntity): List<String> {
        return repository.parseStringList(message.examQuestionsJson)
    }

    fun getMcqQuestionsList(message: ChatMessageEntity): List<com.example.data.QuizQuestionJson> {
        return repository.parseQuizQuestionList(message.mcqQuestionsJson)
    }

    fun getShortAnswerQuestionsList(message: ChatMessageEntity): List<com.example.data.QuizQuestionJson> {
        return repository.parseQuizQuestionList(message.shortAnswerQuestionsJson)
    }

    fun getLongAnswerQuestionsList(message: ChatMessageEntity): List<com.example.data.QuizQuestionJson> {
        return repository.parseQuizQuestionList(message.longAnswerQuestionsJson)
    }

    fun getQuizSummary(message: ChatMessageEntity): com.example.data.QuizSummaryJson? {
        return message.quizSummaryJson?.let { repository.parseQuizSummary(it) }
    }
}
