package com.fviret.podometre.ui.aphorism

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fviret.podometre.data.aphorism.Aphorism
import kotlinx.coroutines.delay

/**
 * Carte de l'aphorisme du jour affichée dans la section "Pensée du jour" des Paramètres.
 * Un appui copie la citation et l'auteur dans le presse-papiers et affiche "Copié !" pendant
 * 2 secondes.
 *
 * Accessibilité (TalkBack) :
 * - Nœud sémantique unique : citation + auteur + catégorie fusionnés via [clearAndSetSemantics]
 * - Rôle [Role.Button] + action personnalisée "Copier la citation"
 * - Annonce "Citation copiée" via [android.view.View.announceForAccessibility] après copie
 *
 * @param aphorism L'aphorisme à afficher.
 */
@Composable
fun AphorismCard(
    aphorism: Aphorism,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val view = LocalView.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    val copyText = "\"${aphorism.text}\" — ${aphorism.author}"
    val categoryPart = if (aphorism.category.isNotBlank()) ", ${aphorism.category}" else ""
    val a11yDescription = "${aphorism.text}, ${aphorism.author}$categoryPart. Appuyer pour copier la citation."

    fun doCopy() {
        clipboard.setText(AnnotatedString(copyText))
        copied = true
        view.announceForAccessibility("Citation copiée")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = a11yDescription
                role = Role.Button
                customActions = listOf(
                    CustomAccessibilityAction(label = "Copier la citation") { doCopy(); true }
                )
            }
            .clickable { doCopy() },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "“${aphorism.text}”",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "— ${aphorism.author}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )

            if (aphorism.category.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = aphorism.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }

            AnimatedVisibility(
                visible = copied,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Copié !",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
