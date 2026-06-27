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
DURATION_SEC=900  # 15 分钟

mkdir -p "$LOG_DIR"

logcat_pids=()

abort() {
  echo "❌ $1"
  exit 1
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

read_battery() {
  local serial=$1
  adb -s "$serial" shell dumpsys battery | grep -E "level|status|scale" | tr -d '\r' | head -n 3
}

log_battery() {
  local serial=$1
  local tag=$2
  local file="$LOG_DIR/battery-${serial}.csv"
  local ts
  ts=$(date '+%Y-%m-%d %H:%M:%S')
  local level
  level=$(adb -s "$serial" shell dumpsys battery | grep level | awk '{print $2}' | tr -d '\r')
  echo "$ts,$tag,$level" >> "$file"
  echo "  [$serial][$tag] level=$level%"
}

start_logcat() {
  local serial=$1
  local file="$LOG_DIR/logcat-${serial}.txt"
  echo "📝 启动 $serial logcat → $file"
  adb -s "$serial" logcat -c
  adb -s "$serial" logcat -v threadtime -T 0 > "$file" &
  logcat_pids+=("$!")
}

stop_logcat() {
  echo "🛑 停止 logcat..."
  for pid in "${logcat_pids[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
}

launch_app() {
  local serial=$1
  echo "🚀 在 $serial 启动应用"
  adb -s "$serial" shell am start -n "$PKG/$ACTIVITY"
}

run_round() {
  local mode=$1  # normal / power_saving
  local round=$2
  echo ""
  echo "===== 第 $round 轮 ${mode} 模式 ====="

  # 记录起始电量
  echo "🔋 起始电量:"
  log_battery "$DEV_A" "${mode}_start_r${round}"
  log_battery "$DEV_B" "${mode}_start_r${round}"

  read -rp "请确认：两台设备屏幕亮度 50%、关闭自动亮度、仅保留 Wi-Fi Direct，已设置 ${mode} 模式。按 Enter 开始 15 分钟通话..."

  launch_app "$DEV_A"
  launch_app "$DEV_B"

  echo "⏱️  开始计时 15 分钟..."
  local end
  end=$(($(date +%s) + DURATION_SEC))
  while [ $(date +%s) -lt "$end" ]; do
    local remaining=$((end - $(date +%s)))
    echo -ne "剩余 ${remaining}s\r"
    sleep 30
    log_battery "$DEV_A" "${mode}_mid_r${round}"
    log_battery "$DEV_B" "${mode}_mid_r${round}"
  done

  echo ""
  echo "🔋 结束电量:"
  log_battery "$DEV_A" "${mode}_end_r${round}"
  log_battery "$DEV_B" "${mode}_end_r${round}"

  adb -s "$DEV_A" shell am force-stop "$PKG" 2>/dev/null || true
  adb -s "$DEV_B" shell am force-stop "$PKG" 2>/dev/null || true
  echo "静置 2 分钟..."
  sleep 120
}

# 主流程
echo "===== M4-T6 真机功耗验证开始 ====="
echo "日志目录: $LOG_DIR"

# 初始化 CSV
for s in "$DEV_A" "$DEV_B"; do
  echo "timestamp,tag,level" > "$LOG_DIR/battery-${s}.csv"
done

check_device "$DEV_A"
check_device "$DEV_B"

install_apk "$DEV_A"
install_apk "$DEV_B"

start_logcat "$DEV_A"
start_logcat "$DEV_B"
trap stop_logcat EXIT

echo ""
echo "⚠️  请先在两台设备上手动授予 OffGrid 所需权限。"
read -rp "完成后按 Enter 继续..."

# 普通模式 3 轮
for r in 1 2 3; do
  run_round "normal" "$r"
done

# 省电模式 3 轮
for r in 1 2 3; do
  run_round "power_saving" "$r"
done

stop_logcat
trap - EXIT

echo ""
echo "===== M4-T6 功耗验证执行结束 ====="
echo "日志目录: $LOG_DIR"
echo "请根据 battery CSV 计算普通/省电模式平均耗电，并回填 docs/M4-T6_VERIFICATION.md。"
