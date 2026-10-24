package com.saltanat45.planner

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private val green = Color.rgb(24, 65, 48)
    private val gold = Color.rgb(218, 176, 93)
    private val cream = Color.rgb(246, 245, 238)

    private val goals = mutableListOf<String>()
    private val tasks = mutableListOf<JSONObject>()
    private lateinit var goalCount: TextView
    private lateinit var taskCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadGoals()
        loadTasks()

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
            setPadding(20, 32, 20, 32)
        }
        root.addView(header)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 12, 18, 24)
        }

        addText(content, "તમારા સપનાની સફર અહીંથી શરૂ થાય છે", 18f, green)

        goalCount = TextView(this).apply {
            textSize = 16f
            setTextColor(green)
            setPadding(4, 8, 4, 8)
        }
        content.addView(goalCount)

        taskCount = TextView(this).apply {
            textSize = 16f
            setTextColor(green)
            setPadding(4, 8, 4, 8)
        }
        content.addView(taskCount)
        updateCounts()

        addButton(content, "🎯 મારા લક્ષ્યો") { showGoals() }
        addButton(content, "➕ નવું કામ ઉમેરો") { addTaskDialog() }
        addButton(content, "✅ મારાં કામ અને પ્રોગ્રેસ") { showTasks() }
        addButton(content, "💰 કુલ ખર્ચ") { showTotalCost() }
        addButton(content, "📔 નોંધો અને અનુભવો") { showNotes() }

        addText(content, "તમારો પ્લાન. તમારી પ્રગતિ. તમારું ભવિષ્ય.", 14f, green)

        val scroll = ScrollView(this)
        scroll.addView(content)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )
        setContentView(root)
    }

    private fun prefs() = getSharedPreferences(
        "saltanat45_data", Context.MODE_PRIVATE
    )

    private fun loadGoals() {
        goals.clear()
        try {
            val array = JSONArray(prefs().getString("goals", "[]"))
            for (i in 0 until array.length()) goals.add(array.getString(i))
        } catch (_: Exception) {}
    }

    private fun saveGoals() {
        val array = JSONArray()
        goals.forEach { array.put(it) }
        prefs().edit().putString("goals", array.toString()).apply()
    }

    private fun loadTasks() {
        tasks.clear()
        try {
            val array = JSONArray(prefs().getString("tasks", "[]"))
            for (i in 0 until array.length()) tasks.add(array.getJSONObject(i))
        } catch (_: Exception) {}
    }

    private fun saveTasks() {
        val array = JSONArray()
        tasks.forEach { array.put(it) }
        prefs().edit().putString("tasks", array.toString()).apply()
    }

    private fun updateCounts() {
        goalCount.text = "🎯 કુલ લક્ષ્યો: ${goals.size}"
        taskCount.text = "✅ કુલ કામ: ${tasks.size}"
    }

    private fun showGoals() {
        val input = EditText(this).apply { hint = "નવું લક્ષ્ય લખો" }

        AlertDialog.Builder(this)
            .setTitle("🎯 મારા લક્ષ્યો")
            .setMessage(
                if (goals.isEmpty()) "હજુ કોઈ લક્ષ્ય નથી."
                else goals.mapIndexed { i, g -> "${i + 1}. $g" }
                    .joinToString("\n")
            )
            .setView(input)
            .setPositiveButton("ઉમેરો") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    goals.add(value)
                    saveGoals()
                    updateCounts()
                    toast("લક્ષ્ય સાચવાયું ✅")
                }
            }
            .setNegativeButton("બંધ", null)
            .show()
    }

    private fun addTaskDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 4)
        }

        val name = EditText(this).apply { hint = "કામનું નામ" }
        form.addView(name)

        val goal = EditText(this).apply {
            hint = "સંબંધિત લક્ષ્ય (વૈકલ્પિક)"
        }
        form.addView(goal)

        val cost = EditText(this).apply {
            hint = "ખર્ચ રૂપિયા (ન હોય તો 0)"
            inputType = 8194
            setText("0")
        }
        form.addView(cost)

        val notes = EditText(this).apply {
            hint = "નોંધ અથવા અનુભવ"
            minLines = 2
            gravity = Gravity.TOP
        }
        form.addView(notes)

        AlertDialog.Builder(this)
            .setTitle("➕ નવું કામ")
            .setView(form)
            .setPositiveButton("સાચવો") { _, _ ->
                val taskName = name.text.toString().trim()
                val amount = cost.text.toString().toDoubleOrNull()

                if (taskName.isEmpty()) {
                    toast("કામનું નામ લખો")
                } else if (amount == null || amount < 0) {
                    toast("ખર્ચની રકમ બરાબર લખો")
                } else {
                    val task = JSONObject().apply {
                        put("name", taskName)
                        put("goal", goal.text.toString().trim())
                        put("cost", amount)
                        put("notes", notes.text.toString().trim())
                        put("progress", 0)
                        put("status", "શરૂ કરવાનું બાકી")
                    }
                    tasks.add(task)
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

        val items = tasks.mapIndexed { i, t ->
            "${i + 1}. ${t.optString("name")} — ${t.optInt("progress")}%"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("✅ મારાં કામ")
            .setItems(items) { _, which -> editTask(which) }
            .setNegativeButton("બંધ", null)
            .show()
    }

    private fun editTask(index: Int) {
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

        val statuses = arrayOf(
            "શરૂ કરવાનું બાકી", "ચાલુ છે", "પૂર્ણ થયું", "થોભાવ્યું"
        )
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )
        val oldStatus = task.optString("status", statuses[0])
        spinner.setSelection(statuses.indexOf(oldStatus).coerceAtLeast(0))
        form.addView(spinner)

        val cost = EditText(this).apply {
            hint = "કુલ ખર્ચ રૂપિયા"
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

        AlertDialog.Builder(this)
            .setTitle(task.optString("name"))
            .setView(form)
            .setPositiveButton("સાચવો") { _, _ ->
                val p = progress.text.toString().toIntOrNull()
                val c = cost.text.toString().toDoubleOrNull()

                if (p == null || p !in 0..100 || c == null || c < 0) {
                    toast("પ્રોગ્રેસ 0-100 અને ખર્ચ યોગ્ય લખો")
                } else {
                    task.put("progress", p)
                    task.put("status", spinner.selectedItem.toString())
                    task.put("cost", c)
                    task.put("notes", notes.text.toString())
                    saveTasks()
                    updateCounts()
                    toast("કામની વિગતો સાચવાઈ ✅")
                }
            }
            .setNeutralButton("કામ કાઢી નાખો") { _, _ ->
                AlertDialog.Builder(this)
                    .setMessage("આ કામ કાઢી નાખવું છે?")
                    .setPositiveButton("હા") { _, _ ->
                        tasks.removeAt(index)
                        saveTasks()
                        updateCounts()
                        toast("કામ કાઢી નાખ્યું")
                    }
                    .setNegativeButton("ના", null)
                    .show()
            }
            .setNegativeButton("રદ કરો", null)
            .show()
    }

    private fun showTotalCost() {
        val total = tasks.sumOf { it.optDouble("cost", 0.0) }
        toast("બધાં કામનો કુલ ખર્ચ: ₹${"%.2f".format(total)}")
    }

    private fun showNotes() {
        val notes = tasks.mapNotNull {
            val note = it.optString("notes").trim()
            if (note.isNotEmpty()) "${it.optString("name")}\n$note" else null
        }

        AlertDialog.Builder(this)
            .setTitle("📔 નોંધો અને અનુભવો")
            .setMessage(
                if (notes.isEmpty()) "હજુ કોઈ નોંધ નથી."
                else notes.joinToString("\n\n──────────\n\n")
            )
            .setPositiveButton("બરાબર", null)
            .show()
    }

    private fun addText(
        parent: LinearLayout, value: String, size: Float, color: Int
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
        parent: LinearLayout, label: String, action: () -> Unit
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
