package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppScreen
import com.example.data.ColleagueRecord
import com.example.data.InfoField
import com.example.data.WorkedFormatters
import com.example.ui.WorkedDashboardState
import com.example.ui.components.AutoAdjustInfoGrid
import com.example.ui.components.DataUrlImage
import com.example.ui.components.GroupedActionRow
import com.example.ui.components.WorkedCard
import com.example.ui.components.WorkedTopBar
import com.example.ui.components.compressUriToDataUrl
import com.example.ui.theme.Accent500
import com.example.ui.theme.Accent600
import com.example.ui.theme.Amber200
import com.example.ui.theme.Brand500
import com.example.ui.theme.Brand600
import com.example.ui.theme.Brand700
import com.example.ui.theme.BrandDarkAccent
import com.example.ui.theme.Emerald300
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
fun DailyTasksScreen(
    state: WorkedDashboardState,
    onBack: () -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    var taskText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        WorkedTopBar(title = "Daily Tasks", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = taskText,
                        onValueChange = { taskText = it },
                        placeholder = { Text("Add an office task...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("daily_task_input")
                    )
                    Button(
                        onClick = {
                            if (taskText.isNotBlank()) {
                                onAddTask(taskText)
                                taskText = ""
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White),
                        modifier = Modifier.height(54.dp).testTag("add_task_button")
                    ) {
                        Text("Add")
                    }
                }
            }

            if (state.tasks.isEmpty()) {
                item {
                    WorkedCard {
                        Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            Text("No tasks yet. Add one above!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(state.tasks, key = { it.id }) { task ->
                    WorkedCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTask(task.id, task.done) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (task.done) Brand600 else Color.Transparent)
                                        .border(2.dp, if (task.done) Brand600 else MaterialTheme.colorScheme.outline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (task.done) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(
                                    text = task.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { onDeleteTask(task.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Task", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColleaguesScreen(
    state: WorkedDashboardState,
    onBack: () -> Unit,
    onCopiedToast: (String) -> Unit,
    onSaveColleague: (editId: String?, name: String, role: String, theme: String, photo: String, bgPhoto: String, info: List<InfoField>) -> Unit,
    onDeleteColleague: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingColleague by remember { mutableStateOf<ColleagueRecord?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filtered = remember(state.colleagues, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) state.colleagues
        else state.colleagues.filter { c ->
            (c.name + " " + c.role).lowercase().contains(q) ||
                c.infoFields.any { (it.label + " " + it.value).lowercase().contains(q) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WorkedTopBar(
            title = "Colleagues",
            onBack = onBack,
            trailing = {
                Button(
                    onClick = { editingColleague = null; showDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_colleague_button")
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Search colleague name, designation, or info...") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filtered.isEmpty()) {
                item {
                    WorkedCard {
                        Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            Text("No colleagues found. Tap + Add to create a card.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { col ->
                    val bannerBrush = when (col.theme) {
                        "mint" -> Brush.linearGradient(listOf(Color(0xFFA7F3D0), Color(0xFFCCFBF1), Color(0xFFF1F5F9)))
                        "sunset" -> Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFFFEDD5), Color(0xFFFFE4E6)))
                        "lavender" -> Brush.linearGradient(listOf(Color(0xFFDDD6FE), Color(0xFFF3E8FF), Color(0xFFF1F5F9)))
                        else -> Brush.linearGradient(listOf(Color(0xFFBAE6FD), Color(0xFFDBEAFE), Color(0xFFF1F5F9)))
                    }
                    val avatarBrush = when (col.theme) {
                        "mint" -> Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF0F766E)))
                        "sunset" -> Brush.linearGradient(listOf(Accent500, Accent600))
                        "lavender" -> Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF4338CA)))
                        else -> Brush.linearGradient(listOf(Brand600, Brand700))
                    }

                    WorkedCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Top Cover Banner + Edit Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(108.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            ) {
                                DataUrlImage(
                                    dataUrl = col.bgPhotoDataUrl,
                                    contentDescription = "Cover Banner",
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().background(bannerBrush))
                                }
                                IconButton(
                                    onClick = { editingColleague = col; showDialog = true },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Colleague", modifier = Modifier.size(16.dp))
                                }
                            }

                            // Overlapping Circular Avatar
                            Box(
                                modifier = Modifier
                                    .offset(y = (-36).dp)
                                    .padding(start = 12.dp)
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF2DD4BF), Color(0xFFFBBF24), Color(0xFFFF7A3D))))
                                    .padding(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DataUrlImage(
                                        dataUrl = col.photoDataUrl,
                                        contentDescription = col.name,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(avatarBrush),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = WorkedFormatters.initialsFor(col.name),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Name & Role
                            Column(
                                modifier = Modifier
                                    .offset(y = (-24).dp)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(col.name, style = MaterialTheme.typography.titleLarge)
                                if (col.role.isNotBlank()) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(col.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Auto-adjust Info Grid
                            Box(modifier = Modifier.offset(y = (-12).dp).padding(horizontal = 4.dp)) {
                                AutoAdjustInfoGrid(
                                    infoFields = col.infoFields,
                                    onDarkPortraitCard = false,
                                    onCopied = onCopiedToast
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ColleagueEditorDialog(
            existing = editingColleague,
            onDismiss = { showDialog = false; editingColleague = null },
            onSave = { name, role, theme, photo, bgPhoto, info ->
                onSaveColleague(editingColleague?.id, name, role, theme, photo, bgPhoto, info)
                showDialog = false
                editingColleague = null
            },
            onDelete = { id ->
                onDeleteColleague(id)
                showDialog = false
                editingColleague = null
            }
        )
    }
}

@Composable
private fun ColleagueEditorDialog(
    existing: ColleagueRecord?,
    onDismiss: () -> Unit,
    onSave: (name: String, role: String, theme: String, photo: String, bgPhoto: String, info: List<InfoField>) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var role by remember { mutableStateOf(existing?.role.orEmpty()) }
    var theme by remember { mutableStateOf(existing?.theme ?: "sky") }
    var photo by remember { mutableStateOf(existing?.photoDataUrl.orEmpty()) }
    var bgPhoto by remember { mutableStateOf(existing?.bgPhotoDataUrl.orEmpty()) }
    val fields = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            val init = existing?.infoFields?.takeIf { it.isNotEmpty() }
                ?: listOf(InfoField("Mobile", ""), InfoField("ID", ""))
            init.forEach { add(it.label to it.value) }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { compressUriToDataUrl(context, it, 280)?.let { url -> photo = url } }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { compressUriToDataUrl(context, it, 420)?.let { url -> bgPhoto = url } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Colleague" else "Add Colleague", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (photo.isBlank()) "Profile Pic" else "Change Pic", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (bgPhoto.isBlank()) "Cover Pic" else "Change Cover", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Designation / Short Bio") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("sky", "mint", "sunset", "lavender").forEach { t ->
                        val sel = theme == t
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (sel) Brand600 else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { theme = t }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(t.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Custom Info Fields", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { if (fields.size < 15) fields.add("" to "") }) {
                        Text("+ Add Field")
                    }
                }
                fields.forEachIndexed { idx, pair ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pair.first,
                            onValueChange = { if (idx in fields.indices) fields[idx] = it to pair.second },
                            placeholder = { Text("Label") },
                            singleLine = true,
                            modifier = Modifier.weight(0.4f)
                        )
                        OutlinedTextField(
                            value = pair.second,
                            onValueChange = { if (idx in fields.indices) fields[idx] = pair.first to it },
                            placeholder = { Text("Value") },
                            singleLine = true,
                            modifier = Modifier.weight(0.55f)
                        )
                        IconButton(
                            onClick = { if (idx in fields.indices) fields.removeAt(idx) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mapped = fields.map { InfoField(it.first, it.second) }
                    onSave(name, role, theme, photo, bgPhoto, mapped)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White)
            ) {
                Text(if (existing != null) "Save Changes" else "Add")
            }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing.id) }) { Text("Delete", color = Rose600) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun MyProfileScreen(
    state: WorkedDashboardState,
    onBack: () -> Unit,
    onCopiedToast: (String) -> Unit,
    onUpdateQuickPhoto: (String) -> Unit,
    onSaveProfile: (name: String, bio: String, photo: String, info: List<InfoField>) -> Unit
) {
    val context = LocalContext.current
    val p = state.profile
    var showEditDialog by remember { mutableStateOf(false) }

    val quickPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { compressUriToDataUrl(context, it, 360)?.let { url -> onUpdateQuickPhoto(url) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        WorkedTopBar(
            title = "My Profile",
            onBack = onBack,
            darkSurface = true,
            trailing = {
                IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Profile", tint = Color(0xFFD4D4D8))
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF0A0A0A),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Large Rounded Portrait
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(276.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF1E1E1E))
                        ) {
                            DataUrlImage(
                                dataUrl = p.photoDataUrl,
                                contentDescription = "Profile Portrait",
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(listOf(Brand700, Brand500, Color(0xFF1F2937)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = WorkedFormatters.initialsFor(p.displayName),
                                            style = MaterialTheme.typography.displaySmall,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "Tap camera to add your photo",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFE0F7FA).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { quickPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = "Upload Photo", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Name + Verified Badge + Bio
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(p.displayName.ifBlank { "Admin" }, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                                Icon(Icons.Rounded.Verified, contentDescription = "Verified", tint = BrandDarkAccent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = p.bio.ifBlank { "Tap Edit Info to add your designation & details." },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA1A1AA)
                            )
                        }

                        // Auto-Adjust Info Grid
                        Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                            AutoAdjustInfoGrid(
                                infoFields = p.infoFields,
                                onDarkPortraitCard = true,
                                onCopied = onCopiedToast
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                                    Text("${state.attendance.size}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA1A1AA))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Groups, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                                    Text("${state.colleagues.size}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA1A1AA))
                                }
                            }
                            Button(
                                onClick = { showEditDialog = true },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Edit Info", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        MyProfileEditorDialog(
            initialName = p.displayName,
            initialBio = p.bio,
            initialPhoto = p.photoDataUrl,
            initialInfo = p.infoFields,
            onDismiss = { showEditDialog = false },
            onSave = { name, bio, photo, info ->
                onSaveProfile(name, bio, photo, info)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun MyProfileEditorDialog(
    initialName: String,
    initialBio: String,
    initialPhoto: String,
    initialInfo: List<InfoField>,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, photo: String, info: List<InfoField>) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var bio by remember { mutableStateOf(initialBio) }
    var photo by remember { mutableStateOf(initialPhoto) }
    val fields = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            initialInfo.forEach { add(it.label to it.value) }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { compressUriToDataUrl(context, it, 360)?.let { url -> photo = url } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit My Profile", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Change Photo")
                    }
                    if (photo.isNotBlank()) {
                        TextButton(onClick = { photo = "" }) { Text("Remove", color = Rose600) }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Designation / Bio") },
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Info", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { if (fields.size < 15) fields.add("" to "") }) { Text("+ Add Field") }
                }
                fields.forEachIndexed { idx, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = pair.first,
                            onValueChange = { if (idx in fields.indices) fields[idx] = it to pair.second },
                            placeholder = { Text("Label") },
                            singleLine = true,
                            modifier = Modifier.weight(0.4f)
                        )
                        OutlinedTextField(
                            value = pair.second,
                            onValueChange = { if (idx in fields.indices) fields[idx] = pair.first to it },
                            placeholder = { Text("Value") },
                            singleLine = true,
                            modifier = Modifier.weight(0.55f)
                        )
                        IconButton(
                            onClick = { if (idx in fields.indices) fields.removeAt(idx) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, bio, photo, fields.map { InfoField(it.first, it.second) }) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White)
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminSettingsScreen(
    state: WorkedDashboardState,
    userEmail: String?,
    isDark: Boolean,
    onNavigate: (AppScreen) -> Unit,
    onToggleDarkMode: () -> Unit,
    onSavePreferences: (displayName: String, weeklyOff: Long, shiftStart: String) -> Unit,
    onExportJson: () -> String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember(state.profile.displayName) { mutableStateOf(state.profile.displayName) }
    var weeklyOff by remember(state.profile.weeklyOff) { mutableStateOf(state.profile.weeklyOff) }
    var offMenuOpen by remember { mutableStateOf(false) }
    var shiftStart by remember(state.profile.shiftStart) { mutableStateOf(state.profile.shiftStart.ifBlank { "09:30" }) }

    Column(modifier = Modifier.fillMaxSize()) {
        WorkedTopBar(title = "Admin Settings")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Dark Mode Toggle Card
            item {
                WorkedCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) TintMintDark else TintSkyLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = if (isDark) Emerald300 else Slate800, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Dark Mode", style = MaterialTheme.typography.titleSmall)
                                Text("AMOLED black low-light appearance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { onToggleDarkMode() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Brand600),
                            modifier = Modifier.testTag("settings_dark_mode_switch")
                        )
                    }
                }
            }

            // Office Schedule Preferences
            item {
                WorkedCard {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("OFFICE SCHEDULE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { offMenuOpen = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(54.dp)
                                ) {
                                    Text(
                                        WorkedFormatters.fullDayNames.getOrElse(weeklyOff.toInt()) { "Sunday" },
                                        maxLines = 1
                                    )
                                }
                                DropdownMenu(expanded = offMenuOpen, onDismissRequest = { offMenuOpen = false }) {
                                    listOf(0L to "Sunday", 6L to "Saturday", 5L to "Friday").forEach { (v, lbl) ->
                                        DropdownMenuItem(text = { Text(lbl) }, onClick = { weeklyOff = v; offMenuOpen = false })
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = shiftStart,
                                onValueChange = { if (it.length <= 5) shiftStart = it },
                                label = { Text("Office Time") },
                                isError = shiftStart.isNotBlank() && WorkedFormatters.normalizeTime24(shiftStart) == null,
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = { onSavePreferences(nameInput, weeklyOff, shiftStart) },
                            enabled = nameInput.isNotBlank() && WorkedFormatters.normalizeTime24(shiftStart) != null,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Brand600, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Save Preferences")
                        }
                    }
                }
            }

            // Office & Directory Links
            item {
                Text(
                    "Office & Directory",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                WorkedCard {
                    Column {
                        GroupedActionRow(
                            icon = Icons.Rounded.AccountCircle,
                            iconTintBg = if (isDark) TintMintDark else TintMintLight,
                            iconTintFg = if (isDark) Emerald300 else Slate800,
                            label = "My Profile Card",
                            onClick = { onNavigate(AppScreen.PROFILE) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GroupedActionRow(
                            icon = Icons.Rounded.Groups,
                            iconTintBg = if (isDark) TintSkyDark else TintSkyLight,
                            iconTintFg = if (isDark) Color(0xFF7DD3FC) else Slate800,
                            label = "Colleagues Directory",
                            onClick = { onNavigate(AppScreen.COLLEAGUES) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GroupedActionRow(
                            icon = Icons.Rounded.EventNote,
                            iconTintBg = if (isDark) TintAmberDark else TintAmberLight,
                            iconTintFg = if (isDark) Amber200 else Slate800,
                            label = "Key Dates",
                            onClick = { onNavigate(AppScreen.REMINDERS) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GroupedActionRow(
                            icon = Icons.Rounded.Layers,
                            iconTintBg = if (isDark) TintLavenderDark else TintLavenderLight,
                            iconTintFg = if (isDark) Color(0xFFD8B4FE) else Slate800,
                            label = "Activity Ledger",
                            onClick = { onNavigate(AppScreen.RECORDS) }
                        )
                    }
                }
            }

            // Data, Cloud Account & Backup
            item {
                Text(
                    "Cloud Account & Backup",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                WorkedCard {
                    Column {
                        GroupedActionRow(
                            icon = Icons.Rounded.FileDownload,
                            iconTintBg = if (isDark) TintMintDark else TintMintLight,
                            iconTintFg = if (isDark) Emerald300 else Slate800,
                            label = "Export Backup (JSON)",
                            onClick = {
                                runCatching {
                                    val json = onExportJson()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, "Work'ed Backup ${WorkedFormatters.todayDateKey()}")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export Work'ed JSON Backup"))
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GroupedActionRow(
                            icon = Icons.AutoMirrored.Rounded.Logout,
                            iconTintBg = if (isDark) TintRoseDark else TintRoseLight,
                            iconTintFg = Rose600,
                            label = "Sign Out (${userEmail ?: "Google"})",
                            labelColor = Rose600,
                            onClick = onSignOut
                        )
                    }
                }
            }
        }
    }
}

