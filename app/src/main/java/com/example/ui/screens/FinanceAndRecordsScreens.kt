package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppScreen
import com.example.data.AttendanceStatuses
import com.example.data.KeyDateRecord
import com.example.data.WorkedFormatters
import com.example.ui.WorkedDashboardState
import com.example.ui.components.GroupedActionRow
import com.example.ui.components.WorkedCard
import com.example.ui.components.WorkedTopBar
import com.example.ui.theme.Accent500
import com.example.ui.theme.Accent600
import com.example.ui.theme.Amber200
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber700
import com.example.ui.theme.Brand500
import com.example.ui.theme.Brand600
import com.example.ui.theme.Brand700
import com.example.ui.theme.Cyan700
import com.example.ui.theme.Emerald300
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate800
import com.example.ui.theme.TintAmberDark
import com.example.ui.theme.TintAmberLight
import com.example.ui.theme.TintLavenderDark
import com.example.ui.theme.TintLavenderLight
import com.example.ui.theme.TintMintDark
import com.example.ui.theme.TintMintLight
import com.example.ui.theme.TintPeachDark
import com.example.ui.theme.TintPeachLight
import com.example.ui.theme.TintRoseDark
import com.example.ui.theme.TintRoseLight
import com.example.ui.theme.TintSkyDark
import com.example.ui.theme.TintSkyLight

@Composable
fun ActivityLedgerScreen(
    state: WorkedDashboardState,
    isDark: Boolean,
    onBack: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenQuickLeave: () -> Unit
) {
    val vals = state.attendance.values
    val present = vals.count { it.status in listOf(AttendanceStatuses.PRESENT, AttendanceStatuses.LATE, AttendanceStatuses.EARLY_GOING) }
    val outWork = state.outOnWorkLogs.size
    val casual = vals.count { it.status == AttendanceStatuses.LEAVE && it.leaveType == "Casual Leave" }
    val earned = vals.count { it.status == AttendanceStatuses.LEAVE && it.leaveType == "Earned Leave" }
    val commuted = vals.count { it.status == AttendanceStatuses.LEAVE && it.leaveType == "Commuted Leave" }
    val totalCount = present + outWork + casual + earned + commuted + state.expenses.size

    data class RecentEntry(val date: String, val title: String, val sub: String, val badge: String, val isOutWork: Boolean)
    val recentEntries = remember(state.outOnWorkLogs, state.attendance) {
        val list = mutableListOf<RecentEntry>()
        state.outOnWorkLogs.take(4).forEach { o ->
            list.add(
                RecentEntry(
                    date = o.date,
                    title = o.meetingWith,
                    sub = "${o.mode} • ${WorkedFormatters.formatTime12h(o.startTime)}–${WorkedFormatters.formatTime12h(o.endTime)}",
                    badge = "Out Work",
                    isOutWork = true
                )
            )
        }
        state.attendance.entries
            .filter { it.value.status == AttendanceStatuses.LEAVE }
            .sortedByDescending { it.key }
            .take(4)
            .forEach { (d, r) ->
                list.add(
                    RecentEntry(
                        date = d,
                        title = r.leaveType.ifBlank { "Leave" },
                        sub = r.note.ifBlank { "Logged on calendar" },
                        badge = "Leave",
                        isOutWork = false
                    )
                )
            }
        list.sortedByDescending { it.date }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WorkedTopBar(title = "Activity Ledger", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(Brand700, Brand500)))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Layers, contentDescription = null, tint = Color.White)
                            }
                            Column {
                                Text("Total Logged Records", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(
                                    "Self-managed • Firebase cloud synced",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE0F7FA)
                                )
                            }
                        }
                        Text(
                            text = totalCount.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Timeoffice",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                WorkedCard {
                    Column {
                        GroupedActionRow(
                            icon = Icons.Rounded.CalendarMonth,
                            iconTintBg = if (isDark) TintMintDark else TintMintLight,
                            iconTintFg = if (isDark) Emerald300 else Slate800,
                            label = "Present Days",
                            trailingText = present.toString().padStart(2, '0'),
                            onClick = { onNavigate(AppScreen.ATTENDANCE) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GroupedActionRow(
                            icon = Icons.Rounded.WorkOutline,
                            iconTintBg = if (isDark) TintPeachDark else TintPeachLight,
                            iconTintFg = if (isDark) Color(0xFFFDBA74) else Slate800,
                            label = "Out Work",
                            trailingText = outWork.toString().padStart(2, '0'),
                            onClick = { onNavigate(AppScreen.OUT_ON_WORK) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GroupedActionRow(
                            icon = Icons.Rounded.EventBusy,
                            iconTintBg = if (isDark) TintSkyDark else TintSkyLight,
                            iconTintFg = if (isDark) Color(0xFF7DD3FC) else Slate800,
                            label = "Casual Leave",
                            trailingText = casual.toString().padStart(2, '0'),
                            onClick = onOpenQuickLeave
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GroupedActionRow(
                            icon = Icons.Rounded.EventAvailable,
                            iconTintBg = if (isDark) TintLavenderDark else TintLavenderLight,
                            iconTintFg = if (isDark) Color(0xFFD8B4FE) else Slate800,
                            label = "Earned Leave",
                            trailingText = earned.toString().padStart(2, '0'),
                            onClick = onOpenQuickLeave
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GroupedActionRow(
                            icon = Icons.Rounded.FavoriteBorder,
                            iconTintBg = if (isDark) TintAmberDark else TintAmberLight,
                            iconTintFg = if (isDark) Amber200 else Slate800,
                            label = "Commuted Leave",
                            trailingText = commuted.toString().padStart(2, '0'),
                            onClick = onOpenQuickLeave
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GroupedActionRow(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            iconTintBg = if (isDark) TintMintDark else TintMintLight,
                            iconTintFg = if (isDark) Emerald300 else Slate800,
                            label = "Expenses Logged",
                            trailingText = state.expenses.size.toString().padStart(2, '0'),
                            onClick = { onNavigate(AppScreen.EXPENSES) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Recent Entries",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (recentEntries.isEmpty()) {
                item {
                    WorkedCard {
                        Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            Text("No recent entries yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(recentEntries) { item ->
                    WorkedCard {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${WorkedFormatters.formatPrettyDate(item.date)} • ${item.sub}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (item.isOutWork) (if (isDark) TintSkyDark else Color(0xFFECFEFF)) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = item.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isOutWork) (if (isDark) Color(0xFF67E8F9) else Cyan700) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
fun ExpensesScreen(
    state: WorkedDashboardState,
    categories: List<String>,
    isDark: Boolean,
    onAddExpense: (date: String, title: String, category: String, amount: Long, owner: String, note: String) -> Unit,
    onToggleRecovered: (id: String, currentRecovered: Boolean) -> Unit,
    onDeleteExpense: (id: String) -> Unit
) {
    var activeFilter by remember { mutableStateOf("all") }
    var showAddDialog by remember { mutableStateOf(false) }

    val hmDue = state.expenses.filter { it.owner == "headmaster" && !it.recovered }.sumOf { it.amount }
    val hmRecovered = state.expenses.filter { it.owner == "headmaster" && it.recovered }.sumOf { it.amount }
    val personalTotal = state.expenses.filter { it.owner == "personal" }.sumOf { it.amount }
    val filtered = state.expenses.filter { activeFilter == "all" || it.owner == activeFilter }

    Column(modifier = Modifier.fillMaxSize()) {
        WorkedTopBar(
            title = "Expenses",
            trailing = {
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_expense_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelMedium)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dual Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Accent500, Accent600)))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("Headmaster Due", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFEDD5))
                            Spacer(Modifier.height(4.dp))
                            Text(WorkedFormatters.formatInr(hmDue), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Recovered: ${WorkedFormatters.formatInr(hmRecovered)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFEDD5).copy(alpha = 0.9f)
                            )
                        }
                    }
                    WorkedCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Personal Spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(WorkedFormatters.formatInr(personalTotal), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${state.expenses.size} record${if (state.expenses.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Segmented Filter Tabs
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("all" to "All", "headmaster" to "Headmaster", "personal" to "Personal").forEach { (key, label) ->
                            val selected = activeFilter == key
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeFilter = key }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    WorkedCard {
                        Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            Text("No expenses in this category.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { exp ->
                    val isHM = exp.owner == "headmaster"
                    WorkedCard {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = exp.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        val (bBg, bFg, bTxt) = when {
                                            isHM && exp.recovered -> Triple(if (isDark) TintMintDark else Color(0xFFECFDF5), if (isDark) Emerald300 else Emerald700, "HM • Recovered")
                                            isHM -> Triple(if (isDark) TintPeachDark else Color(0xFFFFF7ED), if (isDark) Color(0xFFFDBA74) else Accent600, "HM • Due")
                                            else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Personal")
                                        }
                                        Surface(shape = RoundedCornerShape(999.dp), color = bBg) {
                                            Text(bTxt, style = MaterialTheme.typography.labelSmall, color = bFg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "${WorkedFormatters.formatPrettyDate(exp.date)} • ${exp.category}${if (exp.note.isNotBlank()) " • ${exp.note}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = WorkedFormatters.formatInr(exp.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isHM) {
                                    TextButton(
                                        onClick = { onToggleRecovered(exp.id, exp.recovered) },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (exp.recovered) Icons.Rounded.Replay else Icons.Rounded.CheckCircleOutline,
                                            contentDescription = null,
                                            tint = if (exp.recovered) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (exp.recovered) "Mark as Due" else "Mark Recovered",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (exp.recovered) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Text("Personal expense", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(
                                    onClick = { onDeleteExpense(exp.id) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Rose500, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete", style = MaterialTheme.typography.labelMedium, color = Rose500)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { date, title, cat, amount, owner, note ->
                onAddExpense(date, title, cat, amount, owner, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddExpenseDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (date: String, title: String, category: String, amount: Long, owner: String, note: String) -> Unit
) {
    val safeCats = categories.ifEmpty { listOf("Office", "Travel", "Food", "Printing", "Other") }
    var owner by remember { mutableStateOf("headmaster") }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(WorkedFormatters.todayDateKey()) }
    var category by remember { mutableStateOf(safeCats.first()) }
    var catMenu by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("headmaster" to "Headmaster", "personal" to "Personal").forEach { (k, lbl) ->
                        val selected = owner == k
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) {
                                if (k == "headmaster") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) (if (k == "headmaster") Accent500 else Brand600) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { owner = k }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    lbl,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) {
                                        if (k == "headmaster") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("Stationery / Printing / Travel") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { if (it.length <= 10) date = it },
                        label = { Text("Date") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                Box {
                    OutlinedButton(
                        onClick = { catMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Category: $category", modifier = Modifier.weight(1f))
                    }
                    DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                        safeCats.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catMenu = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Voucher / Note (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amount = amountStr.toLongOrNull() ?: 0L
            Button(
                onClick = { onSave(date, title, category, amount, owner, note) },
                enabled = title.isNotBlank() && amount in 1L..100_000_000L && WorkedFormatters.isValidDateKey(date),
                colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White)
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FinanceScreen(
    state: WorkedDashboardState,
    isDark: Boolean,
    onAddFinance: (date: String, title: String, amount: Long, status: String) -> Unit,
    onToggleStatus: (id: String, currentStatus: String) -> Unit,
    onDeleteFinance: (id: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val totalReceived = state.finances.filter { it.status == "received" }.sumOf { it.amount }
    val totalPending = state.finances.filter { it.status != "received" }.sumOf { it.amount }
    val personalSpent = state.expenses.filter { it.owner == "personal" }.sumOf { it.amount }
    val netBalance = totalReceived - personalSpent

    Column(modifier = Modifier.fillMaxSize()) {
        WorkedTopBar(
            title = "Pay & Finance",
            trailing = {
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_finance_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelMedium)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Brand700, Brand500)))
                        .padding(22.dp)
                ) {
                    Column {
                        Text(
                            "Net Balance (Pay − Personal Expenses)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE0F7FA)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            WorkedFormatters.formatInr(netBalance),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.22f))
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Salary & Allowances", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE0F7FA))
                                Spacer(Modifier.height(2.dp))
                                Text(WorkedFormatters.formatInr(totalReceived), style = MaterialTheme.typography.titleMedium, color = Color(0xFFA7F3D0))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pending Arrears / Bills", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE0F7FA))
                                Spacer(Modifier.height(2.dp))
                                Text(WorkedFormatters.formatInr(totalPending), style = MaterialTheme.typography.titleMedium, color = Amber200)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Salary, Allowances & Bills",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (state.finances.isEmpty()) {
                item {
                    WorkedCard {
                        Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            Text("No pay or bill records yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(state.finances, key = { it.id }) { fin ->
                    val isReceived = fin.status == "received"
                    WorkedCard {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fin.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(WorkedFormatters.formatPrettyDate(fin.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isReceived) (if (isDark) TintMintDark else Color(0xFFECFDF5)) else (if (isDark) TintAmberDark else TintAmberLight),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { onToggleStatus(fin.id, fin.status) }
                                    ) {
                                        Text(
                                            text = if (isReceived) "Received" else "Pending (Tap to mark)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isReceived) (if (isDark) Emerald300 else Emerald700) else (if (isDark) Amber200 else Amber700),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "Delete",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Rose500,
                                        modifier = Modifier.clickable { onDeleteFinance(fin.id) }.padding(4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "+${WorkedFormatters.formatInr(fin.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isReceived) (if (isDark) Emerald300 else Emerald700) else (if (isDark) Amber200 else Amber700)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFinanceDialog(
            onDismiss = { showAddDialog = false },
            onSave = { date, title, amount, status ->
                onAddFinance(date, title, amount, status)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddFinanceDialog(
    onDismiss: () -> Unit,
    onSave: (date: String, title: String, amount: Long, status: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(WorkedFormatters.todayDateKey()) }
    var status by remember { mutableStateOf("received") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Salary / Allowance", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description") },
                    placeholder = { Text("Monthly Salary / DA Arrears / TA Bill") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { if (it.length <= 10) date = it },
                        label = { Text("Date") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("received" to "Received", "pending" to "Pending Bill").forEach { (k, lbl) ->
                        val selected = status == k
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) Brand600 else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { status = k }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    lbl,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amt = amountStr.toLongOrNull() ?: 0L
            Button(
                onClick = { onSave(date, title, amt, status) },
                enabled = title.isNotBlank() && amt in 1L..100_000_000L && WorkedFormatters.isValidDateKey(date),
                colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White)
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun KeyDatesScreen(
    state: WorkedDashboardState,
    categories: List<String>,
    isDark: Boolean,
    onBack: () -> Unit,
    onSaveReminder: (editId: String?, title: String, category: String, validFrom: String, validUntil: String, note: String) -> Unit,
    onDeleteReminder: (String) -> Unit
) {
    var filter by remember { mutableStateOf("all") }
    var editingItem by remember { mutableStateOf<KeyDateRecord?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(state.reminders, filter) {
        state.reminders.filter { r ->
            val days = WorkedFormatters.daysUntil(r.validUntil)
            when (filter) {
                "soon" -> days in 0..60
                "expired" -> days < 0
                else -> true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WorkedTopBar(
            title = "Key Dates",
            onBack = onBack,
            trailing = {
                IconButton(onClick = { editingItem = null; showAddDialog = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Add Key Date", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all" to "All", "soon" to "≤ 60d", "expired" to "Expired").forEach { (k, lbl) ->
                            val sel = filter == k
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (sel) Brand600 else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (sel) Brand600 else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable { filter = k }
                            ) {
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Text("${filtered.size} item${if (filtered.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (filtered.isEmpty()) {
                item {
                    WorkedCard {
                        Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            Text("No key dates found. Tap + to add one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    val days = WorkedFormatters.daysUntil(item.validUntil)
                    val progress = WorkedFormatters.validityProgress(item.validFrom, item.validUntil)
                    val (badgeText, badgeBg, badgeFg, barColor) = when {
                        days < 0 -> Quad("Expired ${-days}d ago", if (isDark) TintRoseDark else TintRoseLight, if (isDark) Color(0xFFFDA4AF) else Rose600, Rose500)
                        days <= 60 -> Quad(if (days == 0L) "Expires Today" else "Expires in ${days}d", if (isDark) TintAmberDark else TintAmberLight, if (isDark) Amber200 else Amber700, Amber500)
                        else -> Quad("Active (${days}d left)", if (isDark) TintMintDark else Color(0xFFECFDF5), if (isDark) Emerald300 else Emerald700, Brand500)
                    }

                    WorkedCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(2.dp))
                                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = RoundedCornerShape(999.dp), color = badgeBg) {
                                    Text(badgeText, style = MaterialTheme.typography.labelSmall, color = badgeFg, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(WorkedFormatters.formatPrettyDate(item.validFrom), style = MaterialTheme.typography.bodySmall)
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(WorkedFormatters.formatPrettyDate(item.validUntil), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
                            )
                            if (item.note.isNotBlank()) {
                                Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { editingItem = item; showAddDialog = true }) {
                                    Text("Edit", color = MaterialTheme.colorScheme.primary)
                                }
                                TextButton(onClick = { onDeleteReminder(item.id) }) {
                                    Text("Delete", color = Rose500)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ReminderDialog(
            existing = editingItem,
            categories = categories,
            onDismiss = { showAddDialog = false; editingItem = null },
            onSave = { title, cat, from, until, note ->
                onSaveReminder(editingItem?.id, title, cat, from, until, note)
                showAddDialog = false
                editingItem = null
            }
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun ReminderDialog(
    existing: KeyDateRecord?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, validFrom: String, validUntil: String, note: String) -> Unit
) {
    val safeCats = categories.ifEmpty { listOf("Document", "Compliance", "Contract", "License", "Other") }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category?.ifBlank { safeCats.first() } ?: safeCats.first()) }
    var catMenu by remember { mutableStateOf(false) }
    var validFrom by remember { mutableStateOf(existing?.validFrom?.ifBlank { WorkedFormatters.todayDateKey() } ?: WorkedFormatters.todayDateKey()) }
    var validUntil by remember { mutableStateOf(existing?.validUntil?.ifBlank { WorkedFormatters.shiftDateKey(WorkedFormatters.todayDateKey(), 365) } ?: WorkedFormatters.shiftDateKey(WorkedFormatters.todayDateKey(), 365)) }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Key Date" else "Add Key Date", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Document Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedButton(
                        onClick = { catMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Category: $category", modifier = Modifier.weight(1f))
                    }
                    DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                        safeCats.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catMenu = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = validFrom,
                        onValueChange = { if (it.length <= 10) validFrom = it },
                        label = { Text("Valid From") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = validUntil,
                        onValueChange = { if (it.length <= 10) validUntil = it },
                        label = { Text("Valid Until") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes") },
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val validDates = WorkedFormatters.isValidDateKey(validFrom) &&
                WorkedFormatters.isValidDateKey(validUntil) &&
                validUntil >= validFrom
            Button(
                onClick = { onSave(title, category, validFrom, validUntil, note) },
                enabled = title.isNotBlank() && validDates,
                colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White)
            ) {
                Text(if (existing != null) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
