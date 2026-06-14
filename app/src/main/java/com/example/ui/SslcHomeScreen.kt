package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslcHomeScreen(viewModel: com.example.viewmodel.SslcViewModel, onFeatureClick: (String) -> Unit) {
    val currentLanguage = viewModel.language
    val onLanguageToggle = { viewModel.toggleLanguage() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentLanguage == "English") "SSLC AI ROBOT" else "SSLC AI റോബോട്ട്", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(onClick = onLanguageToggle) {
                        Text(
                            text = if (currentLanguage == "English") "EN" else "ML",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onFeatureClick("Settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        val features = listOf(
            FeatureItem(if (currentLanguage == "English") "Ask AI" else "AI ചോദിക്കുക", Icons.Default.ChatBubble, MaterialTheme.colorScheme.primary, "Ask AI"),
            FeatureItem(if (currentLanguage == "English") "AI Tutor" else "AI ട്യൂട്ടർ", Icons.Default.School, MaterialTheme.colorScheme.secondary, "AI Tutor"),
            FeatureItem(if (currentLanguage == "English") "Quiz Generator" else "ക്വിസ്", Icons.Default.Quiz, MaterialTheme.colorScheme.tertiary, "Quiz Generator"),
            FeatureItem(if (currentLanguage == "English") "Study Planner" else "പഠന പ്ലാനർ", Icons.Default.EventNote, MaterialTheme.colorScheme.error, "Study Planner"),
            FeatureItem(if (currentLanguage == "English") "Revision Notes" else "റിവിഷൻ", Icons.Default.LibraryBooks, MaterialTheme.colorScheme.primary, "Revision Notes"),
            FeatureItem(if (currentLanguage == "English") "Progress" else "പുരോഗതി", Icons.Default.QueryStats, MaterialTheme.colorScheme.secondary, "Progress Dashboard")
        )

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome to SSLC AI ROBOT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Developed By MUHAMMAD SHAAN K P", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            PomodoroTimerWidget(viewModel = viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(features) { feature ->
                    FeatureCard(feature = feature, onClick = { onFeatureClick(feature.internalName) })
                }
            }
        }
    }
}

data class FeatureItem(
    val name: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val internalName: String
)

@Composable
fun PomodoroTimerWidget(viewModel: com.example.viewmodel.SslcViewModel) {
    androidx.compose.runtime.LaunchedEffect(viewModel.isTimerRunning) {
        if (viewModel.isTimerRunning) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                viewModel.tickTimer()
            }
        }
    }

    val timeLeft = viewModel.timerTimeLeft
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeFormatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pomodoro Timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    TextButton(onClick = { viewModel.switchTimerType("Focus") }) {
                        Text("Focus", color = if (viewModel.timerType == "Focus") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { viewModel.switchTimerType("Break") }) {
                        Text("Break", color = if (viewModel.timerType == "Break") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(timeFormatted, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { viewModel.toggleTimer() }) {
                    Text(if (viewModel.isTimerRunning) "Pause" else "Start")
                }
                OutlinedButton(onClick = { viewModel.resetTimer() }) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
fun FeatureCard(feature: FeatureItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = feature.color.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.name,
                    tint = feature.color,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = feature.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
