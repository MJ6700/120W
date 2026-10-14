package com.example.model

data class BatteryChargeTelemetry(
  val level: Int = 50,
  val isCharging: Boolean = true,
  val plugType: String = "AC Charger (Fast)",
  val voltageMv: Int = 4350,
  val currentMa: Int = 5800,
  val wattage: Float = 25.23f,
  val temperatureC: Float = 33.5f,
  val health: String = "Good",
  val protocol: String = "GT 50 Pro HyperCharge (12000mA Unlocked)",
  val bypassModeActive: Boolean = false,
  val thermalThrottlingBypassed: Boolean = true
)
