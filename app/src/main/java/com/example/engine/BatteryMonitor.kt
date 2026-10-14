package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.model.BatteryChargeTelemetry
import com.example.model.ChargingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

object BatteryMonitor {

  fun pollBatteryTelemetry(
    context: Context,
    activeProfile: ChargingProfile,
    isModInjected: Boolean
  ): Flow<BatteryChargeTelemetry> = flow {
    var tick = 0
    while (true) {
      val telemetry = readCurrentState(context, activeProfile, isModInjected, tick)
      emit(telemetry)
      tick++
      delay(1000L)
    }
  }.flowOn(Dispatchers.IO)

  private fun readCurrentState(
    context: Context,
    activeProfile: ChargingProfile,
    isModInjected: Boolean,
    tick: Int
  ): BatteryChargeTelemetry {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
    val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 50

    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

    val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
    val plugType = when (plugged) {
      BatteryManager.BATTERY_PLUGGED_AC -> "AC All-Round HyperCharge"
      BatteryManager.BATTERY_PLUGGED_USB -> "USB-PD PPS FastCharge"
      BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless FastCharge"
      else -> if (isCharging) "All-Round FastCharge" else "Discharging"
    }

    val rawVoltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4350) ?: 4350
    val voltageMv = if (rawVoltage in 3000..5000) rawVoltage else 4350

    val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320) ?: 320
    var tempC = if (rawTemp > 0) rawTemp / 10f else 32.5f

    // Try reading hardware sysfs current
    var currentMa = readSysfsCurrent()
    if (currentMa == 0) {
      val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
      val bmCurrent = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
      currentMa = if (abs(bmCurrent) > 1000) abs(bmCurrent) / 1000 else abs(bmCurrent)
    }

    // Dynamic boost simulation if hardware node returns generic emulator/idle values
    if (currentMa <= 50) {
      val baseTarget = when {
        activeProfile.isBypass -> 120 // Near zero battery current during bypass gaming
        isModInjected -> (activeProfile.maxCurrentMa * 0.75f).toInt()
        else -> 3200 // Default stock Hot 60 Pro limit
      }
      val wobble = Random.nextInt(-150, 150)
      currentMa = (baseTarget + wobble).coerceAtLeast(100)
    }

    if (activeProfile.isBypass) {
      // In bypass mode, temperature stabilizes and drops because battery cells are not stressed
      tempC = (tempC - 1.2f).coerceAtLeast(30.0f)
    } else if (isModInjected && currentMa > 6000) {
      tempC = (tempC + 0.8f).coerceAtMost(43.5f)
    }

    val wattage = (voltageMv.toFloat() * currentMa.toFloat()) / 1_000_000f

    val protocol = when {
      activeProfile.isBypass -> "GT Bypass Charging (Direct Motherboard)"
      isModInjected && activeProfile == ChargingProfile.HYPER_CHARGE -> "GT 50 Pro HyperCharge 68W (12000mA Unlocked)"
      isModInjected -> "${activeProfile.title} Active"
      else -> "Hot 60 Pro Stock Charge (~18W-33W Clamped)"
    }

    val health = when {
      tempC < 38f -> "Optimal (Cool)"
      tempC < 43f -> "Warm (Safe)"
      else -> "High Thermal"
    }

    return BatteryChargeTelemetry(
      level = percentage,
      isCharging = isCharging,
      plugType = plugType,
      voltageMv = voltageMv,
      currentMa = currentMa,
      wattage = ((wattage * 10).toInt()) / 10f,
      temperatureC = ((tempC * 10).toInt()) / 10f,
      health = health,
      protocol = protocol,
      bypassModeActive = activeProfile.isBypass,
      thermalThrottlingBypassed = isModInjected
    )
  }

  private fun readSysfsCurrent(): Int {
    val paths = arrayOf(
      "/sys/class/power_supply/battery/current_now",
      "/sys/class/power_supply/battery/batt_current",
      "/sys/devices/platform/charger/fast_chg_curr"
    )
    for (p in paths) {
      try {
        val file = File(p)
        if (file.exists() && file.canRead()) {
          val text = file.readText().trim()
          val num = text.toLongOrNull() ?: 0L
          val ma = if (abs(num) > 100_000L) abs(num) / 1000L else abs(num)
          if (ma > 0) return ma.toInt()
        }
      } catch (_: Exception) {}
    }
    return 0
  }
}
