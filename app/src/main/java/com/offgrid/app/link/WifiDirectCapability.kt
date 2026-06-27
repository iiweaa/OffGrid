package com.offgrid.app.link

enum class ConcurrencyStatus {
    UNKNOWN,
    SUPPORTED,
    UNSUPPORTED
}

data class WifiDirectCapability(
    val concurrency: ConcurrencyStatus,
    val source: String
) {
    val multiHopAvailable: Boolean
        get() = concurrency == ConcurrencyStatus.SUPPORTED

    fun userMessage(): String = when (concurrency) {
        ConcurrencyStatus.SUPPORTED ->
            "当前设备支持 Wi-Fi Direct AP-STA 并发，多跳中继可用。"
        ConcurrencyStatus.UNSUPPORTED ->
            "当前设备不支持 Wi-Fi Direct AP-STA 并发，多跳中继已禁用。双机直连不受影响。"
        ConcurrencyStatus.UNKNOWN ->
            "无法通过公开 API 确认 AP-STA 并发能力，多跳中继暂不启用。双机直连不受影响。"
    }

    fun statusLabel(): String = when (concurrency) {
        ConcurrencyStatus.SUPPORTED -> "多跳：可用"
        ConcurrencyStatus.UNSUPPORTED -> "多跳：不支持"
        ConcurrencyStatus.UNKNOWN -> "多跳：未知"
    }
}
