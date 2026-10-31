package `in`.hiddenindia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.hiddenindia.app.ui.components.HiddenIndiaLogo
import `in`.hiddenindia.app.ui.components.LogoSize
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.theme.ThemeController
import `in`.hiddenindia.app.ui.viewmodel.AuthFormState
import `in`.hiddenindia.app.ui.viewmodel.AuthViewModel

/**
 * Port of web src/pages/SignupPage.tsx. The main native gap was the Responsible Traveler
 * Pledge checkbox — web treats it as a required field (blocks submit until checked);
 * native previously didn't have it at all, so it wasn't just unstyled, it was missing a
 * real validation rule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    authViewModel: AuthViewModel,
    themeController: ThemeController,
    onBackToWelcome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by authViewModel.signupFormState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var agreedToPledge by remember { mutableStateOf(true) }
    var showPledgeError by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val isLoading = formState is AuthFormState.Loading
    val errorMessage = (formState as? AuthFormState.Error)?.message

    val emailValid = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordLongEnough = password.isBlank() || password.length >= 6
    val passwordsMatch = confirmPassword.isBlank() || confirmPassword == password

    val formValid = name.isNotBlank() && email.isNotBlank() && emailValid &&
        password.length >= 6 && confirmPassword.isNotBlank() && passwordsMatch
    val canSubmit = formValid && !isLoading

    fun clearErrorIfAny() {
        localValidationError = null
        if (errorMessage != null) authViewModel.clearSignupError()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.clickable(onClick = onBackToWelcome), verticalAlignment = Alignment.CenterVertically) {
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
                    contentDescription = "Toggle theme", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HiddenIndiaLogo(size = LogoSize.Lg, showText = false)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Create Explorer Account", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "Unlock your private account AI chat history, personalized heritage itineraries, and offline vaults.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            val displayError = localValidationError ?: errorMessage
            if (displayError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    Text("  $displayError", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                }
            }

            Text("FULL NAME", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            HiddenIndiaTextField(value = name, onValueChange = { name = it; clearErrorIfAny() }, placeholder = "Devika Sharma", trailingIcon = Icons.Filled.Person)

            Spacer(modifier = Modifier.height(14.dp))
            Text("EMAIL ADDRESS", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            HiddenIndiaTextField(value = email, onValueChange = { email = it; clearErrorIfAny() }, placeholder = "devika@heritage.in", trailingIcon = Icons.Filled.Mail, isError = !emailValid, keyboardType = KeyboardType.Email)

            Spacer(modifier = Modifier.height(14.dp))
            Text("PASSWORD (MIN 6 CHARACTERS)", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            HiddenIndiaTextField(
                value = password, onValueChange = { password = it; clearErrorIfAny() }, placeholder = "••••••••",
                isError = !passwordLongEnough, keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIconToggle = Pair(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility) { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("CONFIRM PASSWORD", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            HiddenIndiaTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it; clearErrorIfAny() }, placeholder = "••••••••",
                isError = !passwordsMatch, keyboardType = KeyboardType.Password,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIconToggle = Pair(if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility) { confirmPasswordVisible = !confirmPasswordVisible }
            )
            if (!passwordsMatch && confirmPassword.isNotBlank()) {
                Text("Passwords do not match.", fontSize = 10.sp, color = Color(0xFFEF4444), modifier = Modifier.padding(top = 2.dp, start = 4.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = agreedToPledge,
                    onCheckedChange = { agreedToPledge = it; showPledgeError = false },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        buildAnnotatedString2("I agree to the ", "Responsible Traveler Pledge", " to respect local heritage & communities."),
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp
                    )
                    if (showPledgeError) {
                        Text("You must agree to the Responsible Traveler Pledge to proceed.", fontSize = 10.sp, color = Color(0xFFEF4444), modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canSubmit && agreedToPledge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    .clickable(enabled = !isLoading) {
                        clearErrorIfAny()
                        if (!agreedToPledge) {
                            showPledgeError = true
                        } else if (name.isBlank()) {
                            localValidationError = "Please enter your full name."
                        } else if (email.isBlank()) {
                            localValidationError = "Please enter your email address."
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            localValidationError = "Please enter a valid email address."
                        } else if (password.isBlank()) {
                            localValidationError = "Please enter a password."
                        } else if (password.length < 6) {
                            localValidationError = "Password must be at least 6 characters."
                        } else if (confirmPassword.isBlank()) {
                            localValidationError = "Please confirm your password."
                        } else if (confirmPassword != password) {
                            localValidationError = "Passwords do not match."
                        } else {
                            authViewModel.signup(name.trim(), email.trim(), password, confirmPassword)
                        }
                    }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Text("  Creating Account...", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create Account", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimary)
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp).padding(start = 6.dp))
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Already have an account?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.clickable(onClick = onNavigateToLogin).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sign In", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp).padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun buildAnnotatedString2(pre: String, bold: String, post: String): androidx.compose.ui.text.AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    return androidx.compose.ui.text.buildAnnotatedString {
        append(pre)
        withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor)) { append(bold) }
        append(post)
    }
}
