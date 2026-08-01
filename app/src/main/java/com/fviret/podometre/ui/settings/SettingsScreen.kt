package com.fviret.podometre.ui.settings

import com.fviret.podometre.ui.theme.rememberReduceMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.pow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.BuildConfig
import com.fviret.podometre.R
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.ui.theme.AppColors
import com.fviret.podometre.util.formatSteps
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Écran Paramètres — section US-5.1 : objectif quotidien de pas.
 * Affiche la valeur courante et un picker expandable (5 000–20 000 par pas de 500).
 * Équivalent iOS : SettingsView.swift > Section "Objectif quotidien".
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val stepBadgeCounts by viewModel.stepBadgeCounts.collectAsStateWithLifecycle()
    val stepBadgeFirstDates by viewModel.stepBadgeFirstDates.collectAsStateWithLifecycle()
    val completedJourneyIds by viewModel.completedJourneyIds.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        // ── Section : Objectif quotidien ──────────────────────────────────────
        SectionHeader(title = stringResource(R.string.settings_section_activity))

        StepGoalRow(
            currentGoal = prefs.dailyStepGoal,
            onGoalSelected = { viewModel.updateDailyStepGoal(it) },
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Section : Apparence ───────────────────────────────────────────────
        SectionHeader(title = stringResource(R.string.settings_section_appearance))

        RingColorRow(
            selectedColorId = prefs.ringColorId,
            onColorSelected = { viewModel.updateRingColorId(it) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        DarkModeRow(
            isDarkMode = prefs.isDarkMode,
            onToggle = { viewModel.updateDarkMode(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Section : Mon écran principal ─────────────────────────────────────
        SectionHeader(title = stringResource(R.string.settings_section_display))

        ModuleToggleCard {
            ModuleToggleRow(
                label = stringResource(R.string.settings_toggle_weather),
                checked = prefs.showWeatherForecast,
                onToggle = { viewModel.updateShowWeatherForecast(it) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ModuleToggleRow(
                label = stringResource(R.string.settings_toggle_calendar),
                checked = prefs.showMonthCalendar,
                onToggle = { viewModel.updateShowMonthCalendar(it) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ModuleToggleRow(
                label = stringResource(R.string.settings_toggle_chart),
                checked = prefs.showWeeklyChart,
                onToggle = { viewModel.updateShowWeeklyChart(it) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ModuleToggleRow(
                label = stringResource(R.string.settings_toggle_metrics),
                checked = prefs.showTodayMetrics,
                onToggle = { viewModel.updateShowTodayMetrics(it) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(title = stringResource(R.string.settings_section_notifications))

        NotificationsCard(
            notificationsEnabled = prefs.notificationsEnabled,
            journeyNotificationsEnabled = prefs.journeyNotificationsEnabled,
            onToggleGoal = { viewModel.onToggleGoalNotifications(it) },
            onToggleJourney = { viewModel.onToggleJourneyNotifications(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Section : Pensée du jour ──────────────────────────────────────────
        SectionHeader(title = stringResource(R.string.settings_section_aphorism))

        ModuleToggleCard {
            ModuleToggleRow(
                label = stringResource(R.string.settings_toggle_aphorism),
                checked = prefs.aphorismEnabled,
                onToggle = { viewModel.updateAphorismEnabled(it) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        com.fviret.podometre.ui.aphorism.AphorismCard(
            aphorism = viewModel.todayAphorism,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(title = "Progression")
        if (streak > 0) {
            StreakBanner(streakDays = streak)
            Spacer(modifier = Modifier.height(8.dp))
        }
        BadgesSection(
            stepBadgeCounts = stepBadgeCounts,
            stepBadgeFirstDates = stepBadgeFirstDates,
            completedJourneyIds = completedJourneyIds,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Section : À propos ────────────────────────────────────────────────
        SectionHeader(title = "À propos")

        AboutSection()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** En-tête d'une section de paramètres. */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
    )
}

/**
 * Ligne "Objectif quotidien" avec stepper − / +.
 * Bornes : [STEP_GOAL_MIN] à [STEP_GOAL_MAX] par pas de [STEP_GOAL_STEP].
 * — Appui court : incrément / décrément simple avec rebond.
 * — Appui long : répétition accélérée (intervalle initial 280 ms, ×0,82 par cran, plancher 40 ms).
 * — Rebond : grossissant sur incrément, rétrécissant sur décrément.
 * — Haptique léger à chaque cran.
 * — Sans animation si "Réduire les animations" est actif.
 */
@Composable
private fun StepGoalRow(
    currentGoal: Int,
    onGoalSelected: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()

    // Compteur de changements pour déclencher le rebond à chaque cran (même taps rapprochés)
    var bounceKey by remember { mutableIntStateOf(0) }
    // Direction du dernier changement : +1 grossit, -1 rétrécit
    var bounceDirection by remember { mutableIntStateOf(1) }
    val scaleAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Lance un rebond à chaque nouveau cran
    LaunchedEffect(bounceKey) {
        if (bounceKey == 0) return@LaunchedEffect
        if (reduceMotion) return@LaunchedEffect
        val peak = if (bounceDirection > 0) 1.18f else 0.82f
        scaleAnim.snapTo(peak)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        )
    }

    /**
     * Effectue un changement de valeur d'un [delta] (±[STEP_GOAL_STEP]).
     * Déclenche le rebond et le retour haptique.
     * @param isFirstOfLongPress vrai uniquement pour le premier cran du maintien (déclenche le rebond une fois).
     */
    fun applyDelta(delta: Int, isFirstOfLongPress: Boolean = false) {
        val newGoal = (currentGoal + delta).coerceIn(STEP_GOAL_MIN, STEP_GOAL_MAX)
        if (newGoal == currentGoal) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onGoalSelected(newGoal)
        if (!isFirstOfLongPress) {
            bounceDirection = if (delta > 0) 1 else -1
            bounceKey++
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Bouton − ──────────────────────────────────────────────────────
            StepperButton(
                label = "−",
                contentDesc = "Diminuer l'objectif",
                enabled = currentGoal > STEP_GOAL_MIN,
                onTap = {
                    bounceDirection = -1
                    bounceKey++
                    applyDelta(-STEP_GOAL_STEP)
                },
                onLongPress = {
                    // Premier cran du maintien : rebond unique
                    bounceDirection = -1
                    bounceKey++
                    applyDelta(-STEP_GOAL_STEP, isFirstOfLongPress = true)
                    // Répétition accélérée
                    scope.launch {
                        var intervalMs = 280L
                        while (currentGoal - STEP_GOAL_STEP >= STEP_GOAL_MIN) {
                            delay(intervalMs)
                            applyDelta(-STEP_GOAL_STEP)
                            intervalMs = (intervalMs * 0.82).toLong().coerceAtLeast(40L)
                        }
                    }
                },
            )

            // ── Valeur centrale avec rebond ───────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        // La valeur est annoncée par TalkBack ; les actions incrément/décrément
                        // sont portées par les boutons − et + labellisés adjacents.
                        contentDescription = "${currentGoal.formatSteps()} pas par jour"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentGoal.formatSteps(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.scale(if (reduceMotion) 1f else scaleAnim.value),
                    )
                    Text(
                        text = stringResource(R.string.settings_step_unit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Bouton + ──────────────────────────────────────────────────────
            StepperButton(
                label = "+",
                contentDesc = "Augmenter l'objectif",
                enabled = currentGoal < STEP_GOAL_MAX,
                onTap = {
                    bounceDirection = 1
                    bounceKey++
                    applyDelta(+STEP_GOAL_STEP)
                },
                onLongPress = {
                    bounceDirection = 1
                    bounceKey++
                    applyDelta(+STEP_GOAL_STEP, isFirstOfLongPress = true)
                    scope.launch {
                        var intervalMs = 280L
                        while (currentGoal + STEP_GOAL_STEP <= STEP_GOAL_MAX) {
                            delay(intervalMs)
                            applyDelta(+STEP_GOAL_STEP)
                            intervalMs = (intervalMs * 0.82).toLong().coerceAtLeast(40L)
                        }
                    }
                },
            )
        }
    }
}

/**
 * Bouton circulaire du stepper (− ou +).
 * Gère le tap simple via [onTap] et l'appui long via [onLongPress].
 * Désactivé aux bornes ([enabled] = false).
 *
 * @param label      Texte affiché dans le cercle ("−" ou "+")
 * @param contentDesc Description pour TalkBack ("Diminuer l'objectif" / "Augmenter l'objectif")
 * @param enabled     Faux quand la valeur est à la borne correspondante
 * @param onTap       Callback déclenché sur un tap court
 * @param onLongPress Callback déclenché au début d'un appui long (la répétition est gérée en interne)
 */
@Composable
private fun StepperButton(
    label: String,
    contentDesc: String,
    enabled: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            )
            .then(
                if (enabled) Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { onLongPress() },
                    )
                } else Modifier
            )
            .semantics {
                contentDescription = contentDesc
                role = Role.Button
            },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
    }
}

/**
 * Retourne la map des noms localisés des couleurs de l'anneau.
 * Appelé depuis un Composable pour bénéficier de [stringResource].
 */
@Composable
private fun ringColorNames(): Map<String, String> = mapOf(
    "green"  to stringResource(R.string.ring_color_green),
    "blue"   to stringResource(R.string.ring_color_blue),
    "orange" to stringResource(R.string.ring_color_orange),
    "red"    to stringResource(R.string.ring_color_red),
    "purple" to stringResource(R.string.ring_color_purple),
    "teal"   to stringResource(R.string.ring_color_teal),
)

/**
 * Section "Couleur de l'anneau" — nom de la couleur sélectionnée + grille de 6 cercles.
 * Cercle sélectionné : bordure noire épaisse (comme iOS).
 * Équivalent iOS : Section "Personnalisation des couleurs" dans SettingsView.swift.
 */
@Composable
private fun RingColorRow(
    selectedColorId: String,
    onColorSelected: (String) -> Unit,
) {
    val colorNames = ringColorNames()
    val selectedColor = AppColors.colorForId(selectedColorId)
    val selectedName = colorNames[selectedColorId] ?: selectedColorId
    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Nom de la couleur sélectionnée avec pastille (iso iOS)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(selectedColor),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedName,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            // Grille des 6 cercles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AppColors.ringColorOptions.forEach { (id, color) ->
                    val isSelected = id == selectedColorId
                    val name = colorNames[id] ?: id
                    // contentDescription verbeux : nom + état pour TalkBack
                    val a11yLabel = if (isSelected) "$name — sélectionnée" else name
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            // Cible tactile 48 dp (minimum recommandé Android)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected)
                                    Modifier.border(3.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable(
                                onClickLabel = "Définit la couleur de l'anneau sur $name",
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onColorSelected(id)
                            }
                            .semantics {
                                contentDescription = a11yLabel
                                role = Role.RadioButton
                                selected = isSelected
                            },
                    ) {
                        // Coche ✓ visible sur la pastille sélectionnée (indicateur non-couleur)
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Toggle "Mode sombre" — bascule le thème de toute l'application immédiatement.
 * Équivalent iOS : Toggle isDarkMode dans SettingsView.swift.
 */
@Composable
private fun DarkModeRow(
    isDarkMode: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_dark_mode),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isDarkMode,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle(it)
                },
                modifier = Modifier.semantics {
                    contentDescription = if (isDarkMode) "Mode sombre activé" else "Mode sombre désactivé"
                },
            )
        }
    }
}

/**
 * Section Notifications — deux toggles avec gestion de la permission POST_NOTIFICATIONS.
 * Si la permission est refusée et que l'utilisateur active un toggle, on ouvre les
 * paramètres système Android pour qu'il puisse l'accorder manuellement.
 * Équivalent iOS : Section "Notifications" dans SettingsView.swift.
 */
@Composable
private fun NotificationsCard(
    notificationsEnabled: Boolean,
    journeyNotificationsEnabled: Boolean,
    onToggleGoal: (Boolean) -> Unit,
    onToggleJourney: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    val hasPermission = remember(context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
        else ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Permission refusée : ouvre les paramètres système
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    fun requestIfNeeded(enable: Boolean, onGranted: (Boolean) -> Unit) {
        if (!enable) { onGranted(false); return }
        if (hasPermission) { onGranted(true); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onGranted(true)
        }
    }

    ModuleToggleCard {
        ModuleToggleRow(
            label = stringResource(R.string.settings_notif_daily_goal),
            checked = notificationsEnabled,
            onToggle = { requestIfNeeded(it) { granted -> onToggleGoal(granted) } },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ModuleToggleRow(
            label = "Progression des trajets",
            checked = journeyNotificationsEnabled,
            onToggle = { requestIfNeeded(it) { granted -> onToggleJourney(granted) } },
        )
    }
}

/**
 * Conteneur Card pour une liste de toggles de modules.
 * Regroupe les lignes dans une carte arrondie (même style que les autres sections).
 */
@Composable
private fun ModuleToggleCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

/** Ligne toggle individuelle pour un module de l'écran Activité. */
@Composable
private fun ModuleToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle(it)
            },
            modifier = Modifier.semantics {
                contentDescription = "$label : ${if (checked) "activé" else "désactivé"}"
            },
        )
    }
}

// ── Taille du logo de badge (constante unique) ────────────────────────────────
private val BADGE_LOGO_SIZE = 46.dp

/**
 * Données d'un badge de seuil de pas : seuil, icône vectorielle Material, couleur d'accent,
 * titre court et description contextuelle.
 * La couleur est indépendante de la couleur d'anneau choisie dans les réglages.
 */
private data class BadgeData(
    val threshold: Long,
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val description: String,
)

/** Liste des 6 badges de seuil de pas, centralisée ici comme unique source de vérité. */
private val STEP_BADGES: List<BadgeData> = listOf(
    BadgeData(
        threshold  = 5_000L,
        icon       = Icons.AutoMirrored.Filled.DirectionsWalk,
        color      = Color(0xFF4CAF50),
        title      = "Objectif 5 K",
        description = "Premier 5 000 pas — les premiers pas comptent !",
    ),
    BadgeData(
        threshold  = 10_000L,
        icon       = Icons.AutoMirrored.Filled.DirectionsRun,
        color      = Color(0xFF2196F3),
        title      = "Objectif 10 K",
        description = "Marcheur régulier — 10 000 pas dans la journée.",
    ),
    BadgeData(
        threshold  = 20_000L,
        icon       = Icons.Default.Star,
        color      = Color(0xFFFF9800),
        title      = "Objectif 20 K",
        description = "Grand marcheur — deux fois l'objectif quotidien !",
    ),
    BadgeData(
        threshold  = 30_000L,
        icon       = Icons.Default.MilitaryTech,
        color      = Color(0xFFF44336),
        title      = "Objectif 30 K",
        description = "Marathonien du quotidien — endurance remarquable.",
    ),
    BadgeData(
        threshold  = 50_000L,
        icon       = Icons.Default.EmojiNature,
        color      = Color(0xFF9C27B0),
        title      = "Objectif 50 K",
        description = "Ultra-marcheur — 50 000 pas en une seule journée.",
    ),
    BadgeData(
        threshold  = 100_000L,
        icon       = Icons.Default.EmojiEvents,
        color      = Color(0xFF26BFB8),
        title      = "Objectif 100 K",
        description = "Légende de la marche — exploit hors du commun !",
    ),
)

/**
 * Section "Badges" — grille 3 colonnes avec badges de seuils de pas et badges de trajets.
 * Badges de pas : illustration emoji, couleur propre, pastille du compteur.
 * Badges verrouillés : niveaux de gris + icône 🔒.
 * Tap sur badge débloqué → ModalBottomSheet de détail.
 * Équivalent iOS : BadgeGridView.swift / BadgeData.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgesSection(
    stepBadgeCounts: Map<Long, Int>,
    stepBadgeFirstDates: Map<Long, java.time.LocalDate?>,
    completedJourneyIds: Set<String>,
) {
    var selectedBadge by remember { mutableStateOf<Triple<BadgeData, Int, java.time.LocalDate?>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Column {
        // ── Badges de seuils de pas ───────────────────────────────────────────
        val columns = 3
        val rows = (STEP_BADGES.size + columns - 1) / columns
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < STEP_BADGES.size) {
                        val badge = STEP_BADGES[index]
                        val count = stepBadgeCounts[badge.threshold] ?: 0
                        val firstDate = stepBadgeFirstDates[badge.threshold]
                        StepBadgeCell(
                            badge = badge,
                            count = count,
                            onClick = { selectedBadge = Triple(badge, count, firstDate) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (row < rows - 1) Spacer(modifier = Modifier.height(8.dp))
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )

        // ── Badges de trajets ─────────────────────────────────────────────────
        val journeys = JourneyData.all
        val journeyRows = (journeys.size + columns - 1) / columns
        for (row in 0 until journeyRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < journeys.size) {
                        val journey = journeys[index]
                        val isUnlocked = journey.id.toString() in completedJourneyIds
                        JourneyBadgeCell(
                            emoji = journey.emoji,
                            name = journey.name,
                            isUnlocked = isUnlocked,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (row < journeyRows - 1) Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // ── Modale de détail (ModalBottomSheet) ───────────────────────────────────
    selectedBadge?.let { (badge, count, firstDate) ->
        ModalBottomSheet(
            onDismissRequest = { selectedBadge = null },
            sheetState = sheetState,
        ) {
            BadgeDetailContent(badge = badge, count = count, firstEarnedDate = firstDate)
        }
    }
}

/** Formate un seuil de pas en libellé lisible (ex. 5000 → "5 000", 10000 → "10 000"). */
private fun formatThreshold(threshold: Long): String = threshold.formatSteps()

/**
 * Calcule la couleur de texte (noir ou blanc) offrant le meilleur contraste WCAG AA
 * sur le fond [background] donné. Utilise la luminance relative (formule WCAG 1.4.3)
 * avec le seuil 0,179 (ratio ≥ 4,5:1 garanti).
 */
private fun contrastTextColor(background: Color): Color {
    val r = background.red.linearize()
    val g = background.green.linearize()
    val b = background.blue.linearize()
    val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
    return if (luminance > 0.179) Color.Black else Color.White
}

/** Linéarise une composante sRGB pour le calcul de luminance WCAG. */
private fun Float.linearize(): Double =
    if (this <= 0.03928f) (this / 12.92).toDouble()
    else ((this + 0.055) / 1.055).pow(2.4)

/**
 * Contenu de la modale de détail d'un badge de seuil de pas.
 * Affiche l'illustration agrandie, le titre centré, la pastille compteur et une phrase de contexte.
 */
@Composable
private fun BadgeDetailContent(badge: BadgeData, count: Int, firstEarnedDate: java.time.LocalDate?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Grande icône vectorielle scalable (remplace l'emoji bitmap — KAN-104)
        Icon(
            imageVector = badge.icon,
            contentDescription = null,
            tint = badge.color,
            modifier = Modifier.size(96.dp),
        )
        // Titre centré
        Text(
            text = badge.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = badge.color,
        )
        // Pastille compteur (affichée seulement si count > 0)
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(badge.color)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "$count ×",
                    style = MaterialTheme.typography.labelLarge,
                    color = contrastTextColor(badge.color),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        // Phrase de contexte
        Text(
            text = badge.description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Date du premier déblocage (si disponible)
        if (firstEarnedDate != null) {
            val formatted = firstEarnedDate.format(
                java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.FRENCH)
            )
            Text(
                text = "Débloqué le $formatted",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * Cellule de badge pour un seuil de pas (refonte KAN-89, icônes vectorielles KAN-104).
 * — Icône Material vectorielle scalable (~46 dp)
 * — Titre sous l'icône ("Objectif X K"), visible même verrouillé (grisé)
 * — Pastille colorée "N ×" masquée si count = 0
 * — Verrouillé : niveaux de gris + icône 🔒
 * — Tap → modale de détail (badge débloqué comme verrouillé)
 */
@Composable
private fun StepBadgeCell(
    badge: BadgeData,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUnlocked = count > 0
    val a11y = if (isUnlocked)
        "${badge.title} — débloqué ($count fois)"
    else
        "${badge.title} — verrouillé"

    Box(modifier = modifier.aspectRatio(1f)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUnlocked)
                    badge.color.copy(alpha = 0.15f)
                else
                    badge.color.copy(alpha = 0.08f),
            ),
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isUnlocked)
                        Modifier.border(
                            width = 1.5.dp,
                            color = badge.color.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                        )
                    else Modifier
                )
                .clickable(onClickLabel = a11y, onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = a11y
                    role = Role.Button
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Icône vectorielle scalable (remplace l'emoji bitmap — KAN-104)
                // Verrouillé : couleur réelle atténuée (alpha 0.35) pour l'effet aspirationnel
                Icon(
                    imageVector = badge.icon,
                    contentDescription = null,
                    tint = if (isUnlocked) badge.color
                           else badge.color.copy(alpha = 0.35f),
                    modifier = Modifier.size(BADGE_LOGO_SIZE),
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Titre (visible même verrouillé)
                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked) badge.color
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Pastille compteur (masquée si count = 0)
                if (count > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(badge.color)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "$count ×",
                            style = MaterialTheme.typography.labelSmall,
                            color = contrastTextColor(badge.color),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        // Cadenas en coin bas-droit pour les badges verrouillés (KAN-111 — aspect aspirationnel)
        if (!isUnlocked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp),
            )
        }
    }
}

/**
 * Cellule de badge pour un trajet.
 * Déverrouillé : couleurs normales, emoji agrandi (~46 dp).
 * Verrouillé : niveaux de gris + icône 🔒 + opacité réduite.
 */
@Composable
private fun JourneyBadgeCell(
    emoji: String,
    name: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val a11y = if (isUnlocked) "$name — débloqué" else "$name — verrouillé"
    // Couleur d'accent neutre utilisée pour le contour aspirationnel des badges de trajet verrouillés
    val journeyAccent = MaterialTheme.colorScheme.secondary

    Box(modifier = modifier.aspectRatio(1f)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUnlocked)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    journeyAccent.copy(alpha = 0.08f),
            ),
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isUnlocked)
                        Modifier.border(
                            width = 1.5.dp,
                            color = journeyAccent.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                        )
                    else Modifier
                )
                .clearAndSetSemantics { contentDescription = a11y },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Emoji agrandi (~46 dp) — atténué si verrouillé
                Text(
                    text = emoji,
                    fontSize = 36.sp,
                    textAlign = TextAlign.Center,
                    modifier = if (!isUnlocked) Modifier.alpha(0.35f) else Modifier,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUnlocked)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        // Cadenas en coin bas-droit pour les badges verrouillés (KAN-111 — aspect aspirationnel)
        if (!isUnlocked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp),
            )
        }
    }
}

/**
 * Bannière streak — affiche 🔥 + nombre de jours consécutifs en grand.
 * Masquée si streak == 0 (vérification côté appelant).
 * Équivalent iOS : StreakBannerView.swift
 */
@Composable
private fun StreakBanner(streakDays: Int) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "Série de $streakDays jours consécutifs" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$streakDays",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (streakDays == 1) "jour consécutif" else "jours consécutifs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Section "À propos" — nom de l'app, version, lien politique de confidentialité et crédits.
 * La version est lue depuis [BuildConfig.VERSION_NAME].
 * Tap sur "Politique de confidentialité" → AlertDialog informatif.
 */
@Composable
private fun AboutSection() {
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // Nom + version
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Podomètre",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Politique de confidentialité
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = "Afficher la politique de confidentialité",
                    ) { showPrivacyDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .semantics { role = Role.Button },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Politique de confidentialité",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Crédits
            Text(
                text = "Développé par Fviret • Citations CC0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    // Dialog politique de confidentialité
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(text = "Politique de confidentialité")
            },
            text = {
                Text(
                    text = "Aucune donnée n'est collectée. Toutes les données restent sur votre appareil.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Fermer")
                }
            },
        )
    }
}

/** Ligne placeholder pour les sections à venir. */
@Composable
private fun ComingSoonRow(label: String, ticket: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ticket,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}
