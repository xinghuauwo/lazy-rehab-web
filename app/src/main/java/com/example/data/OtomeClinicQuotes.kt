package com.example.data

import androidx.annotation.DrawableRes
import com.example.R
import org.json.JSONObject

enum class OtomeDoctorExpression(
    @DrawableRes val drawableRes: Int,
    val moodTagZh: String
) {
    COOL(R.drawable.img_otome_doc_cool, "冷面看診中"),
    GENTLE(R.drawable.img_otome_doc_gentle, "珍奶休診中"),
    NIGHT(R.drawable.img_otome_doc_night, "深夜值班發呆中"),
    SIGH(R.drawable.img_otome_doc_sigh, "捏眉心無奈中"),
    POINT(R.drawable.img_otome_doc_point, "拿筆畫重點中"),
    MUG(R.drawable.img_otome_doc_mug, "捧熱茶放空中"),
    NECK(R.drawable.img_otome_doc_neck, "值班揉脖子中")
}

enum class OtomeClinicTheme(
    val id: String,
    val titleZh: String,
    val subtitleZh: String,
    val drawableName: String,
    @DrawableRes val drawableRes: Int,
    val ambientOverlayHex: Long,
    val accentHex: Long,
    val switchDoctorReply: String
) {
    TREEHOUSE(
        id = "treehouse",
        titleZh = "原木樹屋",
        subtitleZh = "木質診間・樹洞玩偶與貓咪沙發",
        drawableName = "img_otome_clinic_bg",
        drawableRes = R.drawable.img_otome_clinic_bg,
        ambientOverlayHex = 0x14FFF5F7,
        accentHex = 0xFFE56B8D,
        switchDoctorReply = "還是原本這間木質樹屋最習慣吧？牆上那群熊玩偶替我們顧著門口呢。"
    ),
    WARM_STUDY(
        id = "warm_study",
        titleZh = "暖光書房",
        subtitleZh = "暮色檯燈・皮革沙發與靜謐書牆",
        drawableName = "img_otome_bg_study",
        drawableRes = R.drawable.img_otome_bg_study,
        ambientOverlayHex = 0x1E2E1C10,
        accentHex = 0xFFD97952,
        switchDoctorReply = "換成暖黃檯燈的書房布置了。光線暗一點比較不刺眼，想偷打瞌睡也沒問題。"
    ),
    AFTERNOON_BALCONY(
        id = "afternoon_balcony",
        titleZh = "午後陽台",
        subtitleZh = "金色日光・龜背芋植栽與籐編躺椅",
        drawableName = "img_otome_bg_balcony",
        drawableRes = R.drawable.img_otome_bg_balcony,
        ambientOverlayHex = 0x14FFF7E2,
        accentHex = 0xFFE07D5A,
        switchDoctorReply = "陽台這邊下午太陽剛好斜照進來，就算不出門，在這曬三分鐘太陽也算光合作用。"
    ),
    MINIMAL_CLINIC(
        id = "minimal_clinic",
        titleZh = "極簡診間",
        subtitleZh = "鼠尾草白・百葉窗與純淨療癒桌面",
        drawableName = "img_otome_bg_minimal",
        drawableRes = R.drawable.img_otome_bg_minimal,
        ambientOverlayHex = 0x12F1F8F5,
        accentHex = 0xFF5C9486,
        switchDoctorReply = "把背景收成極簡診間了。東西少一點、視覺乾淨一點，腦袋裡的雜訊也會跟著安靜。"
    );

    fun toConfigJson(): String {
        return JSONObject().apply {
            put("themeId", id)
            put("titleZh", titleZh)
            put("drawableName", drawableName)
            put("ambientOverlayHex", ambientOverlayHex)
            put("accentHex", accentHex)
        }.toString()
    }

    companion object {
        fun fromId(id: String?): OtomeClinicTheme =
            entries.firstOrNull { it.id == id } ?: TREEHOUSE
    }
}

enum class OtomeTimePeriod(val labelZh: String, val badgeZh: String) {
    MORNING("早安門診", "05:00 - 11:59"),
    AFTERNOON("午後留診", "12:00 - 17:59"),
    EVENING("夜間巡房", "18:00 - 23:59"),
    LATE_NIGHT("凌晨休診中", "00:00 - 04:59");

    companion object {
        fun fromHour(hourOfDay: Int): OtomeTimePeriod = when (hourOfDay) {
            in 0..4 -> LATE_NIGHT
            in 5..11 -> MORNING
            in 12..17 -> AFTERNOON
            else -> EVENING
        }
    }
}

data class OtomeClinicChoice(
    val id: String,
    val patientPrompt: String,
    val doctorReply: String,
    val expression: OtomeDoctorExpression
)

data class OtomeTouchSpot(
    val id: String,
    val labelZh: String,
    val reactionReplies: List<Pair<OtomeDoctorExpression, String>>
)

data class HeartPrescriptionCard(
    val id: String,
    val stampLabel: String,
    val title: String,
    val body: String,
    val isSecretAffection: Boolean
)

data class ObservationNoteEntry(
    val tier: Int,
    val title: String,
    val requiredVisits: Int,
    val requiredCompleted: Int,
    val diaryText: String
)

object OtomeClinicCatalog {
    fun greetingFor(period: OtomeTimePeriod, completedCount: Int, totalCount: Int): Pair<OtomeDoctorExpression, String> {
        if (period == OtomeTimePeriod.LATE_NIGHT) {
            return OtomeDoctorExpression.NIGHT to "凌晨還不睡覺的話，肝指數可是會抗議的。今天看診到此結束，回床上躺平吧。"
        }
        if (totalCount > 0 && completedCount >= totalCount) {
            return OtomeDoctorExpression.MUG to "今天的療程全都打勾了？這在懶癌醫學史上算是罕見奇蹟，准許你安心去當一塊沒有夢想的馬鈴薯。"
        }
        if (completedCount == 0) {
            return OtomeDoctorExpression.COOL to "看你今天一項任務都還沒動——不用解釋，這屬於大腦防止過熱的自動休眠機制，先喝口水坐著發呆吧。"
        }
        return when (period) {
            OtomeTimePeriod.MORNING -> OtomeDoctorExpression.POINT to "早安。患者今日居然在上午就完成 $completedCount 件復健了，能坐著就不要站著，記得補充水分。"
            OtomeTimePeriod.AFTERNOON -> OtomeDoctorExpression.GENTLE to "下午茶時間到，你已完成 $completedCount 項療程。本醫師正在喝全糖珍奶，你要順便歇口氣嗎？"
            OtomeTimePeriod.EVENING -> OtomeDoctorExpression.NECK to "辛苦撐到晚上了。今天完成 $completedCount 件已經打敗昨天的自己，剩下的全部推遲到明天再說。"
            OtomeTimePeriod.LATE_NIGHT -> OtomeDoctorExpression.NIGHT to "凌晨還不睡覺的話，肝指數可是會抗議的。今天看診到此結束，回床上躺平吧。"
        }
    }

    fun progressTapLines(completedCount: Int, totalCount: Int, period: OtomeTimePeriod): List<Pair<OtomeDoctorExpression, String>> {
        val lateNightLines = listOf(
            OtomeDoctorExpression.NIGHT to "別看我趴在桌上發呆，本醫師現在進入省電休診模式了，你也快去蓋棉被。",
            OtomeDoctorExpression.NIGHT to "熬夜對黑眼圈與懶癌康復都沒有任何幫助，唯一建議處方只有立刻躺平閉眼。",
            OtomeDoctorExpression.SIGH to "現在是半夜幾點你心裡有數嗎？再不睡的話，明天早上大腦會直接拒絕開機。",
            OtomeDoctorExpression.MUG to "深夜診療室不提供提神咖啡，只有熱水和勸退患者回床上睡覺的服務。",
            OtomeDoctorExpression.NECK to "連我都值班揉脖子準備下線了，你還盯著螢幕看什麼，趕快閉上眼睛。",
            OtomeDoctorExpression.COOL to "根據半夜偵測儀器顯示，你目前的清醒程度純屬硬撐，建議立刻執行睡眠動作。",
            OtomeDoctorExpression.POINT to "聽醫囑：手機亮度先調最低，十分鐘內把頭放回枕頭上。"
        )
        val allDoneLines = listOf(
            OtomeDoctorExpression.GENTLE to "今日復健進度條 100%，照這個不緊不慢的節奏，再住個三百年就能順利出院了，我看好你。",
            OtomeDoctorExpression.COOL to "表現相當優秀。作為主治醫生，我正式頒發『合格馬鈴薯認證』，剩下的時間准許繼續耍廢。",
            OtomeDoctorExpression.MUG to "今天任務全都清完了，我泡了熱茶，剩餘時間就在診療室安靜發呆到下班吧。",
            OtomeDoctorExpression.POINT to "全部完成？病歷上必須用紅筆畫兩條底線註記：今日患者行動力異常高漲。",
            OtomeDoctorExpression.NECK to "你居然把清單都做完了……搞得我都跟著覺得今天好像很忙一樣，先休息一下。",
            OtomeDoctorExpression.GENTLE to "任務全都清完了？來，分你半口珍珠奶茶的精神能量，今天不需要再努力了。",
            OtomeDoctorExpression.SIGH to "看你全做完了我反而有點擔心——可別把明天份的力氣一次預支光喔。"
        )
        val zeroDoneLines = listOf(
            OtomeDoctorExpression.COOL to "進度 0% 完全在預期之內。記住本院最高指導原則：能坐著就不要站著，能躺著就不要坐著。",
            OtomeDoctorExpression.SIGH to "一項都還沒動沒關係，我看著空白病歷表也是挺平靜的，總比亂塞一堆辦不到的事強。",
            OtomeDoctorExpression.MUG to "今天的熱茶溫度剛剛好，先喝兩口暖暖手，沒力氣做事就單純待在這裡。",
            OtomeDoctorExpression.POINT to "零進度處方簽已經幫你準備好了：等一下選一件只需要 10 秒鐘的小事就行，做不到也算數。",
            OtomeDoctorExpression.NECK to "今天是不是身體特別沉重？我的脖子也有點僵，那我們就一起維持最低耗能模式吧。",
            OtomeDoctorExpression.GENTLE to "如果覺得今天的任務太難，本院允許你把『還有呼吸』當成今日唯一達標指標。",
            OtomeDoctorExpression.COOL to "康復進度條雖然沒動，但至少沒往後倒退，本醫師對此依然維持面無表情的高度肯定。"
        )
        val inProgressLines = listOf(
            OtomeDoctorExpression.COOL to "已完成 $completedCount 項處方。不錯，比昨天那個連這行字都懶得點開看的自己進步一點點了。",
            OtomeDoctorExpression.GENTLE to "其他病房的人整天拚死拚活，肝指數可沒你的漂亮。做完這 $completedCount 項就先喘口氣吧。",
            OtomeDoctorExpression.POINT to "已經解掉 $completedCount 件啦？記住本院的原則，做到這裡覺得累就能隨時按暫停。",
            OtomeDoctorExpression.MUG to "剛才忙完那 $completedCount 件辛苦了。進度有推進就好，沒必要一口氣把自己電量榨乾。",
            OtomeDoctorExpression.SIGH to "看到你居然真的跑去做了 $completedCount 件事，我還得重新修正你今天的懶惰評估分數呢。",
            OtomeDoctorExpression.NECK to "動完這 $completedCount 件之後記得順便轉轉肩膀，別維持同一個姿勢滑太久。",
            OtomeDoctorExpression.COOL to "患者今日順利完成 $completedCount 件微任務，值得寫進《躺平醫學期刊》角落補充一筆。"
        )
        val sharedEverydayLines = listOf(
            OtomeDoctorExpression.COOL to "診療室今天空調開 26 度，安靜又沒人催你交進度，多坐一會兒無所謂。",
            OtomeDoctorExpression.MUG to "杯子裡是無糖熱焙茶，剛剛泡好的，至少聞起來比待辦清單讓人放鬆多了。",
            OtomeDoctorExpression.SIGH to "別問我為什麼白袍口袋插了兩支筆卻很少拿出來寫，因為大部分時間我都懶得寫醫囑。",
            OtomeDoctorExpression.POINT to "臨床經驗告訴我：越是想一口氣做完十件事，最後越容易十件事都不想碰。",
            OtomeDoctorExpression.NECK to "站著巡房巡了一早上，還是診療室這張椅子最符合人體工學。",
            OtomeDoctorExpression.GENTLE to "剛才外面有人急著要出院，不像你這麼懂得欣賞本院躺平療法的奧妙。",
            OtomeDoctorExpression.COOL to "你的脈搏頻率跟窗外那朵幾乎不動的雲一樣穩定，非常適合繼續休息。",
            OtomeDoctorExpression.POINT to "如果腦袋又開始自我檢討「今天怎麼這麼廢」，現在立刻把這條規定畫掉。",
            OtomeDoctorExpression.MUG to "有時候人需要的不是什麼勵志金句，只是一杯溫水跟一個沒人問你進度的下午。",
            OtomeDoctorExpression.SIGH to "又在發呆了？行，那我把今天掛號紀錄的『注意力評估』直接勾選為『佛系運作』。"
        )

        val bucket = when {
            period == OtomeTimePeriod.LATE_NIGHT -> lateNightLines
            totalCount > 0 && completedCount >= totalCount -> allDoneLines
            completedCount == 0 -> zeroDoneLines
            else -> inProgressLines
        }
        return bucket + sharedEverydayLines
    }

    val selfReportChoices: List<OtomeClinicChoice> = listOf(
        OtomeClinicChoice(
            id = "cant_move",
            patientPrompt = "「醫生，我今天完全動不了…」",
            doctorReply = "正常現象。這是大腦為了防止過熱啟動的自動休眠機制，我批准你繼續跟棉被融為一體。",
            expression = OtomeDoctorExpression.COOL
        ),
        OtomeClinicChoice(
            id = "want_praise",
            patientPrompt = "「醫生，其實今天有點想聽一句肯定…」",
            doctorReply = "特地掛號就是為了聽誇獎？……好吧。今天能把手抬起來滑手機點我，在醫學史上已經算是奇蹟了，做得不錯。",
            expression = OtomeDoctorExpression.GENTLE
        ),
        OtomeClinicChoice(
            id = "stay_to_watch",
            patientPrompt = "「沒有不舒服，只是想來診療室發呆」",
            doctorReply = "臨床顯示，對著空氣發呆十分鐘有助於降低對人類社會的焦慮感。剛好我也正想摸魚，准許在這裡一起發呆。",
            expression = OtomeDoctorExpression.MUG
        ),
        OtomeClinicChoice(
            id = "too_many_todos",
            patientPrompt = "「事情堆太多了，連打開都覺得好焦慮」",
            doctorReply = "停，深呼吸。先把那座大魔王砍掉 99%，今天只需要看一眼封面就算完成，剩餘的壓力本院拒收。",
            expression = OtomeDoctorExpression.POINT
        ),
        OtomeClinicChoice(
            id = "neck_tired",
            patientPrompt = "「一直窩著滑手機，肩頸跟腦袋都好緊繃」",
            doctorReply = "這我很有經驗——你看我值班久了也一樣痠。放下手機跟我一起把肩膀往後繞三圈，做完這三圈就准許躺回枕頭上。",
            expression = OtomeDoctorExpression.NECK
        ),
        OtomeClinicChoice(
            id = "guilty_resting",
            patientPrompt = "「總覺得什麼都沒做就休息有點罪惡感…」",
            doctorReply = "……誰規定呼吸跟休息要付費的？罪惡感只會耗損本來就不夠的電量，這條直接幫你開立無效處分。",
            expression = OtomeDoctorExpression.SIGH
        ),
        OtomeClinicChoice(
            id = "never_discharge",
            patientPrompt = "「如果這輩子都不想康復出院怎麼辦？」",
            doctorReply = "沒關係，本院本來就提供終身免費床位，照你這速度住滿三百年都沒問題，不用急著痊癒。",
            expression = OtomeDoctorExpression.COOL
        )
    )

    val touchSpots: List<OtomeTouchSpot> = listOf(
        OtomeTouchSpot(
            id = "forehead",
            labelZh = "額頭・量體溫",
            reactionReplies = listOf(
                OtomeDoctorExpression.COOL to "額頭溫度很正常……但思考過熱指數好像有點偏高，建議立刻關掉內耗開關，發呆五分鐘散熱。",
                OtomeDoctorExpression.SIGH to "別趁量體溫的時候順便把頭髮壓亂，好不容易才看起來比較像有精神的醫師。",
                OtomeDoctorExpression.POINT to "體溫確認無異常。倒是你剛才眉頭是不是又皺起來了？先放平吧。",
                OtomeDoctorExpression.COOL to "你拿自己的手背貼我額頭幹嘛……算了，溫度剛剛好，沒發燒也沒突然想奮發圖強。",
                OtomeDoctorExpression.GENTLE to "額溫 36.5 度，標準省電室溫。要是覺得腦袋嗡嗡作響，今天就不准再看工作訊息。",
                OtomeDoctorExpression.MUG to "剛喝完熱茶所以額頭稍微暖一點而已，不要誤會成我突然燃起幹勁。",
                OtomeDoctorExpression.NECK to "手這麼冰？剛才是不是躲在冷氣房忘了蓋毯子？快把手縮進袖子裡暖一下。",
                OtomeDoctorExpression.SIGH to "量完就別一直停在額頭上戳，白髮會被你搓成鳥窩的。",
                OtomeDoctorExpression.POINT to "報告患者：主治醫生本人目前顱內記憶體也是空的，非常適合陪你一起放空。",
                OtomeDoctorExpression.COOL to "靠這麼近量額溫的話……我只能從你瞳孔反光看出昨晚又滑手機滑到半夜。",
                OtomeDoctorExpression.GENTLE to "嗯，你的指尖溫度比剛進門時暖多了，至少沒再把自己凍著。",
                OtomeDoctorExpression.MUG to "額前劉海擋到視線了對吧？別幫我撥，等下自己照鏡子弄——算了隨你吧。",
                OtomeDoctorExpression.SIGH to "每次近距離檢查你都先往頭頂摸，我是大型貓咪嗎？",
                OtomeDoctorExpression.COOL to "檢測完畢：沒有出現『因為太有罪惡感而發熱』的危險症狀，合格。",
                OtomeDoctorExpression.POINT to "額頭這個位置是中樞偷懶總部，嚴禁外部人士任意敲擊喚醒。",
                OtomeDoctorExpression.NECK to "哎……別按太大力，昨天值夜班睡落枕的餘震還在太陽穴附近呢。",
                OtomeDoctorExpression.GENTLE to "既然你手都伸過來了，那順便幫我確認一下今天有沒有因為太累而眉心緊鎖吧。",
                OtomeDoctorExpression.COOL to "體溫正常。順帶一提，真正該量額溫的是螢幕前面那個快過度思考的患者。",
                OtomeDoctorExpression.MUG to "溫溫的、靜靜的，這才像休診時間該有的溫度。",
                OtomeDoctorExpression.SIGH to "額頭貼紙沒有貼在那裡啦，別一直盯著看——我沒把便利貼黏臉上吧？",
                OtomeDoctorExpression.POINT to "腦壓檢測正常，今日允許大腦維持『零思考待機畫面』四小時以上。",
                OtomeDoctorExpression.COOL to "手掌覆上來的力道還滿輕的嘛……好，批准這五秒鐘安靜降溫。",
                OtomeDoctorExpression.GENTLE to "只要額頭沒發燙、呼吸也順順的，今天剩下的行程全都可以延到明天。",
                OtomeDoctorExpression.NECK to "你該不會是想趁機偷量我有沒有在偷睡覺？抱歉，我睜著眼睛也能進入待機狀態。",
                OtomeDoctorExpression.MUG to "剛才吹了一會兒涼風，額角挺舒服的，你要不要也靠著椅背歇兩分鐘？",
                OtomeDoctorExpression.SIGH to "好啦好啦，知道你有在關心主治醫生有沒有過勞，今天確實有乖乖摸魚，放心。",
                OtomeDoctorExpression.POINT to "根據額頭接觸感應：患者今日撒嬌／偷懶需求度偏高，核准通過。",
                OtomeDoctorExpression.COOL to "不用額溫槍我也知道你現在累了，眼皮都比剛進診間時重了一點點吧？",
                OtomeDoctorExpression.GENTLE to "額頭涼涼的挺醒腦，不過我們科追求的不是醒腦，是安心犯睏。",
                OtomeDoctorExpression.MUG to "量完了？那把你剛才冰冰涼涼的手順便拿去捧著這杯熱茶吧，暖和一點。"
            )
        ),
        OtomeTouchSpot(
            id = "chest_heartbeat",
            labelZh = "胸口・聽心跳",
            reactionReplies = listOf(
                OtomeDoctorExpression.COOL to "……等一下，聽診器明明就掛在脖子上而已，你直接把手貼過來幹嘛？害我心跳確實快了半拍。",
                OtomeDoctorExpression.GENTLE to "距離拉這麼近聽心跳，萬一被你發現我平時其實沒那麼冷靜……好像也沒辦法拿病歷遮掩了。",
                OtomeDoctorExpression.SIGH to "別這樣直勾勾盯著胸口聽啦，這不在健保診療標準流程裡……算了，想多聽十秒就准你吧。",
                OtomeDoctorExpression.COOL to "你靠過來之前能不能先提前掛個號？剛剛脈搏突發性漏跳一拍，這筆醫療紀錄你自己要負責。",
                OtomeDoctorExpression.MUG to "……嗯？聽起來跟平時不太一樣？大概是因為今天診間只有你跟我兩個人，太安靜才顯得大聲吧。",
                OtomeDoctorExpression.GENTLE to "別人都是醫生替患者聽診，怎麼到你這裡反過來檢查我了……好吧，既然是你的話，破例讓你確認一次。",
                OtomeDoctorExpression.NECK to "別把距離靠的這麼近……不是怕吵，是你湊近領口的時候呼吸掃到鎖骨旁邊，我會有點沒辦法專心寫病歷。",
                OtomeDoctorExpression.COOL to "心跳維持在每分鐘七十下左右……不過你要是手指再往襯衫領口靠兩公分，數值就不一定能保證那麼冷面了。",
                OtomeDoctorExpression.GENTLE to "這件衣服料子很薄，貼太近的話連我現在心情還不錯這件事都會被你聽出來呢。",
                OtomeDoctorExpression.SIGH to "……真拿你沒辦法。既然那麼在意我的心跳穩不穩，下次不用找聽診的藉口，直接待在我看得到的地方就好。",
                OtomeDoctorExpression.COOL to "粉紅愛心聽診頭只是院內統一採購款式，絕對不是什麼少女心選品，不准笑。",
                OtomeDoctorExpression.POINT to "聽診結果顯示：主治醫生心臟跳動頻率完美同步於『想準時下班』的鐘擺。",
                OtomeDoctorExpression.MUG to "裡面裝的是剛喝下去的溫熱烏龍茶和 30% 的對社會無奈，聽起來很平靜對吧？",
                OtomeDoctorExpression.SIGH to "這只是普通純棉T恤配白袍，戳胸口不會發出像玩具熊一樣的啾啾聲好嗎。",
                OtomeDoctorExpression.COOL to "聽診器圓盤涼涼的，通常我都會先在手心捂熱再放上去——今天先借你試聽看看。",
                OtomeDoctorExpression.NECK to "胸口這條粉紅聽診器掛一整天其實有點重，幫我扶穩的話先記你一筆協助醫療。",
                OtomeDoctorExpression.POINT to "肺音清澈、心音規律，唯一異常訊號是想趕快拉你一起去喝手搖飲。",
                OtomeDoctorExpression.GENTLE to "原來剛才悄悄吃了我口袋上層的薄荷糖，難怪靠這麼近時聞得到一點點薄荷味。",
                OtomeDoctorExpression.COOL to "醫療報告：我的心跳每秒都在精準地替你過濾掉外面的催促雜訊。",
                OtomeDoctorExpression.SIGH to "領口壓太低會把聽診器耳管扯歪，幫我擺正就好，別趁機拉扯衣服。",
                OtomeDoctorExpression.MUG to "靜下來聽自己的心跳其實滿催眠的，有時候值班發呆我會聽著聽著差點睡著。",
                OtomeDoctorExpression.POINT to "心率報告正常。反倒是你靠著桌子撐太久，等一下記得把背往椅背靠回去。",
                OtomeDoctorExpression.COOL to "這顆金屬聽診頭的隔音效果很普通，外面有人敲門還是聽得一清二楚——還好今天沒人來。",
                OtomeDoctorExpression.GENTLE to "有覺得聽完穩定的心跳聲之後，自己胸口那塊緊緊的悶氣跟著鬆掉一點嗎？",
                OtomeDoctorExpression.NECK to "今天換這件淡粉色內搭單純因為衣櫃最上層剛好拿到它，跟心情沒有因果關係。",
                OtomeDoctorExpression.SIGH to "別拿夾板邊緣戳我的肋骨啦，痛覺神經目前還沒有進入懶癌休眠狀態。",
                OtomeDoctorExpression.COOL to "經胸腔回音測試：診間內完全不存在『必須立刻出門社交』的緊急訊號。",
                OtomeDoctorExpression.POINT to "聽完可以了喔，等下換我替你檢查有沒有因為焦慮而呼吸太淺。",
                OtomeDoctorExpression.MUG to "心律穩定代表今天還算和平，這份安穩也分一半給你當今日護身符。",
                OtomeDoctorExpression.GENTLE to "其實我一直覺得，只要患者進來之後呼吸比剛才慢半拍，那這間診療室就算開得值得了。"
            )
        ),
        OtomeTouchSpot(
            id = "pocket_snacks",
            labelZh = "白袍口袋・檢查庫存",
            reactionReplies = listOf(
                OtomeDoctorExpression.GENTLE to "別隨便翻我白袍口袋，裡面只裝著全糖珍奶吸管跟幾張空白『合法拖延許可證』而已。",
                OtomeDoctorExpression.MUG to "口袋右邊是備用喉糖跟兩支黑筆，左邊空著——本來就是留來收患者的煩惱紙條。",
                OtomeDoctorExpression.NECK to "裡面真的沒有藏什麼高效率神奇藥丸啦，只有一張寫著「準時下班」的便條紙。",
                OtomeDoctorExpression.COOL to "左胸口袋插著兩支幾乎沒水但懶得丟的黑筆，這叫醫師的職業堅持。",
                OtomeDoctorExpression.SIGH to "掏出什麼了？那張折得皺巴巴的收據是昨天偷買黑糖波霸鮮奶的證據，請替我保密。",
                OtomeDoctorExpression.POINT to "別亂摸口袋最底層，那裡躺著三枚硬幣、一張空白病假單跟一顆壓扁的軟糖。",
                OtomeDoctorExpression.GENTLE to "翻到水果口味喉糖了？拿去吃吧，本來就是替不愛喝水的患者準備的。",
                OtomeDoctorExpression.MUG to "白袍口袋其實比想像中深，塞進一整本袖珍版《完全躺平手冊》都沒問題。",
                OtomeDoctorExpression.COOL to "沒有聽話的話，我是從來不隨便掏出口袋裡那顆『特製紅印章』給人蓋的喔。",
                OtomeDoctorExpression.SIGH to "看吧，裡面真的一張『早睡早起獎狀』都沒有，本院不印那種東西。",
                OtomeDoctorExpression.NECK to "筆尖記得按回去。白袍如果染上藍墨水，我又要站在洗衣機前面發呆十分鐘。",
                OtomeDoctorExpression.POINT to "口袋夾層裡收著你以前寫在秘密樹洞裡的紙條，摺得好好的沒有弄皺。",
                OtomeDoctorExpression.GENTLE to "裡面還剩最後一條牛奶糖……好啦分你，今天願意過來報到就算過關獎勵。",
                OtomeDoctorExpression.COOL to "別問為什麼會有粗吸管，珍奶的珍珠用細吸管是吸不上來的，這是常識。",
                OtomeDoctorExpression.MUG to "右邊口袋偶爾會放暖暖包，冬天值班的時候兩隻手插進去就不想拿出來寫字了。",
                OtomeDoctorExpression.SIGH to "你這樣檢查庫存的熟練動作，簡直比藥局工讀生還要像內部稽核員。",
                OtomeDoctorExpression.POINT to "裡面夾了一張寫著『今日免責條款』的備忘卡，需要的時候自己伸手抽一張走。",
                OtomeDoctorExpression.COOL to "沒有偷藏咖啡因錠啦，我們科只備貨洋甘菊茶包跟舒緩眼罩。",
                OtomeDoctorExpression.GENTLE to "別戳太深，口袋縫線其實有點鬆——之前塞太多病例的碎紙屑跟小餅乾撐大的。",
                OtomeDoctorExpression.NECK to "那包是無印感濕紙巾，萬一在病房躺著喝飲料滴到棉被可以順手擦掉。",
                OtomeDoctorExpression.MUG to "偶爾也會在口袋深處摸到幾張很久以前寫一半的塗鴉，像時空膠囊一樣。",
                OtomeDoctorExpression.SIGH to "連橡皮擦跟迴紋針都要盤點嗎？總計零個緊急代辦事項，檢查結束！",
                OtomeDoctorExpression.POINT to "左胸口袋有我畫的簡易心情溫度計……不過永遠只停在『有點想睡』那格。",
                OtomeDoctorExpression.COOL to "剛才摸到的硬硬的東西是診療室備份鑰匙，不是什麼神秘武器。",
                OtomeDoctorExpression.GENTLE to "如果你今天有什麼想交給我的小紙條，不用客氣，口袋這邊位置還很寬敞。",
                OtomeDoctorExpression.MUG to "放著幾包茶葉小試喝包，等下回病房前可以順手拿一包回去泡。",
                OtomeDoctorExpression.SIGH to "白袍左半邊明顯比右半邊沉，大概是因為裝滿了全院患者推遲到明天的願望吧。",
                OtomeDoctorExpression.NECK to "再翻下去就要摸到我昨天還沒拆封的小魚乾零食包了，請給予主治醫生最後一點尊嚴。",
                OtomeDoctorExpression.POINT to "檢驗完畢：口袋物資充足，足以支撐患者安穩度過接下來七十二小時的躺平期。",
                OtomeDoctorExpression.COOL to "翻完了嗎？滿意的話幫我把白袍領口順便撫平兩公分，謝謝合作。"
            )
        ),
        OtomeTouchSpot(
            id = "wrist_pulse",
            labelZh = "手腕・測心率",
            reactionReplies = listOf(
                OtomeDoctorExpression.COOL to "脈搏節奏很悠閒嘛……完美符合『沒有夢想的馬鈴薯』標準心率，身體狀況安詳穩定。",
                OtomeDoctorExpression.POINT to "心率平穩。看樣子今天沒有什麼突發工作或無聊社交嚇到你，繼續保持。",
                OtomeDoctorExpression.GENTLE to "把脈結果：體內急需補充溫水或一小口甜食，其餘各項怠惰數值完全健康。",
                OtomeDoctorExpression.MUG to "手腕這麼細一截，握著夾板盯螢幕太久的話，記得先放下手機甩甩手腕關節。",
                OtomeDoctorExpression.SIGH to "我是讓你測脈搏不是讓你跟我比腕力啦，今天這點電量不要做無謂消耗。",
                OtomeDoctorExpression.NECK to "脈象顯示你的手腕稍微有點緊繃，剛剛滑手機或打字是不是都沒墊護腕？",
                OtomeDoctorExpression.COOL to "嗯……脈搏節奏不疾不徐，甚至透出一種『就算外面下雨我也沒打算出門』的從容。",
                OtomeDoctorExpression.GENTLE to "指腹按上來冰冰的，我順便把掌心溫度分你三秒鐘再測下一輪吧。",
                OtomeDoctorExpression.POINT to "把脈結論：大腦渴望休眠、腸胃渴望宵夜、四肢一致反對任何劇烈移動。",
                OtomeDoctorExpression.MUG to "這個脈率剛剛好配合茶壺冒泡的速度，非常適合維持現狀發呆到天黑。",
                OtomeDoctorExpression.SIGH to "別數我的脈搏跳幾下然後偷記秒數啦，你以為你在校對醫用節拍器嗎？",
                OtomeDoctorExpression.COOL to "握住手腕的時候不要突然抓這麼緊……差點把手裡的木頭病歷夾晃掉。",
                OtomeDoctorExpression.NECK to "我自己的手腕偶爾也會寫病歷寫到痠，所以現在都盡量只寫兩行結論。",
                OtomeDoctorExpression.GENTLE to "感應到極為平靜的生命頻率。看來診療室的空氣確實有幫你卸下一點緊繃。",
                OtomeDoctorExpression.POINT to "寸關尺三部脈象全數寫著同一個字：『累』。建議晚點別熬夜追劇或滑短影音。",
                OtomeDoctorExpression.COOL to "心率比剛進門時下降了五下左右，代表你待在這裡確實有好好放鬆下來。",
                OtomeDoctorExpression.MUG to "腕骨凸起來那邊沒什麼肉，靠著桌面寫東西的時候自己記得墊條小毛巾。",
                OtomeDoctorExpression.SIGH to "測了這麼久還沒測完？該不會你其實只是喜歡抓著醫生的袖口邊緣吧。",
                OtomeDoctorExpression.GENTLE to "放心，從手腕傳來的訊號很安穩，今天的你已經把該撐住的時刻撐過去了。",
                OtomeDoctorExpression.POINT to "警告：手腕偵測到對『明日早八或上班打卡』的強烈排斥波形，屬正常健康反應。",
                OtomeDoctorExpression.COOL to "袖扣沒扣得太緊吧？總覺得每次你碰袖口都在偷看我有沒有好好休息。",
                OtomeDoctorExpression.NECK to "手指跟著動一動、轉五圈手腕再放下——這也算一項合格的微復健動作喔。",
                OtomeDoctorExpression.MUG to "脈搏跳得很踏實嘛，像雨滴慢慢掉在窗台上的節奏一樣令人放心。",
                OtomeDoctorExpression.SIGH to "好喔，脈象正常、血氧充足、唯一缺乏的是沒人催促的八小時深眠。",
                OtomeDoctorExpression.GENTLE to "既然手都搭上來了，那我們就靜靜數完十下心跳，數完這十秒再決定要不要去做下一件事。",
                OtomeDoctorExpression.COOL to "把脈鑑定結果：你現在極度適合拿著三公尺長吸管，一動不動地待在角落充電。",
                OtomeDoctorExpression.POINT to "別看我手腕看起來細，拎起一箱整箱珍珠奶茶回辦公室還是沒問題的。",
                OtomeDoctorExpression.NECK to "手腕稍微垂下來放鬆五秒……對，就這樣別用力，肩頸也會一起輕很多。",
                OtomeDoctorExpression.MUG to "檢測完畢，沒有任何需要馬上起身衝刺的警訊，請安詳地保持慢動作。",
                OtomeDoctorExpression.GENTLE to "脈象說你今天其實已經很努力活著了。剩下的力氣，就留給讓自己舒服的事吧。"
            )
        )
    )

    val heartPrescriptions: List<HeartPrescriptionCard> = listOf(
        HeartPrescriptionCard(
            id = "rx_order_breathe",
            stampLabel = "主治強制醫囑",
            title = "【本日醫囑：禁止因沒進度而自我檢討】",
            body = "醫囑說明：內疚消耗的熱量無助於康復，今晚十點前遇到煩心事請默念三遍「關我什麼事」。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_potato",
            stampLabel = "官方懶癌特批",
            title = "【本日醫囑：合法當一塊馬鈴薯 3 小時】",
            body = "醫囑說明：期間准許雙手離地或完全攤在床上，任何非緊急事項一律核准推遲到明天再說。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_water",
            stampLabel = "防脫水警報章",
            title = "【本日醫囑：緩慢補水三小口】",
            body = "醫囑說明：請配合三公尺超長吸管使用；警告：過量飲水可能導致必須起身前往廁所之嚴重副作用。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_boba",
            stampLabel = "糖分補充條例",
            title = "【本日醫囑：點一杯全糖微冰心情急救劑】",
            body = "醫囑說明：根據本院無根據研究，珍珠咬起來特別療癒；主治醫生本人值班時也常靠這招續命。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_sleep",
            stampLabel = "強制休眠診斷書",
            title = "【本日醫囑：關閉不必要通知並提早躺平】",
            body = "醫囑說明：若外部人類一直找你，可出示此醫囑並聲明「正在配合懶癌復健科重要休眠療程」。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_vip",
            stampLabel = "終身住院證明",
            title = "【本日醫囑：准許續住本院懶癌VIP病房】",
            body = "醫囑說明：不用著急康復，慢慢來比較不會閃到腰；這張處方有效期限為……無限期。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_tea",
            stampLabel = "低耗能休養令",
            title = "【本日醫囑：禁止在睡前滑開焦慮待辦清單】",
            body = "醫囑說明：明天早上再打開也不會跑掉。今晚腦袋唯一許可的工作項目是「決定明天早餐吃什麼」。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_stretch",
            stampLabel = "肩頸保固貼紙",
            title = "【本日醫囑：坐著轉動肩膀三圈後放空】",
            body = "醫囑說明：避免成為人類形狀的化石。轉完三圈立刻回歸舒適姿勢，嚴禁過度運動導致喘氣。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_silence",
            stampLabel = "社交防彈許可",
            title = "【本日醫囑：暫時已讀不回非緊急人類訊息】",
            body = "醫囑說明：社交電力低於 30% 時強行社交會大幅折損壽命，本醫師核准你先裝死半天補電。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_snack",
            stampLabel = "腸胃安撫憑證",
            title = "【本日醫囑：吃一口溫熱食物或小餅乾】",
            body = "醫囑說明：維持基礎體溫即可，不用追求米其林擺盤；只要有進食就自動記入今日康復實績。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_blanket_fort",
            stampLabel = "防風結界批文",
            title = "【本日醫囑：把棉被拉到下巴完成防禦工事】",
            body = "醫囑說明：外界空氣過冷且充滿代辦事項，建議先維持蠶寶寶姿勢觀察世界十分鐘。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_blink",
            stampLabel = "眼部保養公文",
            title = "【本日醫囑：慢慢眨眼五次順便看遠方】",
            body = "醫囑說明：盯著螢幕發呆雖然也是復健，但眼角膜需要濕潤；完成後准許閉眼小憩。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_no_compare",
            stampLabel = "心理免疫特批",
            title = "【本日醫囑：禁止偷看別人的高效率日常】",
            body = "醫囑說明：別人是別人的機型，你這台出廠就是省電模式；今日禁止拿他人進度折磨自己。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_deep_sigh",
            stampLabel = "肺部排氣指令",
            title = "【本日醫囑：用力嘆一口大氣把悶氣吐光】",
            body = "醫囑說明：嘆氣並不會把福氣吐掉，反而能釋放胸口積壓的煩躁；可重複執行三次。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_socks",
            stampLabel = "末梢保暖條例",
            title = "【本日醫囑：替冰涼的腳ㄚ套上一雙厚襪子】",
            body = "醫囑說明：腳底溫暖後懶癌舒適指數會提升 40%；沒力氣彎腰的話靠在床邊慢慢穿就好。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_dessert",
            stampLabel = "多巴胺急救單",
            title = "【本日醫囑：准許吃點甜食而不計算熱量】",
            body = "醫囑說明：今日糖分屬於醫療必需品而非放縱，若旁人碎念可聲明「主治醫生強烈要求」。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_ceiling",
            stampLabel = "腦波歸零核准",
            title = "【本日醫囑：對著天花板放空八分鐘】",
            body = "醫囑說明：臨床實證放空時腦袋會自動清理垃圾快取，期間嚴禁突然冒出『我該起來做事了』的念頭。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_posture",
            stampLabel = "脊椎救護證明",
            title = "【本日醫囑：從蝦米坐姿換成軟爛半躺姿】",
            body = "醫囑說明：既然不想坐直，不如直接把腰靠穩枕頭；保護腰椎是長期賴床的基礎工程。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_music",
            stampLabel = "聽覺舒緩處方",
            title = "【本日醫囑：只播一首喜歡的歌當背景音】",
            body = "醫囑說明：不需要跟著做任何家事或讀書，純粹聽完那四分鐘就算是今日藝術復健。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_pillow_hug",
            stampLabel = "抱枕依附許可",
            title = "【本日醫囑：抓一顆蓬鬆抱枕夾在懷裡】",
            body = "醫囑說明：手邊有軟綿綿的東西能大幅降低孤單耗電量；若沒有抱枕，棉被捲成一團亦可替代。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_zero_ambition",
            stampLabel = "無慾無求印信",
            title = "【本日醫囑：今日放棄成為更好的人】",
            body = "醫囑說明：光是維持現在這個原封不動、安靜喘氣的自己，在門診這裡就已經算滿分了。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_hot_towel",
            stampLabel = "眼壓舒緩批條",
            title = "【本日醫囑：閉眼敷一下溫熱的毛巾或手掌】",
            body = "醫囑說明：搓熱雙手蓋在眼皮上十秒鐘，讓緊繃了一整天的視覺神經暫時請假。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_delivery",
            stampLabel = "烹飪豁免憑證",
            title = "【本日醫囑：合法使用外送或微波食物】",
            body = "醫囑說明：洗鍋子與備料屬於高耗能外科手術，低電量患者今天全面免除開火義務。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_sunbeam",
            stampLabel = "光合作用簽核",
            title = "【本日醫囑：拉開窗簾邊角看一眼天色】",
            body = "醫囑說明：不需要走出去曬太陽，隔著玻璃確認今天雲層還在飄浮就算完成戶外接觸。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_half_task",
            stampLabel = "半成品合法章",
            title = "【本日醫囑：允許任何事情做到一半就停】",
            body = "醫囑說明：書只看兩頁、垃圾只收三個、衣服只折兩件——本院不採行全有或全無標準。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_mute_group",
            stampLabel = "訊息降噪醫囑",
            title = "【本日醫囑：把三個吵鬧群組調成靜音】",
            body = "醫囑說明：紅色未讀數字會誘發交感神經抽搐，減少提醒跳出屬於極高性價比的自保療法。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_midnight_promise",
            stampLabel = "查房特別叮嚀",
            title = "【本日醫囑：今晚躺下後不准再檢討今天】",
            body = "醫囑說明：今天這頁病歷到十點就強制封存歸檔，就算漏了幾件事也等明天的自己再煩惱。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_cat_video",
            stampLabel = "毛絨安撫配方",
            title = "【本日醫囑：看兩支小動物打瞌睡短片】",
            body = "醫囑說明：嚴禁接著點進任何新聞或爭論留言區，只准補充純粹可愛無毒的視覺糖分。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_slow_walk",
            stampLabel = "慢動作許可證",
            title = "【本日醫囑：移動速度調降為正常的一半】",
            body = "醫囑說明：倒水慢一點、起身慢一點、關門輕一點，像一隻不趕時間的樹懶穩穩活過今天。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_cheek_pinch",
            stampLabel = "嘴角保養指引",
            title = "【本日醫囑：提醒臉頰肌肉別一直繃著】",
            body = "醫囑說明：下巴跟牙關記得稍微鬆開一點點，不用勉強微笑，面無表情放鬆才是正確醫囑。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_goodnight_check",
            stampLabel = "主治留診便條",
            title = "【本日醫囑：臨睡前回來診療室打個招呼】",
            body = "醫囑說明：就算今天清單上一個勾都沒有，晚上過來報到說一聲累了，一樣算正常看診完成。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_desk_clear",
            stampLabel = "局部整頓批文",
            title = "【本日醫囑：只清出伸手可及的一小塊空地】",
            body = "醫囑說明：別妄想一口氣大掃除整間房，清出能放一杯熱茶的二十公分方圓就算大勝。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_warm_shower",
            stampLabel = "溫水衝浪特批",
            title = "【本日醫囑：若不想洗澡就只用溫水沖腳】",
            body = "醫囑說明：清潔標準採彈性浮動制；真的累到像一灘泥的話，擦個臉就准許直接進被窩。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_no_overtime",
            stampLabel = "下班強制指令",
            title = "【本日醫囑：切斷對工作或學業的腦內延時】",
            body = "醫囑說明：身體雖然已經離開座位，腦袋卻還在加班屬於嚴重漏電行為，現在立刻拔插頭。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_jaw_stretch",
            stampLabel = "打哈欠聯動章",
            title = "【本日醫囑：想打哈欠時就徹底打完一個】",
            body = "醫囑說明：憋住哈欠會累積無名倦意；打完哈欠順勢眨掉眼角生理食鹽水，天然又舒爽。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_favorite_cup",
            stampLabel = "專用容器指定",
            title = "【本日醫囑：用你最順眼的杯子喝兩口飲品】",
            body = "醫囑說明：不管是熱紅茶、冰豆漿還是常溫白開水，握著喜歡杯子的手感能穩定今日情緒線。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_say_no",
            stampLabel = "拒絕權保護令",
            title = "【本日醫囑：對不想答應的小事延後回覆】",
            body = "醫囑說明：在心裡默念「我先查一下行程」然後直接滑走，為自己爭取寶貴的喘息空間。",
            isSecretAffection = true
        ),
        HeartPrescriptionCard(
            id = "rx_order_ankle_circle",
            stampLabel = "床邊防呆體操",
            title = "【本日醫囑：躺著轉動腳踝左右各五圈】",
            body = "醫囑說明：不用下床、不用換衣服、甚至不用睜開眼睛，算是本院門檻最低的血液循環療法。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_scent",
            stampLabel = "嗅覺安慰劑章",
            title = "【本日醫囑：聞一下喜歡的護手霜或衣物柔順味】",
            body = "醫囑說明：透過氣味提醒身體『現在處於安全舒適區』，立刻降低 15% 莫名其妙的焦躁感。",
            isSecretAffection = false
        ),
        HeartPrescriptionCard(
            id = "rx_order_standby",
            stampLabel = "專屬掛號保留",
            title = "【本日醫囑：今日所有願望都允許慢慢來】",
            body = "醫囑說明：診療室的椅子一直替你留著，急著趕路的話就走散了；照自己的懶惰時鐘走吧。",
            isSecretAffection = true
        )
    )

    // 秘密樹洞碎紙機回覆（共 26 句：原 6 句 + 新增 20 句）
    val secretShredReplies: List<String> = listOf(
        "碎紙機處理完畢。那些煩人的聲音已經切成紙花了，不需要再去撿回來想。",
        "徹底銷毀了。記得本院名言：情緒穩定是抗癌關鍵，遇到鳥事直接默念『關我什麼事』。",
        "滋滋滋地全部絞碎了。煩惱的重量剛才減少了 5 克，現在你可以少掛心這件事了。",
        "紙條已經在碎紙機裡化成無害的紙屑，接下來誰也沒辦法拿它來煩你。",
        "銷毀完成。如果等一下還有新的雜念冒出來，這台碎紙機 24 小時免掛號免費借你用。",
        "處理乾淨了。把腦袋裡的回收桶也順便按下清空鍵，今晚好好呼吸就好。",
        "卡滋卡滋——聽起來很解氣對吧？現在這件事在法律與醫療上都視同從沒發生過。",
        "碎紙機馬達剛完成一次高效減壓運轉。接下來你的工作只有去拿一杯水或直接躺好。",
        "寫進紙裡、丟進刀片、變成雪花。人類的焦慮有 80% 其實都在這個步驟就結束了。",
        "這台醫用碎紙機只吃麻煩不收掛號費，要是還有剩的鳥事，儘管一起塞進來。",
        "紙屑已經掉進不可回收區，請勿進行記憶拼圖復原手術，一律視作已消滅。",
        "幫你把那些讓人胃痛的字眼切到連顯微鏡都拼不回來了，現在胸口有沒有寬鬆一點？",
        "剛剛那點委屈我已經看著機器把它切碎了，誰再提起就由本院統一開出耳背證明。",
        "碎紙聲結束，療程生效。記得把剛剛緊繃的肩膀順便放下來兩公分。",
        "搞定。雖然這台碎紙機平時是拿來碎催進度公文的，不過用來碎你的煩惱效果更好。",
        "碎片已封箱送去外太空垃圾場，連同今天不開心的原因一起准許強制忘記。",
        "你負責把討厭的想法打成字，剩下的交給這組高轉速刀片由我收拾掉。",
        "粉碎完畢。從這一秒起禁止在腦內重播剛才那段讓人煩躁的對話畫面。",
        "聽，紙條變成細碎白雪的聲音……比待辦事項提醒音效好聽一百倍吧？",
        "銷毀流程簽核通過，今日大腦記憶體釋出 256MB，正好拿來裝點心與打瞌睡。",
        "那些不需要對你負責的人和事，就該像這樣俐落切成兩毫米寬的小紙條。",
        "我連廢紙簍的蓋子都幫你踩開又關上了，接下來眼不見為淨，繼續安心耍廢。",
        "放心，碎紙機吃下去的秘密不會出現在任何病歷表上，只有這陣微風知道它消失了。",
        "剛才切得太乾脆，連隔壁藥櫃的珍珠奶茶吸管都感覺跟著抖了兩下——心情好點了嗎？",
        "廢棄煩惱銷毀成功。臨床建議接著搭配三次深呼吸，把剛才積在胸口的悶氣一起吐掉。",
        "碎紙機已徹底消化完畢。剩下的今晚，請只讓舒服跟安靜的事情住進腦袋裡。"
    )

    val secretPocketReplies: List<String> = listOf(
        "字條折好收進白袍口袋檔案夾了。今天不用硬撐著自己扛完所有事，先去好好休息吧。",
        "收到。這份委屈暫時由本院病歷室替你保管，你的任務只剩下今晚安心睡個好覺。",
        "放進左側口袋了。不用急著立刻想出解法，在診療室裡允許你只當個不需要處理難題的病人。",
        "字跡我都看過了——能撐到現在已經很不容易。剩下的煩惱先寄放在我這件白袍裡吧。",
        "折成最小一塊收妥了。明天太陽升起之前，這些事都不准再來打擾你躺平。",
        "放心，口袋有扣好不會掉出來。你先去喝口溫水，這份情緒病歷我替你暫管著。"
    )

    val observationNotes: List<ObservationNoteEntry> = listOf(
        ObservationNoteEntry(
            tier = 1,
            title = "觀察筆記 #01｜初診掛號紀錄",
            requiredVisits = 0,
            requiredCompleted = 0,
            diaryText = "患者剛辦理入住，懶癌細胞活性極高，連看清單都嫌累。評估短期內應該出不了院——還好本院免費床位多的是。"
        ),
        ObservationNoteEntry(
            tier = 2,
            title = "觀察筆記 #02｜居然喝了口水",
            requiredVisits = 2,
            requiredCompleted = 1,
            diaryText = "患者今天竟然完成了微任務，還順便戳了醫生幾下確認存在感。生命跡象穩定微幅上升，在病歷角上註記『可圈可點』。"
        ),
        ObservationNoteEntry(
            tier = 3,
            title = "觀察筆記 #03｜藥局常客觀察",
            requiredVisits = 5,
            requiredCompleted = 3,
            diaryText = "發現患者滿喜歡把奇怪道具擺進病房的，從珍奶點滴到『居然活著』錦旗應有盡有。生活趣味度提高，復健態度良好（雖然還是很懶）。"
        ),
        ObservationNoteEntry(
            tier = 4,
            title = "觀察筆記 #04｜穩定的存在",
            requiredVisits = 8,
            requiredCompleted = 6,
            diaryText = "原本以為這患者過兩天就會忘記來診療室報到，結果居然偶爾還會回來坐坐發呆。這點微小的毅力，比想像中還要頑強很多。"
        ),
        ObservationNoteEntry(
            tier = 5,
            title = "觀察筆記 #05｜白袍口袋檔案",
            requiredVisits = 12,
            requiredCompleted = 10,
            diaryText = "口袋裡收著幾張患者遞過來的煩惱紙條。其實每個人都有撐不住的時候，只要偶爾還願意來這間病房喘口氣，就算是很厲害的大人了。"
        ),
        ObservationNoteEntry(
            tier = 6,
            title = "觀察筆記 #06｜無限期住院核准",
            requiredVisits = 18,
            requiredCompleted = 15,
            diaryText = "照理說醫師應該催促患者早日出院，不過我有點習慣這樣的日常了，把病歷長期保存下來吧。不論今天電量剩 10% 還是 100%，隨時回來躺平都沒問題。"
        )
    )
}
