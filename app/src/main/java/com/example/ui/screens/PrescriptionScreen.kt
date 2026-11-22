package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Battery1Bar
import androidx.compose.material.icons.rounded.Battery3Bar
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DoctorGearConfig
import com.example.data.EnergyLevel
import com.example.data.RehabCatalog
import com.example.data.RxPrescription
import com.example.data.TaskCategory
import com.example.ui.RehabInteractionState
import com.example.ui.UiState
import com.example.ui.components.DoctorInteractionBanner
import com.example.ui.components.RehabGymView

private enum class PrescriptionTabMode(val label: String) {
    PRESCRIPTIONS("療程"),
    REHAB_GYM("復健室")
}

@Composable
fun PrescriptionScreen(
    doctorName: String,
    interaction: RehabInteractionState,
    doctorGear: DoctorGearConfig = DoctorGearConfig(),
    enabledCategories: Set<TaskCategory> = setOf(TaskCategory.DAILY),
    rehabPoints: Long = 0L,
    specialChannelUnlocked: Boolean = false,
    hasSeenRehabGymIntro: Boolean = true,
    onMarkRehabGymIntroSeen: () -> Unit = {},
    onUnlockSpecialChannel: () -> Unit = {},
    onLockSpecialChannel: () -> Unit = {},
    onAddRehabPoints: (Long) -> Unit = {},
    onSetRehabPoints: (Long) -> Unit = {},
    onAccrueIdlePoints: () -> Unit = {},
    onToggleCategory: (TaskCategory) -> Unit = {},
    onPatDoctor: () -> Unit,
    onSelectEnergy: (EnergyLevel) -> Unit,
    onDrawRandom: () -> Unit,
    onAddPrescription: (RxPrescription) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCategories = enabledCategories.ifEmpty { setOf(TaskCategory.DAILY) }
    val filteredList = RehabCatalog.prescriptions.filter {
        it.energyLevel == interaction.selectedEnergyFilter && it.category in activeCategories
    }
    var mode by remember { mutableStateOf(PrescriptionTabMode.PRESCRIPTIONS) }

    if (mode == PrescriptionTabMode.REHAB_GYM) {
        Box(modifier = modifier.fillMaxSize()) {
            RehabGymView(
                doctorName = doctorName,
                rehabPoints = rehabPoints,
                doctorGear = doctorGear,
                specialChannelUnlocked = specialChannelUnlocked,
                hasSeenIntro = hasSeenRehabGymIntro,
                onMarkIntroSeen = onMarkRehabGymIntroSeen,
                onUnlockSpecialChannel = onUnlockSpecialChannel,
                onLockSpecialChannel = onLockSpecialChannel,
                onAddPoints = onAddRehabPoints,
                onSetRehabPoints = onSetRehabPoints,
                onAccrueIdlePoints = onAccrueIdlePoints,
                topControlsInset = 68.dp,
                modifier = Modifier.fillMaxSize()
            )
            PrescriptionModeToggleRow(
                selectedMode = mode,
                onSelectMode = { mode = it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "rx_mode_toggle") {
            PrescriptionModeToggleRow(
                selectedMode = mode,
                onSelectMode = { mode = it }
            )
        }

        if (true) {
            item(key = "rx_doctor_banner") {
                DoctorInteractionBanner(
                    doctorName = doctorName,
                    pose = interaction.prescriptionPose,
                    quote = interaction.prescriptionQuote,
                    patCounter = interaction.prescriptionPatCounter,
                    doctorGear = doctorGear,
                    testTagPrefix = "rx",
                    onPatDoctor = onPatDoctor
                )
            }

            item(key = "rx_header") {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalHospital,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "懶癌復健療程",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    Text(
                        text = "先感覺一下自己現在剩多少電量，讓康復室幫你挑一個剛剛好、不會累的復健療程。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(EnergyLevel.entries, key = { it.wireValue }) { level ->
                            val icon = when (level) {
                                EnergyLevel.LEVEL_1, EnergyLevel.LEVEL_2 -> Icons.Rounded.BatteryAlert
                                EnergyLevel.LEVEL_3, EnergyLevel.LEVEL_4 -> Icons.Rounded.Battery1Bar
                                EnergyLevel.LEVEL_5, EnergyLevel.LEVEL_6 -> Icons.Rounded.Battery3Bar
                                EnergyLevel.LEVEL_7, EnergyLevel.LEVEL_8 -> Icons.Rounded.Battery5Bar
                                EnergyLevel.LEVEL_9, EnergyLevel.LEVEL_10 -> Icons.Rounded.BatteryChargingFull
                            }
                            FilterChip(
                                selected = interaction.selectedEnergyFilter == level,
                                onClick = { onSelectEnergy(level) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(level.shortTag) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("energy_filter_${level.wireValue}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = interaction.selectedEnergyFilter.descriptionZh,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "共 ${filteredList.size} 種推薦",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TaskCategory.entries.forEach { category ->
                            val checked = category in activeCategories
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (checked) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (checked) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onToggleCategory(category) }
                                    .testTag("rx_category_toggle_${category.wireValue}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { onToggleCategory(category) },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = category.labelZh,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                                        color = if (checked) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onDrawRandom,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("draw_random_rx_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Casino,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("選擇困難？幫我隨緣抽一張")
                    }
                }
            }
        }

        interaction.drawnPrescription?.let { picked ->
            item(key = "drawn_${picked.id}") {
                AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "今日隨緣復健療程",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = picked.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "療程叮嚀：${picked.doctorNote}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                            Button(
                                onClick = { onAddPrescription(picked) },
                                enabled = !interaction.isSubmitting,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("accept_drawn_rx_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddTask,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("就決定是你了！加入康復室")
                            }
                        }
                    }
                }
            }
        }

        items(filteredList, key = { it.id }) { prescription ->
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Text(
                                text = "${prescription.effortLevel.labelZh}・${prescription.effortLevel.badgeHint}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = prescription.category.labelZh,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = prescription.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = prescription.doctorNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { onAddPrescription(prescription) },
                        enabled = !interaction.isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("add_rx_${prescription.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddTask,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("加入今日康復")
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun PrescriptionModeToggleRow(
    selectedMode: PrescriptionTabMode,
    onSelectMode: (PrescriptionTabMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rx_mode_toggle")
    ) {
        PrescriptionTabMode.entries.forEachIndexed { index, item ->
            SegmentedButton(
                selected = selectedMode == item,
                onClick = { onSelectMode(item) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = PrescriptionTabMode.entries.size
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
                    inactiveContentColor = MaterialTheme.colorScheme.primary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                ),
                icon = {
                    Icon(
                        imageVector = if (item == PrescriptionTabMode.PRESCRIPTIONS) {
                            Icons.Rounded.LocalHospital
                        } else {
                            Icons.Rounded.FitnessCenter
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag("rx_mode_${item.name.lowercase()}")
            )
        }
    }
}

