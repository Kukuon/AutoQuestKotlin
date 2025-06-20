package com.example.autoquest

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.autoquest.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private var binding: ActivityLoginBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        val emailInput = binding!!.emailInput
        val passwordInput = binding!!.passwordInput

        binding!!.acceptButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            // вход
            // проверка почты на правильность
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (password.isNotEmpty()) {
                    firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@LoginActivity,
                                "Успешный вход " + firebaseAuth.currentUser!!
                                    .email,
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    MainActivity::class.java
                                )
                            )
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(
                                this@LoginActivity,
                                "Не удалось войти",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    // пустой пароль
                    passwordInput.error = "Пароль не может быть пустым"
                }
            } else if (email.isEmpty()) {
                // пустая почта
                emailInput.error = "Почта не может быть пустой"
            } else {
                // почта неправильная
                emailInput.error = "Пожалуйста, введите почту правильно"
            }
        }
        // кнопка продолжить без аккаунта
        binding!!.continueWithoutAccountButton.setOnClickListener {
            startActivity(
                Intent(
                    this@LoginActivity, MainActivity::class.java
                )
            )
        }
        // кнопка сброса пароля
        binding!!.resetPasswordButton.setOnClickListener { showDialogResetPassword() }
        // кнопка перехода на регистрацию
        binding!!.signupButton.setOnClickListener {
            startActivity(
                Intent(
                    this@LoginActivity, SignUpActivity::class.java
                )
            )
        }
    }


    // диалог сброса пароля
    private fun showDialogResetPassword() {
        // билдер для AlertDialog
        val builder = AlertDialog.Builder(this)

        // получаем LayoutInflater
        val inflater = this.layoutInflater

        // создаем view для диалогового окна из пользовательского макета
        val dialogView = inflater.inflate(R.layout.dialog_change_data, null)
        // установка view билдеру
        builder.setView(dialogView)

        // привязка макета к объектам
        val title = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val acceptButton = dialogView.findViewById<Button>(R.id.saveButton)
        val editText = dialogView.findViewById<EditText>(R.id.newDataInput)

        // установка текста
        title.text = "Восстановление пароля"
        editText.hint = "E-mail"
        editText.setCompoundDrawablesWithIntrinsicBounds(
            ContextCompat.getDrawable(
                this,
                R.drawable.mail
            ), null, null, null
        )
        acceptButton.text = "Отправить"

        // создание диалога
        val dialog = builder.create()
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        }

        // кнопка подтверждения
        acceptButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val email = editText.text.toString().trim { it <= ' ' }
                if (email.isEmpty()) {
                    editText.error = "Введите ваш email"
                    dialog.dismiss()
                    return
                }

                // отправка сброса пароля на почту
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Инструкции по восстановлению пароля отправлены на ваш email",
                                Toast.LENGTH_LONG
                            ).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Ошибка отправки инструкции по восстановлению пароля",
                                Toast.LENGTH_LONG
                            ).show()
                            dialog.dismiss()
                        }
                    }
            }
        })
        // показ диалога
        dialog.show()
    }
}
