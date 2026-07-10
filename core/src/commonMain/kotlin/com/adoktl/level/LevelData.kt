package com.adoktl.level

import com.adoktl.math.Vector2

data class LevelSettings(
    val version: Int = 18,
    val artist: String = "",
    val specialArtistType: String = "None",
    val artistPermission: String = "",
    val song: String = "",
    val author: String = "",
    val separateCountdownTime: Boolean = true,
    val previewImage: String = "",
    val previewIcon: String = "",
    val previewIconColor: String = "003f52",
    val previewSongStart: Double = 0.0,
    val previewSongDuration: Double = 10.0,
    val seizureWarning: Boolean = false,
    val levelDesc: String = "",
    val levelTags: String = "",
    val artistLinks: String = "",
    val speedTrialAim: Int = 0,
    val difficulty: Int = 1,
    val requiredMods: List<String> = emptyList(),
    val songFilename: String = "",
    val bpm: Double = 100.0,
    val volume: Int = 100,
    val offset: Double = 0.0,
    val pitch: Int = 100,
    val hitsound: String = "Kick",
    val hitsoundVolume: Int = 100,
    val countdownTicks: Int = 4,
    val tileShape: String = "Long",
    val trackColorType: String = "Single",
    val trackColor: String = "debb7b",
    val secondaryTrackColor: String = "ffffff",
    val trackColorAnimDuration: Double = 2.0,
    val trackColorPulse: String = "None",
    val trackPulseLength: Int = 10,
    val trackStyle: String = "Standard",
    val trackTexture: String = "",
    val trackTextureScale: Double = 1.0,
    val trackGlowIntensity: Int = 100,
    val trackAnimation: String = "None",
    val beatsAhead: Int = 3,
    val trackDisappearAnimation: String = "None",
    val beatsBehind: Int = 4,
    val backgroundColor: String = "000000",
    val showDefaultBGIfNoImage: Boolean = true,
    val showDefaultBGTile: Boolean = true,
    val defaultBGTileColor: String = "101121",
    val defaultBGShapeType: String = "Default",
    val defaultBGShapeColor: String = "ffffff",
    val bgImage: String = "",
    val bgImageColor: String = "ffffff",
    val parallax: Vector2 = Vector2(100.0, 100.0),
    val bgDisplayMode: String = "FitToScreen",
    val imageSmoothing: Boolean = true,
    val lockRot: Boolean = false,
    val loopBG: Boolean = false,
    val scalingRatio: Int = 100,
    val relativeTo: String = "Player",
    val position: Vector2 = Vector2(0.0, 0.0),
    val rotation: Double = 0.0,
    val zoom: Int = 100,
    val pulseOnFloor: Boolean = true,
    val bgVideo: String = "",
    val loopVideo: Boolean = false,
    val vidOffset: Int = 0,
    val floorIconOutlines: Boolean = false,
    val stickToFloors: Boolean = true,
    val planetEase: String = "Linear",
    val planetEaseParts: Int = 1,
    val planetEasePartBehavior: String = "Mirror",
    val defaultTextColor: String = "ffffff",
    val defaultTextShadowColor: String = "00000050",
    val congratsText: String = "",
    val perfectText: String = "",
    val legacyFlash: Boolean = false,
    val legacyCamRelativeTo: Boolean = false,
    val legacySpriteTiles: Boolean = false,
    val legacyTween: Boolean = false,
    val disableV15Features: Boolean = false,
    val legacyPause: Boolean = false
)

data class TileData(
    val index: Int = 0,
    val angle: Double = 180.0,
    val direction: Double? = null,
    val position: Vector2 = Vector2(0.0, 0.0),
    val floor: Int = 0
)

data class LevelData(
    val angleData: List<Double>,
    val settings: LevelSettings,
    val actions: List<LevelAction> = emptyList(),
    val decorations: List<DecorationAction> = emptyList()
)

data class LevelAction(
    val floor: Int,
    val eventType: String,
    val rawData: Map<String, Any?>
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): LevelAction {
            val floor = (map["floor"] as? Number)?.toInt() ?: 0
            val eventType = (map["eventType"] as? String) ?: ""
            return LevelAction(floor, eventType, map)
        }
    }

    fun getString(key: String, default: String = ""): String =
        (rawData[key] as? String) ?: default

    fun getDouble(key: String, default: Double = 0.0): Double =
        (rawData[key] as? Number)?.toDouble() ?: default

    fun getInt(key: String, default: Int = 0): Int =
        (rawData[key] as? Number)?.toInt() ?: default

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        (rawData[key] as? Boolean) ?: default

    fun getList(key: String): List<Any?>? = rawData[key] as? List<Any?>

    @Suppress("UNCHECKED_CAST")
    fun getMap(key: String): Map<String, Any?>? = rawData[key] as? Map<String, Any?>

    fun getDoubleList(key: String): List<Double> =
        (rawData[key] as? List<*>)?.filterIsInstance<Number>()?.map { it.toDouble() } ?: emptyList()

    fun has(key: String): Boolean = rawData.containsKey(key)
}

data class DecorationAction(
    val floor: Int? = null,
    val eventType: String,
    val rawData: Map<String, Any?>
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): DecorationAction {
            val floor = (map["floor"] as? Number)?.toInt()
            val eventType = (map["eventType"] as? String) ?: ""
            return DecorationAction(floor, eventType, map)
        }
    }

    fun getString(key: String, default: String = ""): String =
        (rawData[key] as? String) ?: default
    fun getDouble(key: String, default: Double = 0.0): Double =
        (rawData[key] as? Number)?.toDouble() ?: default
    fun getInt(key: String, default: Int = 0): Int =
        (rawData[key] as? Number)?.toInt() ?: default
    fun getBoolean(key: String, default: Boolean = false): Boolean =
        (rawData[key] as? Boolean) ?: default
    fun getList(key: String): List<Any?>? = rawData[key] as? List<Any?>
    fun getDoubleList(key: String): List<Double> =
        (rawData[key] as? List<*>)?.filterIsInstance<Number>()?.map { it.toDouble() } ?: emptyList()
}