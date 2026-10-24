package com.saltanat45.planner

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val green = Color.rgb(24, 65, 48)
    private val gold = Color.rgb(218, 176, 93)
    private val cream = Color.rgb(246, 245, 238)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        addText(content, "તમારા જીવનના બધા લક્ષ્યો, કામ અને ખર્ચનું એક જ સ્થાન.", 15f, Color.DKGRAY)
        addText(content, "━━━━━━━━━━━━━━━━━━", 16f, gold)
        addText(content, "📊  મારો ડેશબોર્ડ", 22f, green)

        addButton(content, "🎯  મારા લક્ષ્યો") {
            showMessage("તમારા બધા લક્ષ્યોનું વિભાગ આગળના તબક્કામાં તૈયાર થશે.")
        }

        addButton(content, "✅  કામ અને પ્રોગ્રેસ") {
            showMessage("કામ ઉમેરવા અને પ્રોગ્રેસ સાચવવાની સુવિધા આગળ ઉમેરાશે.")
        }

        addButton(content, "💰  બજેટ અને ખર્ચ") {
            showMessage("તમારા બજેટ અને ખર્ચનો વિભાગ આગળ ઉમેરાશે.")
        }

        addButton(content, "📔  અનુભવ અને નોંધો") {
            showMessage("તમારી નોંધો અને અનુભવોનો વિભાગ આગળ ઉમેરાશે.")
        }

        addButton(content, "📷  ફોટા અને વીડિયો") {
            showMessage("ફોટા અને વીડિયો જોડવાની સુવિધા આગળ ઉમેરાશે.")
        }

        addButton(content, "💾  બેકઅપ અને રિસ્ટોર") {
            showMessage("ડેટા બેકઅપ અને પાછો મેળવવાની સુવિધા આગળ ઉમેરાશે.")
        }

        addText(content, "તમારો પ્લાન. તમારી પ્રગતિ. તમારું ભવિષ્ય.", 14f, green)

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
