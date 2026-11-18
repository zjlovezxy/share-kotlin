package io.github.zjlovezxy.sharekt.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import io.github.zjlovezxy.sharekt.core.auth.AuthProvider
import io.github.zjlovezxy.sharekt.core.data.RepositoryProvider
import io.github.zjlovezxy.sharekt.feature.settings.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.clearCache.setOnClickListener {
            (requireActivity().application as RepositoryProvider).weiboRepository.clearCache()
            Snackbar.make(binding.root, "缓存已清除", Snackbar.LENGTH_SHORT).show()
        }
        binding.logout.setOnClickListener {
            (requireActivity().application as AuthProvider).authRepository.logout()
            Snackbar.make(binding.root, "已退出登录", Snackbar.LENGTH_SHORT).show()
        }
        val version = runCatching {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        }.getOrNull()
        binding.version.text = "ShareKt $version\n非官方客户端，仅用于个人学习"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
