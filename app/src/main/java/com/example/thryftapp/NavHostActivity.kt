package com.example.thryftapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.thryftapp.databinding.ActivityNavhostBinding
import androidx.compose.ui.platform.ComposeView
import com.example.thryftapp.theme.FluidBottomNavigationTheme

class NavHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navhost)

        // 🧠 Load your HomeFragment
        val fragment = HomeFragment()
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
        }

        // 🎨 Hook into the ComposeView inside the layout
        findViewById<ComposeView>(R.id.compose_view).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FluidBottomNavigationTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen() // your fluid nav bar composable
                    }
                }
            }
        }
    }
}
