package com.example.app.ui.command

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.app.data.model.response.CommandHistoryItemResponse

class CommandResultDialog : DialogFragment() {

    companion object {
        private const val ARG_COMMAND_ID = "arg_command_id"
        private const val ARG_VEHICLE_ID = "arg_vehicle_id"
        private const val ARG_TYPE = "arg_type"
        private const val ARG_RESULT = "arg_result"
        private const val ARG_REQUEST_TIME = "arg_request_time"
        private const val ARG_RESPONSE_TIME = "arg_response_time"
        private const val ARG_REQUEST_PAYLOAD = "arg_request_payload"
        private const val ARG_RESPONSE_PAYLOAD = "arg_response_payload"

        fun newInstance(item: CommandHistoryItemResponse): CommandResultDialog {
            return CommandResultDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_COMMAND_ID, item.commandId.orEmpty())
                    putString(ARG_VEHICLE_ID, item.vehicleId.orEmpty())
                    putString(ARG_TYPE, item.type.orEmpty())
                    putString(ARG_RESULT, item.result.orEmpty())
                    putString(ARG_REQUEST_TIME, item.requestTime.orEmpty())
                    putString(ARG_RESPONSE_TIME, item.responseTime.orEmpty())
                    putString(ARG_REQUEST_PAYLOAD, item.requestPayload.orEmpty())
                    putString(ARG_RESPONSE_PAYLOAD, item.responsePayload.orEmpty())
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()

        val message = buildString {
            append("commandId：${args.getString(ARG_COMMAND_ID).orEmpty()}\n\n")
            append("vehicleId：${args.getString(ARG_VEHICLE_ID).orEmpty()}\n")
            append("命令类型：${commandDisplayName(args.getString(ARG_TYPE).orEmpty())}\n")
            append("执行结果：${formatResult(args.getString(ARG_RESULT).orEmpty())}\n")
            append("请求时间：${args.getString(ARG_REQUEST_TIME).orEmpty()}\n")
            append("响应时间：${args.getString(ARG_RESPONSE_TIME).orEmpty()}\n\n")
            append("请求载荷：\n${args.getString(ARG_REQUEST_PAYLOAD).orEmpty()}\n\n")
            append("响应载荷：\n${args.getString(ARG_RESPONSE_PAYLOAD).orEmpty()}")
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("命令详情")
            .setMessage(message)
            .setPositiveButton("知道了", null)
            .create()
    }

    private fun commandDisplayName(type: String?): String {
        return when (type) {
            "LOCK_ON" -> "关闭车锁"
            "LOCK_OFF" -> "打开车锁"
            "HVAC_ON" -> "开启空调"
            "HVAC_OFF" -> "关闭空调"
            "WINDOW_OPEN" -> "打开车窗"
            "WINDOW_CLOSE" -> "关闭车窗"
            "ENGINE_ON" -> "启动发动机"
            "ENGINE_OFF" -> "关闭发动机"
            "STATUS_QUERY" -> "状态查询"
            else -> type ?: "-"
        }
    }

    private fun formatResult(result: String?): String {
        return if (result.equals("SUCCESS", ignoreCase = true)) {
            "成功"
        } else {
            "超时"
        }
    }
}