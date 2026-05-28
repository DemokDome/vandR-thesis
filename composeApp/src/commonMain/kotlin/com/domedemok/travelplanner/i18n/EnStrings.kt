@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.domedemok.travelplanner.i18n

import com.domedemok.travelplanner.data.model.ExpenseCategory
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val EN_MONTH_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** "Apr 27, 2026" */
private fun fmtChatDateEn(timestamp: Long): String {
    val dt = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${EN_MONTH_SHORT[dt.month.number - 1]} ${dt.day}, ${dt.year}"
}

object EnStrings : Strings {

    // ── Bottom Navigation ───────────────────────────────────────────────────
    override val navDiscover   = "Discover"
    override val navMyTrips    = "My Trips"
    override val navInbox      = "Inbox"
    override val navProfile    = "Profile"
    override val navPlaces     = "Places"
    override val navMap        = "Map"
    override val navChat       = "Chat"
    override val navBudget     = "Budget"
    override val navPlan       = "Plan"
    override val navGallery    = "Gallery"

    // ── General ─────────────────────────────────────────────────────────────
    override val generalOk      = "OK"
    override val generalCancel  = "Cancel"
    override val generalSave    = "Save"
    override val generalDelete  = "Delete"
    override val generalClose   = "Close"
    override val generalDismiss = "Dismiss"
    override val generalUnknown = "Unknown"
    override val generalBack    = "Back"
    override val generalNext    = "Next"
    override val generalSkip    = "Skip"
    override val generalCreate  = "Create"
    override val generalEdit    = "Edit"
    override val generalRename  = "Rename"
    override val generalRemove  = "Remove"
    override val generalSend    = "Send"

    // ── Splash ──────────────────────────────────────────────────────────────
    override val splashAppName    = "vandR"
    override val splashTagline    = "Your world, planned."
    override val splashGetStarted = "Get Started"
    override val splashLogIn      = "Log In"
    override val splashTerms      = "By continuing, you agree to our Terms & Privacy Policy"
    override val splashParis      = "🗼 Paris"
    override val splashTokyo      = "⛩️ Tokyo"
    override val splashNyc        = "🗽 NYC"
    override val splashRome       = "🏛️ Rome"
    override val splashSantorini  = "🌅 Santorini"

    // ── Login ────────────────────────────────────────────────────────────────
    override val loginSubtitle       = "Welcome back"
    override val loginTitle          = "Log In"
    override val loginEmailLabel     = "Email address"
    override val loginPasswordLabel  = "Password"
    override val loginForgotPassword         = "Forgot Password?"
    override val loginResetPasswordTitle     = "Reset password"
    override val loginResetPasswordMessage   = "Enter your email and we'll send you a link to reset your password."
    override val loginResetPasswordSend      = "Send reset link"
    override val loginResetPasswordSent      = "If an account exists for that email, a reset link has been sent."
    override val loginLoadingButton          = "Logging in…"
    override val loginButton         = "Log In"
    override val loginNoAccount      = "Don't have an account?"
    override val loginSignUpLink     = "Sign Up"
    override val loginPasswordHide   = "Hide password"
    override val loginPasswordShow   = "Show password"

    // ── Sign Up ──────────────────────────────────────────────────────────────
    override val signUpSubtitle        = "Start your journey"
    override val signUpTitle           = "Create Account"
    override val signUpDisplayName     = "Display name"
    override val signUpEmailLabel      = "Email address"
    override val signUpPasswordLabel   = "Password (min 8 characters)"
    override val signUpConfirmPassword = "Confirm password"
    override val signUpLoadingButton   = "Creating account…"
    override val signUpButton          = "Create Account"
    override val signUpHasAccount      = "Already have an account?"
    override val signUpLoginLink       = "Log In"

    // ── Auth errors ──────────────────────────────────────────────────────────
    override val authErrorInvalidCredentials = "Incorrect email or password. Please try again."
    override val authErrorEmailAlreadyInUse  = "An account with this email address already exists. Try signing in instead."
    override val authErrorWeakPassword       = "This password is too weak. Use at least 8 characters with a mix of letters and numbers."
    override val authErrorInvalidEmail       = "That doesn't look like a valid email address."
    override val authErrorUserNotFound       = "No account found with this email address."
    override val authErrorUserDisabled       = "This account has been disabled. Please contact support."
    override val authErrorNetwork            = "Couldn't reach the server. Check your internet connection and try again."
    override val authErrorTooManyRequests    = "Too many attempts. Please wait a few minutes before trying again."
    override val authErrorUnknown            = "Something went wrong. Please try again."

    // ── Explore ──────────────────────────────────────────────────────────────
    override val greetingMorning   = "Good morning"
    override val greetingAfternoon = "Good afternoon"
    override val greetingEvening   = "Good evening"
    override val exploreSubtitle   = "Discover your next\nadventure"
    override val exploreContinue   = "Continue Planning"
    override val exploreLastOpened = "Last opened"
    override val exploreUpcoming   = "Upcoming Trips"
    override val exploreFindVibe   = "Find Your Vibe"
    override val vibeBeach         = "Beach"
    override val vibeMountain      = "Mountain"
    override val vibeCulture       = "Culture"
    override val vibeFoodie        = "Foodie"
    override val travelTipsTitle   = "Travel Tips"
    override val tipPlanAheadTitle = "Plan ahead"
    override val tipPlanAheadBody  = "Trips with detailed itineraries are 3× more likely to stay on budget."
    override val tipInviteTitle    = "Invite friends"
    override val tipInviteBody     = "Collaborative trips are more fun — share via the trip menu."
    override val tipTrackTitle     = "Track spending"
    override val tipTrackBody      = "Use the Budget tab inside each trip to split costs fairly."

    // ── Profile ──────────────────────────────────────────────────────────────
    override val profileDefaultName     = "Traveler"
    override val profileTripsLabel      = "Trips"
    override val profileFavoritesLabel  = "Favorites"
    override val profileMemberSince     = "Member since"
    override val profileAccountSection  = "Account"
    override val profileNameLabel       = "Name"
    override val profileEmailLabel      = "Email"
    override val profileDangerZone      = "Danger Zone"
    override val profileSignOut         = "Sign Out"
    override val profileDeleteAccount   = "Delete Account"
    override val profileSignOutTitle    = "Sign Out?"
    override val profileSignOutMessage  = "You will be returned to the login screen."
    override val profileDeleteTitle     = "Delete Account?"
    override val profileDeleteMessage   = "This will permanently delete your account and all associated data. This cannot be undone."
    override val profileDeleteConfirm   = "Yes, Delete"
    override val profileReAuthTitle     = "Confirm your password"
    override val profileReAuthMessage   = "For security, please enter your password to delete your account."
    override val profilePasswordLabel   = "Password"
    override val profileDeleteButton    = "Delete"
    override val profileLanguageSection = "Language"
    override val profileAppearanceSection = "Appearance"
    override val themeSystem            = "System"
    override val themeLight             = "Light"
    override val themeDark              = "Dark"

    // ── Trip List / Side Navigation (accessibility) ──────────────────────────
    override val sideNavFavorite = "Favorite"

    // ── Trip List ────────────────────────────────────────────────────────────
    override val tripListTitle        = "My Trips"
    override val tripStatusActive     = "Active"
    override val tripStatusUpcoming   = "Upcoming"
    override val tripStatusPast       = "Past"
    override val tripCardMembers: (Int) -> String = { count -> "$count ${if (count == 1) "member" else "members"}" }
    override val tripListEmptyTitle   = "Where to next?"
    override val tripListEmptyBody    = "Create your first trip and start\nplanning your adventure."
    override val tripListCreateButton = "Create a Trip"

    // ── Trip Detail ──────────────────────────────────────────────────────────
    override val tripDetailFallback      = "Trip"
    override val tripDetailNoDateRange   = "No dates set"
    override val tripDetailEditLabel     = "Edit trip"
    override val tripDetailInviteLabel   = "Invite member"
    override val tripDetailDeleteLabel   = "Delete trip"
    override val tripDetailLeaveLabel    = "Leave trip"
    override val tripDetailLeaveTitle    = "Leave Trip?"
    override val tripDetailLeaveMessage  = "Are you sure you want to leave this trip?"
    override val tripDetailLeaveButton   = "Leave"
    override val tripDetailDeleteTitle: (String) -> String = { name -> "Delete \"$name\"?" }
    override val tripDetailDeleteMessage = "This action cannot be undone."
    override val tripDetailNotFound      = "Trip not found"

    // ── Budget ───────────────────────────────────────────────────────────────
    override val budgetTotalSpent   = "Total spent"
    override val budgetNoExpenses   = "No expenses yet"
    override val budgetNoExpensesBody = "Tap \"Add Expense\" to start tracking\nwhat everyone spent on this trip."
    override val budgetExpenseCount: (Int) -> String = { n -> if (n == 1) "1 expense" else "$n expenses" }
    override val budgetAddExpense   = "Add Expense"
    override val budgetSettlement   = "Settlement"
    override val budgetPerPerson    = "Per-person"
    override val budgetExpenses     = "Expenses"
    override val budgetPaid         = "Paid"
    override val budgetShare        = "Share"
    override val budgetBalance      = "Balance"
    override val budgetOwedBack: (String, String) -> String = { name, amount -> "$name is owed $amount back" }
    override val budgetOwes: (String, String) -> String = { name, amount -> "$name owes $amount" }
    override val budgetSettledUp: (String) -> String = { name -> "$name is all settled up ✓" }
    override val budgetPaidSuffix         = "paid"
    override val budgetDeleteExpenseTitle = "Delete expense?"
    override val budgetDeleteExpenseBody: (String) -> String = { name -> "\"$name\" will be removed and all balances recalculated." }
    override val budgetExpenseTotal       = "Total"
    override val budgetExpenseDate        = "Date"

    // ── Add Expense ──────────────────────────────────────────────────────────
    override val expenseNewTitle         = "New Expense"
    override val expenseEditTitle        = "Edit Expense"
    override val expenseDescLabel        = "Description"
    override val expenseDescPlaceholder  = "e.g. Dinner, taxi, museum…"
    override val expenseAmountLabel      = "Total amount"
    override val expenseCategoryLabel    = "Category"
    override val expenseCategoryLabels   = mapOf(
        ExpenseCategory.FOOD_AND_DRINK         to "Food & Drink",
        ExpenseCategory.TRANSPORTATION         to "Transportation",
        ExpenseCategory.ACCOMMODATION          to "Accommodation",
        ExpenseCategory.ACTIVITIES_AND_TICKETS to "Activities & Tickets",
        ExpenseCategory.SHOPPING               to "Shopping",
        ExpenseCategory.OTHER                  to "Other"
    )
    override val expensePaidByLabel      = "Paid by"
    override val expenseSplitLabel       = "Split"
    override val expenseSplitEnterAmount = "Enter amount first"
    override val expenseSplitBalanced    = "Balanced ✓"
    override val expenseSplitLeft: (String) -> String = { amount -> "$amount left to assign" }
    override val expenseSplitOver: (String) -> String = { amount -> "$amount over total" }
    override val expenseSplitEqually     = "Split Equally"
    override val expenseSaveChanges      = "Save Changes"
    override val expenseAddButton        = "Add Expense"

    // ── Chat ─────────────────────────────────────────────────────────────────
    override val chatNoMessages        = "No messages yet"
    override val chatStartConversation = "Start the conversation!"
    override val chatInputPlaceholder  = "Type a message…"
    override val chatSendButton        = "Send"
    override val chatDateFormat: (Long) -> String = ::fmtChatDateEn

    // ── AI Chat ──────────────────────────────────────────────────────────────
    override val aiChatTitle    = "Ask Gemini AI"
    override val aiChatSubtitle = "Get smart travel suggestions\nand personalised tips!"
    override val chatTabGroup   = "Group Chat"
    override val chatTabAi      = "Gemini AI"

    // ── Favorites ────────────────────────────────────────────────────────────

    // ── Manage Members ───────────────────────────────────────────────────────
    override val membersTitle         = "Members"
    override val membersPeopleCount: (Int) -> String = { n -> if (n == 1) "1 person" else "$n people" }
    override val membersOwner         = "Owner"
    override val membersRenameTitle   = "Rename member"
    override val membersRenameLabel   = "New name"
    override val membersRenameButton  = "Rename"
    override val membersRemoveTitle   = "Remove member?"
    override val membersRemoveMessage: (String) -> String = { name -> "Remove $name from this trip?" }
    override val membersRemoveConfirm = "Remove"

    // ── Edit Trip ────────────────────────────────────────────────────────────
    override val editTripTitle      = "Edit Trip"
    override val editTripNameLabel  = "Trip Name"
    override val editTripDescLabel  = "Description"
    override val editTripSelectDates = "Select Trip Dates"
    override val editTripDatesLabel  = "Trip Dates"
    override val editTripTapSelect   = "Tap to select dates"
    override val editTripSave        = "Save"

    // ── Share Trip ───────────────────────────────────────────────────────────
    override val shareTitle: (String) -> String = { name -> "Share \"$name\"" }
    override val shareFallback   = "Share Trip"
    override val shareCodeLabel  = "JOIN CODE"
    override val shareCopyCode   = "Copy Code"
    override val shareCopied     = "Copied!"
    override val shareOrEmail    = "Or invite by email"
    override val shareEmailLabel = "Email address"
    override val shareSendInvite = "Send Invite"
    override val shareQrScan     = "Scan to join"
    override val shareCopyLink   = "Copy link"

    // ── Join Trip ────────────────────────────────────────────────────────────
    override val joinTripTitle         = "Join a Trip"
    override val joinTripSubtitle      = "Enter the 6-character code you received, or scan the QR code with your camera"
    override val joinTripCodeLabel     = "Join Code"
    override val joinTripCodePlaceholder = "e.g. AB3K7X"
    override val joinTripJoin          = "Join Trip"
    override val joinTripJoining       = "Joining…"
    override val joinTripInvalidCode   = "Invalid code. Please check and try again."
    override val joinTripAlreadyMember = "You're already a member of this trip."
    override val joinTripSuccess       = "You've joined the trip!"
    override val joinTripButton        = "Join a Trip"
    override val joinTripScanQr        = "Scan QR Code"
    override val joinTripScanHint      = "Point the camera at the QR code"
    override val joinTripCameraPermission = "Camera permission is required to scan QR codes."
    override val joinTripScannerNotSupported = "QR scanning requires Chrome or Edge."
    override val joinTripCameraUnavailable   = "Camera access is unavailable in this browser."

    // ── New Trip Flow ────────────────────────────────────────────────────────
    override val newTripEyebrow         = "DISCOVER"
    override val newTripFlowTitle       = "Plan a new trip"
    override val newTripWhereGoing      = "Where are you going?"
    override val newTripDestSubtitle    = "Pick a destination to kick things off"
    override val newTripDestSearch      = "Search city or type your own…"
    override val newTripSetDestination  = "Set Destination"
    override val newTripPickDates       = "Pick Dates"
    override val newTripCreate          = "Create Trip"
    override val newTripNameLabel       = "Trip name"
    override val newTripDescLabel       = "Description (optional)"
    override val newTripNamePlaceholder = "e.g. Summer in Italy"
    override val newTripDescPlaceholder = "What's this trip about?"
    override val newTripSkipDates       = "Skip"
    override val newTripWhenGoing       = "When are you going?"
    override val newTripTapSetDates     = "Tap to set your travel dates"
    override val newTripClearDates      = "Clear"
    override val newTripPrevMonth       = "Previous month"
    override val newTripNextMonth       = "Next month"
    override val newTripWeekDayLabels   = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    override val newTripNights: (Int) -> String = { n -> if (n == 1) "1 night" else "$n nights" }
    override val newTripNameTitle       = "Name your trip"
    override val newTripNameSubtitle    = "Give your adventure a memorable name"
    override val newTripSelectStart     = "Select your start date"
    // U+2192 (→) is missing from the bundled Noto Sans Skia font on web; render
    // it as a middle dot instead so it doesn't show up as a tofu box.
    override val newTripPickEnd: (String, Int) -> String = { month, day -> "$month $day · pick end date" }

    // ── Places Screen ────────────────────────────────────────────────────────
    override val placesWhereTo           = "Where to next?"
    override val placesPopularIn: (String) -> String = { location -> "Popular spots in $location" }
    override val placesNoMatches         = "No matches yet"
    override val placesNoMatchesHint     = "Try a keyword, category, or different city."
    override val placesSetDestHint       = "Set a destination to start adding places."
    override val placesChooseDestination = "Choose Destination"
    override val placesExploring         = "Exploring"
    override val placesChangeDestination = "Change destination"
    override val placesNoPlaces          = "No places saved yet"
    override val placesNoPlacesBody      = "Tap \"Discover\" to find popular spots nearby\nand save them to your itinerary."
    override val placesDiscoverEyebrow   = "DISCOVER"
    override val placesCloseSearch       = "Close search"
    override val placesSearchPlaceholder = "e.g. Eiffel Tower, coffee, sushi…"
    override val placesSearch            = "Search"
    override val placesSearching         = "Searching…"
    override val placesSavePlace         = "Save place"
    override val placesLoadMore          = "Load more"

    // ── Map Screen ───────────────────────────────────────────────────────────
    override val mapDailySummary     = "Daily summary"
    override val mapNoDays           = "No itinerary days yet."
    override val mapNoPlacesAssigned = "No places assigned"
    override val mapUnassigned       = "Unassigned"
    override val mapSummaryButton    = "Summary"
    override val mapAllDays          = "All"
    override val mapDayLabel: (Int) -> String = { n -> "Day $n" }
    override val mapClose            = "Close"
    override val mapMyLocation       = "My location"
    override val mapNoPlacesTitle    = "No places to show"
    override val mapNoPlacesBody     = "Add places in the Places tab\nto see them on the map."

    // ── Gallery ──────────────────────────────────────────────────────────────
    override val galleryEmpty       = "No photos yet"
    override val galleryEmptyBody   = "Add photos to remember this trip.\nTap the + button to get started."
    override val galleryAddPhoto    = "Add Photo"
    override val galleryDeleteTitle = "Delete photo?"
    override val galleryDeleteBody  = "This photo will be permanently deleted."
    override val galleryBy          = "by"
    override val galleryPhotoCount: (Int) -> String = { n -> if (n == 1) "1 photo" else "$n photos" }

    // ── Date formatting ──────────────────────────────────────────────────────
    override val monthShortNames = EN_MONTH_SHORT
    override val monthFullNames  = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // ── Offline Banner ───────────────────────────────────────────────────────
    override val offlineBannerText   = "No internet connection – offline mode"

    // ── Inbox Screen ─────────────────────────────────────────────────────────
    override val inboxTitle        = "Inbox"
    override val inboxEmpty        = "All caught up!"
    override val inboxEmptyBody    = "Activity from your trips will appear here."
    override val inboxMarkAllRead  = "Mark all read"
    override val inboxNewMessage: (String) -> String = { name -> "New message from $name" }
    override val inboxNewPlace: (String) -> String = { name -> "New place saved: $name" }
    override val inboxNewExpense: (String) -> String = { title -> "New expense: $title" }
    override val inboxInTrip: (String) -> String = { trip -> "in $trip" }
    override val inboxPlaceRemoved: (String) -> String = { name  -> "Place removed: $name" }
    override val inboxExpenseRemoved: (String) -> String = { title -> "Expense removed: $title" }
    override val inboxAddedToTrip: (String) -> String = { trip  -> "You were added to \"$trip\"" }
    override val inboxMemberJoined: (String) -> String = { name  -> "$name joined the trip" }
    override val inboxMemberLeft: (String) -> String = { name  -> "$name left the trip" }
    override val inboxClearAllTitle   = "Clear all notifications?"
    override val inboxClearAllBody    = "All notifications will be removed. This cannot be undone."
    override val inboxClearAllConfirm = "Clear all"
    override val inboxJustNow      = "Just now"
    override val inboxMinutesAgo: (Int) -> String = { n -> "${n}m ago" }
    override val inboxHoursAgo: (Int) -> String = { n -> "${n}h ago" }
    override val inboxYesterday    = "Yesterday"
    override val inboxDaysAgo: (Int) -> String = { n -> "${n}d ago" }
}