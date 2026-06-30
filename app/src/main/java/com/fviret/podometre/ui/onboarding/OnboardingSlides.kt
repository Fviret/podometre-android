package com.fviret.podometre.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fviret.podometre.R

// ── Slide 1 — Écran Activité ─────────────────────────────────────────────────

@Composable
fun OnboardingSlide1() {
    SlideLayout(
        icon = {
            Icon(
                imageVector = Icons.Default.DirectionsWalk,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = stringResource(R.string.onboarding_slide1_title),
        subtitle = stringResource(R.string.onboarding_slide1_subtitle)
    )
}

// ── Slide 2 — Trajets ────────────────────────────────────────────────────────

@Composable
fun OnboardingSlide2() {
    SlideLayout(
        icon = {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = stringResource(R.string.onboarding_slide2_title),
        subtitle = stringResource(R.string.onboarding_slide2_subtitle)
    )
}

// ── Slide 3 — Permissions ────────────────────────────────────────────────────

@Composable
fun OnboardingSlide3() {
    SlideLayout(
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = stringResource(R.string.onboarding_slide3_title),
        subtitle = stringResource(R.string.onboarding_slide3_subtitle),
        extra = {
            Spacer(Modifier.height(24.dp))
            PermissionRow(
                icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = null) },
                label = stringResource(R.string.onboarding_slide3_perm_steps)
            )
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                icon = { Icon(Icons.Default.Straighten, contentDescription = null) },
                label = stringResource(R.string.onboarding_slide3_perm_distance)
            )
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                label = stringResource(R.string.onboarding_slide3_perm_location)
            )
        }
    )
}

@Composable
private fun PermissionRow(icon: @Composable () -> Unit, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
    ) {
        icon()
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Slide 4 — Objectif ───────────────────────────────────────────────────────

@Composable
fun OnboardingSlide4(
    selectedGoal: Int,
    goalOptions: List<Int>,
    onGoalSelected: (Int) -> Unit
) {
    SlideLayout(
        icon = null,
        title = stringResource(R.string.onboarding_slide4_title),
        subtitle = stringResource(R.string.onboarding_slide4_subtitle),
        extra = {
            Spacer(Modifier.height(24.dp))
            GoalSelector(
                options = goalOptions,
                selected = selectedGoal,
                onSelect = onGoalSelected
            )
        }
    )
}

@Composable
private fun GoalSelector(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    // Grille 2 colonnes de chips sélectionnables
    val chunked = options.chunked(2)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        chunked.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { goal ->
                    FilterChip(
                        selected = goal == selected,
                        onClick = { onSelect(goal) },
                        label = {
                            Text(
                                text = stringResource(R.string.onboarding_slide4_goal_label, goal),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Remplir si nombre impair
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ── Layout commun ────────────────────────────────────────────────────────────

@Composable
private fun SlideLayout(
    icon: (@Composable () -> Unit)?,
    title: String,
    subtitle: String,
    extra: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.height(32.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        extra?.invoke()
    }
}
