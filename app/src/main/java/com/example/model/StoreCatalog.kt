package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class StoreCategory(val labelAr: String) {
    ALL("الكل"),
    BEAD_COLOR("ألوان الخرز"),
    BACKGROUND("الخلفيات"),
    THEME("الثيمات"),
    PLANT("النباتات"),
    FEATURE("الميزات الحصرية"),
}

data class StoreItem(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val category: StoreCategory,
    val coinPrice: Long,
    val previewColorHex: String = "#0F6B56",
    val secondaryColorHex: String = "#C78D3B",
    @DrawableRes val backgroundDrawableRes: Int? = null,
    val isDarkScene: Boolean = false,
    val vipOnly: Boolean = false,
)

data class BillingPackage(
    val sku: String,
    val titleAr: String,
    val descriptionAr: String,
    val priceDisplay: String,
    val coinsGranted: Long,
    val unlocksVip: Boolean = false,
    val badgeAr: String? = null,
)

data class PlantStageInfo(
    val stageIndex: Int,
    val stageNameAr: String,
    val stageHintAr: String,
    val progressToNext: Float,
    val remainingToNext: Long,
    val nextThreshold: Long,
)

object StoreCatalog {

    val defaultUnlockedIds = setOf(
        "bead_amber",
        "bead_emerald",
        "theme_emerald",
        "bg_minimal_ivory",
        "plant_olive",
    )

    val allItems: List<StoreItem> = listOf(
        // Bead Colors
        StoreItem(
            id = "bead_amber",
            titleAr = "كهرمان دافئ",
            descriptionAr = "خرز بلون الكهرمان العتيق بلمعة دافئة",
            category = StoreCategory.BEAD_COLOR,
            coinPrice = 0L,
            previewColorHex = "#C78D3B",
            secondaryColorHex = "#F3C978",
        ),
        StoreItem(
            id = "bead_emerald",
            titleAr = "زمرد البستان",
            descriptionAr = "خرز أخضر زمردي هادئ مريح للعين",
            category = StoreCategory.BEAD_COLOR,
            coinPrice = 0L,
            previewColorHex = "#0F6B56",
            secondaryColorHex = "#62C4A9",
        ),
        StoreItem(
            id = "bead_pearl",
            titleAr = "لؤلؤ نقي",
            descriptionAr = "خرز أبيض لؤلؤي ناعم بلمعة فضية",
            category = StoreCategory.BEAD_COLOR,
            coinPrice = 40L,
            previewColorHex = "#D8D6CF",
            secondaryColorHex = "#FFFFFF",
        ),
        StoreItem(
            id = "bead_turquoise",
            titleAr = "فيروز بحري",
            descriptionAr = "خرز فيروزي يعكس صفاء قطرات الماء",
            category = StoreCategory.BEAD_COLOR,
            coinPrice = 60L,
            previewColorHex = "#0E8FA3",
            secondaryColorHex = "#7CE3F2",
        ),
        StoreItem(
            id = "bead_ruby",
            titleAr = "ياقوت رماني",
            descriptionAr = "خرز أحمر ياقوتي عميق بتدرج فخم",
            category = StoreCategory.BEAD_COLOR,
            coinPrice = 90L,
            previewColorHex = "#9E2A48",
            secondaryColorHex = "#E87A96",
        ),
        StoreItem(
            id = "bead_sandalwood",
            titleAr = "خشب الصندل",
            descriptionAr = "خرز خشبي طبيعي بملمس تقليدي أصيل",
            category = StoreCategory.BEAD_COLOR,
            coinPrice = 110L,
            previewColorHex = "#7C4D32",
            secondaryColorHex = "#BC8A66",
        ),
        StoreItem(
            id = "bead_gold",
            titleAr = "ذهب عتيق",
            descriptionAr = "خرز ذهبي حصري ببريق فاخر",
            category = StoreCategory.BEAD_COLOR,
            coinPrice = 180L,
            previewColorHex = "#D49B1A",
            secondaryColorHex = "#FFE38E",
            vipOnly = true,
        ),

        // Backgrounds
        StoreItem(
            id = "bg_minimal_ivory",
            titleAr = "صفاء الفجر",
            descriptionAr = "خلفية هادئة متدرجة تبرز الخرز والنبتة",
            category = StoreCategory.BACKGROUND,
            coinPrice = 0L,
            previewColorHex = "#F7F5EE",
            secondaryColorHex = "#DCECE5",
        ),
        StoreItem(
            id = "bg_emerald_mist",
            titleAr = "ندى السحر",
            descriptionAr = "تدرج مائي أخضر هادئ بلمسة ضبابية",
            category = StoreCategory.BACKGROUND,
            coinPrice = 50L,
            previewColorHex = "#DCF2EA",
            secondaryColorHex = "#B3DFD1",
        ),
        StoreItem(
            id = "bg_andalusia",
            titleAr = "فناء الأندلس",
            descriptionAr = "فناء أندلسي مشمس بأقواس حجرية ونباتات",
            category = StoreCategory.BACKGROUND,
            coinPrice = 120L,
            previewColorHex = "#E9DFC8",
            secondaryColorHex = "#8CA88E",
            backgroundDrawableRes = R.drawable.img_bg_andalusia,
        ),
        StoreItem(
            id = "bg_spring_garden",
            titleAr = "ربيع الياسمين",
            descriptionAr = "إشراقة ربيعية ناعمة بأغصان الزيتون والياسمين",
            category = StoreCategory.BACKGROUND,
            coinPrice = 140L,
            previewColorHex = "#F8E8DF",
            secondaryColorHex = "#A3C2A8",
            backgroundDrawableRes = R.drawable.img_bg_spring_garden,
        ),
        StoreItem(
            id = "bg_oasis_night",
            titleAr = "واحة الليل المقمرة",
            descriptionAr = "سماء ليلية صافية بالنجوم والهلال فوق الواحة",
            category = StoreCategory.BACKGROUND,
            coinPrice = 160L,
            previewColorHex = "#0B243B",
            secondaryColorHex = "#1E4D6B",
            backgroundDrawableRes = R.drawable.img_bg_oasis_night,
            isDarkScene = true,
        ),

        // Themes
        StoreItem(
            id = "theme_emerald",
            titleAr = "ثيم الزمرد الهادئ",
            descriptionAr = "ألوان البستان الخضراء والذهبية الكلاسيكية",
            category = StoreCategory.THEME,
            coinPrice = 0L,
            previewColorHex = "#0F6B56",
            secondaryColorHex = "#C68B2C",
        ),
        StoreItem(
            id = "theme_sand_dunes",
            titleAr = "ثيم رمال الواحة",
            descriptionAr = "ألوان دافئة مستوحاة من الطين والصحراء",
            category = StoreCategory.THEME,
            coinPrice = 70L,
            previewColorHex = "#9C5B2E",
            secondaryColorHex = "#4F7A52",
        ),
        StoreItem(
            id = "theme_turquoise",
            titleAr = "ثيم الفيروز الأندلسي",
            descriptionAr = "أزرق فيروزي منعش مع لمسات النحاس",
            category = StoreCategory.THEME,
            coinPrice = 90L,
            previewColorHex = "#0E7490",
            secondaryColorHex = "#D97706",
        ),
        StoreItem(
            id = "theme_jasmine",
            titleAr = "ثيم الياسمين الوردي",
            descriptionAr = "تناغم ناعم بين لون الورد الدمشقي والريحان",
            category = StoreCategory.THEME,
            coinPrice = 110L,
            previewColorHex = "#8E4460",
            secondaryColorHex = "#4B7F60",
        ),
        StoreItem(
            id = "theme_royal_night",
            titleAr = "ثيم الليل الملكي",
            descriptionAr = "مظهر داكن فاخر لقيام الليل والأذكار المسائية",
            category = StoreCategory.THEME,
            coinPrice = 150L,
            previewColorHex = "#0B1622",
            secondaryColorHex = "#5EEAD4",
            isDarkScene = true,
        ),

        // Plants
        StoreItem(
            id = "plant_olive",
            titleAr = "شجرة الزيتون",
            descriptionAr = "تنمو من بذرة مباركة إلى شجرة زيتون خضراء",
            category = StoreCategory.PLANT,
            coinPrice = 0L,
            previewColorHex = "#3E7B48",
            secondaryColorHex = "#8FBF7B",
        ),
        StoreItem(
            id = "plant_lotus",
            titleAr = "زهرة اللوتس المائية",
            descriptionAr = "نبتة مائية تفتح بتلاتها البيضاء والوردية مع الذكر",
            category = StoreCategory.PLANT,
            coinPrice = 80L,
            previewColorHex = "#D46A92",
            secondaryColorHex = "#4CA88B",
        ),
        StoreItem(
            id = "plant_palm",
            titleAr = "نخلة الواحة",
            descriptionAr = "نخلة باسقة تتدلى عراجينها كلما زاد تسبيحك",
            category = StoreCategory.PLANT,
            coinPrice = 120L,
            previewColorHex = "#2E6F40",
            secondaryColorHex = "#C98A3A",
        ),
        StoreItem(
            id = "plant_pomegranate",
            titleAr = "شجرة الرمان",
            descriptionAr = "أغصان وارفة تثمر رماناً ياقوتياً عند اكتمال النمو",
            category = StoreCategory.PLANT,
            coinPrice = 150L,
            previewColorHex = "#B23A48",
            secondaryColorHex = "#4C8C54",
        ),
        StoreItem(
            id = "plant_jasmine",
            titleAr = "الياسمين الدمشقي",
            descriptionAr = "شجيرة حصرية تزهر بباقات الياسمين البيضاء العطرة",
            category = StoreCategory.PLANT,
            coinPrice = 220L,
            previewColorHex = "#F4EFE6",
            secondaryColorHex = "#2F7D57",
            vipOnly = true,
        ),

        // Exclusive Features
        StoreItem(
            id = "feat_crystal_sound",
            titleAr = "أصوات البلور والمطر",
            descriptionAr = "فتح نغمات الرنين البلوري ورذاذ المطر الهادئ",
            category = StoreCategory.FEATURE,
            coinPrice = 100L,
            previewColorHex = "#0288D1",
            secondaryColorHex = "#80DEEA",
        ),
        StoreItem(
            id = "feat_custom_color",
            titleAr = "مختبر الألوان المخصصة",
            descriptionAr = "تخصيص لون الخرز بحرية من لوحة الألوان الكاملة",
            category = StoreCategory.FEATURE,
            coinPrice = 85L,
            previewColorHex = "#8E44AD",
            secondaryColorHex = "#F39C12",
        ),
    )

    val billingPackages: List<BillingPackage> = listOf(
        BillingPackage(
            sku = "ghars_coins_250",
            titleAr = "٢٥٠ عملة غرس",
            descriptionAr = "حزمة بداية لفتح ألوان الخرز والنباتات الجديدة",
            priceDisplay = "$0.99",
            coinsGranted = 250L,
        ),
        BillingPackage(
            sku = "ghars_coins_750",
            titleAr = "٧٥٠ عملة غرس",
            descriptionAr = "الأكثر شيوعاً لفتح الخلفيات والثيمات المميزة",
            priceDisplay = "$2.49",
            coinsGranted = 750L,
            badgeAr = "الأكثر طلباً",
        ),
        BillingPackage(
            sku = "ghars_coins_2000",
            titleAr = "٢٠٠٠ عملة غرس",
            descriptionAr = "رصيد وفير لجميع التخصيصات والميزات الحصرية",
            priceDisplay = "$4.99",
            coinsGranted = 2000L,
        ),
        BillingPackage(
            sku = "ghars_vip_garden",
            titleAr = "حزمة البستان الكاملة (VIP)",
            descriptionAr = "فتح جميع الخلفيات والثيمات والنباتات والأصوات الحصرية + ٥٠٠ عملة إضافية",
            priceDisplay = "$6.99",
            coinsGranted = 500L,
            unlocksVip = true,
            badgeAr = "شامل وحصري",
        ),
    )

    val customColorSwatches: List<String> = listOf(
        "#C78D3B", "#0F6B56", "#0E8FA3", "#9E2A48",
        "#7C4D32", "#4B6CB7", "#8E4460", "#2C3E50",
        "#D49B1A", "#3D7A5A", "#6A4C93", "#B55D35",
    )

    fun findById(id: String): StoreItem? = allItems.firstOrNull { it.id == id }

    fun calculatePlantStage(totalTasbih: Long): PlantStageInfo {
        return when {
            totalTasbih < 11 -> PlantStageInfo(
                stageIndex = 0,
                stageNameAr = "المرحلة ١: بذرة في التربة",
                stageHintAr = "اسقِ البذرة بالتسبيح لتنبت",
                progressToNext = (totalTasbih / 11f).coerceIn(0f, 1f),
                remainingToNext = (11L - totalTasbih).coerceAtLeast(0L),
                nextThreshold = 11L,
            )
            totalTasbih < 34 -> PlantStageInfo(
                stageIndex = 1,
                stageNameAr = "المرحلة ٢: برعم أخضر",
                stageHintAr = "بدأت الحياة تدب في غرسك",
                progressToNext = ((totalTasbih - 11) / 23f).coerceIn(0f, 1f),
                remainingToNext = (34L - totalTasbih).coerceAtLeast(0L),
                nextThreshold = 34L,
            )
            totalTasbih < 100 -> PlantStageInfo(
                stageIndex = 2,
                stageNameAr = "المرحلة ٣: شتلة ندية",
                stageHintAr = "الأوراق تتفتح مع كل قطرة ذكر",
                progressToNext = ((totalTasbih - 34) / 66f).coerceIn(0f, 1f),
                remainingToNext = (100L - totalTasbih).coerceAtLeast(0L),
                nextThreshold = 100L,
            )
            totalTasbih < 300 -> PlantStageInfo(
                stageIndex = 3,
                stageNameAr = "المرحلة ٤: نبتة متفرعة",
                stageHintAr = "غرسك يشتد عوده ويزداد خضرة",
                progressToNext = ((totalTasbih - 100) / 200f).coerceIn(0f, 1f),
                remainingToNext = (300L - totalTasbih).coerceAtLeast(0L),
                nextThreshold = 300L,
            )
            totalTasbih < 700 -> PlantStageInfo(
                stageIndex = 4,
                stageNameAr = "المرحلة ٥: نبتة مزهرة",
                stageHintAr = "تتفتح البراعم والزهور بذكر الله",
                progressToNext = ((totalTasbih - 300) / 400f).coerceIn(0f, 1f),
                remainingToNext = (700L - totalTasbih).coerceAtLeast(0L),
                nextThreshold = 700L,
            )
            else -> PlantStageInfo(
                stageIndex = 5,
                stageNameAr = "المرحلة ٦: شجرة مثمرة مكتملة",
                stageHintAr = "ما شاء الله! غرسك مثمر ومزدهر",
                progressToNext = 1f,
                remainingToNext = 0L,
                nextThreshold = 700L,
            )
        }
    }

    fun parseHexColor(hex: String, fallback: Color = Color(0xFFC78D3B)): Color {
        return try {
            val cleaned = hex.removePrefix("#")
            Color(("FF$cleaned").toLong(16))
        } catch (_: Exception) {
            fallback
        }
    }
}
