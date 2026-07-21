package com.fviret.podometre.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.ui.theme.AppColors

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
            text = "Paramètres",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        // ── Section : Objectif quotidien ──────────────────────────────────────
        SectionHeader(title = "Activité")

        StepGoalRow(
            currentGoal = prefs.dailyStepGoal,
            onGoalSelected = { viewModel.updateDailyStepGoal(it) },
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Section : Apparence ───────────────────────────────────────────────
        SectionHeader(title = "Apparence")

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
        SectionHeader(title = "Mon écran principal")

        ModuleToggleCard {
            ModuleToggleRow(
                label = "Météo & prévisions",
                checked = prefs.showWeatherForecast,
                onToggle = { viewModel.updateShowWeatherForecast(it) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ModuleToggleRow(
                label = "Calendrier mensuel",
                checked = prefs.showMonthCalendar,
                onToggle = { viewModel.updateShowMonthCalendar(it) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ModuleToggleRow(
                label = "Graphe hebdomadaire",
                checked = prefs.showWeeklyChart,
                onToggle = { viewModel.updateShowWeeklyChart(it) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ModuleToggleRow(
                label = "Métriques du jour",
                checked = prefs.showTodayMetrics,
                onToggle = { viewModel.updateShowTodayMetrics(it) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(title = "Notifications")

        NotificationsCard(
            notificationsEnabled = prefs.notificationsEnabled,
            journeyNotificationsEnabled = prefs.journeyNotificationsEnabled,
            onToggleGoal = { viewModel.onToggleGoalNotifications(it) },
            onToggleJourney = { viewModel.onToggleJourneyNotifications(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Section : Pensée du jour ──────────────────────────────────────────
        SectionHeader(title = "Pensée du jour")

        ModuleToggleCard {
            ModuleToggleRow(
                label = "Afficher la pensée du jour",
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
 * Ligne "Objectif quotidien" avec picker expandable.
 * Tap sur la ligne développe / réduit la grille de valeurs.
 * La valeur sélectionnée est mise en surbrillance et persiste immédiatement.
 */
@Composable
private fun StepGoalRow(
    currentGoal: Int,
    onGoalSelected: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // ── Ligne principale ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = if (expanded) "Fermer le picker d'objectif" else "Ouvrir le picker d'objectif",
                    ) { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .semantics { role = Role.Button },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pas par jour",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "%,d pas".format(currentGoal).replace(',', ' '),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            // ── Picker expandable ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    StepGoalGrid(
                        options = STEP_GOAL_OPTIONS,
                        selectedGoal = currentGoal,
                        onSelect = { goal ->
                            onGoalSelected(goal)
                            expanded = false
                        },
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

/**
 * Grille des valeurs d'objectif (3 colonnes).
 * La cellule correspondant à [selectedGoal] est mise en couleur primaire.
 */
@Composable
private fun StepGoalGrid(
    options: List<Int>,
    selectedGoal: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = 3
    val rows = (options.size + columns - 1) / columns

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < options.size) {
                        val goal = options[index]
                        val isSelected = goal == selectedGoal
                        GoalCell(
                            goal = goal,
                            isSelected = isSelected,
                            onSelect = { onSelect(goal) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Cellule individuelle du picker d'objectif. */
@Composable
private fun GoalCell(
    goal: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = "%,d".format(goal).replace(',', ' ')
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClickLabel = "Objectif $label pas") { onSelect() }
            .padding(vertical = 10.dp)
            .semantics { contentDescription = "Objectif $label pas${if (isSelected) ", sélectionné" else ""}" },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Noms français des couleurs de l'anneau, dans le même ordre qu'[AppColors.ringColorOptions]. */
private val ringColorNames: Map<String, String> = mapOf(
    "green"  to "Forêt",
    "blue"   to "Océan",
    "orange" to "Soleil",
    "red"    to "Corail",
    "purple" to "Violet",
    "teal"   to "Glace",
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
    val selectedColor = AppColors.colorForId(selectedColorId)
    val selectedName = ringColorNames[selectedColorId] ?: selectedColorId

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
                    val name = ringColorNames[id] ?: id
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
                            ) { onColorSelected(id) }
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
                text = "Mode sombre",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isDarkMode,
                onCheckedChange = onToggle,
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
            label = "Objectif journalier",
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
            onCheckedChange = onToggle,
            modifier = Modifier.semantics {
                contentDescription = "$label : ${if (checked) "activé" else "désactivé"}"
            },
        )
    }
}

// ── Taille du logo de badge (constante unique) ────────────────────────────────
private val BADGE_LOGO_SIZE = 46.dp

/**
 * Données d'un badge de seuil de pas : seuil, emoji illustratif, couleur d'accent,
 * titre court et description contextuelle.
 * La couleur est indépendante de la couleur d'anneau choisie dans les réglages.
 */
private data class BadgeData(
    val threshold: Long,
    val emoji: String,
    val color: Color,
    val title: String,
    val description: String,
)

/** Liste des 6 badges de seuil de pas, centralisée ici comme unique source de vérité. */
private val STEP_BADGES: List<BadgeData> = listOf(
    BadgeData(
        threshold  = 5_000L,
        emoji      = "🚶",
        color      = Color(0xFF4CAF50),
        title      = "Objectif 5 K",
        description = "Premier 5 000 pas — les premiers pas comptent !",
    ),
    BadgeData(
        threshold  = 10_000L,
        emoji      = "🏃",
        color      = Color(0xFF2196F3),
        title      = "Objectif 10 K",
        description = "Marcheur régulier — 10 000 pas dans la journée.",
    ),
    BadgeData(
        threshold  = 20_000L,
        emoji      = "⭐",
        color      = Color(0xFFFF9800),
        title      = "Objectif 20 K",
        description = "Grand marcheur — deux fois l'objectif quotidien !",
    ),
    BadgeData(
        threshold  = 30_000L,
        emoji      = "🏅",
        color      = Color(0xFFF44336),
        title      = "Objectif 30 K",
        description = "Marathonien du quotidien — endurance remarquable.",
    ),
    BadgeData(
        threshold  = 50_000L,
        emoji      = "🦅",
        color      = Color(0xFF9C27B0),
        title      = "Objectif 50 K",
        description = "Ultra-marcheur — 50 000 pas en une seule journée.",
    ),
    BadgeData(
        threshold  = 100_000L,
        emoji      = "🏆",
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
private fun formatThreshold(threshold: Long): String =
    "%,d".format(threshold).replace(',', ' ')

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
        // Grande illustration
        Text(
            text = badge.emoji,
            fontSize = 72.sp,
            textAlign = TextAlign.Center,
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
                    color = Color.White,
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
 * Cellule de badge pour un seuil de pas (refonte KAN-89).
 * — Illustration emoji grande (~46 dp)
 * — Titre sous l'illustration ("Objectif X K"), visible même verrouillé (grisé)
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

    // Filtre niveaux de gris pour les badges verrouillés
    val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

    Box(modifier = modifier.aspectRatio(1f)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUnlocked)
                    badge.color.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isUnlocked)
                        Modifier.drawWithContent {
                            val paint = Paint().apply {
                                colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
                            }
                            drawIntoCanvas { canvas ->
                                canvas.saveLayer(
                                    androidx.compose.ui.geometry.Rect(
                                        0f, 0f, size.width, size.height
                                    ),
                                    paint,
                                )
                                drawContent()
                                canvas.restore()
                            }
                        }
                    else Modifier
                )
                .alpha(if (isUnlocked) 1f else 0.5f)
                .clickable(onClickLabel = a11y, onClick = onClick)
                .clearAndSetSemantics { contentDescription = a11y },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Illustration emoji
                Text(
                    text = badge.emoji,
                    fontSize = 36.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Titre (visible même verrouillé)
                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked) badge.color
                    else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        // Icône 🔒 en overlay pour les badges verrouillés
        if (!isUnlocked) {
            Text(
                text = "🔒",
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
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
    val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

    Box(modifier = modifier.aspectRatio(1f)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUnlocked)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isUnlocked)
                        Modifier.drawWithContent {
                            val paint = Paint().apply {
                                colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
                            }
                            drawIntoCanvas { canvas ->
                                canvas.saveLayer(
                                    androidx.compose.ui.geometry.Rect(
                                        0f, 0f, size.width, size.height
                                    ),
                                    paint,
                                )
                                drawContent()
                                canvas.restore()
                            }
                        }
                    else Modifier
                )
                .alpha(if (isUnlocked) 1f else 0.5f)
                .clearAndSetSemantics { contentDescription = a11y },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Emoji agrandi (~46 dp)
                Text(
                    text = emoji,
                    fontSize = 36.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        // Icône 🔒 en overlay pour les badges verrouillés
        if (!isUnlocked) {
            Text(
                text = "🔒",
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
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
