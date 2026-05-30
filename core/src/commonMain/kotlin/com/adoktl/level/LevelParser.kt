package com.adoktl.level

import com.adoktl.math.Vector2
import com.adoktl.core.json.StringParser

object LevelParser {

    private val parser = StringParser()

    fun parse(jsonString: String): LevelData {
        val root = parser.parse(jsonString) as? Map<String, Any?>
            ?: throw IllegalArgumentException("Invalid ADOFAI level JSON")

        val angleData = parseAngleData(root["angleData"])
        val settings = parseSettings(root["settings"] as? Map<String, Any?> ?: emptyMap())
        val actions = parseActions(root["actions"] as? List<*>)
        val decorations = parseDecorations(root["decorations"] as? List<*>)

        return LevelData(
            angleData = angleData,
            settings = settings,
            actions = actions,
            decorations = decorations
        )
    }

    private fun parseAngleData(data: Any?): List<Double> {
        return when (data) {
            is List<*> -> data.filterIsInstance<Number>().map { it.toDouble() }
            else -> emptyList()
        }
    }

    private fun parseVector2(data: Any?): Vector2 {
        val list = data as? List<*> ?: return Vector2.ZERO
        val x = (list.getOrNull(0) as? Number)?.toDouble() ?: 0.0
        val y = (list.getOrNull(1) as? Number)?.toDouble() ?: 0.0
        return Vector2(x, y)
    }

    private fun parseSettings(map: Map<String, Any?>): LevelSettings {
        return LevelSettings(
            version = (map["version"] as? Number)?.toInt() ?: 18,
            artist = (map["artist"] as? String) ?: "",
            specialArtistType = (map["specialArtistType"] as? String) ?: "None",
            artistPermission = (map["artistPermission"] as? String) ?: "",
            song = (map["song"] as? String) ?: "",
            author = (map["author"] as? String) ?: "",
            separateCountdownTime = (map["separateCountdownTime"] as? Boolean) ?: true,
            previewImage = (map["previewImage"] as? String) ?: "",
            previewIcon = (map["previewIcon"] as? String) ?: "",
            previewIconColor = (map["previewIconColor"] as? String) ?: "003f52",
            previewSongStart = (map["previewSongStart"] as? Number)?.toDouble() ?: 0.0,
            previewSongDuration = (map["previewSongDuration"] as? Number)?.toDouble() ?: 10.0,
            seizureWarning = (map["seizureWarning"] as? Boolean) ?: false,
            levelDesc = (map["levelDesc"] as? String) ?: "",
            levelTags = (map["levelTags"] as? String) ?: "",
            artistLinks = (map["artistLinks"] as? String) ?: "",
            speedTrialAim = (map["speedTrialAim"] as? Number)?.toInt() ?: 0,
            difficulty = (map["difficulty"] as? Number)?.toInt() ?: 1,
            requiredMods = (map["requiredMods"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            songFilename = (map["songFilename"] as? String) ?: "",
            bpm = (map["bpm"] as? Number)?.toDouble() ?: 100.0,
            volume = (map["volume"] as? Number)?.toInt() ?: 100,
            offset = (map["offset"] as? Number)?.toDouble() ?: 0.0,
            pitch = (map["pitch"] as? Number)?.toInt() ?: 100,
            hitsound = (map["hitsound"] as? String) ?: "Kick",
            hitsoundVolume = (map["hitsoundVolume"] as? Number)?.toInt() ?: 100,
            countdownTicks = (map["countdownTicks"] as? Number)?.toInt() ?: 4,
            tileShape = (map["tileShape"] as? String) ?: "Long",
            trackColorType = (map["trackColorType"] as? String) ?: "Single",
            trackColor = (map["trackColor"] as? String) ?: "debb7b",
            secondaryTrackColor = (map["secondaryTrackColor"] as? String) ?: "ffffff",
            trackColorAnimDuration = (map["trackColorAnimDuration"] as? Number)?.toDouble() ?: 2.0,
            trackColorPulse = (map["trackColorPulse"] as? String) ?: "None",
            trackPulseLength = (map["trackPulseLength"] as? Number)?.toInt() ?: 10,
            trackStyle = (map["trackStyle"] as? String) ?: "Standard",
            trackTexture = (map["trackTexture"] as? String) ?: "",
            trackTextureScale = (map["trackTextureScale"] as? Number)?.toDouble() ?: 1.0,
            trackGlowIntensity = (map["trackGlowIntensity"] as? Number)?.toInt() ?: 100,
            trackAnimation = (map["trackAnimation"] as? String) ?: "None",
            beatsAhead = (map["beatsAhead"] as? Number)?.toInt() ?: 3,
            trackDisappearAnimation = (map["trackDisappearAnimation"] as? String) ?: "None",
            beatsBehind = (map["beatsBehind"] as? Number)?.toInt() ?: 4,
            backgroundColor = (map["backgroundColor"] as? String) ?: "000000",
            showDefaultBGIfNoImage = (map["showDefaultBGIfNoImage"] as? Boolean) ?: true,
            showDefaultBGTile = (map["showDefaultBGTile"] as? Boolean) ?: true,
            defaultBGTileColor = (map["defaultBGTileColor"] as? String) ?: "101121",
            defaultBGShapeType = (map["defaultBGShapeType"] as? String) ?: "Default",
            defaultBGShapeColor = (map["defaultBGShapeColor"] as? String) ?: "ffffff",
            bgImage = (map["bgImage"] as? String) ?: "",
            bgImageColor = (map["bgImageColor"] as? String) ?: "ffffff",
            parallax = parseVector2(map["parallax"]),
            bgDisplayMode = (map["bgDisplayMode"] as? String) ?: "FitToScreen",
            imageSmoothing = (map["imageSmoothing"] as? Boolean) ?: true,
            lockRot = (map["lockRot"] as? Boolean) ?: false,
            loopBG = (map["loopBG"] as? Boolean) ?: false,
            scalingRatio = (map["scalingRatio"] as? Number)?.toInt() ?: 100,
            relativeTo = (map["relativeTo"] as? String) ?: "Player",
            position = parseVector2(map["position"]),
            rotation = (map["rotation"] as? Number)?.toDouble() ?: 0.0,
            zoom = (map["zoom"] as? Number)?.toInt() ?: 100,
            pulseOnFloor = (map["pulseOnFloor"] as? Boolean) ?: true,
            bgVideo = (map["bgVideo"] as? String) ?: "",
            loopVideo = (map["loopVideo"] as? Boolean) ?: false,
            vidOffset = (map["vidOffset"] as? Number)?.toInt() ?: 0,
            floorIconOutlines = (map["floorIconOutlines"] as? Boolean) ?: false,
            stickToFloors = (map["stickToFloors"] as? Boolean) ?: true,
            planetEase = (map["planetEase"] as? String) ?: "Linear",
            planetEaseParts = (map["planetEaseParts"] as? Number)?.toInt() ?: 1,
            planetEasePartBehavior = (map["planetEasePartBehavior"] as? String) ?: "Mirror",
            defaultTextColor = (map["defaultTextColor"] as? String) ?: "ffffff",
            defaultTextShadowColor = (map["defaultTextShadowColor"] as? String) ?: "00000050",
            congratsText = (map["congratsText"] as? String) ?: "",
            perfectText = (map["perfectText"] as? String) ?: "",
            legacyFlash = (map["legacyFlash"] as? Boolean) ?: false,
            legacyCamRelativeTo = (map["legacyCamRelativeTo"] as? Boolean) ?: false,
            legacySpriteTiles = (map["legacySpriteTiles"] as? Boolean) ?: false,
            legacyTween = (map["legacyTween"] as? Boolean) ?: false,
            disableV15Features = (map["disableV15Features"] as? Boolean) ?: false,
            legacyPause = (map["legacyPause"] as? Boolean) ?: false
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseActions(actions: List<*>?): List<LevelAction> {
        return actions?.filterIsInstance<Map<String, Any?>>()
            ?.map { LevelAction.fromMap(it) }
            ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseDecorations(decorations: List<*>?): List<DecorationAction> {
        return decorations?.filterIsInstance<Map<String, Any?>>()
            ?.map { DecorationAction.fromMap(it) }
            ?: emptyList()
    }
}