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

    private val filePaths: MutableList<Uri?>? = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateOfferBinding.inflate(
            layoutInflater
        )
        setContentView(binding!!.root)

        storageReference = FirebaseStorage.getInstance().getReference("uploads")
        usersRef = firebaseDatabase.getReference("users")
        offersRef = firebaseDatabase.getReference("offers")
        offerId = offersRef!!.push().key

        binding!!.selectButton.setOnClickListener { v: View? -> selectImage() }

        binding!!.uploadButton.setOnClickListener { v: View? ->
            uploadImage()
            uploadToDatabase()
            startActivity(Intent(this@CreateOfferActivity, MainActivity::class.java))
        }
    }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val bitmaps: MutableList<Bitmap> = ArrayList()

            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    filePaths!!.add(imageUri)
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                        bitmaps.add(bitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } else if (data.data != null) {
                val imageUri = data.data
                filePaths!!.add(imageUri)
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    bitmaps.add(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            displaySelectedImages(bitmaps)
        }
    }

    private fun displaySelectedImages(bitmaps: List<Bitmap>) {
        val adapter = AvatarAdapter(this, bitmaps)
        binding!!.imageGridView.adapter = adapter
    }

    private fun uploadImage() {
        if (filePaths != null && !filePaths.isEmpty()) {
            // Code for showing progressDialog while uploading
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()

            for (uri in filePaths) {
                // Defining the child of storageReference
                val ref = storageReference!!.child(
                    "offer_images/" + offerId + "/" + UUID.randomUUID().toString()
                )
                // adding listeners on upload or failure of image
                ref.putFile(uri!!).addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot? ->
                    // Image uploaded successfully
                    // Dismiss dialog
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@CreateOfferActivity,
                        "Изображения загружены",
                        Toast.LENGTH_SHORT
                    ).show()
                }.addOnFailureListener { e: Exception ->
                    // Error, Image not uploaded
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@CreateOfferActivity,
                        "Не удалось загрузить изображения " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }.addOnProgressListener { taskSnapshot: UploadTask.TaskSnapshot ->
                    // Progress Listener for loading percentage on the dialog box
                    val progress =
                        (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
                }
            }
        }
    }

    private fun uploadToDatabase() {
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

        usersRef!!.child(ownerId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ownerPhoneNumber = snapshot.child("phoneNumber").getValue(
                    String::class.java
                )
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
                    // Сохранение offer в базу данных Realtime Database
                    offersRef!!.child(offerId!!).setValue(item)
                        .addOnSuccessListener { aVoid: Void? ->
                            Toast.makeText(
                                this@CreateOfferActivity,
                                "Объявление добавлено",
                                Toast.LENGTH_SHORT
                            ).show()
                            clearEditTextFields()
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
                // Handle possible errors.
            }
        })
    }

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
