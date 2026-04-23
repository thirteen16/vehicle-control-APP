package com.example.app.common

object UiStateTextResolver {

    fun resolveError(raw: String?): String {
        val msg = raw?.trim().orEmpty()
        if (msg.isBlank()) return "发生未知错误，请稍后重试"

        val lower = msg.lowercase()

        return when {
            "unable to resolve host" in lower ||
                    "failed to connect" in lower ||
                    "connection refused" in lower ||
                    "network is unreachable" in lower ->
                "无法连接服务器，请检查服务器地址、网络连接或后端是否已启动"

            "timeout" in lower ||
                    "timed out" in lower ||
                    "sockettimeoutexception" in lower ->
                "请求超时，请稍后重试"

            "cleartxt" in lower || "cleartext" in lower ->
                "当前地址使用的是 HTTP，请确认应用已允许明文流量或改用 HTTPS"

            "401" in lower || "未登录" in msg ->
                "登录状态已失效，请重新登录"

            "403" in lower || "无权限" in msg ->
                "当前账号无权限执行该操作"

            "404" in lower || "不存在" in msg ->
                "请求的资源不存在，请确认车辆或命令是否有效"

            "500" in lower || "internal server error" in lower ->
                "服务器内部错误，请检查后端日志"

            else -> msg
        }
    }

    fun homeEmptyMessage(): String {
        return "当前账号暂无绑定车辆，请先在后端为该用户分配车辆"
    }

    fun vehicleEmptyMessage(): String {
        return "当前没有可用车辆，请检查用户与车辆绑定关系"
    }

    fun historyEmptyMessage(
        isAllHistory: Boolean,
        resultFilterText: String
    ): String {
        return if (isAllHistory) {
            if (resultFilterText == "全部") {
                "当前账号暂无命令历史"
            } else {
                "当前账号在“$resultFilterText”筛选下暂无命令历史"
            }
        } else {
            if (resultFilterText == "全部") {
                "当前车辆暂无命令历史"
            } else {
                "当前车辆在“$resultFilterText”筛选下暂无命令历史"
            }
        }
    }

    fun disconnectedInfo(): String {
        return "实时通道未连接，页面仍可手动刷新数据"
    }
}