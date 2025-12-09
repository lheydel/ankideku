package com.ankideku.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ankideku.data.remote.llm.LlmHealthStatus
import com.ankideku.data.remote.llm.LlmProvider
import com.ankideku.domain.model.AppTheme
import com.ankideku.domain.model.Settings
import com.ankideku.ui.components.AppButton
import com.ankideku.ui.components.AppButtonVariant
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.theme.LocalAppColors
import com.ankideku.ui.theme.Spacing
import com.ankideku.ui.theme.handPointer

@Composable
fun GeneralTabContent(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    llmHealthStatus: LlmHealthStatus?,
    llmHealthChecking: Boolean,
    onTestConnection: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        AiProviderSection(
            selectedProvider = settings.llmProvider,
            onProviderSelected = { onSettingsChange(settings.copy(llmProvider = it)) },
            healthStatus = llmHealthStatus,
            isChecking = llmHealthChecking,
            onTestConnection = onTestConnection,
        )

        ThemeSection(
            selectedTheme = settings.theme,
            onThemeSelected = { onSettingsChange(settings.copy(theme = it)) },
        )
    }
}

@Composable
private fun AiProviderSection(
    selectedProvider: LlmProvider,
    onProviderSelected: (LlmProvider) -> Unit,
    healthStatus: LlmHealthStatus?,
    isChecking: Boolean,
    onTestConnection: () -> Unit,
) {
    val colors = LocalAppColors.current

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "AI Provider",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surfaceAlt,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AppDropdown(
                            items = LlmProvider.entries.toList(),
                            selectedItem = selectedProvider,
                            onItemSelected = { onProviderSelected(it) },
                            itemLabel = { providerDisplayName(it) },
                            enabled = !isChecking,
                            modifier = Modifier.width(180.dp),
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        Text(
                            text = when {
                                isChecking -> "Checking connection..."
                                healthStatus?.available == true -> healthStatus.info ?: "Connected"
                                healthStatus != null -> healthStatus.error ?: "Connection failed"
                                else -> "Not tested - click Test Connection to verify"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                isChecking -> colors.textSecondary
                                healthStatus?.available == true -> colors.success
                                healthStatus != null -> colors.error
                                else -> colors.warning
                            },
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isChecking -> colors.warning
                                healthStatus?.available == true -> colors.success
                                healthStatus != null -> colors.error
                                else -> colors.warning
                            }
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )

                        AppButton(
                            onClick = onTestConnection,
                            variant = AppButtonVariant.Outlined,
                            enabled = !isChecking,
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(Spacing.sm))
                            }
                            Text("Test Connection")
                        }
                    }
                }

                if (healthStatus != null && !healthStatus.available) {
                    Spacer(Modifier.height(Spacing.sm))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.errorMuted,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = "Error: ${healthStatus.error ?: "Connection failed"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error,
                            )
                            Text(
                                text = "Make sure Claude Code CLI is installed and accessible in your PATH.",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
) {
    val colors = LocalAppColors.current

    Column {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )

        Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppTheme.entries.forEach { theme ->
                FilterChip(
                    selected = selectedTheme == theme,
                    onClick = { onThemeSelected(theme) },
                    modifier = Modifier.handPointer(),
                    label = { Text(themeDisplayName(theme)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colors.surfaceAlt,
                        labelColor = colors.textSecondary,
                        selectedContainerColor = colors.accentMuted,
                        selectedLabelColor = colors.accent,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = colors.border,
                        selectedBorderColor = colors.accent,
                        enabled = true,
                        selected = selectedTheme == theme,
                    ),
                )
            }
        }
    }
}

// Helper functions
internal fun themeDisplayName(theme: AppTheme): String = when (theme) {
    AppTheme.Light -> "Light"
    AppTheme.Dark -> "Dark"
    AppTheme.System -> "System"
}

internal fun providerDisplayName(provider: LlmProvider): String = when (provider) {
    LlmProvider.CLAUDE_CODE -> "Claude Code"
    LlmProvider.MOCK -> "Mock (Testing)"
}
