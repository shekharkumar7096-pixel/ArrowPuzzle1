package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.ArrowPuzzleApp
import com.example.viewmodel.ArrowPuzzleViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ArrowPuzzleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.initializeAds(this)
        setContent {
            ArrowPuzzleApp(viewModel = viewModel)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.soundManager.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.soundManager.onResume()
    }
}
