#!/bin/bash
# M4-T6 真机功耗验证执行脚本
# 用法：bash scripts/run_m4t6_verification.sh

set -euo pipefail

DEV_A="62978a15"
DEV_B="2KE0219B02018194"
APK="app/build/outputs/apk/debug/app-debug.apk"
LOG_DIR="logs/m4t6-$(date +%Y%m%d-%H%M%S)"
PKG="com.offgrid.app"
ACTIVITY="com.offgrid.app.MainActivity"

mkdir -p "$LOG_DIR"

logcat_pids=()
battery_logger_pids=()

abort() {
  echo "❌ $1"
  exit 1
}

cleanup() {
  echo "🛑 停止后台记录..."
  for pid in "${logcat_pids[@]}"; do kill "$pid" 2>/dev/null || true; done
  for pid in "${battery_logger_pids[@]}"; do kill "$pid" 2>/dev/null || true; done
}

check_device() {
  local serial=$1
  adb -s "$serial" get-state >/dev/null 2>&1 || abort "设备 $serial 未连接"
  echo "✅ 设备 $serial 在线"
}

install_apk() {
  local serial=$1
  echo "📦 向 $serial 安装 APK..."
  adb -s "$serial" install -r "$APK" || abort "$serial 安装失败"
}

start_logcat() {
  local serial=$1
  local file="$LOG_DIR/logcat-${serial}.txt"
  echo "📝 启动 $serial logcat → $file"
  adb -s "$serial" logcat -c
  adb -s "$serial" logcat -v threadtime -T 0 > "$file" &
  logcat_pids+=("$!")
}

start_battery_logger() {
  local serial=$1
  local file="$LOG_DIR/battery-${serial}.log"
  echo "🔋 启动 $serial 电量记录 → $file"
  (
    echo "timestamp,level,status,ac_powered,usb_powered"
    while true; do
      local ts
      ts=$(date +%Y-%m-%dT%H:%M:%S)
      local info
      info=$(adb -s "$serial" shell dumpsys battery)
      local level status ac usb
      level=$(echo "$info" | grep -E '^  level:' | awk '{print $2}')
      status=$(echo "$info" | grep -E '^  status:' | awk '{print $2}')
      ac=$(echo "$info" | grep -E '^  AC powered:' | awk '{print $3}')
      usb=$(echo "$info" | grep -E '^  USB powered:' | awk '{print $3}')
      echo "$ts,$level,$status,$ac,$usb" >> "$file"
      sleep 30
    done
  ) &
  battery_logger_pids+=("$!")
}

record_battery() {
  local serial=$1
  local tag=$2
  adb -s "$serial" shell dumpsys battery > "$LOG_DIR/battery-${serial}-${tag}.txt"
}

launch_app() {
  local serial=$1
  echo "🚀 在 $serial 启动应用"
  adb -s "$serial" shell am start -n "$PKG/$ACTIVITY"
}

prompt() {
  echo ""
  echo "────────────────────────────────────────"
  echo "$1"
  echo "────────────────────────────────────────"
  read -rp "准备好后按 Enter 继续..."
}

# 主流程
echo "===== M4-T6 真机功耗验证开始 ====="
echo "日志目录: $LOG_DIR"

check_device "$DEV_A"
check_device "$DEV_B"

install_apk "$DEV_A"
install_apk "$DEV_B"

start_logcat "$DEV_A"
start_logcat "$DEV_B"
start_battery_logger "$DEV_A"
start_battery_logger "$DEV_B"

trap cleanup EXIT

prompt "环境准备：
1. 两台设备电量均 ≥ 50%（当前 A=$DEV_A，B=$DEV_B）；
2. 屏幕亮度固定 50%，关闭自动亮度；
3. 关闭移动数据与 Wi-Fi；
4. 清理后台应用，仅保留 OffGrid；
5. 手动授予 OffGrid：位置、附近设备、录音、通知权限。"

launch_app "$DEV_A"
launch_app "$DEV_B"

# 普通模式 3 轮
for round in 1 2 3; do
  prompt "普通模式 第 ${round} 轮
1. 设备 A($DEV_A): Settings → Connection → Group Owner；
2. 设备 B($DEV_B): Settings → Connection → Client；
3. Settings → Power → 关闭 Power saving mode；
4. 双方建立通话，屏幕常亮，持续 15 分钟。"
  record_battery "$DEV_A" "normal-${round}-start"
  record_battery "$DEV_B" "normal-${round}-start"
  echo "⏱️  请进行 15 分钟通话，结束后按 Enter..."
  read -rp ""
  record_battery "$DEV_A" "normal-${round}-end"
  record_battery "$DEV_B" "normal-${round}-end"
  prompt "普通模式 第 ${round} 轮结束，请结束通话并静置 2 分钟。"
done

# 省电模式 3 轮
for round in 1 2 3; do
  prompt "省电模式 第 ${round} 轮
1. 设备 A($DEV_A): Settings → Connection → Group Owner；
2. 设备 B($DEV_B): Settings → Connection → Client；
3. Settings → Power → 开启 Power saving mode；
4. 双方建立通话，屏幕常亮，持续 15 分钟。"
  record_battery "$DEV_A" "power-${round}-start"
  record_battery "$DEV_B" "power-${round}-start"
  echo "⏱️  请进行 15 分钟通话，结束后按 Enter..."
  read -rp ""
  record_battery "$DEV_A" "power-${round}-end"
  record_battery "$DEV_B" "power-${round}-end"
  prompt "省电模式 第 ${round} 轮结束，请结束通话并静置 2 分钟。"
done

cleanup
trap - EXIT

echo ""
echo "===== M4-T6 验证执行结束 ====="
echo "日志目录: $LOG_DIR"
echo "请将各轮起始/结束电量回填到 docs/M4-T6_VERIFICATION.md 数据记录表。"
