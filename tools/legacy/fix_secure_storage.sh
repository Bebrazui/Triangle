#!/system/bin/sh
# Triangle System Fix: SEPolicy patch for Qualcomm Secure Storage Daemon on Samsung GSI
magiskpolicy --live "permissive hal_secure_storage_daemon_default"
magiskpolicy --live "allow hal_secure_storage_daemon_default device dir { read write open add_name search create }"
magiskpolicy --live "allow hal_secure_storage_daemon_default device file { read write open create getattr }"
