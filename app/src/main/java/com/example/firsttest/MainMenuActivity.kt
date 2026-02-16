package com.example.firsttest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainMenuActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainMenuActivity"
        private const val BASE_URL = "http://10.0.2.2/doc_api/"
        const val URL_STATS = BASE_URL + "get_stats.php"
    }

    private lateinit var tvWelcome: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvActive: TextView
    private lateinit var tvOverdue: TextView
    private lateinit var tvLastUpdate: TextView

    private lateinit var btnDocs: MaterialButton
    private lateinit var btnAdd: MaterialButton
    private lateinit var btnFilter: MaterialButton
    private lateinit var btnAdmin: MaterialButton
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var userId: Int = 0
    private var userName: String = "Пользователь"
    private var userLevel: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // bind views
        tvWelcome = findViewById(R.id.tvWelcome)
        tvRole = findViewById(R.id.tvRole)
        tvTotal = findViewById(R.id.tvTotal)
        tvActive = findViewById(R.id.tvActive)
        tvOverdue = findViewById(R.id.tvOverdue)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)

        btnDocs = findViewById(R.id.btnDocs)
        btnAdd = findViewById(R.id.btnAdd)
        btnFilter = findViewById(R.id.btnFilter)
        btnAdmin = findViewById(R.id.btnAdmin)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnLogout = findViewById(R.id.btnLogout)

        loadUserFromPrefs()

        tvWelcome.text = "Привет, $userName"
        tvRole.text = "Роль: ${roleName(userLevel)}"

        // показываем админ-кнопку только если уровень >= 3
        btnAdmin.visibility = if (userLevel >= 3) View.VISIBLE else View.GONE

        // навигация
        btnDocs.setOnClickListener { startActivity(Intent(this, DocumentListActivity::class.java)) }
        btnAdd.setOnClickListener {
            val i = Intent(this, AddDocumentActivity::class.java)
            i.putExtra("EXTRA_AUTHOR_ID", userId)
            startActivity(i)
        }
        btnFilter.setOnClickListener { startActivity(Intent(this, FilterActivity::class.java)) }
        btnAdmin.setOnClickListener { startActivity(Intent(this, AdminActivity::class.java)) }
        btnRefresh.setOnClickListener { loadStats() }
        btnLogout.setOnClickListener {
            // очистка SharedPreferences и возврат на экран логина
            val pref = getSharedPreferences("user", MODE_PRIVATE)
            pref.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // первичный запрос статистики
        loadStats()
    }

    override fun onResume() {
        super.onResume()
        // обновляем статистику при возвращении на экран (после редактирования/создания)
        loadUserFromPrefs()
        tvWelcome.text = "Привет, $userName"
        tvRole.text = "Роль: ${roleName(userLevel)}"
        loadStats()
    }

    private fun loadUserFromPrefs() {
        val pref = getSharedPreferences("user", MODE_PRIVATE)
        userId = pref.getInt("id", 0)
        userName = pref.getString("name", "Пользователь") ?: "Пользователь"
        userLevel = pref.getInt("level", 1)
    }

    private fun roleName(level: Int): String {
        return when (level) {
            1 -> "Исполнитель"
            2 -> "Менеджер"
            3 -> "Администратор"
            else -> "Пользователь"
        }
    }

    private fun loadStats() {
        val queue = Volley.newRequestQueue(this)
        val req = StringRequest(Request.Method.GET, URL_STATS,
            { response ->
                Log.i(TAG, "stats response: $response")
                parseAndDisplayStats(response)
            },
            { error ->
                Log.e(TAG, "stats error", error)
                Toast.makeText(this, "Ошибка загрузки статистики: ${error.message}", Toast.LENGTH_SHORT).show()
                // при ошибке оставим прежние значения или сбросим на 0
                tvTotal.text = "—"
                tvActive.text = "—"
                tvOverdue.text = "—"
                tvLastUpdate.text = "Обновлено: —"
            })
        req.setShouldCache(false)
        queue.add(req)
    }

    private fun parseAndDisplayStats(response: String) {
        try {
            val obj = JSONObject(response)

            // читаем значения из ответа
            var total = obj.optInt("total", -1)
            var active = obj.optInt("active", -1)
            var overdue = obj.optInt("overdue", -1)

            // нормализация: если сервер вернул -1 (отсутствие), считаем 0
            if (total < 0) total = 0
            if (active < 0) active = 0
            if (overdue < 0) overdue = 0

            // корректировка: сумма активных и просроченных не должна превышать общее число документов.
            // если такое произошло — приводим active к total - overdue (с ограничением нуля)
            if (active + overdue > total) {
                if (total >= overdue) {
                    active = total - overdue
                } else {
                    // крайний случай: overdue > total (невозможно логически) -> обрезаем overdue до total и active = 0
                    overdue = total
                    active = 0
                }
            }

            // обновляем UI
            tvTotal.text = total.toString()
            tvActive.text = active.toString()
            tvOverdue.text = overdue.toString()

            // last update timestamp
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            tvLastUpdate.text = "Обновлено: ${sdf.format(Date())}"

        } catch (e: Exception) {
            Log.e(TAG, "parse stats error", e)
            Toast.makeText(this, "Ошибка обработки статистики", Toast.LENGTH_SHORT).show()
            tvTotal.text = "—"
            tvActive.text = "—"
            tvOverdue.text = "—"
            tvLastUpdate.text = "Обновлено: —"
        }
    }
}
