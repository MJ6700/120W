package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatteryChargeTelemetry
import com.example.ui.theme.GtBorder
import com.example.ui.theme.GtCarbon
import com.example.ui.theme.GtNeonCyan
import com.example.ui.theme.GtNeonYellow
import com.example.ui.theme.GtObsidian
import com.example.ui.theme.GtOrange
import com.example.ui.theme.GtSlate
import com.example.ui.theme.GtTextDim
import com.example.ui.theme.GtTextPrimary
import com.example.ui.theme.GtTextSecondary
import com.example.ui.theme.GtVoltGreen

@Composable
fun TelemetryHud(
  telemetry: BatteryChargeTelemetry,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseGlow by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("telemetry_hud_card"),
    colors = CardDefaults.cardColors(containerColor = GtCarbon),
    shape = RoundedCornerShape(24.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, GtBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Protocol Badge
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(GtSlate)
          .border(1.dp, GtOrange.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
          .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = GtOrange,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = telemetry.protocol.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = GtOrange,
            letterSpacing = 0.8.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Circular Cyber Speed Gauge
      Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.size(200.dp)) {
          val strokeWidth = 14.dp.toPx()
          val radius = (size.minDimension - strokeWidth) / 2

          // Background track
          drawCircle(
            color = Color(0xFF1B2232),
            radius = radius,
            style = Stroke(width = strokeWidth)
          )

          // Active Wattage Arc (mapped 0W to 120W)
          val sweepRatio = (telemetry.wattage / 68f).coerceIn(0.1f, 1f)
          drawArc(
            brush = Brush.sweepGradient(
              listOf(GtNeonCyan, GtOrange, GtNeonYellow)
            ),
            startAngle = -90f,
            sweepAngle = sweepRatio * 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
          )
        }

        // Inner HUD Content
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = String.format("%.1f", telemetry.wattage),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = GtTextPrimary,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "WATTS (LIVE)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = GtOrange.copy(alpha = pulseGlow),
            letterSpacing = 1.2.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${telemetry.level}% CHARGED",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = GtNeonCyan
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 3-Metric Cyber Bar (Current, Voltage, Temp)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        MetricTile(
          icon = Icons.Default.Speed,
          label = "CURRENT",
          value = if (telemetry.bypassModeActive) "0 mA (Bypass)" else "+${telemetry.currentMa} mA",
          tint = GtNeonCyan,
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        MetricTile(
          icon = Icons.Default.ElectricBolt,
          label = "VOLTAGE",
          value = "${String.format("%.2f", telemetry.voltageMv / 1000f)} V",
          tint = GtOrange,
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        MetricTile(
          icon = Icons.Default.DeviceThermostat,
          label = "THERMALS",
          value = "${telemetry.temperatureC}°C",
          tint = if (telemetry.temperatureC < 38f) GtVoltGreen else GtNeonYellow,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun MetricTile(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
  tint: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(GtSlate)
      .border(1.dp, GtBorder, RoundedCornerShape(14.dp))
      .padding(10.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = tint,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = GtTextDim,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = GtTextPrimary,
        textAlign = TextAlign.Center,
        fontSize = 13.sp
      )
    }
  }
}
