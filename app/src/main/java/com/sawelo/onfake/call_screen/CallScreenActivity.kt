package com.sawelo.onfake.call_screen

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sawelo.onfake.MainActivity
import com.sawelo.onfake.`object`.DeclineObject
import com.sawelo.onfake.call_screen.whatsapp_first.WhatsAppFirstIncomingCall
import com.sawelo.onfake.call_screen.whatsapp_first.WhatsAppFirstOngoingCall
import com.sawelo.onfake.call_screen.whatsapp_second.WhatsAppSecondIncomingCall
import com.sawelo.onfake.call_screen.whatsapp_second.WhatsAppSecondOngoingCall
import com.sawelo.onfake.data_class.CallProfileData
import com.sawelo.onfake.data_class.CallScreen
import com.sawelo.onfake.data_class.ContactData
import com.sawelo.onfake.data_class.DeclineData
import com.sawelo.onfake.receiver.DeclineReceiver
import com.sawelo.onfake.ui.theme.OnFakeTheme

class CallScreenActivity : ComponentActivity() {

    private lateinit var proximityWakeLock: PowerManager.WakeLock
    private val callScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, receiverIntent: Intent?) {
            if (receiverIntent?.action == DESTROY_ACTIVITY) {
                finish()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(callScreenReceiver, IntentFilter(DESTROY_ACTIVITY))
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        proximityWakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "infake::proximity_wake_lock")
        proximityWakeLock.setReferenceCounted(false)
        fun setWakeLock() = proximityWakeLock.acquire(10 * 60 * 1000L)
        showWhenLocked()

        val profileData =
            intent?.getParcelableExtra(MainActivity.PROFILE_EXTRA) ?: CallProfileData()
        val isStartFromIncoming =
            intent.getBooleanExtra(MainActivity.IS_START_FROM_INCOMING_EXTRA, true)

        val contactData = ContactData(
            profileData.name,
            profileData.photoUri
        )

        setContent {
            val navController = rememberNavController()
            OnFakeTheme {
                NavHost(
                    navController = navController,
                    startDestination = if (isStartFromIncoming) {
                        INCOMING_CALL_ROUTE
                    } else {
                        ONGOING_CALL_ROUTE
                    }
                ) {
                    fun sendDeclineIntent() {
                        Log.d(THIS_CLASS, "Send decline broadcast from NavHost")
                        val declineData = DeclineData(
                            originInformation = THIS_CLASS,
                            isDestroyAlarmService = true,
                            isDestroyCallNotification = true,
                            isDestroyCallScreenActivity = false,
                        )
                        DeclineObject.declineFunction(this@CallScreenActivity, declineData)
                    }

                    when (profileData.callScreen) {
                        CallScreen.WHATSAPP_FIRST -> {
                            composable(INCOMING_CALL_ROUTE) {
                                WhatsAppFirstIncomingCall(
                                    activity = this@CallScreenActivity,
                                    navController = navController,
                                    isStartAnimation = true,
                                    contactData = contactData
                                )
                            }
                            composable(ONGOING_CALL_ROUTE) {
                                WhatsAppFirstOngoingCall(
                                    this@CallScreenActivity,
                                    contactData = contactData
                                ) {
                                    sendDeclineIntent()
                                    setWakeLock()
                                }
                            }
                        }
                        CallScreen.WHATSAPP_SECOND -> {
                            composable(INCOMING_CALL_ROUTE) {
                                WhatsAppSecondIncomingCall(
                                    activity = this@CallScreenActivity,
                                    navController = navController,
                                    isStartAnimation = true,
                                    contactData = contactData
                                )
                            }
                            composable(ONGOING_CALL_ROUTE) {
                                WhatsAppSecondOngoingCall(
                                    this@CallScreenActivity,
                                    contactData = contactData
                                ) {
                                    sendDeclineIntent()
                                    setWakeLock()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showWhenLocked() {
        // Ensure CallActivity will run when the phone is locked or the screen is off
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setTranslucent(true)
            }
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
        }

        with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestDismissKeyguard(this@CallScreenActivity, null)
            }
        }
    }

    override fun onDestroy() {
        val declineIntent = Intent(this, DeclineReceiver::class.java)
        sendBroadcast(declineIntent)

        unregisterReceiver(callScreenReceiver)
        proximityWakeLock.release()
        super.onDestroy()

        Log.d(THIS_CLASS, "Wakelock is held: ${proximityWakeLock.isHeld}")
        Log.d(THIS_CLASS, "CallScreenActivity is destroyed")
    }

    companion object {
        const val THIS_CLASS = "CallScreenActivity"
        const val INCOMING_CALL_ROUTE = "incoming_call"
        const val ONGOING_CALL_ROUTE = "ongoing_call"
        const val DESTROY_ACTIVITY = "destroy_activity"
    }
}