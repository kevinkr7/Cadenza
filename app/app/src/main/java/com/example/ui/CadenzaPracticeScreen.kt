package com.example.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.CadenzaAnalysisResponse
import com.example.models.CadenzaCompareResponse
import com.example.models.PitchTrackPoint
import com.example.viewmodel.CadenzaPracticeViewModel
import com.example.viewmodel.PracticeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadenzaPracticeScreen(
    onNavigateBack: () -> Unit,
    viewModel: CadenzaPracticeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadReference(it) }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practice Session") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.state == PracticeState.COUNTDOWN || uiState.state == PracticeState.RECORDING) {
                ImmersiveRecordingLayout(
                    state = uiState.state,
                    countdownValue = uiState.countdownValue,
                    onStopClick = { viewModel.stopRecordingAndAnalyze() }
                )
            } else {
                PracticeHeader()

                uiState.referenceFilename?.let {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Selected Song:", fontWeight = FontWeight.Bold)
                            Text(it)
                        }
                    }
                }

                if (uiState.state == PracticeState.IDLE || uiState.state == PracticeState.RESULTS) {
                    Button(
                        onClick = { filePickerLauncher.launch("audio/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Reference Song")
                    }
                }

                StatusCard(
                    statusMessage = uiState.statusMessage,
                    state = uiState.state,
                    countdownValue = uiState.countdownValue
                )

                RecordingControls(
                    state = uiState.state,
                    onRecordClick = {
                        if (viewModel.hasRecordingPermission()) {
                            viewModel.startRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopClick = {
                        viewModel.stopRecordingAndAnalyze()
                    },
                    onClearClick = {
                        viewModel.clearResult()
                    }
                )

                uiState.errorMessage?.let { error ->
                    ErrorCard(errorMessage = error)
                }

                uiState.recordedFilePath?.let { path ->
                    RecordedFileCard(filePath = path)
                }

                uiState.compareResult?.let { result ->
                    CompareResultSection(result = result)
                }
            }
        }
    }
}

@Composable
private fun ImmersiveRecordingLayout(
    state: PracticeState,
    countdownValue: Int,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state == PracticeState.COUNTDOWN) {
            Text(
                text = "$countdownValue",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Get Ready...",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else if (state == PracticeState.RECORDING) {
            Text(
                text = "Recording Active",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp).height(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onStopClick,
                modifier = Modifier.width(200.dp).height(56.dp)
            ) {
                Text("Stop Recording", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun PracticeHeader() {
    Column {
        Text(
            text = "Practice Mode",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Record your voice and let Cadenza analyze your pitch, stability, and practice type.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatusCard(
    statusMessage: String,
    state: PracticeState,
    countdownValue: Int = 3
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge
            )

            if (state == PracticeState.ANALYZING || state == PracticeState.SEPARATING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (state == PracticeState.SEPARATING) "Extracting vocals..." else "Sending audio to backend...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingControls(
    state: PracticeState,
    onRecordClick: () -> Unit,
    onStopClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Voice Recording",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRecordClick,
                    enabled = state == PracticeState.READY_TO_RECORD,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Record")
                }

                Button(
                    onClick = onStopClick,
                    enabled = state == PracticeState.RECORDING,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Stop")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onClearClick,
                enabled = state == PracticeState.RESULTS,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Clear Result")
            }
        }
    }
}

@Composable
private fun ErrorCard(
    errorMessage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RecordedFileCard(
    filePath: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recorded File",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = filePath,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CompareResultSection(
    result: CadenzaCompareResponse
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CompareSummaryCard(result = result)

        if (result.feedback.isNotEmpty()) {
            FeedbackListCard(feedbackList = result.feedback, title = "Coach Feedback")
        }

        ComparisonPitchGraphCard(
            referenceCurve = result.reference_curve,
            userCurve = result.pitch_curve
        )
    }
}

@Composable
private fun CompareSummaryCard(
    result: CadenzaCompareResponse
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Result",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Score: ${result.overall_score}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Tier: ${result.tier.uppercase()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(label = "Pitch Accuracy", value = "${result.pitch_accuracy}%")
            DetailRow(label = "Timing Accuracy", value = "${result.timing_accuracy}%")
            DetailRow(label = "Stability", value = "${result.stability}%")
        }
    }
}

@Composable
private fun FeedbackListCard(
    feedbackList: List<String>,
    title: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            feedbackList.forEach { feedback ->
                Text(
                    text = "• $feedback",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ComparisonPitchGraphCard(
    referenceCurve: List<PitchTrackPoint>,
    userCurve: List<PitchTrackPoint>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pitch Alignment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(12.dp).height(4.dp).padding(end = 4.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) { drawRect(androidx.compose.ui.graphics.Color.Cyan) }
                    }
                    Text("Reference", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(12.dp).height(4.dp).padding(end = 4.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) { drawRect(androidx.compose.ui.graphics.Color(0xFFFFA500)) } // Orange
                    }
                    Text("Your Voice", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (referenceCurve.isEmpty() && userCurve.isEmpty()) {
                Text(
                    text = "No pitch graph data available.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                ComparisonPitchGraph(
                    referenceCurve = referenceCurve,
                    userCurve = userCurve,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            }
        }
    }
}

@Composable
private fun ComparisonPitchGraph(
    referenceCurve: List<PitchTrackPoint>,
    userCurve: List<PitchTrackPoint>,
    modifier: Modifier = Modifier
) {
    val validRef = referenceCurve.filter { it.frequency != null && it.time != null }
    val validUser = userCurve.filter { it.frequency != null && it.time != null }

    if (validRef.isEmpty() && validUser.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "Not enough pitch points to draw graph.")
        }
        return
    }

    val allPoints = validRef + validUser
    val minTime = allPoints.minOf { it.time ?: 0.0 }
    val maxTime = allPoints.maxOf { it.time ?: 0.0 }

    // Convert Hz to MIDI for linear perception
    fun hzToMidi(hz: Double): Double {
        if (hz <= 0) return 0.0
        return 12.0 * kotlin.math.log2(hz / 440.0) + 69.0
    }

    val allMidi = allPoints.map { hzToMidi(it.frequency ?: 0.0) }
    val minPitch = allMidi.minOrNull() ?: 0.0
    val maxPitch = allMidi.maxOrNull() ?: 100.0

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val timeRange = (maxTime - minTime).takeIf { it > 0 } ?: 1.0
        val pitchRange = (maxPitch - minPitch).takeIf { it > 0 } ?: 1.0
        
        // Draw Grid
        drawLine(
            color = androidx.compose.ui.graphics.Color.DarkGray.copy(alpha = 0.5f),
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 2f
        )
        drawLine(
            color = androidx.compose.ui.graphics.Color.DarkGray.copy(alpha = 0.5f),
            start = Offset(0f, 0f),
            end = Offset(0f, height),
            strokeWidth = 2f
        )

        fun drawCurve(points: List<PitchTrackPoint>, color: androidx.compose.ui.graphics.Color) {
            if (points.isEmpty()) return
            val path = Path()
            var isFirst = true

            points.forEach { point ->
                val time = point.time ?: 0.0
                val freq = point.frequency
                
                if (freq != null && freq > 0) {
                    val midi = hzToMidi(freq)
                    val x = ((time - minTime) / timeRange).toFloat() * width
                    val normalizedPitch = ((midi - minPitch) / pitchRange).toFloat()
                    val y = height - (normalizedPitch * height)

                    if (isFirst) {
                        path.moveTo(x, y)
                        isFirst = false
                    } else {
                        path.lineTo(x, y)
                    }
                } else {
                    isFirst = true // Break the line if unvoiced
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 4f)
            )
        }

        drawCurve(validRef, androidx.compose.ui.graphics.Color.Cyan)
        drawCurve(validUser, androidx.compose.ui.graphics.Color(0xFFFFA500)) // Orange
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}