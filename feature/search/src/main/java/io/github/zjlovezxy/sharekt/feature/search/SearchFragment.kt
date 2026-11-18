package io.github.zjlovezxy.sharekt.feature.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.zjlovezxy.sharekt.core.data.RepositoryProvider
import io.github.zjlovezxy.sharekt.core.ui.Navigator
import io.github.zjlovezxy.sharekt.core.ui.StatusAdapter
import io.github.zjlovezxy.sharekt.feature.search.databinding.FragmentSearchBinding
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: SearchViewModel by viewModels {
        val app = requireActivity().application as RepositoryProvider
        SearchViewModel.factory(app.weiboRepository)
    }

    private val adapter by lazy {
        StatusAdapter { status -> (activity as? Navigator)?.openStatus(status.id) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        val submit = {
            viewModel.search(binding.query.text.toString())
            binding.query.clearFocus()
        }
        binding.search.setOnClickListener { submit() }
        binding.query.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { submit(); true } else false
        }

        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val manager = rv.layoutManager as? LinearLayoutManager ?: return
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - 3) viewModel.loadMore()
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: SearchUiState) {
        adapter.submitList(state.items)
        binding.hint.isVisible = state.items.isEmpty()
        binding.hint.text = when {
            state.loading -> "搜索中…"
            state.needLogin -> "搜索需要登录"
            state.error.isNotEmpty() -> state.error
            state.searched -> "没有找到相关微博"
            else -> "输入关键词开始搜索"
        }
    }

    override fun onDestroyView() {
        binding.list.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
