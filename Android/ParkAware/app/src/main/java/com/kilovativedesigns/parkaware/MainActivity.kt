package com.kilovativedesigns.parkaware

import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import com.kilovativedesigns.parkaware.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nav controller
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        // Top-level tabs (no Up arrow there)
        appBarConfig = AppBarConfiguration(
            setOf(R.id.tab_home, R.id.tab_sightings, R.id.tab_rules, R.id.tab_settings)
        )

        // Hook toolbar + bottom nav to NavController
        NavigationUI.setupWithNavController(binding.topAppBar, navController, appBarConfig)
        NavigationUI.setupWithNavController(binding.bottomNav, navController)

        // System bar icon contrast
        val isDark = (resources.configuration.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        // Insets: status bar -> app bar padding; gesture bar -> bottom nav padding
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePadding(top = sys.top)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(left = sys.left, right = sys.right, bottom = sys.bottom)
            insets
        }

        // Robust titles + show/hide toolbar on destinations
        navController.addOnDestinationChangedListener { _, destination, args ->
            val showToolbar = destination.id != R.id.tab_home
            binding.topAppBar.visibility = if (showToolbar) View.VISIBLE else View.GONE

            // Prefer arg "title" (e.g., detail screens), otherwise label, otherwise fallback by id
            val computedTitle =
                args?.getString("title")?.takeIf { it.isNotBlank() }
                    ?: destination.label?.toString()?.takeIf { it.isNotBlank() }
                    ?: when (destination.id) {
                        R.id.tab_sightings -> getString(R.string.recent_sightings)
                        R.id.tab_rules     -> getString(R.string.education)
                        R.id.tab_settings  -> getString(R.string.settings_title)
                        else               -> "" // Home shows no title
                    }

            binding.topAppBar.title = if (showToolbar) computedTitle else ""

            // (Optional) If you ever suspect theme issues, uncomment to force a visible color:
            // binding.topAppBar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }
}