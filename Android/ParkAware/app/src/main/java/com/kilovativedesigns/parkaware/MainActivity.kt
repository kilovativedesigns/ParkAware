package com.kilovativedesigns.parkaware

import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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

        // Nav setup
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        // Top-level destinations (no up arrow)
        appBarConfig = AppBarConfiguration(
            setOf(R.id.tab_home, R.id.tab_sightings, R.id.tab_rules, R.id.tab_settings)
        )

        // Wire toolbar to NavController (for navigation/back arrow)
        NavigationUI.setupWithNavController(binding.topAppBar, navController, appBarConfig)

        // System bar icon contrast
        val isDark = (resources.configuration.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        // ----- Window Insets handling -----
        val navHostView = findViewById<View>(R.id.nav_host_fragment)

        var lastTop = 0
        var lastLeft = 0
        var lastRight = 0
        var lastBottom = 0

        // Toolbar gets TOP inset when visible
        ViewCompat.setOnApplyWindowInsetsListener(binding.topAppBar) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            lastTop = bars.top; lastLeft = bars.left; lastRight = bars.right; lastBottom = bars.bottom
            if (binding.topAppBar.visibility == View.VISIBLE) {
                v.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            } else {
                v.setPadding(0, 0, 0, 0)
            }
            insets
        }

        // Content gets side/bottom always; gets TOP only when toolbar is hidden
        ViewCompat.setOnApplyWindowInsetsListener(navHostView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val giveTopToContent = binding.topAppBar.visibility != View.VISIBLE
            v.updatePadding(
                left = bars.left,
                right = bars.right,
                bottom = bars.bottom,
                top = if (giveTopToContent) bars.top else 0
            )
            insets
        }
        // ----------------------------------

        // Show/hide toolbar on destination changes + set the title explicitly
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val showToolbar = destination.id != R.id.tab_home
            binding.topAppBar.visibility = if (showToolbar) View.VISIBLE else View.GONE

            // Force title so it never shows blank (handles labels like "{title}")
            val explicitTitle =
                arguments?.getString("title")  // for educationDetailFragment
                    ?: destination.label?.toString()
                    ?: ""

            // Only set a title when we’re showing the bar
            binding.topAppBar.title = if (showToolbar) explicitTitle else ""

            // Reapply insets to the view that should own the top inset
            if (showToolbar) {
                binding.topAppBar.updatePadding(top = lastTop, left = lastLeft, right = lastRight)
                navHostView.updatePadding(top = 0, left = lastLeft, right = lastRight, bottom = lastBottom)
            } else {
                binding.topAppBar.setPadding(0, 0, 0, 0)
                navHostView.updatePadding(top = lastTop, left = lastLeft, right = lastRight, bottom = lastBottom)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }
}