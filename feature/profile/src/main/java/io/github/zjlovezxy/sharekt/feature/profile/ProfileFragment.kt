package io.github.zjlovezxy.sharekt.feature.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.github.zjlovezxy.sharekt.core.auth.AuthProvider
import io.github.zjlovezxy.sharekt.core.ui.Navigator
import io.github.zjlovezxy.sharekt.core.ui.loadUrl
import io.github.zjlovezxy.sharekt.feature.profile.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val auth get() = (requireActivity().application as AuthProvider).authRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.login.setOnClickListener { (activity as? Navigator)?.openLogin() }
        binding.logout.setOnClickListener {
            auth.logout()
            render()
        }
        binding.settings.setOnClickListener { (activity as? Navigator)?.openSettings() }
        render()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) render()
    }

    private fun render() {
        val session = auth.current()
        val loggedIn = session.isLoggedIn
        binding.name.text = if (loggedIn) {
            session.screenName.ifEmpty { "已登录（uid ${session.uid}）" }
        } else {
            "未登录"
        }
        binding.uid.text = when {
            !loggedIn -> "登录后可同步关注流、发微博、看私信"
            session.uid.isNotEmpty() -> "uid ${session.uid}"
            else -> ""
        }
        binding.avatar.loadUrl(session.avatarUrl)
        binding.login.isVisible = !loggedIn
        binding.logout.isVisible = loggedIn
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
