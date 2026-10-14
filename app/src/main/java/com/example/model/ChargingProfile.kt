package com.example.model

enum class ChargingProfile(
  val title: String,
  val subtitle: String,
  val maxCurrentMa: Int,
  val targetWattageW: Int,
  val isBypass: Boolean,
  val badgeText: String
) {
  HYPER_CHARGE(
    title = "GT 50 Pro HyperCharge",
    subtitle = "Removes kernel thermal current limits. Unlocks maximum 12,000 mA pump express dual-charge protocol.",
    maxCurrentMa = 12000,
    targetWattageW = 68,
    isBypass = false,
    badgeText = "MAX SPEED"
  ),
  BYPASS_GAMING(
    title = "GT Bypass Gaming Mode",
    subtitle = "Reroutes electricity directly to SoC and motherboard. Battery current drops to 0mA to stop battery heat.",
    maxCurrentMa = 0,
    targetWattageW = 35,
    isBypass = true,
    badgeText = "0% HEAT"
  ),
  COLD_PULSE(
    title = "Smart Cold FastCharge",
    subtitle = "Dynamic current ramping based on cell thermal state. Delivers 2.5x speed while protecting battery health.",
    maxCurrentMa = 7500,
    targetWattageW = 45,
    isBypass = false,
    badgeText = "SAFE DURATION"
  ),
  TURBO_SUSTAINED(
    title = "GT Turbo Sustained 2.0",
    subtitle = "Overrides Android thermal-engine step-down curve, maintaining full charging current past 80% capacity.",
    maxCurrentMa = 9000,
    targetWattageW = 55,
    isBypass = false,
    badgeText = "0-100% SPRINT"
  )
}
