package com.timememorial.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.timememorial.app.ui.add.NewMemorialDialog

class MainActivity : AppCompatActivity() {

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navHome: LinearLayout
    private lateinit var navCalendar: LinearLayout
    private lateinit var navAnniversary: LinearLayout
    private lateinit var navSettings: LinearLayout
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        setContentView(R.layout.activity_main)

        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        fabAdd = findViewById(R.id.fabAdd)
        navHome = findViewById(R.id.navHome)
        navCalendar = findViewById(R.id.navCalendar)
        navAnniversary = findViewById(R.id.navAnniversary)
        navSettings = findViewById(R.id.navSettings)

        setupNavigation()
    }

    private fun setupNavigation() {
        val navController = navHostFragment.navController

        navHome.setOnClickListener {
            navController.navigate(R.id.navHome)
            updateNavState("home")
        }
        navCalendar.setOnClickListener {
            navController.navigate(R.id.navCalendar)
            updateNavState("calendar")
        }
        navAnniversary.setOnClickListener {
            updateNavState("anniversary")
        }
        navSettings.setOnClickListener {
            navController.navigate(R.id.navSettings)
            updateNavState("settings")
        }
        fabAdd.setOnClickListener {
            NewMemorialDialog().show(supportFragmentManager, NewMemorialDialog.TAG)
        }
        updateNavState("home")
    }

    private fun updateNavState(selected: String) {
        data class NavItem(val iconRes: Int, val labelRes: Int, val key: String)
        val items = listOf(
            NavItem(R.id.iconHome, R.id.labelHome, "home"),
            NavItem(R.id.iconCalendar, R.id.labelCalendar, "calendar"),
            NavItem(R.id.iconHeart, R.id.labelAnniversary, "anniversary"),
            NavItem(R.id.iconSettings, R.id.labelSettings, "settings")
        )
        for (item in items) {
            val isActive = item.key == selected
            val icon = findViewById<ImageView>(item.iconRes)
            val label = findViewById<TextView>(item.labelRes)
            icon.isSelected = isActive
            label.isSelected = isActive
            val color = if (isActive) R.color.miui_brand else R.color.miui_text_tertiary
            icon.setColorFilter(getColor(color))
            label.setTextColor(getColor(color))
        }
    }
}
