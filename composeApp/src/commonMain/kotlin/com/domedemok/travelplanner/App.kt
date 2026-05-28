package com.domedemok.travelplanner

import androidx.compose.runtime.*
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.domedemok.travelplanner.i18n.LanguageManager
import com.domedemok.travelplanner.i18n.LocalStrings
import com.domedemok.travelplanner.i18n.stringsFor
import com.domedemok.travelplanner.ui.components.JoinTripSheet
import com.domedemok.travelplanner.ui.components.LocalIsOnline
import com.domedemok.travelplanner.ui.components.LocalLayoutMode
import com.domedemok.travelplanner.ui.components.layoutModeFor
import com.domedemok.travelplanner.ui.navigation.*
import com.domedemok.travelplanner.ui.screens.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.domedemok.travelplanner.ui.theme.TravelPlannerTheme
import com.domedemok.travelplanner.ui.theme.rememberCurrentDarkTheme
import com.domedemok.travelplanner.util.NetworkMonitor
import com.domedemok.travelplanner.viewmodel.AuthUiState
import com.domedemok.travelplanner.viewmodel.AuthViewModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSerializationApi::class)
private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(androidx.navigation3.runtime.NavKey::class) {
            subclassesOfSealed<Route>()
        }
    }
}

@Composable
fun App(initialJoinCode: String? = null) {
    val authViewModel: AuthViewModel = koinViewModel()
    val uiState by authViewModel.uiState.collectAsState()

    val currentLanguage by LanguageManager.language.collectAsState()

    val networkMonitor: NetworkMonitor = koinInject()
    val isOnline by networkMonitor.isOnline.collectAsState()

    DisposableEffect(Unit) {
        networkMonitor.start()
        onDispose { networkMonitor.stop() }
    }

    TravelPlannerTheme(darkTheme = rememberCurrentDarkTheme()) {
        // Surface fills the Skia canvas with the theme background color.
        // On Android, Scaffold does this automatically; on the CMP JS target the
        // canvas is transparent by default, so without this the browser's white
        // page background bleeds through in dark mode.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background,
        ) {
            // BoxWithConstraints measures the host window so the entire tree
            // can read a single, authoritative LocalLayoutMode value. Screens
            // therefore never compute the breakpoint themselves — they just
            // read the local and pick a layout. Per-pixel resize churn is
            // absorbed by compositionLocalOf's structural equality: only an
            // actual COMPACT↔EXPANDED crossing invalidates downstream readers.
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val layoutMode = layoutModeFor(maxWidth)

                CompositionLocalProvider(
                    LocalStrings     provides stringsFor(currentLanguage),
                    LocalIsOnline    provides isOnline,
                    LocalLayoutMode  provides layoutMode,
                ) {
                    // On JS/web, Firebase restores auth state asynchronously from
                    // IndexedDB. Until the first getAuthState() emission lands we
                    // don't know whether the user is logged in, so we show nothing
                    // (the Surface background colour is already painted). Once auth
                    // state is known, AppNavigation mounts with the correct
                    // startDestination — eliminating the Splash→Main ghost transition
                    // that used to appear for returning users on the web target.
                    if (uiState.isAuthStateKnown) {
                        AppNavigation(
                            uiState         = uiState,
                            authViewModel   = authViewModel,
                            initialJoinCode = initialJoinCode,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Navigation shell — only mounted once auth state is definitively known.
// Keeping the backstack inside this composable means rememberNavBackStack
// is always initialised with the correct startDestination.
// ---------------------------------------------------------------------------

@Composable
private fun AppNavigation(
    uiState:         AuthUiState,
    authViewModel:   AuthViewModel,
    initialJoinCode: String?,
) {
    val startDestination: Route = if (uiState.isLoggedIn) Main else Splash
    val backStack = rememberNavBackStack(navConfig, startDestination)

    // Sheet overlay state — shown instead of pushing JoinTrip to the backstack
    var showJoinSheet by remember { mutableStateOf(false) }
    var joinSheetCode by remember { mutableStateOf("") }

    // If the app was launched from a travelplanner://join/{code} deep link, show
    // the join sheet once — but only after auth state is known.
    LaunchedEffect(initialJoinCode, uiState.isLoggedIn) {
        if (!initialJoinCode.isNullOrBlank() && uiState.isLoggedIn) {
            joinSheetCode = initialJoinCode
            showJoinSheet = true
        }
    }

    // React to auth state changes — redirect to the correct root screen.
    LaunchedEffect(uiState.isLoggedIn) {
        val current = backStack.lastOrNull()
        if (!uiState.isLoggedIn) {
            // Signed-out: keep them on the auth-area screens (Splash / Login / SignUp).
            if (current !is Splash && current !is Login && current !is SignUp) {
                backStack.clear()
                backStack.add(Splash)
            }
        } else {
            // Signed-in: if we were on an auth screen, jump to the main shell.
            if (current is Splash || current is Login || current is SignUp) {
                backStack.clear()
                backStack.add(Main)
            }
        }
    }

    // Detect account switches: if isLoggedIn stays `true` but the user ID changes
    // (sign out + sign in as someone else happens faster than a recompose cycle),
    // the LaunchedEffect above never fires. We track the actual uid and force a
    // fresh Main entry — which destroys the old TripViewModel and its Firestore
    // listener — whenever the uid changes while staying logged in.
    val currentUserId = uiState.currentUser?.id
    var prevUserId by remember { mutableStateOf(currentUserId) }
    LaunchedEffect(currentUserId) {
        val old = prevUserId
        val new = currentUserId
        prevUserId = new
        // Both non-null and different → real account switch
        if (old != null && new != null && old != new) {
            backStack.clear()
            backStack.add(Main)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack    = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            entry<Splash> {
                SplashScreen(
                    onGetStarted = { backStack.add(SignUp) },
                    onLogIn      = { backStack.add(Login) },
                )
            }

            entry<Login> {
                LoginScreen(
                    onNavigateToSignUp = { backStack.add(SignUp) },
                    onLoginSuccess     = { /* handled by LaunchedEffect */ },
                )
            }

            entry<SignUp> {
                SignUpScreen(
                    onNavigateToLogin  = { backStack.removeLastOrNull() },
                    onSignUpSuccess    = { /* handled by LaunchedEffect */ },
                )
            }

            // Single entry for all 3 main tabs — MainShell manages the inner tab state.
            entry<Main> {
                MainShell(
                    onTripClick = { tripId -> backStack.add(TripDetail(tripId)) },
                    onJoinTrip  = { joinSheetCode = ""; showJoinSheet = true },
                    onLogout    = { authViewModel.signOut() },
                )
            }

            // Opening a trip pushes this on top of Main, swapping the bottom nav.
            entry<TripDetail> { key ->
                TripDetailScreen(
                    tripId         = key.tripId,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

        },
    )

    // ── Join Trip sheet overlay ───────────────────────────────────────────
    // Shown as a ModalBottomSheet on top of whatever screen is active,
    // instead of pushing a new nav entry (keeps the background visible).
    if (showJoinSheet) {
        JoinTripSheet(
            initialCode = joinSheetCode,
            onJoined    = { tripId ->
                showJoinSheet = false
                backStack.add(TripDetail(tripId))
            },
            onDismiss   = { showJoinSheet = false },
        )
    }
}
