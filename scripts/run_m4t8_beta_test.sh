#!/bin/bash
# M4-T8 Beta 版本测试执行脚本
# 用法：bash scripts/run_m4t8_beta_test.sh

set -euo pipefail

DEV_A="62978a15"
DEV_B="2KE0219B02018194"
APK="app/build/outputs/apk/debug/app-debug.apk"
LOG_DIR="logs/m4t8-$(date +%Y%m%d-%H%M%S)"
PKG="com.offgrid.app"
ACTIVITY="com.offgrid.app.MainActivity"

mkdir -p "$LOG_DIR"

logcat_pids=()

abort() {
  echo "❌ $1"
  exit 1
}

cleanup() {
  echo "🛑 停止 logcat..."
  for pid in "${logcat_pids[@]}"; do kill "$pid" 2>/dev/null || true; done
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

record_state() {
  local tag=$1
  echo "📊 记录状态 [$tag]..."
  adb -s "$DEV_A" shell dumpsys battery > "$LOG_DIR/battery-${DEV_A}-${tag}.txt"
  adb -s "$DEV_B" shell dumpsys battery > "$LOG_DIR/battery-${DEV_B}-${tag}.txt"
  adb -s "$DEV_A" shell dumpsys wifi > "$LOG_DIR/wifi-${DEV_A}-${tag}.txt"
  adb -s "$DEV_B" shell dumpsys wifi > "$LOG_DIR/wifi-${DEV_B}-${tag}.txt"
}

screenshot() {
  local serial=$1
  local name=$2
  local remote="/sdcard/m4t8_${serial}_${name}.png"
  local local="$LOG_DIR/screenshot-${serial}-${name}.png"
  adb -s "$serial" shell screencap -p "$remote"
  adb -s "$serial" pull "$remote" "$local" >/dev/null 2>&1
  adb -s "$serial" shell rm "$remote" 2>/dev/null || true
  echo "📸 截图已保存: $local"
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
  read -rp "完成后按 Enter 继续..."
}

# 主流程
echo "===== M4-T8 Beta 版本测试开始 ====="
echo "日志目录: $LOG_DIR"

check_device "$DEV_A"
check_device "$DEV_B"

install_apk "$DEV_A"
install_apk "$DEV_B"

start_logcat "$DEV_A"
start_logcat "$DEV_B"

trap cleanup EXIT

prompt "环境准备：
1. 两台设备已授予 OffGrid：位置、附近设备、录音、通知权限；
2. 关闭移动数据与 Wi-Fi；
3. 清理后台应用，仅保留 OffGrid；
4. 屏幕亮度固定 50%，关闭自动亮度。"

launch_app "$DEV_A"
launch_app "$DEV_B"

prompt "BC-1: 基础通话（GO + Client）
设备 A($DEV_A): Settings → Connection → Group Owner
设备 B($DEV_B): Settings → Connection → Client
A 进入 Call 点击 Start Call，B 进入 Call 点击 Start Call
双向通话 2 分钟，确认 Neighbors 显示对端，然后双方 End Call。"
record_state "bc1"
screenshot "$DEV_A" "bc1"
screenshot "$DEV_B" "bc1"

prompt "BC-2: 角色互换（Client + GO）
设备 A($DEV_A): Client
设备 B($DEV_B): Group Owner
B 先 Start Call，A 后 Start Call，确认通话正常。"
record_state "bc2"
screenshot "$DEV_A" "bc2"
screenshot "$DEV_B" "bc2"

prompt "BC-3: 锁屏/后台通话
按 BC-1 建立通话后，双方按电源键锁屏，保持 5 分钟。
确认语音不中断、无断连，然后点亮屏幕结束通话。"
record_state "bc3"
screenshot "$DEV_A" "bc3"
screenshot "$DEV_B" "bc3"

prompt "BC-4: 省电模式通话
Settings → Power → 开启 Power saving mode
按 BC-1 建立通话，持续 3 分钟，确认音质可接受、Neighbors 正常刷新。"
record_state "bc4"
screenshot "$DEV_A" "bc4"
screenshot "$DEV_B" "bc4"

prompt "BC-5: 长时间稳定性
按 BC-1 建立通话，保持 15 分钟。
观察是否有 ANR/Crash/断连。"
record_state "bc5-start"
sleep 900
record_state "bc5-end"
screenshot "$DEV_A" "bc5"
screenshot "$DEV_B" "bc5"

prompt "BC-6: 应用被杀后重连
按 BC-1 建立通话，然后在最近任务中上滑关闭 A 端应用。
确认 B 端邻居消失后，重新打开 A 端并重新建立 GO，B 端重新连接。"
record_state "bc6"
screenshot "$DEV_A" "bc6"
screenshot "$DEV_B" "bc6"

prompt "BC-7: 权限边界
依次撤销录音权限、位置权限，点击 Start Call，确认提示正确且不崩溃。
测试完成后重新授予权限。"
record_state "bc7"
screenshot "$DEV_A" "bc7"
screenshot "$DEV_B" "bc7"

prompt "BC-8: 音频外设（可选）
如有有线/蓝牙耳机，请测试插入/拔出/蓝牙连接时音频切换是否正常。
无设备请直接按 Enter。"
record_state "bc8"
screenshot "$DEV_A" "bc8"
screenshot "$DEV_B" "bc8"

cleanup
trap - EXIT

echo ""
echo "===== M4-T8 Beta 测试执行结束 ====="
echo "日志与截图目录: $LOG_DIR"
echo "请将结果回填到 docs/M4-T8_BETA_TEST.md 测试结果记录表。"
