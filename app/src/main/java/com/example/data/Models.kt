package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppScreen {
    HOME,
    ATTENDANCE,
    OUT_ON_WORK,
    RECORDS,
    EXPENSES,
    FINANCE,
    REMINDERS,
    DAILY_TASKS,
    COLLEAGUES,
    PROFILE,
    MORE
}

@Keep
@IgnoreExtraProperties
data class InfoField(
    val label: String = "",
    val value: String = ""
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Admin",
    val bio: String = "",
    val photoDataUrl: String = "",
    val weeklyOff: Long = 0L,
    val shiftStart: String = "09:30",
    val darkMode: Boolean = false,
    val infoFields: List<InfoField> = emptyList(),
    val sessionDate: String = "",
    val punchedIn: Boolean = false,
    val sessionInTime: String = "",
    val sessionOutTime: String = "",
    val onBreak: Boolean = false,
    val breakStartMs: Long = 0L,
    val totalBreakMins: Long = 0L,
    val githubConfigUrl: String = "",
    val githubConfigVersion: String = "1.0.0",
    val sampleDataSeeded: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val dateKey: String = "",
    val status: String = "Present",
    val leaveType: String = "",
    val inTime: String = "",
    val outTime: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OutOnWorkLog(
    @DocumentId val id: String = "",
    val date: String = "",
    val mode: String = "Full Day",
    val startTime: String = "10:00",
    val endTime: String = "16:00",
    val meetingWith: String = "",
    val location: String = "",
    val remarks: String = "",
    val attachmentName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExpenseRecord(
    @DocumentId val id: String = "",
    val date: String = "",
    val title: String = "",
    val category: String = "Office",
    val amount: Long = 0L,
    val owner: String = "headmaster",
    val recovered: Boolean = false,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FinanceRecord(
    @DocumentId val id: String = "",
    val date: String = "",
    val title: String = "",
    val type: String = "income",
    val amount: Long = 0L,
    val status: String = "received",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class KeyDateRecord(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "Document",
    val validFrom: String = "",
    val validUntil: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyTask(
    @DocumentId val id: String = "",
    val text: String = "",
    val done: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ColleagueRecord(
    @DocumentId val id: String = "",
    val name: String = "",
    val role: String = "",
    val theme: String = "sky",
    val photoDataUrl: String = "",
    val bgPhotoDataUrl: String = "",
    val infoFields: List<InfoField> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object WorkedDefaults {
    val expenseCategories: List<String> = listOf("Office", "Travel", "Food", "Printing", "Other")
    val leaveTypes: List<String> = listOf("Casual Leave", "Earned Leave", "Commuted Leave")
    val reminderCategories: List<String> = listOf("Document", "Compliance", "Contract", "License", "Other")
}

object AttendanceStatuses {
    const val PRESENT = "Present"
    const val LEAVE = "Leave"
    const val ABSENT = "Absent"
    const val HOLIDAY = "Holiday"
    const val OFFDAY = "Offday"
    const val OUTSTATION = "Outstation"
    const val LATE = "Late"
    const val EARLY_GOING = "Early_Going"
    const val MISS_PUNCH = "MissPunch"

    val allLabels: List<Pair<String, String>> = listOf(
        PRESENT to "Present",
        LEAVE to "Leave",
        ABSENT to "Absent",
        HOLIDAY to "Holiday",
        OFFDAY to "Offday",
        OUTSTATION to "Out Work",
        LATE to "Late",
        EARLY_GOING to "Early Out",
        MISS_PUNCH to "MissPunch"
    )
}

object WorkedFormatters {
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val fullDayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun todayDateKey(): String {
        val cal = Calendar.getInstance()
        return toDateKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }

    fun toDateKey(year: Int, monthZeroBased: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, day)
    }

    fun currentTime24(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    fun formatTime12h(time24: String): String {
        if (!time24.contains(":")) return "--:--"
        val parts = time24.split(":")
        val hRaw = parts.getOrNull(0)?.toIntOrNull() ?: return "--:--"
        val mStr = parts.getOrNull(1)?.padStart(2, '0') ?: "00"
        val suffix = if (hRaw >= 12) "PM" else "AM"
        val h12 = if (hRaw % 12 == 0) 12 else hRaw % 12
        return String.format(Locale.US, "%02d:%s %s", h12, mStr, suffix)
    }

    fun formatPrettyDate(dateKey: String): String {
        val parts = dateKey.split("-")
        if (parts.size != 3) return dateKey
        val yr = parts[0]
        val moIdx = (parts[1].toIntOrNull() ?: 1) - 1
        val da = parts[2]
        val moName = monthNames.getOrElse(moIdx) { parts[1] }
        return "$da $moName $yr"
    }

    fun formatTodayHeader(): String {
        val cal = Calendar.getInstance()
        val dow = dayNames.getOrElse(cal.get(Calendar.DAY_OF_WEEK) - 1) { "Mon" }
        return "$dow, ${formatPrettyDate(todayDateKey())}"
    }

    fun formatInr(amount: Long): String {
        val nf = NumberFormat.getNumberInstance(Locale("en", "IN"))
        return "₹${nf.format(amount)}"
    }

    private val dateKeyRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    private val time24Regex = Regex("^([0-9]{1,2}):([0-9]{2})$")

    private fun strictDateFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    fun isValidDateKey(dateKey: String): Boolean {
        val trimmed = dateKey.trim()
        if (!dateKeyRegex.matches(trimmed)) return false
        return runCatching { strictDateFormat().parse(trimmed) != null }.getOrDefault(false)
    }

    fun normalizeTime24(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val match = time24Regex.matchEntire(trimmed) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun time24ToMinutes(time24: String): Int? {
        val normalized = normalizeTime24(time24)?.takeIf { it.isNotBlank() } ?: return null
        val parts = normalized.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return h * 60 + m
    }

    fun shiftDateKey(dateKey: String, deltaDays: Int): String {
        val sdf = strictDateFormat()
        val parsed = runCatching { sdf.parse(dateKey.trim()) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_MONTH, deltaDays)
        }
        return toDateKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }

    fun daysUntil(targetDateKey: String): Long {
        val sdf = strictDateFormat()
        val target = runCatching { sdf.parse(targetDateKey.trim()) }.getOrNull() ?: return 9999L
        val today = runCatching { sdf.parse(todayDateKey()) }.getOrNull() ?: Date()
        return TimeUnit.MILLISECONDS.toDays(target.time - today.time)
    }

    fun validityProgress(validFrom: String, validUntil: String): Float {
        val sdf = strictDateFormat()
        val start = runCatching { sdf.parse(validFrom.trim())?.time }.getOrNull() ?: return 1f
        val end = runCatching { sdf.parse(validUntil.trim())?.time }.getOrNull() ?: return 1f
        val now = runCatching { sdf.parse(todayDateKey())?.time }.getOrNull() ?: return 1f
        if (end <= start) return 1f
        return ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
    }

    fun enumerateDateRange(fromKey: String, toKey: String, maxDays: Int = 60): List<String> {
        if (!isValidDateKey(fromKey) || !isValidDateKey(toKey)) return emptyList()
        val sdf = strictDateFormat()
        val start = runCatching { sdf.parse(fromKey.trim()) }.getOrNull() ?: return emptyList()
        val end = runCatching { sdf.parse(toKey.trim()) }.getOrNull() ?: return emptyList()
        if (end.before(start)) return emptyList()
        val result = mutableListOf<String>()
        val cal = Calendar.getInstance().apply { time = start }
        while (!cal.time.after(end) && result.size < maxDays) {
            result.add(toDateKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }

    fun initialsFor(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifEmpty { "A" }
    }
}
