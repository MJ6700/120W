ui_print "**********************************************"
ui_print "   ⚡ INFINIX GT 50 PRO CHARGING MOD ⚡      "
ui_print "       Hot 60 Pro -> GT 50 Pro FastCharge     "
ui_print "**********************************************"
ui_print "- Target Device: Infinix Hot 60 Pro"
ui_print "- Target Profile: Infinix GT 50 Pro HyperCharge"
ui_print "- Max Charging Current: 12,000 mA (Dual Pump)"
ui_print "- Bypass Charging: Supported (Direct SoC)"
ui_print "- Thermal Throttle Clamp: Overridden"
ui_print "**********************************************"
ui_print "- Injecting system.prop properties..."
ui_print "- Installing service.sh background charger daemon..."
ui_print "- Setting executable permissions..."
set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/service.sh 0 0 0755
set_perm $MODPATH/post-fs-data.sh 0 0 0755
ui_print "- Installation complete! Reboot your Infinix device."
