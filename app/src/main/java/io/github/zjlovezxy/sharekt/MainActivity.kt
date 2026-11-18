package io.github.zjlovezxy.sharekt

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.zjlovezxy.sharekt.core.auth.LoginActivity
import io.github.zjlovezxy.sharekt.core.ui.Navigator
import io.github.zjlovezxy.sharekt.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), Navigator {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // 重建 profile 目的地，让登录态立刻生效
            navController.navigate(
                R.id.profileFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.profileFragment, true).build(),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = destination.id in TAB_DESTINATIONS
        }
    }

    override fun openStatus(statusId: String) {
        navController.navigate(R.id.detailFragment, bundleOf("statusId" to statusId))
    }

    override fun openSettings() {
        navController.navigate(R.id.settingsFragment)
    }

    override fun openLogin() {
        loginLauncher.launch(LoginActivity.intent(this))
    }

    private companion object {
        val TAB_DESTINATIONS = setOf(
            R.id.timelineFragment,
            R.id.searchFragment,
            R.id.profileFragment,
        )
    }
}
