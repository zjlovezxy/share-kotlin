package io.github.zjlovezxy.sharekt.feature.search

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

data class SearchUiState(
    val query: String = "",
    val items: List<Status> = emptyList(),
    val page: Int = 0,
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val needLogin: Boolean = false,
    val error: String = "",
    val searched: Boolean = false,
)

class SearchViewModel(private val repository: WeiboRepository) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var busy = false

    fun search(query: String) {
        val keyword = query.trim()
        if (keyword.isEmpty()) return
        busy = true
        _state.value = _state.value.copy(query = keyword, loading = true, error = "", needLogin = false)
        viewModelScope.launch {
            runCatching { repository.search(keyword, page = 1) }
                .onSuccess { page ->
                    _state.value = _state.value.copy(
                        items = page.items,
                        page = 1,
                        loading = false,
                        searched = true,
                        endReached = page.maxId.isEmpty(),
                    )
                }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(
                        items = emptyList(),
                        page = 0,
                        loading = false,
                        searched = true,
                        needLogin = (throwable as? MapiException)?.needLogin == true,
                        error = throwable.userMessage(),
                    )
                }
            busy = false
        }
    }

    fun loadMore() {
        val current = _state.value
        if (busy || current.query.isEmpty() || current.endReached || current.items.isEmpty()) return
        busy = true
        viewModelScope.launch {
            runCatching { repository.search(current.query, page = current.page + 1) }
                .onSuccess { page ->
                    _state.value = _state.value.copy(
                        items = current.items + page.items,
                        page = current.page + 1,
                        endReached = page.maxId.isEmpty(),
                    )
                }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage()) }
            busy = false
        }
    }

    companion object {
        fun factory(repository: WeiboRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SearchViewModel(repository) as T
        }
    }
}
