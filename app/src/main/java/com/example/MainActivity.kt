package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BatteryMonitor
import com.example.engine.ModuleController
import com.example.model.BatteryChargeTelemetry
import com.example.model.ChargingProfile
import com.example.ui.components.BenchmarkCard
import com.example.ui.components.ModuleInspectorDialog
import com.example.ui.components.ProfileSelector
import com.example.ui.components.TelemetryHud
import com.example.ui.theme.GtBorder
import com.example.ui.theme.GtCarbon
import com.example.ui.theme.GtNeonCyan
import com.example.ui.theme.GtNeonYellow
import com.example.ui.theme.GtObsidian
import com.example.ui.theme.GtOrange
import com.example.ui.theme.GtOrangeDark
import com.example.ui.theme.GtSlate
import com.example.ui.theme.GtTextDim
import com.example.ui.theme.GtTextPrimary
import com.example.ui.theme.GtTextSecondary
import com.example.ui.theme.GtVoltGreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        GtChargeModScreen()
      }
    }
  }
}

@Composable
fun GtChargeModScreen() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedProfile by remember { mutableStateOf(ChargingProfile.HYPER_CHARGE) }
  var isModInjected by remember { mutableStateOf(true) }
  var thermalGuardEnabled by remember { mutableStateOf(true) }

  val isRootAvailable = remember { ModuleController.checkRootAvailable() }
  var isApplyingMod by remember { mutableStateOf(false) }
  var isExportingZip by remember { mutableStateOf(false) }
  var executionLog by remember { mutableStateOf<String?>(null) }
  var showInspectorDialog by remember { mutableStateOf(false) }
  var showAdbDialog by remember { mutableStateOf(false) }

  // Collect real-time telemetry Flow
  val telemetry by BatteryMonitor.pollBatteryTelemetry(
    context = context,
    activeProfile = selectedProfile,
    isModInjected = isModInjected
  ).collectAsState(initial = BatteryChargeTelemetry())

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = GtObsidian
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Top Cyber Mecha Header
      HeaderCard(
        isRooted = isRootAvailable,
        onInspectClicked = { showInspectorDialog = true }
      )

      // Live Telemetry Speedometer HUD
      TelemetryHud(telemetry = telemetry)

      // Action Buttons (Apply Root Mod, Export ZIP, Copy ADB)
      ActionsSection(
        isRooted = isRootAvailable,
        isApplying = isApplyingMod,
        isExporting = isExportingZip,
        onApplyRoot = {
          coroutineScope.launch {
            isApplyingMod = true
            val log = ModuleController.applyProfileDirectly(selectedProfile)
            executionLog = log
            isModInjected = true
            isApplyingMod = false
            Toast.makeText(context, "GT 50 Pro Profile Injected!", Toast.LENGTH_SHORT).show()
          }
        },
        onExportZip = {
          coroutineScope.launch {
            isExportingZip = true
            val file = ModuleController.exportModuleZip(context)
            isExportingZip = false
            if (file != null) {
              Toast.makeText(context, "Saved: ${file.name}", Toast.LENGTH_LONG).show()
              ModuleController.shareModuleZip(context, file)
            } else {
              Toast.makeText(context, "Could not export ZIP. Check storage.", Toast.LENGTH_SHORT).show()
            }
          }
        },
        onShowAdb = { showAdbDialog = true }
      )

      // Charging Profile Selector
      ProfileSelector(
        selectedProfile = selectedProfile,
        onProfileSelected = { newProfile ->
          selectedProfile = newProfile
          if (isRootAvailable) {
            coroutineScope.launch {
              ModuleController.applyProfileDirectly(newProfile)
            }
          }
        }
      )

      // Benchmark & Speed Projection
      BenchmarkCard(telemetry = telemetry)

      // Thermal Safety Failsafe Guard Card
      ThermalGuardCard(
        enabled = thermalGuardEnabled,
        onToggle = { thermalGuardEnabled = it }
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  // Execution Log Dialog
  executionLog?.let { log ->
    AlertDialog(
      onDismissRequest = { executionLog = null },
      containerColor = GtCarbon,
      shape = RoundedCornerShape(18.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = GtOrange)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Kernel Sysfs Log", style = MaterialTheme.typography.titleMedium, color = GtTextPrimary)
        }
      },
      text = {
        Text(
          text = log,
          style = MaterialTheme.typography.bodySmall,
          fontFamily = FontFamily.Monospace,
          color = GtNeonCyan,
          fontSize = 12.sp
        )
      },
      confirmButton = {
        Button(
          onClick = { executionLog = null },
          colors = ButtonDefaults.buttonColors(containerColor = GtOrange)
        ) {
          Text("Done")
        }
      }
    )
  }

  // ADB Command Modal
  if (showAdbDialog) {
    val adbCmd = remember(selectedProfile) { ModuleController.getAdbCommandString(selectedProfile) }
    AlertDialog(
      onDismissRequest = { showAdbDialog = false },
      containerColor = GtCarbon,
      shape = RoundedCornerShape(18.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = GtNeonCyan)
          Spacer(modifier = Modifier.width(8.dp))
          Text("ADB Shell Command", style = MaterialTheme.typography.titleMedium, color = GtTextPrimary)
        }
      },
      text = {
        Column {
          Text(
            text = "Run this command via USB Debugging on PC if your Infinix phone is unrooted:",
            style = MaterialTheme.typography.bodySmall,
            color = GtTextSecondary
          )
          Spacer(modifier = Modifier.height(10.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(GtSlate)
              .border(1.dp, GtBorder, RoundedCornerShape(10.dp))
              .padding(10.dp)
          ) {
            Text(
              text = adbCmd,
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = GtNeonCyan,
              fontSize = 11.sp
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCmd))
            Toast.makeText(context, context.getString(R.string.module_copied_adb), Toast.LENGTH_SHORT).show()
            showAdbDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = GtOrange)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Copy Command")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAdbDialog = false }) {
          Text("Close", color = GtTextSecondary)
        }
      }
    )
  }

  // Module Inspector Dialog
  if (showInspectorDialog) {
    ModuleInspectorDialog(onDismiss = { showInspectorDialog = false })
  }
}

@Composable
private fun HeaderCard(
  isRooted: Boolean,
  onInspectClicked: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("header_card"),
    colors = CardDefaults.cardColors(containerColor = GtCarbon),
    shape = RoundedCornerShape(20.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, GtBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(GtOrange.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.ElectricBolt,
              contentDescription = null,
              tint = GtOrange,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "GT 50 PRO CHARGE MOD",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = GtTextPrimary,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "Target: Infinix GT 50 Pro (X6711)",
              style = MaterialTheme.typography.bodySmall,
              color = GtNeonCyan,
              fontSize = 11.sp
            )
          }
        }

        OutlinedButton(
          onClick = onInspectClicked,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = GtOrange),
          modifier = Modifier.height(34.dp)
        ) {
          Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Scripts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Device Spoof pill
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(GtSlate)
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Source: ${Build.MANUFACTURER} ${Build.MODEL}",
          style = MaterialTheme.typography.labelSmall,
          color = GtTextSecondary,
          fontSize = 11.sp
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(if (isRooted) GtVoltGreen else GtNeonYellow)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isRooted) "ROOT ACTIVE" else "MAGISK ZIP READY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isRooted) GtVoltGreen else GtNeonYellow,
            fontSize = 10.sp
          )
        }
      }
    }
  }
}

@Composable
private fun ActionsSection(
  isRooted: Boolean,
  isApplying: Boolean,
  isExporting: Boolean,
  onApplyRoot: () -> Unit,
  onExportZip: () -> Unit,
  onShowAdb: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Primary Apply Button
    Button(
      onClick = onApplyRoot,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("apply_root_button"),
      colors = ButtonDefaults.buttonColors(
        containerColor = GtOrange,
        contentColor = Color.White
      ),
      shape = RoundedCornerShape(14.dp),
      enabled = !isApplying
    ) {
      if (isApplying) {
        CircularProgressIndicator(
          color = Color.White,
          strokeWidth = 2.dp,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text("Injecting GT 50 Pro Kernel Nodes...", fontWeight = FontWeight.Bold)
      } else {
        Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isRooted) "⚡ APPLY GT 50 PRO CHARGING (ROOT)" else "⚡ INJECT GT 50 PRO CHARGING KERNEL NODES",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      }
    }

    // Secondary Row (Export ZIP & ADB)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onExportZip,
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("export_zip_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = GtCarbon,
          contentColor = GtNeonCyan
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GtBorder),
        enabled = !isExporting
      ) {
        if (isExporting) {
          CircularProgressIndicator(
            color = GtNeonCyan,
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp)
          )
        } else {
          Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Flashable ZIP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }

      Button(
        onClick = onShowAdb,
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("copy_adb_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = GtCarbon,
          contentColor = GtTextPrimary
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GtBorder)
      ) {
        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp), tint = GtOrange)
        Spacer(modifier = Modifier.width(6.dp))
        Text("ADB Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  }
}

@Composable
private fun ThermalGuardCard(
  enabled: Boolean,
  onToggle: (Boolean) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("thermal_guard_card"),
    colors = CardDefaults.cardColors(containerColor = GtCarbon),
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, GtBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(GtVoltGreen.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = GtVoltGreen,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Active Thermal Guard (45°C Failsafe)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GtTextPrimary
          )
          Text(
            text = "Auto-regulates current if battery temp exceeds 45°C to protect cell life.",
            style = MaterialTheme.typography.bodySmall,
            color = GtTextSecondary,
            fontSize = 11.sp
          )
        }
      }

      Switch(
        checked = enabled,
        onCheckedChange = onToggle,
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color.White,
          checkedTrackColor = GtVoltGreen,
          uncheckedTrackColor = GtSlate
        )
      )
    }
  }
}
