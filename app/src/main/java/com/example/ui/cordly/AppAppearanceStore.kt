package com.example.ui.cordly

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import java.io.File

enum class AppWallpaperMode(val storageKey: String, val label: String) {
    DEFAULT("default", "Pure Black"),
    PRESET_1("preset_1", "Plasma Sunset"),
    PRESET_2("preset_2", "Midnight Violet"),
    PRESET_3("preset_3", "Deep Teal"),
    SHAPE_HEX("shape_hex", "Cyber Hex"),
    SHAPE_AURORA("shape_aurora", "Aurora Waves"),
    SHAPE_WAVES("shape_waves", "Neon Rings"),
    SHAPE_GALAXY("shape_galaxy", "Starfield Grid"),
    CUSTOM("custom", "Custom Photo");

    @DrawableRes
    fun drawableRes(): Int? = when (this) {
        PRESET_1 -> R.drawable.bg_preset_1
        PRESET_2 -> R.drawable.bg_preset_2
        PRESET_3 -> R.drawable.bg_preset_3
        else -> null
    }

    fun isShapeWallpaper(): Boolean =
        this == SHAPE_HEX || this == SHAPE_AURORA || this == SHAPE_WAVES || this == SHAPE_GALAXY

    companion object {
        fun fromKey(key: String?): AppWallpaperMode {
            return entries.firstOrNull { it.storageKey == key } ?: DEFAULT
        }
    }
}

enum class BannerPatternType {
    DISCORD_SPLASH,
    HEX_CYBER,
    AURORA_WAVE,
    PRISM_SHARDS,
    SUNSET_RAYS,
    SAKURA_PETALS,
    MATRIX_GRID,
    NEBULA_STARS
}

data class ProfileBannerPreset(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val pattern: BannerPatternType,
    val colors: List<Color>
) {
    val bannerSource: String get() = "${ProfileBannerCatalog.PREFIX}$id"
}

object ProfileBannerCatalog {
    const val PREFIX = "preset_banner:"

    val presets: List<ProfileBannerPreset> = listOf(
        ProfileBannerPreset(
            id = "discord_splash",
            titleEn = "Discord Neon",
            titleAr = "أزرق ديسكورد",
            pattern = BannerPatternType.DISCORD_SPLASH,
            colors = listOf(Color(0xFF0C05AB), Color(0xFF5865F2), Color(0xFF22D3EE))
        ),
        ProfileBannerPreset(
            id = "cyber_hex",
            titleEn = "Cyber Hex",
            titleAr = "سداسي سيبراني",
            pattern = BannerPatternType.HEX_CYBER,
            colors = listOf(Color(0xFF071426), Color(0xFF0EA5E9), Color(0xFF22D3EE))
        ),
        ProfileBannerPreset(
            id = "aurora_wave",
            titleEn = "Aurora Wave",
            titleAr = "موجات الشفق",
            pattern = BannerPatternType.AURORA_WAVE,
            colors = listOf(Color(0xFF091E2C), Color(0xFF10B981), Color(0xFF6366F1))
        ),
        ProfileBannerPreset(
            id = "prism_shards",
            titleEn = "Crystal Prism",
            titleAr = "كريستال ماسي",
            pattern = BannerPatternType.PRISM_SHARDS,
            colors = listOf(Color(0xFF1E1B4B), Color(0xFF8B5CF6), Color(0xFFEC4899))
        ),
        ProfileBannerPreset(
            id = "sunset_rays",
            titleEn = "Solar Flare",
            titleAr = "أشعة ذهبية",
            pattern = BannerPatternType.SUNSET_RAYS,
            colors = listOf(Color(0xFF2A0914), Color(0xFFEF4444), Color(0xFFF59E0B))
        ),
        ProfileBannerPreset(
            id = "sakura_petals",
            titleEn = "Sakura Bloom",
            titleAr = "أزهار ساكورا",
            pattern = BannerPatternType.SAKURA_PETALS,
            colors = listOf(Color(0xFF2B0B24), Color(0xFFEC4899), Color(0xFFF9A8D4))
        ),
        ProfileBannerPreset(
            id = "matrix_grid",
            titleEn = "Emerald Grid",
            titleAr = "شبكة نيون",
            pattern = BannerPatternType.MATRIX_GRID,
            colors = listOf(Color(0xFF041C14), Color(0xFF059669), Color(0xFF34D399))
        ),
        ProfileBannerPreset(
            id = "nebula_stars",
            titleEn = "Galaxy Stars",
            titleAr = "نجوم المجرة",
            pattern = BannerPatternType.NEBULA_STARS,
            colors = listOf(Color(0xFF08051A), Color(0xFF4F46E5), Color(0xFFC084FC))
        )
    )

    fun findBySource(source: String?): ProfileBannerPreset? {
        if (source == null || !source.startsWith(PREFIX)) return null
        val id = source.removePrefix(PREFIX)
        return presets.firstOrNull { it.id == id }
    }
}

enum class AppThemeMode(val storageKey: String) {
    NIGHT("night"),
    DAY("day");

    companion object {
        fun fromKey(key: String?): AppThemeMode {
            return entries.firstOrNull { it.storageKey == key } ?: NIGHT
        }
    }
}

enum class AppAccentColor(
    val storageKey: String,
    val labelEn: String,
    val labelAr: String,
    val primary: Color,
    val primaryBright: Color,
    val darkSecondary: Color,
    val lightSecondary: Color
) {
    BLURPLE(
        storageKey = "blurple",
        labelEn = "Blurple",
        labelAr = "أزرق ديسكورد",
        primary = Color(0xFF5865F2),
        primaryBright = Color(0xFF7289DA),
        darkSecondary = Color(0xFF22D3EE),
        lightSecondary = Color(0xFF0284C7)
    ),
    OCEAN(
        storageKey = "ocean",
        labelEn = "Ocean",
        labelAr = "أزرق سماوي",
        primary = Color(0xFF0EA5E9),
        primaryBright = Color(0xFF38BDF8),
        darkSecondary = Color(0xFF2DD4BF),
        lightSecondary = Color(0xFF0F766E)
    ),
    EMERALD(
        storageKey = "emerald",
        labelEn = "Emerald",
        labelAr = "زمردي",
        primary = Color(0xFF10B981),
        primaryBright = Color(0xFF34D399),
        darkSecondary = Color(0xFF22D3EE),
        lightSecondary = Color(0xFF0369A1)
    ),
    VIOLET(
        storageKey = "violet",
        labelEn = "Violet",
        labelAr = "بنفسجي",
        primary = Color(0xFF8B5CF6),
        primaryBright = Color(0xFFA78BFA),
        darkSecondary = Color(0xFFF472B6),
        lightSecondary = Color(0xFFBE185D)
    ),
    CRIMSON(
        storageKey = "crimson",
        labelEn = "Crimson",
        labelAr = "أحمر ياقوتي",
        primary = Color(0xFFEF4444),
        primaryBright = Color(0xFFF87171),
        darkSecondary = Color(0xFFFBBF24),
        lightSecondary = Color(0xFFB45309)
    ),
    AMBER(
        storageKey = "amber",
        labelEn = "Gold",
        labelAr = "ذهبي",
        primary = Color(0xFFF59E0B),
        primaryBright = Color(0xFFFBBF24),
        darkSecondary = Color(0xFFFB7185),
        lightSecondary = Color(0xFFBE123C)
    ),
    SAKURA(
        storageKey = "sakura",
        labelEn = "Pink",
        labelAr = "وردي",
        primary = Color(0xFFEC4899),
        primaryBright = Color(0xFFF472B6),
        darkSecondary = Color(0xFF38BDF8),
        lightSecondary = Color(0xFF4338CA)
    ),
    MONO(
        storageKey = "mono",
        labelEn = "Titanium",
        labelAr = "فضي أنيق",
        primary = Color(0xFF475569),
        primaryBright = Color(0xFF94A3B8),
        darkSecondary = Color(0xFF22D3EE),
        lightSecondary = Color(0xFF0284C7)
    );

    companion object {
        fun fromKey(key: String?): AppAccentColor {
            return entries.firstOrNull { it.storageKey == key } ?: BLURPLE
        }
    }
}

data class WallpaperState(
    val mode: AppWallpaperMode = AppWallpaperMode.DEFAULT,
    val customFilePath: String? = null,
    val customVersion: Long = 0L
)

object AppAppearanceStore {
    private const val PREFS_NAME = "neov_chat_appearance"
    private const val KEY_WALLPAPER_MODE = "wallpaper_mode"
    private const val KEY_CUSTOM_VERSION = "custom_bg_version"
    private const val KEY_LANGUAGE_CODE = "app_language_code"
    private const val KEY_THEME_MODE = "app_theme_mode"
    private const val KEY_ACCENT_COLOR = "app_accent_color"
    private const val CUSTOM_WALLPAPER_FILENAME = "custom_app_wallpaper.jpg"

    @Volatile
    private var cachedState: WallpaperState? = null

    fun peekCached(): WallpaperState? = cachedState

    suspend fun loadAsync(context: Context): WallpaperState =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            load(context)
        }

    fun load(context: Context): WallpaperState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = AppWallpaperMode.fromKey(prefs.getString(KEY_WALLPAPER_MODE, null))
        val file = File(context.filesDir, CUSTOM_WALLPAPER_FILENAME)
        val version = prefs.getLong(KEY_CUSTOM_VERSION, 0L)
        return WallpaperState(
            mode = if (mode == AppWallpaperMode.CUSTOM && !file.exists()) AppWallpaperMode.DEFAULT else mode,
            customFilePath = if (file.exists()) file.absolutePath else null,
            customVersion = version
        ).also { cachedState = it }
    }

    fun saveMode(context: Context, mode: AppWallpaperMode): WallpaperState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WALLPAPER_MODE, mode.storageKey).apply()
        return load(context)
    }

    fun markCustomUpdated(context: Context): WallpaperState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_WALLPAPER_MODE, AppWallpaperMode.CUSTOM.storageKey)
            .putLong(KEY_CUSTOM_VERSION, System.currentTimeMillis())
            .apply()
        return load(context)
    }

    fun customWallpaperFile(context: Context): File {
        return File(context.filesDir, CUSTOM_WALLPAPER_FILENAME)
    }

    fun customProfileVideoFile(context: Context, uid: String): File {
        return File(context.filesDir, "custom_profile_video_${uid}.mp4")
    }

    fun customProfileUnlimitedVideoFile(context: Context, uid: String): File {
        return File(context.filesDir, "custom_profile_unlimited_video_${uid}.mp4")
    }

    fun loadLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE_CODE, "system") ?: "system"
    }

    fun saveLanguageCode(context: Context, code: String): String {
        val clean = code.trim().ifBlank { "system" }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE_CODE, clean).apply()
        return clean
    }

    fun loadThemeMode(context: Context): AppThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, null))
    }

    fun saveThemeMode(context: Context, mode: AppThemeMode): AppThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.storageKey).apply()
        return mode
    }

    fun loadAccentColor(context: Context): AppAccentColor {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppAccentColor.fromKey(prefs.getString(KEY_ACCENT_COLOR, null))
    }

    fun saveAccentColor(context: Context, accent: AppAccentColor): AppAccentColor {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACCENT_COLOR, accent.storageKey).apply()
        return accent
    }
}

enum class FrameShapeStyle {
    CIRCLE_ORBIT,
    OCTAGON_CYBER,
    PRISM_DIAMOND,
    DOUBLE_STARBURST,
    FLAME_CROWN,
    MATRIX_DASHED
}

data class ProfileFrameItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val price: Long,
    val ringColors: List<Color>,
    val strokeDp: Float = 3f,
    val animated: Boolean = false,
    val shapeStyle: FrameShapeStyle = FrameShapeStyle.CIRCLE_ORBIT
)

data class ProfileVideoEffectItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val price: Long,
    val accentColors: List<Color>,
    val rawResId: Int? = null,
    val isCustomUpload: Boolean = false,
    val isUnlimitedDuration: Boolean = false
)

object PeyShopCatalog {
    val frames: List<ProfileFrameItem> = listOf(
        ProfileFrameItem(
            id = "none",
            title = "Classic Steel",
            subtitle = "Clean forged-iron border",
            price = 0L,
            ringColors = listOf(Color(0xFF475569), Color(0xFF334155)),
            strokeDp = 1.5f,
            animated = false,
            shapeStyle = FrameShapeStyle.CIRCLE_ORBIT
        ),
        ProfileFrameItem(
            id = "neon_cyber",
            title = "Neon Cyber Scanner",
            subtitle = "Spinning dashed cyan & blurple cyber ring",
            price = 150L,
            ringColors = listOf(Color(0xFF22D3EE), Color(0xFF5865F2), Color(0xFF22D3EE)),
            strokeDp = 3.4f,
            animated = true,
            shapeStyle = FrameShapeStyle.MATRIX_DASHED
        ),
        ProfileFrameItem(
            id = "arctic_crystal",
            title = "Arctic Prism Diamond",
            subtitle = "Twin faceted ice-blue rotating diamond star",
            price = 180L,
            ringColors = listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFFE0F2FE)),
            strokeDp = 3.5f,
            animated = true,
            shapeStyle = FrameShapeStyle.PRISM_DIAMOND
        ),
        ProfileFrameItem(
            id = "molten_gold",
            title = "Solar Champion",
            subtitle = "Blazing gold crown flares & ember halo",
            price = 200L,
            ringColors = listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFFBBF24)),
            strokeDp = 3.6f,
            animated = true,
            shapeStyle = FrameShapeStyle.FLAME_CROWN
        ),
        ProfileFrameItem(
            id = "emerald_matrix",
            title = "Emerald Octagon Shield",
            subtitle = "Sharp octagonal neon emerald cyber frame",
            price = 250L,
            ringColors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF06B6D4), Color(0xFF10B981)),
            strokeDp = 3.6f,
            animated = true,
            shapeStyle = FrameShapeStyle.OCTAGON_CYBER
        ),
        ProfileFrameItem(
            id = "inferno_crown",
            title = "Royal Phoenix Crown",
            subtitle = "Radiant flame spikes & crimson-gold crest",
            price = 300L,
            ringColors = listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFFDE047), Color(0xFFF59E0B)),
            strokeDp = 3.8f,
            animated = true,
            shapeStyle = FrameShapeStyle.FLAME_CROWN
        ),
        ProfileFrameItem(
            id = "sakura_bloom",
            title = "Sakura Orbit Blossom",
            subtitle = "Silky rose-pink gradient halo & floating petals",
            price = 320L,
            ringColors = listOf(Color(0xFFEC4899), Color(0xFFF9A8D4), Color(0xFFFBBF24), Color(0xFFEC4899)),
            strokeDp = 3.6f,
            animated = true,
            shapeStyle = FrameShapeStyle.CIRCLE_ORBIT
        ),
        ProfileFrameItem(
            id = "diamond_pixel",
            title = "Voxel Diamond Crest",
            subtitle = "Crystalline twin-diamond sky & cobalt crest",
            price = 350L,
            ringColors = listOf(Color(0xFF38BDF8), Color(0xFFE0F2FE), Color(0xFF3B82F6), Color(0xFF38BDF8)),
            strokeDp = 3.8f,
            animated = true,
            shapeStyle = FrameShapeStyle.PRISM_DIAMOND
        ),
        ProfileFrameItem(
            id = "crimson_halo",
            title = "Crimson Starburst",
            subtitle = "Fierce ruby & magenta starburst aura",
            price = 400L,
            ringColors = listOf(Color(0xFFF43F5E), Color(0xFFEC4899), Color(0xFF9333EA), Color(0xFFF43F5E)),
            strokeDp = 3.8f,
            animated = true,
            shapeStyle = FrameShapeStyle.DOUBLE_STARBURST
        ),
        ProfileFrameItem(
            id = "void_glitch",
            title = "Void Hex Glitch",
            subtitle = "Dark violet & electric neon octagonal frame",
            price = 450L,
            ringColors = listOf(Color(0xFF8B5CF6), Color(0xFFD946EF), Color(0xFF22D3EE), Color(0xFF8B5CF6)),
            strokeDp = 4.0f,
            animated = true,
            shapeStyle = FrameShapeStyle.OCTAGON_CYBER
        ),
        ProfileFrameItem(
            id = "prismatic_aura",
            title = "Prismatic Mythic",
            subtitle = "Full-spectrum rotating cosmic starburst halo",
            price = 600L,
            ringColors = listOf(
                Color(0xFFEF4444),
                Color(0xFFF59E0B),
                Color(0xFF10B981),
                Color(0xFF22D3EE),
                Color(0xFF6366F1),
                Color(0xFFEC4899),
                Color(0xFFEF4444)
            ),
            strokeDp = 4.2f,
            animated = true,
            shapeStyle = FrameShapeStyle.DOUBLE_STARBURST
        ),
        ProfileFrameItem(
            id = "galaxy_infinity",
            title = "Galaxy Infinity Ring",
            subtitle = "Dual counter-spinning cosmic scanner & gold sparks",
            price = 750L,
            ringColors = listOf(Color(0xFF4F46E5), Color(0xFF22D3EE), Color(0xFFFBBF24), Color(0xFFEC4899), Color(0xFF4F46E5)),
            strokeDp = 4.3f,
            animated = true,
            shapeStyle = FrameShapeStyle.MATRIX_DASHED
        )
    )

    val profileEffects: List<ProfileVideoEffectItem> = listOf(
        ProfileVideoEffectItem(
            id = "none",
            title = "No Video Intro",
            subtitle = "Standard static Discord banner",
            price = 0L,
            accentColors = listOf(Color(0xFF334155), Color(0xFF1E293B)),
            rawResId = null
        ),
        ProfileVideoEffectItem(
            id = "cyber_wave",
            title = "Cyber Pulse Wave (5s Video)",
            subtitle = "Electric neon blurple motion loop on profile open",
            price = 100L,
            accentColors = listOf(Color(0xFF5865F2), Color(0xFF22D3EE)),
            rawResId = R.raw.anim_cyber_wave
        ),
        ProfileVideoEffectItem(
            id = "solar_flame",
            title = "Solar Flare Drift (5s Video)",
            subtitle = "Molten amber & ember waves across your profile",
            price = 150L,
            accentColors = listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
            rawResId = R.raw.anim_solar_flame
        ),
        ProfileVideoEffectItem(
            id = "emerald_matrix",
            title = "Emerald Surge (5s Video)",
            subtitle = "Flowing neon emerald waves behind your card",
            price = 200L,
            accentColors = listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
            rawResId = R.raw.anim_emerald_matrix
        ),
        ProfileVideoEffectItem(
            id = "royal_galaxy",
            title = "Cosmic Galaxy Spin (5s Video)",
            subtitle = "Swirling violet & indigo nebula entrance video",
            price = 250L,
            accentColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
            rawResId = R.raw.anim_royal_galaxy
        ),
        ProfileVideoEffectItem(
            id = "custom_video",
            title = "Your Custom 5s Video",
            subtitle = "Pick any video from gallery — auto-trimmed to 5 seconds",
            price = 200L,
            accentColors = listOf(Color(0xFF22D3EE), Color(0xFF5865F2)),
            rawResId = null,
            isCustomUpload = true,
            isUnlimitedDuration = false
        ),
        ProfileVideoEffectItem(
            id = "custom_video_unlimited",
            title = "Unlimited Full Video (∞)",
            subtitle = "Play your entire full-length gallery video with unlimited time (no 5s limit!)",
            price = 1000L,
            accentColors = listOf(Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF6366F1)),
            rawResId = null,
            isCustomUpload = true,
            isUnlimitedDuration = true
        )
    )

    fun findById(frameId: String?): ProfileFrameItem {
        return frames.firstOrNull { it.id == frameId } ?: frames.first()
    }

    fun findEffectById(effectId: String?): ProfileVideoEffectItem {
        return profileEffects.firstOrNull { it.id == effectId } ?: profileEffects.first()
    }
}
