package com.example.uberx

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.uberx.models.DriverInfoModel
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.FirebaseDatabase.getInstance
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal
import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object{
        val LOGIN_REQUEST_CODE = 7171
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if (firebaseAuth != null && listener != null)
            firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    private fun delaySplashScreen() {
        Completable.timer(5, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe({
                firebaseAuth.addAuthStateListener(listener)
            })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        init()

    }

    private fun init(){
        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE)

        providers = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener {
            val user = firebaseAuth.currentUser
            if (user!= null){
                FirebaseInstanceIdInternal.NewTokenListener{
                    Log.e("Token", it)
                    UserUtils.updateToken(this@SplashScreenActivity, it)
                }
                checkUserFromFirebase()
            }else{
                showLoginLayout()
            }
        }

    }

    private fun checkUserFromFirebase() {
        driverInfoRef
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val model = snapshot.getValue(DriverInfoModel::class.java)
                        goToHomeActivityModel(model)
                    }else{
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, error.message, Toast.LENGTH_SHORT).show()
                }

            })
    }

    private fun goToHomeActivityModel(model: DriverInfoModel?) {
        Common.currentUser = model
        startActivity(Intent(this, DriverHomeActivity::class.java))
    }

    private fun showRegisterLayout() {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val first_name = itemView.findViewById<View>(R.id.edt_first_name) as TextView
        val last_name = itemView.findViewById<View>(R.id.edt_last_name) as TextView
        val phone_number = itemView.findViewById<View>(R.id.edt_phone_number) as TextView
        val btn_continue = itemView.findViewById<View>(R.id.btn_register) as Button

        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
            !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
            phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        btn_continue.setOnClickListener {
            if (TextUtils.isDigitsOnly(first_name.text.toString())){
                Toast.makeText(this@SplashScreenActivity, "Please enter first name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if (TextUtils.isDigitsOnly(last_name.text.toString())) {
                Toast.makeText(this@SplashScreenActivity, "Please enter first name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if (TextUtils.isDigitsOnly(phone_number.text.toString())) {
                Toast.makeText(this@SplashScreenActivity, "Please enter phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                val model = DriverInfoModel()
                model.firstName = first_name.text.toString()
                model.lastName = last_name.text.toString()
                model.phoneNo = phone_number.text.toString()
                model.rating = 0.0
                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(model)
                    .addOnFailureListener {
                        Toast.makeText(this@SplashScreenActivity, ""+it.message, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        progressBar.visibility = View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(this@SplashScreenActivity, "Registered successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        goToHomeActivityModel(model)
                        progressBar.visibility = View.GONE
                    }

            }
        }

    }

    private fun showLoginLayout() {
        val authMethodPicker = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.get_started_btn)
            .build()
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPicker)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()

        , LOGIN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE){
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            }else{
                Toast.makeText(this, ""+response!!.error!!.message, Toast.LENGTH_SHORT).show()
            }
        }


    }
}