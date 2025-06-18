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

class ProfileFragment() : Fragment() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseUser: FirebaseUser? = firebaseAuth.getCurrentUser()

    private var storageReferenceAvatar: StorageReference? = null
    private var userRef: DatabaseReference? = null

    private var binding: FragmentProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        if (firebaseUser != null) {
            storageReferenceAvatar = FirebaseStorage.getInstance().getReference("uploads")
                .child("user_avatars/" + firebaseUser.getUid())
            userRef = FirebaseDatabase.getInstance().getReference().child("users")
                .child(firebaseUser.getUid())

            updateImageView()

            userRef!!.child("name").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name: String? = snapshot.getValue(String::class.java)
                    if (name != null) {
                        binding!!.usernameTV.setText(name)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибок
                }
            })

            userRef!!.child("phoneNumber").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val phoneNumber: String? = snapshot.getValue(String::class.java)
                    if (phoneNumber != null) {
                        binding!!.phoneTV.setText(phoneNumber)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибок
                }
            })

            binding!!.emailTV.setText(firebaseUser.getEmail())
            binding!!.idTV.setText("ID " + firebaseUser.getUid())

            binding!!.exitButton.setOnClickListener(View.OnClickListener({ v: View? ->
                showConfirmationDialog(
                    "Выйти из аккаунта?",
                    0
                )
            }))

            binding!!.myOffersButton.setOnClickListener(View.OnClickListener({ v: View? ->
                if (firebaseUser != null) {
                    startActivity(Intent(getContext(), MyOffersActivity::class.java))
                } else {
                    Toast.makeText(
                        getContext(),
                        "Невозможно открыть объявления",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }))

            binding!!.changeNameButton.setOnClickListener(View.OnClickListener({ v: View? ->
                showChangeDataDialog(
                    "Изменить имя",
                    0
                )
            }))
            binding!!.phoneTV.setOnClickListener(View.OnClickListener({ v: View? ->
                showChangeDataDialog(
                    "Изменить телефон",
                    1
                )
            }))
            binding!!.passwordTV.setOnClickListener(View.OnClickListener({ v: View? ->
                showChangeDataDialog(
                    "Изменить пароль",
                    2
                )
            }))

            binding!!.avatarImageView.setOnClickListener(View.OnClickListener({ v: View? -> selectImage() }))

            return binding!!.getRoot()
        } else {
            val rootView: View = inflater.inflate(R.layout.fragment_unlogged, container, false)
            val goToLoginButton: Button = rootView.findViewById(R.id.goToLoginButton)
            goToLoginButton.setOnClickListener(View.OnClickListener({ v: View? ->
                startActivity(
                    Intent(getContext(), LoginActivity::class.java)
                )
            }))
            return rootView
        }
    }

    private fun selectImage() {
        val intent: Intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(
            Intent.createChooser(intent, "Выберите изображение"),
            PICK_IMAGE_REQUEST
        )
    }

    private fun updateImageView() {
        val ONE_MEGABYTE: Long = (1024 * 1024).toLong()
        storageReferenceAvatar!!.getBytes(ONE_MEGABYTE)
            .addOnSuccessListener(OnSuccessListener({ bytes: ByteArray ->
                val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding!!.avatarImageView.setImageBitmap(bitmap)
            })).addOnFailureListener(OnFailureListener({ e: Exception? ->
            Toast.makeText(getContext(), "Ошибка в загрузке фото профиля", Toast.LENGTH_SHORT)
                .show()
        }))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == PICK_IMAGE_REQUEST) && (resultCode == Activity.RESULT_OK) && (data != null) && (data.getData() != null)) {
            val filePath: Uri? = data.getData()
            if (filePath != null) {
                storageReferenceAvatar!!.putFile(filePath)
                    .addOnSuccessListener(OnSuccessListener({ taskSnapshot: UploadTask.TaskSnapshot? ->
                        Toast.makeText(
                            getActivity(),
                            "Изображение успешно загружено",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateImageView()
                    }))
                    .addOnFailureListener(OnFailureListener({ e: Exception ->
                        Toast.makeText(
                            getActivity(),
                            "Ошибка загрузки изображения: " + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }))
            }
        }
    }

    private fun showConfirmationDialog(parameterName: String, mode: Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater: LayoutInflater = this.getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.dialog_confirmation, null)
        builder.setView(dialogView)

        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        title.setText(parameterName)

        val acceptButton: ImageButton = dialogView.findViewById(R.id.acceptButton)
        val cancelButton: ImageButton = dialogView.findViewById(R.id.cancelButton)

        val dialog: AlertDialog = builder.create()

        if (dialog.getWindow() != null) {
            dialog.getWindow()!!.setBackgroundDrawableResource(android.R.color.transparent)
        }

        acceptButton.setOnClickListener(View.OnClickListener { v: View? ->
            if (firebaseUser != null) {
                val intent: Intent = getActivity()!!.getIntent()
                when (mode) {
                    0 -> {
                        firebaseAuth.signOut()
                        dialog.dismiss()
                        getActivity()!!.finish()
                        startActivity(intent)
                    }

                    1 -> userRef!!.removeValue()
                        .addOnSuccessListener(OnSuccessListener { unused: Void? ->
                            storageReferenceAvatar!!.delete().addOnSuccessListener(
                                OnSuccessListener { avoid: Void? ->
                                    firebaseUser.delete().addOnSuccessListener(
                                        OnSuccessListener { unused1: Void? ->
                                            requireActivity().finish()
                                            startActivity(intent)
                                        }
                                    )
                                }
                            )
                        })
                }
            }
        })

        cancelButton.setOnClickListener(View.OnClickListener({ v: View? -> dialog.dismiss() }))
        dialog.show()
    }

    private fun showChangeDataDialog(parameterName: String, mode: Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater: LayoutInflater = this.getLayoutInflater()
        val dialogView: View = inflater.inflate(R.layout.dialog_change_data, null)
        builder.setView(dialogView)

        val title: TextView = dialogView.findViewById(R.id.dialogTitle)
        val editText: EditText = dialogView.findViewById(R.id.newDataInput)
        val saveButton: Button = dialogView.findViewById(R.id.saveButton)

        title.setText(parameterName)

        val dialog: AlertDialog = builder.create()

        if (dialog.getWindow() != null) {
            dialog.getWindow()!!.setBackgroundDrawableResource(android.R.color.transparent)
        }

        saveButton.setOnClickListener(View.OnClickListener { v: View? ->
            val newData: String = editText.getText().toString()
            if (firebaseUser != null) {
                if (!newData.isEmpty()) {
                    when (mode) {
                        0 -> userRef!!.child("name").setValue(newData)
                            .addOnCompleteListener(OnCompleteListener { task: Task<Void?> ->
                                if (task.isSuccessful()) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Имя изменено на " + newData,
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
                            })

                        1 -> userRef!!.child("phoneNumber").setValue(newData)
                            .addOnCompleteListener(OnCompleteListener { task: Task<Void?> ->
                                if (task.isSuccessful()) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Телефон изменен на " + newData,
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
                            })

                        2 -> firebaseUser.updatePassword(newData)
                            .addOnCompleteListener(OnCompleteListener { task: Task<Void?> ->
                                if (task.isSuccessful()) {
                                    Log.d("UpdatePassword", "User password updated.")
                                    Toast.makeText(
                                        getContext(),
                                        "Пароль успешно изменен",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Log.d(
                                        "UpdatePassword",
                                        "Error password not updated.",
                                        task.getException()
                                    )
                                    Toast.makeText(
                                        getContext(),
                                        "Ошибка при изменении пароля",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                dialog.dismiss()
                            })
                    }
                } else {
                    editText.setError("Поле ввода должно быть заполнено")
                }
            }
        })
        dialog.show()
    }

    companion object {
        private val PICK_IMAGE_REQUEST: Int = 22
    }
}