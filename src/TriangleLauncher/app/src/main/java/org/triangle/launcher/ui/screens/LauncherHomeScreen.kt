package org.triangle.launcher.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.triangle.launcher.anim.MorphTransitionState
import org.triangle.launcher.data.AppRepository
import org.triangle.launcher.model.AppModel
import org.triangle.launcher.model.LauncherConfig
import org.triangle.launcher.ui.icons.Phosphor
import org.triangle.launcher.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DesktopGridPosition(
    val packageName: String,
    val page: Int,
    val col: Int,
    val row: Int
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LauncherHomeScreen(
    appRepository: AppRepository,
    onOpenSettings: () -> Unit
) {
    val apps by appRepository.apps.collectAsState()

    var isDrawerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForDialog by remember { mutableStateOf<AppModel?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    val morphState = remember { MorphTransitionState() }
    var morphActiveApp by remember { mutableStateOf<AppModel?>(null) }
    var showMorphSurface by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var rootWidthPx by remember { mutableFloatStateOf(1080f) }
    var rootHeightPx by remember { mutableFloatStateOf(2400f) }

    val iconBoundsMap = remember { mutableMapOf<String, Triple<Offset, Size, Float>>() }

    val dockPackages = remember {
        setOf("com.android.dialer", "com.android.chrome", "com.android.messaging", "com.android.camera2")
    }
    val dockApps = remember(apps) {
        val found = apps.filter { it.packageName in dockPackages }
        if (found.isNotEmpty()) found else apps.take(4)
    }

    var desktopGrid by remember { mutableStateOf<List<DesktopGridPosition>>(emptyList()) }

    LaunchedEffect(apps) {
        if (apps.isNotEmpty() && desktopGrid.isEmpty()) {
            val nonDock = apps.filter { it !in dockApps }
            val positions = mutableListOf<DesktopGridPosition>()
            var p = 0
            var c = 0
            var r = 0
            nonDock.forEach { app ->
                positions.add(DesktopGridPosition(app.packageName, p, c, r))
                c++
                if (c >= 4) {
                    c = 0
                    r++
                    if (r >= 4) { // 4x4 на страницу
                        r = 0
                        p++
                    }
                }
            }
            desktopGrid = positions
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            showMorphSurface = false
            morphActiveApp = null
        }
    }

    LaunchedEffect(showMorphSurface) {
        if (!showMorphSurface) return@LaunchedEffect
        var lastTime = System.nanoTime()
        while (isActive && showMorphSurface) {
            awaitFrame()
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
            lastTime = now

            if (morphState.isRunning) {
                morphState.updateTick(dt) {
                    if (!morphState.isOpen) {
                        showMorphSurface = false
                        morphActiveApp = null
                    } else {
                        val target = morphActiveApp
                        showMorphSurface = false
                        morphActiveApp = null
                        target?.let { app ->
                            appRepository.launchApp(app)
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = isSettingsOpen || isDrawerOpen || showMorphSurface) {
        if (showMorphSurface) {
            morphState.startDismiss()
        } else if (isSettingsOpen) {
            isSettingsOpen = false
        } else if (isDrawerOpen) {
            isDrawerOpen = false
            searchQuery = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .onGloballyPositioned { rootCoords ->
                val size = rootCoords.size
                if (size.width > 0 && size.height > 0) {
                    rootWidthPx = size.width.toFloat()
                    rootHeightPx = size.height.toFloat()
                }
            }
    ) {
        val scrimAlpha = if (showMorphSurface) {
            val progress = ((morphState.wPos - morphState.startIconW) / (rootWidthPx - morphState.startIconW)).coerceIn(0f, 1f)
            progress * 0.35f
        } else 0f

        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
            )
        }

        val launcherScale = if (showMorphSurface) morphState.launcherScalePos else 1.0f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .graphicsLayer {
                    scaleX = launcherScale
                    scaleY = launcherScale
                }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val currentTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
            val currentDate = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()) }

            // Editorial Brutalist Big Typography Clock Widget
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentDate.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.82f)
                )
                Text(
                    text = currentTime,
                    fontSize = 94.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-5).sp,
                    color = Color.White,
                    lineHeight = 94.sp
                )
            }

            // ПЛАВНЫЙ РАБОЧИЙ СТОЛ СО СТРАНИЦАМИ (PAGER) И ПЕРЕМЕЩЕНИЕМ ИКОНОК
            val totalPages = remember(desktopGrid) {
                (desktopGrid.maxOfOrNull { it.page } ?: 0) + 1
            }
            val pagerState = rememberPagerState(pageCount = { totalPages.coerceAtLeast(1) })

            var draggingPkg by remember { mutableStateOf<String?>(null) }
            var dragRawOffset by remember { mutableStateOf(Offset.Zero) }
            val coroutineScope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = draggingPkg == null, // Отключаем свайп страниц во время перетаскивания иконки!
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { pageIndex ->
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val gridW = maxWidth
                        val gridH = maxHeight
                        val cellW = gridW / 4
                        val cellH = (gridH / 4).coerceAtMost(LauncherConfig.iconSlotHeight + 14.dp)
                        val cellWPx = with(density) { cellW.toPx() }
                        val cellHPx = with(density) { cellH.toPx() }

                        val pageItems = desktopGrid.filter { it.page == pageIndex }

                        pageItems.forEach { item ->
                            val app = apps.find { it.packageName == item.packageName }
                            if (app != null) {
                                val isBeingDragged = draggingPkg == item.packageName
                                val isHidden = showMorphSurface && morphState.activeIconPkg == app.packageName

                                val targetX = item.col * cellWPx
                                val targetY = item.row * cellHPx

                                val animX = remember { Animatable(targetX) }
                                val animY = remember { Animatable(targetY) }

                                LaunchedEffect(item.col, item.row, isBeingDragged) {
                                    if (!isBeingDragged) {
                                        launch {
                                            animX.animateTo(
                                                targetX,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                        launch {
                                            animY.animateTo(
                                                targetY,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                }

                                val dragScale by animateFloatAsState(
                                    targetValue = if (isBeingDragged) 1.18f else 1.0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "dragScale"
                                )
                                val dragShadowElevation by animateFloatAsState(
                                    targetValue = if (isBeingDragged) 16f else 0f,
                                    animationSpec = tween(150),
                                    label = "dragShadow"
                                )

                                Box(
                                    modifier = Modifier
                                        .offset {
                                            if (isBeingDragged) {
                                                IntOffset(
                                                    (targetX + dragRawOffset.x).roundToInt(),
                                                    (targetY + dragRawOffset.y).roundToInt()
                                                )
                                            } else {
                                                IntOffset(
                                                    animX.value.roundToInt(),
                                                    animY.value.roundToInt()
                                                )
                                            }
                                        }
                                        .size(cellW, cellH)
                                        .zIndex(if (isBeingDragged) 200f else 1f)
                                        .graphicsLayer {
                                            scaleX = dragScale
                                            scaleY = dragScale
                                            shadowElevation = dragShadowElevation
                                        }
                                        .pointerInput(item.packageName) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingPkg = item.packageName
                                                    dragRawOffset = Offset.Zero
                                                },
                                                onDragEnd = {
                                                    val finalX = targetX + dragRawOffset.x
                                                    val finalY = targetY + dragRawOffset.y
                                                    val finalCol = (finalX / cellWPx).roundToInt().coerceIn(0, 3)
                                                    val finalRow = (finalY / cellHPx).roundToInt().coerceIn(0, 3)

                                                    draggingPkg = null
                                                    dragRawOffset = Offset.Zero

                                                    val targetOccupant = desktopGrid.find {
                                                        it.packageName != item.packageName && it.page == pageIndex && it.col == finalCol && it.row == finalRow
                                                    }

                                                    desktopGrid = desktopGrid.map {
                                                        when {
                                                            it.packageName == item.packageName -> it.copy(col = finalCol, row = finalRow)
                                                            it.packageName == targetOccupant?.packageName -> it.copy(col = item.col, row = item.row)
                                                            else -> it
                                                        }
                                                    }
                                                },
                                                onDragCancel = {
                                                    draggingPkg = null
                                                    dragRawOffset = Offset.Zero
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragRawOffset += dragAmount
                                                }
                                            )
                                        }
                                ) {
                                    PureAppIconItem(
                                        app = app,
                                        showLabel = true,
                                        iconSize = LauncherConfig.desktopIconSize,
                                        isMorphActive = isHidden,
                                        onPositioned = { pos, size, radiusPx ->
                                            if (!isBeingDragged) {
                                                iconBoundsMap[app.packageName] = Triple(pos, size, radiusPx)
                                            }
                                        },
                                        onClick = {
                                            if (draggingPkg == null) {
                                                val bounds = iconBoundsMap[app.packageName]
                                                if (bounds != null) {
                                                    morphActiveApp = app
                                                    showMorphSurface = true
                                                    morphState.startOpen(
                                                        pkg = app.packageName,
                                                        iconX = bounds.first.x,
                                                        iconY = bounds.first.y,
                                                        iconW = bounds.second.width,
                                                        iconH = bounds.second.height,
                                                        iconCornerRadius = bounds.third,
                                                        screenW = rootWidthPx,
                                                        screenH = rootHeightPx
                                                    )
                                                } else {
                                                    appRepository.launchApp(app)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            // Обработано в detectDragGesturesAfterLongPress
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Индикатор страниц (Page Indicator Dots)
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(totalPages) { idx ->
                            val isCurrent = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) Color.White else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }
                }
            }

            // Elegant Floating Search Capsule & Settings Access
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { isDrawerOpen = true },
                    color = Color(0xFF14171F).copy(alpha = 0.75f),
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Phosphor.MagnifyingGlass,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Поиск",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { isSettingsOpen = true },
                    shape = CircleShape,
                    color = Color(0xFF14171F).copy(alpha = 0.75f),
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Phosphor.GearSix,
                            contentDescription = "Launcher Settings",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dock: чистые оригинальные иконки без обрезания обводки и со 100% центровкой
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockApps.forEach { app ->
                    val isHidden = showMorphSurface && morphState.activeIconPkg == app.packageName
                    Box(
                        modifier = Modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        PureAppIconItem(
                            app = app,
                            showLabel = false,
                            iconSize = LauncherConfig.dockIconSize,
                            isMorphActive = isHidden,
                            onPositioned = { pos, size, radiusPx ->
                                iconBoundsMap[app.packageName] = Triple(pos, size, radiusPx)
                            },
                            onClick = {
                                val bounds = iconBoundsMap[app.packageName]
                                if (bounds != null) {
                                    morphActiveApp = app
                                    showMorphSurface = true
                                    morphState.startOpen(
                                        pkg = app.packageName,
                                        iconX = bounds.first.x,
                                        iconY = bounds.first.y,
                                        iconW = bounds.second.width,
                                        iconH = bounds.second.height,
                                        iconCornerRadius = bounds.third,
                                        screenW = rootWidthPx,
                                        screenH = rootHeightPx
                                    )
                                } else {
                                    appRepository.launchApp(app)
                                }
                            },
                            onLongClick = {
                                selectedAppForDialog = app
                            }
                        )
                    }
                }
            }
        }

        // --- THE MORPHING SURFACE (OriginOS RectFSpringAnim 1:1) ---
        if (showMorphSurface && morphActiveApp != null) {
            val app = morphActiveApp!!
            val curW = morphState.wPos.coerceAtLeast(1f)
            val curH = curW * morphState.clipHPos.coerceAtLeast(0.1f)
            val left = morphState.cxPos - curW / 2f
            val top = morphState.cyPos - curH / 2f

            val radiusDp = with(density) { morphState.radiusPos.coerceAtLeast(0f).toDp() }
            val widthDp = with(density) { curW.toDp() }
            val heightDp = with(density) { curH.toDp() }
            val leftDp = with(density) { left.toDp() }
            val topDp = with(density) { top.toDp() }

            val progress = ((curW - morphState.startIconW) / (rootWidthPx - morphState.startIconW)).coerceIn(0f, 1f)
            val surfaceBgAlpha = (progress / 0.35f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .offset(leftDp, topDp)
                    .size(widthDp, heightDp)
                    .clip(RoundedCornerShape(radiusDp))
                    .background(Color(0xFF11141C).copy(alpha = surfaceBgAlpha))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                morphState.startDismiss()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                morphState.cxVel += dragAmount.x * 40f
                                morphState.cyVel += dragAmount.y * 40f
                                morphState.cxPos += dragAmount.x
                                morphState.cyPos += dragAmount.y
                            }
                        )
                    }
            ) {
                // 1. Полноэкранный реальный макет интерфейса
                val appAlpha = morphState.appAlphaPos.coerceIn(0f, 1f)
                val appSlidePx = (1.0f - progress) * 16f

                if (appAlpha > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = appAlpha
                                translationY = appSlidePx
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                IconButton(
                                    onClick = { morphState.startDismiss() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Phosphor.ArrowLeft,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .background(Color(0xFF1B202D), RoundedCornerShape(22.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Running ${app.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(vertical = 12.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .width(80.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .clickable { morphState.startDismiss() }
                            )
                        }
                    }
                }

                // 2. Иконка приложения (плавное затухание/появление в морфе)
                val glyphAlpha = (1.0f - ((progress - 0.05f) / 0.30f)).coerceIn(0f, 1f)
                val glyphScale = 1.0f + progress * 0.25f

                if (glyphAlpha > 0.005f && app.icon != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = glyphAlpha
                                scaleX = glyphScale
                                scaleY = glyphScale
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = app.icon.asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier.size(with(density) { morphState.startIconW.toDp() }),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // --- Compact Bottom Search & App Sheet ---
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable {
                        isDrawerOpen = false
                        searchQuery = ""
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .clickable(enabled = false) { /* prevent click-through */ },
                    color = Color(0xFF14171F).copy(alpha = 0.98f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    AppDrawerScreen(
                        apps = apps,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onClose = {
                            isDrawerOpen = false
                            searchQuery = ""
                        },
                        onAppClick = { app ->
                            isDrawerOpen = false
                            searchQuery = ""
                            appRepository.launchApp(app)
                        },
                        onAppLongClick = { app ->
                            selectedAppForDialog = app
                        }
                    )
                }
            }
        }

        selectedAppForDialog?.let { targetApp ->
            AppContextDialog(
                app = targetApp,
                onDismiss = { selectedAppForDialog = null },
                onOpenAppInfo = {
                    appRepository.openAppInfo(targetApp)
                    selectedAppForDialog = null
                }
            )
        }

        // --- Bottom Sheet Настроек Лаунчера ---
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isSettingsOpen = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                LauncherSettingsDialog(
                    onDismiss = { isSettingsOpen = false }
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PureAppIconItem(
    app: AppModel,
    showLabel: Boolean = true,
    iconSize: androidx.compose.ui.unit.Dp = LauncherConfig.desktopIconSize,
    isMorphActive: Boolean = false,
    onPositioned: (Offset, Size, Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val density = LocalDensity.current
    val cornerRadiusDp = LauncherConfig.iconCornerRadiusDp
    val cornerRadiusPx = with(density) { cornerRadiusDp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(LauncherConfig.iconSlotHeight)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(cornerRadiusDp))
                .onGloballyPositioned { coords ->
                    val pos = coords.boundsInRoot().topLeft
                    val size = coords.boundsInRoot().size
                    onPositioned(pos, size, cornerRadiusPx)
                },
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon.asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (isMorphActive) 0f else 1f
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AppDrawerScreen(
    apps: List<AppModel>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit
) {
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Drag handle at top of sheet
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Phosphor.MagnifyingGlass,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                            color = Color.White
                        ),
                        cursorBrush = SolidColor(AccentColor),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search apps...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.45f)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Phosphor.X,
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onAppClick(app) },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon.asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(44.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.4f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppContextDialog(
    app: AppModel,
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                text = app.label,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
        },
        text = {
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenAppInfo) {
                Text(text = "App Info", color = AccentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = Color.Gray)
            }
        }
    )
}
