package org.triangle.launcher.anim

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import org.triangle.launcher.model.LauncherConfig

class MorphTransitionState {
    val springCX = VivoSpringODE(500f, 1.1f)
    val springCY = VivoSpringODE(500f, 1.1f)
    val springW = VivoSpringODE(500f, 1.1f)
    val springClipH = VivoSpringODE(700f, 1.1f)
    val springRadius = VivoSpringODE(950f, 1.1f)
    val springAppAlpha = VivoSpringODE(1000f, 0.6f)
    val springLauncher = VivoSpringODE(400f, 1.1f)
    val springBlur = VivoSpringODE(600f, 1.0f)

    var cxPos by mutableFloatStateOf(0f)
    var cxVel = 0f

    var cyPos by mutableFloatStateOf(0f)
    var cyVel = 0f

    var wPos by mutableFloatStateOf(0f)
    var wVel = 0f

    var clipHPos by mutableFloatStateOf(1.0f)
    var clipHVel = 0f

    var radiusPos by mutableFloatStateOf(19f)
    var radiusVel = 0f

    var appAlphaPos by mutableFloatStateOf(0f)
    var appAlphaVel = 0f

    var launcherScalePos by mutableFloatStateOf(1.0f)
    var launcherScaleVel = 0f

    var blurRadiusPos by mutableFloatStateOf(0f)
    var blurRadiusVel = 0f

    var targetCX: Float = 0f
    var targetCY: Float = 0f
    var targetW: Float = 0f
    var targetClipH: Float = 1.0f
    var targetRadius: Float = 0f
    var targetAppAlpha: Float = 1.0f
    var targetLauncherScale: Float = 1.0f
    var targetBlur: Float = 0f

    var isOpen: Boolean = false
    var isRunning: Boolean = false
    var anchorZ: Float = 0.5f
    var elapsedMs: Float = 0f
    var alphaDelayMs: Float = 0f // Без искусственной задержки: интерфейс оживает мгновенно вместе с полетом!

    var activeIconPkg: String? = null
    var startIconX: Float = 0f
    var startIconY: Float = 0f
    var startIconW: Float = 0f
    var startIconH: Float = 0f
    var startCornerRadius: Float = 19f

    fun configure(isOpen: Boolean) {
        this.isOpen = isOpen
        val k = if (isOpen) LauncherConfig.springStiffnessOpening else LauncherConfig.springStiffnessClosing
        val d = if (isOpen) LauncherConfig.springDampingOpening else LauncherConfig.springDampingClosing
        val lk = 400f
        val lD = 1.1f
        val hk = if (isOpen) 700f else 500f
        val rk = LauncherConfig.springRadiusStiffness
        val rD = 1.1f
        val ak = LauncherConfig.springAppAlphaStiffness
        val aD = 0.6f
        val bk = 600f
        val bD = 1.0f

        springCX.set(k, d)
        springCY.set(k, d)
        springW.set(k, d)
        springClipH.set(hk, d)
        springRadius.set(rk, rD)
        springAppAlpha.set(ak, aD)
        springLauncher.set(lk, lD)
        springBlur.set(bk, bD)
        this.alphaDelayMs = 0f
    }

    fun startOpen(
        pkg: String,
        iconX: Float,
        iconY: Float,
        iconW: Float,
        iconH: Float,
        iconCornerRadius: Float,
        screenW: Float,
        screenH: Float
    ) {
        this.activeIconPkg = pkg
        this.startIconX = iconX
        this.startIconY = iconY
        this.startIconW = iconW
        this.startIconH = iconH
        this.startCornerRadius = iconCornerRadius

        val centerX = iconX + iconW / 2f
        val centerY = iconY + iconH / 2f

        // Точный расчет anchorZ: при росте окна от иконки в полный экран
        // верхний край окна должен попадать ровно в 0 (верх экрана), а нижний - в screenH
        // top = cy - curH * (1 - anchorZ) => при полном экране (curH = screenH, cy = screenH/2):
        // top = screenH/2 - screenH * (1 - anchorZ) = 0 => 1 - anchorZ = 0.5 => anchorZ = 0.5!
        this.anchorZ = 0.5f

        configure(isOpen = true)

        cxPos = centerX; cxVel = 0f
        cyPos = centerY; cyVel = 0f
        wPos = iconW; wVel = 0f
        clipHPos = iconH / iconW; clipHVel = 0f
        radiusPos = iconCornerRadius; radiusVel = 0f
        appAlphaPos = 0f; appAlphaVel = 0f
        launcherScalePos = 1.0f; launcherScaleVel = 0f
        blurRadiusPos = 0f; blurRadiusVel = 0f

        targetCX = screenW / 2f
        targetCY = screenH / 2f
        targetW = screenW
        targetClipH = screenH / screenW
        targetRadius = 0f
        targetAppAlpha = 1.0f
        targetLauncherScale = 0.90f
        targetBlur = 24.0f

        elapsedMs = 0f
        isRunning = true
    }

    fun startDismiss(customVelX: Float = 0f, customVelY: Float = 0f) {
        configure(isOpen = false)

        val targetCenterX = startIconX + startIconW / 2f
        val targetCenterY = startIconY + startIconH / 2f

        targetCX = targetCenterX
        targetCY = targetCenterY
        targetW = startIconW
        targetClipH = startIconH / startIconW
        targetRadius = startCornerRadius
        targetAppAlpha = 0.0f
        targetLauncherScale = 1.0f
        targetBlur = 0.0f

        cxVel = customVelX
        cyVel = customVelY

        elapsedMs = 0f
        isRunning = true
    }

    fun updateTick(dt: Float, onSettled: () -> Unit) {
        elapsedMs += dt * 1000f

        val rCX = springCX.update(cxPos, targetCX, cxVel, dt)
        cxPos = rCX.pos; cxVel = rCX.vel

        val rCY = springCY.update(cyPos, targetCY, cyVel, dt)
        cyPos = rCY.pos; cyVel = rCY.vel

        val rW = springW.update(wPos, targetW, wVel, dt)
        wPos = rW.pos; wVel = rW.vel

        val rClipH = springClipH.update(clipHPos, targetClipH, clipHVel, dt)
        clipHPos = rClipH.pos; clipHVel = rClipH.vel

        val rRadius = springRadius.update(radiusPos, targetRadius, radiusVel, dt)
        radiusPos = rRadius.pos; radiusVel = rRadius.vel

        val rLauncher = springLauncher.update(launcherScalePos, targetLauncherScale, launcherScaleVel, dt)
        launcherScalePos = rLauncher.pos; launcherScaleVel = rLauncher.vel

        val rBlur = springBlur.update(blurRadiusPos, targetBlur, blurRadiusVel, dt)
        blurRadiusPos = rBlur.pos; blurRadiusVel = rBlur.vel

        val rAppAlpha = springAppAlpha.update(appAlphaPos, targetAppAlpha, appAlphaVel, dt)
        appAlphaPos = rAppAlpha.pos; appAlphaVel = rAppAlpha.vel

        val diffCX = Math.abs(cxPos - targetCX)
        val diffCY = Math.abs(cyPos - targetCY)
        val diffW = Math.abs(wPos - targetW)
        val velMagnitude = Math.abs(cxVel) + Math.abs(cyVel) + Math.abs(wVel)

        val isSettled = (diffCX < 1.0f && diffCY < 1.0f && diffW < 1.0f && velMagnitude < 30f)
        if (isSettled) {
            cxPos = targetCX
            cyPos = targetCY
            wPos = targetW
            clipHPos = targetClipH
            radiusPos = targetRadius
            isRunning = false
            onSettled()
        }
    }
}
