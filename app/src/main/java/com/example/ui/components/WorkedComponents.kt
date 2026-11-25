package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatuses
import com.example.data.InfoField
import com.example.ui.theme.Brand600
import com.example.ui.theme.BrandDarkAccent
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusEarlyOut
import com.example.ui.theme.StatusHoliday
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusLeave
import com.example.ui.theme.StatusMissPunch
import com.example.ui.theme.StatusOffday
import com.example.ui.theme.StatusOutWork
import com.example.ui.theme.StatusPresent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.R
import java.io.ByteArrayOutputStream

@Composable
fun WorkedMinimalLogo(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    containerColor: Color = Color.White.copy(alpha = 0.16f)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_worked_logo),
            contentDescription = "Work'ed logo",
            modifier = Modifier.size(size * 0.68f)
        )
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

fun compressUriToDataUrl(context: Context, uri: Uri, maxDim: Int = 360): String? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= maxDim && bounds.outHeight / (sampleSize * 2) >= maxDim) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return@runCatching null

        val scale = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height, 1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                raw,
                (raw.width * scale).toInt().coerceAtLeast(1),
                (raw.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== raw) raw.recycle() }
        } else raw

        val maxBase64Len = 190_000
        var quality = 74
        var result: String? = null
        while (quality >= 30) {
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val dataUrl = "data:image/jpeg;base64,${Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)}"
            if (dataUrl.length <= maxBase64Len) {
                result = dataUrl
                break
            }
            quality -= 12
        }
        scaled.recycle()
        result
    }.getOrNull()
}

fun decodeDataUrlBitmap(dataUrl: String): ImageBitmap? {
    if (dataUrl.isBlank()) return null
    return runCatching {
        val pureB64 = dataUrl.substringAfter("base64,", dataUrl)
        val bytes = Base64.decode(pureB64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

@Composable
fun DataUrlImage(
    dataUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit
) {
    val bitmap = remember(dataUrl) { decodeDataUrlBitmap(dataUrl) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        fallback()
    }
}

@Composable
fun WorkedTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    darkSurface: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val bgColor = if (darkSurface) Color.Black else MaterialTheme.colorScheme.surface
    val fgColor = if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurface
    Surface(
        color = bgColor,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = fgColor
                        )
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = fgColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                if (trailing != null) {
                    trailing()
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            }
            HorizontalDivider(
                color = if (darkSurface) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun WorkedCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
fun GroupedActionRow(
    icon: ImageVector,
    iconTintBg: Color,
    iconTintFg: Color,
    label: String,
    trailingText: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    .background(iconTintBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintFg,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!trailingText.isNullOrBlank()) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun semanticInfoIcon(label: String): ImageVector {
    val l = label.lowercase()
    return when {
        Regex("(phone|mobile|mob|tel|call|contact|whatsapp|number)").containsMatchIn(l) -> Icons.Rounded.Call
        Regex("(^id$|emp|employee|badge|code|roll|reg|gpf|pran|pan)").containsMatchIn(l) -> Icons.Rounded.Badge
        Regex("(email|mail)").containsMatchIn(l) -> Icons.Rounded.Email
        Regex("(room|desk|cabin|office|block|dept|department|branch|school)").containsMatchIn(l) -> Icons.Rounded.AccountBalance
        Regex("(time|shift|hour|timing)").containsMatchIn(l) -> Icons.Rounded.Schedule
        Regex("(address|loc|location|place|city|post)").containsMatchIn(l) -> Icons.Rounded.LocationOn
        Regex("(note|remark|info|bio|about)").containsMatchIn(l) -> Icons.Rounded.Description
        Regex("(blood|med|health)").containsMatchIn(l) -> Icons.Rounded.FavoriteBorder
        Regex("(date|join|dob|bday)").containsMatchIn(l) -> Icons.Rounded.CalendarMonth
        Regex("(rank|grade|pay|scale|designation)").containsMatchIn(l) -> Icons.Rounded.WorkspacePremium
        else -> Icons.Rounded.Tag
    }
}

@Composable
fun AutoAdjustInfoGrid(
    infoFields: List<InfoField>,
    onDarkPortraitCard: Boolean,
    onCopied: (String) -> Unit
) {
    val context = LocalContext.current
    val items = infoFields.filter { it.label.isNotBlank() || it.value.isNotBlank() }
    if (items.isEmpty()) return

    val shortItems = items.filter { it.value.length <= 16 && it.label.length <= 14 }
    val longItems = items.filterNot { it.value.length <= 16 && it.label.length <= 14 }

    val boxBg = if (onDarkPortraitCard) Color.White.copy(alpha = 0.06f) else MaterialTheme.colorScheme.background
    val boxBorder = if (onDarkPortraitCard) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant
    val valColor = if (onDarkPortraitCard) Color.White else MaterialTheme.colorScheme.onSurface
    val iconColor = if (onDarkPortraitCard) Color(0xFFA1A1AA) else MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (shortItems.isNotEmpty()) {
            val chunks = shortItems.chunked(3)
            chunks.forEach { rowItems ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = boxBg,
                    border = BorderStroke(1.dp, boxBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEachIndexed { idx, field ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        copyToClipboard(context, field.label, field.value)
                                        onCopied("Copied ${field.label.ifBlank { "info" }}!")
                                    }
                                    .padding(vertical = 11.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = semanticInfoIcon(field.label),
                                    contentDescription = field.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = field.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = valColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (idx < rowItems.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(48.dp)
                                        .align(Alignment.CenterVertically)
                                        .background(boxBorder)
                                )
                            }
                        }
                    }
                }
            }
        }

        longItems.forEach { field ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = boxBg,
                border = BorderStroke(1.dp, boxBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        copyToClipboard(context, field.label, field.value)
                        onCopied("Copied ${field.label.ifBlank { "info" }}!")
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = semanticInfoIcon(field.label),
                        contentDescription = field.label,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = field.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (onDarkPortraitCard) Color(0xFFE4E4E7) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

fun statusColors(status: String): Pair<Color, Color> = when (status) {
    AttendanceStatuses.PRESENT -> StatusPresent to Color.White
    AttendanceStatuses.LEAVE -> StatusLeave to Color.White
    AttendanceStatuses.ABSENT -> StatusAbsent to Color.White
    AttendanceStatuses.HOLIDAY -> StatusHoliday to Slate900
    AttendanceStatuses.OFFDAY -> StatusOffday to Color(0xFF475569)
    AttendanceStatuses.OUTSTATION -> StatusOutWork to Slate900
    AttendanceStatuses.LATE -> StatusLate to Color.White
    AttendanceStatuses.EARLY_GOING -> StatusEarlyOut to Color.White
    AttendanceStatuses.MISS_PUNCH -> StatusMissPunch to Color.White
    else -> Brand600 to Color.White
}

@Composable
fun brandAccentColor(isDark: Boolean): Color = if (isDark) BrandDarkAccent else Brand600
