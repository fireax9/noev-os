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
    CUSTOM("custom", "Custom Photo");

    @DrawableRes
    fun drawableRes(): Int? = when (this) {
        PRESET_1 -> R.drawable.bg_preset_1
        PRESET_2 -> R.drawable.bg_preset_2
        PRESET_3 -> R.drawable.bg_preset_3
        else -> null
    }

    companion object {
        fun fromKey(key: String?): AppWallpaperMode {
            return entries.firstOrNull { it.storageKey == key } ?: DEFAULT
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
}

data class ProfileFrameItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val price: Long,
    val ringColors: List<Color>,
    val strokeDp: Float = 3f,
    val animated: Boolean = false
)

data class ProfileVideoEffectItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val price: Long,
    val accentColors: List<Color>,
    val rawResId: Int? = null,
    val isCustomUpload: Boolean = false
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
            animated = false
        ),
        ProfileFrameItem(
            id = "neon_cyber",
            title = "Neon Cyber Pulse",
            subtitle = "Spinning cyan & blurple orbital ring",
            price = 150L,
            ringColors = listOf(Color(0xFF22D3EE), Color(0xFF5865F2), Color(0xFF22D3EE)),
            strokeDp = 3.4f,
            animated = true
        ),
        ProfileFrameItem(
            id = "molten_gold",
            title = "Solar Champion",
            subtitle = "Spinning gold & blazing ember halo",
            price = 200L,
            ringColors = listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFFBBF24)),
            strokeDp = 3.6f,
            animated = true
        ),
        ProfileFrameItem(
            id = "emerald_matrix",
            title = "Emerald Overdrive",
            subtitle = "Arcade emerald vortex ring",
            price = 250L,
            ringColors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF06B6D4), Color(0xFF10B981)),
            strokeDp = 3.6f,
            animated = true
        ),
        ProfileFrameItem(
            id = "diamond_pixel",
            title = "Voxel Diamond Crest",
            subtitle = "Crystalline sky & cobalt turbine",
            price = 350L,
            ringColors = listOf(Color(0xFF38BDF8), Color(0xFFE0F2FE), Color(0xFF3B82F6), Color(0xFF38BDF8)),
            strokeDp = 3.8f,
            animated = true
        ),
        ProfileFrameItem(
            id = "crimson_halo",
            title = "Crimson Striker",
            subtitle = "Fierce ruby & magenta pulse aura",
            price = 400L,
            ringColors = listOf(Color(0xFFF43F5E), Color(0xFFEC4899), Color(0xFF9333EA), Color(0xFFF43F5E)),
            strokeDp = 3.8f,
            animated = true
        ),
        ProfileFrameItem(
            id = "prismatic_aura",
            title = "Prismatic Mythic",
            subtitle = "Full-spectrum rotating cosmic halo",
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
            animated = true
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
            isCustomUpload = true
        )
    )

    fun findById(frameId: String?): ProfileFrameItem {
        return frames.firstOrNull { it.id == frameId } ?: frames.first()
    }

    fun findEffectById(effectId: String?): ProfileVideoEffectItem {
        return profileEffects.firstOrNull { it.id == effectId } ?: profileEffects.first()
    }
}
