package com.example.firsttest   // <- проверь, чтобы совпадало с package в проекте

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    // Укажи правильный адрес (для AVD эмулятора)
    private val BASE_URL = "http://10.0.2.2/doc_api/"   // или "http://10.0.2.2:8080/doc_api/"

    private val TAG = "LoginDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginInput = findViewById<EditText>(R.id.loginInput)
        val passInput = findViewById<EditText>(R.id.passwordInput)
        val btn = findViewById<Button>(R.id.btnLogin)

        btn.setOnClickListener {
            val login = loginInput.text.toString().trim()
            val pass = passInput.text.toString().trim()

            if (login.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            doLogin(login, pass)
        }
    }

    private fun doLogin(login: String, pass: String) {
        val url = BASE_URL + "login.php"
        Log.i(TAG, "Attempt login. URL=$url login=$login")

        // Показать progress
        val pd = ProgressDialog(this)
        pd.setMessage("Вход...")
        pd.setCancelable(false)
        pd.show()

        val queue = Volley.newRequestQueue(this)

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                pd.dismiss()
                Log.i(TAG, "Server response: $response")

                try {
                    val obj = JSONObject(response)

                    // Если сервер вернул структуру {"success": true, "user": {...}}
                    if (obj.optBoolean("success", false)) {
                        val user = obj.optJSONObject("user")
                        if (user != null) {
                            val id = user.optInt("id", -1)
                            val name = user.optString("full_name", "Unknown")
                            val level = user.optInt("access_level", 1)

                            // Сохраняем в preferences
                            val pref = getSharedPreferences("user", MODE_PRIVATE)
                            pref.edit()
                                .putInt("id", id)
                                .putString("name", name)
                                .putInt("level", level)
                                .apply()

                            Log.i(TAG, "Login success. id=$id name=$name level=$level")

                            // Переход в главное меню
                            val i = Intent(this, MainMenuActivity::class.java)
                            startActivity(i)
                            finish()
                        } else {
                            Log.e(TAG, "Login: user object is null")
                            Toast.makeText(this, "Ошибка ответа сервера (user)", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val err = obj.optString("error", "Неверный логин/пароль")
                        Log.i(TAG, "Login failed: $err")
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show()
                    }
                } catch (e: JSONException) {
                    Log.e(TAG, "JSON parse error", e)
                    Toast.makeText(this, "Ошибка разбора ответа сервера", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                pd.dismiss()
                // Показываем более полезную информацию
                val msg = error.message ?: error.toString()
                Log.e(TAG, "Volley error", error)
                Toast.makeText(this, "Ошибка соединения: $msg", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "login" to login,
                    "password" to pass
                )
            }
        }

        // Таймаут увеличен — иногда локальный сервер медлит
        request.retryPolicy = DefaultRetryPolicy(
            10_000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        queue.add(request)
    }
}
