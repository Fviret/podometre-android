package com.fviret.podometre.ui.aphorism

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fviret.podometre.data.aphorism.Aphorism

/**
 * Popup matinale "Pensée du jour" affichée une fois par jour à la première ouverture.
 * Affiche le texte de l'aphorisme, son auteur, sa catégorie (si renseignée), et un bouton
 * de fermeture "Make my day".
 *
 * Accessibilité (TalkBack) :
 * - Dialog Compose piège le focus (comportement modal natif)
 * - Titre "Pensée du jour" marqué `heading()` pour navigation par en-têtes
 * - Bloc citation/auteur/catégorie fusionné en un seul nœud via [clearAndSetSemantics]
 * - Bouton libellé "Fermer la pensée du jour" (contentDescription explicite)
 * - Emoji décoratif masqué (clearAndSetSemantics vide)
 * - Animations réduites : [Dialog] Compose honore `ANIMATOR_DURATION_SCALE = 0`
 *
 * @param aphorism L'aphorisme sélectionné pour aujourd'hui.
 * @param onDismiss Appelé quand l'utilisateur ferme la popup (bouton ou tap extérieur).
 */
@Composable
fun AphorismPopup(
    aphorism: Aphorism,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.testTag("aphorism_popup"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "✨",
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clearAndSetSemantics {},
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pensée du jour",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(modifier = Modifier.height(16.dp))

                val a11yText = buildString {
                    append(aphorism.text)
                    append(", ")
                    append(aphorism.author)
                    if (aphorism.category.isNotBlank()) {
                        append(", ")
                        append(aphorism.category)
                    }
                }
                Column(
                    modifier = Modifier.clearAndSetSemantics { contentDescription = a11yText },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "\u201C${aphorism.text}\u201D",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "— ${aphorism.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                    )

                    if (aphorism.category.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = aphorism.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        // Retour haptique de confirmation avant fermeture (cohérent avec navigation/copie).
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aphorism_dismiss_button")
                        .semantics { contentDescription = "Fermer la pensée du jour" },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(text = "Make my day ✊")
                }
            }
        }
    }
}
