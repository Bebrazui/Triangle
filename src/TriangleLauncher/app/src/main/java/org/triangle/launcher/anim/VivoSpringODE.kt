package org.triangle.launcher.anim

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Аналитический решатель ОДУ (гармонического осциллятора) второго порядка 1:1 из OriginOS.
 * Декомпилировано из Lapptransition/utils/b.smali (VivoSpringAnimation).
 */
class VivoSpringODE(
    var stiffness: Float = 500f,
    var dampingRatio: Float = 1.1f
) {
    private var w0: Float = 0f
    private var gammaMinus: Float = 0f
    private var gammaPlus: Float = 0f
    private var wd: Float = 0f

    init {
        set(stiffness, dampingRatio)
    }

    fun set(stiffness: Float, dampingRatio: Float) {
        this.stiffness = stiffness
        this.dampingRatio = dampingRatio
        this.w0 = sqrt(stiffness)
        val zeta = dampingRatio
        if (zeta > 1.0f) {
            val s = sqrt(zeta * zeta - 1.0f)
            this.gammaMinus = -zeta * w0 + w0 * s
            this.gammaPlus = -zeta * w0 - w0 * s
        } else if (zeta < 1.0f) {
            this.wd = w0 * sqrt(1.0f - zeta * zeta)
        }
    }

    data class Result(val pos: Float, val vel: Float)

    fun update(current: Float, target: Float, velocity: Float, dt: Float): Result {
        val x0 = current - target
        val zeta = this.dampingRatio
        val w0 = this.w0

        val x: Float
        val v: Float

        if (zeta > 1.0f) {
            // Overdamped
            val c2 = (velocity - gammaMinus * x0) / (gammaPlus - gammaMinus)
            val c1 = x0 - c2
            val expM = exp(gammaMinus * dt)
            val expP = exp(gammaPlus * dt)
            x = c1 * expM + c2 * expP
            v = c1 * gammaMinus * expM + c2 * gammaPlus * expP
        } else if (zeta == 1.0f) {
            // Critically damped
            val c1 = x0
            val c2 = velocity + w0 * x0
            val e = exp(-w0 * dt)
            x = (c1 + c2 * dt) * e
            v = (c2 - w0 * (c1 + c2 * dt)) * e
        } else {
            // Underdamped
            val wd = this.wd
            val c1 = x0
            val c2 = (velocity + zeta * w0 * x0) / wd
            val e = exp(-zeta * w0 * dt)
            val cosVal = cos(wd * dt)
            val sinVal = sin(wd * dt)
            x = e * (c1 * cosVal + c2 * sinVal)
            v = -zeta * w0 * x + e * (-c1 * wd * sinVal + c2 * wd * cosVal)
        }

        return Result(target + x, v)
    }
}
