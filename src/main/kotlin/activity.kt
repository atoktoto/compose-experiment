
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
import org.jnativehook.GlobalScreen
import org.jnativehook.NativeHookException
import org.jnativehook.NativeInputEvent
import org.jnativehook.keyboard.NativeKeyEvent
import org.jnativehook.keyboard.NativeKeyListener
import org.jnativehook.mouse.NativeMouseEvent
import org.jnativehook.mouse.NativeMouseInputListener
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

object ActivityTracking {

    fun activityEventFlow(): Flow<ActivityEvent> {
        disableLogging()

        try {
            GlobalScreen.registerNativeHook()
        } catch (ex: NativeHookException) {
            System.err.println("There was a problem registering the native hook.")
            System.err.println(ex.message)
        }

        val flow = callbackFlow<NativeInputEvent> {
            val mouseListener = GlobalMouseListener { event ->
                sendBlocking(event)
            }

            val keyListener = KeyListener { event ->
                sendBlocking(event)
            }

            GlobalScreen.addNativeMouseListener(mouseListener)
            GlobalScreen.addNativeMouseMotionListener(mouseListener)
            GlobalScreen.addNativeKeyListener(keyListener)

            awaitClose {
                GlobalScreen.removeNativeMouseListener(mouseListener)
                GlobalScreen.removeNativeMouseMotionListener(mouseListener)
                GlobalScreen.removeNativeKeyListener(keyListener)
            }
        }

        return channelFlow<ActivityEvent> {
            flow
                .onEach { channel.send(ActivityEvent(active = true)) }
                .debounce(1000 * 10)
                .onEach { channel.send(ActivityEvent(active = false)) }
                .collect()
        }.distinctUntilChanged()
    }

    data class ActivityEvent(
        val active: Boolean
    )

    private fun disableLogging() {
        val logger: Logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
        LogManager.getLogManager().reset()
        logger.level = Level.OFF
        logger.useParentHandlers = false
    }

    private class KeyListener(private val onNativeKeyEvent: (NativeKeyEvent) -> Unit) : NativeKeyListener {
        override fun nativeKeyTyped(e: NativeKeyEvent) = onNativeKeyEvent(e)
        override fun nativeKeyPressed(e: NativeKeyEvent) = onNativeKeyEvent(e)
        override fun nativeKeyReleased(e: NativeKeyEvent) = onNativeKeyEvent(e)
    }

    private class GlobalMouseListener(private val onMouseActivity: (NativeMouseEvent) -> Unit) : NativeMouseInputListener {
        override fun nativeMouseClicked(e: NativeMouseEvent) = onMouseActivity(e)
        override fun nativeMousePressed(e: NativeMouseEvent) = onMouseActivity(e)
        override fun nativeMouseReleased(e: NativeMouseEvent) = onMouseActivity(e)
        override fun nativeMouseMoved(e: NativeMouseEvent) = onMouseActivity(e)
        override fun nativeMouseDragged(e: NativeMouseEvent) = onMouseActivity(e)
    }
}

