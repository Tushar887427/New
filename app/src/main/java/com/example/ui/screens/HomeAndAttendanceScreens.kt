package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.AppScreen
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatuses
import com.example.data.WorkedFormatters
import com.example.ui.WorkedDashboardState
import com.example.ui.components.DataUrlImage
import com.example.ui.components.GroupedActionRow
import com.example.ui.components.WorkedCard
import com.example.ui.components.WorkedMinimalLogo
import com.example.ui.components.WorkedTopBar
import com.example.ui.components.statusColors
import com.example.ui.theme.Accent500
import com.example.ui.theme.Accent600
import com.example.ui.theme.Amber200
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber950
import com.example.ui.theme.Brand500
import com.example.ui.theme.Brand600
import com.example.ui.theme.Brand700
import com.example.ui.theme.Emerald300
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.StatusPresent
import com.example.ui.theme.TintAmberDark
import com.example.ui.theme.TintAmberLight
import com.example.ui.theme.TintLavenderDark
import com.example.ui.theme.TintLavenderLight
import com.example.ui.theme.TintMintDark
import com.example.ui.theme.TintMintLight
import com.example.ui.theme.TintPeachDark
import com.example.ui.theme.TintPeachLight
import com.example.ui.theme.TintSkyDark
import com.example.ui.theme.TintSkyLight
import com.google.android.gms.location.LocationServices
import java.util.Calendar

@Composable
fun HomeScreen(
    state: WorkedDashboardState,
    isDark: Boolean,
    isPunchBusy: Boolean = false,
    isBreakBusy: Boolean = false,
    onNavigate: (AppScreen) -> Unit,
    onToggleDarkMode: () -> Unit,
    onPunchToggle: () -> Unit,
    onBreakToggle: () -> Unit,
    onOpenMissedPunch: () -> Unit,
    onOpenQuickLeave: () -> Unit
) {
    val profile = state.profile
    val todayKey = WorkedFormatters.todayDateKey()
    val todayRec = state.attendance[todayKey]
    val isSameDay = profile.sessionDate == todayKey
    val punchedIn = isSameDay && profile.punchedIn
    val inTime = if (isSameDay && profile.sessionInTime.isNotBlank()) profile.sessionInTime else todayRec?.inTime.orEmpty()
    val outTime = if (punchedIn) {
        ""
    } else if (isSameDay && profile.sessionOutTime.isNotBlank()) {
        profile.sessionOutTime
    } else {
        todayRec?.outTime.orEmpty()
    }
    val onBreak = isSameDay && profile.onBreak
    val totalBreakMins = if (isSameDay) profile.totalBreakMins else 0L

    val monthPrefix = todayKey.take(7)
    val presentCount = state.attendance.entries.count { (k, r) ->
        k.startsWith(monthPrefix) && r.status in listOf(
            AttendanceStatuses.PRESENT,
            AttendanceStatuses.LATE,
            AttendanceStatuses.EARLY_GOING
        )
    }
    val outWorkCount = state.attendance.entries.count { (k, r) ->
        k.startsWith(monthPrefix) && r.status == AttendanceStatuses.OUTSTATION
    }
    val leaveCount = state.attendance.entries.count { (k, r) ->
        k.startsWith(monthPrefix) && r.status == AttendanceStatuses.LEAVE
    }
    val hmDue = state.expenses
        .filter { it.owner == "headmaster" && !it.recovered }
        .sumOf { it.amount }
    val hasReminderAlert = state.reminders.any { WorkedFormatters.daysUntil(it.validUntil) <= 60 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Top Sunlit Summer Header + Floating Attendance Card
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(Brush.linearGradient(listOf(Brand700, Brand500)))
                        .statusBarsPadding()
                        .padding(start = 22.dp, end = 18.dp, top = 16.dp, bottom = 92.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                WorkedMinimalLogo(size = 20.dp)
                                Text(
                                    text = "Work'ed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE0F7FA)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Hello, ${profile.displayName.ifBlank { "Admin" }}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onToggleDarkMode,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .testTag("home_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "Toggle Dark Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { onNavigate(AppScreen.REMINDERS) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                        .testTag("home_reminders_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EventNote,
                                        contentDescription = "Key Dates",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (hasReminderAlert) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Amber500)
                                            .border(1.5.dp, Brand700, CircleShape)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { onNavigate(AppScreen.PROFILE) }
                                    .testTag("home_profile_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                DataUrlImage(
                                    dataUrl = profile.photoDataUrl,
                                    contentDescription = "My Profile",
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = WorkedFormatters.initialsFor(profile.displayName),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Today's Attendance Card
                WorkedCard(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .padding(top = 112.dp)
                        .statusBarsPadding()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Today's Attendance",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = WorkedFormatters.formatTodayHeader(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { onNavigate(AppScreen.ATTENDANCE) },
                                modifier = Modifier.testTag("home_attendance_details_btn")
                            ) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // In / Out Readout Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "In Time",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = WorkedFormatters.formatTime12h(inTime),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(32.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Out Time",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = WorkedFormatters.formatTime12h(outTime),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Main Punch In / Out Gradient CTA
                        val punchGradient = if (punchedIn) {
                            Brush.horizontalGradient(listOf(Rose500, Rose600))
                        } else {
                            Brush.horizontalGradient(listOf(Accent500, Accent600))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(punchGradient)
                                .clickable(enabled = !isPunchBusy, onClick = onPunchToggle)
                                .testTag("punch_toggle_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isPunchBusy) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Saving...",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (punchedIn) Icons.AutoMirrored.Rounded.Logout else Icons.AutoMirrored.Rounded.Login,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = when {
                                            punchedIn -> "Punch Out Now"
                                            outTime.isNotBlank() -> "Punch In Again"
                                            else -> "Punch In Now"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // 3 Tactile Quick Action Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) TintMintDark else TintMintLight,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = onOpenMissedPunch)
                                    .testTag("home_missed_punch_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccessTime,
                                        contentDescription = "Log or Correct Time",
                                        tint = if (isDark) Emerald300 else Slate800,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) TintPeachDark else TintPeachLight,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onNavigate(AppScreen.OUT_ON_WORK) }
                                    .testTag("home_out_work_pill")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.WorkOutline,
                                        contentDescription = "Out Work",
                                        tint = if (isDark) Color(0xFFFDBA74) else Slate800,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = when {
                                    onBreak -> if (isDark) TintAmberDark else Color(0xFFFEF3C7)
                                    isDark -> TintSkyDark
                                    else -> TintSkyLight
                                },
                                border = if (onBreak) BorderStroke(1.5.dp, Amber500) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(enabled = !isBreakBusy, onClick = onBreakToggle)
                                    .testTag("home_break_toggle_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (onBreak) Icons.Rounded.PauseCircle else Icons.Rounded.Coffee,
                                        contentDescription = if (onBreak) "End Break" else "Start Break",
                                        tint = when {
                                            onBreak -> if (isDark) Amber200 else Amber950
                                            isDark -> Color(0xFF7DD3FC)
                                            else -> Slate800
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Weekly off - ${WorkedFormatters.dayNames.getOrElse(profile.weeklyOff.toInt()) { "Sun" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (onBreak) "On break now" else "Breaks: ${totalBreakMins}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Monthly Summary Card
        item {
            Spacer(Modifier.height(18.dp))
            WorkedCard(modifier = Modifier.padding(horizontal = 18.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Summary",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { onNavigate(AppScreen.RECORDS) }) {
                            Text(
                                text = "Full Ledger",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatTile(
                            count = presentCount,
                            label = "Present",
                            valueColor = if (isDark) Emerald300 else StatusPresent,
                            onClick = { onNavigate(AppScreen.ATTENDANCE) },
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatTile(
                            count = outWorkCount,
                            label = "Out Work",
                            valueColor = MaterialTheme.colorScheme.primary,
                            onClick = { onNavigate(AppScreen.RECORDS) },
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatTile(
                            count = leaveCount,
                            label = "Leaves",
                            valueColor = if (isDark) Amber200 else Amber500,
                            onClick = { onNavigate(AppScreen.RECORDS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    // Headmaster Recoverable Banner
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) TintAmberDark else TintAmberLight,
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Amber500.copy(alpha = 0.3f) else Amber500.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onNavigate(AppScreen.EXPENSES) }
                            .testTag("home_headmaster_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Amber500.copy(alpha = 0.2f) else Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReceiptLong,
                                        contentDescription = null,
                                        tint = if (isDark) Amber200 else Slate800,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "To Recover from Headmaster",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Amber200 else Amber950
                                    )
                                    Text(
                                        text = "Tap to view or settle bills",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Amber200.copy(alpha = 0.78f) else Amber950.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            Text(
                                text = WorkedFormatters.formatInr(hmDue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Amber200 else Amber950
                            )
                        }
                    }
                }
            }
        }

        // Office Actions Grouped List
        item {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Office Actions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
            )
            WorkedCard(modifier = Modifier.padding(horizontal = 18.dp)) {
                Column {
                    GroupedActionRow(
                        icon = Icons.Rounded.EventBusy,
                        iconTintBg = if (isDark) TintMintDark else TintMintLight,
                        iconTintFg = if (isDark) Emerald300 else Slate800,
                        label = "Leave",
                        onClick = onOpenQuickLeave
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    GroupedActionRow(
                        icon = Icons.Rounded.WorkOutline,
                        iconTintBg = if (isDark) TintPeachDark else TintPeachLight,
                        iconTintFg = if (isDark) Color(0xFFFDBA74) else Slate800,
                        label = "Out Work",
                        onClick = { onNavigate(AppScreen.OUT_ON_WORK) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    GroupedActionRow(
                        icon = Icons.Rounded.Checklist,
                        iconTintBg = if (isDark) TintLavenderDark else TintLavenderLight,
                        iconTintFg = if (isDark) Color(0xFFD8B4FE) else Slate800,
                        label = "Daily Tasks",
                        onClick = { onNavigate(AppScreen.DAILY_TASKS) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    GroupedActionRow(
                        icon = Icons.Rounded.Groups,
                        iconTintBg = if (isDark) TintSkyDark else TintSkyLight,
                        iconTintFg = if (isDark) Color(0xFF7DD3FC) else Slate800,
                        label = "Colleagues",
                        onClick = { onNavigate(AppScreen.COLLEAGUES) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatTile(
    count: Int,
    label: String,
    valueColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = count.toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun AttendanceScreen(
    state: WorkedDashboardState,
    yearMonth: Pair<Int, Int>,
    leaveTypes: List<String>,
    onBack: () -> Unit,
    onChangeMonth: (Int) -> Unit,
    onOpenMissedPunch: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onSaveDay: (dateKey: String, status: String, leaveType: String, inTime: String, outTime: String, note: String) -> Unit,
    onClearDay: (String) -> Unit
) {
    val (year, month) = yearMonth
    val cal = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun..6=Sat
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val todayKey = WorkedFormatters.todayDateKey()
    val weeklyOff = state.profile.weeklyOff.toInt()

    var editingDateKey by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        WorkedTopBar(title = "Attendance", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 4 Sub-Nav Tiles
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubNavTile(
                            icon = Icons.Rounded.CalendarMonth,
                            label = "Calendar",
                            active = true,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        SubNavTile(
                            icon = Icons.Rounded.AccessTime,
                            label = "Log Time",
                            active = false,
                            onClick = onOpenMissedPunch,
                            modifier = Modifier.weight(1f)
                        )
                        SubNavTile(
                            icon = Icons.Rounded.WorkOutline,
                            label = "Out Work",
                            active = false,
                            onClick = { onNavigate(AppScreen.OUT_ON_WORK) },
                            modifier = Modifier.weight(1f)
                        )
                        SubNavTile(
                            icon = Icons.Rounded.Schedule,
                            label = "Tasks",
                            active = false,
                            onClick = { onNavigate(AppScreen.DAILY_TASKS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Month Picker Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { onChangeMonth(-1) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous Month")
                            }
                            Text(
                                text = "${WorkedFormatters.monthNames.getOrElse(month) { "Jan" }}, $year",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { onChangeMonth(1) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next Month")
                            }
                        }
                    }
                    Text(
                        text = "Tap any date to edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 7-Column Calendar Card
            item {
                WorkedCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            WorkedFormatters.dayNames.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        val totalCells = firstDayOfWeek + daysInMonth
                        val rows = (totalCells + 6) / 7
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (r in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (c in 0 until 7) {
                                        val cellIdx = r * 7 + c
                                        val dayNum = cellIdx - firstDayOfWeek + 1
                                        if (dayNum in 1..daysInMonth) {
                                            val key = WorkedFormatters.toDateKey(year, month, dayNum)
                                            val rec = state.attendance[key]
                                            val isToday = key == todayKey
                                            val isOffDay = c == weeklyOff
                                            val (bg, fg) = when {
                                                rec != null -> statusColors(rec.status)
                                                isOffDay -> statusColors(AttendanceStatuses.OFFDAY)
                                                else -> Color.Transparent to MaterialTheme.colorScheme.onSurface
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(bg)
                                                        .then(
                                                            if (isToday) Modifier.border(
                                                                2.dp,
                                                                MaterialTheme.colorScheme.primary,
                                                                CircleShape
                                                            ) else Modifier
                                                        )
                                                        .clickable { editingDateKey = key },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = dayNum.toString().padStart(2, '0'),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = fg
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Legends Card
            item {
                WorkedCard {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Legends",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))
                        val legendRows = AttendanceStatuses.allLabels.chunked(3)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            legendRows.forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { (key, label) ->
                                        val (dotColor, _) = statusColors(key)
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(dotColor)
                                            )
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        }
    }

    editingDateKey?.let { key ->
        DayEditorDialog(
            dateKey = key,
            existing = state.attendance[key],
            leaveTypes = leaveTypes,
            onDismiss = { editingDateKey = null },
            onSave = { status, leaveType, inT, outT, note ->
                onSaveDay(key, status, leaveType, inT, outT, note)
                editingDateKey = null
            },
            onClear = {
                onClearDay(key)
                editingDateKey = null
            }
        )
    }
}

@Composable
private fun SubNavTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (active) Brand600 else MaterialTheme.colorScheme.surface,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun OutOnWorkScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onSubmit: (date: String, mode: String, start: String, end: String, office: String, location: String, remarks: String, attachment: String, onDone: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var dateKey by remember { mutableStateOf(WorkedFormatters.todayDateKey()) }
    var mode by remember { mutableStateOf("Full Day") }
    var startTime by remember { mutableStateOf("10:00") }
    var endTime by remember { mutableStateOf("16:00") }
    var meetingWith by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var locationStatus by remember { mutableStateOf("Add Live Location") }
    var remarks by remember { mutableStateOf("") }
    var attachmentName by remember { mutableStateOf("") }

    fun fetchLocationNow() {
        locationStatus = "Fetching GPS..."
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val coords = String.format(java.util.Locale.US, "%.4f, %.4f", loc.latitude, loc.longitude)
                    location = coords
                    locationStatus = "Pinned: $coords"
                } else {
                    location = "Field Visit (GPS Pending)"
                    locationStatus = "Location Tagged (Manual)"
                }
            }
            .addOnFailureListener {
                location = "Field Visit (GPS Unavailable)"
                locationStatus = "Location Tagged (Manual)"
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLocationNow()
        } else {
            location = "Field Visit (Manual Tag)"
            locationStatus = "Location Tagged (Manual)"
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachmentName = uri.lastPathSegment?.takeLast(40) ?: "order_photo.jpg"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WorkedTopBar(title = "Out Work", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Stepper
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { dateKey = WorkedFormatters.shiftDateKey(dateKey, -1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Previous Day",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = dateKey,
                        onValueChange = { if (it.length <= 10) dateKey = it },
                        singleLine = true,
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { dateKey = WorkedFormatters.shiftDateKey(dateKey, 1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Next Day",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Day Mode Selector
            item {
                Text(
                    text = "Day Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Half Day", "Full Day", "Quarter Day").forEach { itemMode ->
                        val selected = mode == itemMode
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { mode = itemMode }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = itemMode,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Start & End Time
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { if (it.length <= 5) startTime = it },
                        label = { Text("Start (HH:mm)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { if (it.length <= 5) endTime = it },
                        label = { Text("End (HH:mm)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Office / Department Visit
            item {
                OutlinedTextField(
                    value = meetingWith,
                    onValueChange = { meetingWith = it },
                    label = { Text("Office / Department Visit") },
                    placeholder = { Text("e.g., DEO Office / Treasury / BDO") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("oow_office_input")
                )
            }

            // Live GPS Location
            item {
                OutlinedButton(
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            fetchLocationNow()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = locationStatus, style = MaterialTheme.typography.labelLarge)
                }
            }

            // Remarks
            item {
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    placeholder = { Text("Official work notes, memo number, etc.") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Photo Picker Attachment (Zero Storage Permissions per Play Policy)
            item {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (attachmentName.isBlank()) "Attach order / photo (optional)" else "Attached: $attachmentName",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        onSubmit(
                            dateKey,
                            mode,
                            startTime,
                            endTime,
                            meetingWith,
                            location,
                            remarks,
                            attachmentName,
                            onSaved
                        )
                    },
                    enabled = meetingWith.isNotBlank() && WorkedFormatters.isValidDateKey(dateKey),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_oow_button")
                ) {
                    Text("Save Out Work Record", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun MissedPunchDialog(
    initialDate: String,
    existing: AttendanceRecord?,
    onDismiss: () -> Unit,
    onSave: (dateKey: String, inTime: String, outTime: String, note: String) -> Unit
) {
    var dateKey by remember { mutableStateOf(initialDate) }
    var inTime by remember { mutableStateOf(existing?.inTime?.ifBlank { "09:30" } ?: "09:30") }
    var outTime by remember { mutableStateOf(existing?.outTime?.ifBlank { "17:00" } ?: "17:00") }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log / Correct Office Time", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dateKey,
                    onValueChange = { if (it.length <= 10) dateKey = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inTime,
                        onValueChange = { if (it.length <= 5) inTime = it },
                        label = { Text("In (HH:mm)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = outTime,
                        onValueChange = { if (it.length <= 5) outTime = it },
                        label = { Text("Out (HH:mm)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g., Forgot biometric punch") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val dateValid = WorkedFormatters.isValidDateKey(dateKey)
            val inValid = inTime.isBlank() || WorkedFormatters.normalizeTime24(inTime) != null
            val outValid = outTime.isBlank() || WorkedFormatters.normalizeTime24(outTime) != null
            Button(
                onClick = { onSave(dateKey, inTime, outTime, note) },
                enabled = dateValid && (inTime.isNotBlank() || outTime.isNotBlank()) && inValid && outValid,
                colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White)
            ) {
                Text("Save Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun QuickLeaveDialog(
    leaveTypes: List<String>,
    onDismiss: () -> Unit,
    onSave: (leaveType: String, fromKey: String, toKey: String, note: String) -> Unit
) {
    val safeTypes = leaveTypes.ifEmpty { listOf("Casual Leave", "Earned Leave", "Commuted Leave") }
    var selectedType by remember { mutableStateOf(safeTypes.first()) }
    var expanded by remember { mutableStateOf(false) }
    var fromKey by remember { mutableStateOf(WorkedFormatters.todayDateKey()) }
    var toKey by remember { mutableStateOf(WorkedFormatters.todayDateKey()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Leave", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(selectedType, modifier = Modifier.weight(1f))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        safeTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    selectedType = t
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fromKey,
                        onValueChange = { if (it.length <= 10) fromKey = it },
                        label = { Text("From") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = toKey,
                        onValueChange = { if (it.length <= 10) toKey = it },
                        label = { Text("To") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Reason / Note") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val rangeValid = WorkedFormatters.isValidDateKey(fromKey) &&
                WorkedFormatters.isValidDateKey(toKey) &&
                toKey >= fromKey
            Button(
                onClick = { onSave(selectedType, fromKey, toKey, note) },
                enabled = rangeValid,
                colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White)
            ) {
                Text("Save Leave")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DayEditorDialog(
    dateKey: String,
    existing: AttendanceRecord?,
    leaveTypes: List<String>,
    onDismiss: () -> Unit,
    onSave: (status: String, leaveType: String, inTime: String, outTime: String, note: String) -> Unit,
    onClear: () -> Unit
) {
    val safeLeaveTypes = leaveTypes.ifEmpty { listOf("Casual Leave", "Earned Leave", "Commuted Leave") }
    var status by remember { mutableStateOf(existing?.status ?: AttendanceStatuses.PRESENT) }
    var statusMenuOpen by remember { mutableStateOf(false) }
    var leaveType by remember { mutableStateOf(existing?.leaveType?.ifBlank { safeLeaveTypes.first() } ?: safeLeaveTypes.first()) }
    var leaveMenuOpen by remember { mutableStateOf(false) }
    var inTime by remember { mutableStateOf(existing?.inTime ?: "09:30") }
    var outTime by remember { mutableStateOf(existing?.outTime ?: "17:00") }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }

    val statusLabel = AttendanceStatuses.allLabels.firstOrNull { it.first == status }?.second ?: status

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${WorkedFormatters.formatPrettyDate(dateKey)}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { statusMenuOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Status: $statusLabel", modifier = Modifier.weight(1f))
                    }
                    DropdownMenu(expanded = statusMenuOpen, onDismissRequest = { statusMenuOpen = false }) {
                        AttendanceStatuses.allLabels.forEach { (k, lbl) ->
                            DropdownMenuItem(
                                text = { Text(lbl) },
                                onClick = {
                                    status = k
                                    statusMenuOpen = false
                                }
                            )
                        }
                    }
                }
                if (status == AttendanceStatuses.LEAVE) {
                    Box {
                        OutlinedButton(
                            onClick = { leaveMenuOpen = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Leave: $leaveType", modifier = Modifier.weight(1f))
                        }
                        DropdownMenu(expanded = leaveMenuOpen, onDismissRequest = { leaveMenuOpen = false }) {
                            safeLeaveTypes.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = {
                                        leaveType = t
                                        leaveMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inTime,
                        onValueChange = { if (it.length <= 5) inTime = it },
                        label = { Text("In Time") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = outTime,
                        onValueChange = { if (it.length <= 5) outTime = it },
                        label = { Text("Out Time") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val inValid = inTime.isBlank() || WorkedFormatters.normalizeTime24(inTime) != null
            val outValid = outTime.isBlank() || WorkedFormatters.normalizeTime24(outTime) != null
            Button(
                onClick = { onSave(status, leaveType, inTime, outTime, note) },
                enabled = inValid && outValid,
                colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White)
            ) {
                Text("Save Day")
            }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = Rose600)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
