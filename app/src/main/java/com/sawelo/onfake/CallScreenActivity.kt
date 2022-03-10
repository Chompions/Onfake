package com.sawelo.onfake

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sawelo.onfake.`object`.DeclineObject
import com.sawelo.onfake.call_screen.whatsapp_first.WhatsAppFirstIncomingCall
import com.sawelo.onfake.call_screen.whatsapp_first.WhatsAppFirstOngoingCall
import com.sawelo.onfake.call_screen.whatsapp_second.WhatsAppSecondIncomingCall
import com.sawelo.onfake.call_screen.whatsapp_second.WhatsAppSecondOngoingCall
import com.sawelo.onfake.data_class.*
import com.sawelo.onfake.ui.theme.OnFakeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CallScreenActivity : ComponentActivity() {

    private lateinit var proximityWakeLock: PowerManager.WakeLock
    private val callScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, receiverIntent: Intent?) {
            if (receiverIntent?.action == DESTROY_CALL_SCREEN_ACTIVITY) {
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

        registerReceiver(callScreenReceiver, IntentFilter(DESTROY_CALL_SCREEN_ACTIVITY))
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        proximityWakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "infake::proximity_wake_lock")
        proximityWakeLock.setReferenceCounted(false)
        fun setWakeLock() = proximityWakeLock.acquire(10 * 60 * 1000L)
        showWhenLocked()

        val isStartFromIncoming =
            intent.getBooleanExtra(MainActivity.IS_START_FROM_INCOMING_EXTRA, true)

        setContent {
            val context = LocalContext.current
            val navController = rememberNavController()
            var callProfile by rememberSaveable { mutableStateOf<CallProfileData?>(null)}

            LaunchedEffect(true) {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getInstance(context)
                    while (callProfile == null) {
                        delay(500)
                        callProfile = db.callProfileDao().getCallProfile().firstOrNull()
                    }
                }
            }

            val contactData = ContactData(
                callProfile?.name ?: CallProfileDefaultValue.nameValue,
                Uri.parse(callProfile?.photoUri ?: CallProfileDefaultValue.photoUriValue)
            )

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
                        val declineData = DeclineData(
                            originInformation = THIS_CLASS,
                            isDestroyAlarmService = true,
                            isDestroyCallNotification = true,
                            isDestroyCallScreenActivity = false,
                        )
                        DeclineObject.declineFunction(this@CallScreenActivity, declineData)
                    }

                    when (callProfile?.callScreen) {
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
                        null -> {
                            composable(INCOMING_CALL_ROUTE) {
                                Box(Modifier.background(Color.DarkGray))
                            }
                            composable(ONGOING_CALL_ROUTE) {
                                Box(Modifier.background(Color.DarkGray))
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun showWhenLocked() {
        // Ensure CallActivity will run when the phone is locked or the screen is off
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
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestDismissKeyguard(this@CallScreenActivity, null)
            }
            val keyLock = this.newKeyguardLock("IN")
            keyLock.disableKeyguard()
        }
    }

    override fun onDestroy() {
        val declineData = DeclineData(
            originInformation = THIS_CLASS,
            isDestroyAlarmService = true,
            isDestroyCallNotification = true,
            isDestroyCallScreenActivity = false,
        )
        DeclineObject.declineFunction(this@CallScreenActivity, declineData)

        unregisterReceiver(callScreenReceiver)
        proximityWakeLock.release()
        super.onDestroy()
    }

    companion object {
        const val THIS_CLASS = "CallScreenActivity"
        const val INCOMING_CALL_ROUTE = "incoming_call"
        const val ONGOING_CALL_ROUTE = "ongoing_call"
        const val DESTROY_CALL_SCREEN_ACTIVITY = "destroy_call_screen_activity"
    }
}