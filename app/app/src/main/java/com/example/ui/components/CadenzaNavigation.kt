package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.theme.*

@Composable
fun CadenzaNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on auth, splash, onboarding, or singing session screens
    if (currentRoute == "splash" || currentRoute == "login" || currentRoute == "onboarding" || currentRoute == "singing_session") {
        return
    }

    val items = listOf(
        NavItem("dashboard", Icons.Default.Home, "Home"),
        NavItem("learn", Icons.Default.MenuBook, "Learn"),
        NavItem("community", Icons.Default.People, "Community"),
        NavItem("progress_tracking", Icons.Default.BarChart, "Progress"),
        NavItem("ai_coach", Icons.Default.Face, "Coach"),
        NavItem("profile", Icons.Default.Person, "Profile")
    )

    // A physical control strip look
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = CadenzaShadowDark, spotColor = CadenzaShadowDark)
            .clip(RoundedCornerShape(24.dp))
            .background(CadenzaPanelLight)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val interactionSource = remember { MutableInteractionSource() }

                val animatedColor by animateColorAsState(
                    if (selected) CadenzaHyperMagenta else CadenzaTextSecondary.copy(alpha = 0.5f),
                    label = "color"
                )
                val animatedScale by animateFloatAsState(
                    if (selected) 1.2f else 1f,
                    animationSpec = tween(200),
                    label = "scale"
                )
                val animatedElevation by animateDpAsState(
                    if (selected) 4.dp else 0.dp,
                    label = "elevation"
                )
                val bgColor by animateColorAsState(
                    if (selected) CadenzaOffWhite else Color.Transparent,
                    label = "bgcolor"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(animatedElevation, CircleShape, spotColor = CadenzaShadowDark)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo("dashboard") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // We only use the scale on the icon, not the box
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = animatedColor,
                        modifier = Modifier.size(24.dp * animatedScale)
                    )
                }
            }
        }
    }
}

private data class NavItem(val route: String, val icon: ImageVector, val label: String)
