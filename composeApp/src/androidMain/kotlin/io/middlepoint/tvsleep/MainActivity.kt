package io.middlepoint.tvsleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import io.middlepoint.tvsleep.ui.screens.Connecting
import io.middlepoint.tvsleep.ui.screens.ConnectingScreen
import io.middlepoint.tvsleep.ui.screens.Debug
import io.middlepoint.tvsleep.ui.screens.DebugScreen
import io.middlepoint.tvsleep.ui.screens.SetupADB
import io.middlepoint.tvsleep.ui.screens.SetupScreen
import io.middlepoint.tvsleep.ui.screens.TimeSelection
import io.middlepoint.tvsleep.ui.screens.TimeSelectionScreen
import io.middlepoint.tvsleep.ui.screens.Timer
import io.middlepoint.tvsleep.ui.screens.TimerScreen
import io.middlepoint.tvsleep.ui.screens.mapToScreen
import io.middlepoint.tvsleep.ui.theme.TVsleepTheme

@Suppress("ktlint:standard:no-consecutive-comments")
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel by viewModels<MainActivityViewModel>()
            val homeState by viewModel.homeState.collectAsState()
            val navController = rememberNavController()
            val isDebug = remember { BuildConfig.DEBUG }

            TVsleepTheme {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .safeContentPadding(),
                    colors =
                        SurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Connecting,
                        enterTransition = { fadeIn(spring(stiffness = Spring.StiffnessMedium)) },
                        exitTransition = { fadeOut(spring(stiffness = Spring.StiffnessMedium)) },
                    ) {
                        composable<Connecting> {
                            ConnectingScreen {
                                viewModel.startADBServer()
                            }
                        }

                        composable<TimeSelection> {
                            TimeSelectionScreen {
                                viewModel.onTimeSelected(it)
                            }
                        }
                        composable<Timer> {
                            TimerScreen()
                        }
                        composable<SetupADB> {
                            SetupScreen()
                        }

                        composable<Debug> {
                            DebugScreen(navController, viewModel)
                        }
                    }

                    if (isDebug) {
                        val status by viewModel.outputText.collectAsState("Waiting Status")
                        Text(
                            status,
                            modifier = Modifier.align(Alignment.TopStart),
                        )
                    }

                    BackHandler {
                        finish()
                    }

                    LaunchedEffect(homeState) {
                        homeState.let { state ->
                            navController.navigate(
                                route = state.mapToScreen(),
                                navOptions =
                                    navOptions {
                                        popUpTo(Connecting) { inclusive = true }
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
