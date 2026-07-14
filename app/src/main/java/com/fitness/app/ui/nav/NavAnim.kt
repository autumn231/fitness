package com.fitness.app.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIntoContainer
import androidx.compose.animation.slideOutOfContainer

private const val DURATION = 320

/** 进入动画：从右侧滑入 + 淡入 + 轻微缩放 */
fun AnimatedContentTransitionScope<*>.slideInHorizontally() =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(DURATION)
    ) + fadeIn(tween(DURATION)) + scaleIn(tween(DURATION), initialScale = 0.96f)

/** 退出动画：向左滑出 + 淡出 + 轻微缩放 */
fun AnimatedContentTransitionScope<*>.slideOutHorizontally() =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(DURATION)
    ) + fadeOut(tween(DURATION)) + scaleOut(tween(DURATION), targetScale = 0.96f)

/** 返回进入动画：从左侧滑入 + 淡入 */
fun AnimatedContentTransitionScope<*>.popSlideInHorizontally() =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(DURATION)
    ) + fadeIn(tween(DURATION)) + scaleIn(tween(DURATION), initialScale = 0.96f)

/** 返回退出动画：向右滑出 + 淡出 */
fun AnimatedContentTransitionScope<*>.popSlideOutHorizontally() =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(DURATION)
    ) + fadeOut(tween(DURATION)) + scaleOut(tween(DURATION), targetScale = 0.96f)

/** Tab 间淡入淡出动画 */
fun tabFadeIn() = fadeIn(tween(220))
fun tabFadeOut() = fadeOut(tween(220))
