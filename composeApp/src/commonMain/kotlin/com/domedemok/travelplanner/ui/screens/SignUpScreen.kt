package com.domedemok.travelplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val s = LocalStrings.current
    val isExpandedLayout = LocalLayoutMode.current == LayoutMode.EXPANDED
    val uiState by viewModel.uiState.collectAsState()

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName     by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onSignUpSuccess()
    }

    if (isExpandedLayout) {
        Box(
            modifier         = Modifier.fillMaxSize().background(AppGradients.profileHeader),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier  = Modifier.width(440.dp),
                shape     = AppShape.xl,
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.xxl).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    Text(s.signUpTitle, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
                    Text(s.signUpSubtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    SignUpFormContent(
                        displayName            = displayName,
                        onDisplayNameChange    = { displayName = it },
                        email                  = email,
                        onEmailChange          = { email = it },
                        password               = password,
                        onPasswordChange       = { password = it },
                        confirmPassword        = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        showPassword           = showPassword,
                        onTogglePassword       = { showPassword = !showPassword },
                        showConfirmPass        = showConfirmPass,
                        onToggleConfirmPass    = { showConfirmPass = !showConfirmPass },
                        error                  = uiState.error?.resolve(s),
                        isLoading              = uiState.isLoading,
                        onSignUp               = { viewModel.signUp(email, password, confirmPassword, displayName) },
                        onNavigateToLogin      = onNavigateToLogin,
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
                    Text(s.signUpSubtitle, fontSize = 14.sp, fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
                    Text(s.signUpTitle, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
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
                SignUpFormContent(
                    displayName            = displayName,
                    onDisplayNameChange    = { displayName = it },
                    email                  = email,
                    onEmailChange          = { email = it },
                    password               = password,
                    onPasswordChange       = { password = it },
                    confirmPassword        = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it },
                    showPassword           = showPassword,
                    onTogglePassword       = { showPassword = !showPassword },
                    showConfirmPass        = showConfirmPass,
                    onToggleConfirmPass    = { showConfirmPass = !showConfirmPass },
                    error                  = uiState.error?.resolve(s),
                    isLoading              = uiState.isLoading,
                    onSignUp               = { viewModel.signUp(email, password, confirmPassword, displayName) },
                    onNavigateToLogin      = onNavigateToLogin,
                )
            }
        }
    }
}

@Composable
private fun SignUpFormContent(
    displayName:             String,
    onDisplayNameChange:     (String) -> Unit,
    email:                   String,
    onEmailChange:           (String) -> Unit,
    password:                String,
    onPasswordChange:        (String) -> Unit,
    confirmPassword:         String,
    onConfirmPasswordChange: (String) -> Unit,
    showPassword:            Boolean,
    onTogglePassword:        () -> Unit,
    showConfirmPass:         Boolean,
    onToggleConfirmPass:     () -> Unit,
    error:                   String?,
    isLoading:               Boolean,
    onSignUp:                () -> Unit,
    onNavigateToLogin:       () -> Unit,
) {
    val s = LocalStrings.current

    AuthTextField(
        value         = displayName,
        onValueChange = onDisplayNameChange,
        label         = s.signUpDisplayName,
        leadingIcon   = Icons.Default.Person,
        keyboardType  = KeyboardType.Text,
        imeAction     = ImeAction.Next,
        enabled       = !isLoading,
    )

    AuthTextField(
        value         = email,
        onValueChange = onEmailChange,
        label         = s.signUpEmailLabel,
        leadingIcon   = Icons.Default.Mail,
        keyboardType  = KeyboardType.Email,
        imeAction     = ImeAction.Next,
        enabled       = !isLoading,
    )

    AuthTextField(
        value            = password,
        onValueChange    = onPasswordChange,
        label            = s.signUpPasswordLabel,
        leadingIcon      = Icons.Default.Lock,
        keyboardType     = KeyboardType.Password,
        imeAction        = ImeAction.Next,
        enabled          = !isLoading,
        isPassword       = true,
        showPassword     = showPassword,
        onTogglePassword = onTogglePassword,
    )

    AuthTextField(
        value            = confirmPassword,
        onValueChange    = onConfirmPasswordChange,
        label            = s.signUpConfirmPassword,
        leadingIcon      = Icons.Default.Lock,
        keyboardType     = KeyboardType.Password,
        imeAction        = ImeAction.Done,
        enabled          = !isLoading,
        isPassword       = true,
        showPassword     = showConfirmPass,
        onTogglePassword = onToggleConfirmPass,
        keyboardActions  = KeyboardActions(onDone = { onSignUp() }),
    )

    if (error != null) {
        Text(error, color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
    }

    GradientButton(
        text      = if (isLoading) s.signUpLoadingButton else s.signUpButton,
        onClick   = onSignUp,
        enabled   = !isLoading,
        isLoading = isLoading,
    )

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(s.signUpHasAccount, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        TextButton(onClick = onNavigateToLogin) {
            Text(s.signUpLoginLink, color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
