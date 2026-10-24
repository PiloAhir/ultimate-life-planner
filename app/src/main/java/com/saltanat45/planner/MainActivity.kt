package com.saltanat45.planner

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.RectF
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import kotlin.math.max

class MainActivity : Activity() {

    private val green = Color.rgb(24, 65, 48)
    private val gold = Color.rgb(218, 176, 93)
    private val cream = Color.rgb(246, 245, 238)
    private val dark = Color.rgb(35, 43, 39)
    private val muted = Color.rgb(110, 120, 112)
    private val white = Color.WHITE

    private val goals = mutableListOf<String>()
    private val tasks = mutableListOf<JSONObject>()
    private val missionYears = mutableListOf<JSONObject>()

    private lateinit var goalCount: TextView
    private lateinit var taskCount: TextView
    private lateinit var missionCount: TextView

    private val imageRequest = 1003
    private val videoRequest = 1004
    private var pendingMediaTaskIndex = -1

    private val statuses = arrayOf(
        "શરૂ કરવાનું બાકી",
        "ચાલુ છે",
        "પૂર્ણ થયું",
        "થોભાવ્યું"
    )

    private val moneyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadGoals()
        loadTasks()
        loadMission()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cream)
        }

        val header = TextView(this).apply {
            text = "👑 SALTANAT 45\nMy Life Blueprint"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(white)
            setBackgroundColor(green)
            setPadding(20, 32, 20, 32)
        }
        root.addView(header)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 12, 18, 24)
        }

        addText(content, "તમારા સપનાની સફર અહીંથી શરૂ થાય છે", 18f, green)

        missionCount = makeCountText()
        goalCount = makeCountText()
        taskCount = makeCountText()

        content.addView(missionCount)
        content.addView(goalCount)
        content.addView(taskCount)

        updateCounts()

        addButton(content, "📊 ડેશબોર્ડ અને ગ્રાફ્સ") {
            showDashboard()
        }

        addButton(content, "👑 18 વર્ષનું મિશન") {
            showMissionYears()
        }

        addButton(content, "🎯 મારા લક્ષ્યો") {
            showGoals()
        }

        addButton(content, "➕ નવું કામ ઉમેરો") {
            addTaskDialog()
        }

        addButton(content, "✅ મારાં કામ અને પ્રોગ્રેસ") {
            showTasks()
        }

        addButton(content, "💰 કુલ ખર્ચ") {
            showTotalCost()
        }

        addButton(content, "📔 નોંધો અને અનુભવો") {
            showNotes()
        }

        addText(content, "18 વર્ષનું આયોજન. એક સમયે એક પગલું.", 14f, green)

        val scroll = ScrollView(this)
        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun makeCountText() = TextView(this).apply {
        textSize = 16f
        setTextColor(green)
        setPadding(4, 8, 4, 8)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun prefs() = getSharedPreferences(
        "saltanat45_data",
        Context.MODE_PRIVATE
    )

    // ==================== ડેશબોર્ડ ====================

    private fun showDashboard() {
        val missionProgress = if (missionYears.isEmpty()) 0 else
            missionYears.sumOf {
                it.optInt("progress", 0).coerceIn(0, 100)
            } / missionYears.size

        val taskProgress = if (tasks.isEmpty()) 0 else
            tasks.sumOf {
                it.optInt("progress", 0).coerceIn(0, 100)
            } / tasks.size

        val completedMission = missionYears.count {
            it.optString("status") == "પૂર્ણ થયું"
        }

        val completedTasks = tasks.count {
            it.optString("status") == "પૂર્ણ થયું"
        }

        val runningTasks = tasks.count {
            it.optString("status") == "ચાલુ છે"
        }

        val pendingTasks = tasks.count {
            it.optString("status") == "શરૂ કરવાનું બાકી"
        }

        val pausedTasks = tasks.count {
            it.optString("status") == "થોભાવ્યું"
        }

        val taskCost = tasks.sumOf {
            it.optDouble("cost", 0.0).coerceAtLeast(0.0)
        }

        val missionCost = missionYears.sumOf {
            it.optDouble("cost", 0.0).coerceAtLeast(0.0)
        }

        val totalCost = taskCost + missionCost

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(20))
            setBackgroundColor(cream)
        }

        addText(panel, "👑 SALTANAT 45 DASHBOARD", 22f, green)
        addText(panel, "તમારા 18 વર્ષના મિશનની એક નજરમાં સ્થિતિ", 14f, muted)

        // ચાર મુખ્ય કાર્ડ
        val rowOne = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        rowOne.addView(
            dashboardCard("🎯 લક્ષ્યો", goals.size.toString(), "કુલ લક્ષ્યો"),
            weightParams()
        )
        rowOne.addView(
            dashboardCard("✅ કામ", tasks.size.toString(), "કુલ કામ"),
            weightParams()
        )
        panel.addView(rowOne)

        val rowTwo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        rowTwo.addView(
            dashboardCard(
                "🏁 પૂર્ણ કામ",
                completedTasks.toString(),
                "કામ પૂર્ણ"
            ),
            weightParams()
        )
        rowTwo.addView(
            dashboardCard(
                "👑 મિશન",
                "$missionProgress%",
                "$completedMission / 18 વર્ષ પૂર્ણ"
            ),
            weightParams()
        )
        panel.addView(rowTwo)

        addText(panel, "📈 મિશનની એકંદર પ્રગતિ", 18f, green)
        panel.addView(ProgressBarView(missionProgress))

        addText(
            panel,
            "વર્ષવાર પ્રગતિનો ગ્રાફ",
            18f,
            green
        )

        val progressValues = missionYears.map {
            it.optInt("progress", 0).coerceIn(0, 100).toFloat()
        }

        val yearLabels = missionYears.map {
            "વ${it.optInt("year")}"
        }

        panel.addView(
            chartInHorizontalScroll(
                values = progressValues,
                labels = yearLabels,
                chartTitle = "પ્રગતિ (%)",
                percentMode = true
            )
        )

        addText(panel, "💰 ખર્ચની સ્થિતિ", 18f, green)

        addText(
            panel,
            "સામાન્ય કામનો ખર્ચ: ₹${moneyFormat.format(taskCost)}",
            15f,
            dark
        )

        addText(
            panel,
            "18 વર્ષના મિશનનો ખર્ચ: ₹${moneyFormat.format(missionCost)}",
            15f,
            dark
        )

        addText(
            panel,
            "કુલ નોંધાયેલ ખર્ચ: ₹${moneyFormat.format(totalCost)}",
            17f,
            green
        )

        addText(
            panel,
            "વર્ષ પ્રમાણે મિશનનો ખર્ચ",
            17f,
            green
        )

        val expenseValues = missionYears.map {
            it.optDouble("cost", 0.0).coerceAtLeast(0.0).toFloat()
        }

        panel.addView(
            chartInHorizontalScroll(
                values = expenseValues,
                labels = yearLabels,
                chartTitle = "ખર્ચ (₹)",
                percentMode = false
            )
        )

        addText(panel, "📋 કામની સ્થિતિ", 18f, green)

        panel.addView(
            dashboardCard(
                "🟡 ચાલુ છે",
                runningTasks.toString(),
                "સામાન્ય કામ"
            )
        )

        panel.addView(
            dashboardCard(
                "⚪ બાકી છે",
                pendingTasks.toString(),
                "શરૂ કરવાનું બાકી"
            )
        )

        panel.addView(
            dashboardCard(
                "🟢 પૂર્ણ થયું",
                completedTasks.toString(),
                "સામાન્ય કામ"
            )
        )

        panel.addView(
            dashboardCard(
                "⏸️ થોભાવ્યું",
                pausedTasks.toString(),
                "હાલ અટકાવેલાં કામ"
            )
        )

        addText(
            panel,
            "18 વર્ષના મિશનની પ્રગતિ: $missionProgress%",
            16f,
            green
        )

        addText(
            panel,
            "સામાન્ય કામની સરેરાશ પ્રગતિ: $taskProgress%",
            16f,
            green
        )

        addText(
            panel,
            "નોંધ: ગ્રાફમાં દેખાતો ખર્ચ તમે એપમાં નોંધેલી રકમ પર આધારિત છે.",
            13f,
            muted
        )

        val scroll = ScrollView(this).apply {
            addView(panel)
        }

        AlertDialog.Builder(this)
            .setTitle("📊 ડેશબોર્ડ")
            .setView(scroll)
            .setPositiveButton("બંધ", null)
            .show()
    }

    private fun weightParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }

    private fun dashboardCard(
        title: String,
        value: String,
        subtitle: String
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(white)
        }

        val heading = TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(green)
        }

        val number = TextView(this).apply {
            text = value
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            setTextColor(green)
            setPadding(0, dp(5), 0, dp(4))
        }

        val detail = TextView(this).apply {
            text = subtitle
            textSize = 11f
            setTextColor(muted)
        }

        card.addView(heading)
        card.addView(number)
        card.addView(detail)

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dp(4), 0, dp(6))
        }

        card.layoutParams = params
        return card
    }

    private fun chartInHorizontalScroll(
        values: List<Float>,
        labels: List<String>,
        chartTitle: String,
        percentMode: Boolean
    ): HorizontalScrollView {
        val horizontal = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = true
            setBackgroundColor(white)
        }

        val chart = BarChartView(
            this,
            values,
            labels,
            chartTitle,
            percentMode
        )

        horizontal.addView(
            chart,
            HorizontalScrollView.LayoutParams(
                max(dp(900), dp(55) * values.size),
                dp(280)
            )
        )

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(280)
        ).apply {
            setMargins(0, dp(5), 0, dp(14))
        }

        horizontal.layoutParams = params
        return horizontal
    }

    // ==================== ગ્રાફ દોરવાનું કસ્ટમ View ====================

    private inner class BarChartView(
        context: Context,
        private val values: List<Float>,
        private val labels: List<String>,
        private val chartTitle: String,
        private val percentMode: Boolean
    ) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            setBackgroundColor(white)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val density = resources.displayMetrics.density
            val widthF = width.toFloat()
            val heightF = height.toFloat()

            paint.color = green
            paint.textSize = 14f * density
            paint.typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
            canvas.drawText(
                chartTitle,
                14f * density,
                22f * density,
                paint
            )

            if (values.isEmpty()) {
                paint.textSize = 14f * density
                paint.typeface = Typeface.DEFAULT
                canvas.drawText(
                    "હજુ કોઈ ડેટા ઉપલબ્ધ નથી",
                    14f * density,
                    60f * density,
                    paint
                )
                return
            }

            val left = 42f * density
            val right = widthF - 12f * density
            val top = 42f * density
            val bottom = heightF - 44f * density

            val graphHeight = bottom - top
            val graphWidth = right - left

            val maxValue = if (percentMode) {
                100f
            } else {
                max(1f, values.maxOrNull() ?: 1f)
            }

            // ચાર આડી માર્ગદર્શક રેખાઓ
            paint.strokeWidth = 1f * density
            paint.color = Color.rgb(225, 229, 222)
            paint.textSize = 10f * density
            paint.typeface = Typeface.DEFAULT

            for (step in 0..4) {
                val y = bottom - graphHeight * step / 4f

                canvas.drawLine(left, y, right, y, paint)

                val labelValue = maxValue * step / 4f
                val axisText = if (percentMode) {
                    "${labelValue.toInt()}%"
                } else {
                    compactMoney(labelValue.toDouble())
                }

                canvas.drawText(
                    axisText,
                    2f * density,
                    y + 4f * density,
                    paint
                )
            }

            val count = values.size
            val slot = graphWidth / count
            val barWidth = slot * 0.58f

            values.forEachIndexed { index, rawValue ->
                val value = rawValue.coerceAtLeast(0f)
                val barHeight =
                    (value / maxValue).coerceIn(0f, 1f) * graphHeight

                val centerX = left + slot * (index + 0.5f)
                val barTop = bottom - barHeight

                paint.color = if (value > 0f) gold else
                    Color.rgb(215, 219, 212)

                val rect = RectF(
                    centerX - barWidth / 2f,
                    barTop,
                    centerX + barWidth / 2f,
                    bottom
                )

                canvas.drawRoundRect(
                    rect,
                    4f * density,
                    4f * density,
                    paint
                )

                paint.color = dark
                paint.textSize = 9f * density
                paint.typeface = Typeface.DEFAULT

                if (percentMode && value > 0f) {
                    val valueText = "${value.toInt()}%"
                    canvas.drawText(
                        valueText,
                        centerX - paint.measureText(valueText) / 2f,
                        max(top + 11f * density, barTop - 4f * density),
                        paint
                    )
                }

                val label = labels.getOrNull(index) ?: ""
                val textWidth = paint.measureText(label)

                canvas.drawText(
                    label,
                    centerX - textWidth / 2f,
                    bottom + 18f * density,
                    paint
                )
            }
        }

        private fun compactMoney(value: Double): String {
            return when {
                value >= 10_000_000 ->
                    "₹${String.format("%.1f", value / 10_000_000)}Cr"

                value >= 100_000 ->
                    "₹${String.format("%.1f", value / 100_000)}L"

                value >= 1_000 ->
                    "₹${String.format("%.0f", value / 1_000)}k"

                else -> "₹${value.toInt()}"
            }
        }
    }

    private inner class ProgressBarView(
        context: Context,
        private val progress: Int
    ) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val density = resources.displayMetrics.density
            val left = 4f * density
            val top = 10f * density
            val right = width - 4f * density
            val barHeight = 24f * density
            val bottom = top + barHeight

            paint.color = Color.rgb(224, 226, 216)

            canvas.drawRoundRect(
                RectF(left, top, right, bottom),
                12f * density,
                12f * density,
                paint
            )

            val progressRight =
                left + (right - left) * progress.coerceIn(0, 100) / 100f

            if (progressRight > left) {
                paint.color = green

                canvas.drawRoundRect(
                    RectF(left, top, progressRight, bottom),
                    12f * density,
                    12f * density,
                    paint
                )
            }

            paint.color = dark
            paint.textSize = 13f * density
            paint.typeface = Typeface.DEFAULT_BOLD

            canvas.drawText(
                "$progress% પૂર્ણ",
                left,
                bottom + 22f * density,
                paint
            )
        }
    }

    // ==================== લક્ષ્યોનું સંગ્રહ ====================

    private fun loadGoals() {
        goals.clear()
        try {
            val array = JSONArray(prefs().getString("goals", "[]"))
            for (i in 0 until array.length()) {
                goals.add(array.getString(i))
            }
        } catch (_: Exception) {
            goals.clear()
        }
    }

    private fun saveGoals() {
        val array = JSONArray()
        goals.forEach { array.put(it) }
        prefs().edit().putString("goals", array.toString()).apply()
    }

    // ==================== કામનું સંગ્રહ ====================

    private fun loadTasks() {
        tasks.clear()
        try {
            val array = JSONArray(prefs().getString("tasks", "[]"))
            for (i in 0 until array.length()) {
                tasks.add(array.getJSONObject(i))
            }
        } catch (_: Exception) {
            tasks.clear()
        }
    }

    private fun saveTasks() {
        val array = JSONArray()
        tasks.forEach { array.put(it) }
        prefs().edit().putString("tasks", array.toString()).apply()
    }

    // ==================== 18 વર્ષનું મિશન ====================

    private fun loadMission() {
        missionYears.clear()
        val stored = prefs().getString("mission_years", null)

        if (stored != null) {
            try {
                val array = JSONArray(stored)
                for (i in 0 until array.length()) {
                    missionYears.add(array.getJSONObject(i))
                }
            } catch (_: Exception) {
                missionYears.clear()
            }
        }

        if (missionYears.size != 18) {
            missionYears.clear()
            missionYears.addAll(createDefaultMission())
            saveMission()
        }
    }

    private fun saveMission() {
        val array = JSONArray()
        missionYears.forEach { array.put(it) }
        prefs().edit().putString("mission_years", array.toString()).apply()
    }

    private fun createDefaultMission(): List<JSONObject> {
        val plans = listOf(
            Triple("પાયો અને પાવર રૂટ",
                "શિક્ષણ, કારકિર્દી, આવક અને બચતનો પાયો મજબૂત કરવો.",
                listOf("CTET અને TET-2 માટે તૈયારી", "શિક્ષણ ક્ષેત્રે કારકિર્દી વિકસાવવી", "Studymate📚 ચેનલનો વિકાસ", "આવક-ખર્ચ અને બચતનું આયોજન")),
            Triple("કારકિર્દી અને આર્થિક પાયો",
                "કૌશલ્ય, આવક અને લાંબા ગાળાની નાણાકીય યોજના.",
                listOf("કારકિર્દીની પ્રગતિની સમીક્ષા", "નિયમિત બચતનું લક્ષ્ય", "વધારાની આવકની શક્યતાઓ તપાસવી", "ખેતી માટે પ્રાથમિક બજેટ")),
            Triple("જમીન માટે તૈયારી",
                "જમીન, સ્થાન, પાણી અને ખર્ચ અંગે માહિતી એકત્ર કરવી.",
                listOf("જમીન ખરીદી અથવા ઉપલબ્ધ જમીનની સમીક્ષા", "જમીનના દસ્તાવેજોની ચકાસણી", "માટી અને પાણીની તપાસ", "18 વર્ષના પ્લાનનું બજેટ બનાવવું")),
            Triple("જમીન અને સીમાડા",
                "ખેતરનું માળખું અને જમીનની સુરક્ષા.",
                listOf("જમીનનો માપ અને નકશો તૈયાર કરવો", "સીમાડા અને પ્રવેશમાર્ગનું આયોજન", "સાગવાન અને અન્ય વૃક્ષોનું આયોજન", "ખેતી માટે જમીન તૈયાર કરવી")),
            Triple("ફળવાડી અને હરિયાળી",
                "ફળો, શાકભાજી અને ઉપયોગી વૃક્ષોની શરૂઆત.",
                listOf("સ્થાનિક આબોહવાને અનુકૂળ ફળવાડી", "શાકભાજી માટે જગ્યા", "મસાલા અને ઔષધીય છોડ", "છોડના વિકાસની નોંધ રાખવી")),
            Triple("ફાર્મહાઉસનો પાયો",
                "પરિવાર માટે ઘર અને બગીચાનું આયોજન.",
                listOf("ફાર્મહાઉસનો વ્યવહારુ નકશો", "હવા-ઉજાસ અને પાણીની વ્યવસ્થા", "બગીચો અને ખુલ્લી લોબી", "બાંધકામનું તબક્કાવાર બજેટ")),
            Triple("જળસંગ્રહ",
                "વરસાદી પાણીનો સંગ્રહ અને પાણીની સુરક્ષા.",
                listOf("જમીનના ઢાળ અને વરસાદનો અભ્યાસ", "તળાવ માટે યોગ્ય સ્થળ પસંદ કરવું", "તળાવની ક્ષમતા અને ખર્ચનો અંદાજ", "જરૂરી મંજૂરી અને સલામતીની તપાસ")),
            Triple("સિંચાઈ અને પાણી બચત",
                "ખેતીમાં પાણીનો કાર્યક્ષમ ઉપયોગ.",
                listOf("ડ્રિપ અથવા યોગ્ય સિંચાઈ પદ્ધતિ", "વરસાદી પાણીનો ઉપયોગ", "પાણીના વપરાશની નોંધ", "પાણી બચાવવાની વ્યવસ્થા")),
            Triple("બહુવિધ પાક અને સ્વાવલંબન",
                "અનાજ, કઠોળ, શાકભાજી અને ફળોની ખેતી.",
                listOf("ઋતુ પ્રમાણે પાકનું આયોજન", "અનાજ અને કઠોળ ઉગાડવાનો પ્રયાસ", "બીજ સંગ્રહની વ્યવસ્થા", "ખેતીના ખર્ચ અને ઉપજની નોંધ")),
            Triple("ડેરીનો પાયો",
                "પશુપાલન માટે જગ્યા અને વ્યવસ્થાની તૈયારી.",
                listOf("પશુશાળા અને પાણીની વ્યવસ્થા", "ચારા માટે જમીનનું આયોજન", "પશુચિકિત્સક અને ખર્ચની માહિતી", "ડેરીના નફા-નુકસાનનો અંદાજ")),
            Triple("ડેરીનો વિકાસ",
                "ક્ષમતા અને સંસાધનો પ્રમાણે ડેરી વિકસાવવી.",
                listOf("પશુઓની સંખ્યા બજેટ પ્રમાણે નક્કી કરવી", "દૂધ અને દૂધની બનાવટોની વ્યવસ્થા", "છાણમાંથી ખાતર બનાવવું", "પશુઓના આરોગ્યની નિયમિત સંભાળ")),
            Triple("ખેતરનું ચક્રિય અર્થતંત્ર",
                "ખેતરના કચરાનો ઉપયોગ અને ખર્ચ ઘટાડવો.",
                listOf("કમ્પોસ્ટ ખાતર", "બાયોગેસની શક્યતા તપાસવી", "પાણી અને ઊર્જાની બચત", "ખેતી અને ડેરીનો હિસાબ")),
            Triple("માળખાકીય વિકાસ",
                "ખેતરમાં જરૂરી રસ્તા અને સુવિધાઓ.",
                listOf("આંતરિક રસ્તાઓનું આયોજન", "સાધનો માટે સ્ટોરેજ", "સુરક્ષા અને પ્રકાશ વ્યવસ્થા", "મશીનરીની જરૂરિયાત અને ખર્ચ")),
            Triple("પરિવહન અને ટેકનોલોજી",
                "ખેતરની અંદરના પરિવહન અને નવી ટેકનોલોજી.",
                listOf("ખેતરના પરિવહન માટે વિકલ્પો", "ઇલેક્ટ્રિક વાહનના વિકલ્પો", "ટાવર અને માઇક્રો-રેલની શક્યતા તપાસવી", "એન્જિનિયરિંગ અને સલામતીનો અભ્યાસ")),
            Triple("સુવિધાઓનું એકીકરણ",
                "ઘર, ખેતર, બગીચો અને કામની વ્યવસ્થા જોડવી.",
                listOf("તમામ સુવિધાઓની સમીક્ષા", "જાળવણીનું વાર્ષિક બજેટ", "સ્વચ્છતા અને કચરા વ્યવસ્થાપન", "પરિવારની જરૂરિયાતો મુજબ સુધારા")),
            Triple("ઊર્જાની આત્મનિર્ભરતા",
                "સૌર ઊર્જા, બેટરી અને વીજળીનું આયોજન.",
                listOf("વીજળીની જરૂરિયાતનો હિસાબ", "સૌર ઊર્જાનો ખર્ચ અને ક્ષમતા", "બેટરી અને બેકઅપનું આયોજન", "પવન ઊર્જાની વ્યવહારુ શક્યતા")),
            Triple("સંપૂર્ણ ફાર્મહાઉસ વ્યવસ્થા",
                "ખોરાક, પાણી, ઊર્જા અને રહેઠાણનું સંકલન.",
                listOf("વર્ષભરનો ખોરાક ઉત્પાદન પ્લાન", "પાણી અને વીજળીની સમીક્ષા", "ઘર અને ખેતરની જાળવણી", "પરિવાર માટે આરામ અને બગીચો")),
            Triple("અંતિમ એકીકરણ",
                "બધી વ્યવસ્થાઓને એક સુવ્યવસ્થિત જીવનશૈલીમાં જોડવી.",
                listOf("18 વર્ષના લક્ષ્યોનું મૂલ્યાંકન", "બાકી રહેલા કામની પ્રાથમિકતા", "ટેલિસ્કોપ અને શોખ માટેની જગ્યા", "આગળનાં વર્ષો માટે જાળવણી યોજના", "પરિવાર સાથે મહાબળેશ્વરની સફરનું આયોજન"))
        )

        return plans.mapIndexed { index, plan ->
            JSONObject().apply {
                put("year", index + 1)
                put("title", plan.first)
                put("focus", plan.second)
                put("goals", JSONArray(plan.third))
                put("progress", 0)
                put("status", "શરૂ કરવાનું બાકી")
                put("cost", 0.0)
                put("notes", "")
            }
        }
    }

    private fun updateCounts() {
        goalCount.text = "🎯 કુલ લક્ષ્યો: ${goals.size}"
        taskCount.text = "✅ કુલ કામ: ${tasks.size}"

        val progress = if (missionYears.isEmpty()) 0 else
            missionYears.sumOf {
                it.optInt("progress", 0).coerceIn(0, 100)
            } / missionYears.size

        val completed = missionYears.count {
            it.optString("status") == "પૂર્ણ થયું"
        }

        missionCount.text =
            "👑 18 વર્ષનું મિશન: $progress% પ્રગતિ\nપૂર્ણ થયેલાં વર્ષ: $completed / 18"
    }

    private fun showMissionYears() {
        val items = missionYears.map { year ->
            "વર્ષ ${year.optInt("year")} — ${year.optString("title")}\n" +
                "${year.optString("status")} • પ્રગતિ ${year.optInt("progress")}%"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("👑 18 વર્ષનું મિશન")
            .setItems(items) { _, which -> editMissionYear(which) }
            .setPositiveButton("બંધ", null)
            .show()
    }

    private fun editMissionYear(index: Int) {
        if (index !in missionYears.indices) return
        val year = missionYears[index]

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 4)
        }

        val heading = TextView(this).apply {
            text = "વર્ષ ${year.optInt("year")} — ${year.optString("title")}\n\n${year.optString("focus")}"
            textSize = 16f
            setTextColor(green)
            setPadding(0, 8, 0, 12)
        }
        form.addView(heading)

        addText(form, "આ વર્ષનાં લક્ષ્યો (દરેક નવી લાઇનમાં એક)", 14f, green)

        val goalsInput = EditText(this).apply {
            hint = "દરેક લક્ષ્ય અલગ લાઇનમાં લખો"
            minLines = 4
            gravity = Gravity.TOP
            setText(jsonArrayToLines(year.optJSONArray("goals")))
        }
        form.addView(goalsInput)

        val progressInput = EditText(this).apply {
            hint = "પ્રગતિ 0 થી 100"
            inputType = 2
            setText(year.optInt("progress").toString())
        }
        form.addView(progressInput)

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )
        spinner.setSelection(statuses.indexOf(year.optString("status")).coerceAtLeast(0))
        form.addView(spinner)

        val costInput = EditText(this).apply {
            hint = "આ વર્ષનો ખર્ચ રૂપિયા"
            inputType = 8194
            setText(year.optDouble("cost", 0.0).toString())
        }
        form.addView(costInput)

        val notesInput = EditText(this).apply {
            hint = "નોંધો, અનુભવ અને આગળનું આયોજન"
            minLines = 3
            gravity = Gravity.TOP
            setText(year.optString("notes"))
        }
        form.addView(notesInput)

        val scroll = ScrollView(this)
        scroll.addView(form)

        val dialog = AlertDialog.Builder(this)
            .setTitle("વર્ષ ${year.optInt("year")} નું આયોજન")
            .setView(scroll)
            .setPositiveButton("સાચવો", null)
            .setNeutralButton("મૂળ લક્ષ્યો પાછાં લાવો", null)
            .setNegativeButton("બંધ", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val progress = progressInput.text.toString().toIntOrNull()
                val cost = costInput.text.toString().toDoubleOrNull()

                if (progress == null || progress !in 0..100) {
                    toast("પ્રગતિ 0 થી 100 વચ્ચે લખો")
                    return@setOnClickListener
                }

                if (cost == null || !cost.isFinite() || cost < 0) {
                    toast("ખર્ચની યોગ્ય રકમ લખો")
                    return@setOnClickListener
                }

                val goalLines = goalsInput.text.toString()
                    .lines().map { it.trim() }.filter { it.isNotEmpty() }

                year.put("goals", JSONArray(goalLines))
                year.put("progress", progress)
                year.put("status", spinner.selectedItem.toString())
                year.put("cost", cost)
                year.put("notes", notesInput.text.toString())

                saveMission()
                updateCounts()
                dialog.dismiss()
                toast("વર્ષનું આયોજન સાચવાયું ✅")
            }

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val defaultYear = createDefaultMission().getOrNull(index)
                if (defaultYear != null) {
                    year.put("goals", defaultYear.getJSONArray("goals"))
                    goalsInput.setText(jsonArrayToLines(defaultYear.getJSONArray("goals")))
                    toast("મૂળ લક્ષ્યો પાછાં મૂક્યાં છે. સાચવો દબાવો.")
                }
            }
        }

        dialog.show()
    }

    private fun jsonArrayToLines(array: JSONArray?): String {
        if (array == null) return ""
        val lines = mutableListOf<String>()
        for (i in 0 until array.length()) lines.add(array.optString(i))
        return lines.joinToString("\n")
    }

    // ==================== સામાન્ય લક્ષ્યો ====================

    private fun showGoals() {
        val input = EditText(this).apply { hint = "નવું લક્ષ્ય લખો" }

        val message = if (goals.isEmpty()) {
            "હજુ કોઈ લક્ષ્ય નથી."
        } else {
            goals.mapIndexed { i, goal -> "${i + 1}. $goal" }.joinToString("\n")
        }

        AlertDialog.Builder(this)
            .setTitle("🎯 મારા લક્ષ્યો")
            .setMessage(message)
            .setView(input)
            .setPositiveButton("ઉમેરો") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    goals.add(value)
                    saveGoals()
                    updateCounts()
                    toast("લક્ષ્ય સાચવાયું ✅")
                } else {
                    toast("લક્ષ્યનું નામ લખો")
                }
            }
            .setNegativeButton("બંધ", null)
            .show()
    }

    // ==================== નવું કામ ઉમેરવું ====================

    private fun addTaskDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 4)
        }

        val name = EditText(this).apply { hint = "કામનું નામ" }
        val goal = EditText(this).apply { hint = "સંબંધિત લક્ષ્ય (વૈકલ્પિક)" }
        val cost = EditText(this).apply {
            hint = "ખર્ચ રૂપિયા"
            inputType = 8194
            setText("0")
        }
        val notes = EditText(this).apply {
            hint = "નોંધ અથવા અનુભવ"
            minLines = 2
            gravity = Gravity.TOP
        }

        form.addView(name)
        form.addView(goal)
        form.addView(cost)
        form.addView(notes)

        AlertDialog.Builder(this)
            .setTitle("➕ નવું કામ")
            .setView(form)
            .setPositiveButton("સાચવો") { _, _ ->
                val taskName = name.text.toString().trim()
                val amount = cost.text.toString().toDoubleOrNull()

                if (taskName.isEmpty()) {
                    toast("કામનું નામ લખો")
                } else if (amount == null || !amount.isFinite() || amount < 0) {
                    toast("ખર્ચની યોગ્ય રકમ લખો")
                } else {
                    tasks.add(JSONObject().apply {
                        put("name", taskName)
                        put("goal", goal.text.toString().trim())
                        put("cost", amount)
                        put("notes", notes.text.toString().trim())
                        put("progress", 0)
                        put("status", "શરૂ કરવાનું બાકી")
                        put("media", JSONArray())
                    })
                    saveTasks()
                    updateCounts()
                    toast("કામ સાચવાઈ ગયું ✅")
                }
            }
            .setNegativeButton("રદ કરો", null)
            .show()
    }

    private fun showTasks() {
        if (tasks.isEmpty()) {
            toast("હજુ કોઈ કામ ઉમેર્યું નથી.")
            return
        }

        val items = tasks.mapIndexed { i, task ->
            "${i + 1}. ${task.optString("name")} — ${task.optInt("progress")}%"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("✅ મારાં કામ")
            .setItems(items) { _, which -> editTask(which) }
            .setNegativeButton("બંધ", null)
            .show()
    }

    private fun editTask(index: Int) {
        if (index !in tasks.indices) return
        val task = tasks[index]

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 4)
        }

        val progress = EditText(this).apply {
            hint = "પ્રોગ્રેસ 0 થી 100"
            inputType = 2
            setText(task.optInt("progress").toString())
        }
        form.addView(progress)

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )
        spinner.setSelection(
            statuses.indexOf(task.optString("status", statuses[0])).coerceAtLeast(0)
        )
        form.addView(spinner)

        val cost = EditText(this).apply {
            hint = "ખર્ચ રૂપિયા"
            inputType = 8194
            setText(task.optDouble("cost", 0.0).toString())
        }
        form.addView(cost)

        val notes = EditText(this).apply {
            hint = "નોંધ અને અનુભવ"
            minLines = 2
            gravity = Gravity.TOP
            setText(task.optString("notes"))
        }
        form.addView(notes)

        val mediaButton = Button(this).apply {
            text = "📸 ફોટા અને વીડિયો મેનેજ કરો"
            isAllCaps = false
        }
        form.addView(mediaButton)
        mediaButton.setOnClickListener { manageMedia(index) }

        val dialog = AlertDialog.Builder(this)
            .setTitle(task.optString("name"))
            .setView(form)
            .setPositiveButton("સાચવો", null)
            .setNeutralButton("કામ કાઢી નાખો", null)
            .setNegativeButton("રદ કરો", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val p = progress.text.toString().toIntOrNull()
                val c = cost.text.toString().toDoubleOrNull()

                if (p == null || p !in 0..100 ||
                    c == null || !c.isFinite() || c < 0
                ) {
                    toast("પ્રોગ્રેસ 0-100 અને ખર્ચ યોગ્ય લખો")
                } else {
                    task.put("progress", p)
                    task.put("status", spinner.selectedItem.toString())
                    task.put("cost", c)
                    task.put("notes", notes.text.toString())
                    saveTasks()
                    updateCounts()
                    toast("કામની વિગતો સાચવાઈ ✅")
                    dialog.dismiss()
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                AlertDialog.Builder(this)
                    .setMessage("આ કામ કાઢી નાખવું છે?")
                    .setPositiveButton("હા") { _, _ ->
                        tasks.removeAt(index)
                        saveTasks()
                        updateCounts()
                        dialog.dismiss()
                        toast("કામ કાઢી નાખ્યું")
                    }
                    .setNegativeButton("ના", null)
                    .show()
            }
        }

        dialog.show()
    }

    // ==================== કુલ ખર્ચ ====================

    private fun showTotalCost() {
        val taskTotal = tasks.sumOf { it.optDouble("cost", 0.0) }
        val missionTotal = missionYears.sumOf { it.optDouble("cost", 0.0) }

        AlertDialog.Builder(this)
            .setTitle("💰 ખર્ચનો હિસાબ")
            .setMessage(
                "સામાન્ય કામનો ખર્ચ: ₹${moneyFormat.format(taskTotal)}\n\n" +
                    "18 વર્ષના મિશનનો ખર્ચ: ₹${moneyFormat.format(missionTotal)}\n\n" +
                    "કુલ નોંધાયેલ ખર્ચ: ₹${moneyFormat.format(taskTotal + missionTotal)}"
            )
            .setPositiveButton("બરાબર", null)
            .show()
    }

    // ==================== નોંધો ====================

    private fun showNotes() {
        val taskNotes = tasks.mapNotNull { task ->
            val note = task.optString("notes").trim()
            if (note.isNotEmpty()) "${task.optString("name")}\n$note" else null
        }

        val missionNotes = missionYears.mapNotNull { year ->
            val note = year.optString("notes").trim()
            if (note.isNotEmpty()) {
                "વર્ષ ${year.optInt("year")} — ${year.optString("title")}\n$note"
            } else null
        }

        val allNotes = taskNotes + missionNotes

        AlertDialog.Builder(this)
            .setTitle("📔 નોંધો અને અનુભવો")
            .setMessage(
                if (allNotes.isEmpty()) "હજુ કોઈ નોંધ નથી."
                else allNotes.joinToString("\n\n──────────\n\n")
            )
            .setPositiveButton("બરાબર", null)
            .show()
    }

    // ==================== ફોટા અને વીડિયો ====================

    @Deprecated("Uses Android document picker result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK || data == null) {
            pendingMediaTaskIndex = -1
            return
        }

        val uri = data.data

        when (requestCode) {
            imageRequest -> {
                val index = pendingMediaTaskIndex
                if (index in tasks.indices) {
                    val clipData = data.clipData
                    if (clipData != null) {
                        for (i in 0 until clipData.itemCount) {
                            saveMediaReference(index, clipData.getItemAt(i).uri, "image")
                        }
                    } else if (uri != null) {
                        saveMediaReference(index, uri, "image")
                    }
                }
                pendingMediaTaskIndex = -1
            }

            videoRequest -> {
                val index = pendingMediaTaskIndex
                if (index in tasks.indices && uri != null) {
                    saveMediaReference(index, uri, "video")
                }
                pendingMediaTaskIndex = -1
            }
        }
    }

    private fun manageMedia(index: Int) {
        if (index !in tasks.indices) {
            toast("સંબંધિત કામ મળ્યું નથી.")
            return
        }

        val options = arrayOf(
            "📸 ફોટા પસંદ કરો",
            "🎥 વીડિયો પસંદ કરો",
            "📂 જોડેલી ફાઇલો જુઓ"
        )

        AlertDialog.Builder(this)
            .setTitle("મીડિયા મેનેજ કરો")
            .setItems(options) { _, choice ->
                when (choice) {
                    0 -> {
                        pendingMediaTaskIndex = index
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "image/*"
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        }
                        startActivityForResult(intent, imageRequest)
                    }

                    1 -> {
                        pendingMediaTaskIndex = index
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "video/*"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        }
                        startActivityForResult(intent, videoRequest)
                    }

                    2 -> showTaskMedia(index)
                }
            }
            .setNegativeButton("બંધ", null)
            .show()
    }

    private fun saveMediaReference(index: Int, uri: Uri, type: String) {
        try {
            if (index !in tasks.indices) {
                toast("સંબંધિત કામ મળ્યું નથી.")
                return
            }

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // કેટલાક providers કાયમી પરવાનગી આપતા નથી.
            }

            val task = tasks[index]
            val media = task.optJSONArray("media") ?: JSONArray()

            media.put(JSONObject().apply {
                put("uri", uri.toString())
                put("type", type)
                put("name", uri.lastPathSegment ?: "Media file")
            })

            task.put("media", media)
            saveTasks()
            toast("મીડિયા ઉમેરાયું ✅")
        } catch (e: Exception) {
            toast("મીડિયા સાચવી શકાયું નથી: ${e.message}")
        }
    }

    private fun showTaskMedia(index: Int) {
        if (index !in tasks.indices) {
            toast("સંબંધિત કામ મળ્યું નથી.")
            return
        }

        val media = tasks[index].optJSONArray("media") ?: JSONArray()

        if (media.length() == 0) {
            toast("આ કામ સાથે કોઈ ફોટો કે વીડિયો જોડાયેલો નથી.")
            return
        }

        val items = Array(media.length()) { i ->
            val item = media.getJSONObject(i)
            val icon = if (item.optString("type") == "video") "🎥" else "📸"
            "$icon ${item.optString("name")}"
        }

        AlertDialog.Builder(this)
            .setTitle("જોડેલી ફાઇલો")
            .setItems(items) { _, which ->
                try {
                    val item = media.getJSONObject(which)
                    val uri = Uri.parse(item.getString("uri"))
                    val mime = if (item.optString("type") == "video") {
                        "video/*"
                    } else {
                        "image/*"
                    }

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    toast("ફાઇલ ખોલી શકાયી નથી: ${e.message}")
                }
            }
            .setNegativeButton("બંધ", null)
            .show()
    }

    // ==================== UI સહાયક ફંક્શન ====================

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float,
        color: Int
    ) {
        val text = TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            setPadding(4, 12, 4, 12)
        }
        parent.addView(text)
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(green)
            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
