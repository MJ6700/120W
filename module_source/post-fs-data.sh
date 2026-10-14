#!/system/bin/sh
MODDIR=${0%/*}
echo 0 > /sys/class/power_supply/battery/charge_control_limit 2>/dev/null
