package io.github.zjlovezxy.sharekt.feature.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.zjlovezxy.sharekt.core.data.RepositoryProvider
import io.github.zjlovezxy.sharekt.feature.detail.databinding.FragmentDetailBinding
import kotlinx.coroutines.launch

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val statusId: String by lazy { requireArguments().getString(ARG_STATUS_ID).orEmpty() }

    private val viewModel: DetailViewModel by viewModels {
        val app = requireActivity().application as RepositoryProvider
        DetailViewModel.factory(app.weiboRepository, statusId)
    }

    private val adapter = DetailAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.refresh.setOnRefreshListener { viewModel.load(fresh = true) }
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val manager = rv.layoutManager as? LinearLayoutManager ?: return
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.loadMoreComments()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }

        viewModel.load()
    }

    private fun render(state: DetailUiState) {
        val items = buildList<Any> {
            state.status?.let { add(it) }
            addAll(state.comments)
        }
        adapter.submitList(items)
        binding.refresh.isRefreshing = state.refreshing
        binding.hint.text = when {
            items.isNotEmpty() -> ""
            state.error.isNotEmpty() -> "${state.error}\n\n下拉重试"
            else -> "正在加载…"
        }
        binding.hint.visibility = if (binding.hint.text.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        binding.list.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_STATUS_ID = "statusId"
    }
}
