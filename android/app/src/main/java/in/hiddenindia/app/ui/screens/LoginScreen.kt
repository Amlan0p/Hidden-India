package `in`.hiddenindia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.hiddenindia.app.ui.components.HiddenIndiaLogo
import `in`.hiddenindia.app.ui.components.LogoSize
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.theme.ThemeController
import `in`.hiddenindia.app.ui.viewmodel.AuthFormState
import `in`.hiddenindia.app.ui.viewmodel.AuthViewModel

private const val DEMO_EMAIL = "developer@hidden_india.in"
private const val DEMO_PASSWORD = "heritage123"

/**
 * Port of web src/pages/LoginPage.tsx. Adds what the native version was missing entirely:
 * the account-not-found -> "Create Account" flow (the backend already flagged this via
 * ApiError.Http.notFound, nothing read it), the forgot-password modal (the reset endpoint
 * already existed in AuthRepository, unused), remember-me, and the one-click demo login.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    themeController: ThemeController,
    onBackToWelcome: () -> Unit,
    onNavigateToSignup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by authViewModel.loginFormState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var showForgotModal by remember { mutableStateOf(false) }

    var localError by remember { mutableStateOf<String?>(null) }

    val isLoading = formState is AuthFormState.Loading
    val backendError = formState as? AuthFormState.Error

    val emailValid = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val canSubmit = email.isNotBlank() && password.isNotBlank() && emailValid && !isLoading

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Header: Back to Welcome + Theme toggle
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.clickable(onClick = onBackToWelcome),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text("  Back to Welcome", fontFamily = JetBrainsMono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable { themeController.toggle() }
                    .padding(8.dp)
            ) {
                Icon(
                    if (themeController.isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Brand & Heading
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HiddenIndiaLogo(size = LogoSize.Lg, showText = false)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Sign In to Hidden India", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "Access verified heritage destinations, your account AI chat history, maps, and offline vaults.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Login Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            val displayErrorMessage = localError ?: backendError?.message
            if (displayErrorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        Text("  $displayErrorMessage", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                    }
                    if (backendError?.notFound == true) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("No account exists for this email.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable(onClick = onNavigateToSignup)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Create Account", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary)
                                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp).padding(start = 2.dp))
                            }
                        }
                    }
                }
            }

            Text("EMAIL ADDRESS", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            HiddenIndiaTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                    if (formState is AuthFormState.Error) authViewModel.clearLoginError()
                },
                placeholder = "explorer@hiddenindia.com",
                trailingIcon = Icons.Filled.Mail,
                isError = !emailValid,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PASSWORD", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Forgot password?", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showForgotModal = true }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            HiddenIndiaTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                    if (formState is AuthFormState.Error) authViewModel.clearLoginError()
                },
                placeholder = "••••••••",
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIconToggle = Pair(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility) { passwordVisible = !passwordVisible },
                keyboardType = KeyboardType.Password
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberMe, onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text("Remember me on this device", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canSubmit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    .clickable(enabled = !isLoading) {
                        localError = null
                        if (email.isBlank()) {
                            localError = "Please enter your email address."
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            localError = "Please enter a valid email address."
                        } else if (password.isBlank()) {
                            localError = "Please enter your password."
                        } else {
                            authViewModel.login(email.trim(), password)
                        }
                    }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Text("  Authenticating...", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Log In", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimary)
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp).padding(start = 6.dp))
                }
            }

            Box(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLoading) {
                        email = DEMO_EMAIL
                        password = DEMO_PASSWORD
                        authViewModel.login(DEMO_EMAIL, DEMO_PASSWORD)
                    }
                    .padding(vertical = 12.dp)
                    .testTag("btn_demo_login"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("  ONE-CLICK EXPLORER DEMO LOGIN", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Don't have an account?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.clickable(onClick = onNavigateToSignup).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Create Account", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp).padding(start = 4.dp))
            }
        }
    }

    if (showForgotModal) {
        ForgotPasswordDialog(authViewModel = authViewModel, initialEmail = email, onDismiss = { showForgotModal = false; authViewModel.clearForgotPasswordStatus() })
    }
}

@Composable
private fun ForgotPasswordDialog(authViewModel: AuthViewModel, initialEmail: String, onDismiss: () -> Unit) {
    var forgotEmail by remember { mutableStateOf(initialEmail) }
    var newPassword by remember { mutableStateOf("") }
    val state by authViewModel.forgotPasswordState.collectAsState()
    val message by authViewModel.forgotPasswordMessage.collectAsState()
    val isLoading = state is AuthFormState.Loading
    val error = (state as? AuthFormState.Error)?.message

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Text("Reset Your Password", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 10.dp))
            Text(
                "Enter your registered email address to set a new password or receive reset instructions.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            if (message != null) {
                Text(
                    message ?: "", fontSize = 11.sp, color = Color(0xFF059669),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.1f))
                        .padding(10.dp)
                )
            }
            if (error != null) {
                Text(
                    error, fontSize = 11.sp, color = Color(0xFFDC2626),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                        .padding(10.dp)
                )
            }

            Text("REGISTERED EMAIL", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            HiddenIndiaTextField(value = forgotEmail, onValueChange = { forgotEmail = it }, placeholder = "explorer@hiddenindia.com", keyboardType = KeyboardType.Email)

            Spacer(modifier = Modifier.height(10.dp))
            Text("NEW PASSWORD (OPTIONAL — INSTANT RESET)", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            HiddenIndiaTextField(value = newPassword, onValueChange = { newPassword = it }, placeholder = "At least 6 characters", visualTransformation = PasswordVisualTransformation())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(enabled = !isLoading && forgotEmail.isNotBlank()) {
                        authViewModel.resetPassword(forgotEmail.trim(), newPassword.ifBlank { null })
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Reset Password", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
internal fun HiddenIndiaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingIconToggle: Pair<androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        placeholder = {
            Text(
                placeholder,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        trailingIcon = {
            if (trailingIconToggle != null) {
                val (icon, onClick) = trailingIconToggle
                IconButton(onClick = onClick) {
                    Icon(
                        icon,
                        contentDescription = "Toggle password visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (trailingIcon != null) {
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            errorContainerColor = MaterialTheme.colorScheme.background,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            errorBorderColor = Color(0xFFEF4444),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}
