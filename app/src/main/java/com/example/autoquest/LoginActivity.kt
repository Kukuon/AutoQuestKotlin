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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

class LoginActivity() : AppCompatActivity() {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private var binding: ActivityLoginBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        val emailInput = binding!!.emailInput
        val passwordInput = binding!!.passwordInput

        binding!!.acceptButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val email = emailInput.text.toString()
                val password = passwordInput.text.toString()
                if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if (!password.isEmpty()) {
                        firebaseAuth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener(
                                OnSuccessListener {
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
                                }).addOnFailureListener(object : OnFailureListener {
                            override fun onFailure(e: Exception) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Не удалось войти",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    } else {
                        passwordInput.error = "Пароль не может быть пустым"
                    }
                } else if (email.isEmpty()) {
                    emailInput.error = "Почта не может быть пустой"
                } else {
                    emailInput.error = "Пожалуйста, введите почту правильно"
                }
            }
        })
        binding!!.continueWithoutAccountButton.setOnClickListener { v: View? ->
            startActivity(
                Intent(
                    this@LoginActivity, MainActivity::class.java
                )
            )
        }
        binding!!.resetPasswordButton.setOnClickListener { v: View? -> showDialogResetPassword() }
        binding!!.signupButton.setOnClickListener { v: View? ->
            startActivity(
                Intent(
                    this@LoginActivity, SignUpActivity::class.java
                )
            )
        }
    }

    private fun showDialogResetPassword() {
        // Создаем билдер для AlertDialog
        val builder = AlertDialog.Builder(this)

        // Получаем LayoutInflater
        val inflater = this.layoutInflater

        // Создаем View для диалогового окна из пользовательского макета
        val dialogView = inflater.inflate(R.layout.dialog_change_data, null)
        builder.setView(dialogView)

        val title = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val acceptButton = dialogView.findViewById<Button>(R.id.saveButton)
        val editText = dialogView.findViewById<EditText>(R.id.newDataInput)

        title.text = "Восстановление пароля"
        editText.hint = "E-mail"
        editText.setCompoundDrawablesWithIntrinsicBounds(
            ContextCompat.getDrawable(
                this,
                R.drawable.mail
            ), null, null, null
        )
        acceptButton.text = "Отправить"

        val dialog = builder.create()
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        }

        acceptButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val email = editText.text.toString().trim { it <= ' ' }
                if (email.isEmpty()) {
                    editText.error = "Введите ваш email"
                    dialog.dismiss()
                    return
                }

                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(object : OnCompleteListener<Void?> {
                        override fun onComplete(task: Task<Void?>) {
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
                    })
            }
        })
        dialog.show()
    }
}
