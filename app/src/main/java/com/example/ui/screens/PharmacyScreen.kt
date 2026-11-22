package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DoctorGearConfig
import com.example.data.DoctorPose
import com.example.data.GuestRehabStore
import com.example.data.OtomeClinicTheme
import com.example.data.PharmacyCategory
import com.example.data.PharmacyItem
import com.example.data.RehabCatalog
import com.example.data.RehabProfile
import com.example.data.RehabTask
import com.example.ui.UiState
import com.example.ui.components.DoctorAvatarWithGear
import com.example.ui.components.OtomeCounselingView
import kotlinx.coroutines.delay

private enum class PharmacyMode(val label: String) {
    SUPPLY("院內免費道具領取處"),
    COUNSELING("心理診療室")
}

@Composable
fun PharmacyScreen(
    profileState: UiState<RehabProfile?>,
    tasksState: UiState<List<RehabTask>>,
    todayDateKey: String,
    pharmacyPose: DoctorPose,
    pharmacyQuote: String,
    pharmacyPatCounter: Int,
    doctorGear: DoctorGearConfig = DoctorGearConfig(),
    clinicProgress: GuestRehabStore.OtomeClinicProgress = GuestRehabStore.OtomeClinicProgress(),
    clinicTheme: OtomeClinicTheme = OtomeClinicTheme.TREEHOUSE,
    isSubmitting: Boolean,
    onPatPharmacyDoctor: () -> Unit,
    onRedeemItem: (PharmacyItem) -> Unit,
    onToggleEquipItem: (PharmacyItem) -> Unit,
    onSelectClinicTheme: (OtomeClinicTheme) -> Unit = {},
    onRecordClinicInteraction: () -> Unit = {},
    onRecordSecretLetterGiven: () -> Unit = {},
    onSaveDrawnHeartRx: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val ownedIds = profile?.ownedItemIds?.toSet().orEmpty()
    val equippedIds = profile?.equippedItemIds?.toSet().orEmpty()
    val doctorName = profile?.feifeiNickname?.ifBlank { "主治醫生" } ?: "主治醫生"
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val totalCompleted = profile?.totalCompleted ?: 0L

    var pharmacyMode by remember { mutableStateOf(PharmacyMode.SUPPLY) }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("pharmacy_mode_toggle")
        ) {
            PharmacyMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = pharmacyMode == mode,
                    onClick = { pharmacyMode = mode },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PharmacyMode.entries.size
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        inactiveContentColor = MaterialTheme.colorScheme.primary,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        inactiveBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    ),
                    icon = {
                        Icon(
                            imageVector = if (mode == PharmacyMode.SUPPLY) {
                                Icons.Rounded.LocalPharmacy
                            } else {
                                Icons.Rounded.Favorite
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = mode.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.testTag("pharmacy_mode_${mode.name.lowercase()}")
                )
            }
        }

        if (pharmacyMode == PharmacyMode.SUPPLY) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "pharmacy_doctor_counter") {
                    PharmacyDoctorCounterCard(
                        doctorName = doctorName,
                        pose = pharmacyPose,
                        quote = pharmacyQuote,
                        patCounter = pharmacyPatCounter,
                        doctorGear = doctorGear,
                        ownedCount = ownedIds.size,
                        totalCount = RehabCatalog.pharmacyItems.size,
                        onPatDoctor = onPatPharmacyDoctor
                    )
                }

                PharmacyCategory.entries.forEach { category ->
                    val categoryItems = RehabCatalog.pharmacyItems.filter { it.category == category }
                    item(key = "cat_header_${category.name}") {
                        Text(
                            text = category.titleZh,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                        )
                    }

                    items(categoryItems, key = { it.id }) { item ->
                        val isOwned = item.id in ownedIds
                        val isEquipped = item.id in equippedIds
                        PharmacyItemCard(
                            item = item,
                            isOwned = isOwned,
                            isEquipped = isEquipped,
                            isSubmitting = isSubmitting,
                            onRedeem = { onRedeemItem(item) },
                            onToggleEquip = { onToggleEquipItem(item) }
                        )
                    }
                }
            }
        } else {
            OtomeCounselingView(
                doctorName = doctorName,
                tasks = tasks,
                totalCompleted = totalCompleted,
                todayDateKey = todayDateKey,
                clinicProgress = clinicProgress,
                clinicTheme = clinicTheme,
                onSelectClinicTheme = onSelectClinicTheme,
                onRecordClinicInteraction = onRecordClinicInteraction,
                onRecordSecretLetterGiven = onRecordSecretLetterGiven,
                onSaveDrawnHeartRx = onSaveDrawnHeartRx,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PharmacyDoctorCounterCard(
    doctorName: String,
    pose: DoctorPose,
    quote: String,
    patCounter: Int,
    doctorGear: DoctorGearConfig,
    ownedCount: Int,
    totalCount: Int,
    onPatDoctor: () -> Unit
) {
    var bouncing by remember { mutableStateOf(false) }

    LaunchedEffect(patCounter) {
        if (patCounter > 0) {
            bouncing = true
            delay(180L)
            bouncing = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pharmacyFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pharmacyFloatY"
    )

    val scale by animateFloatAsState(
        targetValue = if (bouncing) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pharmacyBounce"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalPharmacy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "院內免費道具領取處",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "圖鑑 $ownedCount / $totalCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onPatDoctor
                    )
                    .testTag("pharmacy_doctor_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = pose,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "pharmacyPoseTransition"
                        ) { currentPose ->
                            DoctorAvatarWithGear(
                                drawableRes = currentPose.drawableRes,
                                gear = doctorGear,
                                contentDescription = "${doctorName}藥局動作：${currentPose.poseLabelZh}",
                                modifier = Modifier
                                    .height(106.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationY = floatY
                                    }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLowest
                            ) {
                                Text(
                                    text = doctorName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "「$quote」",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "點一下醫師切換動作",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PharmacyItemCard(
    item: PharmacyItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    isSubmitting: Boolean,
    onRedeem: () -> Unit,
    onToggleEquip: () -> Unit
) {
    val containerColor = when {
        isEquipped -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isEquipped) 1.5.dp else 1.dp,
            color = if (isEquipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pharmacy_item_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isOwned) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = item.drawableRes),
                            contentDescription = item.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOwned) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "已擁有",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "免費領取",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isOwned) {
                if (isEquipped) {
                    Button(
                        onClick = onToggleEquip,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("equip_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bed,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("已擺進病房（點此收回）")
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleEquip,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("equip_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "擺進病房／病床旁",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Button(
                    onClick = onRedeem,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("redeem_item_${item.id}")
                ) {
                    Text(text = "領取並擺進病房")
                }
            }
        }
    }
}
