package com.example.firsttest

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject

// Модель для документа (упрощённо)
data class AdminDoc(val id: Int, val regNumber: String, val title: String, val status: String)

// Модель для лога
data class LogRecord(val id: Int, val documentId: Int?, val userId: Int?, val action: String, val timestamp: String)

class AdminActivity : AppCompatActivity() {

    private val TAG = "AdminActivity"
    private val BASE_URL = "http://10.0.2.2/doc_api/" // <-- поменяй при необходимости

    // UI
    private lateinit var btnManage: MaterialButton
    private lateinit var btnLogs: MaterialButton

    private lateinit var manageView: View
    private lateinit var logsView: View

    // Manage
    private lateinit var btnRefreshDocs: MaterialButton
    private lateinit var adminDocRecycler: RecyclerView
    private lateinit var adminDocEmpty: TextView
    private val docs = ArrayList<AdminDoc>()
    private lateinit var docsAdapter: AdminDocsAdapter

    // Logs
    private lateinit var btnRefreshLogs: MaterialButton
    private lateinit var logsRecycler: RecyclerView
    private lateinit var logsEmpty: TextView
    private val logs = ArrayList<LogRecord>()
    private lateinit var logsAdapter: LogsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        btnManage = findViewById(R.id.btnManage)
        btnLogs = findViewById(R.id.btnLogs)

        manageView = findViewById(R.id.manageView)
        logsView = findViewById(R.id.logsView)

        // Manage
        btnRefreshDocs = findViewById(R.id.btnRefreshDocs)
        adminDocRecycler = findViewById(R.id.adminDocRecycler)
        adminDocEmpty = findViewById(R.id.adminDocEmpty)

        adminDocRecycler.layoutManager = LinearLayoutManager(this)
        docsAdapter = AdminDocsAdapter(docs,
            onDelete = { doc -> confirmDelete(doc) }
        )
        adminDocRecycler.adapter = docsAdapter
        btnRefreshDocs.setOnClickListener { loadDocuments() }

        // Logs
        btnRefreshLogs = findViewById(R.id.btnRefreshLogs)
        logsRecycler = findViewById(R.id.logsRecycler)
        logsEmpty = findViewById(R.id.logsEmpty)

        logsRecycler.layoutManager = LinearLayoutManager(this)
        logsAdapter = LogsAdapter(logs)
        logsRecycler.adapter = logsAdapter
        btnRefreshLogs.setOnClickListener { loadLogs() }

        // Toggle
        btnManage.setOnClickListener { showManage() }
        btnLogs.setOnClickListener { showLogs() }

        // initial
        showManage()
        loadDocuments()
    }

    private fun showManage() {
        manageView.visibility = View.VISIBLE
        logsView.visibility = View.GONE
        btnManage.isEnabled = false
        btnLogs.isEnabled = true
    }

    private fun showLogs() {
        manageView.visibility = View.GONE
        logsView.visibility = View.VISIBLE
        btnManage.isEnabled = true
        btnLogs.isEnabled = false
        loadLogs()
    }

    // ---------------- Documents ----------------
    private fun loadDocuments() {
        val url = BASE_URL + "get_documents.php"
        val queue = Volley.newRequestQueue(this)
        val req = StringRequest(Request.Method.GET, url,
            { resp ->
                try {
                    docs.clear()
                    val arr = JSONArray(resp)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        docs.add(AdminDoc(
                            id = o.optInt("id", 0),
                            regNumber = o.optString("reg_number", ""),
                            title = o.optString("title", ""),
                            status = o.optString("status", "")
                        ))
                    }
                    docsAdapter.notifyDataSetChanged()
                    adminDocEmpty.visibility = if (docs.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    Log.e(TAG, "parse docs error", e)
                    adminDocEmpty.visibility = View.VISIBLE
                    Toast.makeText(this, "Ошибка обработки списка документов", Toast.LENGTH_SHORT).show()
                }
            },
            { err ->
                Log.e(TAG, "load docs error", err)
                adminDocEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "Ошибка соединения при загрузке документов", Toast.LENGTH_SHORT).show()
            })
        req.setShouldCache(false)
        queue.add(req)
    }

    private fun confirmDelete(doc: AdminDoc) {
        AlertDialog.Builder(this)
            .setTitle("Удалить документ")
            .setMessage("Вы уверены, что хотите удалить документ \"${doc.title}\" (№ ${doc.regNumber})?")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ -> deleteDocument(doc) }
            .show()
    }

    private fun deleteDocument(doc: AdminDoc) {
        val url = BASE_URL + "delete_document.php"
        val queue = Volley.newRequestQueue(this)

        val postRequest = object : StringRequest(
            Method.POST, url,
            { resp ->
                // Убираем возможные пробелы и BOM
                val trimmed = resp.trim()
                try {
                    val obj = JSONObject(trimmed)
                    val ok = obj.optBoolean("success", false)
                    if (ok) {
                        Toast.makeText(this, "Документ удалён", Toast.LENGTH_SHORT).show()
                        val idx = docs.indexOfFirst { it.id == doc.id }
                        if (idx >= 0) {
                            docs.removeAt(idx)
                            docsAdapter.notifyItemRemoved(idx)
                            adminDocEmpty.visibility = if (docs.isEmpty()) View.VISIBLE else View.GONE
                        }
                    } else {
                        val err = obj.optString("error", "Ошибка удаления")
                        Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Показываем сырой ответ для отладки — это поможет увидеть, почему JSON не парсится
                    Log.e(TAG, "parse delete resp exception", e)
                    Log.e(TAG, "raw server response: [$trimmed]")
                    // Показываем кусочек ответа в Toast (или long) чтобы сразу увидеть проблему в эмуляторе
                    Toast.makeText(this, "Неправильный ответ сервера: ${trimmed.take(200)}", Toast.LENGTH_LONG).show()
                }
            },
            { err ->
                Log.e(TAG, "delete error", err)
                Toast.makeText(this, "Ошибка соединения при удалении", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("id" to doc.id.toString())
            }
        }
        postRequest.setShouldCache(false)
        queue.add(postRequest)
    }


    // ---------------- Logs ----------------
    private fun loadLogs() {
        val url = BASE_URL + "get_logs.php" // Совет: реализовать этот PHP если его нет
        val queue = Volley.newRequestQueue(this)
        val req = StringRequest(Request.Method.GET, url,
            { resp ->
                try {
                    logs.clear()
                    val arr = JSONArray(resp)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        logs.add(LogRecord(
                            id = o.optInt("id", 0),
                            documentId = if (o.has("document_id")) o.optInt("document_id") else null,
                            userId = if (o.has("user_id")) o.optInt("user_id") else null,
                            action = o.optString("action", ""),
                            timestamp = o.optString("timestamp", "")
                        ))
                    }
                    logsAdapter.notifyDataSetChanged()
                    logsEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    Log.e(TAG, "parse logs error", e)
                    logsEmpty.visibility = View.VISIBLE
                    Toast.makeText(this, "Ошибка обработки журнала", Toast.LENGTH_SHORT).show()
                }
            },
            { err ->
                Log.e(TAG, "load logs error", err)
                logsEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "Ошибка загрузки журнала (возможно нет get_logs.php)", Toast.LENGTH_LONG).show()
            })
        req.setShouldCache(false)
        queue.add(req)
    }

    // ---------------- Adapters ----------------
    inner class AdminDocsAdapter(
        private val list: List<AdminDoc>,
        private val onDelete: (AdminDoc) -> Unit
    ) : RecyclerView.Adapter<AdminDocsAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            private val title = view.findViewById<TextView>(R.id.docTitle)
            private val reg = view.findViewById<TextView>(R.id.docReg)
            private val status = view.findViewById<TextView>(R.id.docStatus)
            private val card = view.findViewById<CardView>(R.id.cardContainer)

            fun bind(item: AdminDoc) {
                title.text = item.title
                reg.text = "№ ${item.regNumber}"
                status.text = "Статус: ${item.status}"
                card.setOnLongClickListener {
                    // long click -> delete
                    onDelete(item)
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.document_item, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int = list.size
    }

    inner class LogsAdapter(private val items: List<LogRecord>) :
        RecyclerView.Adapter<LogsAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            private val title = view.findViewById<TextView>(android.R.id.text1)
            private val subtitle = view.findViewById<TextView>(android.R.id.text2)

            fun bind(item: LogRecord) {
                title.text = "${item.timestamp} — ${item.action}"
                val docPart = item.documentId?.let { " doc_id:$it" } ?: ""
                val userPart = item.userId?.let { " user:$it" } ?: ""
                subtitle.text = "$docPart$userPart"
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            // simple two-line layout
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}
