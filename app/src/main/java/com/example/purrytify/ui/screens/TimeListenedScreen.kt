package com.example.purrytify.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.data.DailyListenDuration
import com.example.purrytify.viewmodel.ProfileViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun TimeListenedScreen(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val dailyData by profileViewModel.dailyListenData.collectAsState()
    val selectedMonth by profileViewModel.selectedMonthData.collectAsState()
//    val uiState by profileViewModel.uiState.collectAsState() // Untuk total bulanan jika diperlukan

    // Panggil loadDailyListenDetailsForMonth saat screen pertama kali dibuat atau selectedMonth berubah
    LaunchedEffect(selectedMonth) {
        profileViewModel.loadDailyListenDetailsForMonth(selectedMonth)
    }

    val totalMinutesThisMonth = dailyData.sumOf { it.totalDurationMillis } / (1000 * 60)
    val daysInMonth = selectedMonth.lengthOfMonth()
    val dailyAverageMinutes = if (dailyData.any { it.totalDurationMillis > 0 }) { // Hitung avg jika ada data
        totalMinutesThisMonth / dailyData.count { it.totalDurationMillis > 0 }
    } else {
        0L
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time listened") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212), // Warna gelap seperti Spotify
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212) // Background gelap
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                buildAnnotatedString {
                    append("You listened to music for ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF1DB954))) {
                        append("$totalMinutesThisMonth minutes")
                    }
                    append(" this month.")
                },
                fontSize = 26.sp,
                color = Color.White,
                lineHeight = 34.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Daily average: ${dailyAverageMinutes} min",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (dailyData.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color(0xFF1E1E1E)), contentAlignment = Alignment.Center) {
                    Text("No listening data for this month.", color = Color.Gray)
                }
            } else {
                DailyListenChart(dailyData = dailyData, month = selectedMonth)
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun DailyListenChart(dailyData: List<DailyListenDuration>, month: YearMonth) {
    val maxDurationMinutes = (dailyData.maxOfOrNull { it.totalDurationMillis } ?: 1L) / (1000 * 60) // Maksimum durasi dalam menit
    val chartHeight = 200.dp
    val barWidthFactor = 0.8f // Lebar bar relatif terhadap ruang yang tersedia per hari
    val daysInMonth = month.lengthOfMonth()
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .background(Color(0xFF1A1A1A)) // Warna background chart
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        val availableWidthForBars = size.width - 30.dp.toPx() // Sedikit padding untuk label Y
        val spacePerDay = availableWidthForBars / daysInMonth
        val actualBarWidth = spacePerDay * barWidthFactor
        val barSpacing = spacePerDay * (1 - barWidthFactor) / 2 // Spasi di setiap sisi bar

        // Garis horizontal dasar (sumbu X)
        drawLine(
            color = Color.DarkGray,
            start = Offset(30.dp.toPx(), size.height - 20.dp.toPx()), // 20.dp untuk label X
            end = Offset(size.width, size.height - 20.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        // Label sumbu Y (menit) - Sederhana
        drawText(
            textMeasurer = textMeasurer,
            text = "minutes",
            topLeft = Offset(0f, 0f),
            style = TextStyle(color = Color.Gray, fontSize = 10.sp)
        )
         drawText(
            textMeasurer = textMeasurer,
            text = "0",
            topLeft = Offset(10.dp.toPx(), size.height - 20.dp.toPx() - textMeasurer.measure(AnnotatedString("0")).size.height / 2),
            style = TextStyle(color = Color.Gray, fontSize = 10.sp)
        )


        // Garis putus-putus untuk rata-rata jika ada (opsional)
        // val averageLineY = ...

        // Data untuk chart (map tanggal ke durasi)
        val dataMap = dailyData.associate {
            LocalDate.parse(it.playDate, DateTimeFormatter.ISO_LOCAL_DATE).dayOfMonth to (it.totalDurationMillis / (1000 * 60))
        }

        for (day in 1..daysInMonth) {
            val listenMinutes = dataMap[day] ?: 0L
            val barHeight = if (maxDurationMinutes > 0) {
                (listenMinutes.toFloat() / maxDurationMinutes.toFloat()) * (size.height - 40.dp.toPx()) // 40.dp untuk padding atas/bawah
            } else {
                0f
            }
            barHeight.coerceAtLeast(0f) // Pastikan tidak negatif

            val xOffset = 30.dp.toPx() + (day - 1) * spacePerDay + barSpacing

            if (listenMinutes > 0) { // Hanya gambar bar jika ada durasi
                drawRect(
                    color = Color(0xFF1DB954), // Warna hijau Spotify
                    topLeft = Offset(xOffset, size.height - 20.dp.toPx() - barHeight),
                    size = androidx.compose.ui.geometry.Size(actualBarWidth, barHeight)
                )
            }

            // Label hari (misalnya setiap 5 hari)
            if (day == 1 || day % 7 == 0 || day == daysInMonth) {
                 val dayText = day.toString()
                 val textSize = textMeasurer.measure(AnnotatedString(dayText))
                 drawText(
                    textMeasurer = textMeasurer,
                    text = dayText,
                    topLeft = Offset(xOffset + actualBarWidth / 2 - textSize.size.width / 2, size.height - 18.dp.toPx()),
                    style = TextStyle(color = Color.Gray, fontSize = 8.sp)
                )
            }
        }
         // Label "day" untuk sumbu X
        drawText(
            textMeasurer = textMeasurer,
            text = "day",
            topLeft = Offset(size.width - textMeasurer.measure(AnnotatedString("day")).size.width - 5.dp.toPx(), size.height - 18.dp.toPx()),
            style = TextStyle(color = Color.Gray, fontSize = 10.sp)
        )
    }
}