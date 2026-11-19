package io.github.zjlovezxy.sharekt.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.zjlovezxy.sharekt.core.data.WeiboRepository
import io.github.zjlovezxy.sharekt.core.model.Comment
import io.github.zjlovezxy.sharekt.core.model.Status
import io.github.zjlovezxy.sharekt.core.network.userMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val status: Status? = null,
    val comments: List<Comment> = emptyList(),
    /** 评论游标（h5 的 max_id），0 表示还没有下一页。 */
    val commentsMaxId: Long = 0L,
    val endReached: Boolean = false,
    val refreshing: Boolean = false,
    val error: String = "",
)

class DetailViewModel(
    private val repository: WeiboRepository,
    private val statusId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    private var busy = false

    fun load(fresh: Boolean = false) {
        if (busy) return
        busy = true
        _state.value = _state.value.copy(refreshing = fresh, error = "")
        viewModelScope.launch {
            val statusJob = async { runCatching { repository.status(statusId, fresh) } }
            val commentsJob = async { runCatching { repository.comments(statusId) } }

            val statusResult = statusJob.await()
            val commentsResult = commentsJob.await()

            val error = statusResult.exceptionOrNull()?.userMessage()
                ?: commentsResult.exceptionOrNull()?.userMessage()
                ?: ""

            val page = commentsResult.getOrNull()
            _state.value = _state.value.copy(
                status = statusResult.getOrNull() ?: _state.value.status,
                comments = page?.items ?: _state.value.comments,
                commentsMaxId = page?.maxId?.toLongOrNull() ?: _state.value.commentsMaxId,
                endReached = page != null && page.maxId.isEmpty(),
                refreshing = false,
                error = error,
            )
            busy = false
        }
    }

    fun loadMoreComments() {
        val current = _state.value
        if (busy || current.endReached || current.comments.isEmpty() || current.commentsMaxId <= 0L) return
        busy = true
        viewModelScope.launch {
            runCatching { repository.comments(statusId, maxId = current.commentsMaxId) }
                .onSuccess { page ->
                    _state.value = _state.value.copy(
                        comments = (current.comments + page.items).distinctBy { it.id },
                        commentsMaxId = page.maxId.toLongOrNull() ?: 0L,
                        endReached = page.maxId.isEmpty(),
                    )
                }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage()) }
            busy = false
        }
    }

    companion object {
        fun factory(repository: WeiboRepository, statusId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DetailViewModel(repository, statusId) as T
        }
    }
}
