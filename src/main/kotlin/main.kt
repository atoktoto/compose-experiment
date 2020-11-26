import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.Window
import androidx.compose.desktop.WindowEvents
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.Image
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer
import kotlin.time.*

private val now: Long
    get() = System.currentTimeMillis()

@ExperimentalTime
fun main() = runBlocking {
    val isActive = mutableStateOf(false)

    var timeWorked = 0.seconds

    var timeStart = System.currentTimeMillis()
    val noBreakTimeString = mutableStateOf("0s")
    val workTimeString = mutableStateOf("0s")

    val trayIcon = showTrayIcon()
//    trayIcon.displayMessage(
//        "Hello, World",
//        "notification demo",
//        TrayIcon.MessageType.NONE
//    )

    fixedRateTimer(period = 100L) {
        val noBrakeDuration = (now - timeStart).toDuration(DurationUnit.MILLISECONDS)
        val workDuration = if(isActive.value) timeWorked + noBrakeDuration else timeWorked
        workTimeString.value = workDuration.toString()
        noBreakTimeString.value = noBrakeDuration.toString()
    }

    launch {
        ActivityTracking
            .activityEventFlow()
            .collect {
                if(it.active.not()) {
                    timeWorked += (now - timeStart).toDuration(TimeUnit.MILLISECONDS)
                }

                timeStart = now
                isActive.value = it.active
            }
    }

    Window(
        title = "Home Office Assistant",
        size = IntSize(420, 768),
        events = WindowEvents()
    ) {
        MaterialTheme {
            DesktopTheme {
                layout(isActive, noBreakTimeString, workTimeString)
            }
        }
    }
}

@Composable fun layout(
    isActive: MutableState<Boolean>,
    noBreakTimeString: MutableState<String>,
    workTimeString: MutableState<String>,
) {
//    var text by remember { mutableStateOf("Hello, World!") }
    val isActive by remember { isActive }
    val noBreakTimeString by remember { noBreakTimeString }
    val workTimeString by remember { workTimeString }

    Column(modifier = Modifier.fillMaxWidth(1.0f).padding(16.dp)) {

        Row {
            if(isActive) {
                val builder = AnnotatedString.Builder()
                builder.append("🔨 You are working for ")
                builder.append(AnnotatedString(noBreakTimeString, SpanStyle(fontWeight = FontWeight.Bold)))
                builder.append(AnnotatedString(" without a break.", SpanStyle(fontWeight = FontWeight.Bold)))

                Text(builder.toAnnotatedString())
            } else {
                Text("💤 Break time")
            }
        }


        Row {
            val builder = AnnotatedString.Builder()
            builder.append("🛠 You clocked in ")
            builder.append(AnnotatedString(workTimeString, SpanStyle(fontWeight = FontWeight.Bold)))
            builder.append(AnnotatedString(" today.", SpanStyle(fontWeight = FontWeight.Bold)))
            Text(builder.toAnnotatedString())
        }

        Spacer(Modifier.preferredSize(16.dp))

        Row {
            Column {
                Text("🗓 ")
            }
            Column {
                Text("Next meeting in 0s.")
                Text(AnnotatedString("Boring meeting title", SpanStyle(fontStyle = FontStyle.Italic)))
            }

        }

        Spacer(Modifier.preferredSize(16.dp))

        Text("Actions", Modifier.align(BiasAlignment.Horizontal(0f)))

        Spacer(Modifier.preferredSize(8.dp))

        Button(onClick = { }, modifier = Modifier.align(BiasAlignment.Horizontal(0f))) {
            Text("🥛 Drank some water")
        }

        Spacer(Modifier.preferredSize(8.dp))

        Button(onClick = { }, modifier = Modifier.align(BiasAlignment.Horizontal(0f))) {
            Text("😴 Took a break")
        }
    }
}

fun showTrayIcon(): TrayIcon {
    val image: Image = Toolkit.getDefaultToolkit().createImage("icon.png")
    //Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("icon.png"));

    val trayIcon = TrayIcon(image, "Tray Demo")
    trayIcon.isImageAutoSize = true
    trayIcon.toolTip = "System tray icon demo"



    SystemTray.getSystemTray().add(trayIcon)

    return trayIcon
}
