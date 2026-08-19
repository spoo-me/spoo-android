package me.spoo.android.ui.screens.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spoo.android.SpooApp
import me.spoo.android.data.CreateLinkRequest
import me.spoo.android.data.LinksRepository
import me.spoo.android.data.SpooLink

enum class LinkSort { Recent, Clicks }

sealed interface CreateState {
    data object Idle : CreateState
    data object Submitting : CreateState
    data class Done(val link: SpooLink) : CreateState
    data class Failed(val reason: String) : CreateState
}

class LinksViewModel(
    private val repository: LinksRepository = SpooApp.graph.linksRepository,
) : ViewModel() {

    init {
        viewModelScope.launch { runCatching { repository.refresh() } }
    }

    val query = MutableStateFlow("")
    val sort = MutableStateFlow(LinkSort.Recent)

    val links: StateFlow<List<SpooLink>> =
        combine(repository.links, query, sort) { all, q, sort ->
            val filtered = if (q.isBlank()) all else all.filter {
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

    fun create(request: CreateLinkRequest) {
        _createState.value = CreateState.Submitting
        viewModelScope.launch {
            _createState.value = try {
                CreateState.Done(repository.create(request))
            } catch (e: Exception) {
                CreateState.Failed(e.message ?: "Could not shorten this link")
            }
        }
    }

    fun resetCreate() {
        _createState.value = CreateState.Idle
    }
}
