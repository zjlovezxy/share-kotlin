package io.github.zjlovezxy.sharekt.feature.timeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.zjlovezxy.sharekt.core.data.RepositoryProvider
import io.github.zjlovezxy.sharekt.core.ui.Navigator
import io.github.zjlovezxy.sharekt.core.ui.StatusAdapter
import io.github.zjlovezxy.sharekt.feature.timeline.databinding.FragmentTimelineBinding
import kotlinx.coroutines.launch

class TimelineFragment : Fragment() {

    private var _binding: FragmentTimelineBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: TimelineViewModel by viewModels {
        val app = requireActivity().application as RepositoryProvider
        TimelineViewModel.factory(app.weiboRepository)
    }

    private val adapter by lazy {
        StatusAdapter { status -> (activity as? Navigator)?.openStatus(status.id) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.refresh.setOnRefreshListener { viewModel.refresh() }
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.loadMore()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }

        viewModel.firstLoad()
    }

    private fun render(state: TimelineUiState) {
        adapter.submitList(state.items)
        binding.refresh.isRefreshing = state.refreshing

        // 只有列表空的时候才用居中提示，否则会盖在内容上
        binding.hint.text = when {
            state.items.isNotEmpty() -> ""
            state.needLogin -> "登录后查看关注的时间线\n（到「我的」→ 登录）"
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
}
