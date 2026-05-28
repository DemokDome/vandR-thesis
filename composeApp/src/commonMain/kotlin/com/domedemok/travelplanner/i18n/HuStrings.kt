@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.domedemok.travelplanner.i18n

import com.domedemok.travelplanner.data.model.ExpenseCategory
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val HU_MONTH_SHORT = listOf(
    "jan.", "febr.", "márc.", "ápr.", "máj.", "jún.",
    "júl.", "aug.", "szept.", "okt.", "nov.", "dec."
)

/** "2026. ápr. 27." */
private fun fmtChatDateHu(timestamp: Long): String {
    val dt = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}. ${HU_MONTH_SHORT[dt.month.number - 1]} ${dt.day}."
}

object HuStrings : Strings {

    // ── Navigáció ────────────────────────────────────────────────────────────
    override val navDiscover   = "Felfedezés"
    override val navMyTrips    = "Utazásaim"
    override val navInbox      = "Értesítők"
    override val navProfile    = "Profil"
    override val navPlaces     = "Helyek"
    override val navMap        = "Térkép"
    override val navChat       = "Chat"
    override val navBudget     = "Költségvetés"
    override val navPlan       = "Úti Terv"
    override val navGallery    = "Galéria"

    // ── Általános ────────────────────────────────────────────────────────────
    override val generalOk      = "OK"
    override val generalCancel  = "Mégse"
    override val generalSave    = "Mentés"
    override val generalDelete  = "Törlés"
    override val generalClose   = "Bezárás"
    override val generalDismiss = "Elvetés"
    override val generalUnknown = "Ismeretlen"
    override val generalBack    = "Vissza"
    override val generalNext    = "Tovább"
    override val generalSkip    = "Kihagyás"
    override val generalCreate  = "Létrehozás"
    override val generalEdit    = "Szerkesztés"
    override val generalRename  = "Átnevezés"
    override val generalRemove  = "Eltávolítás"
    override val generalSend    = "Küldés"

    // ── Kezdőképernyő ────────────────────────────────────────────────────────
    override val splashAppName    = "vandR"
    override val splashTagline    = "A világ, megtervezve."
    override val splashGetStarted = "Kezdés"
    override val splashLogIn      = "Bejelentkezés"
    override val splashTerms      = "A folytatással elfogadod az ÁSZF-et és az Adatvédelmi Szabályzatot"
    override val splashParis      = "🗼 Párizs"
    override val splashTokyo      = "⛩️ Tokió"
    override val splashNyc        = "🗽 New York"
    override val splashRome       = "🏛️ Róma"
    override val splashSantorini  = "🌅 Santorini"

    // ── Bejelentkezés ────────────────────────────────────────────────────────
    override val loginSubtitle       = "Üdvözlünk vissza"
    override val loginTitle          = "Bejelentkezés"
    override val loginEmailLabel     = "E-mail cím"
    override val loginPasswordLabel  = "Jelszó"
    override val loginForgotPassword         = "Elfelejtett jelszó?"
    override val loginResetPasswordTitle     = "Jelszó visszaállítása"
    override val loginResetPasswordMessage   = "Add meg az email-címed, és küldünk egy jelszó-visszaállító linket."
    override val loginResetPasswordSend      = "Visszaállító link küldése"
    override val loginResetPasswordSent      = "Ha létezik fiók ehhez az email-címhez, elküldtük a visszaállító linket."
    override val loginLoadingButton          = "Bejelentkezés..."
    override val loginButton         = "Bejelentkezés"
    override val loginNoAccount      = "Nincs még fiókod?"
    override val loginSignUpLink     = "Regisztráció"
    override val loginPasswordHide   = "Jelszó elrejtése"
    override val loginPasswordShow   = "Jelszó megjelenítése"

    // ── Regisztráció ─────────────────────────────────────────────────────────
    override val signUpSubtitle        = "Kezdd el az utazásod"
    override val signUpTitle           = "Fiók létrehozása"
    override val signUpDisplayName     = "Megjelenített név"
    override val signUpEmailLabel      = "E-mail cím"
    override val signUpPasswordLabel   = "Jelszó (min. 8 karakter)"
    override val signUpConfirmPassword = "Jelszó megerősítése"
    override val signUpLoadingButton   = "Fiók létrehozása..."
    override val signUpButton          = "Fiók létrehozása"
    override val signUpHasAccount      = "Van már fiókod?"
    override val signUpLoginLink       = "Bejelentkezés"

    // ── Auth hibák ───────────────────────────────────────────────────────────
    override val authErrorInvalidCredentials = "Helytelen email cím vagy jelszó. Próbáld újra."
    override val authErrorEmailAlreadyInUse  = "Már létezik fiók ezzel az email címmel. Inkább jelentkezz be."
    override val authErrorWeakPassword       = "Túl gyenge jelszó. Használj legalább 8 karaktert, betűk és számok keverékével."
    override val authErrorInvalidEmail       = "Ez nem tűnik érvényes email címnek."
    override val authErrorUserNotFound       = "Nincs fiók ezzel az email címmel."
    override val authErrorUserDisabled       = "Ez a fiók letiltásra került. Fordulj az ügyfélszolgálathoz."
    override val authErrorNetwork            = "Nem sikerült elérni a szervert. Ellenőrizd az internetkapcsolatot és próbáld újra."
    override val authErrorTooManyRequests    = "Túl sok próbálkozás. Várj néhány percet, mielőtt újra megpróbálnád."
    override val authErrorUnknown            = "Hiba történt. Próbáld újra."

    // ── Felfedezés ───────────────────────────────────────────────────────────
    override val greetingMorning   = "Jó reggelt"
    override val greetingAfternoon = "Jó napot"
    override val greetingEvening   = "Jó estét"
    override val exploreSubtitle   = "Fedezd fel a következő\nkalandodat"
    override val exploreContinue   = "Folytatás"
    override val exploreLastOpened = "Utoljára megnyitva"
    override val exploreUpcoming   = "Közelgő utazások"
    override val exploreFindVibe   = "Találd meg a hangulatod"
    override val vibeBeach         = "Tengerpart"
    override val vibeMountain      = "Hegyvidék"
    override val vibeCulture       = "Kultúra"
    override val vibeFoodie        = "Gasztronómia"
    override val travelTipsTitle   = "Utazási tippek"
    override val tipPlanAheadTitle = "Tervezz előre"
    override val tipPlanAheadBody  = "A részletes útvonaltervvel rendelkező utazások 3× nagyobb valószínűséggel maradnak a büdzsén belül."
    override val tipInviteTitle    = "Hívj barátokat"
    override val tipInviteBody     = "A közös utazások szórakoztatóbbak — oszd meg az utazás menüből."
    override val tipTrackTitle     = "Kövesd a kiadásokat"
    override val tipTrackBody      = "Használd a Büdzsé lapot az egyes utazásokon belül az igazságos költségmegosztáshoz."

    // ── Profil ───────────────────────────────────────────────────────────────
    override val profileDefaultName     = "Utazó"
    override val profileTripsLabel      = "Utazások"
    override val profileFavoritesLabel  = "Kedvencek"
    override val profileMemberSince     = "Tagság kezdete"
    override val profileAccountSection  = "Fiók"
    override val profileNameLabel       = "Név"
    override val profileEmailLabel      = "E-mail"
    override val profileDangerZone      = "Veszélyzóna"
    override val profileSignOut         = "Kijelentkezés"
    override val profileDeleteAccount   = "Fiók törlése"
    override val profileSignOutTitle    = "Kijelentkezés?"
    override val profileSignOutMessage  = "Visszakerülsz a bejelentkezési képernyőre."
    override val profileDeleteTitle     = "Fiók törlése?"
    override val profileDeleteMessage   = "Ez véglegesen törli a fiókodat és az összes kapcsolódó adatot. Ez nem vonható vissza."
    override val profileDeleteConfirm   = "Igen, törlés"
    override val profileReAuthTitle     = "Erősítsd meg a jelszavadat"
    override val profileReAuthMessage   = "Biztonsági okokból add meg a jelszavadat a fiók törléséhez."
    override val profilePasswordLabel   = "Jelszó"
    override val profileDeleteButton    = "Törlés"
    override val profileLanguageSection = "Nyelv"
    override val profileAppearanceSection = "Megjelenés"
    override val themeSystem            = "Rendszer"
    override val themeLight             = "Világos"
    override val themeDark              = "Sötét"

    // ── Utazások listája / oldalsó navigáció (akadálymentesítés) ─────────────
    override val sideNavFavorite = "Kedvenc"

    // ── Utazások listája ──────────────────────────────────────────────────────
    override val tripListTitle        = "Utazásaim"
    override val tripStatusActive     = "Folyamatban"
    override val tripStatusUpcoming   = "Hamarosan"
    override val tripStatusPast       = "Lezárult"
    override val tripCardMembers: (Int) -> String = { count -> if (count == 1) "1 tag" else "$count tag" }
    override val tripListEmptyTitle   = "Hova utazol következőnek?"
    override val tripListEmptyBody    = "Hozd létre az első utazásodat és\nkezdj el tervezni!"
    override val tripListCreateButton = "Utazás létrehozása"

    // ── Utazás részletek ──────────────────────────────────────────────────────
    override val tripDetailFallback      = "Utazás"
    override val tripDetailNoDateRange   = "Nincs dátum megadva"
    override val tripDetailEditLabel     = "Utazás szerkesztése"
    override val tripDetailInviteLabel   = "Tag meghívása"
    override val tripDetailDeleteLabel   = "Utazás törlése"
    override val tripDetailLeaveLabel    = "Utazás elhagyása"
    override val tripDetailLeaveTitle    = "Utazás elhagyása?"
    override val tripDetailLeaveMessage  = "Biztosan el akarod hagyni ezt az utazást?"
    override val tripDetailLeaveButton   = "Elhagyás"
    override val tripDetailDeleteTitle: (String) -> String = { name -> "Törlés: \"$name\"?" }
    override val tripDetailDeleteMessage = "Ez a művelet nem vonható vissza."
    override val tripDetailNotFound      = "Az utazás nem található"

    // ── Büdzsé ───────────────────────────────────────────────────────────────
    override val budgetTotalSpent   = "Összesen elköltve"
    override val budgetNoExpenses   = "Még nincsenek kiadások"
    override val budgetNoExpensesBody = "Koppints a Kiadás hozzáadása gombra, és kezd el nyomon követni,\nki mennyit költött az utazáson."
    override val budgetExpenseCount: (Int) -> String = { n -> if (n == 1) "1 kiadás" else "$n kiadás" }
    override val budgetAddExpense   = "Kiadás hozzáadása"
    override val budgetSettlement   = "Elszámolás"
    override val budgetPerPerson    = "Személyenként"
    override val budgetExpenses     = "Kiadások"
    override val budgetPaid         = "Fizetett"
    override val budgetShare        = "Részarány"
    override val budgetBalance      = "Egyenleg"
    override val budgetOwedBack: (String, String) -> String = { name, amount -> "$name visszakap $amount-t" }
    override val budgetOwes: (String, String) -> String = { name, amount -> "$name tartozik $amount-val" }
    override val budgetSettledUp: (String) -> String = { name -> "$name rendezve ✓" }
    override val budgetPaidSuffix         = "fizette"
    override val budgetDeleteExpenseTitle = "Kiadás törlése?"
    override val budgetDeleteExpenseBody: (String) -> String = { name -> "$name el lesz távolítva és az egyenlegek újra lesznek számítva." }
    override val budgetExpenseTotal       = "Összeg"
    override val budgetExpenseDate        = "Dátum"

    // ── Kiadás hozzáadása ────────────────────────────────────────────────────
    override val expenseNewTitle         = "Új kiadás"
    override val expenseEditTitle        = "Kiadás szerkesztése"
    override val expenseDescLabel        = "Leírás"
    override val expenseDescPlaceholder  = "pl. Vacsora, taxi, múzeum…"
    override val expenseAmountLabel      = "Teljes összeg"
    override val expenseCategoryLabel    = "Kategória"
    override val expenseCategoryLabels   = mapOf(
        ExpenseCategory.FOOD_AND_DRINK         to "Étel & Ital",
        ExpenseCategory.TRANSPORTATION         to "Közlekedés",
        ExpenseCategory.ACCOMMODATION          to "Szállás",
        ExpenseCategory.ACTIVITIES_AND_TICKETS to "Programok & Jegyek",
        ExpenseCategory.SHOPPING               to "Vásárlás",
        ExpenseCategory.OTHER                  to "Egyéb"
    )
    override val expensePaidByLabel      = "Fizette"
    override val expenseSplitLabel       = "Felosztás"
    override val expenseSplitEnterAmount = "Először add meg az összeget"
    override val expenseSplitBalanced    = "Kiegyenlítve ✓"
    override val expenseSplitLeft: (String) -> String = { amount -> "$amount még kiosztandó" }
    override val expenseSplitOver: (String) -> String = { amount -> "$amount túllépi az összeget" }
    override val expenseSplitEqually     = "Egyenlő felosztás"
    override val expenseSaveChanges      = "Változások mentése"
    override val expenseAddButton        = "Kiadás hozzáadása"

    // ── Chat ─────────────────────────────────────────────────────────────────
    override val chatNoMessages        = "Még nincsenek üzenetek"
    override val chatStartConversation = "Kezdd el a beszélgetést!"
    override val chatInputPlaceholder  = "Írj egy üzenetet…"
    override val chatSendButton        = "Küldés"
    override val chatDateFormat: (Long) -> String = ::fmtChatDateHu

    // ── AI Chat ──────────────────────────────────────────────────────────────
    override val aiChatTitle    = "Kérdezd az AI-t"
    override val aiChatSubtitle = "Kapj okos utazási javaslatokat\nés személyre szabott tippeket!"
    override val chatTabGroup   = "Csoport Chat"
    override val chatTabAi      = "Gemini AI"

    // ── Kedvencek ────────────────────────────────────────────────────────────

    // ── Tagok kezelése ───────────────────────────────────────────────────────
    override val membersTitle         = "Tagok"
    override val membersPeopleCount: (Int) -> String = { n -> if (n == 1) "1 személy" else "$n személy" }
    override val membersOwner         = "Tulajdonos"
    override val membersRenameTitle   = "Tag átnevezése"
    override val membersRenameLabel   = "Új név"
    override val membersRenameButton  = "Átnevezés"
    override val membersRemoveTitle   = "Tag eltávolítása?"
    override val membersRemoveMessage: (String) -> String = { name -> "Eltávolítod $name-t ebből az utazásból?" }
    override val membersRemoveConfirm = "Eltávolítás"

    // ── Utazás szerkesztése ──────────────────────────────────────────────────
    override val editTripTitle       = "Utazás szerkesztése"
    override val editTripNameLabel   = "Utazás neve"
    override val editTripDescLabel   = "Leírás"
    override val editTripSelectDates = "Utazás dátumainak kiválasztása"
    override val editTripDatesLabel  = "Utazás dátumai"
    override val editTripTapSelect   = "Érintsd meg a dátumok kiválasztásához"
    override val editTripSave        = "Mentés"

    // ── Utazás megosztása ────────────────────────────────────────────────────
    override val shareTitle: (String) -> String = { name -> "Megosztás: \"$name\"" }
    override val shareFallback   = "Utazás megosztása"
    override val shareCodeLabel  = "CSATLAKOZÁSI KÓD"
    override val shareCopyCode   = "Kód másolása"
    override val shareCopied     = "Másolva!"
    override val shareOrEmail    = "Vagy hívj meg e-mailben"
    override val shareEmailLabel = "E-mail cím"
    override val shareSendInvite = "Meghívó küldése"
    override val shareQrScan     = "Szkenneld be a csatlakozáshoz"
    override val shareCopyLink   = "Link másolása"

    // ── Utazáshoz csatlakozás ────────────────────────────────────────────────
    override val joinTripTitle         = "Csatlakozás utazáshoz"
    override val joinTripSubtitle      = "Írd be a megosztott 6 karakteres kódot, vagy olvasd be a QR-kódot a kamerával"
    override val joinTripCodeLabel     = "Csatlakozási kód"
    override val joinTripCodePlaceholder = "pl. AB3K7X"
    override val joinTripJoin          = "Csatlakozás"
    override val joinTripJoining       = "Csatlakozás…"
    override val joinTripInvalidCode   = "Érvénytelen kód. Kérjük, ellenőrizd és próbáld újra."
    override val joinTripAlreadyMember = "Már tagja vagy ennek az utazásnak."
    override val joinTripSuccess       = "Sikeresen csatlakoztál az utazáshoz!"
    override val joinTripButton        = "Csatlakozás utazáshoz"
    override val joinTripScanQr        = "QR-kód beolvasása"
    override val joinTripScanHint      = "Tartsd a kamerát a QR-kód felé"
    override val joinTripCameraPermission = "A QR-kód beolvasásához kamera engedély szükséges."
    override val joinTripScannerNotSupported = "A QR-beolvasáshoz Chrome vagy Edge böngésző szükséges."
    override val joinTripCameraUnavailable   = "A kamera nem érhető el ebben a böngészőben."

    // ── Új utazás ────────────────────────────────────────────────────────────
    override val newTripEyebrow         = "FELFEDEZÉS"
    override val newTripFlowTitle       = "Tervezz új utazást"
    override val newTripWhereGoing      = "Hova utazol?"
    override val newTripDestSubtitle    = "Válassz úti célt a kezdéshez"
    override val newTripDestSearch      = "Keress városra, vagy írd be a saját célpontod…"
    override val newTripSetDestination  = "Úti cél megadása"
    override val newTripPickDates       = "Dátumok kiválasztása"
    override val newTripCreate          = "Utazás létrehozása"
    override val newTripNameLabel       = "Utazás neve"
    override val newTripDescLabel       = "Leírás (opcionális)"
    override val newTripNamePlaceholder = "pl. Nyár Olaszországban"
    override val newTripDescPlaceholder = "Miről szól ez az utazás?"
    override val newTripSkipDates       = "Kihagyás"
    override val newTripWhenGoing       = "Mikor utazol?"
    override val newTripTapSetDates     = "Koppints az utazási dátumok beállításához"
    override val newTripClearDates      = "Törlés"
    override val newTripPrevMonth       = "Előző hónap"
    override val newTripNextMonth       = "Következő hónap"
    override val newTripWeekDayLabels   = listOf("V", "H", "K", "Sze", "Cs", "P", "Szo")
    override val newTripNights: (Int) -> String = { n -> if (n == 1) "1 éjszaka" else "$n éjszaka" }
    override val newTripNameTitle       = "Nevezd el az utazásodat"
    override val newTripNameSubtitle    = "Adj egy emlékezetes nevet a kalandodnak"
    override val newTripSelectStart     = "Válaszd ki a kezdő dátumot"
    // U+2192 (→) is missing from the bundled Noto Sans Skia font on web; render
    // it as a middle dot instead so it doesn't show up as a tofu box.
    override val newTripPickEnd: (String, Int) -> String = { month, day -> "$month $day · válaszd ki a végdátumot" }

    // ── Helyek képernyő ──────────────────────────────────────────────────────
    override val placesWhereTo           = "Hová mész?"
    override val placesPopularIn: (String) -> String = { location -> "Népszerű helyek itt: $location" }
    override val placesNoMatches         = "Nincs találat"
    override val placesNoMatchesHint     = "Próbálj más kulcsszót, kategóriát vagy várost."
    override val placesSetDestHint       = "Adj meg egy úti célt a helyek hozzáadásának megkezdéséhez."
    override val placesChooseDestination = "Úti cél kiválasztása"
    override val placesExploring         = "Felfedezés"
    override val placesChangeDestination = "Úti cél módosítása"
    override val placesNoPlaces          = "Még nincsenek mentett helyek"
    override val placesNoPlacesBody      = "Koppints a Felfedezés gombra, hogy megtaláld\na népszerű helyeket és mentsd az útitervbe."
    override val placesDiscoverEyebrow   = "FELFEDEZÉS"
    override val placesCloseSearch       = "Keresés bezárása"
    override val placesSearchPlaceholder = "pl. Eiffel-torony, kávé, sushi…"
    override val placesSearch            = "Keresés"
    override val placesSearching         = "Keresés…"
    override val placesSavePlace         = "Hely mentése"
    override val placesLoadMore          = "Több betöltése"

    // ── Térkép képernyő ──────────────────────────────────────────────────────
    override val mapDailySummary     = "Napi összefoglaló"
    override val mapNoDays           = "Még nincsenek napok az útitervben."
    override val mapNoPlacesAssigned = "Nincs hozzárendelt hely"
    override val mapUnassigned       = "Nem hozzárendelt"
    override val mapSummaryButton    = "Összefoglaló"
    override val mapAllDays          = "Összes"
    override val mapDayLabel: (Int) -> String = { n -> "$n. nap" }
    override val mapClose            = "Bezárás"
    override val mapMyLocation       = "Saját helyzetem"
    override val mapNoPlacesTitle    = "Nincs megjelenítendő hely"
    override val mapNoPlacesBody     = "Adj hozzá helyeket a Helyek fülön,\nhogy megjelenjenek a térképen."

    // ── Galéria ──────────────────────────────────────────────────────────────
    override val galleryEmpty       = "Még nincsenek fotók"
    override val galleryEmptyBody   = "Adj hozzá fotókat az utazásod emlékéül.\nNyomj a + gombra a kezdéshez."
    override val galleryAddPhoto    = "Fotó hozzáadása"
    override val galleryDeleteTitle = "Fotó törlése?"
    override val galleryDeleteBody  = "Ez a fotó véglegesen törlésre kerül."
    override val galleryBy          = "feltöltötte:"
    override val galleryPhotoCount: (Int) -> String = { n -> if (n == 1) "1 fotó" else "$n fotó" }

    // ── Dátumformátum ────────────────────────────────────────────────────────
    override val monthShortNames = HU_MONTH_SHORT
    override val monthFullNames  = listOf(
        "január", "február", "március", "április", "május", "június",
        "július", "augusztus", "szeptember", "október", "november", "december"
    )

    // ── Offline csík ─────────────────────────────────────────────────────────
    override val offlineBannerText   = "Nincs internetkapcsolat – offline mód"

    // ── Értesítők képernyő ───────────────────────────────────────────────────
    override val inboxTitle        = "Értesítők"
    override val inboxEmpty        = "Minden elolvasva!"
    override val inboxEmptyBody    = "Az utazásaid friss aktivitása itt jelenik meg."
    override val inboxMarkAllRead  = "Mind olvasottnak jelöl"
    override val inboxNewMessage: (String) -> String = { name -> "Új üzenet: $name" }
    override val inboxNewPlace: (String) -> String = { name -> "Új hely mentve: $name" }
    override val inboxNewExpense: (String) -> String = { title -> "Új kiadás: $title" }
    override val inboxInTrip: (String) -> String = { trip -> "$trip utazásban" }
    override val inboxPlaceRemoved: (String) -> String = { name  -> "Hely eltávolítva: $name" }
    override val inboxExpenseRemoved: (String) -> String = { title -> "Kiadás eltávolítva: $title" }
    override val inboxAddedToTrip: (String) -> String = { trip  -> "Hozzáadtak az utazához: $trip" }
    override val inboxMemberJoined: (String) -> String = { name  -> "$name csatlakozott az utazáshoz" }
    override val inboxMemberLeft: (String) -> String = { name  -> "$name elhagyta az utazást" }
    override val inboxClearAllTitle   = "Összes értesítés törlése?"
    override val inboxClearAllBody    = "Minden értesítés törlésre kerül. Ez nem vonható vissza."
    override val inboxClearAllConfirm = "Összes törlése"
    override val inboxJustNow      = "Most"
    override val inboxMinutesAgo: (Int) -> String = { n -> "${n} perce" }
    override val inboxHoursAgo: (Int) -> String = { n -> "${n} órája" }
    override val inboxYesterday    = "Tegnap"
    override val inboxDaysAgo: (Int) -> String = { n -> "${n} napja" }
}