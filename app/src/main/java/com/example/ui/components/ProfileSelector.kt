package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChargingProfile
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

@Composable
fun ProfileSelector(
  selectedProfile: ChargingProfile,
  onProfileSelected: (ChargingProfile) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = "GT 50 PRO CHARGING MODES",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = GtTextSecondary,
      letterSpacing = 1.sp,
      modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )

    ChargingProfile.entries.forEach { profile ->
      val isSelected = profile == selectedProfile
      val borderColor = if (isSelected) GtOrange else GtBorder
      val containerColor = if (isSelected) GtSlate else GtCarbon

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 5.dp)
          .clip(RoundedCornerShape(16.dp))
          .clickable { onProfileSelected(profile) }
          .testTag("profile_card_${profile.name}"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(if (isSelected) GtOrange.copy(alpha = 0.2f) else GtBorder.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when {
                profile.isBypass -> Icons.Default.SportsEsports
                profile == ChargingProfile.HYPER_CHARGE -> Icons.Default.Bolt
                else -> Icons.Default.Tune
              },
              contentDescription = null,
              tint = if (isSelected) GtOrange else GtTextSecondary,
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = profile.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) GtOrange else GtTextPrimary
              )

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSelected) GtOrange.copy(alpha = 0.15f) else GtBorder)
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = profile.badgeText,
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) GtOrange else GtTextDim
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = profile.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = GtTextSecondary,
              fontSize = 12.sp,
              lineHeight = 16.sp
            )
          }

          if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = GtVoltGreen,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}
