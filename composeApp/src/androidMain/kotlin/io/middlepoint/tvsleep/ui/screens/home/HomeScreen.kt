package io.middlepoint.tvsleep.ui.screens.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import io.middlepoint.tvsleep.BuildConfig
import io.middlepoint.tvsleep.DashedBorder
import io.middlepoint.tvsleep.Purple40
import io.middlepoint.tvsleep.R
import io.middlepoint.tvsleep.ui.components.TVCPBanner
import io.middlepoint.tvsleep.ui.components.V2FocusableCard
import io.middlepoint.tvsleep.ui.components.V2Header
import io.middlepoint.tvsleep.ui.components.dashedBorder
import io.middlepoint.tvsleep.ui.theme.TVsleepTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = viewModel(),
  onNavigateToCustomTime: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()

  BackHandler(enabled = uiState.itemInDeleteMode != null || uiState.selectionMode == SelectionMode.App) {
    if (uiState.selectionMode == SelectionMode.App) {
      viewModel.onEvent(TimeSelectionEvent.OnBackFromAppSelection)
    } else {
      viewModel.onEvent(TimeSelectionEvent.OnCancelDelete)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(top = 72.dp, start = 96.dp, end = 96.dp, bottom = 64.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    AnimatedContent(targetState = uiState.selectionMode, label = "Time/App selection") {
      when (it) {
        SelectionMode.Time ->
          TimerSetup(
            state = uiState,
            onEvent = viewModel::onEvent,
            onNavigateToCustomTime = onNavigateToCustomTime
          )

        SelectionMode.App ->
          AppSelection(
            state = uiState,
            onEvent = viewModel::onEvent,
          )
      }
    }
  }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppSelection(
  state: TimeSelectionState,
  onEvent: (TimeSelectionEvent) -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    AnimatedContent(targetState = state.appSelectionMode, label = "App selection mode") {
      when (it) {
        AppSelectionMode.Curated -> CuratedApps(focusRequester, state, onEvent)
        AppSelectionMode.All -> AllApps(focusRequester, state, onEvent)
      }
    }
  }
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }
}

@Composable
@OptIn(ExperimentalTvMaterial3Api::class)
private fun AllApps(
  focusRequester: FocusRequester,
  state: TimeSelectionState,
  onEvent: (TimeSelectionEvent) -> Unit
) {
  LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Fixed(6),
    contentPadding = PaddingValues(0.dp),
    modifier = Modifier.focusRequester(focusRequester),
    verticalItemSpacing = 18.dp,
    horizontalArrangement = Arrangement.spacedBy(18.dp),
    userScrollEnabled = true,
  ) {
    items(state.installedApps) { app ->
      AppCard(
        app = app,
        onEvent = onEvent,
        height = 184.dp,
        iconSize = 60.dp,
        labelSize = 22
      )
    }
  }
}

@Composable
@OptIn(ExperimentalTvMaterial3Api::class)
private fun CuratedApps(
  focusRequester: FocusRequester,
  state: TimeSelectionState,
  onEvent: (TimeSelectionEvent) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    V2Header(
      step = 2,
      totalSteps = 2,
      eyebrow = "STEP 2 OF 2 · PICK AN APP",
      title = stringResource(R.string.app_selection_title)
    )

    Spacer(modifier = Modifier.height(32.dp))

    LazyVerticalStaggeredGrid(
      columns = StaggeredGridCells.Fixed(4),
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier.focusRequester(focusRequester),
      verticalItemSpacing = 22.dp,
      horizontalArrangement = Arrangement.spacedBy(22.dp),
      userScrollEnabled = true,
    ) {
    item {
      V2FocusableCard(
        onClick = { onEvent(TimeSelectionEvent.StartTimerOnly) },
        modifier = Modifier.height(232.dp),
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(RoundedCornerShape(18.dp))
              .background(Purple40),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Timer,
              contentDescription = stringResource(R.string.start_timer_only),
              modifier = Modifier.size(48.dp),
              tint = Color.White
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = stringResource(R.string.start_timer_only),
            textAlign = TextAlign.Center,
            fontSize = 28.sp,
            fontWeight = FontWeight.W600,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    val allApps = (state.popularApps + state.userSelectedApps).distinctBy { it.packageName }
    items(allApps) { app ->
      AppCard(app = app, onEvent = onEvent)
    }
    item {
      V2FocusableCard(
        onClick = { onEvent(TimeSelectionEvent.OnAddAppsClicked) },
        modifier = Modifier
          .height(232.dp)
          .dashedBorder(),
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.add_apps),
            modifier = Modifier.size(48.dp),
            tint = DashedBorder
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = stringResource(R.string.add_apps),
            textAlign = TextAlign.Center,
            fontSize = 28.sp,
            fontWeight = FontWeight.W600,
            color = DashedBorder,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
    } // end LazyVerticalStaggeredGrid
  } // end Column
}

@Composable
@OptIn(ExperimentalTvMaterial3Api::class)
private fun AppCard(
  app: AppInfo,
  onEvent: (TimeSelectionEvent) -> Unit,
  height: Dp = 232.dp,
  iconSize: Dp = 80.dp,
  labelSize: Int = 28
) {
  V2FocusableCard(
    onClick = { onEvent(TimeSelectionEvent.OnAppSelected(app)) },
    modifier = Modifier.height(height),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.primaryContainer),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Image(
        painter = rememberAsyncImagePainter(app.icon),
        contentDescription = app.label,
        modifier = Modifier
          .size(iconSize)
          .clip(RoundedCornerShape(18.dp))
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = app.label,
        textAlign = TextAlign.Center,
        fontSize = labelSize.sp,
        fontWeight = FontWeight.W600,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TimerSetup(
  state: TimeSelectionState,
  onEvent: (TimeSelectionEvent) -> Unit,
  onNavigateToCustomTime: () -> Unit
) {
  val focusRequester = remember { FocusRequester() }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      V2Header(
        step = 1,
        totalSteps = 2,
        eyebrow = "STEP 1 OF 2 · PICK A DURATION",
        title = stringResource(R.string.timer_setup_title)
      )

      Spacer(modifier = Modifier.height(24.dp))

      val context = LocalContext.current
      TVCPBanner(
        onBannerClick = {
          try {
            context.startActivity(
              Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=io.middlepoint.tvcp"))
            )
          } catch (e: ActivityNotFoundException) {
            context.startActivity(
              Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=io.middlepoint.tvcp"))
            )
          }
        },
        modifier = Modifier.padding(bottom = 24.dp)
      )

      LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(4),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.focusRequester(focusRequester),
        verticalItemSpacing = 22.dp,
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        userScrollEnabled = true,
      ) {
      if (BuildConfig.DEBUG) {
        item {
          TimeOption(
            time = "DEBUG",
            isInDeleteMode = false,
            isEasterEgg = state.showEasterEgg,
            onClick = { onEvent(TimeSelectionEvent.OnTimeSelected(debugTimeOption)) },
            onLongClick = { onEvent(TimeSelectionEvent.ShowEasterEgg) },
          )
        }
      }

      items(state.timeOptions) { item ->
        val isInDeleteMode = state.itemInDeleteMode == item
        TimeOption(
          time = item.time,
          isInDeleteMode = isInDeleteMode,
          isEasterEgg = false,
          onClick = {
            if (isInDeleteMode) {
              onEvent(TimeSelectionEvent.OnDeleteItem(item))
            } else {
              onEvent(TimeSelectionEvent.OnTimeSelected(item))
            }
          },
          onLongClick = { onEvent(TimeSelectionEvent.OnTimeItemLongPress(item)) },
        )
      }

      item {
        TimeOption(
          time = "Custom",
          isInDeleteMode = false,
          isEasterEgg = state.showEasterEgg,
          isCustomTile = true,
          onClick = onNavigateToCustomTime,
          onLongClick = { onEvent(TimeSelectionEvent.ShowEasterEgg) },
        )
      }
    }
    } // end Column
  } // end Box

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }
}

private enum class TimeOptionContentState {
  Normal,
  Delete,
  EasterEgg,
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TimeOption(
  time: String = "00:00",
  isInDeleteMode: Boolean,
  isEasterEgg: Boolean,
  isCustomTile: Boolean = false,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)?,
) {
  val animatedColor by animateColorAsState(
    targetValue = if (isInDeleteMode) Color.Red else MaterialTheme.colorScheme.primaryContainer,
    label = "Card color",
  )

  val baseModifier = Modifier.height(232.dp)
  val tileModifier = if (isCustomTile) {
    baseModifier
      .dashedBorder()
      .background(Color.Transparent)
  } else {
    baseModifier
  }

  V2FocusableCard(
    onClick = onClick,
    onLongClick = onLongClick,
    modifier = tileModifier,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(if (isCustomTile) Color.Transparent else animatedColor),
      contentAlignment = Alignment.Center,
    ) {
      val contentState =
        when {
          isEasterEgg -> TimeOptionContentState.EasterEgg
          isInDeleteMode -> TimeOptionContentState.Delete
          else -> TimeOptionContentState.Normal
        }
      AnimatedContent(targetState = contentState, label = "Content animation") { state ->
        when (state) {
          TimeOptionContentState.Normal -> {
            Text(
              text = time,
              fontSize = 56.sp,
              fontWeight = FontWeight.W700,
              letterSpacing = (-0.025).em,
              textAlign = TextAlign.Center,
            )
          }

          TimeOptionContentState.Delete -> {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete",
              modifier = Modifier.size(48.dp),
            )
          }

          TimeOptionContentState.EasterEgg -> {
            Icon(
              imageVector = Icons.Default.SentimentVerySatisfied,
              contentDescription = "Smiley face",
              modifier = Modifier.size(48.dp),
            )
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun HomeScreenPreview() {
  TVsleepTheme {
    HomeScreen(onNavigateToCustomTime = {})
  }
}
