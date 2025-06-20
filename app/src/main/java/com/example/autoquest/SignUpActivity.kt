package com.example.autoquest

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.autoquest.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

class SignUpActivity : AppCompatActivity() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storageReference: StorageReference =
        FirebaseStorage.getInstance().getReference("uploads")

    private val PICK_IMAGE_REQUEST: Int = 22
    // путь uri
    private var filePath: Uri? = null

    private var binding: ActivitySignupBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())


        // привязка макетов к объектам
        binding!!.avatarImageView.setOnClickListener { selectImage() }


        // кнопка подтверждения
        binding!!.acceptButton.setOnClickListener {

            // получение данных из полей ввода
            val email: String = binding!!.emailInput.text.toString().trim { it <= ' ' }

            val password: String =
                binding!!.passwordInput.text.toString().trim { it <= ' ' }
            val phoneNumber: String =
                binding!!.phoneInput.text.toString().trim { it <= ' ' }
            val name: String = binding!!.nameInput.text.toString().trim { it <= ' ' }


            // если пусто поле -> ошибка
            if (email.isEmpty()) {
                binding!!.emailInput.error = "E-mail не может быть пустым"
            }
            if (phoneNumber.isEmpty() || !validatePhoneNumber(phoneNumber)) {
                binding!!.phoneInput.error = "Номер телефона неверного формата"
            }
            if (name.isEmpty()) {
                binding!!.nameInput.error = "Имя не может быть пустым"
            }
            if (password.isEmpty()) {
                binding!!.passwordInput.error = "Пароль не может быть пустым"
            } else {

                // создание пользователя
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Получение FirebaseUser из AuthResult
                            val user: FirebaseUser? = task.result.user

                            saveUserDataToDatabase(user!!.uid, phoneNumber, name)

                            // выгрузка аватара
                            uploadImage()

                            Toast.makeText(
                                this@SignUpActivity,
                                "Успешная регистрация",
                                Toast.LENGTH_SHORT
                            ).show()
                            // вход в аккаунт сразу после регистрации
                            firebaseAuth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    startActivity(
                                        Intent(
                                            this@SignUpActivity,
                                            MainActivity::class.java
                                        )
                                    )
                                    finish()
                                }.addOnFailureListener {
                                    Toast.makeText(
                                        this@SignUpActivity,
                                        "Не удалось войти",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                this@SignUpActivity,
                                "Не удалось зарегистрироваться: " + task.exception!!.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
        }

        // кнопк перехода на страницу входа
        binding!!.loginButton.setOnClickListener {
            startActivity(
                Intent(
                    this@SignUpActivity, LoginActivity::class.java
                )
            )
        }
    }

    // функция выбора изображений для автара
    private fun selectImage() {
        // Defining Implicit Intent to mobile gallery
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(
            Intent.createChooser(intent, "Select Image from here..."),
            PICK_IMAGE_REQUEST
        )
    }


    // обработка выбора изображений
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == PICK_IMAGE_REQUEST) && (resultCode == RESULT_OK) && (data != null) && (data.data != null)) {
            // получение uri данных
            filePath = data.data
            try {
                // через bitmap устанавливаем картинку на view
                val bitmap: Bitmap =
                    MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                binding!!.avatarImageView.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // функция выгрузки изображений
    private fun uploadImage() {
        if (filePath != null) {
            // прогрессбар

            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()

            val ref: StorageReference =
                storageReference.child("user_avatars/" + firebaseAuth.uid)

            // добавление слушетелей на выгрузку или ошибку
            ref.putFile(filePath!!)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this@SignUpActivity, "Image Uploaded!!", Toast.LENGTH_SHORT)
                        .show()
                }.addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this@SignUpActivity, "Failed " + e.message, Toast.LENGTH_SHORT)
                        .show()
                }.addOnProgressListener { taskSnapshot ->
                    // проценты на прогрессбаре
                    val progress: Double =
                        (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    progressDialog.setMessage("Uploaded " + (progress.toInt()) + "%")
                }
        }
    }


    // Сохранение номера телефона и имени в базе данных
    private fun saveUserDataToDatabase(userId: String, phoneNumber: String, name: String) {
        val userRef: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("users").child(userId)
        userRef.child("phoneNumber").setValue(phoneNumber)
        userRef.child("name").setValue(name)
    }

    // Получение номера телефона и имени из базы данных
    private fun getPhoneNumberFromDatabase(userId: String) {
        val userRef: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val phoneNumber: String = snapshot.child("phoneNumber").value.toString()
                    val name: String = snapshot.child("name").value.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Обработка ошибок
            }
        })
    }

    companion object {
        private val PHONE_NUMBER_PATTERN: Pattern = Pattern.compile("^\\+\\d{11}$")

        fun validatePhoneNumber(phoneNumber: String?): Boolean {
            val matcher: Matcher = PHONE_NUMBER_PATTERN.matcher(phoneNumber)
            return matcher.matches()
        }
    }
}
