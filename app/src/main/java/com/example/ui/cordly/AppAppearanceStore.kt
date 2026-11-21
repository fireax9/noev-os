package com.example.ui.cordly

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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
    val colors: List<Color>,
    val requiredRank: String = "normal",
    @DrawableRes val artworkResId: Int? = null
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
        ),
        // Exclusive HD Anime Banners — strictly Dragon Ball, Jujutsu Kaisen & Blue Lock ONLY
        ProfileBannerPreset(
            id = "dbz_aura_banner",
            titleEn = "DBZ: Kamehameha",
            titleAr = "دراغون بول (ألترا)",
            pattern = BannerPatternType.SUNSET_RAYS,
            colors = listOf(Color(0xFF1E1200), Color(0xFFF59E0B), Color(0xFFFDE047)),
            requiredRank = "ultra",
            artworkResId = R.drawable.img_anime_dbz
        ),
        ProfileBannerPreset(
            id = "jjk_domain_banner",
            titleEn = "Gojo: Unlimited Void",
            titleAr = "غوجو: توسيع المجال",
            pattern = BannerPatternType.PRISM_SHARDS,
            colors = listOf(Color(0xFF12051F), Color(0xFF9333EA), Color(0xFF38BDF8)),
            requiredRank = "ultra",
            artworkResId = R.drawable.img_anime_gojo
        ),
        ProfileBannerPreset(
            id = "jjk_sukuna_banner",
            titleEn = "Sukuna: Shrine Hands",
            titleAr = "سكونا: توسيع المجال",
            pattern = BannerPatternType.SUNSET_RAYS,
            colors = listOf(Color(0xFF1A0509), Color(0xFFEF4444), Color(0xFF7F1D1D)),
            requiredRank = "neo_plus",
            artworkResId = R.drawable.img_anime_sukuna
        ),
        ProfileBannerPreset(
            id = "bluelock_flow_banner",
            titleEn = "Blue Lock: Eyes & Ball",
            titleAr = "بلو لوك (ألترا)",
            pattern = BannerPatternType.HEX_CYBER,
            colors = listOf(Color(0xFF020C1E), Color(0xFF1D4ED8), Color(0xFF22D3EE)),
            requiredRank = "ultra",
            artworkResId = R.drawable.img_anime_bluelock
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

enum class AppLogoVariant(
    val storageKey: String,
    val aliasName: String,
    val titleEn: String,
    val titleAr: String,
    val badgeColor: Color
) {
    DEFAULT("default", "com.example.LauncherDefault", "Discord Blue", "أزرق ديسكورد", Color(0xFF5865F2)),
    GOLD("gold", "com.example.LauncherGold", "Neo+ Gold", "ذهبي نيو بلس", Color(0xFFF59E0B)),
    CRIMSON("crimson", "com.example.LauncherCrimson", "Crimson Blaze", "أحمر ملكي", Color(0xFFEF4444)),
    EMERALD("emerald", "com.example.LauncherEmerald", "Cyber Emerald", "زمردي سيبراني", Color(0xFF10B981));

    companion object {
        fun fromKey(key: String?): AppLogoVariant {
            return entries.firstOrNull { it.storageKey == key } ?: DEFAULT
        }
    }
}

data class AppUiLayoutPrefs(
    val railOnRight: Boolean = false,
    val compactMessageCards: Boolean = false
)

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
    private const val KEY_APP_LOGO = "app_launcher_logo"
    private const val KEY_RAIL_ON_RIGHT = "layout_rail_on_right"
    private const val KEY_COMPACT_CARDS = "layout_compact_cards"
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

    fun customAvatarVideoFile(context: Context, uid: String): File {
        return File(context.filesDir, "custom_avatar_video_${uid}.mp4")
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

    fun loadLogoVariant(context: Context): AppLogoVariant {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppLogoVariant.fromKey(prefs.getString(KEY_APP_LOGO, null))
    }

    fun applyAppLogoVariant(context: Context, variant: AppLogoVariant): AppLogoVariant {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_LOGO, variant.storageKey).apply()
        val pm = context.packageManager
        val pkg = context.packageName
        // CRITICAL: Enable the newly selected LAUNCHER alias FIRST before disabling others,
        // otherwise Android sees zero enabled LAUNCHER activities transiently and kills/resets the task.
        try {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, variant.aliasName),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {
        }
        AppLogoVariant.entries
            .filter { it != variant }
            .forEach { entry ->
                try {
                    pm.setComponentEnabledSetting(
                        ComponentName(pkg, entry.aliasName),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (_: Exception) {
                }
            }
        return variant
    }

    fun loadLayoutPrefs(context: Context): AppUiLayoutPrefs {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppUiLayoutPrefs(
            railOnRight = prefs.getBoolean(KEY_RAIL_ON_RIGHT, false),
            compactMessageCards = prefs.getBoolean(KEY_COMPACT_CARDS, false)
        )
    }

    fun saveLayoutPrefs(context: Context, layout: AppUiLayoutPrefs): AppUiLayoutPrefs {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_RAIL_ON_RIGHT, layout.railOnRight)
            .putBoolean(KEY_COMPACT_CARDS, layout.compactMessageCards)
            .apply()
        return layout
    }
}
