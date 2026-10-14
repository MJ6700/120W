package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatteryChargeTelemetry
import com.example.ui.theme.GtBorder
import com.example.ui.theme.GtCarbon
import com.example.ui.theme.GtNeonCyan
import com.example.ui.theme.GtNeonYellow
import com.example.ui.theme.GtOrange
import com.example.ui.theme.GtSlate
import com.example.ui.theme.GtTextDim
import com.example.ui.theme.GtTextPrimary
import com.example.ui.theme.GtTextSecondary
import com.example.ui.theme.GtVoltGreen
import kotlinx.coroutines.delay

@Composable
fun BenchmarkCard(
  telemetry: BatteryChargeTelemetry,
  modifier: Modifier = Modifier
) {
  var isBenchmarking by remember { mutableStateOf(false) }
  var benchmarkSeconds by remember { mutableIntStateOf(0) }
  var accumulatedMah by remember { mutableStateOf(0f) }

  LaunchedEffect(isBenchmarking) {
    if (isBenchmarking) {
      benchmarkSeconds = 0
      accumulatedMah = 0f
      while (benchmarkSeconds < 30) {
        delay(1000L)
        benchmarkSeconds++
        // mAh = (currentMa * hours) = currentMa * (1/3600)
        accumulatedMah += (telemetry.currentMa / 3600f)
      }
      isBenchmarking = false
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("benchmark_card"),
    colors = CardDefaults.cardColors(containerColor = GtCarbon),
    shape = RoundedCornerShape(20.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, GtBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = GtNeonYellow,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "SPEED & 0-100% PROJECTION",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GtTextPrimary
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(GtVoltGreen.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = "+175% FASTER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = GtVoltGreen,
            fontSize = 11.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Comparison bars
      Column(modifier = Modifier.fillMaxWidth()) {
        ComparisonRow(
          device = "Hot 60 Pro (Stock)",
          speed = "18W - 33W",
          time = "~75 mins",
          barColor = GtTextDim,
          fillPercent = 0.35f
        )

        Spacer(modifier = Modifier.height(10.dp))

        ComparisonRow(
          device = "GT 50 Pro (Mod Active)",
          speed = "68W - 120W (HyperCharge)",
          time = "~28 mins",
          barColor = GtOrange,
          fillPercent = 1.0f
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Benchmark Run box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(GtSlate)
          .border(1.dp, GtBorder, RoundedCornerShape(14.dp))
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (isBenchmarking) "RUNNING BENCHMARK (${30 - benchmarkSeconds}s)" else "LIVE CHARGING VELOCITY",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isBenchmarking) GtOrange else GtTextDim,
              fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (isBenchmarking) {
                "+${String.format("%.1f", accumulatedMah)} mAh gained"
              } else {
                "+${(telemetry.currentMa / 60)} mAh / minute"
              },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = GtNeonCyan,
              fontFamily = FontFamily.Monospace
            )
          }

          Button(
            onClick = { isBenchmarking = !isBenchmarking },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isBenchmarking) GtBorder else GtOrange
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(38.dp)
          ) {
            Icon(
              imageVector = if (isBenchmarking) Icons.Default.Timer else Icons.Default.PlayArrow,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isBenchmarking) "Stop" else "Test",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ComparisonRow(
  device: String,
  speed: String,
  time: String,
  barColor: Color,
  fillPercent: Float
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = device,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = GtTextPrimary,
        fontSize = 12.sp
      )
      Text(
        text = "$speed • $time",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = barColor,
        fontSize = 12.sp
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0xFF1E2532))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(fillPercent)
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(barColor)
      )
    }
  }
}
