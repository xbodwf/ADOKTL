package com.adoktl.math

import kotlin.math.pow

object EasingFunctions {

    private fun pow(x: Double, y: Double): Double = x.pow(y)

    private val funcs: Map<String, (Double) -> Double> = mapOf(
        "Linear" to { t -> t },
        "InSine" to { t -> 1.0 - kotlin.math.cos((t * kotlin.math.PI) / 2.0) },
        "OutSine" to { t -> kotlin.math.sin((t * kotlin.math.PI) / 2.0) },
        "InOutSine" to { t -> -(kotlin.math.cos(kotlin.math.PI * t) - 1.0) / 2.0 },
        "InQuad" to { t -> t * t },
        "OutQuad" to { t -> 1.0 - (1.0 - t) * (1.0 - t) },
        "InOutQuad" to { t -> if (t < 0.5) 2.0 * t * t else 1.0 - pow(-2.0 * t + 2.0, 2.0) / 2.0 },
        "InCubic" to { t -> t * t * t },
        "OutCubic" to { t -> 1.0 - pow(1.0 - t, 3.0) },
        "InOutCubic" to { t -> if (t < 0.5) 4.0 * t * t * t else 1.0 - pow(-2.0 * t + 2.0, 3.0) / 2.0 },
        "InQuart" to { t -> t * t * t * t },
        "OutQuart" to { t -> 1.0 - pow(1.0 - t, 4.0) },
        "InOutQuart" to { t -> if (t < 0.5) 8.0 * t * t * t * t else 1.0 - pow(-2.0 * t + 2.0, 4.0) / 2.0 },
        "InQuint" to { t -> t * t * t * t * t },
        "OutQuint" to { t -> 1.0 - pow(1.0 - t, 5.0) },
        "InOutQuint" to { t -> if (t < 0.5) 16.0 * t * t * t * t * t else 1.0 - pow(-2.0 * t + 2.0, 5.0) / 2.0 },
        "InExpo" to { t -> if (t == 0.0) 0.0 else pow(2.0, 10.0 * t - 10.0) },
        "OutExpo" to { t -> if (t == 1.0) 1.0 else 1.0 - pow(2.0, -10.0 * t) },
        "InOutExpo" to { t ->
            when {
                t == 0.0 -> 0.0
                t == 1.0 -> 1.0
                t < 0.5 -> pow(2.0, 20.0 * t - 10.0) / 2.0
                else -> (2.0 - pow(2.0, -20.0 * t + 10.0)) / 2.0
            }
        },
        "InCirc" to { t -> 1.0 - kotlin.math.sqrt(1.0 - t * t) },
        "OutCirc" to { t -> kotlin.math.sqrt(1.0 - pow(t - 1.0, 2.0)) },
        "InOutCirc" to { t ->
            if (t < 0.5) (1.0 - kotlin.math.sqrt(1.0 - pow(2.0 * t, 2.0))) / 2.0
            else (kotlin.math.sqrt(1.0 - pow(-2.0 * t + 2.0, 2.0)) + 1.0) / 2.0
        },
        "InBack" to { t ->
            val c1 = 1.70158; val c3 = c1 + 1.0
            c3 * t * t * t - c1 * t * t
        },
        "OutBack" to { t ->
            val c1 = 1.70158; val c3 = c1 + 1.0
            1.0 + c3 * pow(t - 1.0, 3.0) + c1 * pow(t - 1.0, 2.0)
        },
        "InOutBack" to { t ->
            val c1 = 1.70158; val c2 = c1 * 1.525
            if (t < 0.5) (pow(2.0 * t, 2.0) * ((c2 + 1.0) * 2.0 * t - c2)) / 2.0
            else (pow(2.0 * t - 2.0, 2.0) * ((c2 + 1.0) * (t * 2.0 - 2.0) + c2) + 2.0) / 2.0
        },
        "InElastic" to { t ->
            val c4 = (2.0 * kotlin.math.PI) / 3.0
            when {
                t == 0.0 -> 0.0
                t == 1.0 -> 1.0
                else -> -pow(2.0, 10.0 * t - 10.0) * kotlin.math.sin((t * 10.0 - 10.75) * c4)
            }
        },
        "OutElastic" to { t ->
            val c4 = (2.0 * kotlin.math.PI) / 3.0
            when {
                t == 0.0 -> 0.0
                t == 1.0 -> 1.0
                else -> pow(2.0, -10.0 * t) * kotlin.math.sin((t * 10.0 - 0.75) * c4) + 1.0
            }
        },
        "InOutElastic" to { t ->
            val c5 = (2.0 * kotlin.math.PI) / 4.5
            when {
                t == 0.0 -> 0.0
                t == 1.0 -> 1.0
                t < 0.5 -> -(pow(2.0, 20.0 * t - 10.0) * kotlin.math.sin((20.0 * t - 11.125) * c5)) / 2.0
                else -> (pow(2.0, -20.0 * t + 10.0) * kotlin.math.sin((20.0 * t - 11.125) * c5)) / 2.0 + 1.0
            }
        },
        "InBounce" to { t -> 1.0 - bounceOut(1.0 - t) },
        "OutBounce" to { t -> bounceOut(t) },
        "InOutBounce" to { t ->
            if (t < 0.5) (1.0 - bounceOut(1.0 - 2.0 * t)) / 2.0
            else (1.0 + bounceOut(2.0 * t - 1.0)) / 2.0
        },
        "Unset" to { t -> t }
    )

    private fun bounceOut(t: Double): Double {
        val n1 = 7.5625
        val d1 = 2.75
        return when {
            t < 1.0 / d1 -> n1 * t * t
            t < 2.0 / d1 -> n1 * (t - 1.5 / d1) * t + 0.75
            t < 2.5 / d1 -> n1 * (t - 2.25 / d1) * t + 0.9375
            else -> n1 * (t - 2.625 / d1) * t + 0.984375
        }
    }

    fun get(name: String): (Double) -> Double = funcs[name] ?: funcs["Linear"]!!
    fun has(name: String): Boolean = funcs.containsKey(name)
}