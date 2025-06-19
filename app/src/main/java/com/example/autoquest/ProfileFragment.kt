package com.example.autoquest

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.autoquest.databinding.FragmentProfileBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask

class ProfileFragment : Fragment() {
    // объекты текущего пользователя
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseUser: FirebaseUser? = firebaseAuth.currentUser

    // объекты firebase storage ссылки
    private var storageReferenceAvatar: StorageReference? = null
    private var userRef: DatabaseReference? = null

    // биндинг разметка
    private var binding: FragmentProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)


        // если авторизован
        if (firebaseUser != null) {
            // получение ссылки на аватарку
            storageReferenceAvatar = FirebaseStorage.getInstance().getReference("uploads")
                .child("user_avatars/" + firebaseUser.uid)
            // получение ссылки на пользователя
            userRef = FirebaseDatabase.getInstance().getReference().child("users")
                .child(firebaseUser.uid)

            // загрузка аватара
            updateImageView()

            // загрузка имени из бд
            userRef!!.child("name").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name: String? = snapshot.getValue(String::class.java)
                    if (name != null) {
                        binding!!.usernameTV.text = name
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибок
                }
            })

            // загрузка номера телефона из бд
            userRef!!.child("phoneNumber").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val phoneNumber: String? = snapshot.getValue(String::class.java)
                    if (phoneNumber != null) {
                        binding!!.phoneTV.text = phoneNumber
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибок
                }
            })

            // установка почты и id в интерфейс
            binding!!.emailTV.text = firebaseUser.email
            binding!!.idTV.text = "ID " + firebaseUser.uid

            // кнопка выхода
            binding!!.exitButton.setOnClickListener {
                showConfirmationDialog(
                    "Выйти из аккаунта?",
                    0
                )
            }

            // кнопка перехода к своим объявлениям
            binding!!.myOffersButton.setOnClickListener {
                if (firebaseUser != null) {
                    startActivity(Intent(context, MyOffersActivity::class.java))
                } else {
                    Toast.makeText(
                        context,
                        "Невозможно открыть объявления",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // кнопки изменения данных пользователя
            binding!!.changeNameButton.setOnClickListener {
                showChangeDataDialog("Изменить имя", 0)
            }
            binding!!.phoneTV.setOnClickListener {
                showChangeDataDialog("Изменить телефон", 1)
            }
            binding!!.passwordTV.setOnClickListener { showChangeDataDialog(
                "Изменить пароль", 2
            )
            }

            // нажатие на аватарку дает выбрать новую аватарку
            binding!!.avatarImageView.setOnClickListener { selectImage() }

            return binding!!.getRoot()
        } else {
            // если пользователь не авторизован то показать экран с кнопкой входа
            val rootView: View = inflater.inflate(R.layout.fragment_unlogged, container, false)
            val goToLoginButton: Button = rootView.findViewById(R.id.goToLoginButton)
            goToLoginButton.setOnClickListener(View.OnClickListener {
                startActivity(
                    Intent(context, LoginActivity::class.java)
                )
            })
            return rootView
        }
    }


    // функция выбора изображения
    private fun selectImage() {
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(
            Intent.createChooser(intent, "Выберите изображение"),
            PICK_IMAGE_REQUEST
        )
    }

    // обновление изображения профиля
    private fun updateImageView() {
        val ONE_MEGABYTE: Long = (1024 * 1024).toLong()
        storageReferenceAvatar!!.getBytes(ONE_MEGABYTE)
            .addOnSuccessListener { bytes: ByteArray ->
                val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding!!.avatarImageView.setImageBitmap(bitmap)
            }.addOnFailureListener {
                Toast.makeText(context, "Ошибка в загрузке фото профиля", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    // обработка результата выбора изображения
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == PICK_IMAGE_REQUEST) && (resultCode == Activity.RESULT_OK) && (data != null) && (data.data != null)) {
            val filePath: Uri? = data.data
            if (filePath != null) {
                storageReferenceAvatar!!.putFile(filePath)
                    .addOnSuccessListener {
                        Toast.makeText(
                            activity,
                            "Изображение успешно загружено",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateImageView()
                    }
                    .addOnFailureListener { e: Exception ->
                        Toast.makeText(
                            activity,
                            "Ошибка загрузки изображения: " + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    // диалог подветрждения действия удаления и выхода
    private fun showConfirmationDialog(parameterName: String, mode: Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater: LayoutInflater = this.getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.dialog_confirmation, null)
        builder.setView(dialogView)

        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        title.text = parameterName

        val acceptButton: ImageButton = dialogView.findViewById(R.id.acceptButton)
        val cancelButton: ImageButton = dialogView.findViewById(R.id.cancelButton)

        val dialog: AlertDialog = builder.create()

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        }

        acceptButton.setOnClickListener {
            if (firebaseUser != null) {
                val intent: Intent = requireActivity().intent
                when (mode) {
                    0 -> {
                        // выход из профиля
                        firebaseAuth.signOut()
                        dialog.dismiss()
                        requireActivity().finish()
                        startActivity(intent)
                    }

                    1 -> userRef!!.removeValue()
                            // удаление аккаунта
                        .addOnSuccessListener {
                            storageReferenceAvatar!!.delete().addOnSuccessListener {
                                firebaseUser.delete().addOnSuccessListener {
                                    requireActivity().finish()
                                    startActivity(intent)
                                }
                            }
                        }
                }
            }
        }

        // кнопка отмены диалога
        cancelButton.setOnClickListener { dialog.dismiss() }
        // показ диалога
        dialog.show()
    }

    // диалог изменения данных имени телефона и пароля
    private fun showChangeDataDialog(parameterName: String, mode: Int) {
        // билдер диалога
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        // layout для диалога
        val inflater: LayoutInflater = this.getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.dialog_change_data, null)
        builder.setView(dialogView)

        // привязка макета к объектам
        val title: TextView = dialogView.findViewById(R.id.dialogTitle)
        val editText: EditText = dialogView.findViewById(R.id.newDataInput)
        val saveButton: Button = dialogView.findViewById(R.id.saveButton)


        // заголовок диалога
        title.text = parameterName


        // создание диалога
        val dialog: AlertDialog = builder.create()

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        }

        // кнопка сохранения изменений
        saveButton.setOnClickListener {
            val newData: String = editText.text.toString()
            // пользователь авторизован
            if (firebaseUser != null) {
                if (newData.isNotEmpty()) {
                    when (mode) {
                        // смена имения
                        0 -> userRef!!.child("name").setValue(newData)
                            .addOnCompleteListener { task: Task<Void?> ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Имя изменено на $newData",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Не удалось изменить имя",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                dialog.dismiss()
                            }
                        // смена телеофна
                        1 -> userRef!!.child("phoneNumber").setValue(newData)
                            .addOnCompleteListener { task: Task<Void?> ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Телефон изменен на $newData",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Не удалось изменить телефон",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                dialog.dismiss()
                            }

                        // смена пароля
                        2 -> firebaseUser.updatePassword(newData)
                            .addOnCompleteListener { task: Task<Void?> ->
                                if (task.isSuccessful) {
                                    Log.d("UpdatePassword", "User password updated.")
                                    Toast.makeText(
                                        context,
                                        "Пароль успешно изменен",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Log.d(
                                        "UpdatePassword",
                                        "Error password not updated.",
                                        task.exception
                                    )
                                    Toast.makeText(
                                        context,
                                        "Ошибка при изменении пароля",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                dialog.dismiss()
                            }
                    }
                } else {
                    editText.error = "Поле ввода должно быть заполнено"
                }
            }
        }
        // показ диалога
        dialog.show()
    }

    companion object {
        private const val PICK_IMAGE_REQUEST: Int = 22
    }
}