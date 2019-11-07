package com.dscpwani.firebaselogin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    lateinit var imageView: ImageView
    lateinit var editTextName: EditText
    lateinit var editTextPhone: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    lateinit var storage: FirebaseStorage
    private val GALLERY = 1
    private val CAMERA = 2
    private var filePath: Uri? = null
    lateinit var bitmap: Bitmap
    lateinit var byteArrayOutputStream: ByteArrayOutputStream

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        imageView = findViewById(R.id.imageView)
        editTextName = findViewById(R.id.editTextName)
        editTextPhone = findViewById(R.id.editTextPhone)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        //initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        //initialize storage
        storage = FirebaseStorage.getInstance()

        imageView.setOnClickListener {
            showPictureDialog()
        }

        readData(auth.currentUser?.uid.toString())

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_profile, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
            }
            R.id.action_save -> {
                saveDetails()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //picture dialog
    private fun showPictureDialog() {
        val pictureDialog = android.app.AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Photo Gallery", "Camera")
        pictureDialog.setItems(
            pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }

    //from gallery
    private fun choosePhotoFromGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        startActivityForResult(galleryIntent, GALLERY)
    }

    //from camera
    private fun takePhotoFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 0) {
            return
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                filePath = data.data
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, filePath)
                    val bitmapResized = Bitmap.createScaledBitmap(bitmap, 250, 250, true)
                    imageView.setImageBitmap(bitmapResized)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
                }
            }

        } else if (requestCode == CAMERA) {
            try {
                bitmap = data?.getExtras()!!.get("data") as Bitmap

                val bitmapResized = Bitmap.createScaledBitmap(bitmap, 250, 250, true)

                imageView.setImageBitmap(bitmapResized)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDetails() {
        val name = editTextName.text.toString()
        val phone = editTextPhone.text.toString()

        // Create a storage reference from our app
        var storageRef = storage.reference
        // Create a child reference
        var imagesRef: StorageReference? = storageRef.child("images")

        if (TextUtils.isEmpty(name)) {
            editTextName.error = "Cannot be empty"
        } else if (TextUtils.isEmpty(phone)) {
            editTextPhone.error = "Cannot be empty"
        } else {
            //save details
            val userId = auth.currentUser!!.uid
            database.child("users").child(userId).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        Log.w("PROFILE ACTIVITY", "getUser:onCancelled", p0.toException())
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        // Get user value
                        //val user = p0.getValue(User::class.java)
                      //database.child("users").child(userId).push().key
                        val user = p0.child("users").child(userId)

                        if (!user.exists()) {
                            // User is null, error out
                            Log.e("PROFILE ACTIVITY", "User $userId is unexpectedly null")
                            Toast.makeText(
                                baseContext,
                                "Error: could not fetch user.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Write new post
                            if (filePath != null) {
                                //byteArrayOutputStream  = ByteArrayOutputStream()
                                //bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                                //val data = byteArrayOutputStream.toByteArray()

                                // imageView.isDrawingCacheEnabled = true
                                // imageView.buildDrawingCache()
                                val bitmap = (imageView.drawable as BitmapDrawable).bitmap
                                val baos = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                                val data = baos.toByteArray()

                                val uploadTask = imagesRef?.putBytes(data!!)

                                //  val urlTask =
                                uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                                    if (!task.isSuccessful) {
                                        task.exception?.let {
                                            throw it
                                        }
                                    }
                                    return@Continuation imagesRef?.downloadUrl
                                })?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val downloadUri = task.result
                                        Log.d("IMAGE URL:", downloadUri.toString())
                                        writeNewPost(
                                            userId,
                                            editTextName.text.toString(),
                                            editTextPhone.text.toString(),
                                            downloadUri.toString()
                                        )
                                    } else {

                                        // Handle failures
                                    }
                                }?.addOnFailureListener {

                                    Toast.makeText(
                                        baseContext,
                                        "Process Failed. Try again",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    baseContext,
                                    "Please Upload an Image",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }

                    }
                }
            )

        }

    }

    private fun writeNewPost(
        userId: String,
        name: String,
        phone: String,
        imageUrl: String
    ) {

        val key = database.child("users").child(userId).push().key
        if (key == null) {
            Log.w("PRODUCT ACTIVITY", "Couldn't get push key for posts")
            return
        }
        val data = User(name, phone, imageUrl)
        database.child("users").child(userId).setValue(data)
        imageView.setImageResource(R.drawable.ic_account)
        editTextName.text.clear()
        editTextPhone.text.clear()

        readData(userId)
    }


    private fun readData(userId: String) {

        database.addValueEventListener(
            object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    val user = p0.getValue(User::class.java)
                    if (user != null) {
                        val name = p0.child("users").child(userId).child("name")
                            .getValue(String::class.java)
                        val phone = p0.child("users").child(userId).child("phone")
                            .getValue(String::class.java)
                        val image = p0.child("users").child(userId).child("imageUrl")
                            .getValue(String::class.java)
                        Picasso.get().load(image).networkPolicy(NetworkPolicy.OFFLINE)
                            .into(imageView,
                                object : Callback {
                                    override fun onError(e: Exception?) {
                                        Picasso.get().load(image)
                                            .error(R.drawable.ic_account)
                                            .into(imageView, object : Callback {
                                                override fun onError(e: Exception?) {
                                                    Log.v("Picasso", "Could not fetch image")
                                                }

                                                override fun onSuccess() {

                                                }
                                            })
                                    }

                                    override fun onSuccess() {

                                    }
                                })

                        editTextName.setText(name)
                        editTextPhone.setText(phone)

                    }

                }
            }
        )
    }
}
