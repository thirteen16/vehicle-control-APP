package com.example.app.ui.command

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.common.UiStateTextResolver
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.model.response.CommandHistoryItemResponse
import com.example.app.data.repository.CommandRepository
import com.example.app.di.NetworkModule
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommandHistoryFragment : Fragment(R.layout.fragment_command_history) {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvStateBanner: TextView
    private lateinit var tvCurrentFilter: TextView
    private lateinit var tvResultFilter: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var historyContainer: LinearLayout

    private lateinit var spTimeFilter: Spinner
    private lateinit var spResultFilter: Spinner

    private lateinit var viewModel: CommandHistoryViewModel

    private val expandedMonthKeys = mutableSetOf<String>()
    private val initializedMonthKeys = mutableSetOf<String>()
    private var ignoreSpinnerCallback = false

    private val timeFilterOptions = listOf(
        "全部" to CommandTimeFilter.ALL_TIME,
        "近一天" to CommandTimeFilter.LAST_DAY,
        "近一周" to CommandTimeFilter.LAST_WEEK,
        "近半年" to CommandTimeFilter.HALF_YEAR,
        "近一年" to CommandTimeFilter.ONE_YEAR
    )

    private val resultFilterOptions = listOf(
        "全部" to CommandResultFilter.ALL,
        "成功" to CommandResultFilter.SUCCESS,
        "超时" to CommandResultFilter.TIMEOUT
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContext = requireContext().applicationContext
        val tokenStore = NetworkModule.provideTokenStore(appContext)
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
        val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)
        val commandApi = NetworkModule.provideCommandApi(retrofit)
        val selectedVehicleStore = SelectedVehicleStore(appContext)
        val commandRepository = CommandRepository(commandApi, selectedVehicleStore)

        viewModel = ViewModelProvider(
            this,
            CommandHistoryViewModel.Factory(commandRepository)
        )[CommandHistoryViewModel::class.java]

        initViews(view)
        setupSpinners()
        observeUiState()
    }

    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.progressBar)
        tvStateBanner = view.findViewById(R.id.tvStateBanner)
        tvCurrentFilter = view.findViewById(R.id.tvCurrentFilter)
        tvResultFilter = view.findViewById(R.id.tvResultFilter)
        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        historyContainer = view.findViewById(R.id.historyContainer)

        spTimeFilter = view.findViewById(R.id.spTimeFilter)
        spResultFilter = view.findViewById(R.id.spResultFilter)
    }

    private fun setupSpinners() {
        val timeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            timeFilterOptions.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val resultAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            resultFilterOptions.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spTimeFilter.adapter = timeAdapter
        spResultFilter.adapter = resultAdapter

        spTimeFilter.setSelection(0, false)
        spResultFilter.setSelection(0, false)

        spTimeFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (ignoreSpinnerCallback) return
                viewModel.setTimeFilter(timeFilterOptions[position].second)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spResultFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (ignoreSpinnerCallback) return
                viewModel.setResultFilter(resultFilterOptions[position].second)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            tvCurrentFilter.text = if (state.isAllHistory) {
                "全部车辆"
            } else {
                state.selectedVehicleId ?: "未选择车辆"
            }

            tvResultFilter.text = displayFilterText(state.resultFilter)
            tvTotalCount.text = "当前共 ${state.items.size} 条结果"

            syncSpinnerSelection(state)
            renderStateBanner(state)
            renderHistoryGroups(state.items)
        }
    }

    private fun syncSpinnerSelection(state: CommandHistoryUiState) {
        ignoreSpinnerCallback = true

        val timeIndex = timeFilterOptions.indexOfFirst { it.second == state.timeFilter }
            .takeIf { it >= 0 } ?: 0

        val resultIndex = resultFilterOptions.indexOfFirst { it.second == state.resultFilter }
            .takeIf { it >= 0 } ?: 0

        if (spTimeFilter.selectedItemPosition != timeIndex) {
            spTimeFilter.setSelection(timeIndex, false)
        }

        if (spResultFilter.selectedItemPosition != resultIndex) {
            spResultFilter.setSelection(resultIndex, false)
        }

        ignoreSpinnerCallback = false
    }

    private fun renderStateBanner(state: CommandHistoryUiState) {
        when {
            !state.errorMessage.isNullOrBlank() -> {
                tvStateBanner.visibility = View.VISIBLE
                tvStateBanner.text = "加载失败：${UiStateTextResolver.resolveError(state.errorMessage)}"
                tvStateBanner.setBackgroundResource(R.drawable.bg_notice_error)
            }

            state.items.isEmpty() -> {
                tvStateBanner.visibility = View.VISIBLE
                tvStateBanner.text = "暂无历史记录"
                tvStateBanner.setBackgroundResource(R.drawable.bg_notice_empty)
            }

            !state.infoMessage.isNullOrBlank() -> {
                tvStateBanner.visibility = View.VISIBLE
                tvStateBanner.text = state.infoMessage
                tvStateBanner.setBackgroundResource(R.drawable.bg_notice_info)
            }

            else -> {
                tvStateBanner.visibility = View.GONE
            }
        }
    }

    private fun renderHistoryGroups(items: List<CommandHistoryItemResponse>) {
        historyContainer.removeAllViews()

        if (items.isEmpty()) {
            return
        }

        val sortedItems = items.sortedByDescending {
            parseDisplayDate(historyTimeText(it))?.time ?: Long.MIN_VALUE
        }

        val groupedItems = sortedItems.groupBy { monthKey(historyTimeText(it)) }
        val currentMonthKeys = groupedItems.keys.toSet()

        initializedMonthKeys.retainAll(currentMonthKeys)
        expandedMonthKeys.retainAll(currentMonthKeys)

        currentMonthKeys.forEach { month ->
            if (!initializedMonthKeys.contains(month)) {
                initializedMonthKeys.add(month)
                expandedMonthKeys.add(month)
            }
        }

        groupedItems.forEach { entry ->
            val month = entry.key
            val monthItems = entry.value
            val isExpanded = expandedMonthKeys.contains(month)

            val groupHeader = createMonthHeader(
                month = month,
                count = monthItems.size,
                expanded = isExpanded
            )

            groupHeader.setOnClickListener {
                if (expandedMonthKeys.contains(month)) {
                    expandedMonthKeys.remove(month)
                } else {
                    expandedMonthKeys.add(month)
                }

                renderHistoryGroups(items)
            }

            historyContainer.addView(groupHeader)

            if (isExpanded) {
                monthItems.forEach { item ->
                    historyContainer.addView(createHistoryItemView(item))
                }
            }
        }
    }

    private fun createMonthHeader(
        month: String,
        count: Int,
        expanded: Boolean
    ): View {
        val context = requireContext()

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(10), dp(12))
            setBackgroundResource(R.drawable.bg_car_card)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(8)
            }
        }

        val countText = TextView(context).apply {
            text = "共 $count 条记录"
            textSize = 15f
            setTextColor(Color.parseColor("#7B8794"))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val monthText = TextView(context).apply {
            text = month
            textSize = 16f
            setTextColor(Color.parseColor("#1F2933"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }

        val arrowButton = TextView(context).apply {
            text = ">"
            textSize = 22f
            gravity = Gravity.CENTER
            rotation = if (expanded) 90f else 0f
            setTextColor(Color.parseColor("#1F2933"))
            setTypeface(null, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_car_soft_button)
            layoutParams = LinearLayout.LayoutParams(
                dp(38),
                dp(34)
            ).apply {
                leftMargin = dp(10)
            }
        }

        header.addView(countText)
        header.addView(monthText)
        header.addView(arrowButton)

        return header
    }

    private fun createHistoryItemView(item: CommandHistoryItemResponse): View {
        val inflater = LayoutInflater.from(requireContext())
        val itemView = inflater.inflate(R.layout.item_command_history, historyContainer, false)

        val root = itemView.findViewById<View>(R.id.rootHistoryItem)
        val tvType = itemView.findViewById<TextView>(R.id.tvType)
        val tvResultBadge = itemView.findViewById<TextView>(R.id.tvResultBadge)
        val tvVehicleId = itemView.findViewById<TextView>(R.id.tvVehicleId)
        val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
        val tvDetailHint = itemView.findViewById<TextView>(R.id.tvDetailHint)

        val rawTime = historyTimeText(item)

        tvType.text = commandDisplayName(item.type)
        tvResultBadge.text = formatResult(item.result)
        tvVehicleId.text = item.vehicleId ?: "-"
        tvTime.text = formatDayMinute(rawTime)
        tvDetailHint.text = ""

        applyResultBadgeStyle(tvResultBadge, item.result)

        root.setOnClickListener {
            CommandResultDialog.newInstance(item)
                .show(childFragmentManager, "command_result_dialog")
        }

        return itemView
    }

    private fun historyTimeText(item: CommandHistoryItemResponse): String? {
        return listOf(
            item.requestTime,
            item.createdTime,
            item.responseTime,
            item.updatedTime
        ).firstOrNull { !it.isNullOrBlank() }
    }

    private fun monthKey(raw: String?): String {
        val directMonth = extractMonth(raw)
        if (!directMonth.isNullOrBlank()) {
            return directMonth
        }

        val date = parseDisplayDate(raw)
        if (date != null) {
            return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date)
        }

        return "未知时间"
    }

    private fun formatDayMinute(raw: String?): String {
        val directTime = extractMonthDayMinute(raw)
        if (!directTime.isNullOrBlank()) {
            return directTime
        }

        val date = parseDisplayDate(raw)
        if (date != null) {
            return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date)
        }

        return raw ?: "-"
    }

    private fun extractMonth(raw: String?): String? {
        if (raw.isNullOrBlank()) {
            return null
        }

        val text = raw.trim()
        val regex = Regex("""(\d{4})[-/](\d{1,2})""")
        val match = regex.find(text) ?: return null

        val year = match.groupValues[1]
        val month = match.groupValues[2].padStart(2, '0')

        return "$year-$month"
    }

    private fun extractMonthDayMinute(raw: String?): String? {
        if (raw.isNullOrBlank()) {
            return null
        }

        val text = raw.trim()

        val regex = Regex(
            """\d{4}[-/](\d{1,2})[-/](\d{1,2})(?:[T\s]+)(\d{1,2}):(\d{1,2})"""
        )

        val match = regex.find(text) ?: return null

        val month = match.groupValues[1].padStart(2, '0')
        val day = match.groupValues[2].padStart(2, '0')
        val hour = match.groupValues[3].padStart(2, '0')
        val minute = match.groupValues[4].padStart(2, '0')

        return "$month-$day $hour:$minute"
    }

    private fun parseDisplayDate(raw: String?): Date? {
        if (raw.isNullOrBlank()) {
            return null
        }

        val text = raw.trim()

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "yyyy/MM/dd"
        )

        for (pattern in patterns) {
            try {
                val formatter = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    isLenient = false
                }

                val position = ParsePosition(0)
                val date = formatter.parse(text, position)

                if (date != null && position.index == text.length) {
                    return date
                }
            } catch (_: Exception) {
            }
        }

        return null
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

    private fun applyResultBadgeStyle(textView: TextView, result: String?) {
        if (result.equals("SUCCESS", ignoreCase = true)) {
            textView.setBackgroundResource(R.drawable.bg_status_success)
            textView.setTextColor(Color.parseColor("#159A86"))
        } else {
            textView.setBackgroundResource(R.drawable.bg_status_failed)
            textView.setTextColor(Color.parseColor("#F04438"))
        }
    }

    private fun displayFilterText(filter: CommandResultFilter): String {
        return when (filter) {
            CommandResultFilter.ALL -> "全部"
            CommandResultFilter.SUCCESS -> "成功"
            CommandResultFilter.TIMEOUT -> "超时"
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }
}