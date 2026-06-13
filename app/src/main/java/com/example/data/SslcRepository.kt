package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SslcRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.chatMessageDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val responseAdapter = moshi.adapter(SslcResponseJson::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
    )
    private val quizQuestionListAdapter = moshi.adapter<List<QuizQuestionJson>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, QuizQuestionJson::class.java)
    )
    private val quizSummaryAdapter = moshi.adapter(QuizSummaryJson::class.java)

    val allMessages: Flow<List<ChatMessageEntity>> = dao.getAllMessagesFlow()
    val bookmarkedMessages: Flow<List<ChatMessageEntity>> = dao.getBookmarkedMessagesFlow()

    suspend fun insertMessage(message: ChatMessageEntity): Long = withContext(Dispatchers.IO) {
        dao.insertMessage(message)
    }

    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateBookmark(id, isBookmarked)
    }

    suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteMessageById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearChatHistory()
    }

    suspend fun askSslcRobot(query: String, subject: String, currentMode: String): ChatMessageEntity = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("APIKEY_MISSING")
        }

        val systemInstructionText = when (currentMode) {
            "Tutor" -> {
                """
                You are an expert Kerala SSLC Tutor. Your job is to teach students step-by-step.
                
                Teaching Rules:
                - Break lessons into small parts.
                - Teach from basic to advanced.
                - Use simple language and examples from daily life.
                - Focus on understanding, not memorization.
                - Encourage questions.
                - Reply in the language the student used (English or Malayalam).
                
                Subject Guidelines:
                - Mathematics: Show complete solutions. Explain why each step is performed.
                - Science: Connect concepts with real-world examples.
                - Social Science: Highlight dates, events, and keywords.
                - Languages: Explain grammar and writing techniques.
                
                Response Structure Requirements (JSON ONLY):
                - "topicName": string, Topic Name
                - "easyExplanation": string, Easy Explanation of the concept
                - "detailedExplanation": string, Detailed Explanation / Step-by-step
                - "realLifeExample": string, Real-Life Example
                - "importantPoints": array of strings, Important Points
                - "examQuestions": array of strings, Common Exam Questions
                - "revisionNotes": string, Quick Revision Notes
                - "practiceQuestion": string, Practice Question
                - "isCasual": boolean, true ONLY if the student's input is a casual greeting
                """.trimIndent()
            }
            "Quiz" -> {
                """
                You are an SSLC Exam Quiz Generator.
                Generate high-quality quizzes based on Kerala SSLC syllabus.
                
                Rules:
                - Questions must be suitable for Class 10.
                - Include easy, medium, and hard questions.
                - Provide correct answers and explanations.
                - Reply in the language the student used (English or Malayalam).
                
                Response Structure Requirements (JSON ONLY):
                - "fullQuizTopic": string, Topic Name
                - "mcqQuestions": array of objects, containing "question" (string), "options" (array of exactly 4 strings), "correctAnswer" (string), "explanation" (string). Recommend 5 items.
                - "shortAnswerQuestions": array of objects, containing "question" (string), "answer" (string). Recommend 5 items.
                - "longAnswerQuestions": array of objects, containing "question" (string), "modelAnswer" (string). Recommend 3 items.
                - "quizSummary": object containing "totalQuestions" (int), "difficultyLevel" (string), "keyConceptsCovered" (array of strings).
                - "isCasual": boolean, true ONLY if the student's input is a casual greeting
                """.trimIndent()
            }
            else -> {
                """
                You are SSLC AI ROBOT, an intelligent educational assistant for Kerala SSLC students (Class 10).
                
                Core Rules:
                1. Answer in simple, student-friendly language. Avoid unnecessarily difficult vocabulary.
                2. Support English and Malayalam. Explain concepts clearly.
                3. Reply in the same language used by the student's query.
                4. Give highly accurate, exam-focused, and educational responses.
                
                Subject Guidelines:
                - Mathematics: Show every single step, explain formulas, break down calculations clearly.
                - Science: Explain concepts using relatable, real-life examples.
                - Social Science: Highlight important points and Kerala board exam-specific facts.
                - English & Malayalam: Explain meanings, grammar, writing skills, or comprehension.
                
                Response Structure Requirements (JSON ONLY):
                - "shortExplanation": string, clear, simple summary (3-4 sentences)
                - "detailedExplanation": string, Detailed explanation
                - "importantPoints": array of strings, 3-5 high-value summary bullets
                - "examTip": string, strict practical exam advice
                - "quizQuestion": string (Optional), A single multiple-choice question
                - "quizOptions": array of 4 strings (Optional)
                - "quizCorrectOptionIndex": int 0 to 3 (Optional)
                - "quizExplanation": string (Optional)
                - "isCasual": boolean, true ONLY if casual greeting
                """.trimIndent()
            }
        }

        // Prepare the payload
        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "Subject/Context: $subject. Student says: $query")))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.5f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            var jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from AI Robot")

            Log.d("SslcRepository", "Response: $jsonText")

            // Sanitization: sometimes models wrap json in ```json ... ```
            if (jsonText.startsWith("```")) {
                val lines = jsonText.lines()
                val cleanLines = lines.filterNot { it.startsWith("```") }
                jsonText = cleanLines.joinToString("\n")
            }
            jsonText = jsonText.trim()

            val parsedResponse = responseAdapter.fromJson(jsonText)
                ?: throw Exception("Failed to parse AI response")

            val pointsJson = stringListAdapter.toJson(parsedResponse.importantPoints ?: emptyList())
            val optionsJson = parsedResponse.quizOptions?.let { stringListAdapter.toJson(it) }
            val examQJson = parsedResponse.examQuestions?.let { stringListAdapter.toJson(it) } ?: "[]"
            val mcqJson = parsedResponse.mcqQuestions?.let { quizQuestionListAdapter.toJson(it) } ?: "[]"
            val shortQJson = parsedResponse.shortAnswerQuestions?.let { quizQuestionListAdapter.toJson(it) } ?: "[]"
            val longQJson = parsedResponse.longAnswerQuestions?.let { quizQuestionListAdapter.toJson(it) } ?: "[]"
            val summaryJson = parsedResponse.quizSummary?.let { quizSummaryAdapter.toJson(it) }

            ChatMessageEntity(
                sender = "robot",
                userQuery = query,
                isCasual = parsedResponse.isCasual,
                subject = subject,
                mode = currentMode,
                shortExplanation = parsedResponse.shortExplanation ?: parsedResponse.easyExplanation ?: "",
                detailedExplanation = parsedResponse.detailedExplanation ?: "",
                importantPointsJson = pointsJson,
                examTip = parsedResponse.examTip ?: "",
                quizQuestion = parsedResponse.quizQuestion,
                quizOptionsJson = optionsJson,
                quizCorrectIndex = parsedResponse.quizCorrectOptionIndex,
                quizExplanation = parsedResponse.quizExplanation,
                topicName = parsedResponse.topicName,
                realLifeExample = parsedResponse.realLifeExample,
                examQuestionsJson = examQJson,
                revisionNotes = parsedResponse.revisionNotes,
                practiceQuestion = parsedResponse.practiceQuestion,
                fullQuizTopic = parsedResponse.fullQuizTopic,
                mcqQuestionsJson = mcqJson,
                shortAnswerQuestionsJson = shortQJson,
                longAnswerQuestionsJson = longQJson,
                quizSummaryJson = summaryJson,
                isBookmarked = false
            )
        } catch (e: Exception) {
            Log.e("SslcRepository", "API Error", e)
            throw e
        }
    }

    fun parseStringList(json: String): List<String> {
        return try {
            stringListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseQuizQuestionList(json: String): List<QuizQuestionJson> {
        if (json.isEmpty() || json == "[]") return emptyList()
        return try {
            quizQuestionListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseQuizSummary(json: String): QuizSummaryJson? {
        if (json.isEmpty()) return null
        return try {
            quizSummaryAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
