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
import androidx.core.view.WindowCompat
import com.example.thryftapp.theme.FluidBottomNavigationTheme

class NavHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navhost) //set layout

        //load home fragment
        val fragment = HomeFragment()
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment) //load into container
        }

        //setup compose view for fluid nav barr
        findViewById<ComposeView>(R.id.compose_view).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed) //cleanup strategy
            setContent {
                FluidBottomNavigationTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen() //fluid nav bar composable
                    }
                }
            }
        }
    }
}
