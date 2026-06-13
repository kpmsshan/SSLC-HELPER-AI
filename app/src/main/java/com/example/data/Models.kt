package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuizQuestionJson(
    @Json(name = "question") val question: String = "",
    @Json(name = "options") val options: List<String>? = null,
    @Json(name = "correctAnswer") val correctAnswer: String? = null,
    @Json(name = "explanation") val explanation: String? = null,
    @Json(name = "answer") val answer: String? = null,
    @Json(name = "modelAnswer") val modelAnswer: String? = null
)

@JsonClass(generateAdapter = true)
data class QuizSummaryJson(
    @Json(name = "totalQuestions") val totalQuestions: Int = 0,
    @Json(name = "difficultyLevel") val difficultyLevel: String = "",
    @Json(name = "keyConceptsCovered") val keyConceptsCovered: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SslcResponseJson(
    @Json(name = "isCasual") val isCasual: Boolean = false,
    
    // Assistant Mode properties
    @Json(name = "shortExplanation") val shortExplanation: String? = null,
    @Json(name = "examTip") val examTip: String? = null,
    @Json(name = "quizQuestion") val quizQuestion: String? = null,
    @Json(name = "quizOptions") val quizOptions: List<String>? = null,
    @Json(name = "quizCorrectOptionIndex") val quizCorrectOptionIndex: Int? = null,
    @Json(name = "quizExplanation") val quizExplanation: String? = null,

    // Tutor Mode properties
    @Json(name = "topicName") val topicName: String? = null,
    @Json(name = "easyExplanation") val easyExplanation: String? = null,
    @Json(name = "realLifeExample") val realLifeExample: String? = null,
    @Json(name = "examQuestions") val examQuestions: List<String>? = null,
    @Json(name = "revisionNotes") val revisionNotes: String? = null,
    @Json(name = "practiceQuestion") val practiceQuestion: String? = null,

    // Quiz Generator properties
    @Json(name = "fullQuizTopic") val fullQuizTopic: String? = null,
    @Json(name = "mcqQuestions") val mcqQuestions: List<QuizQuestionJson>? = null,
    @Json(name = "shortAnswerQuestions") val shortAnswerQuestions: List<QuizQuestionJson>? = null,
    @Json(name = "longAnswerQuestions") val longAnswerQuestions: List<QuizQuestionJson>? = null,
    @Json(name = "quizSummary") val quizSummary: QuizSummaryJson? = null,

    // Shared properties
    @Json(name = "detailedExplanation") val detailedExplanation: String? = null,
    @Json(name = "importantPoints") val importantPoints: List<String>? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sender: String, // "user" or "robot"
    val timestamp: Long = System.currentTimeMillis(),
    val userQuery: String,
    val isCasual: Boolean = false,
    val subject: String = "General",
    val isBookmarked: Boolean = false,
    val mode: String = "Assistant", // "Assistant", "Tutor", or "Quiz"
    
    // Shared / Mapped properties
    val shortExplanation: String = "", 
    val detailedExplanation: String = "",
    val importantPointsJson: String = "[]",
    
    // Assistant specific properties
    val examTip: String = "",
    val quizQuestion: String? = null,
    val quizOptionsJson: String? = null,
    val quizCorrectIndex: Int? = null,
    val quizExplanation: String? = null,

    // Tutor specific properties
    val topicName: String? = null,
    val realLifeExample: String? = null,
    val examQuestionsJson: String = "[]",
    val revisionNotes: String? = null,
    val practiceQuestion: String? = null,

    // Quiz specific properties
    val fullQuizTopic: String? = null,
    val mcqQuestionsJson: String = "[]",
    val shortAnswerQuestionsJson: String = "[]",
    val longAnswerQuestionsJson: String = "[]",
    val quizSummaryJson: String? = null
)
