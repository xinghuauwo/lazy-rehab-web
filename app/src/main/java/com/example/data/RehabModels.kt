package com.example.data

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Keep
@IgnoreExtraProperties
data class RehabProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "康復小夥伴",
    val feifeiNickname: String = "主治醫生",
    val todayDate: String = "",
    val todayCount: Long = 0L,
    val totalCompleted: Long = 0L,
    val antibodyBalance: Long = 0L,
    val ownedItemIds: List<String> = emptyList(),
    val equippedItemIds: List<String> = emptyList(),
    val wardPlacements: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RehabTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val effortLevel: String = EffortLevel.MICRO.wireValue,
    val category: String = TaskCategory.DAILY.wireValue,
    val completed: Boolean = false,
    val completedDate: String = "",
    val note: String = "",
    val sortOrder: Long = 0L,
    val timeSlotsEnabled: Boolean = false,
    val morningDone: Boolean = false,
    val noonDone: Boolean = false,
    val eveningDone: Boolean = false,
    val morningEnabled: Boolean = true,
    val noonEnabled: Boolean = true,
    val eveningEnabled: Boolean = true,
    val resetHour: Long = 0L,
    val deadlineDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun activeTimeSlotIndices(): List<Int> = buildList {
        if (morningEnabled) add(0)
        if (noonEnabled) add(1)
        if (eveningEnabled) add(2)
    }.ifEmpty { listOf(0, 1, 2) }

    fun allEnabledTimeSlotsDone(
        morning: Boolean = morningDone,
        noon: Boolean = noonDone,
        evening: Boolean = eveningDone
    ): Boolean = activeTimeSlotIndices().all { idx ->
        when (idx) {
            0 -> morning
            1 -> noon
            else -> evening
        }
    }
}

enum class TaskCategory(
    val wireValue: String,
    val labelZh: String
) {
    DAILY("daily", "日常"),
    GAME("game", "遊戲"),
    HOMEWORK("homework", "作業"),
    COURSE("course", "課程");

    companion object {
        fun fromWire(raw: String?): TaskCategory =
            entries.firstOrNull { it.wireValue == raw } ?: DAILY

        fun labelFor(raw: String?): String {
            val clean = raw?.trim().orEmpty()
            if (clean.isEmpty()) return DAILY.labelZh
            return entries.firstOrNull { it.wireValue == clean }?.labelZh ?: clean
        }

        fun buildOptions(customCategories: List<String>): List<TaskCategoryOption> {
            val builtIn = entries.map {
                TaskCategoryOption(wireValue = it.wireValue, labelZh = it.labelZh, isBuiltIn = true)
            }
            val custom = customCategories.distinct().mapNotNull { name ->
                val trimmed = name.trim()
                if (trimmed.isEmpty() || entries.any { it.wireValue == trimmed || it.labelZh == trimmed }) {
                    null
                } else {
                    TaskCategoryOption(wireValue = trimmed, labelZh = trimmed, isBuiltIn = false)
                }
            }
            return builtIn + custom
        }
    }
}

data class TaskCategoryOption(
    val wireValue: String,
    val labelZh: String,
    val isBuiltIn: Boolean
)

data class WardDoctorSnapshot(
    val spriteKey: String = "IDLE_A",
    val xFraction: Float = 0.56f,
    val yFraction: Float = 0.72f,
    val facingRight: Boolean = false,
    val anchorId: String = ""
) {
    fun encode(): String {
        val cx = xFraction.coerceIn(0.10f, 0.90f)
        val cy = yFraction.coerceIn(0.28f, 0.90f)
        val safeAnchor = anchorId.take(32).replace(":", "_")
        return String.format(Locale.US, "%s:%.3f:%.3f:%s:%s", spriteKey, cx, cy, facingRight, safeAnchor)
    }

    companion object {
        fun decode(raw: String?): WardDoctorSnapshot {
            if (raw.isNullOrBlank()) return WardDoctorSnapshot()
            val trimmed = raw.trim()
            val parts = trimmed.split(":")
            if (parts.size >= 4) {
                val key = parts[0].ifBlank { "IDLE_A" }
                val x = parts[1].toFloatOrNull()?.coerceIn(0.10f, 0.90f) ?: 0.56f
                val y = parts[2].toFloatOrNull()?.coerceIn(0.28f, 0.90f) ?: 0.72f
                val facing = parts[3].toBooleanStrictOrNull() ?: false
                val anchor = parts.getOrNull(4).orEmpty()
                return WardDoctorSnapshot(key, x, y, facing, anchor)
            }
            val legacySprite = when (trimmed) {
                DoctorPose.SIPPING.name -> "POSE_SIP"
                DoctorPose.APPROVE.name -> "POSE_APPROVE"
                DoctorPose.PHARMACY.name -> "INTERACT"
                else -> "IDLE_A"
            }
            return WardDoctorSnapshot(spriteKey = legacySprite, xFraction = 0.59f, yFraction = 0.73f)
        }
    }
}

@Keep
@IgnoreExtraProperties
data class RehabCheckin(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val energyLevel: String = EnergyLevel.LEVEL_1.wireValue,
    val energyPercent: Int? = null,
    val reflection: String = "",
    val wardSnapshot: List<String> = emptyList(),
    val doctorPoseSnapshot: String = "",
    val completedTaskTitles: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedPercent(): Int {
        val exact = energyPercent
        return if (exact != null && exact in 0..100) {
            exact
        } else {
            EnergyLevel.fromWire(energyLevel).percentValue
        }
    }

    fun formattedEnergyTag(): String {
        val level = EnergyLevel.fromWire(energyLevel)
        val statusText = level.shortTag.substringAfter(' ', level.shortTag)
        return "${resolvedPercent()}% $statusText"
    }
}

enum class EffortLevel(
    val wireValue: String,
    val labelZh: String,
    val badgeHint: String,
    val rewardAntibodies: Long
) {
    MICRO("micro", "超微事", "約 1 分鐘", 15L),
    GENTLE("gentle", "小康復", "約 5 分鐘", 25L),
    BOSS("boss", "大魔王", "建議切小塊", 40L);

    companion object {
        fun fromWire(raw: String?): EffortLevel =
            entries.firstOrNull { it.wireValue == raw } ?: MICRO
    }
}

enum class DoctorPose(
    @DrawableRes val drawableRes: Int,
    val poseLabelZh: String
) {
    OBSERVE(R.drawable.img_mascot_main, "冷面值班"),
    PHARMACY(R.drawable.img_doctor_pharmacy, "藥局發藥"),
    SIPPING(R.drawable.img_doctor_sipping, "偷喝珍奶"),
    APPROVE(R.drawable.img_doctor_approve, "面無表情給讚")
}

enum class PharmacyCategory(val titleZh: String) {
    CONSUMABLE("💊 基礎消耗品"),
    BED_GEAR("🛏️ 病床升級裝備"),
    TREASURE("🏆 鎮院之寶")
}

data class PharmacyItem(
    val id: String,
    val name: String,
    val shortLabel: String,
    val cost: Long,
    val emoji: String,
    @DrawableRes val drawableRes: Int,
    val category: PharmacyCategory,
    val description: String
)

data class WardPlacement(
    val itemId: String,
    val xFraction: Float,
    val yFraction: Float
) {
    fun encode(): String {
        val cx = xFraction.coerceIn(0.08f, 0.92f)
        val cy = yFraction.coerceIn(0.14f, 0.86f)
        return String.format(Locale.US, "%s:%.3f:%.3f", itemId, cx, cy)
    }

    companion object {
        fun decode(raw: String): WardPlacement? {
            val parts = raw.split(":")
            val id = parts.getOrNull(0)?.trim().orEmpty()
            if (id.isEmpty()) return null
            val defaultPos = RehabCatalog.defaultWardPosition(id)
            val x = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0.08f, 0.92f) ?: defaultPos.first
            val y = parts.getOrNull(2)?.toFloatOrNull()?.coerceIn(0.14f, 0.86f) ?: defaultPos.second
            return WardPlacement(id, x, y)
        }
    }
}

data class WardAnchor(
    val id: String,
    val labelZh: String,
    val xFraction: Float,
    val yFraction: Float,
    val isWallSurface: Boolean = false
)

fun RehabProfile?.resolvedWardPlacements(): List<WardPlacement> {
    if (this == null) return emptyList()
    val decoded = wardPlacements.mapNotNull { WardPlacement.decode(it) }
    if (decoded.isNotEmpty()) return decoded
    return equippedItemIds.distinct().map { id ->
        val (dx, dy) = RehabCatalog.defaultWardPosition(id)
        WardPlacement(id, dx, dy)
    }
}

enum class EnergyLevel(
    val wireValue: String,
    val percentValue: Int,
    val percentLabel: String,
    val labelZh: String,
    val shortTag: String,
    val descriptionZh: String
) {
    LEVEL_1("e10", 10, "10%", "電量 10% ・ 完全休眠中", "10% 完全休眠", "棉被與床墊引力達到上限，維持呼吸就算是重大醫學實績"),
    LEVEL_2("e20", 20, "20%", "電量 20% ・ 幾乎沒電中", "20% 幾乎沒電", "只建議執行喝三口水、翻身或眨眼等不費力復健項目"),
    LEVEL_3("e30", 30, "30%", "電量 30% ・ 微弱訊號中", "30% 微弱訊號", "大腦處於省電休眠模式，適合拿著水杯慢慢開機"),
    LEVEL_4("e40", 40, "40%", "電量 40% ・ 勉強探出頭", "40% 勉強開機", "稍微坐得起來一點，可以處理 2 分鐘內超微小雜務"),
    LEVEL_5("e50", 50, "50%", "電量 50% ・ 佛系半電中", "50% 半電待機", "介於想做點事與直接睡死之間，挑一件輕量任務剛剛好"),
    LEVEL_6("e60", 60, "60%", "電量 60% ・ 緩慢充電中", "60% 緩慢充電", "終於成功對抗地心引力，趁機解掉一個卡關開頭吧"),
    LEVEL_7("e70", 70, "70%", "電量 70% ・ 居然有點電", "70% 居然有點電", "罕見的機能在線狀態，做完一點點事就能名正言順躺回床"),
    LEVEL_8("e80", 80, "80%", "電量 80% ・ 機能運作順", "80% 機能正常", "活力高於本院患者平均水準，記得做完要適時補水休養"),
    LEVEL_9("e90", 90, "90%", "電量 90% ・ 醫學小奇蹟", "90% 醫學奇蹟", "警告：活躍度略高，請留意肩頸與腰椎安全以免過度勞累"),
    LEVEL_10("e100", 100, "100%", "電量 100% ・ 滿格怪胎級", "100% 滿格怪胎", "本院百年一見的滿電狀態！去清空大魔王後準時睡覺吧");

    companion object {
        fun fromWire(raw: String?): EnergyLevel = when (raw) {
            "flat" -> LEVEL_1
            "low" -> LEVEL_3
            "steady" -> LEVEL_6
            else -> entries.firstOrNull { it.wireValue == raw } ?: LEVEL_1
        }
    }
}

enum class FeifeiStage(
    val titleZh: String,
    val statusBadge: String,
    val subtitleZh: String,
    @DrawableRes val drawableRes: Int
) {
    FLAT(
        titleZh = "冷面值班發呆中",
        statusBadge = "Lv.0 主治觀察中",
        subtitleZh = "患者生命跡象穩定，本院建議先躺著呼吸就好。",
        drawableRes = R.drawable.img_mascot_main
    ),
    SIT(
        titleZh = "默默翻閱病歷表",
        statusBadge = "Lv.1 微量起色",
        subtitleZh = "竟然動了一點點，病歷上先幫你記一筆奇蹟。",
        drawableRes = R.drawable.img_mascot_main
    ),
    CHEER(
        titleZh = "面無表情微點頭",
        statusBadge = "Lv.2 復健有感",
        subtitleZh = "雖然表情沒變，但本醫師內心對你表示高度肯定。",
        drawableRes = R.drawable.img_mascot_main
    ),
    GLOW(
        titleZh = "開立本日滿分診斷",
        statusBadge = "Lv.3 今日奇蹟",
        subtitleZh = "今天的復健指標已達標，准許你安心去當一塊馬鈴薯。",
        drawableRes = R.drawable.img_mascot_main
    );

    companion object {
        fun fromTodayCount(count: Long): FeifeiStage = when {
            count <= 0L -> FLAT
            count <= 2L -> SIT
            count <= 4L -> CHEER
            else -> GLOW
        }
    }
}

data class RxPrescription(
    val id: String,
    val title: String,
    val effortLevel: EffortLevel,
    val energyLevel: EnergyLevel,
    val doctorNote: String,
    val category: TaskCategory = TaskCategory.DAILY
)

object RehabCatalog {
    val prescriptions: List<RxPrescription>
        get() = PrescriptionCatalog.allPrescriptions

    val starterTasks: List<Triple<String, EffortLevel, String>> = listOf(
        Triple("喝一口水", EffortLevel.MICRO, "本院初診基本醫囑：先確認吞嚥功能正常，潤喉就算實績。"),
        Triple("伸懶腰晃動肩膀 10 秒", EffortLevel.MICRO, "不用站起來，坐著或躺著動十秒都算合格復健。"),
        Triple("打開一件拖延很久的事，只看 2 分鐘", EffortLevel.GENTLE, "嫌太長的話，按右邊「⋮」交給本醫師幫你切小塊。")
    )

    val feifeiQuotes: List<String>
        get() = DoctorQuotes.allDoctorQuotes

    fun taskCompleteQuote(effortLevel: EffortLevel, currentQuote: String): String {
        val pool = when (effortLevel) {
            EffortLevel.MICRO -> DoctorQuotes.taskCompleteMicroQuotes
            EffortLevel.GENTLE -> DoctorQuotes.taskCompleteGentleQuotes
            EffortLevel.BOSS -> DoctorQuotes.taskCompleteBossQuotes
        }
        return pool.filter { it != currentQuote }.randomOrNull() ?: pool.first()
    }

    fun taskUncheckQuote(taskTitle: String, currentQuote: String): String {
        val formattedPool = DoctorQuotes.taskUncheckQuotes.map { template ->
            String.format(template, taskTitle.take(18))
        }
        return formattedPool.filter { it != currentQuote }.randomOrNull() ?: formattedPool.first()
    }

    fun roomQuickRecommendations(categoryWire: String, categoryLabel: String): List<String> {
        return when (categoryWire.trim().lowercase()) {
            TaskCategory.DAILY.wireValue -> listOf(
                "喝三口水",
                "站起來伸懶腰 10 秒",
                "丟掉桌上 1 個垃圾",
                "洗乾淨 1 個馬克杯",
                "深呼吸放空 30 秒",
                "塗護唇膏補充水分"
            )
            TaskCategory.GAME.wireValue -> listOf(
                "領取遊戲每日登入簽到",
                "清完 1 管自動掃蕩體力",
                "清理 5 個遊戲背包雜物",
                "聽 1 首喜歡的遊戲 BGM",
                "幫主畫面角色換個造型",
                "打完這局準時關機喝水"
            )
            TaskCategory.HOMEWORK.wireValue -> listOf(
                "打開作業檔案只看 2 分鐘",
                "新建空白文件打好檔名",
                "畫出題目 1 個關鍵字",
                "寫出第 1 句開頭或 1 題",
                "整理 1 本課本與重點筆",
                "按 Ctrl+S 存檔並備份"
            )
            else -> {
                val label = categoryLabel.trim().ifEmpty { "這區" }
                when {
                    listOf("閱讀", "看書", "書", "小說", "漫畫").any { label.contains(it) } -> listOf(
                        "翻開書本只看 1 頁",
                        "替書架挑 1 本放床頭",
                        "讀完 1 個小段落就停",
                        "把書籤夾回上次那頁"
                    )
                    listOf("運動", "健身", "散步", "拉伸", "瑜珈").any { label.contains(it) } -> listOf(
                        "原地踏步或甩手 15 秒",
                        "轉動脖子與肩膀各 3 圈",
                        "穿好鞋到門外吹風 3 分鐘",
                        "靠牆伸展小腿 20 秒"
                    )
                    listOf("家事", "整理", "打掃", "收納", "清潔").any { label.contains(it) } -> listOf(
                        "收起桌面上 2 件雜物",
                        "把地板顯眼處掃 1 小塊",
                        "整理床邊 20 公分空地",
                        "將髒衣服丟進洗衣籃"
                    )
                    listOf("畫", "創作", "寫", "剪輯", "攝影").any { label.contains(it) } -> listOf(
                        "打開軟體開好空白畫布",
                        "隨手寫或畫 3 筆草稿",
                        "儲存 1 張靈感參考圖",
                        "只做 2 分鐘收尾小修"
                    )
                    listOf("外出", "出門", "採買", "買").any { label.contains(it) } -> listOf(
                        "先確認鑰匙錢包在桌上",
                        "列好要買的 2 樣清單",
                        "換好外出衣服先坐著休息",
                        "走到樓下信箱或巷口就折返"
                    )
                    else -> listOf(
                        "打開「$label」相關頁面看 1 分鐘",
                        "先準備「$label」所需的 1 樣東西",
                        "執行最輕鬆的「$label」第 1 小步",
                        "花 2 分鐘整理「$label」進度"
                    )
                }
            }
        }
    }

    val shrinkSuggestions: List<String>
        get() = DoctorQuotes.shrinkSuggestions

    val pharmacyQuotes: List<String>
        get() = DoctorQuotes.pharmacyQuotes

    val prescriptionDoctorQuotes: List<String>
        get() = DoctorQuotes.prescriptionDoctorQuotes

    val memoryDoctorQuotes: List<String>
        get() = DoctorQuotes.memoryDoctorQuotes

    val pharmacyItems: List<PharmacyItem> = listOf(
        // 基礎消耗品
        PharmacyItem(
            id = "pill_victory",
            name = "精神勝利法膠囊",
            shortLabel = "精神膠囊",
            cost = 30L,
            emoji = "💊",
            drawableRes = R.drawable.img_prop_pill_victory,
            category = PharmacyCategory.CONSUMABLE,
            description = "內含 100% 純天然空氣。吞服後會產生「我今天已經很努力了」的強烈幻覺，無任何副作用。"
        ),
        PharmacyItem(
            id = "long_straw",
            name = "三公尺超長吸管",
            shortLabel = "3m吸管",
            cost = 50L,
            emoji = "🥤",
            drawableRes = R.drawable.img_prop_long_straw,
            category = PharmacyCategory.CONSUMABLE,
            description = "專為連坐起來喝水都覺得浪費生命的患者設計，從床頭一路延伸至書桌水杯，實現真正的躺平補水。"
        ),
        PharmacyItem(
            id = "procrastination_permit",
            name = "合法拖延許可證",
            shortLabel = "拖延許可",
            cost = 80L,
            emoji = "📜",
            drawableRes = R.drawable.img_prop_procrastination_permit,
            category = PharmacyCategory.CONSUMABLE,
            description = "蓋有主治醫生鋼印的官方文件。出示此證可理直氣壯地將任何非緊急事項推遲到「明天再說」。"
        ),
        // 病床升級裝備
        PharmacyItem(
            id = "antigravity_pillow",
            name = "反重力記憶枕",
            shortLabel = "記憶枕",
            cost = 120L,
            emoji = "🛏️",
            drawableRes = R.drawable.img_prop_antigravity_pillow,
            category = PharmacyCategory.BED_GEAR,
            description = "擁有強大的磁場，一旦頭部接觸枕面，將會產生高達 10 倍的地心引力，讓你與床鋪合而為一。"
        ),
        PharmacyItem(
            id = "antisocial_blanket",
            name = "防社交結界被單",
            shortLabel = "結界被單",
            cost = 180L,
            emoji = "🫧",
            drawableRes = R.drawable.img_prop_antisocial_blanket,
            category = PharmacyCategory.BED_GEAR,
            description = "只要將此被單拉過頭頂，即可自動屏蔽外界所有的訊息通知、未接來電與人類社交需求。"
        ),
        PharmacyItem(
            id = "boba_iv_drip",
            name = "生理食鹽水（其實是手搖飲）點滴架",
            shortLabel = "珍奶點滴",
            cost = 250L,
            emoji = "🧋",
            drawableRes = R.drawable.img_prop_boba_iv_drip,
            category = PharmacyCategory.BED_GEAR,
            description = "透過靜脈注射直接將全糖珍珠奶茶輸送至體內，省去咀嚼與吞嚥的體力消耗。"
        ),
        PharmacyItem(
            id = "auto_turn_arm",
            name = "自動翻身機械臂",
            shortLabel = "翻身機械臂",
            cost = 320L,
            emoji = "🦾",
            drawableRes = R.drawable.img_prop_auto_turn_arm,
            category = PharmacyCategory.BED_GEAR,
            description = "每隔兩小時自動幫患者翻面一次，有效預防長期維持同一姿勢滑手機造成的單側手臂麻痺。"
        ),
        // 鎮院之寶
        PharmacyItem(
            id = "silent_alarm",
            name = "永遠不會響的鬧鐘",
            shortLabel = "靜音鬧鐘",
            cost = 500L,
            emoji = "🔕",
            drawableRes = R.drawable.img_prop_silent_alarm,
            category = PharmacyCategory.TREASURE,
            description = "拆除了所有發聲零件的高科技工藝品，每天早上準時用「絕對的安靜」溫柔喚醒你的自然醒。"
        ),
        PharmacyItem(
            id = "miracle_banner",
            name = "醫學奇蹟錦旗",
            shortLabel = "居然活著",
            cost = 800L,
            emoji = "🚩",
            drawableRes = R.drawable.img_prop_miracle_banner,
            category = PharmacyCategory.TREASURE,
            description = "掛在病床正上方的金色錦旗，上面繡著四個大字：「居然活著」，是本院最高榮譽象徵。"
        ),
        PharmacyItem(
            id = "vip_deed",
            name = "VIP 終身免費床位地契",
            shortLabel = "VIP地契",
            cost = 1500L,
            emoji = "👑",
            drawableRes = R.drawable.img_prop_vip_deed,
            category = PharmacyCategory.TREASURE,
            description = "恭喜你放棄治療並決定在此定居！持有此地契可永久享有本院軟爛 VIP 待遇，永遠不用出院。"
        )
    )

    val wardAnchors: List<WardAnchor> = listOf(
        WardAnchor("anchor_wall_left", "床頭上方牆", 0.34f, 0.25f, isWallSurface = true),
        WardAnchor("anchor_wall_mid", "病歷牆區", 0.52f, 0.30f, isWallSurface = true),
        WardAnchor("anchor_window_sill", "陽光窗台", 0.74f, 0.38f, isWallSurface = false),
        WardAnchor("anchor_nightstand", "床頭小桌", 0.445f, 0.435f, isWallSurface = false),
        WardAnchor("anchor_cabinet_top", "旁邊藥櫃", 0.605f, 0.425f, isWallSurface = false),
        WardAnchor("anchor_bed_pillow", "病床枕頭", 0.305f, 0.485f, isWallSurface = false),
        WardAnchor("anchor_bed_blanket", "病床棉被", 0.405f, 0.575f, isWallSurface = false),
        WardAnchor("anchor_bed_iv", "點滴架位", 0.225f, 0.585f, isWallSurface = false),
        WardAnchor("anchor_chair_side", "陪病椅", 0.845f, 0.655f, isWallSurface = false),
        WardAnchor("anchor_bed_front", "床邊地板", 0.44f, 0.755f, isWallSurface = false),
        WardAnchor("anchor_rug_center", "圓毯區", 0.575f, 0.725f, isWallSurface = false)
    )

    fun anchorById(id: String): WardAnchor? = wardAnchors.firstOrNull { it.id == id }

    fun nearestWardAnchor(xFraction: Float, yFraction: Float): WardAnchor {
        return wardAnchors.minByOrNull { anchor ->
            val dx = anchor.xFraction - xFraction
            val dy = anchor.yFraction - yFraction
            dx * dx + dy * dy
        } ?: wardAnchors.first()
    }

    /**
     * Tight geometry-aware drop recognition for the 2.5D isometric ward background (952x840).
     * Returns null on open empty floor/wall gaps so random doctor drops stay unanchored
     * and trigger playful floor-drop dialogues instead of aggressively snapping to furniture.
     */
    fun resolveWardDropAnchorOrNull(xFraction: Float, yFraction: Float): WardAnchor? {
        val id = when {
            // 1. Visitor chair (right front)
            xFraction in 0.77f..0.93f && yFraction in 0.51f..0.75f -> "anchor_chair_side"
            // 2. Side pink 2-drawer medicine cabinet (top + drawers)
            xFraction in 0.54f..0.68f && yFraction in 0.35f..0.52f -> "anchor_cabinet_top"
            // 3. Bedside white nightstand with lamp & tissue box
            xFraction in 0.40f..0.50f && yFraction in 0.35f..0.45f -> "anchor_nightstand"
            // 4. IV drip stand on left side of bed
            xFraction in 0.14f..0.245f && yFraction in 0.42f..0.70f -> "anchor_bed_iv"
            // 5. Hospital bed pillow / headboard zone
            xFraction in 0.25f..0.38f && yFraction in 0.40f..0.52f -> "anchor_bed_pillow"
            // 6. Hospital bed pink blanket / mattress zone
            xFraction in 0.27f..0.55f && yFraction in 0.50f..0.67f -> "anchor_bed_blanket"
            // 7. Window sill with succulent plant
            xFraction in 0.69f..0.86f && yFraction in 0.27f..0.44f -> "anchor_window_sill"
            // 8. Wall poster / monitor spots
            xFraction in 0.18f..0.40f && yFraction in 0.18f..0.33f -> "anchor_wall_left"
            xFraction in 0.44f..0.62f && yFraction in 0.18f..0.33f -> "anchor_wall_mid"
            // 9. Tight floor/rug spots (leaving surrounding open floor free for random drops!)
            kotlin.math.abs(xFraction - 0.44f) <= 0.055f && kotlin.math.abs(yFraction - 0.755f) <= 0.045f -> "anchor_bed_front"
            kotlin.math.abs(xFraction - 0.575f) <= 0.06f && kotlin.math.abs(yFraction - 0.725f) <= 0.045f -> "anchor_rug_center"
            else -> null
        }
        return id?.let { anchorById(it) }
    }

    fun resolveWardDropAnchor(xFraction: Float, yFraction: Float): WardAnchor =
        resolveWardDropAnchorOrNull(xFraction, yFraction) ?: nearestWardAnchor(xFraction, yFraction)

    fun defaultWardPosition(itemId: String): Pair<Float, Float> = when (itemId) {
        "miracle_banner" -> 0.34f to 0.25f
        "vip_deed" -> 0.52f to 0.30f
        "procrastination_permit" -> 0.605f to 0.425f
        "boba_iv_drip" -> 0.225f to 0.585f
        "antigravity_pillow" -> 0.305f to 0.485f
        "antisocial_blanket" -> 0.405f to 0.575f
        "auto_turn_arm" -> 0.44f to 0.755f
        "long_straw" -> 0.575f to 0.725f
        "pill_victory" -> 0.74f to 0.38f
        "silent_alarm" -> 0.445f to 0.435f
        else -> 0.575f to 0.725f
    }

    val wardDoctorWanderLines: List<String>
        get() = DoctorQuotes.wardDoctorWanderLines

    val wardDoctorTapLines: List<String>
        get() = DoctorQuotes.wardDoctorTapLines

    val wardDoctorDragStartLines: List<String>
        get() = DoctorQuotes.wardDoctorDragStartLines

    fun wardDoctorAnchorDropLine(anchorId: String, currentLine: String): String {
        val pool = DoctorQuotes.wardDoctorDropLinesByAnchor[anchorId]
            ?: DoctorQuotes.wardDoctorDropLinesByAnchor.getValue("anchor_rug_center")
        val filtered = pool.filter { it != currentLine }
        return filtered.randomOrNull() ?: pool.first()
    }

    val doctorEquipReactions: List<String>
        get() = DoctorQuotes.doctorEquipReactions

    fun wardItemInteractionLine(targetId: String): String =
        DoctorQuotes.wardItemInteractionLine(targetId)

    fun wardDoctorRandomDropLine(currentLine: String): String {
        val pool = DoctorQuotes.wardDoctorRandomDropLines
        val filtered = pool.filter { it != currentLine }
        return filtered.randomOrNull() ?: pool.first()
    }

    fun taskDeleteQuote(taskTitle: String, currentQuote: String): String {
        val safeTitle = taskTitle.trim().ifEmpty { "這項待辦" }
        val pool = DoctorQuotes.taskDeleteQuotes.map { String.format(Locale.US, it, safeTitle) }
        val filtered = pool.filter { it != currentQuote }
        return filtered.randomOrNull() ?: pool.first()
    }

    fun homeworkDeadlineReminder(tasks: List<RehabTask>, todayDateKey: String): String? {
        val pendingHomework = tasks.filter {
            it.category == TaskCategory.HOMEWORK.wireValue && !it.completed && it.deadlineDate.isNotBlank()
        }
        if (pendingHomework.isEmpty()) return null
        val scored = pendingHomework.mapNotNull { task ->
            val diff = daysUntilDateKey(task.deadlineDate, todayDateKey) ?: return@mapNotNull null
            task to diff
        }.sortedBy { it.second }
        val urgent = scored.firstOrNull() ?: return null
        val (task, daysLeft) = urgent
        return when {
            daysLeft < 0 -> DoctorQuotes.homeworkDeadlineOverdueQuotes.first()
            daysLeft == 0 -> String.format(Locale.US, DoctorQuotes.homeworkDeadlineTodayQuotes.first(), task.title)
            daysLeft in 1..3 -> String.format(Locale.US, DoctorQuotes.homeworkDeadlineSoonQuotes.first(), task.title, daysLeft)
            else -> null
        }
    }

    val specialCgItems: List<SpecialCgItem> = listOf(
        SpecialCgItem(
            id = "cg_rehab_nap",
            title = "午間偷閒小憩",
            subtitle = "樹屋診療室的陽光午睡",
            costPoints = 120L,
            drawableRes = R.drawable.img_cg_rehab_nap,
            description = "原本只是說『替你確認抱枕支撐度』，結果連主治醫生自己都趴在沙發上瞇了十分鐘。"
        ),
        SpecialCgItem(
            id = "cg_rehab_boba",
            title = "全糖珍奶特調時間",
            subtitle = "茶水間的隱藏醫囑福利",
            costPoints = 180L,
            drawableRes = R.drawable.img_cg_rehab_boba,
            description = "病歷上寫的是『補充水分與熱量』，實際上是主治醫生順手多買了一杯微冰全糖分給你。"
        ),
        SpecialCgItem(
            id = "cg_rehab_sunlight",
            title = "窗邊光合作用紀念",
            subtitle = "不用出門的午後暖光",
            costPoints = 240L,
            drawableRes = R.drawable.img_cg_rehab_sunlight,
            description = "窗外風很輕、病房很安靜，不需要趕著成為誰——今天也順利地被溫暖日光輕輕接住了。"
        ),
        SpecialCgItem(
            id = "cg_rehab_treehouse",
            title = "樹屋診療室的日常",
            subtitle = "專屬病歷紀錄的一隅",
            costPoints = 300L,
            drawableRes = R.drawable.img_cg_rehab_treehouse,
            description = "沙發上的小花貓睡得比患者還沉，主治醫生低頭替你翻看今天的復健紀錄——只要待在這裡，好像什麼都可以慢慢來。"
        )
    )
}

enum class DoctorCoatStyle(
    val id: String,
    val titleZh: String,
    val badgeZh: String,
    val descriptionZh: String
) {
    CLASSIC_WHITE(
        id = "classic_white",
        titleZh = "標準值班白袍",
        badgeZh = "預設款",
        descriptionZh = "乾淨俐落的基礎白袍，口袋裡只裝著無奈與下班願望。"
    ),
    STAR_DIRECTOR(
        id = "star_director",
        titleZh = "星輝摸魚白袍",
        badgeZh = "金色徽章",
        descriptionZh = "領口別上金色摸魚星星章，口袋插著開不出嚴厲醫囑的星星筆。"
    ),
    CAT_PAW_COAT(
        id = "cat_paw_coat",
        titleZh = "貓掌口袋白袍",
        badgeZh = "肉球縫線",
        descriptionZh = "口袋繡著雙倍貓掌肉墊＋領口粉紅小啾啾，專治心情低落患者。"
    ),
    PASTEL_MINT_COAT(
        id = "pastel_mint_coat",
        titleZh = "薄荷休診長袍",
        badgeZh = "休診滾邊",
        descriptionZh = "縫上本院療癒薄荷綠滾邊與『休診發呆中』名牌的特製長袍。"
    );

    companion object {
        fun fromId(id: String?): DoctorCoatStyle =
            entries.firstOrNull { it.id == id } ?: CLASSIC_WHITE
    }
}

enum class DoctorStethoscopeStyle(
    val id: String,
    val titleZh: String,
    val badgeZh: String,
    val descriptionZh: String
) {
    DEFAULT_HEART(
        id = "default_heart",
        titleZh = "粉心基礎聽診器",
        badgeZh = "預設款",
        descriptionZh = "能聽見心跳與肚子餓聲音的普通粉心聽診器。"
    ),
    GOLD_VIP(
        id = "gold_vip",
        titleZh = "黃金特級聽診器",
        badgeZh = "特別版",
        descriptionZh = "香檳金導管搭配閃亮星芒聽診頭，專門鑑定患者是否合格耍廢。"
    ),
    CAT_BELL(
        id = "cat_bell",
        titleZh = "貓耳鈴鐺聽診器",
        badgeZh = "療癒系",
        descriptionZh = "淺藍耳管配上會輕輕搖晃的金色小鈴鐺與粉嫩貓耳聽診頭。"
    ),
    BOBA_SCOPE(
        id = "boba_scope",
        titleZh = "珍奶續命聽診器",
        badgeZh = "全糖微冰",
        descriptionZh = "焦糖奶茶色導管加上珍珠奶茶造型聽診頭，隨時偵測今日含糖量。"
    );

    companion object {
        fun fromId(id: String?): DoctorStethoscopeStyle =
            entries.firstOrNull { it.id == id } ?: DEFAULT_HEART
    }
}

enum class DoctorHeadwearStyle(
    val id: String,
    val titleZh: String,
    val badgeZh: String,
    val descriptionZh: String
) {
    NONE(
        id = "none",
        titleZh = "無頭飾",
        badgeZh = "預設款",
        descriptionZh = "一頭蓬鬆冷靜的白髮原裝上陣，保持最經典的醫院看診氣場。"
    ),
    CAT_EARS(
        id = "cat_ears",
        titleZh = "毛絨白貓耳",
        badgeZh = "貓咪執照",
        descriptionZh = "戴上瞬間看起來依然面無表情，但摸魚說服力莫名提升 200%。"
    ),
    HEAD_MIRROR(
        id = "head_mirror",
        titleZh = "刻板印象醫生光碟片",
        badgeZh = "復古醫材",
        descriptionZh = "閃亮亮的銀色額鏡圓盤，雖然今天也沒要照耳朵，戴著比較像有上班。"
    ),
    FLOWER_CROWN(
        id = "flower_crown",
        titleZh = "春日小花環",
        badgeZh = "療癒植栽",
        descriptionZh = "洋甘菊與柔和粉藍小花編成的花環，專治連看病歷都覺得累的下午。"
    ),
    PAW_CLIP(
        id = "paw_clip",
        titleZh = "粉紅貓爪髮夾",
        badgeZh = "側髮點綴",
        descriptionZh = "悄悄夾在瀏海旁的軟Q肉球髮夾，只有仔細靠近才看得出來的反差配件。"
    );

    companion object {
        fun fromId(id: String?): DoctorHeadwearStyle =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

data class DoctorGearConfig(
    val coat: DoctorCoatStyle = DoctorCoatStyle.CLASSIC_WHITE,
    val stethoscope: DoctorStethoscopeStyle = DoctorStethoscopeStyle.DEFAULT_HEART,
    val headwear: DoctorHeadwearStyle = DoctorHeadwearStyle.NONE
)

data class CourseScheduleEntry(
    val id: String,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val startTime: String,
    val endTime: String,
    val title: String,
    val location: String = ""
) {
    fun encode(): String {
        val safeTitle = title.replace("|", " ").replace("\n", " ").trim()
        val safeLoc = location.replace("|", " ").replace("\n", " ").trim()
        return "$id|$dayOfWeek|$startTime|$endTime|$safeTitle|$safeLoc"
    }

    companion object {
        fun decode(raw: String): CourseScheduleEntry? {
            val parts = raw.split("|")
            if (parts.size < 5) return null
            val id = parts[0].trim().ifEmpty { return null }
            val day = parts[1].toIntOrNull()?.coerceIn(1, 7) ?: 1
            val start = parts[2].trim().ifEmpty { "09:00" }
            val end = parts[3].trim().ifEmpty { "10:00" }
            val title = parts[4].trim().ifEmpty { return null }
            val loc = parts.getOrNull(5)?.trim().orEmpty()
            return CourseScheduleEntry(id, day, start, end, title, loc)
        }
    }
}

enum class TimetableCellColor(
    val id: String,
    val labelZh: String,
    val bgArgb: Long,
    val fgArgb: Long
) {
    NONE("none", "奶油白", 0xFFFFFDFDL, 0xFF4B3532L),
    SAKURA("sakura", "櫻花粉", 0xFFFCE4ECL, 0xFF7B3F52L),
    PEACH("peach", "蜜桃奶", 0xFFFFEAD9L, 0xFF6D4433L),
    SKY("sky", "晴空藍", 0xFFE3F0FCL, 0xFF2C567AL),
    OCEAN("ocean", "寧靜藍", 0xFFD4E6FAL, 0xFF244B6EL),
    MINT("mint", "薄荷綠", 0xFFE2F3ECL, 0xFF2E614DL),
    LAVENDER("lavender", "薰衣草", 0xFFEFE7FBL, 0xFF543E78L);

    companion object {
        fun fromId(id: String?): TimetableCellColor =
            entries.firstOrNull { it.id == id } ?: SKY
    }
}

data class TimetablePeriodRow(
    val id: String,
    val label: String,
    val startTime: String,
    val endTime: String
)

data class TimetableCellEntry(
    val periodId: String,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val title: String,
    val note: String = "",
    val colorId: String = TimetableCellColor.SKY.id
)

data class CustomTimetableConfig(
    val tableTitle: String = "我的每週課表／班表",
    val cornerLabel: String = "節次",
    val periods: List<TimetablePeriodRow> = defaultPeriods(),
    val cells: List<TimetableCellEntry> = emptyList()
) {
    companion object {
        fun defaultPeriods(): List<TimetablePeriodRow> = listOf(
            TimetablePeriodRow("p1", "1", "08:10", "09:00"),
            TimetablePeriodRow("p2", "2", "09:10", "10:00"),
            TimetablePeriodRow("p3", "3", "10:10", "11:00"),
            TimetablePeriodRow("p4", "4", "11:10", "12:00"),
            TimetablePeriodRow("p5", "5\n(中午)", "12:10", "13:00"),
            TimetablePeriodRow("p6", "6", "13:10", "14:00"),
            TimetablePeriodRow("p7", "7", "14:10", "15:00"),
            TimetablePeriodRow("p8", "8", "15:10", "16:00"),
            TimetablePeriodRow("p9", "9", "16:10", "17:00")
        )
    }
}

fun isEarly8AmStartTime(timeText: String): Boolean {
    val match = Regex("""(\d{1,2})\s*[:：]\s*(\d{2})""").find(timeText.trim()) ?: return false
    val hour = match.groupValues[1].toIntOrNull() ?: return false
    return hour == 8
}

data class CalendarEvent(
    val id: String,
    val dateKey: String, // yyyy-MM-dd
    val timeText: String,
    val title: String,
    val note: String = ""
) {
    fun encode(): String {
        val safeTitle = title.replace("|", " ").replace("\n", " ").trim()
        val safeNote = note.replace("|", " ").replace("\n", " ").trim()
        return "$id|$dateKey|$timeText|$safeTitle|$safeNote"
    }

    companion object {
        fun decode(raw: String): CalendarEvent? {
            val parts = raw.split("|")
            if (parts.size < 4) return null
            val id = parts[0].trim().ifEmpty { return null }
            val dateKey = parts[1].trim().ifEmpty { return null }
            val timeText = parts[2].trim().ifEmpty { "全日" }
            val title = parts[3].trim().ifEmpty { return null }
            val note = parts.getOrNull(4)?.trim().orEmpty()
            return CalendarEvent(id, dateKey, timeText, title, note)
        }
    }
}

data class SpecialCgItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val costPoints: Long,
    @DrawableRes val drawableRes: Int,
    val description: String
)

fun currentTodayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date(nowMillis))
}

fun lastResetBoundaryMillis(
    categoryWire: String,
    resetHour: Long,
    nowMillis: Long = System.currentTimeMillis()
): Long {
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = nowMillis
        val targetHour = if (categoryWire == TaskCategory.GAME.wireValue) {
            resetHour.coerceIn(0L, 23L).toInt()
        } else {
            0
        }
        set(java.util.Calendar.HOUR_OF_DAY, targetHour)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        if (timeInMillis > nowMillis) {
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
    }
    return cal.timeInMillis
}

fun effectiveCompletionDateKey(
    categoryWire: String,
    resetHour: Long,
    nowMillis: Long = System.currentTimeMillis()
): String {
    if (categoryWire != TaskCategory.GAME.wireValue) {
        return currentTodayDateKey(nowMillis)
    }
    val safeHour = resetHour.coerceIn(0L, 23L).toInt()
    if (safeHour == 0) {
        return currentTodayDateKey(nowMillis)
    }
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = nowMillis
        if (get(java.util.Calendar.HOUR_OF_DAY) < safeHour) {
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
    }
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

fun shouldResetTask(
    categoryWire: String,
    resetHour: Long,
    completedDate: String,
    updatedMillis: Long,
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    if (categoryWire == TaskCategory.HOMEWORK.wireValue) return false
    val todayKey = currentTodayDateKey(nowMillis)
    if (categoryWire == TaskCategory.GAME.wireValue) {
        val safeHour = resetHour.coerceIn(0L, 23L)
        if (safeHour == 0L) {
            return completedDate != todayKey
        }
        val expectedKey = effectiveCompletionDateKey(categoryWire, safeHour, nowMillis)
        if (completedDate == expectedKey) return false
        val boundaryMillis = lastResetBoundaryMillis(categoryWire, safeHour, nowMillis)
        return when {
            updatedMillis > 0L -> updatedMillis < boundaryMillis
            completedDate.isBlank() -> true
            else -> completedDate != expectedKey
        }
    }
    return completedDate != todayKey
}

fun daysUntilDateKey(deadlineDate: String, todayDateKey: String = currentTodayDateKey()): Int? {
    if (deadlineDate.isBlank()) return null
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val target = formatter.parse(deadlineDate) ?: return null
        val today = formatter.parse(todayDateKey) ?: return null
        val diffMillis = target.time - today.time
        (diffMillis / 86_400_000L).toInt()
    } catch (_: Exception) {
        null
    }
}

fun currentDayOfWeekMonToSun(): Int {
    val cal = java.util.Calendar.getInstance()
    return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY -> 1
        java.util.Calendar.TUESDAY -> 2
        java.util.Calendar.WEDNESDAY -> 3
        java.util.Calendar.THURSDAY -> 4
        java.util.Calendar.FRIDAY -> 5
        java.util.Calendar.SATURDAY -> 6
        else -> 7
    }
}

