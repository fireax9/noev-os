package com.example.ui.cordly

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class FrameShapeStyle {
    CIRCLE_ORBIT,
    OCTAGON_CYBER,
    PRISM_DIAMOND,
    DOUBLE_STARBURST,
    FLAME_CROWN,
    MATRIX_DASHED,
    SUKUNA_SHRINE_HANDS
}

data class ProfileFrameItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val price: Long,
    val ringColors: List<Color>,
    val strokeDp: Float = 3f,
    val animated: Boolean = false,
    val shapeStyle: FrameShapeStyle = FrameShapeStyle.CIRCLE_ORBIT,
    val category: String = "standard", // "standard" | "gaming" | "anime"
    val requiredRank: String = "normal", // "normal" | "ultra" | "neo_plus"
    @DrawableRes val artworkResId: Int? = null
)

data class ProfileCardFrameItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val price: Long,
    val borderColors: List<Color>,
    val accentColor: Color,
    val requiredRank: String = "normal", // "normal" | "ultra" | "neo_plus"
    val category: String = "standard", // "standard" | "gaming" | "anime"
    @DrawableRes val artworkResId: Int? = null
)

data class ProfileVideoEffectItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val price: Long,
    val accentColors: List<Color>,
    val rawResId: Int? = null,
    val isCustomUpload: Boolean = false,
    val isUnlimitedDuration: Boolean = false,
    val requiredRank: String = "normal"
)

data class ChatStickerItem(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val emoji: String,
    val accentColor: Color
)

data class ChatGifPreset(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val symbol: String
)

object PeyShopCatalog {
    // Circular Avatar Frames ONLY (no square avatar frames)
    val frames: List<ProfileFrameItem> = listOf(
        ProfileFrameItem(
            id = "none",
            title = "Classic Steel",
            subtitle = "Clean forged-iron circular ring",
            price = 0L,
            ringColors = listOf(Color(0xFF475569), Color(0xFF334155)),
            strokeDp = 1.5f,
            animated = false,
            shapeStyle = FrameShapeStyle.CIRCLE_ORBIT,
            category = "standard"
        ),
        ProfileFrameItem(
            id = "neon_cyber",
            title = "Neon Cyber Scanner",
            subtitle = "Spinning dashed cyan & blurple cyber ring",
            price = 150L,
            ringColors = listOf(Color(0xFF22D3EE), Color(0xFF5865F2), Color(0xFF22D3EE)),
            strokeDp = 3.4f,
            animated = true,
            shapeStyle = FrameShapeStyle.MATRIX_DASHED,
            category = "standard"
        ),
        ProfileFrameItem(
            id = "arctic_crystal",
            title = "Arctic Prism Diamond",
            subtitle = "Twin faceted ice-blue rotating diamond star",
            price = 180L,
            ringColors = listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFFE0F2FE)),
            strokeDp = 3.5f,
            animated = true,
            shapeStyle = FrameShapeStyle.PRISM_DIAMOND,
            category = "standard"
        ),
        ProfileFrameItem(
            id = "molten_gold",
            title = "Solar Champion",
            subtitle = "Blazing gold crown flares & ember halo",
            price = 200L,
            ringColors = listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFFBBF24)),
            strokeDp = 3.6f,
            animated = true,
            shapeStyle = FrameShapeStyle.FLAME_CROWN,
            category = "standard"
        ),
        ProfileFrameItem(
            id = "emerald_matrix",
            title = "Emerald Octagon Shield",
            subtitle = "Sharp octagonal neon emerald cyber ring",
            price = 250L,
            ringColors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF06B6D4), Color(0xFF10B981)),
            strokeDp = 3.6f,
            animated = true,
            shapeStyle = FrameShapeStyle.OCTAGON_CYBER,
            category = "standard"
        ),
        ProfileFrameItem(
            id = "inferno_crown",
            title = "Royal Phoenix Crown",
            subtitle = "Radiant flame spikes & crimson-gold crest",
            price = 300L,
            ringColors = listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFFDE047), Color(0xFFF59E0B)),
            strokeDp = 3.8f,
            animated = true,
            shapeStyle = FrameShapeStyle.FLAME_CROWN,
            category = "standard"
        ),
        ProfileFrameItem(
            id = "sakura_bloom",
            title = "Sakura Orbit Blossom",
            subtitle = "Silky rose-pink gradient halo & floating petals",
            price = 320L,
            ringColors = listOf(Color(0xFFEC4899), Color(0xFFF9A8D4), Color(0xFFFBBF24), Color(0xFFEC4899)),
            strokeDp = 3.6f,
            animated = true,
            shapeStyle = FrameShapeStyle.CIRCLE_ORBIT,
            category = "standard"
        ),
        ProfileFrameItem(
            id = "diamond_pixel",
            title = "Voxel Diamond Crest",
            subtitle = "Crystalline twin-diamond sky & cobalt crest",
            price = 350L,
            ringColors = listOf(Color(0xFF38BDF8), Color(0xFFE0F2FE), Color(0xFF3B82F6), Color(0xFF38BDF8)),
            strokeDp = 3.8f,
            animated = true,
            shapeStyle = FrameShapeStyle.PRISM_DIAMOND,
            category = "standard"
        ),
        ProfileFrameItem(
            id = "crimson_halo",
            title = "Crimson Starburst",
            subtitle = "Fierce ruby & magenta starburst aura",
            price = 400L,
            ringColors = listOf(Color(0xFFF43F5E), Color(0xFFEC4899), Color(0xFF9333EA), Color(0xFFF43F5E)),
            strokeDp = 3.8f,
            animated = true,
            shapeStyle = FrameShapeStyle.DOUBLE_STARBURST,
            category = "standard"
        ),
        ProfileFrameItem(
            id = "void_glitch",
            title = "Void Hex Glitch",
            subtitle = "Dark violet & electric neon octagonal ring",
            price = 450L,
            ringColors = listOf(Color(0xFF8B5CF6), Color(0xFFD946EF), Color(0xFF22D3EE), Color(0xFF8B5CF6)),
            strokeDp = 4.0f,
            animated = true,
            shapeStyle = FrameShapeStyle.OCTAGON_CYBER,
            category = "standard"
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
            shapeStyle = FrameShapeStyle.DOUBLE_STARBURST,
            category = "standard"
        ),
        ProfileFrameItem(
            id = "galaxy_infinity",
            title = "Galaxy Infinity Ring",
            subtitle = "Dual counter-spinning cosmic scanner & gold sparks",
            price = 750L,
            ringColors = listOf(Color(0xFF4F46E5), Color(0xFF22D3EE), Color(0xFFFBBF24), Color(0xFFEC4899), Color(0xFF4F46E5)),
            strokeDp = 4.3f,
            animated = true,
            shapeStyle = FrameShapeStyle.MATRIX_DASHED,
            category = "standard"
        ),
        // Gaming Circular Frames (Ultra / Neo+)
        ProfileFrameItem(
            id = "gaming_rgb",
            title = "Gaming Pro RGB",
            subtitle = "Ultra esports RGB overclocked ring",
            price = 500L,
            ringColors = listOf(Color(0xFFEF4444), Color(0xFF22D3EE), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444)),
            strokeDp = 4.2f,
            animated = true,
            shapeStyle = FrameShapeStyle.OCTAGON_CYBER,
            category = "gaming",
            requiredRank = "ultra"
        ),
        ProfileFrameItem(
            id = "gaming_esports",
            title = "Esports Cyber Orbit",
            subtitle = "Circular tournament HUD energy halo",
            price = 700L,
            ringColors = listOf(Color(0xFF38BDF8), Color(0xFF10B981), Color(0xFF6366F1), Color(0xFF38BDF8)),
            strokeDp = 4.4f,
            animated = true,
            shapeStyle = FrameShapeStyle.CIRCLE_ORBIT,
            category = "gaming",
            requiredRank = "ultra"
        ),
        // HD Anime Circular Avatar Frames — STRICTLY Dragon Ball, Jujutsu Kaisen & Blue Lock ONLY
        ProfileFrameItem(
            id = "dbz_super_saiyan",
            title = "Dragon Ball: Super Saiyan",
            subtitle = "Super Saiyan warrior & Kamehameha emblem ring",
            price = 550L,
            ringColors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFF22D3EE), Color(0xFFFDE047)),
            strokeDp = 4.4f,
            animated = true,
            shapeStyle = FrameShapeStyle.DOUBLE_STARBURST,
            category = "anime",
            requiredRank = "ultra",
            artworkResId = R.drawable.img_anime_dbz
        ),
        ProfileFrameItem(
            id = "dbz_kamehameha_sphere",
            title = "Dragon Ball: Kamehameha",
            subtitle = "Blue energy Kamehameha blast circular crest",
            price = 800L,
            ringColors = listOf(Color(0xFF38BDF8), Color(0xFF22D3EE), Color(0xFFFDE047), Color(0xFF38BDF8)),
            strokeDp = 4.5f,
            animated = true,
            shapeStyle = FrameShapeStyle.FLAME_CROWN,
            category = "anime",
            requiredRank = "ultra",
            artworkResId = R.drawable.img_anime_dbz
        ),
        ProfileFrameItem(
            id = "jjk_gojo_hollow",
            title = "Gojo: Six Eyes & Void",
            subtitle = "Gojo Six Eyes & Unlimited Void circular ring",
            price = 850L,
            ringColors = listOf(Color(0xFFA855F7), Color(0xFF38BDF8), Color(0xFFEC4899), Color(0xFFA855F7)),
            strokeDp = 4.5f,
            animated = true,
            shapeStyle = FrameShapeStyle.CIRCLE_ORBIT,
            category = "anime",
            requiredRank = "ultra",
            artworkResId = R.drawable.img_anime_gojo
        ),
        ProfileFrameItem(
            id = "jjk_sukuna_hands",
            title = "Sukuna: Shrine Hands",
            subtitle = "Neo+ blazing Malevolent Shrine Domain Expansion hands frame",
            price = 1100L,
            ringColors = listOf(Color(0xFFFF4500), Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF991B1B), Color(0xFFFF4500)),
            strokeDp = 4.8f,
            animated = true,
            shapeStyle = FrameShapeStyle.SUKUNA_SHRINE_HANDS,
            category = "anime",
            requiredRank = "neo_plus",
            artworkResId = null
        ),
        ProfileFrameItem(
            id = "bluelock_egoist",
            title = "Blue Lock: Eyes & Ball",
            subtitle = "Glowing striker eyes & blazing ball circular ring",
            price = 600L,
            ringColors = listOf(Color(0xFF1D4ED8), Color(0xFF22D3EE), Color(0xFF38BDF8), Color(0xFF22D3EE)),
            strokeDp = 4.4f,
            animated = true,
            shapeStyle = FrameShapeStyle.OCTAGON_CYBER,
            category = "anime",
            requiredRank = "ultra",
            artworkResId = R.drawable.img_anime_bluelock
        ),
        ProfileFrameItem(
            id = "bluelock_isagi_aura",
            title = "Blue Lock: Spatial Flow",
            subtitle = "Neon cyan striker chain aura ring",
            price = 800L,
            ringColors = listOf(Color(0xFF10B981), Color(0xFF22D3EE), Color(0xFF1D4ED8), Color(0xFF10B981)),
            strokeDp = 4.4f,
            animated = true,
            shapeStyle = FrameShapeStyle.MATRIX_DASHED,
            category = "anime",
            requiredRank = "ultra",
            artworkResId = R.drawable.img_anime_bluelock
        )
    )

    // Full Profile Card Frames (wrapping the full profile card popup with banner, avatar & bio)
    val cardFrames: List<ProfileCardFrameItem> = listOf(
        ProfileCardFrameItem(
            id = "none",
            title = "Default Clean Card",
            subtitle = "Standard sleek Discord dark profile card",
            price = 0L,
            borderColors = listOf(Color(0xFF334155), Color(0xFF5865F2)),
            accentColor = Color(0xFF5865F2),
            category = "standard"
        ),
        ProfileCardFrameItem(
            id = "card_neon_blurple",
            title = "Neon Blurple Edge",
            subtitle = "Electric Discord blue & cyan full card frame",
            price = 300L,
            borderColors = listOf(Color(0xFF5865F2), Color(0xFF22D3EE), Color(0xFF8B5CF6), Color(0xFF5865F2)),
            accentColor = Color(0xFF22D3EE),
            category = "standard"
        ),
        ProfileCardFrameItem(
            id = "card_royal_gold",
            title = "Imperial Gold Card",
            subtitle = "Gleaming championship gold & ember full card border",
            price = 450L,
            borderColors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFFDE047)),
            accentColor = Color(0xFFF59E0B),
            category = "standard"
        ),
        ProfileCardFrameItem(
            id = "card_emerald_cyber",
            title = "Emerald Cyber HUD",
            subtitle = "Futuristic neon emerald & cyan profile card glow",
            price = 500L,
            borderColors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF22D3EE), Color(0xFF10B981)),
            accentColor = Color(0xFF10B981),
            category = "standard"
        ),
        ProfileCardFrameItem(
            id = "card_gaming_rgb",
            title = "Esports RGB Card",
            subtitle = "Ultra Pro RGB illuminated tournament full card",
            price = 700L,
            borderColors = listOf(Color(0xFFEF4444), Color(0xFF22D3EE), Color(0xFF10B981), Color(0xFFF59E0B)),
            accentColor = Color(0xFF22D3EE),
            requiredRank = "ultra",
            category = "gaming"
        ),
        // Anime Full Profile Card Frames — Strictly Dragon Ball, Jujutsu Kaisen & Blue Lock ONLY
        ProfileCardFrameItem(
            id = "card_dbz_kamehameha",
            title = "Dragon Ball: Kamehameha Card",
            subtitle = "Super Saiyan warrior & Kamehameha full profile frame",
            price = 850L,
            borderColors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFF22D3EE), Color(0xFFFDE047)),
            accentColor = Color(0xFFFDE047),
            requiredRank = "ultra",
            category = "anime",
            artworkResId = R.drawable.img_anime_dbz
        ),
        ProfileCardFrameItem(
            id = "card_jjk_gojo_void",
            title = "Gojo: Six Eyes & Void Card",
            subtitle = "Glowing Six Eyes & Unlimited Void full profile frame",
            price = 900L,
            borderColors = listOf(Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF38BDF8)),
            accentColor = Color(0xFFA855F7),
            requiredRank = "ultra",
            category = "anime",
            artworkResId = R.drawable.img_anime_gojo
        ),
        ProfileCardFrameItem(
            id = "card_bluelock_striker",
            title = "Blue Lock: Eyes & Ball Card",
            subtitle = "Glowing striker eyes & blazing soccer ball full card",
            price = 850L,
            borderColors = listOf(Color(0xFF1D4ED8), Color(0xFF22D3EE), Color(0xFF10B981), Color(0xFF1D4ED8)),
            accentColor = Color(0xFF22D3EE),
            requiredRank = "ultra",
            category = "anime",
            artworkResId = R.drawable.img_anime_bluelock
        ),
        ProfileCardFrameItem(
            id = "card_jjk_sukuna_shrine",
            title = "Sukuna: Domain Hands Card",
            subtitle = "Neo+ Malevolent Shrine hands framing your entire profile",
            price = 1200L,
            borderColors = listOf(Color(0xFFEF4444), Color(0xFF991B1B), Color(0xFF9333EA), Color(0xFFEF4444)),
            accentColor = Color(0xFFEF4444),
            requiredRank = "neo_plus",
            category = "anime",
            artworkResId = R.drawable.img_anime_sukuna
        )
    )

    val profileEffects: List<ProfileVideoEffectItem> = listOf(
        ProfileVideoEffectItem(
            id = "none",
            title = "No Video Intro",
            subtitle = "Standard static banner",
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
        // Anime Video Intros — strictly Dragon Ball, Jujutsu Kaisen, Blue Lock ONLY
        ProfileVideoEffectItem(
            id = "dbz_saiyan_intro",
            title = "Dragon Ball: Ki Surge (HD)",
            subtitle = "Golden Super Saiyan flare video loop (Ultra)",
            price = 350L,
            accentColors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B)),
            rawResId = R.raw.anim_solar_flame,
            requiredRank = "ultra"
        ),
        ProfileVideoEffectItem(
            id = "jjk_domain_intro",
            title = "Jujutsu Kaisen: Infinity (HD)",
            subtitle = "Violet cursed energy vortex loop (Ultra)",
            price = 350L,
            accentColors = listOf(Color(0xFF9333EA), Color(0xFFEF4444)),
            rawResId = R.raw.anim_royal_galaxy,
            requiredRank = "ultra"
        ),
        ProfileVideoEffectItem(
            id = "bluelock_ego_intro",
            title = "Blue Lock: Ego Flow (HD)",
            subtitle = "Electric striker aura wave loop (Ultra)",
            price = 350L,
            accentColors = listOf(Color(0xFF1D4ED8), Color(0xFF22D3EE)),
            rawResId = R.raw.anim_cyber_wave,
            requiredRank = "ultra"
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
            subtitle = "Play your entire full-length gallery video with unlimited time (1000 P E Y)",
            price = 1000L,
            accentColors = listOf(Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF6366F1)),
            rawResId = null,
            isCustomUpload = true,
            isUnlimitedDuration = true
        )
    )

    val stickers: List<ChatStickerItem> = listOf(
        ChatStickerItem("stk_fire", "On Fire", "حماس", "🔥", Color(0xFFEF4444)),
        ChatStickerItem("stk_gg", "GG Pro", "فوز", "🏆", Color(0xFFF59E0B)),
        ChatStickerItem("stk_heart", "Big Love", "حب", "💖", Color(0xFFEC4899)),
        ChatStickerItem("stk_laugh", "LOL", "ضحك", "😂", Color(0xFFFBBF24)),
        ChatStickerItem("stk_crown", "King", "ملك", "👑", Color(0xFFF59E0B)),
        ChatStickerItem("stk_bolt", "Power", "طاقة", "⚡", Color(0xFF22D3EE)),
        ChatStickerItem("stk_rocket", "Boost", "انطلاق", "🚀", Color(0xFF5865F2)),
        ChatStickerItem("stk_party", "Party", "احتفال", "🎉", Color(0xFF10B981)),
        ChatStickerItem("stk_cool", "Cool", "رائع", "😎", Color(0xFF38BDF8)),
        ChatStickerItem("stk_eyes", "Watching", "مراقب", "👀", Color(0xFFA855F7)),
        ChatStickerItem("stk_salute", "Respect", "تحية", "🫡", Color(0xFF34D399)),
        ChatStickerItem("stk_star", "Mythic", "أسطوري", "🌟", Color(0xFFFDE047))
    )

    val gifPresets: List<ChatGifPreset> = listOf(
        ChatGifPreset("gif_saiyan", "Power Aura", "هالة القوة", Color(0xFFF59E0B), Color(0xFF22D3EE), "⚡"),
        ChatGifPreset("gif_domain", "Void Pulse", "نبض الفراغ", Color(0xFF9333EA), Color(0xFFEF4444), "🌀"),
        ChatGifPreset("gif_ego", "Striker Goal", "هدف أسطوري", Color(0xFF1D4ED8), Color(0xFF22D3EE), "⚽"),
        ChatGifPreset("gif_confetti", "Celebration", "احتفال متحرك", Color(0xFFEC4899), Color(0xFFFBBF24), "🎊"),
        ChatGifPreset("gif_heart", "Heart Beat", "نبض القلب", Color(0xFFF43F5E), Color(0xFFF472B6), "💓"),
        ChatGifPreset("gif_cyber", "Cyber Scan", "مسح سيبراني", Color(0xFF22D3EE), Color(0xFF5865F2), "🛰️"),
        ChatGifPreset("gif_gg", "Victory Glow", "بريق النصر", Color(0xFF10B981), Color(0xFFFDE047), "👑"),
        ChatGifPreset("gif_music", "Vibe Wave", "موجة موسيقية", Color(0xFF6366F1), Color(0xFF34D399), "🎵")
    )

    fun findById(frameId: String?): ProfileFrameItem {
        return frames.firstOrNull { it.id == frameId } ?: frames.first()
    }

    fun findCardFrameById(cardFrameId: String?): ProfileCardFrameItem {
        return cardFrames.firstOrNull { it.id == cardFrameId } ?: cardFrames.first()
    }

    fun findEffectById(effectId: String?): ProfileVideoEffectItem {
        return profileEffects.firstOrNull { it.id == effectId } ?: profileEffects.first()
    }

    fun findStickerById(stickerId: String?): ChatStickerItem? {
        return stickers.firstOrNull { it.id == stickerId }
    }

    fun findGifById(gifId: String?): ChatGifPreset? {
        return gifPresets.firstOrNull { it.id == gifId }
    }
}

sealed interface DecodedCustomSticker {
    data class ImageSticker(val dataUri: String) : DecodedCustomSticker
    data class EmojiSticker(val emoji: String, val accentHex: String, val title: String) : DecodedCustomSticker
}

object CustomStickerCodec {
    private const val PREFIX_IMAGE = "custom_img:"
    private const val PREFIX_EMOJI = "custom_emoji:"

    fun encodeImageSticker(imageDataUri: String): String = "$PREFIX_IMAGE$imageDataUri"

    fun encodeEmojiSticker(emoji: String, accentHex: String, title: String): String {
        val safeEmoji = emoji.replace("|", "").ifBlank { "✨" }
        val safeHex = accentHex.replace("|", "").ifBlank { "#5865F2" }
        val safeTitle = title.replace("|", "").ifBlank { "Sticker" }
        return "$PREFIX_EMOJI$safeEmoji|$safeHex|$safeTitle"
    }

    fun encodeForMessage(sticker: com.example.data.model.CustomSticker): String {
        val img = sticker.imageData
        return if (!img.isNullOrBlank()) {
            encodeImageSticker(img)
        } else {
            encodeEmojiSticker(sticker.emoji, sticker.accentHex, sticker.title)
        }
    }

    fun decode(mediaData: String?): DecodedCustomSticker? {
        if (mediaData.isNullOrBlank()) return null
        return when {
            mediaData.startsWith(PREFIX_IMAGE) -> {
                val uri = mediaData.removePrefix(PREFIX_IMAGE)
                if (uri.isNotBlank()) DecodedCustomSticker.ImageSticker(uri) else null
            }
            mediaData.startsWith("data:image") -> {
                DecodedCustomSticker.ImageSticker(mediaData)
            }
            mediaData.startsWith(PREFIX_EMOJI) -> {
                val parts = mediaData.removePrefix(PREFIX_EMOJI).split("|", limit = 3)
                DecodedCustomSticker.EmojiSticker(
                    emoji = parts.getOrNull(0)?.ifBlank { "✨" } ?: "✨",
                    accentHex = parts.getOrNull(1)?.ifBlank { "#5865F2" } ?: "#5865F2",
                    title = parts.getOrNull(2)?.ifBlank { "Sticker" } ?: "Sticker"
                )
            }
            else -> null
        }
    }
}

