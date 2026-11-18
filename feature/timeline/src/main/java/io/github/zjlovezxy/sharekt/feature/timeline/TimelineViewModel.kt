package io.github.zjlovezxy.sharekt.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.zjlovezxy.sharekt.core.data.WeiboRepository
import io.github.zjlovezxy.sharekt.core.model.Status
import io.github.zjlovezxy.sharekt.core.network.MapiException
import io.github.zjlovezxy.sharekt.core.network.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimelineUiState(
    val items: List<Status> = emptyList(),
    val page: Int = 0,
    val loadingMore: Boolean = false,
    val refreshing: Boolean = false,
    val endReached: Boolean = false,
    val needLogin: Boolean = false,
    val error: String = "",
)

class TimelineViewModel(private val repository: WeiboRepository) : ViewModel() {

    private val _state = MutableStateFlow(TimelineUiState())
    val state: StateFlow<TimelineUiState> = _state.asStateFlow()

    private var maxId: String = ""
    private var busy: Boolean = false

    fun firstLoad() {
        if (_state.value.items.isEmpty()) load(firstPage = true, fresh = false)
    }

    fun refresh() = load(firstPage = true, fresh = true)

    fun loadMore() {
        val current = _state.value
        if (current.items.isEmpty() || current.endReached || busy) return
        load(firstPage = false, fresh = false)
    }

    private fun load(firstPage: Boolean, fresh: Boolean) {
        if (busy) return
        busy = true
        val current = _state.value
        _state.value = current.copy(
            refreshing = fresh,
            loadingMore = !firstPage,
            error = "",
        )
        viewModelScope.launch {
            runCatching {
                repository.friendsTimeline(
                    fresh = fresh,
                    maxId = if (firstPage) "" else maxId,
                    page = if (firstPage) 1 else current.page + 1,
                )
            }.onSuccess { page ->
                maxId = page.maxId
                val merged = if (firstPage) page.items else current.items + page.items
                _state.value = _state.value.copy(
                    items = merged.distinctBy { it.id },
                    page = if (firstPage) 1 else current.page + 1,
                    endReached = page.maxId.isEmpty(),
                    refreshing = false,
                    loadingMore = false,
                    needLogin = false,
                    error = "",
                )
            }.onFailure { throwable ->
                _state.value = _state.value.copy(
                    refreshing = false,
                    loadingMore = false,
                    needLogin = (throwable as? MapiException)?.needLogin == true,
                    error = throwable.userMessage(),
                )
            }
            busy = false
        }
    }

    companion object {
        fun factory(repository: WeiboRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TimelineViewModel(repository) as T
        }
    }
}
