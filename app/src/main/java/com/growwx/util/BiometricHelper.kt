package com.growwx.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * BiometricHelper — wraps AndroidX Biometric library.
 *
 * Usage in a Composable:
 *   val activity = LocalContext.current as FragmentActivity
 *   BiometricHelper.authenticate(activity,
 *       onSuccess = { /* navigate */ },
 *       onError   = { msg -> showToast(msg) }
 *   )
 *
 * Add to app/build.gradle.kts:
 *   implementation("androidx.biometric:biometric:1.2.0-alpha05")
 */
object BiometricHelper {

    fun isAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Verify Identity",
        subtitle: String = "Use biometrics to access GrowwX",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }
            override fun onAuthenticationFailed() {
                onError("Authentication failed. Try again.")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(info)
    }
}
