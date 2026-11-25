package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatuses
import com.example.data.ColleagueRecord
import com.example.data.DailyTask
import com.example.data.ExpenseRecord
import com.example.data.FinanceRecord
import com.example.data.InfoField
import com.example.data.KeyDateRecord
import com.example.data.OutOnWorkLog
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.WorkedFormatters
import com.example.data.WorkedRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class WorkedDashboardState(
    val profile: UserProfile = UserProfile(),
    val attendance: Map<String, AttendanceRecord> = emptyMap(),
    val outOnWorkLogs: List<OutOnWorkLog> = emptyList(),
    val expenses: List<ExpenseRecord> = emptyList(),
    val finances: List<FinanceRecord> = emptyList(),
    val reminders: List<KeyDateRecord> = emptyList(),
    val tasks: List<DailyTask> = emptyList(),
    val colleagues: List<ColleagueRecord> = emptyList()
)

class WorkedViewModel(
    private val repository: WorkedRepository,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _calendarYearMonth = MutableStateFlow(
        Calendar.getInstance().let { it.get(Calendar.YEAR) to it.get(Calendar.MONTH) }
    )
    val calendarYearMonth: StateFlow<Pair<Int, Int>> = _calendarYearMonth.asStateFlow()

    // Instant optimistic Dark Mode state so UI toggles smoothly without Firestore latency flicker
    private val _optimisticDarkMode = MutableStateFlow<Boolean?>(null)

    // In-flight guards to prevent double-tap race conditions on Punch In / Break
    private val _isPunchBusy = MutableStateFlow(false)
    val isPunchBusy: StateFlow<Boolean> = _isPunchBusy.asStateFlow()

    private val _isBreakBusy = MutableStateFlow(false)
    val isBreakBusy: StateFlow<Boolean> = _isBreakBusy.asStateFlow()

    private fun <T> Flow<T>.toSafeUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val profileFlow = repository.observeProfile(currentUserId)
    private val attendanceFlow = repository.observeAttendance(currentUserId)
    private val outOnWorkFlow = repository.observeOutOnWork(currentUserId)
    private val expensesFlow = repository.observeExpenses(currentUserId)
    private val financesFlow = repository.observeFinances(currentUserId)
    private val remindersFlow = repository.observeReminders(currentUserId)
    private val tasksFlow = repository.observeTasks(currentUserId)
    private val colleaguesFlow = repository.observeColleagues(currentUserId)

    private val corePartFlow = combine(
        profileFlow,
        attendanceFlow,
        outOnWorkFlow,
        expensesFlow,
        _optimisticDarkMode
    ) { profile, attendance, oow, expenses, darkOverride ->
        val baseProfile = profile ?: UserProfile(userId = currentUserId, displayName = initialDisplayName)
        val mergedProfile = if (darkOverride != null) baseProfile.copy(darkMode = darkOverride) else baseProfile
        CorePart(mergedProfile, attendance, oow, expenses)
    }

    private val secondaryPartFlow = combine(
        financesFlow,
        remindersFlow,
        tasksFlow,
        colleaguesFlow
    ) { finances, reminders, tasks, colleagues ->
        SecondaryPart(finances, reminders, tasks, colleagues)
    }

    val dashboardState: StateFlow<UiState<WorkedDashboardState>> = combine(
        corePartFlow,
        secondaryPartFlow
    ) { core, sec ->
        val attendanceMap = LinkedHashMap<String, AttendanceRecord>()
        core.attendance.forEach { rec ->
            val key = rec.dateKey.ifBlank { rec.id }
            if (key.isNotBlank()) attendanceMap[key] = rec
        }
        WorkedDashboardState(
            profile = core.profile,
            attendance = attendanceMap,
            outOnWorkLogs = core.outOnWork,
            expenses = core.expenses,
            finances = sec.finances,
            reminders = sec.reminders,
            tasks = sec.tasks,
            colleagues = sec.colleagues
        )
    }.toSafeUiState("dashboard")

    init {
        viewModelScope.launch {
            val res = repository.ensureProfileInitialized(initialDisplayName)
            if (res.isFailure) {
                emitToast("Cloud sync warning: ${res.exceptionOrNull()?.localizedMessage ?: "Retry later"}")
            }
        }
    }

    private fun emitToast(msg: String) {
        _toastEvents.tryEmit(msg)
    }

    private fun computeShiftStatus(inTime: String, shiftStart: String): String {
        val inMins = WorkedFormatters.time24ToMinutes(inTime)
        val shiftMins = WorkedFormatters.time24ToMinutes(shiftStart.ifBlank { "09:30" }) ?: (9 * 60 + 30)
        return if (inMins != null && inMins > shiftMins) {
            AttendanceStatuses.LATE
        } else {
            AttendanceStatuses.PRESENT
        }
    }

    fun changeCalendarMonth(delta: Int) {
        _calendarYearMonth.update { (year, month) ->
            var newMonth = month + delta
            var newYear = year
            if (newMonth < 0) {
                newMonth = 11
                newYear--
            } else if (newMonth > 11) {
                newMonth = 0
                newYear++
            }
            newYear to newMonth
        }
    }

    fun toggleDarkMode(currentDark: Boolean) {
        val targetDark = !currentDark
        _optimisticDarkMode.value = targetDark
        viewModelScope.launch {
            val res = repository.updateProfileFields(
                updates = mapOf("darkMode" to targetDark),
                fallbackDisplayName = initialDisplayName
            )
            if (res.isSuccess) {
                if (_optimisticDarkMode.value == targetDark) {
                    _optimisticDarkMode.value = null
                }
                emitToast(if (targetDark) "Dark mode enabled" else "Light mode enabled")
            } else {
                if (_optimisticDarkMode.value == targetDark) {
                    _optimisticDarkMode.value = null
                }
                emitToast("Could not update theme: ${res.exceptionOrNull()?.localizedMessage ?: "Try again"}")
            }
        }
    }

    fun handlePunchToggle(profile: UserProfile, todayAttendance: AttendanceRecord?) {
        if (_isPunchBusy.value) return
        _isPunchBusy.value = true
        viewModelScope.launch {
            try {
                val todayKey = WorkedFormatters.todayDateKey()
                val nowTime = WorkedFormatters.currentTime24()
                val isSameDay = profile.sessionDate == todayKey
                val currentlyPunchedIn = isSameDay && profile.punchedIn
                val existingIn = if (isSameDay && profile.sessionInTime.isNotBlank()) {
                    profile.sessionInTime
                } else {
                    todayAttendance?.inTime.orEmpty()
                }
                val currentBreakMins = if (isSameDay) profile.totalBreakMins.coerceIn(0L, 1440L) else 0L

                if (!currentlyPunchedIn) {
                    val effectiveIn = WorkedFormatters.normalizeTime24(existingIn)
                        ?.takeIf { it.isNotBlank() } ?: nowTime
                    val status = computeShiftStatus(effectiveIn, profile.shiftStart)

                    val res = repository.savePunchSessionAndAttendance(
                        profileUpdates = mapOf(
                            "sessionDate" to todayKey,
                            "punchedIn" to true,
                            "sessionInTime" to effectiveIn,
                            "sessionOutTime" to "",
                            "onBreak" to false,
                            "breakStartMs" to 0L,
                            "totalBreakMins" to currentBreakMins
                        ),
                        fallbackDisplayName = initialDisplayName,
                        dateKey = todayKey,
                        status = status,
                        leaveType = "",
                        inTime = effectiveIn,
                        outTime = "",
                        note = todayAttendance?.note.orEmpty()
                    )
                    if (res.isSuccess) {
                        emitToast("Punched in at ${WorkedFormatters.formatTime12h(effectiveIn)}")
                    } else {
                        emitToast("Failed to punch in: ${res.exceptionOrNull()?.localizedMessage ?: "Try again"}")
                    }
                } else {
                    val effectiveIn = WorkedFormatters.normalizeTime24(existingIn)
                        ?.takeIf { it.isNotBlank() } ?: nowTime
                    var finalBreakMins = currentBreakMins
                    if (isSameDay && profile.onBreak && profile.breakStartMs > 0L) {
                        val elapsed = ((System.currentTimeMillis() - profile.breakStartMs) / 60000L).coerceAtLeast(1L)
                        finalBreakMins = (finalBreakMins + elapsed).coerceAtMost(1440L)
                    }

                    val workingStatuses = setOf(
                        AttendanceStatuses.PRESENT,
                        AttendanceStatuses.LATE,
                        AttendanceStatuses.EARLY_GOING,
                        AttendanceStatuses.OUTSTATION
                    )
                    val finalStatus = todayAttendance?.status?.takeIf { it in workingStatuses }
                        ?: computeShiftStatus(effectiveIn, profile.shiftStart)

                    val res = repository.savePunchSessionAndAttendance(
                        profileUpdates = mapOf(
                            "sessionDate" to todayKey,
                            "punchedIn" to false,
                            "sessionInTime" to effectiveIn,
                            "sessionOutTime" to nowTime,
                            "onBreak" to false,
                            "breakStartMs" to 0L,
                            "totalBreakMins" to finalBreakMins
                        ),
                        fallbackDisplayName = initialDisplayName,
                        dateKey = todayKey,
                        status = finalStatus,
                        leaveType = "",
                        inTime = effectiveIn,
                        outTime = nowTime,
                        note = todayAttendance?.note.orEmpty()
                    )
                    if (res.isSuccess) {
                        emitToast("Punched out at ${WorkedFormatters.formatTime12h(nowTime)}")
                    } else {
                        emitToast("Failed to punch out: ${res.exceptionOrNull()?.localizedMessage ?: "Try again"}")
                    }
                }
            } finally {
                _isPunchBusy.value = false
            }
        }
    }

    fun handleBreakToggle(profile: UserProfile) {
        if (_isBreakBusy.value) return
        val todayKey = WorkedFormatters.todayDateKey()
        val isSameDay = profile.sessionDate == todayKey
        val currentlyPunchedIn = isSameDay && profile.punchedIn
        val currentlyOnBreak = isSameDay && profile.onBreak

        if (!currentlyOnBreak && !currentlyPunchedIn) {
            emitToast("Punch in first to start a break")
            return
        }

        _isBreakBusy.value = true
        viewModelScope.launch {
            try {
                val currentBreakMins = if (isSameDay) profile.totalBreakMins.coerceIn(0L, 1440L) else 0L
                if (!currentlyOnBreak) {
                    val res = repository.updateProfileFields(
                        updates = mapOf(
                            "sessionDate" to todayKey,
                            "onBreak" to true,
                            "breakStartMs" to System.currentTimeMillis(),
                            "totalBreakMins" to currentBreakMins
                        ),
                        fallbackDisplayName = initialDisplayName
                    )
                    if (res.isSuccess) emitToast("Break started")
                    else emitToast("Could not start break")
                } else {
                    val startMs = profile.breakStartMs.takeIf { it > 0L } ?: System.currentTimeMillis()
                    val elapsed = ((System.currentTimeMillis() - startMs) / 60000L).coerceAtLeast(1L)
                    val total = (currentBreakMins + elapsed).coerceAtMost(1440L)
                    val res = repository.updateProfileFields(
                        updates = mapOf(
                            "sessionDate" to todayKey,
                            "onBreak" to false,
                            "breakStartMs" to 0L,
                            "totalBreakMins" to total
                        ),
                        fallbackDisplayName = initialDisplayName
                    )
                    if (res.isSuccess) emitToast("Break ended (+${elapsed}m)")
                    else emitToast("Could not end break")
                }
            } finally {
                _isBreakBusy.value = false
            }
        }
    }

    fun saveMissedPunch(dateKey: String, inTime: String, outTime: String, note: String, shiftStart: String) {
        viewModelScope.launch {
            if (!WorkedFormatters.isValidDateKey(dateKey)) {
                emitToast("Enter a valid date (YYYY-MM-DD)")
                return@launch
            }
            val normIn = WorkedFormatters.normalizeTime24(inTime)
            val normOut = WorkedFormatters.normalizeTime24(outTime)
            if (normIn == null || normOut == null) {
                emitToast("Use 24-hour HH:mm format for times")
                return@launch
            }
            if (normIn.isBlank() && normOut.isBlank()) {
                emitToast("Enter at least an In Time")
                return@launch
            }
            val inMins = WorkedFormatters.time24ToMinutes(normIn)
            val outMins = WorkedFormatters.time24ToMinutes(normOut)
            if (inMins != null && outMins != null && outMins < inMins) {
                emitToast("Out time cannot be earlier than In time")
                return@launch
            }

            val status = computeShiftStatus(normIn, shiftStart)
            val todayKey = WorkedFormatters.todayDateKey()
            val res = if (dateKey.trim() == todayKey) {
                val stillPunchedIn = normIn.isNotBlank() && normOut.isBlank()
                repository.savePunchSessionAndAttendance(
                    profileUpdates = mapOf(
                        "sessionDate" to todayKey,
                        "punchedIn" to stillPunchedIn,
                        "sessionInTime" to normIn,
                        "sessionOutTime" to normOut,
                        "onBreak" to false,
                        "breakStartMs" to 0L
                    ),
                    fallbackDisplayName = initialDisplayName,
                    dateKey = todayKey,
                    status = status,
                    leaveType = "",
                    inTime = normIn,
                    outTime = normOut,
                    note = note
                )
            } else {
                repository.upsertAttendance(dateKey.trim(), status, "", normIn, normOut, note)
            }
            if (res.isSuccess) emitToast("Office time updated")
            else emitToast(res.exceptionOrNull()?.localizedMessage ?: "Could not update office time")
        }
    }

    fun saveQuickLeave(leaveType: String, fromKey: String, toKey: String, note: String) {
        viewModelScope.launch {
            if (!WorkedFormatters.isValidDateKey(fromKey) || !WorkedFormatters.isValidDateKey(toKey)) {
                emitToast("Enter valid dates (YYYY-MM-DD)")
                return@launch
            }
            val dates = WorkedFormatters.enumerateDateRange(fromKey, toKey)
            if (dates.isEmpty()) {
                emitToast("End date cannot be before start date")
                return@launch
            }
            var successCount = 0
            val todayKey = WorkedFormatters.todayDateKey()
            dates.forEach { d ->
                val res = if (d == todayKey) {
                    repository.savePunchSessionAndAttendance(
                        profileUpdates = mapOf(
                            "sessionDate" to todayKey,
                            "punchedIn" to false,
                            "sessionInTime" to "",
                            "sessionOutTime" to "",
                            "onBreak" to false,
                            "breakStartMs" to 0L
                        ),
                        fallbackDisplayName = initialDisplayName,
                        dateKey = todayKey,
                        status = AttendanceStatuses.LEAVE,
                        leaveType = leaveType,
                        inTime = "",
                        outTime = "",
                        note = note
                    )
                } else {
                    repository.upsertAttendance(d, AttendanceStatuses.LEAVE, leaveType, "", "", note)
                }
                if (res.isSuccess) successCount++
            }
            if (successCount > 0) {
                emitToast("$leaveType saved ($successCount day${if (successCount > 1) "s" else ""})")
            } else {
                emitToast("Could not save leave")
            }
        }
    }

    fun saveDayEditor(dateKey: String, status: String, leaveType: String, inTime: String, outTime: String, note: String) {
        viewModelScope.launch {
            val normIn = WorkedFormatters.normalizeTime24(inTime)
            val normOut = WorkedFormatters.normalizeTime24(outTime)
            if (normIn == null || normOut == null) {
                emitToast("Use 24-hour HH:mm format for times")
                return@launch
            }
            val todayKey = WorkedFormatters.todayDateKey()
            val effectiveLeaveType = if (status == AttendanceStatuses.LEAVE) leaveType else ""
            val res = if (dateKey.trim() == todayKey) {
                val workingStatuses = setOf(
                    AttendanceStatuses.PRESENT,
                    AttendanceStatuses.LATE,
                    AttendanceStatuses.EARLY_GOING,
                    AttendanceStatuses.OUTSTATION
                )
                val isWorking = status in workingStatuses
                val stillPunchedIn = isWorking && normIn.isNotBlank() && normOut.isBlank()
                repository.savePunchSessionAndAttendance(
                    profileUpdates = mapOf(
                        "sessionDate" to todayKey,
                        "punchedIn" to stillPunchedIn,
                        "sessionInTime" to if (isWorking) normIn else "",
                        "sessionOutTime" to if (isWorking) normOut else "",
                        "onBreak" to false,
                        "breakStartMs" to 0L
                    ),
                    fallbackDisplayName = initialDisplayName,
                    dateKey = todayKey,
                    status = status,
                    leaveType = effectiveLeaveType,
                    inTime = normIn,
                    outTime = normOut,
                    note = note
                )
            } else {
                repository.upsertAttendance(
                    dateKey = dateKey,
                    status = status,
                    leaveType = effectiveLeaveType,
                    inTime = normIn,
                    outTime = normOut,
                    note = note
                )
            }
            if (res.isSuccess) emitToast("Attendance updated")
            else emitToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to update day")
        }
    }

    fun clearDayRecord(dateKey: String) {
        viewModelScope.launch {
            val res = repository.deleteAttendance(dateKey)
            if (res.isSuccess) {
                if (dateKey.trim() == WorkedFormatters.todayDateKey()) {
                    repository.updateProfileFields(
                        updates = mapOf(
                            "sessionDate" to WorkedFormatters.todayDateKey(),
                            "punchedIn" to false,
                            "sessionInTime" to "",
                            "sessionOutTime" to "",
                            "onBreak" to false,
                            "breakStartMs" to 0L,
                            "totalBreakMins" to 0L
                        ),
                        fallbackDisplayName = initialDisplayName
                    )
                }
                emitToast("Day cleared")
            } else {
                emitToast("Failed to clear day")
            }
        }
    }

    fun saveOutOnWork(
        date: String,
        mode: String,
        startTime: String,
        endTime: String,
        meetingWith: String,
        location: String,
        remarks: String,
        attachmentName: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.addOutOnWork(date, mode, startTime, endTime, meetingWith, location, remarks, attachmentName)
            if (res.isSuccess) {
                emitToast("Out Work saved & marked on calendar")
                onSaved()
            } else {
                emitToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to save Out Work")
            }
        }
    }

    fun addExpense(date: String, title: String, category: String, amount: Long, owner: String, note: String) {
        viewModelScope.launch {
            val res = repository.addExpense(date, title, category, amount, owner, note)
            if (res.isSuccess) emitToast("Expense added")
            else emitToast(res.exceptionOrNull()?.localizedMessage ?: "Invalid expense details")
        }
    }

    fun toggleExpenseRecovered(id: String, currentRecovered: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleExpenseRecovered(id, !currentRecovered)
            if (res.isSuccess) emitToast(if (!currentRecovered) "Marked as recovered" else "Marked as due")
            else emitToast("Could not update expense status")
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            if (repository.deleteExpense(id).isSuccess) emitToast("Expense deleted")
            else emitToast("Could not delete expense")
        }
    }

    fun addFinance(date: String, title: String, amount: Long, status: String) {
        viewModelScope.launch {
            val res = repository.addFinance(date, title, amount, status)
            if (res.isSuccess) emitToast("Pay record saved")
            else emitToast(res.exceptionOrNull()?.localizedMessage ?: "Invalid pay details")
        }
    }

    fun toggleFinanceStatus(id: String, currentStatus: String) {
        viewModelScope.launch {
            val next = if (currentStatus == "received") "pending" else "received"
            if (repository.toggleFinanceStatus(id, next).isSuccess) emitToast("Marked as $next")
            else emitToast("Could not update pay status")
        }
    }

    fun deleteFinance(id: String) {
        viewModelScope.launch {
            if (repository.deleteFinance(id).isSuccess) emitToast("Entry deleted")
            else emitToast("Could not delete entry")
        }
    }

    fun saveReminder(editId: String?, title: String, category: String, validFrom: String, validUntil: String, note: String) {
        viewModelScope.launch {
            val res = repository.saveReminder(editId, title, category, validFrom, validUntil, note)
            if (res.isSuccess) emitToast("Key date saved")
            else emitToast(res.exceptionOrNull()?.localizedMessage ?: "Could not save key date")
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            if (repository.deleteReminder(id).isSuccess) emitToast("Key date removed")
            else emitToast("Could not remove key date")
        }
    }

    fun addTask(text: String) {
        viewModelScope.launch {
            val res = repository.addTask(text)
            if (res.isSuccess) emitToast("Task added")
            else emitToast(res.exceptionOrNull()?.localizedMessage ?: "Could not add task")
        }
    }

    fun toggleTask(id: String, done: Boolean) {
        viewModelScope.launch {
            if (repository.toggleTask(id, !done).isFailure) {
                emitToast("Could not update task")
            }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            if (repository.deleteTask(id).isSuccess) emitToast("Task deleted")
            else emitToast("Could not delete task")
        }
    }

    fun saveColleague(
        editId: String?,
        name: String,
        role: String,
        theme: String,
        photoDataUrl: String,
        bgPhotoDataUrl: String,
        infoFields: List<InfoField>
    ) {
        viewModelScope.launch {
            val res = repository.saveColleague(editId, name, role, theme, photoDataUrl, bgPhotoDataUrl, infoFields)
            if (res.isSuccess) emitToast("Colleague saved")
            else emitToast(res.exceptionOrNull()?.localizedMessage ?: "Could not save colleague")
        }
    }

    fun deleteColleague(id: String) {
        viewModelScope.launch {
            if (repository.deleteColleague(id).isSuccess) emitToast("Colleague removed")
            else emitToast("Could not remove colleague")
        }
    }

    fun saveMyProfile(displayName: String, bio: String, photoDataUrl: String, infoFields: List<InfoField>) {
        viewModelScope.launch {
            val safePhoto = if (photoDataUrl.length <= 200_000) photoDataUrl else ""
            val safeInfo = infoFields
                .filter { it.label.isNotBlank() || it.value.isNotBlank() }
                .take(20)
                .map {
                    mapOf(
                        "label" to it.label.trim().ifBlank { "Info" }.take(60),
                        "value" to it.value.trim().take(300)
                    )
                }
            val res = repository.updateProfileFields(
                updates = mapOf(
                    "displayName" to displayName.trim().ifBlank { "Admin" }.take(80),
                    "bio" to bio.trim().take(300),
                    "photoDataUrl" to safePhoto,
                    "infoFields" to safeInfo
                ),
                fallbackDisplayName = initialDisplayName
            )
            if (res.isSuccess) emitToast("Profile saved")
            else emitToast("Could not save profile")
        }
    }

    fun updateProfilePhoto(photoDataUrl: String) {
        viewModelScope.launch {
            if (photoDataUrl.length > 200_000) {
                emitToast("Photo is too large — try a smaller image")
                return@launch
            }
            val res = repository.updateProfileFields(
                updates = mapOf("photoDataUrl" to photoDataUrl),
                fallbackDisplayName = initialDisplayName
            )
            if (res.isSuccess) emitToast("Profile photo updated")
            else emitToast("Could not update profile photo")
        }
    }

    fun saveOfficePreferences(displayName: String, weeklyOff: Long, shiftStart: String) {
        viewModelScope.launch {
            val normalizedShift = WorkedFormatters.normalizeTime24(shiftStart)
            if (normalizedShift.isNullOrBlank()) {
                emitToast("Office time must be in HH:mm format (e.g. 09:30)")
                return@launch
            }
            val res = repository.updateProfileFields(
                updates = mapOf(
                    "displayName" to displayName.trim().ifBlank { "Admin" }.take(80),
                    "weeklyOff" to weeklyOff.coerceIn(0L, 6L),
                    "shiftStart" to normalizedShift
                ),
                fallbackDisplayName = initialDisplayName
            )
            if (res.isSuccess) emitToast("Preferences saved")
            else emitToast("Could not save preferences")
        }
    }

    fun buildBackupJson(state: WorkedDashboardState): String = repository.buildBackupJson(
        profile = state.profile,
        attendance = state.attendance.values.toList(),
        outOnWork = state.outOnWorkLogs,
        expenses = state.expenses,
        finances = state.finances,
        reminders = state.reminders,
        tasks = state.tasks,
        colleagues = state.colleagues
    )

    private data class CorePart(
        val profile: UserProfile,
        val attendance: List<AttendanceRecord>,
        val outOnWork: List<OutOnWorkLog>,
        val expenses: List<ExpenseRecord>
    )

    private data class SecondaryPart(
        val finances: List<FinanceRecord>,
        val reminders: List<KeyDateRecord>,
        val tasks: List<DailyTask>,
        val colleagues: List<ColleagueRecord>
    )

    companion object {
        private const val TAG = "WorkedVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
