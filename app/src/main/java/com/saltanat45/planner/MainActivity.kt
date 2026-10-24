package com.saltanat45.planner

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.content.Context
import org.json.JSONArray

class MainActivity : Activity() {

    private val green = Color.rgb(24, 65, 48)
    private val gold = Color.rgb(218, 176, 93)
    private val cream = Color.rgb(246, 245, 238)

    private val goals = mutableListOf<String>()
    private lateinit var goalCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadGoals()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cream)
        }

        val header = TextView(this).apply {
            text = "👑 SALTANAT 45\nMy Life Blueprint"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setPadding(20, 36, 20, 36)
        }
        root.addView(header)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 24)
        }

        addText(content, "તમારા સપનાની સફર અહીંથી શરૂ થાય છે", 18f, green)
        addText(
            content,
            "તમારા જીવનના બધા લક્ષ્યો, કામ અને ખર્ચનું એક જ સ્થાન.",
            15f,
            Color.DKGRAY
        )

        addText(content, "━━━━━━━━━━━━━━━━━━", 16f, gold)
        addText(content, "📊 મારો ડેશબોર્ડ", 22f, green)

        goalCount = TextView(this).apply {
            textSize = 17f
            setTextColor(green)
            setPadding(4, 10, 4, 10)
        }
        content.addView(goalCount)
        updateGoalCount()

        addButton(content, "🎯 મારા લક્ષ્યો") {
            showGoalsDialog()
        }

        addButton(content, "✅ કામ અને પ્રોગ્રેસ") {
            showMessage("કામ અને પ્રોગ્રેસની સુવિધા આગળના તબક્કામાં બનાવશું.")
        }

        addButton(content, "💰 બજેટ અને ખર્ચ") {
            showMessage("બજેટ અને ખર્ચની સુવિધા આગળ બનાવશું.")
        }

        addButton(content, "📔 અનુભવ અને નોંધો") {
            showMessage("નોંધો અને અનુભવોની સુવિધા આગળ બનાવશું.")
        }

        addButton(content, "📷 ફોટા અને વીડિયો") {
            showMessage("ફોટા અને વીડિયો જોડવાની સુવિધા આગળ બનાવશું.")
        }

        addButton(content, "💾 બેકઅપ અને રિસ્ટોર") {
            showMessage("બેકઅપ અને રિસ્ટોરની સુવિધા આગળ બનાવશું.")
        }

        addText(
            content,
            "તમારો પ્લાન. તમારી પ્રગતિ. તમારું ભવિષ્ય.",
            14f,
            green
        )

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

    private fun showGoalsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 12, 24, 4)
        }

        val input = EditText(this).apply {
            hint = "નવું લક્ષ્ય લખો"
            textSize = 16f
        }
        layout.addView(input)

        val list = TextView(this).apply {
            textSize = 16f
            setTextColor(green)
            setPadding(0, 16, 0, 8)
            text = if (goals.isEmpty()) {
                "હજુ કોઈ લક્ષ્ય ઉમેર્યું નથી."
            } else {
                goals.mapIndexed { index, goal ->
                    "${index + 1}. $goal"
                }.joinToString("\n\n")
            }
        }
        layout.addView(list)

        val dialog = AlertDialog.Builder(this)
            .setTitle("🎯 મારા લક્ષ્યો")
            .setView(layout)
            .setPositiveButton("લક્ષ્ય ઉમેરો", null)
            .setNegativeButton("બંધ કરો", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val newGoal = input.text.toString().trim()

                    if (newGoal.isEmpty()) {
                        input.error = "પહેલાં લક્ષ્ય લખો"
                    } else {
                        goals.add(newGoal)
                        saveGoals()
                        updateGoalCount()
                        dialog.dismiss()
                        showMessage("લક્ષ્ય ફોનમાં સાચવાઈ ગયું ✅")
                    }
                }
        }

        dialog.show()
    }

    private fun loadGoals() {
        val prefs = getSharedPreferences(
            "saltanat45_data",
            Context.MODE_PRIVATE
        )
        val saved = prefs.getString("goals", "[]") ?: "[]"

        try {
            val array = JSONArray(saved)
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

        getSharedPreferences(
            "saltanat45_data",
            Context.MODE_PRIVATE
        ).edit()
            .putString("goals", array.toString())
            .apply()
    }

    private fun updateGoalCount() {
        goalCount.text = "🎯 કુલ લક્ષ્યો: ${goals.size}"
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float,
        color: Int
    ) {
        val text = TextView(this).apply {
            text = value
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

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(
            this,
            message,
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}
