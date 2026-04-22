package com.growwx.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.growwx.data.local.UserPreferences
import com.growwx.util.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── State ────────────────────────────────────────────────────────────────────

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val mode: AuthMode = AuthMode.LOGIN
)

enum class AuthMode { LOGIN, SIGNUP }

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val prefs: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setMode(mode: AuthMode) {
        _state.update { it.copy(mode = mode, error = null) }
    }

    fun login(email: String, password: String) {
        if (!validate(email, password)) return
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // Simulate Firebase Auth call
            delay(1200)
            val uid = "user_${email.hashCode()}"
            val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            prefs.saveUser(uid, name, email)
            Analytics.log("login_success")
            _state.update { it.copy(isLoading = false, isSuccess = true) }
        }
    }

    fun signup(name: String, email: String, password: String) {
        if (name.isBlank()) { _state.update { it.copy(error = "Name is required") }; return }
        if (!validate(email, password)) return
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            delay(1400)
            val uid = "user_${email.hashCode()}"
            prefs.saveUser(uid, name.trim(), email)
            prefs.setOnboarded()
            Analytics.log("signup_success")
            _state.update { it.copy(isLoading = false, isSuccess = true) }
        }
    }

    fun loginAsDemo() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            delay(800)
            prefs.saveUser("demo_user", "Demo User", "demo@growwx.in")
            Analytics.log("demo_login")
            _state.update { it.copy(isLoading = false, isSuccess = true) }
        }
    }

    private fun validate(email: String, password: String): Boolean {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(error = "Enter a valid email address") }
            return false
        }
        if (password.length < 6) {
            _state.update { it.copy(error = "Password must be at least 6 characters") }
            return false
        }
        return true
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.growwx.ui.components.*
import com.growwx.ui.theme.*

@Composable
fun AuthScreen(
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        // ── Logo ──
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            modifier = Modifier.size(72.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(GrowwXColor.Green, GrowwXColor.GreenDark)))
            ) {
                Text("📈", fontSize = 34.sp)
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("GrowwX", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
        Text("Invest Smart. Grow Faster.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.extendedColors.textMuted)

        Spacer(Modifier.height(40.dp))

        // ── Card ──
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Mode toggle
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.extendedColors.inputBg) {
                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        AuthMode.values().forEach { m ->
                            val isSelected = state.mode == m
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) GrowwXColor.Green else Color.Transparent,
                                modifier = Modifier.weight(1f).clickable { viewModel.setMode(m) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text(
                                        if (m == AuthMode.LOGIN) "Log In" else "Sign Up",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) Color.White else MaterialTheme.extendedColors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (state.mode == AuthMode.SIGNUP) {
                    GrowwXTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Full Name",
                        placeholder = "Rahul Sharma"
                    )
                    Spacer(Modifier.height(14.dp))
                }

                GrowwXTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "you@example.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(Modifier.height(14.dp))

                GrowwXTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "••••••••",
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.extendedColors.textMuted)
                        }
                    }
                )

                // Error message
                AnimatedVisibility(visible = state.error != null) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.extendedColors.redLight, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = GrowwXColor.Red, modifier = Modifier.padding(10.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))

                GrowwXButton(
                    text = if (state.mode == AuthMode.LOGIN) "Log In" else "Create Account",
                    isLoading = state.isLoading,
                    onClick = {
                        if (state.mode == AuthMode.LOGIN) viewModel.login(email, password)
                        else viewModel.signup(name, email, password)
                    }
                )

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Divider(modifier = Modifier.weight(1f), color = MaterialTheme.extendedColors.border)
                    Text("  or  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted)
                    Divider(modifier = Modifier.weight(1f), color = MaterialTheme.extendedColors.border)
                }

                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { viewModel.loginAsDemo() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.extendedColors.border)
                ) {
                    Text("🚀  Try Demo Account", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("By continuing, you agree to our Terms & Privacy Policy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
