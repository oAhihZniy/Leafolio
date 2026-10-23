package app.leafolio

import android.app.KeyguardManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.lifecycleScope
import app.leafolio.data.UserPreferences
import app.leafolio.data.UserPreferencesStore
import kotlinx.coroutines.launch

open class PrivateActivity : ComponentActivity() {
    var privacyReady by mutableStateOf(false); private set
    var locked by mutableStateOf(true); private set
    var privatePreferences by mutableStateOf(UserPreferences()); private set
    private var leftAt: Long? = null
    private var authenticating = false
    private var lastAuthentication = 0L
    private var callback: ((Boolean) -> Unit)? = null
    private var signal: CancellationSignal? = null
    private val credential = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        finishAuthentication(it.resultCode == RESULT_OK)
    }
    fun preparePrivacy() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        lifecycleScope.launch {
            UserPreferencesStore(this@PrivateActivity).preferences.collect { prefs ->
                val first = !privacyReady
                privatePreferences = prefs
                if (first) locked = prefs.lockDelaySeconds >= 0
                if (prefs.lockDelaySeconds < 0) locked = false
                privacyReady = true
                secureWindow()
                if (first && locked) unlock()
            }
        }
    }
    private fun secureWindow() {
        if (!privacyReady || locked || privatePreferences.hidePreview) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(privacyReady && !locked && !privatePreferences.hidePreview)
    }
    override fun onPause() {
        if (privatePreferences.lockDelaySeconds >= 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        super.onPause()
    }
    override fun onStop() {
        if (!authenticating) {
            leftAt = android.os.SystemClock.elapsedRealtime()
            if (privatePreferences.lockDelaySeconds == 0) locked = true
        }
        super.onStop()
    }
    override fun onStart() {
        super.onStart()
        val away = leftAt
        if (privacyReady && !authenticating && app.leafolio.data.LockPolicy.needsLock(privatePreferences.lockDelaySeconds, away, android.os.SystemClock.elapsedRealtime())) locked = true
        leftAt = null
        secureWindow()
    }
    fun unlock() { authenticate(force = true) { if (it) { locked = false; secureWindow() } } }
    fun authenticate(force: Boolean = false, done: (Boolean) -> Unit) {
        if (!force && !locked && lastAuthentication > 0 && android.os.SystemClock.elapsedRealtime() - lastAuthentication < 30_000) { done(true); return }
        if (authenticating) return
        if (!getSystemService(KeyguardManager::class.java).isDeviceSecure) {
            android.widget.Toast.makeText(this, "请先在系统设置中设置锁屏密码或生物识别。", android.widget.Toast.LENGTH_LONG).show()
            done(false); return
        }
        authenticating = true; callback = done
        if (Build.VERSION.SDK_INT >= 30) {
            signal = CancellationSignal()
            BiometricPrompt.Builder(this).setTitle("解锁一页").setSubtitle("使用生物识别或设备锁屏凭证")
                .setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build().authenticate(signal!!, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { finishAuthentication(true) }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { finishAuthentication(false) }
                })
        } else {
            @Suppress("DEPRECATION")
            val intent = getSystemService(KeyguardManager::class.java).createConfirmDeviceCredentialIntent("解锁一页", "验证设备锁屏凭证")
            if (intent != null) credential.launch(intent) else finishAuthentication(false)
        }
    }
    private fun finishAuthentication(success: Boolean) {
        authenticating = false
        if (success) lastAuthentication = android.os.SystemClock.elapsedRealtime()
        val done = callback; callback = null
        done?.invoke(success)
    }
    override fun onDestroy() { signal?.cancel(); super.onDestroy() }
}

@Composable
fun PrivacyCurtain(activity: PrivateActivity) {
    val dark = when (activity.privatePreferences.theme) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false,
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        val bg = if (dark) Color(0xFF282C34) else Color(0xFFF8F8F4)
        val ink = if (dark) Color(0xFFD7DAE0) else Color(0xFF303630)
        val accent = if (dark) Color(0xFF61AFEF) else Color(0xFF527359)
        Column(Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.safeDrawing).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("一页", color = ink, style = MaterialTheme.typography.headlineLarge)
            Text("你的记录，只为你展开", color = ink, modifier = Modifier.padding(top = 16.dp, bottom = 32.dp))
            if (activity.privacyReady) Button(onClick = { activity.unlock() }, colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text("解锁") }
        }
    }
}
