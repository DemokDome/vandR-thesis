package com.domedemok.travelplanner.i18n

import com.domedemok.travelplanner.data.model.ExpenseCategory
import com.domedemok.travelplanner.data.repository.rules.AuthErrorCode

/**
 * All UI-visible strings for the TravelPlanner app.
 *
 * Parameterised strings are expressed as lambdas so the call-site stays
 * idiomatic Kotlin while keeping the translation unit self-contained:
 * s.budgetOwedBack("Alice", "$12.50")  →  "Alice is owed $12.50 back"
 */
interface Strings {

    // ── Bottom Navigation ───────────────────────────────────────────────────
    val navDiscover:   String
    val navMyTrips:    String
    val navInbox:      String
    val navProfile:    String
    val navPlaces:     String
    val navMap:        String
    val navChat:       String
    val navBudget:     String
    val navPlan:       String
    val navGallery:    String

    // ── Inbox Screen ────────────────────────────────────────────────────────
    val inboxTitle:           String
    val inboxEmpty:           String
    val inboxEmptyBody:       String
    val inboxMarkAllRead:     String
    val inboxNewMessage:      (String) -> String   // "New message from {name}"
    val inboxNewPlace:        (String) -> String   // "New place added: {name}"
    val inboxNewExpense:      (String) -> String   // "New expense: {title}"
    val inboxInTrip:          (String) -> String   // "in {tripName}"
    val inboxPlaceRemoved:    (String) -> String   // "Place removed: {name}"
    val inboxExpenseRemoved:  (String) -> String   // "Expense removed: {title}"
    val inboxAddedToTrip:     (String) -> String   // "You were added to {tripName}"
    val inboxMemberJoined:    (String) -> String   // "{name} joined the trip"
    val inboxMemberLeft:      (String) -> String   // "{name} left the trip"
    val inboxClearAllTitle:   String
    val inboxClearAllBody:    String
    val inboxClearAllConfirm: String
    val inboxJustNow:         String
    val inboxMinutesAgo:      (Int) -> String
    val inboxHoursAgo:        (Int) -> String
    val inboxYesterday:       String
    val inboxDaysAgo:         (Int) -> String

    // ── General / shared ────────────────────────────────────────────────────
    val generalOk:      String
    val generalCancel:  String
    val generalSave:    String
    val generalDelete:  String
    val generalClose:   String
    val generalDismiss: String
    val generalUnknown: String
    val generalBack:    String
    val generalNext:    String
    val generalSkip:    String
    val generalCreate:  String
    val generalEdit:    String
    val generalRename:  String
    val generalRemove:  String
    val generalSend:    String

    // ── Splash Screen ───────────────────────────────────────────────────────
    val splashAppName:    String
    val splashTagline:    String
    val splashGetStarted: String
    val splashLogIn:      String
    val splashTerms:      String
    // Destination pills shown on the splash (keep emoji inline)
    val splashParis:     String
    val splashTokyo:     String
    val splashNyc:       String
    val splashRome:      String
    val splashSantorini: String

    // ── Login Screen ────────────────────────────────────────────────────────
    val loginSubtitle:        String
    val loginTitle:           String
    val loginEmailLabel:      String
    val loginPasswordLabel:   String
    val loginForgotPassword:  String
    /** Title shown above the password-reset email field. */
    val loginResetPasswordTitle:    String
    /** Body text inside the password-reset dialog. */
    val loginResetPasswordMessage:  String
    /** CTA on the password-reset dialog that triggers the email send. */
    val loginResetPasswordSend:     String
    /** Confirmation snackbar/dialog after the reset email is sent. */
    val loginResetPasswordSent:     String
    val loginLoadingButton:   String
    val loginButton:          String
    val loginNoAccount:       String
    val loginSignUpLink:      String
    val loginPasswordHide:    String
    val loginPasswordShow:    String

    // ── Sign Up Screen ──────────────────────────────────────────────────────
    val signUpSubtitle:        String
    val signUpTitle:           String
    val signUpDisplayName:     String
    val signUpEmailLabel:      String
    val signUpPasswordLabel:   String
    val signUpConfirmPassword: String
    val signUpLoadingButton:   String
    val signUpButton:          String
    val signUpHasAccount:      String
    val signUpLoginLink:       String

    // ── Auth error messages ─────────────────────────────────────────────────
    // Shown on the login / signup / delete-account screens instead of the raw
    // Firebase exception text. The viewmodel hands the UI an AuthErrorCode and
    // the UI resolves it via the helper below.
    val authErrorInvalidCredentials: String
    val authErrorEmailAlreadyInUse:  String
    val authErrorWeakPassword:       String
    val authErrorInvalidEmail:       String
    val authErrorUserNotFound:       String
    val authErrorUserDisabled:       String
    val authErrorNetwork:            String
    val authErrorTooManyRequests:    String
    val authErrorUnknown:            String

    // ── Explore Screen ──────────────────────────────────────────────────────
    val greetingMorning:      String
    val greetingAfternoon:    String
    val greetingEvening:      String
    val exploreSubtitle:      String
    val exploreContinue:      String
    val exploreLastOpened:    String
    val exploreUpcoming:      String
    val exploreFindVibe:      String
    val vibeBeach:            String
    val vibeMountain:         String
    val vibeCulture:          String
    val vibeFoodie:           String
    val travelTipsTitle:      String
    val tipPlanAheadTitle:    String
    val tipPlanAheadBody:     String
    val tipInviteTitle:       String
    val tipInviteBody:        String
    val tipTrackTitle:        String
    val tipTrackBody:         String

    // ── Profile Screen ──────────────────────────────────────────────────────
    val profileDefaultName:        String
    val profileTripsLabel:         String
    val profileFavoritesLabel:     String
    val profileMemberSince:        String
    val profileAccountSection:     String
    val profileNameLabel:          String
    val profileEmailLabel:         String
    val profileDangerZone:         String
    val profileSignOut:            String
    val profileDeleteAccount:      String
    val profileSignOutTitle:       String
    val profileSignOutMessage:     String
    val profileDeleteTitle:        String
    val profileDeleteMessage:      String
    val profileDeleteConfirm:      String
    val profileReAuthTitle:        String
    val profileReAuthMessage:      String
    val profilePasswordLabel:      String
    val profileDeleteButton:       String
    val profileLanguageSection:    String
    val profileAppearanceSection:  String
    val themeSystem:               String
    val themeLight:                String
    val themeDark:                 String

    // ── Trip List / Side Navigation (accessibility) ─────────────────────────
    /** Content description for the favourite-toggle icon button. */
    val sideNavFavorite: String

    // ── Trip List Screen ────────────────────────────────────────────────────
    val tripListTitle:       String
    /** "Active" badge on TripCard for an in-progress trip. */
    val tripStatusActive:    String
    /** "Upcoming" badge on TripCard for a future trip. */
    val tripStatusUpcoming:  String
    /** "Past" badge on TripCard for a finished trip. */
    val tripStatusPast:      String
    /** "{n} members" — total member count including the creator. */
    val tripCardMembers:     (Int) -> String
    val tripListEmptyTitle:  String
    val tripListEmptyBody:   String
    val tripListCreateButton: String

    // ── Trip Detail Screen ──────────────────────────────────────────────────
    val tripDetailFallback:        String
    val tripDetailNoDateRange:     String
    /** Header icon button label — also used as the edit-trip dialog title fallback. */
    val tripDetailEditLabel:       String
    /** Header icon button label — invite a member to the trip via share sheet. */
    val tripDetailInviteLabel:     String
    /** Header icon button label — owner-only destructive action. */
    val tripDetailDeleteLabel:     String
    /** Header icon button label — non-owner destructive action. */
    val tripDetailLeaveLabel:      String
    val tripDetailLeaveTitle:      String
    val tripDetailLeaveMessage:    String
    val tripDetailLeaveButton:     String
    val tripDetailDeleteTitle:     (String) -> String
    val tripDetailDeleteMessage:   String
    val tripDetailNotFound:        String

    // ── Budget Screen ────────────────────────────────────────────────────────
    val budgetTotalSpent:    String
    val budgetNoExpenses:    String
    val budgetNoExpensesBody: String
    val budgetExpenseCount:  (Int) -> String
    val budgetAddExpense:    String
    val budgetSettlement:    String
    val budgetPerPerson:     String
    val budgetExpenses:      String
    val budgetPaid:          String
    val budgetShare:         String
    val budgetBalance:       String
    val budgetOwedBack:      (String, String) -> String
    val budgetOwes:          (String, String) -> String
    val budgetSettledUp:     (String) -> String
    val budgetPaidSuffix:    String
    /** Alert title when the user taps the delete icon on an expense. */
    val budgetDeleteExpenseTitle: String
    /** Alert body — [name] is the expense title being deleted. */
    val budgetDeleteExpenseBody:  (String) -> String
    /** Row label for the total amount in the expense detail sheet. */
    val budgetExpenseTotal:       String
    /** Row label for the expense date in the expense detail sheet. */
    val budgetExpenseDate:        String

    // ── Add / Edit Expense Sheet ─────────────────────────────────────────────
    val expenseNewTitle:              String
    val expenseEditTitle:             String
    val expenseDescLabel:             String
    val expenseDescPlaceholder:       String
    val expenseAmountLabel:           String
    val expenseCategoryLabel:         String
    /** Localised label for each [ExpenseCategory] — shown on the category chips in AddExpenseDialog. */
    val expenseCategoryLabels:        Map<ExpenseCategory, String>
    val expensePaidByLabel:           String
    val expenseSplitLabel:            String
    val expenseSplitEnterAmount:      String
    val expenseSplitBalanced:         String
    val expenseSplitLeft:             (String) -> String
    val expenseSplitOver:             (String) -> String
    val expenseSplitEqually:          String
    val expenseSaveChanges:           String
    val expenseAddButton:             String

    // ── Chat Screen ──────────────────────────────────────────────────────────
    val chatNoMessages:        String
    val chatStartConversation: String
    /** Placeholder text inside the message-composer text field. */
    val chatInputPlaceholder:  String
    /** Accessibility content description for the send icon button. */
    val chatSendButton:        String
    /** Formats a message timestamp (epoch millis) for the chat bubble subtitle. */
    val chatDateFormat:        (Long) -> String

    // ── AI Chat Screen ───────────────────────────────────────────────────────
    val aiChatTitle:    String
    val aiChatSubtitle: String
    /** Pill-segment label for the group chat tab inside TripChatWrapperScreen. */
    val chatTabGroup:   String
    /** Pill-segment label for the AI chat tab inside TripChatWrapperScreen. */
    val chatTabAi:      String

    // ── Favorites Screen ─────────────────────────────────────────────────────

    // ── Manage Members Sheet ─────────────────────────────────────────────────
    val membersTitle:          String
    val membersPeopleCount:    (Int) -> String
    val membersOwner:          String
    val membersRenameTitle:    String
    val membersRenameLabel:    String
    val membersRenameButton:   String
    val membersRemoveTitle:    String
    val membersRemoveMessage:  (String) -> String
    val membersRemoveConfirm:  String

    // ── Edit Trip Dialog ─────────────────────────────────────────────────────
    val editTripTitle:       String
    val editTripNameLabel:   String
    val editTripDescLabel:   String
    val editTripSelectDates: String
    val editTripDatesLabel:  String
    val editTripTapSelect:   String
    val editTripSave:        String

    // ── Share Trip Dialog ────────────────────────────────────────────────────
    val shareTitle:       (String) -> String
    val shareFallback:    String
    val shareCodeLabel:   String
    val shareCopyCode:    String
    val shareCopied:      String
    val shareOrEmail:     String
    val shareEmailLabel:  String
    val shareSendInvite:  String
    val shareQrScan:      String
    val shareCopyLink:    String

    // ── Join Trip Screen ─────────────────────────────────────────────────────
    val joinTripTitle:            String
    val joinTripSubtitle:         String
    val joinTripCodeLabel:        String
    val joinTripCodePlaceholder:  String
    val joinTripJoin:             String
    val joinTripJoining:          String
    val joinTripInvalidCode:      String
    val joinTripAlreadyMember:    String
    val joinTripSuccess:          String
    val joinTripButton:           String
    val joinTripScanQr:           String
    val joinTripScanHint:         String
    val joinTripCameraPermission: String
    val joinTripScannerNotSupported: String
    /** Shown when the JS browser exposes no `mediaDevices` API at all (older browsers). */
    val joinTripCameraUnavailable: String

    // ── New Trip Flow ────────────────────────────────────────────────────────
    val newTripEyebrow:        String
    val newTripFlowTitle:      String
    val newTripWhereGoing:     String
    val newTripDestSubtitle:   String
    val newTripDestSearch:     String
    val newTripSetDestination: String
    val newTripPickDates:      String
    val newTripCreate:         String
    val newTripNameLabel:      String
    val newTripDescLabel:      String
    val newTripNamePlaceholder: String
    val newTripDescPlaceholder: String
    val newTripSkipDates:      String
    /** Step-2 title: "When are you going?" */
    val newTripWhenGoing:      String
    /** Step-2 subtitle below the title. */
    val newTripTapSetDates:    String
    /** Button to reset the selected date range. */
    val newTripClearDates:     String
    /** Content description for the "previous month" chevron. */
    val newTripPrevMonth:      String
    /** Content description for the "next month" chevron. */
    val newTripNextMonth:      String
    /**
     * Two-letter weekday column headers, Sunday-first (index 0 = Sunday).
     * Must have exactly 7 entries.
     */
    val newTripWeekDayLabels:  List<String>
    /** "{n} night(s)" label below the range pill. */
    val newTripNights:         (Int) -> String
    /** Step-0 section title: "Name your trip". */
    val newTripNameTitle:      String
    /** Step-0 subtitle below the title. */
    val newTripNameSubtitle:   String
    /** Range-pill text when no start date is chosen yet. */
    val newTripSelectStart:    String
    /**
     * Range-pill text after start is chosen but end is not.
     * Parameters: (shortMonthName, dayNumber) → e.g. "Apr 27 → pick end date"
     */
    val newTripPickEnd:        (String, Int) -> String

    // ── Places Screen ────────────────────────────────────────────────────────
    /** Section title above the destination input on the Places search sheet. */
    val placesWhereTo:           String
    /** Section title above the suggested-places list — "{locationName}" is interpolated. */
    val placesPopularIn:         (String) -> String
    /** Empty-state shown when the search query yields no results. */
    val placesNoMatches:         String
    /** Body below [placesNoMatches] in the no-results state. */
    val placesNoMatchesHint:     String
    /** Hint text shown in the no-destination empty state. */
    val placesSetDestHint:       String
    /** CTA button label in the no-destination empty state. */
    val placesChooseDestination: String
    /** Sub-label above the destination name in the destination pill. */
    val placesExploring:         String
    /** Content description for the "change destination" icon. */
    val placesChangeDestination: String
    /** Title in the no-saved-places empty state (destination set, but list empty). */
    val placesNoPlaces:          String
    /** Body in the no-saved-places empty state. */
    val placesNoPlacesBody:      String
    /** Eyebrow label at the top of the search sheet. */
    val placesDiscoverEyebrow:   String
    /** Content description for the "close search sheet" icon button. */
    val placesCloseSearch:       String
    /** Placeholder inside the search text field. */
    val placesSearchPlaceholder: String
    /** Search button label when idle. */
    val placesSearch:            String
    /** Search button label while searching. */
    val placesSearching:         String
    /** Content description for the "add / save place" icon button. */
    val placesSavePlace:         String
    /** "Load more" button inside the search results list. */
    val placesLoadMore:          String

    // ── Map Screen ───────────────────────────────────────────────────────────
    val mapDailySummary:         String
    val mapNoDays:               String
    val mapNoPlacesAssigned:     String
    val mapUnassigned:           String
    val mapSummaryButton:        String
    val mapAllDays:              String
    val mapDayLabel:             (Int) -> String
    val mapClose:                String
    val mapMyLocation:           String
    val mapNoPlacesTitle:        String
    val mapNoPlacesBody:         String

    // ── Gallery Screen ───────────────────────────────────────────────────────
    val galleryEmpty:       String
    val galleryEmptyBody:   String
    val galleryAddPhoto:    String
    val galleryDeleteTitle: String
    val galleryDeleteBody:  String
    val galleryBy:          String
    val galleryPhotoCount:  (Int) -> String

    // ── Date formatting ──────────────────────────────────────────────────────
    /**
     * Abbreviated month names, January … December (index 0 = January).
     * Used wherever a compact date such as "Apr 27" or "ápr. 27" is displayed.
     */
    val monthShortNames: List<String>
    /**
     * Full month names, January … December (index 0 = January).
     * Used as calendar month headers (e.g. "April 2026").
     */
    val monthFullNames: List<String>

    // ── Offline Banner ───────────────────────────────────────────────────────
    val offlineBannerText:       String
}

/**
 * Returns the localised display label for this expense category.
 *
 * The exhaustive `when` is the compile-time guard: adding a new
 * [ExpenseCategory] value without updating this function (and the
 * corresponding [Strings.expenseCategoryLabels] map) produces a compile error.
 */
fun ExpenseCategory.localizedLabel(strings: Strings): String = when (this) {
    ExpenseCategory.FOOD_AND_DRINK         -> strings.expenseCategoryLabels[ExpenseCategory.FOOD_AND_DRINK]         ?: displayName
    ExpenseCategory.TRANSPORTATION         -> strings.expenseCategoryLabels[ExpenseCategory.TRANSPORTATION]         ?: displayName
    ExpenseCategory.ACCOMMODATION          -> strings.expenseCategoryLabels[ExpenseCategory.ACCOMMODATION]          ?: displayName
    ExpenseCategory.ACTIVITIES_AND_TICKETS -> strings.expenseCategoryLabels[ExpenseCategory.ACTIVITIES_AND_TICKETS] ?: displayName
    ExpenseCategory.SHOPPING               -> strings.expenseCategoryLabels[ExpenseCategory.SHOPPING]               ?: displayName
    ExpenseCategory.OTHER                  -> strings.expenseCategoryLabels[ExpenseCategory.OTHER]                  ?: displayName
}

/**
 * Returns the localised user-facing message for an [AuthErrorCode].
 *
 * Exhaustive `when` again — adding a new [AuthErrorCode] without translating
 * it here is a compile error, so no error code can ship without a message.
 */
fun AuthErrorCode.localizedMessage(strings: Strings): String = when (this) {
    AuthErrorCode.INVALID_CREDENTIALS  -> strings.authErrorInvalidCredentials
    AuthErrorCode.EMAIL_ALREADY_IN_USE -> strings.authErrorEmailAlreadyInUse
    AuthErrorCode.WEAK_PASSWORD        -> strings.authErrorWeakPassword
    AuthErrorCode.INVALID_EMAIL        -> strings.authErrorInvalidEmail
    AuthErrorCode.USER_NOT_FOUND       -> strings.authErrorUserNotFound
    AuthErrorCode.USER_DISABLED        -> strings.authErrorUserDisabled
    AuthErrorCode.NETWORK_ERROR        -> strings.authErrorNetwork
    AuthErrorCode.TOO_MANY_REQUESTS    -> strings.authErrorTooManyRequests
    AuthErrorCode.UNKNOWN              -> strings.authErrorUnknown
}