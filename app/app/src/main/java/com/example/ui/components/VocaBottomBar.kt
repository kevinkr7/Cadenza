package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("dashboard", Icons.Default.Home, "Home")
    object Songs : BottomNavItem("songs", Icons.Default.MusicNote, "Songs")
    object Practice : BottomNavItem("practice", Icons.Default.Mic, "Practice")
    object Progress : BottomNavItem("progress", Icons.Default.BarChart, "Progress")
    object Coach : BottomNavItem("coach", Icons.Default.Person, "Coach")
}

@Preview
@Composable
fun VocaBottomBarPreview() {
    // Mock the state for preview
    Surface(color = MaterialTheme.colorScheme.background) {
        VocaBottomBarContent(
            items = listOf(
                BottomNavItem.Home,
                BottomNavItem.Songs,
                BottomNavItem.Practice,
                BottomNavItem.Progress,
                BottomNavItem.Coach
            ),
            currentRoute = "dashboard",
            onItemClick = {}
        )
    }
}

@Composable
fun VocaBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Songs,
        BottomNavItem.Practice,
        BottomNavItem.Progress,
        BottomNavItem.Coach
    )

    val showBottomBar = currentRoute in items.map { it.route }

    if (showBottomBar) {
        VocaBottomBarContent(
            items = items,
            currentRoute = currentRoute,
            onItemClick = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
private fun VocaBottomBarContent(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .neumorphic(isPressed = false, cornerRadius = 40.dp, elevation = 8.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(40.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    
                    CadenzaNavItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onItemClick(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CadenzaNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource, 
                indication = null, 
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val iconColor = if (isSelected) com.example.ui.theme.CadenzaPrimary else com.example.ui.theme.CadenzaTextSecondary
        
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = iconColor
        )
    }
}
