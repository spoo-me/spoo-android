package me.spoo.android.ui.screens.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spoo.android.SpooApp
import me.spoo.android.data.CreateLinkRequest
import me.spoo.android.data.EmojiCatalog
import me.spoo.android.data.FriendlyError
import me.spoo.android.data.LinkEdit
import me.spoo.android.data.friendlyError
import me.spoo.android.data.LinksFilter
import me.spoo.android.data.LinksRepository
import me.spoo.android.data.SpooLink

enum class LinkSort { Recent, Clicks }

/** Floor for pull-to-refresh so the hold phase is visible (see refresh). */
const val MIN_REFRESH_MS = 650L

sealed interface CreateState {
    data object Idle : CreateState
    data object Submitting : CreateState
    data class Done(val link: SpooLink) : CreateState
    data class Failed(val error: FriendlyError) : CreateState
}

sealed interface EditState {
    data object Idle : EditState
    data object Submitting : EditState
    data object Done : EditState
    data class Failed(val error: FriendlyError) : EditState
}

class LinksViewModel(
    private val repository: LinksRepository = SpooApp.graph.linksRepository,
) : ViewModel() {

    init {
        viewModelScope.launch { runCatching { repository.refresh() } }
    }

    /** Pull-to-refresh. */
    val refreshing = MutableStateFlow(false)

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            val started = System.currentTimeMillis()
            runCatching { repository.refresh() }
                .onFailure { actionMessage.value = "Couldn't refresh" }
            // A fast backend finishes before the finger lifts and the pull
            // reads as a dead snap-back: hold the spinner long enough to
            // look like work.
            delay((MIN_REFRESH_MS - (System.currentTimeMillis() - started)).coerceAtLeast(0))
            refreshing.value = false
        }
    }

    val query = MutableStateFlow("")
    val sort = MutableStateFlow(LinkSort.Recent)
    val filter = MutableStateFlow(LinksFilter())

    val links: StateFlow<List<SpooLink>> =
        combine(repository.links, query, sort, filter) { all, q, sort, filter ->
            val byFilter = all.filter(filter::matches)
            val filtered = if (q.isBlank()) byFilter else byFilter.filter {
                it.shortCode.contains(q, ignoreCase = true) ||
                    it.originalUrl.contains(q, ignoreCase = true)
            }
            when (sort) {
                LinkSort.Recent -> filtered
                LinkSort.Clicks -> filtered.sortedByDescending { it.totalClicks }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _createState = MutableStateFlow<CreateState>(CreateState.Idle)
    val createState: StateFlow<CreateState> = _createState

    /** Accepted emoji-alias catalogue; fetched once, on first demand. */
    val emojiCatalog = MutableStateFlow<EmojiCatalog?>(null)
    private var emojiCatalogRequested = false

    fun ensureEmojiCatalog() {
        if (emojiCatalogRequested) return
        emojiCatalogRequested = true
        viewModelScope.launch {
            runCatching { repository.emojiCatalog() }
                .onSuccess { emojiCatalog.value = it }
                .onFailure { emojiCatalogRequested = false } // retry next open
        }
    }

    fun create(request: CreateLinkRequest) {
        _createState.value = CreateState.Submitting
        viewModelScope.launch {
            _createState.value = try {
                CreateState.Done(repository.create(request))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CreateState.Failed(friendlyError(e, "Could not shorten this link."))
            }
        }
    }

    fun resetCreate() {
        _createState.value = CreateState.Idle
    }

    private val _editState = MutableStateFlow<EditState>(EditState.Idle)
    val editState: StateFlow<EditState> = _editState

    fun updateLink(id: String, edit: LinkEdit) {
        _editState.value = EditState.Submitting
        viewModelScope.launch {
            _editState.value = try {
                repository.update(id, edit)
                EditState.Done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EditState.Failed(friendlyError(e, "Could not save changes."))
            }
        }
    }

    fun resetEdit() {
        _editState.value = EditState.Idle
    }

    /** One-shot snackbar line for fire-and-forget actions. */
    val actionMessage = MutableStateFlow<String?>(null)

    fun deleteLink(id: String) {
        viewModelScope.launch {
            try {
                repository.delete(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                actionMessage.value = friendlyError(e, "Could not delete the link.").message
            }
        }
    }

    /** Long-press multi-select. Non-empty set = selection mode. */
    val selection = MutableStateFlow<Set<String>>(emptySet())

    fun toggleSelected(id: String) {
        selection.value = if (id in selection.value) selection.value - id else selection.value + id
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun deleteSelected() = bulk("deleted") { repository.bulkDelete(it) }

    fun setSelectedStatus(active: Boolean) =
        bulk(if (active) "enabled" else "disabled") { repository.bulkSetStatus(it, active) }

    fun setSelectedExpiry(millis: Long?) =
        bulk(if (millis == null) "expiry cleared" else "set to expire") {
            repository.bulkSetExpiry(it, millis)
        }

    private fun bulk(pastTense: String, op: suspend (List<String>) -> Unit) {
        val ids = selection.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                op(ids)
                selection.value = emptySet()
                actionMessage.value = "${ids.size} ${if (ids.size == 1) "link" else "links"} $pastTense"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                actionMessage.value = friendlyError(e, "Bulk action failed.").message
            }
        }
    }
}
