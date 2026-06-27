#!/bin/bash
# M4-T5 真机网络验证执行脚本
# 用法：bash scripts/run_m4t5_verification.sh

set -euo pipefail

DEV_A="62978a15"
DEV_B="2KE0219B02018194"
APK="app/build/outputs/apk/debug/app-debug.apk"
LOG_DIR="logs/m4t5-$(date +%Y%m%d-%H%M%S)"
PKG="com.offgrid.app"
ACTIVITY="com.offgrid.app.MainActivity"

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

screenshot() {
  local serial=$1
  local name=$2
  local remote="/sdcard/m4t5_${serial}_${name}.png"
  local local="$LOG_DIR/screenshot-${serial}-${name}.png"
  adb -s "$serial" shell screencap -p "$remote"
  adb -s "$serial" pull "$remote" "$local" >/dev/null 2>&1
  adb -s "$serial" shell rm "$remote" 2>/dev/null || true
  echo "📸 截图已保存: $local"
}

record_state() {
  local tag=$1
  echo "📊 记录状态 [$tag]..."
  adb -s "$DEV_A" shell dumpsys battery > "$LOG_DIR/battery-${DEV_A}-${tag}.txt"
  adb -s "$DEV_B" shell dumpsys battery > "$LOG_DIR/battery-${DEV_B}-${tag}.txt"
  adb -s "$DEV_A" shell dumpsys wifi > "$LOG_DIR/wifi-${DEV_A}-${tag}.txt"
  adb -s "$DEV_B" shell dumpsys wifi > "$LOG_DIR/wifi-${DEV_B}-${tag}.txt"
  screenshot "$DEV_A" "$tag"
  screenshot "$DEV_B" "$tag"
}

prompt() {
  echo ""
  echo "────────────────────────────────────────"
  echo "$1"
  echo "────────────────────────────────────────"
  read -rp "完成后按 Enter 继续..."
}

# 主流程
echo "===== M4-T5 真机网络验证开始 ====="
echo "日志目录: $LOG_DIR"

check_device "$DEV_A"
check_device "$DEV_B"

install_apk "$DEV_A"
install_apk "$DEV_B"

start_logcat "$DEV_A"
start_logcat "$DEV_B"

trap stop_logcat EXIT

prompt "请在两台设备上手动授予 OffGrid：位置、附近设备、录音、通知权限。"

launch_app "$DEV_A"
launch_app "$DEV_B"

prompt "TC-1: GO + Client 模式
设备 A($DEV_A): Settings → Connection → Group Owner
设备 B($DEV_B): Settings → Connection → Client
设备 A 进入 Call 点击 Start Call，等待 Group 创建。
设备 B 进入 Call 点击 Start Call，等待连接成功。
双方通话 1 分钟，观察 Neighbors 列表与语音质量。"
record_state "tc1"

prompt "TC-2: Client + GO 模式
设备 A($DEV_A): Client
设备 B($DEV_B): Group Owner
设备 B 先 Start Call，设备 A 后 Start Call，确认通话正常。"
record_state "tc2"

prompt "TC-3: Auto + Auto 模式
两台设备均设为 Auto，同时点击 Start Call，确认最终通话正常。"
record_state "tc3"

prompt "TC-4: 故障提示验证
依次撤销权限、Client 未发现 GO、GO 创建受限，确认提示文案正确且不崩溃。"
record_state "tc4"

stop_logcat
trap - EXIT

echo ""
echo "===== M4-T5 验证执行结束 ====="
echo "日志与截图目录: $LOG_DIR"
echo "请将结果回填到 docs/M4-T5_VERIFICATION.md 测试结果记录表。"
