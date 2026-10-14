#!/system/bin/sh
# Infinix GT 50 Pro Charging Daemon
MODDIR=${0%/*}

until [ $(getprop sys.boot_completed) = 1 ]; do
  sleep 2
done
sleep 4

while true; do
  # 1. Uncap battery charging current limits (12000mA = 12A / 6000mA = 6A)
  for node in \
    /sys/class/power_supply/battery/current_max \
    /sys/class/power_supply/battery/constant_charge_current_max \
    /sys/class/power_supply/battery/step_charging_current \
    /sys/class/power_supply/main/current_max \
    /sys/class/power_supply/usb/current_max \
    /sys/class/power_supply/usb/hw_current_max \
    /sys/class/power_supply/battery/input_current_max \
    /sys/devices/platform/charger/fast_chg_curr \
    /sys/devices/platform/tran_battery/fast_charge_current \
    /sys/devices/platform/charger/hyper_charge_current \
    /sys/devices/platform/tran_charger/current_max
  do
    if [ -f "$node" ]; then
      chmod 664 "$node" 2>/dev/null
      echo 12000000 > "$node" 2>/dev/null || echo 6000000 > "$node" 2>/dev/null
    fi
  done

  # 2. Enable Transsion HyperCharge & Bypass Charge flags
  for flag in \
    /sys/devices/platform/charger/fast_chg_en \
    /sys/devices/platform/charger/hyper_charge \
    /sys/class/power_supply/battery/hyper_charging \
    /sys/class/power_supply/battery/tran_bypass_chg \
    /sys/devices/platform/tran_battery/bypass_charge \
    /sys/devices/platform/tran_charger/boost_mode
  do
    if [ -f "$flag" ]; then
      chmod 664 "$flag" 2>/dev/null
      echo 1 > "$flag" 2>/dev/null
    fi
  done

  # 3. Disable charging thermal throttle ceiling
  for therm in \
    /sys/class/power_supply/battery/charge_control_limit \
    /sys/class/power_supply/battery/thermal_input_current_limit \
    /sys/class/power_supply/battery/system_temp_level
  do
    if [ -f "$therm" ]; then
      chmod 664 "$therm" 2>/dev/null
      echo 0 > "$therm" 2>/dev/null
    fi
  done

  sleep 15
done
