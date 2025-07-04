package com.example.autoquest

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.autoquest.databinding.ActivityCreateOfferBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.IOException
import java.util.UUID

class CreateOfferActivity : AppCompatActivity() {
    private var binding: ActivityCreateOfferBinding? = null

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseUser = firebaseAuth.currentUser

    private var storageReference: StorageReference? = null
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private var usersRef: DatabaseReference? = null
    private var offersRef: DatabaseReference? = null

    private var offerId: String? = null

    //  список путей url картинок
    private val filePaths: MutableList<Uri?> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateOfferBinding.inflate(
            layoutInflater
        )
        setContentView(binding!!.root)

        // инициализация ссылки на firebase бд
        storageReference = FirebaseStorage.getInstance().getReference("uploads")
        usersRef = firebaseDatabase.getReference("users")
        offersRef = firebaseDatabase.getReference("offers")
        // уникальный ключ генерируется для каждого оффера
        offerId = offersRef!!.push().key
        // бинд кнопки выбора изображений для оффера
        binding!!.selectButton.setOnClickListener { v: View? -> selectImage() }
        // бинд кнопки выгрузки оффера в firebase
        binding!!.uploadButton.setOnClickListener { v: View? ->

            uploadImage()
            uploadToDatabase()
            startActivity(Intent(this@CreateOfferActivity, MainActivity::class.java))
        }
    }

    // функция запускающая выбор картинок из галереи на телефоне
    private fun selectImage() {
        val intent = Intent()
        intent.setType("image/")
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(
            Intent.createChooser(intent, "Select image from here..."),
            PICK_IMAGE_REQUEST
        )
    }

    // обработка результатов выбора картинок
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            // объект bitmap для сохранения картинок
            val bitmaps: MutableList<Bitmap> = ArrayList()

            // если выбрано несколько
            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    // добавляем Uri в список
                    filePaths.add(imageUri)
                    try {
                        // получаем Bitmap для отображения превью
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                        bitmaps.add(bitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                // если выбрано одно изображение
            } else if (data.data != null) {
                val imageUri = data.data
                filePaths.add(imageUri)
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    bitmaps.add(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            // показываем выбранные изображения в гриде
            displaySelectedImages(bitmaps)
        }
    }

    // выводим изображения в GridView
    private fun displaySelectedImages(bitmaps: List<Bitmap>) {
        val adapter = AvatarAdapter(this, bitmaps)
        binding!!.imageGridView.adapter = adapter
    }

    // выгрузка изображений в Firebase
    private fun uploadImage() {
        if (filePaths != null && !filePaths.isEmpty()) {
            // прогресбар загрузки
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()

            for (uri in filePaths) {
                // создание пути для каждого файла для storage
                val ref = storageReference!!.child(
                    "offer_images/" + offerId + "/" + UUID.randomUUID().toString()
                )
                // загрузка и слушаем результат
                ref.putFile(uri!!).addOnSuccessListener {

                    progressDialog.dismiss()
                    Toast.makeText(
                        this@CreateOfferActivity,
                        "Изображения загружены",
                        Toast.LENGTH_SHORT
                    ).show()
                }.addOnFailureListener { e: Exception ->

                    progressDialog.dismiss()
                    Toast.makeText(
                        this@CreateOfferActivity,
                        "Не удалось загрузить изображения " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }.addOnProgressListener { taskSnapshot: UploadTask.TaskSnapshot ->
                    // обновление диалога програссбара
                    val progress =
                        (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
                }
            }
        }
    }

    //выгрузка остальных данных ( не изображений) в Firebase DATABASE (не firestore)
    private fun uploadToDatabase() {
        // данные из полей ввода
        val brand = (findViewById<View>(R.id.brandET) as EditText).text.toString()
        val model = (findViewById<View>(R.id.modelET) as EditText).text.toString()
        val generation = (findViewById<View>(R.id.generationET) as EditText).text.toString()
        val price = (findViewById<View>(R.id.priceET) as EditText).text.toString()
        val year = (findViewById<View>(R.id.yearET) as EditText).text.toString()
        val enginePower = (findViewById<View>(R.id.enginePowerET) as EditText).text.toString()
        val fuelConsumption =
            (findViewById<View>(R.id.fuelConsumptionET) as EditText).text.toString()
        val description = (findViewById<View>(R.id.descriptionET) as EditText).text.toString()

        val ownerId = firebaseUser!!.uid

        // получаем телефон владельца из базы чтобы добавить в объявление
        usersRef!!.child(ownerId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ownerPhoneNumber = snapshot.child("phoneNumber").value.toString()
                val item = Offer(
                    offerId,
                    brand,
                    model,
                    generation,
                    price,
                    year,
                    description,
                    enginePower,
                    fuelConsumption,
                    ownerId,
                    ownerPhoneNumber
                )

                if (offerId != null) {
                    // Сохранение оферв в базу данных Realtime Database
                    offersRef!!.child(offerId!!).setValue(item)
                        .addOnSuccessListener { aVoid: Void? ->
                            Toast.makeText(
                                this@CreateOfferActivity,
                                "Объявление добавлено",
                                Toast.LENGTH_SHORT
                            ).show()
                            clearEditTextFields() // очистка всех полей
                            startActivity(
                                Intent(
                                    this@CreateOfferActivity,
                                    MainActivity::class.java
                                )
                            )
                        }.addOnFailureListener { e: Exception? ->
                        Toast.makeText(
                            this@CreateOfferActivity,
                            "Не удалось добавить данные",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // для ошибок
            }
        })
    }


    // функция очистки всех полей ввода
    private fun clearEditTextFields() {
        (findViewById<View>(R.id.brandET) as EditText).setText("")
        (findViewById<View>(R.id.modelET) as EditText).setText("")
        (findViewById<View>(R.id.generationET) as EditText).setText("")
        (findViewById<View>(R.id.priceET) as EditText).setText("")
        (findViewById<View>(R.id.yearET) as EditText).setText("")
        (findViewById<View>(R.id.enginePowerET) as EditText).setText("")
        (findViewById<View>(R.id.fuelConsumptionET) as EditText).setText("")
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 22
        private const val TAG = "CreateOfferActivity"
    }
}
