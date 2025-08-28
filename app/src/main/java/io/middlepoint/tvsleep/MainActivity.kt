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
                            HomeScreen(viewModel)
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

//                    BackHandler {
//                        logger.d { "BackHandler: $backStackEntry" }
// //                        if (backStackEntry?.destination?.route == Home::class.qualifiedName) {
// //                            finish()
// //                        } else {
// //                            navController.popBackStack()
// //                        }
//                        finish()
//                    }

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

@Composable
fun MainScreenBody(
    time: Long,
    tick: Long,
    timerLabel: String,
    timerScreenState: TimerState,
    timerVisibility: Boolean,
    onActionClick: () -> Unit,
    onDelete: () -> Unit,
    onOptionTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(
        modifier =
            modifier
                .background(color = MaterialTheme.colorScheme.primary)
                .fillMaxSize(),
    ) {
        val (actionButtons, timer) = createRefs()
        val progress = (tick.toFloat() / time.toFloat()).coerceAtLeast(0f)
        val progressOffset = (1 - progress)
        val animatedProgress by animateFloatAsState(
            targetValue = progressOffset,
            animationSpec =
                SpringSpec(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                    // The default threshold is 0.01, or 1% of the overall progress range, which is quite
                    // large and noticeable.
                    visibilityThreshold = 1 / 1000f,
                ),
        )

        MainTimer(
            animatedProgress = animatedProgress,
            formattedTime = timerLabel,
            timerScreenState = timerScreenState,
            timerVisibility = timerVisibility,
            modifier =
                Modifier
                    .size(200.dp)
                    .constrainAs(timer) {
                        linkTo(
                            start = parent.start,
                            top = parent.top,
                            end = parent.end,
                            bottom = parent.bottom,
                            bottomMargin = 16.dp,
                        )
                    },
            onOptionTimerClick = onOptionTimerClick,
        )

//        ActionButtons(
//            onActionClick = onActionClick,
//            onDelete = onDelete,
//            timerScreenState = timerScreenState,
//            modifier =
//                Modifier
//                    .constrainAs(actionButtons) {
//                        bottom.linkTo(parent.bottom, margin = 16.dp)
//                        linkTo(
//                            start = parent.start,
//                            end = parent.end,
//                        )
//                        width = Dimension.fillToConstraints
//                    },
//        )
    }
}

@Composable
fun MainTimer(
    animatedProgress: Float,
    timerVisibility: Boolean,
    timerScreenState: TimerState,
    formattedTime: String,
    onOptionTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val progressVisibility =
            if (timerScreenState == TimerState.Finished) timerVisibility else true
        if (progressVisibility) {
            CircularProgressWithThumb(
                progress = animatedProgress,
                strokeWidth = 4.dp,
                thumbSize = 6.dp,
                modifier = Modifier.fillMaxSize(),
            )
        }

        ConstraintLayout(
            modifier = Modifier.fillMaxSize(),
        ) {
            val (timer, optionTimer) = createRefs()
            val labelTimerVisibility =
                if (timerScreenState == TimerState.Paused) timerVisibility else true
            AnimatedVisibility(
                visible = labelTimerVisibility,
                enter = fadeIn(initialAlpha = 0.6f),
                exit = fadeOut(),
                modifier =
                    Modifier.constrainAs(timer) {
                        linkTo(
                            start = parent.start,
                            top = parent.top,
                            end = parent.end,
                            bottom = parent.bottom,
                        )
                    },
            ) {
                val color =
                    if (timerScreenState == TimerState.Finished) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondary
                    }
                Text(
                    text = formattedTime,
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = formattedTime.calculateFontSize(),
                            fontWeight = FontWeight.W400,
                            letterSpacing = 1.sp,
                        ),
                    color = color,
                )
            }

            Button(
                onClick = onOptionTimerClick,
                modifier =
                    Modifier.constrainAs(optionTimer) {
                        linkTo(start = parent.start, end = parent.end)
                        bottom.linkTo(parent.bottom, margin = 20.dp)
                    },
            ) {
                val resId =
                    when (timerScreenState) {
                        TimerState.Started, TimerState.Finished -> R.string.label_plus_one_minute
                        else -> R.string.label_reset
                    }
                Text(
                    text = stringResource(resId),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.W400,
                        ),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
fun ActionButtons(
    timerScreenState: TimerState,
    onActionClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(modifier) {
        val (action, delete, addTimer) = createRefs()
        IconButton(
            onClick = onActionClick,
            modifier =
                Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = CircleShape,
                    ).constrainAs(action) {
                        linkTo(
                            start = parent.start,
                            end = parent.end,
                        )
                    },
        ) {
            val icon =
                when (timerScreenState) {
                    TimerState.Started -> Icons.Outlined.Pause
                    TimerState.Paused, TimerState.Stopped -> Icons.Outlined.PlayArrow
                    else -> Icons.Outlined.Stop
                }
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.label_start),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Button(
            onClick = onDelete,
            modifier =
                Modifier.constrainAs(delete) {
                    linkTo(top = action.top, bottom = action.bottom)
                    start.linkTo(parent.start, margin = 16.dp)
                },
        ) {
            Text(
                text = stringResource(R.string.label_delete),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.W400,
                    ),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Button(
            onClick = {},
            enabled = false,
            modifier =
                Modifier
                    .constrainAs(addTimer) {
                        linkTo(top = action.top, bottom = action.bottom)
                        end.linkTo(parent.end, margin = 16.dp)
                    },
        ) {
            Text(
                text = stringResource(R.string.label_add_timer),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.W400,
                    ),
            )
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    TVsleepTheme {
        Greeting("Android")
    }
}
