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
import androidx.preference.PreferenceManager
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import co.touchlab.kermit.Logger
import com.google.android.play.core.review.ReviewManagerFactory
import io.middlepoint.tvsleep.ui.screens.Connecting
import io.middlepoint.tvsleep.ui.screens.ConnectingScreen
import io.middlepoint.tvsleep.ui.screens.Debug
import io.middlepoint.tvsleep.ui.screens.DebugScreen
import io.middlepoint.tvsleep.ui.screens.SetupADB
import io.middlepoint.tvsleep.ui.screens.SetupScreen
import io.middlepoint.tvsleep.ui.screens.TimeSelection
import io.middlepoint.tvsleep.ui.screens.home.HomeScreen
import io.middlepoint.tvsleep.ui.screens.Timer
import io.middlepoint.tvsleep.ui.screens.timer.TimerScreen
import io.middlepoint.tvsleep.ui.screens.mapToScreen
import io.middlepoint.tvsleep.ui.theme.TVsleepTheme
import androidx.core.content.edit

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
              HomeScreen()
              LaunchedEffect(Unit) {
                showReviewIfNeeded()
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

  private fun showReviewIfNeeded() {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val showReviewKey = getString(R.string.key_show_review)
    val reviewShownKey = getString(R.string.key_review_shown_count)
    val shouldShowReview = prefs.getBoolean(showReviewKey, false)
    val reviewShownCount = prefs.getInt(reviewShownKey, 0)
    val logger = Logger.withTag("MainActivity")

    if (shouldShowReview && reviewShownCount < 1) {
      val reviewManager = ReviewManagerFactory.create(this)
      val request = reviewManager.requestReviewFlow()
      request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
          val reviewInfo = task.result
          reviewManager.launchReviewFlow(this, reviewInfo)
          prefs.edit(commit = true) {
            putBoolean(showReviewKey, false)
            putInt(reviewShownKey, reviewShownCount + 1)
          }
          logger.d("Review flow launched, show_review set to false.")
        } else {
          logger.e(task.exception) { "Error requesting review flow" }
        }
      }
    } else {
      logger.d("Not showing review.")
    }
  }
}
