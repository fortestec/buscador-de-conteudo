package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.VideoPlatform
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlin.math.roundToLong

private val SUGGESTED_NICHES = listOf(
    "Vídeos de Motoboys Engraçados",
    "Humor e Cotidiano",
    "Tecnologia e IA",
    "Finanças Pessoais",
    "Fitness e Saúde",
    "Marketing e Vendas",
    "Gastronomia",
    "Beleza e Moda",
    "Educação e Carreira"
)

private val TONE_OPTIONS = listOf(
    "Direto e Energético",
    "Didático e Leve",
    "Storytelling Emocional",
    "Humor e Espontâneo",
    "Opinião Forte"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingProfileScreen(
    onSaveProfile: (
        displayName: String,
        niche: String,
        targetAudience: String,
        toneStyle: String,
        preferredPlatforms: List<String>,
        weeklyGoal: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultName = remember {
        Firebase.auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Criador"
    }

    var displayName by remember { mutableStateOf(defaultName) }
    var niche by remember { mutableStateOf("Tecnologia e IA") }
    var targetAudience by remember { mutableStateOf("Pessoas buscando dicas práticas para o dia a dia") }
    var toneStyle by remember { mutableStateOf("Direto e Energético") }
    var selectedPlatforms by remember {
        mutableStateOf(VideoPlatform.entries.map { it.label }.toSet())
    }
    var weeklyGoalSlider by remember { mutableFloatStateOf(5f) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = "CONFIGURAÇÃO INICIAL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Personalize seu Radar Viral",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Esses dados permitem que a IA calcule exatamente quais vídeos em alta têm maior chance de engajar no seu perfil.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(22.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it.take(60) },
                label = { Text("Como quer ser chamado(a)?") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_name_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Seu nicho principal",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SUGGESTED_NICHES.forEach { preset ->
                    val selected = niche.equals(preset, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { niche = preset },
                        label = { Text(preset) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = niche,
                onValueChange = { niche = it.take(80) },
                label = { Text("Ou digite seu nicho") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_niche_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = targetAudience,
                onValueChange = { targetAudience = it.take(160) },
                label = { Text("Quem você quer alcançar? (Público-alvo)") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_audience_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Estilo e tom de comunicação",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TONE_OPTIONS.forEach { tone ->
                    val selected = toneStyle == tone
                    FilterChip(
                        selected = selected,
                        onClick = { toneStyle = tone },
                        label = { Text(tone) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Plataformas onde você posta",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoPlatform.entries.forEach { platform ->
                    val selected = selectedPlatforms.contains(platform.label)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedPlatforms = if (selected && selectedPlatforms.size > 1) {
                                selectedPlatforms - platform.label
                            } else {
                                selectedPlatforms + platform.label
                            }
                        },
                        label = { Text(platform.label) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Meta semanal: ${weeklyGoalSlider.roundToLong()} vídeos por semana",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Slider(
                value = weeklyGoalSlider,
                onValueChange = { weeklyGoalSlider = it },
                valueRange = 1f..14f,
                steps = 12
            )

            if (!validationError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val cleanNiche = niche.trim()
                    val cleanAudience = targetAudience.trim()
                    if (cleanNiche.length < 2) {
                        validationError = "Informe seu nicho (mínimo 2 caracteres)."
                        return@Button
                    }
                    if (cleanAudience.length < 2) {
                        validationError = "Descreva seu público-alvo."
                        return@Button
                    }
                    validationError = null
                    onSaveProfile(
                        displayName.trim().ifBlank { "Criador" },
                        cleanNiche,
                        cleanAudience,
                        toneStyle,
                        selectedPlatforms.toList(),
                        weeklyGoalSlider.roundToLong()
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("save_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ativar Meu Radar Diário",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
