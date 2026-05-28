package com.domedemok.travelplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.ui.components.LayoutMode
import com.domedemok.travelplanner.ui.components.LocalLayoutMode
import com.domedemok.travelplanner.viewmodel.resolve
import com.domedemok.travelplanner.ui.theme.*
import com.domedemok.travelplanner.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val s = LocalStrings.current
    val isExpandedLayout = LocalLayoutMode.current == LayoutMode.EXPANDED
    val uiState by viewModel.uiState.collectAsState()
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail      by remember { mutableStateOf("") }
    var resetSentNotice by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    // When the VM signals success, swap the form dialog for a confirmation notice
    // and clear the flag so a second attempt later in the session still works.
    LaunchedEffect(uiState.passwordResetEmailSent) {
        if (uiState.passwordResetEmailSent) {
            showResetDialog = false
            resetSentNotice = true
            viewModel.clearPasswordResetSent()
        }
    }

    if (showResetDialog) {
        ForgotPasswordDialog(
            email          = resetEmail,
            onEmailChange  = { resetEmail = it },
            error          = uiState.error?.resolve(s),
            isLoading      = uiState.isLoading,
            onSend         = { viewModel.sendPasswordResetEmail(resetEmail) },
            onDismiss      = {
                showResetDialog = false
                if (uiState.error != null) viewModel.clearError()
            },
        )
    }

    if (resetSentNotice) {
        AlertDialog(
            onDismissRequest = { resetSentNotice = false },
            shape            = AppShape.xl,
            title            = { Text(s.loginResetPasswordTitle, fontWeight = FontWeight.Bold) },
            text             = { Text(s.loginResetPasswordSent) },
            confirmButton    = {
                TextButton(onClick = { resetSentNotice = false }) { Text(s.generalOk) }
            },
        )
    }

    if (isExpandedLayout) {
        Box(
            modifier         = Modifier.fillMaxSize().background(AppGradients.profileHeader),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier  = Modifier.width(420.dp),
                shape     = AppShape.xl,
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.xxl).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    Text(s.loginTitle, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
                    Text(s.loginSubtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    LoginFormContent(
                        email              = email,
                        onEmailChange      = { email = it },
                        password           = password,
                        onPasswordChange   = { password = it },
                        showPassword       = showPassword,
                        onTogglePassword   = { showPassword = !showPassword },
                        error              = uiState.error?.resolve(s),
                        isLoading          = uiState.isLoading,
                        onLogin            = { viewModel.signIn(email, password) },
                        onNavigateToSignUp = onNavigateToSignUp,
                        onForgotPassword   = {
                            // Pre-fill the reset dialog with whatever the user already typed
                            // in the login email field; saves them re-typing in 90% of cases.
                            resetEmail      = email
                            showResetDialog = true
                        },
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
                    .background(AppGradients.profileHeader),
                contentAlignment = Alignment.BottomStart,
            ) {
                Column(modifier = Modifier.padding(AppSpacing.xl).padding(bottom = AppSpacing.xxl)) {
                    Text(s.loginSubtitle, fontSize = 14.sp, fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
                    Text(s.loginTitle, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary, letterSpacing = (-0.5).sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .align(Alignment.BottomCenter)
                    .clip(AppShape.bottomSheet)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = AppSpacing.xl)
                    .padding(top = AppSpacing.xxl, bottom = AppSpacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                LoginFormContent(
                    email              = email,
                    onEmailChange      = { email = it },
                    password           = password,
                    onPasswordChange   = { password = it },
                    showPassword       = showPassword,
                    onTogglePassword   = { showPassword = !showPassword },
                    error              = uiState.error?.resolve(s),
                    isLoading          = uiState.isLoading,
                    onLogin            = { viewModel.signIn(email, password) },
                    onNavigateToSignUp = onNavigateToSignUp,
                    onForgotPassword   = {
                        resetEmail      = email
                        showResetDialog = true
                    },
                )
            }
        }
    }
}

// ── Forgot password dialog ───────────────────────────────────────────────────

@Composable
private fun ForgotPasswordDialog(
    email:         String,
    onEmailChange: (String) -> Unit,
    error:         String?,
    isLoading:     Boolean,
    onSend:        () -> Unit,
    onDismiss:     () -> Unit,
) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = AppShape.xl,
        title            = { Text(s.loginResetPasswordTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Text(s.loginResetPasswordMessage, fontSize = 13.sp)
                AuthTextField(
                    value         = email,
                    onValueChange = onEmailChange,
                    label         = s.loginEmailLabel,
                    leadingIcon   = Icons.Default.Mail,
                    keyboardType  = KeyboardType.Email,
                    imeAction     = ImeAction.Done,
                    enabled       = !isLoading,
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSend, enabled = email.isNotBlank() && !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(s.loginResetPasswordSend, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.generalCancel) }
        },
    )
}

@Composable
private fun LoginFormContent(
    email:              String,
    onEmailChange:      (String) -> Unit,
    password:           String,
    onPasswordChange:   (String) -> Unit,
    showPassword:       Boolean,
    onTogglePassword:   () -> Unit,
    error:              String?,
    isLoading:          Boolean,
    onLogin:            () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onForgotPassword:   () -> Unit,
) {
    val s = LocalStrings.current

    AuthTextField(
        value         = email,
        onValueChange = onEmailChange,
        label         = s.loginEmailLabel,
        leadingIcon   = Icons.Default.Mail,
        keyboardType  = KeyboardType.Email,
        imeAction     = ImeAction.Next,
        enabled       = !isLoading,
    )

    AuthTextField(
        value            = password,
        onValueChange    = onPasswordChange,
        label            = s.loginPasswordLabel,
        leadingIcon      = Icons.Default.Lock,
        keyboardType     = KeyboardType.Password,
        imeAction        = ImeAction.Done,
        enabled          = !isLoading,
        isPassword       = true,
        showPassword     = showPassword,
        onTogglePassword = onTogglePassword,
        keyboardActions  = KeyboardActions(onDone = { onLogin() }),
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        TextButton(onClick = onForgotPassword) {
            Text(s.loginForgotPassword, color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (error != null) {
        Text(error, color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
    }

    GradientButton(
        text      = if (isLoading) s.loginLoadingButton else s.loginButton,
        onClick   = onLogin,
        enabled   = !isLoading,
        isLoading = isLoading,
    )

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(s.loginNoAccount, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        TextButton(onClick = onNavigateToSignUp) {
            Text(s.loginSignUpLink, color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Reusable auth text field ──────────────────────────────────────────────────

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {},
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val s = LocalStrings.current
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        singleLine    = true,
        enabled       = enabled,
        shape         = AppShape.lg,
        leadingIcon   = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon  = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector        = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) s.loginPasswordHide else s.loginPasswordShow,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(20.dp),
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation()
                               else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction    = imeAction,
        ),
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor    = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ── Reusable gradient primary button ─────────────────────────────────────────

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradient: Brush = AppGradients.primaryButton,
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        shape    = AppShape.lg,
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Color.Transparent,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) Modifier.background(gradient, AppShape.lg)
                    else Modifier.background(MaterialTheme.colorScheme.surfaceVariant, AppShape.lg)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color    = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text       = text,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (enabled) Color.White
                                 else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
