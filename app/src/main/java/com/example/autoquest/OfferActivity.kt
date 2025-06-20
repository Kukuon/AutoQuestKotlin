package com.example.autoquest

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.GridView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.autoquest.databinding.ActivityOfferBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageReference

class OfferActivity : AppCompatActivity() {
    var binding: ActivityOfferBinding? = null

    private var imageUrls: MutableList<String>? = null

    private var imageAdapter: ImageAdapter? = null
    private var parameterAdapter: ParameterAdapter? = null


    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseUser: FirebaseUser? = firebaseAuth.currentUser

    private var parametersRef: DatabaseReference? = null
    private var userRef: DatabaseReference? = null
    private var userRef1: DatabaseReference? = null
    private var storageReferenceImages: StorageReference? = null
    private var storageReferenceAvatar: StorageReference? = null

    private var isAdmin: String? = null
    private var ownerPhoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfferBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        // Получение ID объявления
        val offerId: String? = intent.getStringExtra("offerId")

        // Инициализируем списки для изображений и параметров
        imageUrls = ArrayList()

        binding!!.returnButton.setOnClickListener {
            startActivity(
                Intent(
                    this@OfferActivity, MainActivity::class.java
                )
            )
        }
        binding!!.addFavoriteOfferButton.setOnClickListener { toggleFavoriteOffer(offerId) }
        binding!!.deleteOfferButton.setOnClickListener { deleteOffer(offerId) }

        loadOfferData(offerId)
        loadOfferImages(offerId)

        binding!!.showPhoneButton.setOnClickListener { showPhoneNumberDialog() }
    }


    private fun loadOfferData(offerId: String?) {
        // получение данных объявления
        parametersRef = FirebaseDatabase.getInstance().getReference("offers").child((offerId)!!)


        parametersRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // получаем параметры из снимка данных
                    val brand: String = snapshot.child("brand").value.toString()

                    val model: String = snapshot.child("model").value.toString()
                    val generation: String = snapshot.child("generation").value.toString()
                    val price: String = snapshot.child("price").value.toString()
                    val year: String = snapshot.child("year").value.toString()
                    val description: String = snapshot.child("description").value.toString()
                    val enginePower: String = snapshot.child("enginePower").value.toString()
                    val fuelConsumption: String = snapshot.child("fuelConsumption").value.toString()
                    ownerPhoneNumber = snapshot.child("ownerPhoneNumber").value.toString()
                    val ownerId: String = snapshot.child("ownerId").value.toString()

                    val parameters: MutableList<Parameter> = ArrayList()

                    // загрузка аватарки владельца объявления
                    loadUserAvatar(ownerId)

                    // создаем объекты параметров и добавляем их в список
                    parameters.add(Parameter("Бренд", brand))
                    parameters.add(Parameter("Модель", model))
                    parameters.add(Parameter("Поколение", generation))
                    parameters.add(Parameter("Год", year))
                    parameters.add(Parameter("Мощность двигателя", enginePower))
                    parameters.add(Parameter("Расход топлива", fuelConsumption))

                    // привязка текста к соответсвующим textView
                    binding!!.titleTV.text = "$brand $model $generation"
                    binding!!.priceTV.text = "$price ₽"
                    binding!!.descriptionTV.text = description

                    // установка имени пользователя в объявлении, взтого из firebae databse
                    userRef =
                        FirebaseDatabase.getInstance().getReference("users").child((ownerId))
                    userRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            binding!!.usernameTV.text = snapshot.child("name").value.toString()
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })

                    if (firebaseUser != null) {
                        userRef1 = FirebaseDatabase.getInstance().getReference("users")
                            .child(firebaseUser.uid)
                        userRef1!!.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                isAdmin = snapshot.child("isAdmin").value.toString()
                                if (isAdmin == "true") {
                                    binding!!.deleteOfferButton.visibility = View.VISIBLE
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                binding!!.deleteOfferButton.visibility = View.GONE
                            }
                        })
                    }


                    val parametersGridView: GridView = findViewById(R.id.parametersGridView)
                    parameterAdapter = ParameterAdapter(this@OfferActivity, parameters)
                    parametersGridView.adapter = parameterAdapter

                    // уведомляем адаптер об изменениях
                    parameterAdapter!!.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@OfferActivity, "Ошибка загрузки параметров", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }


    private fun loadOfferImages(offerId: String?) {
        // Получение списка файлов изображений из Firebase Storage
        storageReferenceImages =
            FirebaseStorage.getInstance().getReference("uploads/offer_images/$offerId")
        storageReferenceImages!!.listAll()
            .addOnSuccessListener { listResult: ListResult ->
                for (item: StorageReference in listResult.items) {
                    // Получение URL-адреса для каждого изображения и добавление его в список
                    item.getDownloadUrl().addOnSuccessListener { uri: Uri ->
                        imageUrls!!.add(uri.toString())
                        // Уведомление адаптера об изменениях
                        imageAdapter!!.notifyDataSetChanged()
                    }.addOnFailureListener {
                        // Обработка ошибок загрузки изображения
                        Toast.makeText(
                            this@OfferActivity,
                            "Ошибка загрузки изображения",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("OfferActivity", "Ошибка загрузки изображения")
                    }
                }
            }.addOnFailureListener {
                // Обработка ошибок получения списка изображений
                Toast.makeText(
                    this@OfferActivity,
                    "Ошибка получения списка изображений",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("OfferActivity", "Ошибка получения списка изображений")
            }

        val imageScrollContainer: RecyclerView = findViewById(R.id.imageScrollContainer)
        val layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        imageScrollContainer.setLayoutManager(layoutManager)
        imageAdapter = ImageAdapter(this, imageUrls)
        imageScrollContainer.setAdapter(imageAdapter)
    }


    private fun loadUserAvatar(ownerId: String?) {
        storageReferenceAvatar =
            FirebaseStorage.getInstance().getReference("uploads").child("user_avatars/$ownerId")
        // Загружаем аватар в виде массива байт
        val ONE_MEGABYTE: Long = (1024 * 1024).toLong()
        storageReferenceAvatar!!.getBytes(ONE_MEGABYTE)
            .addOnSuccessListener { bytes ->
                val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding!!.avatarImageView.setImageBitmap(bitmap)
            }.addOnFailureListener { // Обработка ошибки
                Toast.makeText(
                    this@OfferActivity,
                    "Ошибка в загрузке фото профиля",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    // функция показа диаологового окна
    private fun showPhoneNumberDialog() {
        // Создаем билдер для AlertDialog
        val builder: AlertDialog.Builder = AlertDialog.Builder(
            this
        )

        // Получаем LayoutInflater
        val inflater: LayoutInflater = this.layoutInflater

        // Создаем View для диалогового окна из пользовательского макета
        val dialogView: View = inflater.inflate(R.layout.dialog_confirmation, null)
        builder.setView(dialogView)

        // Настраиваем содержимое диалогового окна
        val phoneTextView: TextView = dialogView.findViewById(R.id.dialog_phone_number)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        title.text = "Позвонить на номер?"
        phoneTextView.text = ownerPhoneNumber

        val acceptButton: ImageButton = dialogView.findViewById(R.id.acceptButton)
        val cancelButton: ImageButton = dialogView.findViewById(R.id.cancelButton)

        // Создаем и показываем AlertDialog
        val dialog: AlertDialog = builder.create()

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        }

        acceptButton.setOnClickListener {
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.setData(Uri.parse("tel:$ownerPhoneNumber"))
                startActivity(dialIntent)
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    "Не удалось совершить вызов",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }


    private fun deleteOffer(offerId: String?) {
        // Получаем ссылки на базу данных Firebase
        val databaseReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("offers")
        // Используем тот же путь, что и при загрузке изображений
        val storageReferenceImages: StorageReference =
            FirebaseStorage.getInstance().getReference("uploads/offer_images/$offerId")

        // Удаляем все изображения оффера
        storageReferenceImages.listAll()
            .addOnSuccessListener { listResult: ListResult ->
                if (listResult.items.isEmpty()) {
                    // Если нет файлов, сразу удаляем из базы данных
                    deleteFromDatabase(databaseReference, offerId)
                } else {
                    // Счетчик для отслеживания успешных удалений
                    val deleteCount: IntArray = intArrayOf(0)
                    val totalFiles: Int = listResult.items.size

                    for (item: StorageReference in listResult.items) {
                        item.delete().addOnSuccessListener {
                            deleteCount[0]++
                            // Если все файлы удалены, удаляем из базы
                            if (deleteCount[0] == totalFiles) {
                                deleteFromDatabase(databaseReference, offerId)
                            }
                        }.addOnFailureListener { e: Exception? ->
                            Log.e("OfferActivity", "Ошибка удаления файла", e)
                            deleteCount[0]++
                            // Продолжаем и при ошибке удаления файла
                            if (deleteCount[0] == totalFiles) {
                                deleteFromDatabase(databaseReference, offerId)
                            }
                        }
                    }
                }
            }.addOnFailureListener { e: Exception? ->
                Log.e("OfferActivity", "Ошибка при получении списка файлов", e)
                // Если не удалось получить список файлов все равно пытаемся удалить
                deleteFromDatabase(databaseReference, offerId)
            }
    }

    // Функция для удаления оффера из базы данных
    private fun deleteFromDatabase(databaseReference: DatabaseReference, offerId: String?) {
        databaseReference.child((offerId)!!).removeValue()
            .addOnSuccessListener {
                // высплывающее уведомление
                Toast.makeText(
                    this@OfferActivity,
                    "Объявление успешно удалено",
                    Toast.LENGTH_SHORT
                ).show()
                // и возврат на исходное активитит
                startActivity(Intent(this@OfferActivity, MainActivity::class.java))
            }.addOnFailureListener { e ->
                Toast.makeText(this@OfferActivity, "Ошибка удаления объявления", Toast.LENGTH_SHORT)
                    .show()
                Log.e("OfferActivity", "Ошибка удаления объявления", e)
            }
    }

// функция для доавления или удаления избранного
    private fun toggleFavoriteOffer(offerId: String?) {
        if (firebaseUser != null) {
            // Получаем ссылку на узел с избранными объявлениями пользователя
            userRef = FirebaseDatabase.getInstance().getReference("users")
                .child((firebaseAuth.uid)!!).child("favorites_offers")

            // Добавляем слушатель для получения текущего состояния избранных объявлений
            userRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val favoritesList: MutableList<String?> = ArrayList()
                    if (snapshot.exists()) {
                        for (favoriteSnapshot: DataSnapshot in snapshot.getChildren()) {
                            val favoriteOfferId: String = favoriteSnapshot.value.toString()
                            favoritesList.add(favoriteOfferId)
                        }
                    }

                    // Проверяем, есть ли уже это объявление в избранном
                    if (favoritesList.contains(offerId)) {
                        // Если да, удаляем его
                        favoritesList.remove(offerId)
                        userRef!!.setValue(favoritesList)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@OfferActivity,
                                    "Объявление удалено из избранного",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Обновить кнопку или UI
                                binding!!.addFavoriteOfferButton.setImageResource(R.drawable.favorite_off_svgrepo_com)
                            }.addOnFailureListener { e: Exception? ->
                                Toast.makeText(
                                    this@OfferActivity,
                                    "Ошибка удаления из избранного",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("OfferActivity", "Ошибка удаления из избранного", e)
                            }
                    } else {
                        // Если нет, добавляем его
                        favoritesList.add(offerId)
                        userRef!!.setValue(favoritesList)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@OfferActivity,
                                    "Объявление добавлено в избранное",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Обновить кнопку или UI
                                binding!!.addFavoriteOfferButton.setImageResource(R.drawable.favorite_svgrepo_com)
                            }.addOnFailureListener { e: Exception? ->
                                Toast.makeText(
                                    this@OfferActivity,
                                    "Ошибка добавления в избранное",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("OfferActivity", "Ошибка добавления в избранное", e)
                            }
                    }
                }

                // ошибка
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@OfferActivity,
                        "Ошибка получения избранного",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("OfferActivity", "Ошибка получения избранного", error.toException())
                }
            })
        } else {
            Toast.makeText(
                this,
                "Чтобы добавить в избранное, необходимо войти в аккаунт",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
