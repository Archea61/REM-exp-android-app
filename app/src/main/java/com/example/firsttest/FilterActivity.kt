package com.example.firsttest

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import org.json.JSONArray

data class FilterItem(
    val id: Int,
    val regNumber: String,
    val title: String,
    val status: String,
    val typeName: String?
)

class FilterActivity : AppCompatActivity() {

    // === Конфигурация endpoint'ов ===
    private val BASE_URL = "http://10.0.2.2/doc_api/"
    private val ENDPOINT_CALL_FILTER_STATUS = "call_filter_status.php" // POST {status}
    private val ENDPOINT_CALL_FILTER_TYPE = "call_filter_type.php"     // POST {type}
    private val ENDPOINT_CALL_OVERDUE = "call_overdue.php"             // GET
    private val ENDPOINT_CALL_COMPLETED = "call_completed.php"         // GET
    private val ENDPOINT_SEARCH = "search_documents.php"               // POST {term}
    private val ENDPOINT_GET_ALL = "get_documents.php"                // GET (fallback / by department local filter)
    // ===================================

    private lateinit var btnFilter1: MaterialButton
    private lateinit var btnFilter2: MaterialButton
    private lateinit var btnFilter3: MaterialButton
    private lateinit var btnFilter4: MaterialButton
    private lateinit var btnFilter5: MaterialButton
    private var btnSearch: MaterialButton? = null

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView

    private val data = ArrayList<FilterItem>()
    private lateinit var adapter: FilterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        btnFilter1 = findViewById(R.id.btnFilter1) // фильтр по статусу (ввод)
        btnFilter2 = findViewById(R.id.btnFilter2) // выполненные (completed)
        btnFilter3 = findViewById(R.id.btnFilter3) // фильтр по типу (ввод)
        btnFilter5 = findViewById(R.id.btnFilter5) // просроченные

        // btnSearch может быть в layout или нет — безопасно ищем
        val maybeSearch = findViewById<View?>(R.id.btnSearch)
        if (maybeSearch is MaterialButton) btnSearch = maybeSearch

        recycler = findViewById(R.id.filterRecycler)
        emptyView = findViewById(R.id.filterEmpty)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = FilterAdapter(data) { item ->
            showDocumentOptions(item.id, item)
        }
        recycler.adapter = adapter

        // события кнопок
        btnFilter1.setOnClickListener { promptForStatusAndLoad() }
        btnFilter2.setOnClickListener { loadFromUrlGet(BASE_URL + ENDPOINT_CALL_COMPLETED) }
        btnFilter3.setOnClickListener { promptForTypeAndLoad() }
        btnFilter5.setOnClickListener { loadFromUrlGet(BASE_URL + ENDPOINT_CALL_OVERDUE) }

        // если есть кнопка поиска в layout, привяжем её
        btnSearch?.setOnClickListener { promptForSearchAndLoad() }
    }

    // ---------------- Document options (Edit/Delete) ----------------

    private fun showDocumentOptions(docId: Int, item: FilterItem) {
        val options = arrayOf("Редактировать", "Удалить", "Отмена")
        AlertDialog.Builder(this)
            .setTitle(item.title)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        val i = android.content.Intent(this, AddDocumentActivity::class.java)
                        i.putExtra("EXTRA_DOC_ID", docId)
                        startActivity(i)
                    }
                    1 -> confirmAndDelete(docId)
                    2 -> dialog.dismiss()
                }
            }.show()
    }

    private fun confirmAndDelete(docId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить документ")
            .setMessage("Вы уверены, что хотите удалить документ №$docId?")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ -> performDelete(docId) }
            .show()
    }

    private fun performDelete(docId: Int) {
        val url = BASE_URL + "delete_document.php"
        val queue = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, url,
            { resp ->
                try {
                    val obj = org.json.JSONObject(resp)
                    if (obj.optBoolean("success", false)) {
                        val idx = data.indexOfFirst { it.id == docId }
                        if (idx >= 0) {
                            data.removeAt(idx)
                            adapter.notifyItemRemoved(idx)
                        } else {
                            // fallback: загрузим все документы
                            loadFromUrlGet(BASE_URL + ENDPOINT_GET_ALL)
                        }
                        Toast.makeText(this, "Документ удалён", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Ошибка: ${obj.optString("error")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Неправильный ответ сервера", Toast.LENGTH_SHORT).show()
                }
            },
            { _ -> Toast.makeText(this, "Ошибка соединения при удалении", Toast.LENGTH_SHORT).show() }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("id" to docId.toString())
            }
        }
        req.setShouldCache(false)
        queue.add(req)
    }

    // ---------------- Dialogs ----------------

    private fun promptForStatusAndLoad() {
        val et = EditText(this)
        et.hint = "Введите статус (например: на согласовании)"
        AlertDialog.Builder(this)
            .setTitle("Фильтр по статусу")
            .setView(et)
            .setPositiveButton("Показать") { _, _ ->
                val status = et.text.toString().trim()
                if (status.isEmpty()) {
                    Toast.makeText(this, "Введите статус", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                postToEndpoint(BASE_URL + ENDPOINT_CALL_FILTER_STATUS, mapOf("status" to status))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun promptForTypeAndLoad() {
        val et = EditText(this)
        et.hint = "Введите тип (например: Приказ)"
        AlertDialog.Builder(this)
            .setTitle("Фильтр по типу")
            .setView(et)
            .setPositiveButton("Показать") { _, _ ->
                val type = et.text.toString().trim()
                if (type.isEmpty()) {
                    Toast.makeText(this, "Введите тип", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                postToEndpoint(BASE_URL + ENDPOINT_CALL_FILTER_TYPE, mapOf("type" to type))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun promptForDeptAndFilterLocal() {
        val et = EditText(this)
        et.hint = "Введите подразделение (например: Бурение)"
        AlertDialog.Builder(this)
            .setTitle("Фильтр по подразделению (локально)")
            .setView(et)
            .setPositiveButton("Показать") { _, _ ->
                val dept = et.text.toString().trim()
                if (dept.isEmpty()) {
                    Toast.makeText(this, "Введите подразделение", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // получаем все документы и фильтруем локально по полю author_name или receiver
                loadAndFilterByDept(dept)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun promptForSearchAndLoad() {
        val et = EditText(this)
        et.hint = "Введите поисковый термин (часть названия)"
        AlertDialog.Builder(this)
            .setTitle("Поиск по названию")
            .setView(et)
            .setPositiveButton("Поиск") { _, _ ->
                val term = et.text.toString().trim()
                if (term.isEmpty()) {
                    Toast.makeText(this, "Введите термин", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                postToEndpoint(BASE_URL + ENDPOINT_SEARCH, mapOf("term" to term))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ---------------- Network helpers ----------------

    // GET запрос (для call_overdue и call_completed)
    private fun loadFromUrlGet(url: String) {
        data.clear()
        emptyView.visibility = View.GONE

        val queue = Volley.newRequestQueue(this)
        val req = StringRequest(Request.Method.GET, url,
            { resp ->
                parseAndShow(resp)
            },
            {
                emptyView.visibility = View.VISIBLE
                Toast.makeText(this, "Ошибка соединения", Toast.LENGTH_SHORT).show()
            })
        req.setShouldCache(false)
        queue.add(req)
    }

    // POST запрос с параметрами (form-encoded)
    private fun postToEndpoint(url: String, params: Map<String, String>) {
        data.clear()
        emptyView.visibility = View.GONE

        val queue = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, url,
            { resp -> parseAndShow(resp) },
            { _ ->
                emptyView.visibility = View.VISIBLE
                Toast.makeText(this, "Ошибка соединения", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                val m = HashMap<String, String>()
                m.putAll(params)
                return m
            }
        }
        req.setShouldCache(false)
        queue.add(req)
    }

    // GET -> get_documents.php и локальная фильтрация по подразделению
    private fun loadAndFilterByDept(dept: String) {
        val url = BASE_URL + ENDPOINT_GET_ALL
        val queue = Volley.newRequestQueue(this)
        val req = StringRequest(Request.Method.GET, url,
            { resp ->
                try {
                    data.clear()
                    val arr = JSONArray(resp)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        // проверяем author_name и receiver поля на совпадение с dept (регистронезависимо)
                        val author = o.optString("author_name", "")
                        val receiver = o.optString("receiver", "")
                        if (author.contains(dept, ignoreCase = true) || receiver.contains(dept, ignoreCase = true)) {
                            data.add(FilterItem(
                                id = o.optInt("id", 0),
                                regNumber = o.optString("reg_number", ""),
                                title = o.optString("title", ""),
                                status = o.optString("status", ""),
                                typeName = o.optString("type_name", null)
                            ))
                        }
                    }
                    adapter.notifyDataSetChanged()
                    emptyView.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    emptyView.visibility = View.VISIBLE
                    Toast.makeText(this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                }
            },
            {
                emptyView.visibility = View.VISIBLE
                Toast.makeText(this, "Ошибка соединения", Toast.LENGTH_SHORT).show()
            })
        req.setShouldCache(false)
        queue.add(req)
    }

    // Парсинг общего ответа (ожидается JSON-массив)
    private fun parseAndShow(resp: String) {
        try {
            data.clear()
            val arr = JSONArray(resp)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                data.add(FilterItem(
                    id = o.optInt("id", 0),
                    regNumber = o.optString("reg_number", ""),
                    title = o.optString("title", ""),
                    status = o.optString("status", ""),
                    typeName = o.optString("type_name", null)
                ))
            }
            adapter.notifyDataSetChanged()
            emptyView.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            emptyView.visibility = View.VISIBLE
            Toast.makeText(this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- Adapter ----------------
    class FilterAdapter(private val items: List<FilterItem>, private val onClick: (FilterItem) -> Unit) :
        RecyclerView.Adapter<FilterAdapter.Holder>() {
        inner class Holder(view: View): RecyclerView.ViewHolder(view) {
            private val title = view.findViewById<TextView>(R.id.docTitle)
            private val reg = view.findViewById<TextView>(R.id.docReg)
            private val status = view.findViewById<TextView>(R.id.docStatus)
            private val type = view.findViewById<TextView>(R.id.docType)

            fun bind(item: FilterItem) {
                title.text = item.title
                reg.text = "№ ${item.regNumber}"
                status.text = "Статус: ${item.status}"
                type.text = "Тип: ${item.typeName ?: "-"}"
                itemView.setOnClickListener { onClick(item) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.document_item, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}
