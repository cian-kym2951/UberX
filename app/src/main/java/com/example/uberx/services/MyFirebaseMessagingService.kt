package com.example.uberx.services

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.uberx.Common
import com.example.uberx.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FirebaseAuth.getInstance().currentUser!!.uid != null)
            UserUtils.updateToken(this, token)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        if (data != null){
            Common.showNotification(this, Random.nextInt(),
            data[Common.NOTI_TILE],
            data[Common.NOTI_BODY],
            null)
        }
    }

}