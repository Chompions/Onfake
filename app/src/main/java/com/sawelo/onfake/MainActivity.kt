package com.sawelo.onfake

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.request.RequestOptions
import com.sawelo.onfake.`object`.UpdateTextObject
import com.sawelo.onfake.data_class.CallProfileData
import com.sawelo.onfake.data_class.ClockType
import com.sawelo.onfake.data_class.ScheduleData
import com.sawelo.onfake.service.AlarmService
import com.sawelo.onfake.ui.theme.OnFakeTheme
import com.skydoves.landscapist.CircularReveal
import com.skydoves.landscapist.glide.GlideImage

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnFakeTheme {
                CreateProfile()
            }
        }
    }

    companion object {
        const val PROFILE_EXTRA = "profile_extra"
        const val START_FROM_INCOMING_EXTRA = "start_from_incoming_extra"
    }
}

@Composable
fun CreateProfile() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val scale = .5F

    var nameText by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var mainScheduleText by rememberSaveable { mutableStateOf("Schedule Call") }

    var openScheduleListDialog by rememberSaveable { mutableStateOf(false) }
    var openTimerDialog by rememberSaveable { mutableStateOf(false) }
    var openAlarmDialog by rememberSaveable { mutableStateOf(false) }

    var tempSecond by rememberSaveable { mutableStateOf(0) }
    var tempMinute by rememberSaveable { mutableStateOf(0) }
    var tempHour by rememberSaveable { mutableStateOf(0) }

    fun cleanTempData() {
        tempHour = 0
        tempMinute = 0
        tempSecond = 0
    }

    var setScheduleData by rememberSaveable { mutableStateOf(ScheduleData(ClockType.TIMER)) }

    val getPhotoUri = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) {
        photoUri = it
    }

    val getContactUri = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()) {
        if (it != null) {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.moveToFirst()

            val contactNameIndex: Int = cursor?.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) ?: -1
            val contactPhotoIndex: Int = cursor?.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI) ?: -1

            val contactName = cursor?.getString(contactNameIndex)
            val contactPhoto = cursor?.getString(contactPhotoIndex)

            nameText = if (!contactName.isNullOrBlank()) cursor.getString(contactNameIndex) else ""
            photoUri = if (!contactPhoto.isNullOrBlank()) Uri.parse(cursor.getString(contactPhotoIndex)) else null

            cursor?.close()
        }
    }

    val getContactPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { isGranted ->
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
            minute = 0
        ),
        ScheduleData(
            ClockType.TIMER,
            minute = 2
        ),
        ScheduleData(
            ClockType.TIMER,
            minute = 5
        ),
        ScheduleData(
            ClockType.TIMER,
            minute = 10
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
                                setScheduleData = ScheduleData(
                                    it.clockType,
                                    hour = it.hour,
                                    minute = it.minute,
                                    second = it.second
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
                        cleanTempData()
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
                            maxValue = 24
                            value = setScheduleData.hour
                            setOnValueChangedListener { _, _, newVal ->
                                tempHour = newVal
                            }
                        }
                    })
                    AndroidView(factory = {
                        NumberPicker(it).apply {
                            minValue = 0
                            maxValue = 60
                            value = setScheduleData.minute
                            setOnValueChangedListener { _, _, newVal ->
                                tempMinute = newVal
                            }
                        }
                    })
                    AndroidView(factory = {
                        NumberPicker(it).apply {
                            minValue = 0
                            maxValue = 60
                            value = setScheduleData.second
                            setOnValueChangedListener { _, _, newVal ->
                                tempSecond = newVal
                            }
                        }
                    })
                }
            },
            buttons = {
                Row {
                    Button(
                        onClick = {
                            cleanTempData()
                            openTimerDialog = false
                        },
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            setScheduleData = ScheduleData(
                                ClockType.TIMER,
                                hour = tempHour,
                                minute = tempMinute,
                                second = tempSecond
                            )

                            val text = UpdateTextObject.updateMainText(setScheduleData)
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
                                this.hour = setScheduleData.hour
                                this.minute = setScheduleData.minute
                            }
                            setOnTimeChangedListener { _, hourOfDay, minute ->
                                tempHour = hourOfDay
                                tempMinute = minute
                            }
                        }
                    })
                }
            },
            buttons = {
                Row {
                    Button(
                        onClick = {
                            cleanTempData()
                            openAlarmDialog = false
                        },
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            setScheduleData = ScheduleData(
                                ClockType.ALARM,
                                hour = tempHour,
                                minute = tempMinute,
                                second = tempSecond
                            )

                            val text = UpdateTextObject.updateMainText(setScheduleData)
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

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = "Create") },
                icon = { Icon(Icons.Default.AddIcCall, "Create") },
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    val profileData = CallProfileData(scheduleData = setScheduleData)
                    if (nameText.isNotBlank()) profileData.name = nameText
                    if (photoUri != null) profileData.photoUri = photoUri as Uri

                    val intent = Intent(context, AlarmService::class.java)
                        .putExtra(MainActivity.PROFILE_EXTRA, profileData)

                    if (Build.VERSION.SDK_INT >= 26) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                })
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .background(MaterialTheme.colors.background)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())

        ) {
            Button(
                onClick = {getContactPermission.launch(android.Manifest.permission.READ_CONTACTS)},
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
                        .clickable {
                            getPhotoUri.launch("image/*")
                        }
                ) {
                    if (photoUri == null) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Photo",
                            modifier = Modifier.padding(20.dp),
                            tint = MaterialTheme.colors.primaryVariant.copy(alpha = .5f)
                        )
                    } else {
                        if (!LocalView.current.isInEditMode) {
                            GlideImage(
                                imageModel = photoUri,
                                contentScale = ContentScale.Crop,
                                circularReveal = CircularReveal(duration = 1000),
                                requestOptions = {
                                    RequestOptions().override(500 , 500)
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
                Spacer(modifier = Modifier.size(8.dp))
                Text(mainScheduleText)
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colors.primary.copy(alpha = .2f),
                    modifier = Modifier.padding(8.dp)
                ) {
                    FirstWhatsAppIncomingCall(
                        inCallScreen = false,
                        modifier = Modifier
                            .size(screenWidth * scale, screenHeight * scale)
                            .requiredSize(screenWidth, screenHeight)
                            .scale(scale)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colors.primary.copy(alpha = .2f),
                    modifier = Modifier.padding(8.dp)
                ) {
                    FirstWhatsAppIncomingCall(
                        inCallScreen = false,
                        modifier = Modifier
                            .size(screenWidth * scale, screenHeight * scale)
                            .requiredSize(screenWidth, screenHeight)
                            .scale(scale)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OnFakeTheme(darkTheme = false) {
        CreateProfile()
    }
}