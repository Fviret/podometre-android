package com.fviret.podometre.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.R

/**
 * Écran d'onboarding — carrousel 4 slides non dismissable.
 * Appelle [onComplete] une fois que l'utilisateur a terminé le flux.
 * Équivalent iOS : OnboardingView (fullScreenCover).
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { OnboardingViewModel.TOTAL_PAGES })

    // Synchronise le pager avec l'état du ViewModel
    LaunchedEffect(uiState.currentPage) {
        pagerState.animateScrollToPage(uiState.currentPage)
    }

    // Signal de complétion → remonte au parent
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onComplete()
    }

    // Bloquer le bouton retour — onboarding non dismissable
    BackHandler { /* intentionnellement vide */ }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Carrousel ────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> OnboardingSlide1()
                    1 -> OnboardingSlide2()
                    2 -> OnboardingSlide3()
                    3 -> OnboardingSlide4(
                        selectedGoal = uiState.selectedGoal,
                        goalOptions = OnboardingViewModel.GOAL_OPTIONS,
                        onGoalSelected = viewModel::setGoal
                    )
                }
            }

            // ── Indicateurs de page ───────────────────────────────────────
            PageIndicators(
                pageCount = OnboardingViewModel.TOTAL_PAGES,
                currentPage = uiState.currentPage,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // ── Bouton navigation ─────────────────────────────────────────
            val isLastPage = uiState.currentPage == OnboardingViewModel.TOTAL_PAGES - 1
            Button(
                onClick = {
                    if (isLastPage) viewModel.completeOnboarding()
                    else viewModel.nextPage()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isLastPage) R.string.onboarding_start
                        else R.string.onboarding_next
                    )
                )
            }
        }
    }
}

/**
 * Indicateurs de page animés (points Material 3).
 */
@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val color by animateColorAsState(
                targetValue = if (index == currentPage)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outlineVariant,
                label = "indicator_color_$index"
            )
            val size = if (index == currentPage) 10.dp else 8.dp
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
