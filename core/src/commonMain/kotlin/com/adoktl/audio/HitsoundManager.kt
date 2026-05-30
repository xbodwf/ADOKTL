package com.adoktl.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class HitsoundManager(
    private val defaultType: HitsoundType = HitsoundType.Kick,
    private val defaultVolume: Int = 100,
    private val audioConfig: AudioConfig = AudioConfig()
) {
    private var _enabled: Boolean = true
    private var _synthesizedData: MixBuffer? = null

    val isEnabled: Boolean get() = _enabled
    val isSynthesized: Boolean get() = _synthesizedData != null

    fun setEnabled(enabled: Boolean) {
        _enabled = enabled
    }

    fun getDefaultType(): HitsoundType = defaultType
    fun getDefaultVolume(): Int = defaultVolume

    suspend fun buildHitsoundGroups(
        tileHitsoundEvents: Map<Int, List<Map<String, Any?>>>,
        tileStartTimes: DoubleArray,
        totalDuration: Double
    ): List<TimestampGroup> {
        if (!_enabled || tileHitsoundEvents.isEmpty()) return emptyList()

        val groupMap = mutableMapOf<String, TimestampGroup>()

        for ((floor, events) in tileHitsoundEvents) {
            val tileStart = tileStartTimes.getOrElse(floor) { 0.0 }

            for (event in events) {
                val eventType = (event["eventType"] as? String) ?: continue
                val hitsoundType = HitsoundType.fromString(eventType)

                val hitsound = event["hitsound"] as? String
                val hitsoundVolume = (event["hitsoundVolume"] as? Number)?.toInt() ?: defaultVolume

                val type = if (hitsound != null) HitsoundType.fromString(hitsound) else hitsoundType
                val volume = hitsoundVolume
                val timeOffset = (event["angleOffset"] as? Number)?.toDouble() ?: 0.0
                val time = tileStart + (timeOffset / 180.0)

                val key = "${type.name}_$volume"
                val existing = groupMap[key]
                if (existing != null) {
                    groupMap[key] = existing.copy(
                        timestamps = existing.timestamps + time
                    )
                } else {
                    groupMap[key] = TimestampGroup(
                        type = type,
                        volume = volume,
                        timestamps = listOf(time)
                    )
                }
            }
        }

        return groupMap.values.toList()
    }

    suspend fun preSynthesize(
        groups: List<TimestampGroup>,
        totalDuration: Double,
        onProgress: ((Double) -> Unit)? = null
    ) = withContext(Dispatchers.Default) {
        if (!_enabled || groups.isEmpty()) {
            _synthesizedData = null
            onProgress?.invoke(1.0)
            return@withContext
        }

        val activeGroups = groups.filter { it.type != HitsoundType.None && it.timestamps.isNotEmpty() }
        if (activeGroups.isEmpty()) {
            _synthesizedData = null
            onProgress?.invoke(1.0)
            return@withContext
        }

        val sampleRate = audioConfig.sampleRate
        val numChannels = 2

        var maxHitDuration = 0.0
        val hitDurations = mutableMapOf<HitsoundType, Int>()

        for (group in activeGroups) {
            val hitLength = getHitSampleLength(group.type, sampleRate)
            hitDurations[group.type] = hitLength
            val dur = hitLength.toDouble() / sampleRate
            if (dur > maxHitDuration) maxHitDuration = dur
        }

        val bufferLength = ((totalDuration + maxHitDuration + 1.0) * sampleRate).toInt()
            .coerceAtMost(MAX_BUFFER_SIZE)

        if (bufferLength <= 0) {
            _synthesizedData = null
            onProgress?.invoke(1.0)
            return@withContext
        }

        onProgress?.invoke(0.05)

        val outputData = Array(numChannels) { FloatArray(bufferLength) }

        var totalHits = activeGroups.sumOf { it.timestamps.size }
        var processedHits = 0
        var peakAmplitude = 0f

        for (group in activeGroups) {
            val hitLen = hitDurations[group.type] ?: 0
            val volScale = group.volume / 100f
            val hitSamples = generateHitSamples(group.type, hitLen, sampleRate)

            for (timestamp in group.timestamps) {
                if (timestamp < 0) continue
                val startSample = (timestamp * sampleRate).toInt()
                val len = minOf(hitLen, bufferLength - startSample)

                for (ch in 0 until numChannels) {
                    val dst = outputData[ch]
                    val src = if (ch < hitSamples.size) hitSamples[ch] else hitSamples.last()
                    for (i in 0 until len) {
                        val sample = dst[startSample + i] + src[i] * volScale
                        dst[startSample + i] = sample
                        val absVal = if (sample < 0) -sample else sample
                        if (absVal > peakAmplitude) peakAmplitude = absVal
                    }
                }

                processedHits++
                if (totalHits > 0) {
                    onProgress?.invoke(0.05 + (processedHits.toDouble() / totalHits) * 0.85)
                }
            }
        }

        onProgress?.invoke(0.95)

        val TARGET_HEADROOM = 0.9f
        val gain = if (peakAmplitude > TARGET_HEADROOM) TARGET_HEADROOM / peakAmplitude else 1.0f

        for (ch in 0 until numChannels) {
            val d = outputData[ch]
            if (gain < 1.0f) {
                for (i in d.indices) {
                    d[i] = softClip((d[i] * gain).toDouble()).toFloat()
                }
            } else {
                for (i in d.indices) {
                    val absVal = if (d[i] < 0) -d[i] else d[i]
                    if (absVal > 0.5f) {
                        d[i] = softClip(d[i].toDouble()).toFloat()
                    }
                }
            }
        }

        _synthesizedData = MixBuffer(
            channels = numChannels,
            sampleRate = sampleRate,
            length = bufferLength,
            data = outputData
        )

        onProgress?.invoke(1.0)
    }

    fun getSynthesizedData(): MixBuffer? = _synthesizedData

    fun clear() {
        _synthesizedData = null
    }

    companion object {
        private const val MAX_BUFFER_SIZE = 2147483647 / 4

        private val hitSampleCache = mutableMapOf<HitsoundType, Array<FloatArray>>()

        fun getHitSampleLength(type: HitsoundType, sampleRate: Int): Int {
            return when (type) {
                HitsoundType.Kick, HitsoundType.KickHouse,
                HitsoundType.KickChroma, HitsoundType.KickRupture -> (0.15 * sampleRate).toInt()
                HitsoundType.Snare, HitsoundType.SnareHouse,
                HitsoundType.SnareVapor -> (0.12 * sampleRate).toInt()
                HitsoundType.Clap, HitsoundType.ClapHit,
                HitsoundType.ClapHitEcho -> (0.1 * sampleRate).toInt()
                HitsoundType.Hat, HitsoundType.HatHouse -> (0.08 * sampleRate).toInt()
                HitsoundType.FireTile, HitsoundType.IceTile -> (0.2 * sampleRate).toInt()
                else -> (0.1 * sampleRate).toInt()
            }
        }

        fun generateHitSamples(type: HitsoundType, length: Int, sampleRate: Int): Array<FloatArray> {
            val cached = hitSampleCache[type]
            if (cached != null && cached[0].size >= length) return cached

            val numChannels = 2
            val data = Array(numChannels) { FloatArray(length) }

            val freq: Double
            val decay: Double
            val noiseAmount: Double

            when (type) {
                HitsoundType.Kick -> {
                    freq = 80.0; decay = 8.0; noiseAmount = 0.1
                }
                HitsoundType.KickHouse -> {
                    freq = 100.0; decay = 6.0; noiseAmount = 0.15
                }
                HitsoundType.KickChroma -> {
                    freq = 120.0; decay = 7.0; noiseAmount = 0.1
                }
                HitsoundType.KickRupture -> {
                    freq = 90.0; decay = 9.0; noiseAmount = 0.2
                }
                HitsoundType.Snare -> {
                    freq = 200.0; decay = 5.0; noiseAmount = 0.6
                }
                HitsoundType.SnareHouse -> {
                    freq = 180.0; decay = 4.0; noiseAmount = 0.5
                }
                HitsoundType.SnareVapor -> {
                    freq = 150.0; decay = 6.0; noiseAmount = 0.4
                }
                HitsoundType.Clap, HitsoundType.ClapHit -> {
                    freq = 300.0; decay = 3.0; noiseAmount = 0.7
                }
                HitsoundType.ClapHitEcho -> {
                    freq = 300.0; decay = 5.0; noiseAmount = 0.7
                }
                HitsoundType.Hat -> {
                    freq = 800.0; decay = 2.0; noiseAmount = 0.5
                }
                HitsoundType.HatHouse -> {
                    freq = 600.0; decay = 2.5; noiseAmount = 0.4
                }
                HitsoundType.FireTile -> {
                    freq = 400.0; decay = 4.0; noiseAmount = 0.8
                }
                HitsoundType.IceTile -> {
                    freq = 1000.0; decay = 3.0; noiseAmount = 0.3
                }
                else -> {
                    freq = 200.0; decay = 4.0; noiseAmount = 0.3
                }
            }

            for (i in 0 until length) {
                val t = i.toDouble() / sampleRate
                val env = kotlin.math.exp(-decay * t)
                val tone = kotlin.math.sin(2.0 * kotlin.math.PI * freq * t) * (1.0 - noiseAmount)
                val noise = (kotlin.random.Random.nextDouble() * 2.0 - 1.0) * noiseAmount
                val sample = (tone + noise) * env

                for (ch in 0 until numChannels) {
                    data[ch][i] = sample.toFloat()
                }
            }

            hitSampleCache[type] = data
            return data
        }
    }
}