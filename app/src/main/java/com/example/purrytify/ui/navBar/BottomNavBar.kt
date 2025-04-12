package com.example.purrytify.ui.navBar

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavBar(
    currentRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.Black
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { onItemSelected("home") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color.White
                )
            },
            label = { Text(text = "Home", color = Color.White) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White
            )
        )
        NavigationBarItem(
            selected = currentRoute == "library",
            onClick = { onItemSelected("library") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Library",
                    tint = Color.White
                )
            },
            label = { Text(text = "Library", color = Color.White) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White
            )
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = { onItemSelected("profile") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White
                )
            },
            label = { Text(text = "Profile", color = Color.White) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White
            )
        )
    }
}

annotation class BottomNavBar

@Composable
fun VerticalNavBar(
    currentRoute: String,
    onItemSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gunakan item yang sama, lalu atur agar tampil vertikal
        NavBarItem(
            icon = Icons.Default.Home,
            label = "Home",
            selected = currentRoute == "home",
            onClick = { onItemSelected("home") }
        )
        NavBarItem(
            icon = Icons.Default.Folder,
            label = "Library",
            selected = currentRoute == "library",
            onClick = { onItemSelected("library") }
        )
        NavBarItem(
            icon = Icons.Default.Person,
            label = "Profile",
            selected = currentRoute == "profile",
            onClick = { onItemSelected("profile") }
        )
    }
}

@Composable
fun NavBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tintColor = if (selected) Color.White else Color.White
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.padding(4.dp)
        )
        Text(
            text = label,
            color = tintColor,
            style = MaterialTheme.typography.bodySmall
        )
    }
}