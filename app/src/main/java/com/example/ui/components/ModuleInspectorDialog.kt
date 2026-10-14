package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ModuleController
import com.example.ui.theme.GtBorder
import com.example.ui.theme.GtCarbon
import com.example.ui.theme.GtNeonCyan
import com.example.ui.theme.GtOrange
import com.example.ui.theme.GtSlate
import com.example.ui.theme.GtTextDim
import com.example.ui.theme.GtTextPrimary
import com.example.ui.theme.GtTextSecondary

@Composable
fun ModuleInspectorDialog(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("system.prop", "service.sh", "module.prop", "Device Info")

  val content = when (selectedTab) {
    0 -> ModuleController.SYSTEM_PROP_CONTENT
    1 -> ModuleController.SERVICE_SH_CONTENT
    2 -> ModuleController.MODULE_PROP_CONTENT
    else -> ModuleController.getDeviceInspectionInfo().entries.joinToString("\n") { "${it.key}: ${it.value}" }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = GtCarbon,
    shape = RoundedCornerShape(20.dp),
    modifier = modifier.testTag("module_inspector_dialog"),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = GtOrange,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Module Code & Script",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GtTextPrimary
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = GtTextSecondary
          )
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        ScrollableTabRow(
          selectedTabIndex = selectedTab,
          containerColor = GtSlate,
          contentColor = GtOrange,
          edgePadding = 0.dp,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = GtOrange
            )
          }
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = {
                Text(
                  text = title,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                  color = if (selectedTab == index) GtOrange else GtTextSecondary
                )
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GtSlate)
            .border(1.dp, GtBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
        ) {
          Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = GtNeonCyan,
            lineHeight = 16.sp
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          cm.setPrimaryClip(ClipData.newPlainText("Module Code", content))
          Toast.makeText(context, "Copied ${tabs[selectedTab]} to clipboard!", Toast.LENGTH_SHORT).show()
        },
        colors = ButtonDefaults.buttonColors(containerColor = GtOrange)
      ) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Copy Script")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = GtTextSecondary)
      }
    }
  )
}
