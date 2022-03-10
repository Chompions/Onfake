package com.sawelo.onfake

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.NumberPicker
import android.widget.TimePicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.request.RequestOptions
import com.sawelo.onfake.`object`.DeclineObject
import com.sawelo.onfake.`object`.UpdateTextObject
import com.sawelo.onfake.call_screen.whatsapp_first.WhatsAppFirstIncomingCall
import com.sawelo.onfake.call_screen.whatsapp_second.WhatsAppSecondIncomingCall
import com.sawelo.onfake.data_class.*
import com.sawelo.onfake.service.AlarmService
import com.sawelo.onfake.ui.theme.OnFakeTheme
import com.skydoves.landscapist.CircularReveal
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MainActivity : ComponentActivity() {

    private val isCallActiveLiveData = MutableLiveData(AlarmService.IS_RUNNING)
    private val mainActivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, receiverIntent: Intent?) {
            if (receiverIntent?.action == DEACTIVATE_CALL) {
                Log.d("MainActivity", "Deactivating call from receiver")
                isCallActiveLiveData.value = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppDatabase.getInstance(this)

        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Create your custom animation.
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_Y,
                0f,
                -splashScreenView.view.height.toFloat()
            )
            slideUp.interpolator = AccelerateInterpolator()
            slideUp.duration = 500L

            // Call SplashScreenView.remove at the end of your custom animation.
            slideUp.doOnEnd { splashScreenView.remove() }

            // Run your animation.
            slideUp.start()
        }

        super.onCreate(savedInstanceState)
        registerReceiver(mainActivityReceiver, IntentFilter(DEACTIVATE_CALL))

        setContent {
            OnFakeTheme {
                CreateProfile(isCallActiveLiveData)
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(mainActivityReceiver)
        super.onDestroy()
    }

    companion object {
        const val PROFILE_EXTRA = "profile_extra"
        const val IS_START_FROM_INCOMING_EXTRA = "is_start_from_incoming_extra"
        const val DEACTIVATE_CALL = "deactivate_call"
    }
}

@Preview(showBackground = true)
@Composable
fun CreateProfile(isCallActiveLiveData: LiveData<Boolean>? = null) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val timeZone = TimeZone.currentSystemDefault()

    var nameText by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var mainScheduleText by rememberSaveable { mutableStateOf("Schedule Call") }
    var callScreen by rememberSaveable { mutableStateOf(CallScreen.WHATSAPP_FIRST) }
    var checkShowNotificationText by rememberSaveable { mutableStateOf(false) }

    var openScheduleListDialog by rememberSaveable { mutableStateOf(false) }
    var openTimerDialog by rememberSaveable { mutableStateOf(false) }
    var openAlarmDialog by rememberSaveable { mutableStateOf(false) }

    var isCallActive by rememberSaveable { mutableStateOf(false) }
    isCallActiveLiveData?.observe(lifecycle) {
        isCallActive = it
    }

    var tempTimeData by rememberSaveable { mutableStateOf(TimeData())}
    var fixedScheduleData by rememberSaveable { mutableStateOf(
        ScheduleData(ClockType.TIMER, TimeData())) }

    fun getNowTimeData(now: Instant): TimeData {
        val nowDateTime = now.toLocalDateTime(timeZone)
        return TimeData(
            hour = nowDateTime.hour,
            minute = nowDateTime.minute,
            second = nowDateTime.second,
            year = nowDateTime.year,
            month = nowDateTime.monthNumber,
            dayOfMonth = nowDateTime.dayOfMonth
        )
    }

    val getPhotoUri = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        photoUri = it
    }

    val getContactUri = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) {
        if (it != null) {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.moveToFirst()

            val contactNameIndex: Int =
                cursor?.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) ?: -1
            val contactPhotoIndex: Int =
                cursor?.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI) ?: -1

            val contactName = cursor?.getString(contactNameIndex)
            val contactPhoto = cursor?.getString(contactPhotoIndex)

            nameText = if (!contactName.isNullOrBlank()) cursor.getString(contactNameIndex) else ""
            photoUri =
                if (!contactPhoto.isNullOrBlank()) Uri.parse(cursor.getString(contactPhotoIndex)) else null

            cursor?.close()
        }
    }

    val getContactPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getContactUri.launch(null)
        }
    }

    /**
     * This timeList will be editable or customizable to user preferences
     */
    val timeList = mutableListOf(
        ScheduleData(
            ClockType.TIMER,
            TimeData(second = 0)
        ),
        ScheduleData(
            ClockType.TIMER,
            TimeData(second = 10)
        ),
        ScheduleData(
            ClockType.TIMER,
            TimeData(minute = 2)
        ),
        ScheduleData(
            ClockType.TIMER,
            TimeData(minute = 5)
        ),
        ScheduleData(
            ClockType.TIMER,
            TimeData(minute = 10)
        )
    )

    if (openScheduleListDialog) {
        AlertDialog(
            onDismissRequest = { openScheduleListDialog = false },
            title = { Text("Set Time") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    timeList.forEach {
                        val text = UpdateTextObject.updateMainText(it)

                        Button(
                            onClick = {
                                fixedScheduleData = ScheduleData(
                                    it.clockType,
                                    it.targetTime,
                                )

                                mainScheduleText = text.first
                                openScheduleListDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(text.first)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openTimerDialog = true
                                openScheduleListDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                "Timer",
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Timer")
                        }
                        Spacer(modifier = Modifier.size(10.dp))
                        OutlinedButton(
                            onClick = {
                                openAlarmDialog = true
                                openScheduleListDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Alarm,
                                "Alarm",
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Alarm")
                        }
                    }
                }
            },
            buttons = {
                Button(
                    onClick = {
                        tempTimeData = TimeData()
                        openScheduleListDialog = false
                    },
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (openTimerDialog) {
        AlertDialog(
            onDismissRequest = { openTimerDialog = false },
            title = { Text("Timer") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AndroidView(factory = {
                        NumberPicker(it).apply {
                            minValue = 0
                            maxValue = 23
                            value = fixedScheduleData.targetTime.hour
                            setOnValueChangedListener { _, _, newVal ->
                                tempTimeData.hour = newVal
                            }
                        }
                    })
                    AndroidView(factory = {
                        NumberPicker(it).apply {
                            minValue = 0
                            maxValue = 59
                            value = fixedScheduleData.targetTime.minute
                            setOnValueChangedListener { _, _, newVal ->
                                tempTimeData.minute = newVal
                            }
                        }
                    })
                    AndroidView(factory = {
                        NumberPicker(it).apply {
                            minValue = 0
                            maxValue = 59
                            value = fixedScheduleData.targetTime.second
                            setOnValueChangedListener { _, _, newVal ->
                                tempTimeData.second = newVal
                            }
                        }
                    })
                }
            },
            buttons = {
                Row {
                    Button(
                        onClick = {
                            tempTimeData = TimeData()
                            openTimerDialog = false
                        },
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            fixedScheduleData = ScheduleData(
                                ClockType.TIMER,
                                TimeData(
                                    hour = tempTimeData.hour,
                                    minute = tempTimeData.minute,
                                    second = tempTimeData.second
                                ),
                            )

                            val text = UpdateTextObject.updateMainText(fixedScheduleData)
                            mainScheduleText = text.first

                            openTimerDialog = false
                        },
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Ok")
                    }
                }
            },
        )
    }

    if (openAlarmDialog) {
        AlertDialog(
            onDismissRequest = { openAlarmDialog = false },
            title = { Text("Timer") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AndroidView(factory = {
                        TimePicker(it).apply {
                            if (Build.VERSION.SDK_INT >= 23) {
                                val now = Clock.System.now().toLocalDateTime(timeZone)
                                this.hour = now.hour
                                this.minute = now.minute
                            }
                            setOnTimeChangedListener { _, hourOfDay, minute ->
                                tempTimeData.hour = hourOfDay
                                tempTimeData.minute = minute
                            }
                        }
                    })
                }
            },
            buttons = {
                Row {
                    Button(
                        onClick = {
                            tempTimeData = TimeData()
                            openAlarmDialog = false
                        },
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            fixedScheduleData = ScheduleData(
                                ClockType.ALARM,
                                TimeData(
                                    hour = tempTimeData.hour,
                                    minute = tempTimeData.minute,
                                    second = tempTimeData.second
                                ),
                            )

                            val text = UpdateTextObject.updateMainText(fixedScheduleData)
                            mainScheduleText = text.first

                            openAlarmDialog = false
                        },
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Ok")
                    }
                }
            },
        )
    }

    Scaffold {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .background(MaterialTheme.colors.background)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(9F)
                    .verticalScroll(rememberScrollState())
            ) {
                Button(
                    onClick = { getContactPermission.launch(android.Manifest.permission.READ_CONTACTS) },
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text("Use Existing Contact")
                }

                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colors.primary.copy(alpha = .2f),
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable {
                                getPhotoUri.launch("image/*")
                            }
                    ) {
                        if (photoUri == null) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Photo",
                                modifier = Modifier.padding(20.dp),
                                tint = MaterialTheme.colors.primary.copy(alpha = .5f)
                            )
                        } else {
                            if (!LocalView.current.isInEditMode) {
                                GlideImage(
                                    imageModel = photoUri,
                                    contentScale = ContentScale.Crop,
                                    circularReveal = CircularReveal(duration = 1000),
                                    requestOptions = {
                                        RequestOptions().override(500, 500)
                                    },
                                    loading = {
                                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                                    },
                                    failure = {
                                        Icon(
                                            Icons.Default.BrokenImage,
                                            contentDescription = "Photo",
                                            modifier = Modifier
                                                .padding(20.dp)
                                                .align(Alignment.Center),
                                            tint = MaterialTheme.colors.primaryVariant.copy(alpha = .5f)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    if (photoUri != null) {
                        IconButton(
                            onClick = { photoUri = null },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .absoluteOffset(10.dp, 10.dp)
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Cancel photo",
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("Name") },
                    leadingIcon = { Icon(Icons.Default.Person, "Name") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.onPrimary,
                        unfocusedBorderColor = MaterialTheme.colors.primary
                    ),
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { openScheduleListDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(top = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        "Schedule",
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(mainScheduleText)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Switch(
                        checked = checkShowNotificationText,
                        onCheckedChange = {checkShowNotificationText = it},
                    )
                    Text(
                        "Show timer in notification",
                        fontSize = 14.sp

                    )
                }

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                ) {
                    val contactData = ContactData()
                    if (nameText.isNotBlank()) contactData.name =
                        nameText else CallProfileDefaultValue.nameValue
                    if (photoUri != null) contactData.photoBitmap = photoUri as Uri else Uri.parse(
                        CallProfileDefaultValue.photoUriValue
                    )

                    val configuration = LocalConfiguration.current

                    val screenHeight = configuration.screenHeightDp.dp
                    val screenWidth = configuration.screenWidthDp.dp
                    val scale = .5F

                    val callScreenModifier = Modifier
                        .size(screenWidth * scale, screenHeight * scale)
                        .requiredSize(screenWidth, screenHeight)
                        .scale(scale)
                        .clip(RoundedCornerShape(8.dp))

                    val callScreenMap: Map<CallScreen, @Composable () -> Unit> = mapOf(
                        CallScreen.WHATSAPP_FIRST to {
                            WhatsAppFirstIncomingCall(
                                isStartAnimation = false,
                                modifier = callScreenModifier,
                                contactData = contactData
                            )
                        },
                        CallScreen.WHATSAPP_SECOND to {
                            WhatsAppSecondIncomingCall(
                                isStartAnimation = false,
                                modifier = callScreenModifier,
                                contactData = contactData
                            )
                        },
                    )

                    if (!LocalView.current.isInEditMode) {
                        callScreenMap.forEach {
                            CallScreenRow(
                                thisCallScreen = it.key,
                                currentCallScreen = callScreen,
                                composableCallScreen = it.value,
                                onClick = { thisCallScreen ->
                                    callScreen = thisCallScreen
                                }
                            )
                        }
                    }
                }
            }
            Button(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1F)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                onClick = {
                    fixedScheduleData.startTime = getNowTimeData(Clock.System.now())

                    val profileData = CallProfileData(
                        scheduleData = fixedScheduleData,
                        showNotificationText = checkShowNotificationText,
                    )

                    if (nameText.isNotBlank()) profileData.name = nameText
                    if (photoUri != null) profileData.photoUri = photoUri.toString()
                    profileData.callScreen = callScreen

                    val declineData = DeclineData(
                        originInformation = "MainActivity",
                        isDestroyAlarmService = true,
                        isDestroyCallNotification = true,
                        isDestroyCallScreenActivity = true,
                        isDeactivateCallMainActivity = false
                    )
                    DeclineObject.declineFunction(context, declineData)

                    if(!isCallActive) {
                        isCallActive = true
                        val alarmIntent = Intent(context, AlarmService::class.java)
                            .putExtra(MainActivity.PROFILE_EXTRA, profileData)
                        if (Build.VERSION.SDK_INT >= 26) {
                            context.startForegroundService(alarmIntent)
                        } else {
                            context.startService(alarmIntent)
                        }

                    } else {
                        isCallActive = false
                    }
                }
            ) {
                if (!isCallActive) {
                    Icon(Icons.Default.AddIcCall, "Start")
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Start")
                } else {
                    Icon(Icons.Default.Close, "Stop")
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Stop")
                }
            }
        }

    }
}

@Composable
fun CallScreenRow(
    thisCallScreen: CallScreen,
    currentCallScreen: CallScreen,
    composableCallScreen: @Composable () -> Unit,
    onClick: (CallScreen) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(
                vertical = 10.dp,
                horizontal = 8.dp
            )
            .clickable { onClick(thisCallScreen) }
    ) {
        composableCallScreen()

        if (currentCallScreen == thisCallScreen) {
            Icon(
                imageVector = Icons.Default.RadioButtonChecked,
                contentDescription = "Check",
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.RadioButtonUnchecked,
                contentDescription = "Uncheck",
                tint = MaterialTheme.colors.primaryVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }

    }
}


