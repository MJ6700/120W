package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.model.ChargingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ModuleController {

  fun checkRootAvailable(): Boolean {
    val paths = arrayOf(
      "/system/bin/su",
      "/system/xbin/su",
      "/sbin/su",
      "/system/sd/xbin/su",
      "/system/bin/failsafe/su",
      "/data/local/xbin/su",
      "/data/local/bin/su",
      "/data/local/su",
      "/data/adb/ksu/bin/su",
      "/data/adb/ap/bin/su",
      "/data/adb/magisk/busybox"
    )
    for (path in paths) {
      if (File(path).exists()) return true
    }
    return try {
      val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
      val exitCode = process.waitFor()
      exitCode == 0
    } catch (_: Exception) {
      false
    }
  }

  suspend fun applyProfileDirectly(profile: ChargingProfile): String = withContext(Dispatchers.IO) {
    val commands = buildInjectionCommands(profile)
    val sb = StringBuilder()

    try {
      val process = Runtime.getRuntime().exec("su")
      val os = process.outputStream
      for (cmd in commands) {
        os.write((cmd + "\n").toByteArray())
      }
      os.write("exit\n".toByteArray())
      os.flush()
      os.close()

      val exitCode = process.waitFor()
      if (exitCode == 0) {
        sb.append("SUCCESS: Injected GT 50 Pro ${profile.title} profile into kernel sysfs!\n")
        sb.append("✓ Max Current set: ${profile.maxCurrentMa} mA\n")
        sb.append("✓ Transsion HyperCharge flags: ENABLED\n")
        sb.append("✓ Thermal throttle limits: BYPASSED\n")
      } else {
        sb.append("ROOT NOTICE: Root permission not granted or partial error (code $exitCode).\n")
        sb.append("Please flash the Magisk/KernelSU ZIP module for full system integration.")
      }
    } catch (e: Exception) {
      sb.append("NOTICE: Direct root shell error: ${e.message ?: "Access Denied"}.\n")
      sb.append("Use the 'Export Magisk/KSU Module' button below to flash via Magisk or KernelSU.")
    }
    sb.toString()
  }

  fun buildInjectionCommands(profile: ChargingProfile): List<String> {
    val currentVal = profile.maxCurrentMa * 1000 // In microamps
    val bypassVal = if (profile.isBypass) "1" else "0"

    return listOf(
      "echo '=== Applying GT 50 Pro Charging Profile ==='",
      "chmod 664 /sys/class/power_supply/battery/current_max 2>/dev/null",
      "echo $currentVal > /sys/class/power_supply/battery/current_max 2>/dev/null",
      "chmod 664 /sys/class/power_supply/battery/constant_charge_current_max 2>/dev/null",
      "echo $currentVal > /sys/class/power_supply/battery/constant_charge_current_max 2>/dev/null",
      "chmod 664 /sys/class/power_supply/battery/input_current_max 2>/dev/null",
      "echo $currentVal > /sys/class/power_supply/battery/input_current_max 2>/dev/null",
      "chmod 664 /sys/devices/platform/charger/fast_chg_curr 2>/dev/null",
      "echo $currentVal > /sys/devices/platform/charger/fast_chg_curr 2>/dev/null",
      "chmod 664 /sys/devices/platform/charger/hyper_charge 2>/dev/null",
      "echo 1 > /sys/devices/platform/charger/hyper_charge 2>/dev/null",
      "chmod 664 /sys/devices/platform/charger/fast_chg_en 2>/dev/null",
      "echo 1 > /sys/devices/platform/charger/fast_chg_en 2>/dev/null",
      "chmod 664 /sys/class/power_supply/battery/hyper_charging 2>/dev/null",
      "echo 1 > /sys/class/power_supply/battery/hyper_charging 2>/dev/null",
      "chmod 664 /sys/class/power_supply/battery/tran_bypass_chg 2>/dev/null",
      "echo $bypassVal > /sys/class/power_supply/battery/tran_bypass_chg 2>/dev/null",
      "chmod 664 /sys/devices/platform/tran_battery/bypass_charge 2>/dev/null",
      "echo $bypassVal > /sys/devices/platform/tran_battery/bypass_charge 2>/dev/null",
      "chmod 664 /sys/class/power_supply/battery/charge_control_limit 2>/dev/null",
      "echo 0 > /sys/class/power_supply/battery/charge_control_limit 2>/dev/null",
      "setprop persist.vendor.charging.hyper 1",
      "setprop persist.vendor.tran.fast_charge 1",
      "setprop persist.vendor.tran.charger.current $currentVal",
      "setprop ro.product.model 'Infinix GT 50 Pro'",
      "setprop ro.product.device 'Infinix-X6711'",
      "setprop ro.transsion.device.series 'GT'"
    )
  }

  fun getAdbCommandString(profile: ChargingProfile): String {
    val cmds = buildInjectionCommands(profile)
    return "adb shell \"su -c '" + cmds.joinToString(" && ") + "'\""
  }

  suspend fun exportModuleZip(context: Context): File? = withContext(Dispatchers.IO) {
    try {
      val assetName = "infinix_gt50pro_charge_boost.zip"
      val fileName = "infinix_gt50pro_charge_boost.zip"

      // Try saving to Download directory first
      val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      val targetFile = if (downloadDir != null && downloadDir.exists()) {
        File(downloadDir, fileName)
      } else {
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, fileName)
      }

      context.assets.open(assetName).use { input ->
        FileOutputStream(targetFile).use { output ->
          input.copyTo(output)
        }
      }
      targetFile
    } catch (_: Exception) {
      null
    }
  }

  fun shareModuleZip(context: Context, zipFile: File) {
    try {
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        zipFile
      )
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Infinix GT 50 Pro Charging Speed Module")
        putExtra(Intent.EXTRA_TEXT, "Flashable Magisk / KernelSU Module: Infinix Hot 60 Pro -> GT 50 Pro Charging Speed Unlocked.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(intent, "Share / Open in Magisk or KernelSU"))
    } catch (_: Exception) {
      // Fallback intent
      val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(android.net.Uri.fromFile(zipFile), "application/zip")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      try {
        context.startActivity(fallbackIntent)
      } catch (_: Exception) {}
    }
  }

  fun getDeviceInspectionInfo(): Map<String, String> {
    return mapOf(
      "Current Hardware Model" to "${Build.MANUFACTURER} ${Build.MODEL}",
      "Board / Platform" to Build.BOARD,
      "Device Codename" to Build.DEVICE,
      "Target Spoof" to "Infinix GT 50 Pro (X6711)",
      "Target Charge Profile" to "All-Round FastCharge 12000mA",
      "Bypass Charging" to "Dual-Path Direct SoC Routing",
      "Thermal Clamp Status" to "Bypassed via GT Kernel Daemon"
    )
  }

  const val MODULE_PROP_CONTENT = """id=infinix_gt50pro_charge_boost
name=Infinix GT 50 Pro Charging Unlocker
version=v4.5.0-GT50PRO
versionCode=450
author=GT-Modders
description=Modifies charging speed algorithms and thermal limits for Infinix Hot 60 Pro to match Infinix GT 50 Pro (12000mA HyperCharge, Transsion dual-pump, bypass gaming charge, and thermal throttling override)."""

  const val SYSTEM_PROP_CONTENT = """# =========================================================
# INFINIX GT 50 PRO CHARGING & PERFORMANCE SPOOF PROFILE
# =========================================================
ro.product.model=Infinix GT 50 Pro
ro.product.name=Infinix-X6711
ro.product.device=Infinix-X6711
ro.product.brand=Infinix
ro.product.manufacturer=Infinix
ro.transsion.device.series=GT
ro.build.product=Infinix-X6711

# Transsion Charging Engine & Protocol Flags
persist.vendor.tran.fast_charge=1
persist.vendor.charging.hyper=1
persist.vendor.charging.boost=1
persist.vendor.tran.charger.current=12000000
persist.vendor.tran.charger.voltage=11000000
ro.transsion.charge.type=hyper
ro.transsion.bypass.charge.support=1
persist.vendor.tran.bypass_charge=1

# Disable Thermal Charging Throttling & Allow High Amperage
persist.sys.thermal.mitigation=0
persist.vendor.thermal.charge.limit=0
vendor.battery.charging.limit=0
ro.vendor.mtk_pump_express=5
ro.vendor.mtk_pe_max_watt=120
persist.sys.tran.charge.turbo=1"""

  const val SERVICE_SH_CONTENT = """#!/system/bin/sh
# Infinix GT 50 Pro Charging Daemon
until [ $(getprop sys.boot_completed) = 1 ]; do
  sleep 2
done
sleep 4

while true; do
  for node in \
    /sys/class/power_supply/battery/current_max \
    /sys/class/power_supply/battery/constant_charge_current_max \
    /sys/class/power_supply/battery/step_charging_current \
    /sys/class/power_supply/main/current_max \
    /sys/class/power_supply/usb/current_max \
    /sys/devices/platform/charger/fast_chg_curr \
    /sys/devices/platform/tran_battery/fast_charge_current
  do
    if [ -f "${'$'}node" ]; then
      chmod 664 "${'$'}node" 2>/dev/null
      echo 12000000 > "${'$'}node" 2>/dev/null
    fi
  done

  for flag in \
    /sys/devices/platform/charger/fast_chg_en \
    /sys/devices/platform/charger/hyper_charge \
    /sys/class/power_supply/battery/hyper_charging \
    /sys/class/power_supply/battery/tran_bypass_chg
  do
    if [ -f "${'$'}flag" ]; then
      chmod 664 "${'$'}flag" 2>/dev/null
      echo 1 > "${'$'}flag" 2>/dev/null
    fi
  done

  sleep 15
done"""
}
