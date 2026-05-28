package com.domedemok.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domedemok.travelplanner.data.model.InboxItem
import com.domedemok.travelplanner.data.repository.ChatRepository
import com.domedemok.travelplanner.data.repository.ExpenseRepository
import com.domedemok.travelplanner.data.repository.PlaceRepository
import com.domedemok.travelplanner.data.repository.TripRepository
import com.domedemok.travelplanner.util.getCurrentTimestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InboxUiState(
    val items: List<InboxItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class InboxViewModel(
    private val tripRepository: TripRepository,
    private val chatRepository: ChatRepository,
    private val placeRepository: PlaceRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    // One active subscriber job per trip per type.
    // Before starting a new job for a tripId, check if one is already active.
    // This prevents duplicate collectors when getTripsFlow() re-emits (e.g. on any
    // field change in the trip document), which was the root cause of notifications
    // disappearing and state thrashing.
    private val chatJobs    = mutableMapOf<String, Job>()
    private val placeJobs   = mutableMapOf<String, Job>()
    private val expenseJobs = mutableMapOf<String, Job>()

    // ── In-memory notification history ────────────────────────────────────────
    private val placeHistory         = mutableMapOf<String, MutableMap<String, InboxItem.PlaceNotification>>()
    private val deletedPlaceNotifs   = mutableMapOf<String, MutableList<InboxItem.PlaceNotification>>()
    private val expenseHistory       = mutableMapOf<String, MutableMap<String, InboxItem.ExpenseNotification>>()
    private val deletedExpenseNotifs = mutableMapOf<String, MutableList<InboxItem.ExpenseNotification>>()
    // Snapshot of tripMembers.keys per trip — diffed across emissions to detect joins/leaves.
    private val membersHistory       = mutableMapOf<String, Set<String>>()
    private val memberNotifs         = mutableMapOf<String, InboxItem.MemberNotification>()
    private val dismissedIds         = mutableSetOf<String>()

    init {
        observeActivity()
    }

    private fun observeActivity() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val sevenDaysAgo = getCurrentTimestamp() - 7L * 24 * 3600 * 1000
            // tripRepository.currentUserId reads auth.currentUser?.uid synchronously — no Firestore
            // call, never fails silently. The previous authRepository.getCurrentUser() made a
            // network call and returned null on any error, causing the entire ViewModel to exit.
            val userId = tripRepository.currentUserId
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            tripRepository.getTripsFlow()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { trips ->
                    _uiState.update { it.copy(isLoading = true) }

                    val activeTripIds = trips
                        .sortedByDescending { maxOf(it.updatedAt, it.lastOpenedAt) }
                        .take(5)
                    val activeIds = activeTripIds.map { it.id }.toSet()

                    // Cancel subscriptions for trips that are no longer in the active set
                    val staleIds = (chatJobs.keys + placeJobs.keys + expenseJobs.keys) - activeIds
                    for (id in staleIds) {
                        chatJobs.remove(id)?.cancel()
                        placeJobs.remove(id)?.cancel()
                        expenseJobs.remove(id)?.cancel()
                    }

                    for (trip in activeTripIds) {
                        val now = getCurrentTimestamp()

                        // ── Detect member joins / leaves via tripMembers.keys ───────
                        // Diff against the owner-excluded set: the owner is always present
                        // and we don't want to fire a "join" notification for them on the
                        // very first emission of a trip they created.
                        val curMembers  = trip.tripMembers.keys - trip.createdBy
                        val prevMembers = membersHistory[trip.id]

                        if (prevMembers == null) {
                            // First encounter: if current user is a non-owner member → self-join notification
                            if (userId in curMembers) {
                                val notifId = "self_join_${trip.id}"
                                memberNotifs.getOrPut(notifId) {
                                    InboxItem.MemberNotification(
                                        id         = notifId,
                                        tripId     = trip.id,
                                        tripName   = trip.name,
                                        memberName = trip.displayNameFor(userId) ?: "",
                                        isJoined   = true,
                                        isSelf     = true,
                                        timestamp  = now,
                                    )
                                }
                            }
                        } else if (prevMembers != curMembers) {
                            for (uid in curMembers - prevMembers) {
                                val notifId = "member_join_${trip.id}_$uid"
                                memberNotifs.getOrPut(notifId) {
                                    InboxItem.MemberNotification(
                                        id         = notifId,
                                        tripId     = trip.id,
                                        tripName   = trip.name,
                                        memberName = trip.displayNameFor(uid) ?: uid,
                                        isJoined   = true,
                                        isSelf     = (uid == userId),
                                        timestamp  = now,
                                    )
                                }
                            }
                            for (uid in prevMembers - curMembers) {
                                val notifId = "member_leave_${trip.id}_$uid"
                                memberNotifs.getOrPut(notifId) {
                                    InboxItem.MemberNotification(
                                        id         = notifId,
                                        tripId     = trip.id,
                                        tripName   = trip.name,
                                        // After leaving, the name lives in formerMembers — displayNameFor handles both.
                                        memberName = trip.displayNameFor(uid) ?: uid,
                                        isJoined   = false,
                                        isSelf     = (uid == userId),
                                        timestamp  = now,
                                    )
                                }
                            }
                        }
                        membersHistory[trip.id] = curMembers

                        // ── Chat messages ─────────────────────────────────────────
                        if (chatJobs[trip.id]?.isActive != true) {
                            runCatching { chatRepository.getMessagesFlow(trip.id) }
                                .onSuccess { flow ->
                                    chatJobs[trip.id] = viewModelScope.launch {
                                        flow.catch {}
                                            .collect { messages ->
                                                val chatItems = messages
                                                    .filter {
                                                        it.senderId.isNotBlank() &&
                                                        it.senderId != userId &&
                                                        it.timestamp > sevenDaysAgo
                                                    }
                                                    .map { msg ->
                                                        InboxItem.ChatNotification(
                                                            id             = msg.id.ifEmpty { "${trip.id}_${msg.timestamp}" },
                                                            tripId         = trip.id,
                                                            tripName       = trip.name,
                                                            senderName     = msg.senderName,
                                                            messagePreview = msg.text.take(80),
                                                            timestamp      = msg.timestamp,
                                                        )
                                                    }
                                                    .filter { it.id !in dismissedIds }
                                                _uiState.update { state ->
                                                    val without = state.items.filter { item ->
                                                        !(item is InboxItem.ChatNotification && item.tripId == trip.id)
                                                    }
                                                    state.copy(
                                                        items = (without + chatItems)
                                                            .distinctBy { it.id }
                                                            .sortedByDescending { it.timestamp },
                                                        isLoading = false,
                                                    )
                                                }
                                            }
                                    }
                                }
                        }

                        // ── Places — real-time with deletion tracking ─────────────
                        if (placeJobs[trip.id]?.isActive != true) {
                            runCatching { placeRepository.getPlacesForTripFlow(trip.id) }
                                .onSuccess { flow ->
                                    placeJobs[trip.id] = viewModelScope.launch {
                                        flow.catch {}
                                            .collect { places ->
                                                val emitTime = getCurrentTimestamp()
                                                val liveItems = places
                                                    .filter {
                                                        it.addedAt > sevenDaysAgo &&
                                                        it.addedBy.isNotBlank() &&
                                                        it.addedBy != userId
                                                    }
                                                    .map { place ->
                                                        InboxItem.PlaceNotification(
                                                            id        = place.id,
                                                            tripId    = trip.id,
                                                            tripName  = trip.name,
                                                            placeName = place.name,
                                                            category  = place.category,
                                                            timestamp = place.addedAt,
                                                            isDeleted = false,
                                                        )
                                                    }

                                                val tripHist    = placeHistory.getOrPut(trip.id) { mutableMapOf() }
                                                val tripDeleted = deletedPlaceNotifs.getOrPut(trip.id) { mutableListOf() }
                                                val liveIds     = liveItems.map { it.id }.toSet()

                                                for ((oldId, oldNotif) in tripHist) {
                                                    if (oldId !in liveIds) {
                                                        val deletedId = "del_$oldId"
                                                        if (tripDeleted.none { it.id == deletedId }) {
                                                            tripDeleted += oldNotif.copy(
                                                                id        = deletedId,
                                                                timestamp = emitTime,
                                                                isDeleted = true,
                                                            )
                                                        }
                                                    }
                                                }
                                                liveItems.forEach { tripHist[it.id] = it }

                                                // Use tripHist (the cumulative add-history), NOT liveItems —
                                                // this way the original "added" notification persists even
                                                // after the underlying place is deleted, and shows alongside
                                                // the new "removed" notification.
                                                val allPlaceItems = (tripHist.values + tripDeleted)
                                                    .filter { it.id !in dismissedIds }

                                                _uiState.update { state ->
                                                    val without = state.items.filter { item ->
                                                        !(item is InboxItem.PlaceNotification && item.tripId == trip.id)
                                                    }
                                                    state.copy(
                                                        items = (without + allPlaceItems)
                                                            .distinctBy { it.id }
                                                            .sortedByDescending { it.timestamp },
                                                        isLoading = false,
                                                    )
                                                }
                                            }
                                    }
                                }
                        }

                        // ── Expenses — real-time with deletion tracking ────────────
                        if (expenseJobs[trip.id]?.isActive != true) {
                            runCatching { expenseRepository.getExpensesFlow(trip.id) }
                                .onSuccess { flow ->
                                    expenseJobs[trip.id] = viewModelScope.launch {
                                        flow.catch {}
                                            .collect { expenses ->
                                                val emitTime = getCurrentTimestamp()
                                                val liveItems = expenses
                                                    .filter { expense ->
                                                        expense.date > sevenDaysAgo &&
                                                        expense.paidBy.isNotBlank() &&
                                                        expense.paidBy != userId &&
                                                        expense.debts.containsKey(userId)
                                                    }
                                                    .map { expense ->
                                                        val paidByName = trip.displayNameFor(expense.paidBy)
                                                            ?: expense.paidBy
                                                        InboxItem.ExpenseNotification(
                                                            id         = expense.id,
                                                            tripId     = trip.id,
                                                            tripName   = trip.name,
                                                            title      = expense.title,
                                                            amount     = expense.amount,
                                                            paidByName = paidByName,
                                                            timestamp  = expense.date,
                                                            isDeleted  = false,
                                                        )
                                                    }

                                                val tripHist    = expenseHistory.getOrPut(trip.id) { mutableMapOf() }
                                                val tripDeleted = deletedExpenseNotifs.getOrPut(trip.id) { mutableListOf() }
                                                val liveIds     = liveItems.map { it.id }.toSet()

                                                for ((oldId, oldNotif) in tripHist) {
                                                    if (oldId !in liveIds) {
                                                        val deletedId = "del_$oldId"
                                                        if (tripDeleted.none { it.id == deletedId }) {
                                                            tripDeleted += oldNotif.copy(
                                                                id        = deletedId,
                                                                timestamp = emitTime,
                                                                isDeleted = true,
                                                            )
                                                        }
                                                    }
                                                }
                                                liveItems.forEach { tripHist[it.id] = it }

                                                // Same persistence rule as places — keep the original
                                                // "added" notification visible after the expense is removed.
                                                val allExpenseItems = (tripHist.values + tripDeleted)
                                                    .filter { it.id !in dismissedIds }

                                                _uiState.update { state ->
                                                    val without = state.items.filter { item ->
                                                        !(item is InboxItem.ExpenseNotification && item.tripId == trip.id)
                                                    }
                                                    state.copy(
                                                        items = (without + allExpenseItems)
                                                            .sortedByDescending { it.timestamp },
                                                        isLoading = false,
                                                    )
                                                }
                                            }
                                    }
                                }
                        }
                    }

                    // Member notifications are generated synchronously above; merge into state
                    val visibleMemberNotifs = memberNotifs.values
                        .filter { it.id !in dismissedIds }
                    _uiState.update { state ->
                        val withoutMember = state.items.filterNot { it is InboxItem.MemberNotification }
                        state.copy(
                            items = (withoutMember + visibleMemberNotifs)
                                .distinctBy { it.id }
                                .sortedByDescending { it.timestamp },
                            isLoading = false,
                        )
                    }
                }
        }
    }

    /** Permanently hides one item (in-memory; resets on app restart). */
    fun dismissItem(id: String) {
        dismissedIds.add(id)
        _uiState.update { state -> state.copy(items = state.items.filter { it.id != id }) }
    }

    /** Dismisses every currently visible notification at once. */
    fun clearAll() {
        dismissedIds.addAll(_uiState.value.items.map { it.id })
        _uiState.update { it.copy(items = emptyList()) }
    }

    fun refresh() {
        _uiState.update { it.copy(error = null) }
        observeActivity()
    }
}
