package me.spoo.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.spoo.android.BuildConfig
import me.spoo.android.data.AppSettings
import me.spoo.android.data.ThemeMode

/**
 * Settings, in the M3E grouped-list idiom: section labels outside, groups
 * of full-width rows with split rounded corners, identity in leading icons.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    username: String,
    settings: AppSettings,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetUseDeviceColors: (Boolean) -> Unit,
    onSetSeedColor: (Long) -> Unit,
    onSetShowShare: (Boolean) -> Unit,
    onSetMockData: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Settings") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Group("Appearance") {
                GroupRow(0, rowCountAppearance(settings)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Theme",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            ThemeMode.entries.forEachIndexed { i, mode ->
                                ToggleButton(
                                    checked = settings.themeMode == mode,
                                    onCheckedChange = { onSetThemeMode(mode) },
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                                    shapes = when (i) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        ThemeMode.entries.lastIndex ->
                                            ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                ) {
                                    Icon(
                                        when (mode) {
                                            ThemeMode.System -> Icons.Outlined.BrightnessAuto
                                            ThemeMode.Light -> Icons.Outlined.LightMode
                                            ThemeMode.Dark -> Icons.Outlined.DarkMode
                                        },
                                        contentDescription = mode.name,
                                    )
                                }
                            }
                        }
                    }
                }
                GroupRow(1, rowCountAppearance(settings)) {
                    SwitchRow(
                        icon = Icons.Outlined.Wallpaper,
                        title = "Device colors",
                        subtitle = "Follow your wallpaper's palette",
                        checked = settings.useDeviceColors,
                        onChecked = onSetUseDeviceColors,
                    )
                }
                if (!settings.useDeviceColors) {
                    GroupRow(2, rowCountAppearance(settings)) {
                        RowScaffold(Icons.Outlined.Palette, "Accent color") {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AppSettings.SEED_CHOICES.forEach { seed ->
                                    Swatch(
                                        color = Color(seed),
                                        selected = settings.seedColor == seed,
                                        onClick = { onSetSeedColor(seed) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Group("Behavior") {
                GroupRow(0, 1) {
                    SwitchRow(
                        icon = Icons.Outlined.Share,
                        title = "Share in link menu",
                        subtitle = "Show a Share action on every link",
                        checked = settings.showShareInMenu,
                        onChecked = onSetShowShare,
                    )
                }
            }

            Group("Account") {
                GroupRow(0, 2) {
                    RowScaffold(
                        Icons.Outlined.AccountCircle,
                        username,
                        subtitle = "Signed in with spoo.me",
                    )
                }
                GroupRow(1, 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSignOut)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Sign out",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                Group("Developer") {
                    GroupRow(0, 1) {
                        SwitchRow(
                            icon = Icons.Outlined.Science,
                            title = "Mock data",
                            subtitle = "Fixture links and stats, no backend",
                            checked = settings.mockData,
                            onChecked = onSetMockData,
                        )
                    }
                }
            }

            Text(
                "spoo.me for Android ${BuildConfig.VERSION_NAME}\n" +
                    "World map: Simple World Map by Fritz Lekschas, CC BY-SA 3.0",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun rowCountAppearance(settings: AppSettings) = if (settings.useDeviceColors) 2 else 3

@Composable
private fun Group(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
    }
}

/** Split-corner group row: big radius on the group's outer edges. */
@Composable
private fun GroupRow(index: Int, count: Int, content: @Composable () -> Unit) {
    Surface(
        shape = groupShape(index, count),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.padding(bottom = if (index == count - 1) 0.dp else 2.dp),
    ) { content() }
}

private fun groupShape(index: Int, count: Int): Shape {
    val big = 20.dp
    val small = 5.dp
    return RoundedCornerShape(
        topStart = if (index == 0) big else small,
        topEnd = if (index == 0) big else small,
        bottomStart = if (index == count - 1) big else small,
        bottomEnd = if (index == count - 1) big else small,
    )
}

@Composable
private fun RowScaffold(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    below: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.padding(16.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            below?.invoke()
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
        }
    }
}
