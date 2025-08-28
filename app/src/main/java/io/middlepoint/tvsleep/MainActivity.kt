package io.middlepoint.tvsleep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import co.touchlab.kermit.Logger
import io.middlepoint.tvsleep.ui.components.CircularProgressWithThumb
import io.middlepoint.tvsleep.ui.screens.Connecting
import io.middlepoint.tvsleep.ui.screens.ConnectingScreen
import io.middlepoint.tvsleep.ui.screens.Debug
import io.middlepoint.tvsleep.ui.screens.DebugScreen
import io.middlepoint.tvsleep.ui.screens.Home
import io.middlepoint.tvsleep.ui.screens.HomeScreen
import io.middlepoint.tvsleep.ui.screens.Setup
import io.middlepoint.tvsleep.ui.screens.SetupScreen
import io.middlepoint.tvsleep.ui.screens.Start
import io.middlepoint.tvsleep.ui.screens.StartScreen
import io.middlepoint.tvsleep.ui.screens.mapToScreen
import io.middlepoint.tvsleep.ui.theme.Purple40
import io.middlepoint.tvsleep.ui.theme.TVsleepTheme
import io.middlepoint.tvsleep.utils.TimerState
import io.middlepoint.tvsleep.utils.calculateFontSize
import kotlinx.coroutines.delay

@Suppress("ktlint:standard:no-consecutive-comments")
class MainActivity : ComponentActivity() {
    private val logger = Logger.withTag("MainActivity")

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel by viewModels<MainActivityViewModel>()
            val homeState by viewModel.homeState.observeAsState()
            val navController = rememberNavController()

            TVsleepTheme {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .safeContentPadding(),
                    colors = SurfaceDefaults.colors(containerColor = Purple40),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Start,
                        enterTransition = { fadeIn(spring(stiffness = Spring.StiffnessMedium)) },
                        exitTransition = { fadeOut(spring(stiffness = Spring.StiffnessMedium)) },
                    ) {
                        composable<Start> {
                            StartScreen(viewModel)
                        }
                        composable<Home> {
                            HomeScreen()
                        }
                        composable<Setup> {
                            SetupScreen(viewModel)
                        }
                        composable<Connecting> {
                            ConnectingScreen()
                        }
                        composable<Debug> {
                            DebugScreen(navController, viewModel)
                        }
                    }

                    val backStackEntry by navController.currentBackStackEntryAsState()

                    BackHandler {
                        finish()
                    }

                    LaunchedEffect(homeState) {
                        homeState?.let { state ->
                            navController.navigate(
                                route = state.mapToScreen(),
                                navOptions =
                                    navOptions {
                                        popUpTo(Start) { inclusive = true }
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
