package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WorkedRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val profileWriteMutex = Mutex()

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun requireSafeDocId(id: String): String {
        val trimmed = id.trim()
        require(SAFE_DOC_ID_REGEX.matches(trimmed)) { "Invalid record identifier" }
        return trimmed
    }

    private fun requireValidDate(dateKey: String, fieldLabel: String = "Date"): String {
        val trimmed = dateKey.trim()
        require(WorkedFormatters.isValidDateKey(trimmed)) { "$fieldLabel must be a valid YYYY-MM-DD date" }
        return trimmed
    }

    private fun sanitizeOptionalTime(raw: String, fieldLabel: String = "Time"): String {
        return WorkedFormatters.normalizeTime24(raw)
            ?: throw IllegalArgumentException("$fieldLabel must be in HH:mm (24-hour) format")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun defaultProfileMap(uid: String, fallbackName: String): MutableMap<String, Any> {
        val safeName = fallbackName.trim().ifBlank { "Admin" }.take(80)
        return mutableMapOf(
            "userId" to uid,
            "displayName" to safeName,
            "bio" to "",
            "photoDataUrl" to "",
            "weeklyOff" to 0L,
            "shiftStart" to "09:30",
            "darkMode" to false,
            "infoFields" to emptyList<Map<String, String>>(),
            "sessionDate" to WorkedFormatters.todayDateKey(),
            "punchedIn" to false,
            "sessionInTime" to "",
            "sessionOutTime" to "",
            "onBreak" to false,
            "breakStartMs" to 0L,
            "totalBreakMins" to 0L,
            "githubConfigUrl" to "",
            "githubConfigVersion" to "1.0.0",
            "sampleDataSeeded" to false
        )
    }

    private fun <T> Flow<T>.logAndRethrow(uid: String, op: OperationType, path: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, op, path)
            }
            throw error
        }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            userDoc(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logAndRethrow(userId, OperationType.GET, path)
        )
    }

    fun observeAttendance(userId: String): Flow<List<AttendanceRecord>> = flow {
        val path = "users/$userId/attendance"
        emitAll(
            userDoc(userId).collection("attendance")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AttendanceRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeOutOnWork(userId: String): Flow<List<OutOnWorkLog>> = flow {
        val path = "users/$userId/out_on_work"
        emitAll(
            userDoc(userId).collection("out_on_work")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        OutOnWorkLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeExpenses(userId: String): Flow<List<ExpenseRecord>> = flow {
        val path = "users/$userId/expenses"
        emitAll(
            userDoc(userId).collection("expenses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ExpenseRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeFinances(userId: String): Flow<List<FinanceRecord>> = flow {
        val path = "users/$userId/finances"
        emitAll(
            userDoc(userId).collection("finances")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        FinanceRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeReminders(userId: String): Flow<List<KeyDateRecord>> = flow {
        val path = "users/$userId/reminders"
        emitAll(
            userDoc(userId).collection("reminders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        KeyDateRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.validUntil }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeTasks(userId: String): Flow<List<DailyTask>> = flow {
        val path = "users/$userId/tasks"
        emitAll(
            userDoc(userId).collection("tasks")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        DailyTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeColleagues(userId: String): Flow<List<ColleagueRecord>> = flow {
        val path = "users/$userId/colleagues"
        emitAll(
            userDoc(userId).collection("colleagues")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ColleagueRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    suspend fun ensureProfileInitialized(fallbackName: String): Result<Unit> = profileWriteMutex.withLock {
        var path = "users/unknown"
        return@withLock try {
            val uid = requireUserId()
            val ref = userDoc(uid)
            path = ref.path
            val snap = ref.get().await()
            if (!snap.exists()) {
                val profileData = defaultProfileMap(uid, fallbackName).apply {
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                ref.set(profileData).await()
            } else {
                // Self-heal any missing required fields on legacy profile documents
                val defaults = defaultProfileMap(uid, fallbackName)
                val missingFields = mutableMapOf<String, Any>()
                defaults.forEach { (k, v) ->
                    if (k != "userId" && !snap.contains(k) && k in ALLOWED_PROFILE_UPDATE_KEYS) {
                        missingFields[k] = v
                    }
                }
                if (missingFields.isNotEmpty()) {
                    missingFields["updatedAt"] = FieldValue.serverTimestamp()
                    ref.update(missingFields).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Safely updates fields on `/users/{uid}`. If the profile document does not yet exist
     * (for example, if the user taps Dark Mode or Punch In immediately after signing in),
     * atomically creates the full schema-compliant profile document with [updates] merged in.
     */
    suspend fun updateProfileFields(
        updates: Map<String, Any>,
        fallbackDisplayName: String = "Admin"
    ): Result<Unit> = profileWriteMutex.withLock {
        var path = "users/unknown"
        return@withLock try {
            val uid = requireUserId()
            val ref = userDoc(uid)
            path = ref.path
            val safeUpdates = updates.filterKeys { it in ALLOWED_PROFILE_UPDATE_KEYS }
            require(safeUpdates.isNotEmpty()) { "No valid profile fields to update" }

            val snap = ref.get().await()
            if (!snap.exists()) {
                val fullDoc = defaultProfileMap(uid, fallbackDisplayName).apply {
                    putAll(safeUpdates)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                ref.set(fullDoc).await()
            } else {
                val payload = mutableMapOf<String, Any>()
                val defaults = defaultProfileMap(uid, fallbackDisplayName)
                defaults.forEach { (k, v) ->
                    if (k != "userId" && !snap.contains(k) && k in ALLOWED_PROFILE_UPDATE_KEYS) {
                        payload[k] = v
                    }
                }
                payload.putAll(safeUpdates)
                payload["updatedAt"] = FieldValue.serverTimestamp()
                ref.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Atomically updates both the user's active punch session (`/users/{uid}`) and today's
     * attendance document (`/users/{uid}/attendance/{dateKey}`) in a single Firestore WriteBatch
     * so Home and Calendar states can never fall out of sync.
     */
    suspend fun savePunchSessionAndAttendance(
        profileUpdates: Map<String, Any>,
        fallbackDisplayName: String,
        dateKey: String,
        status: String,
        leaveType: String = "",
        inTime: String = "",
        outTime: String = "",
        note: String = ""
    ): Result<Unit> = profileWriteMutex.withLock {
        var path = "users/unknown"
        return@withLock try {
            val uid = requireUserId()
            val safeDate = requireValidDate(dateKey)
            val safeStatus = if (status in VALID_ATTENDANCE_STATUSES) status else AttendanceStatuses.PRESENT
            val safeIn = sanitizeOptionalTime(inTime, "In time")
            val safeOut = sanitizeOptionalTime(outTime, "Out time")
            val safeUpdates = profileUpdates.filterKeys { it in ALLOWED_PROFILE_UPDATE_KEYS }

            val profileRef = userDoc(uid)
            val attendanceRef = userDoc(uid).collection("attendance").document(safeDate)
            path = profileRef.path

            val profileSnap = profileRef.get().await()
            val attendanceSnap = attendanceRef.get().await()

            val batch = db.batch()
            if (!profileSnap.exists()) {
                val fullProfile = defaultProfileMap(uid, fallbackDisplayName).apply {
                    putAll(safeUpdates)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(profileRef, fullProfile)
            } else {
                val payload = mutableMapOf<String, Any>()
                val defaults = defaultProfileMap(uid, fallbackDisplayName)
                defaults.forEach { (k, v) ->
                    if (k != "userId" && !profileSnap.contains(k) && k in ALLOWED_PROFILE_UPDATE_KEYS) {
                        payload[k] = v
                    }
                }
                payload.putAll(safeUpdates)
                payload["updatedAt"] = FieldValue.serverTimestamp()
                batch.update(profileRef, payload)
            }

            if (attendanceSnap.exists()) {
                batch.update(
                    attendanceRef,
                    mapOf(
                        "status" to safeStatus,
                        "leaveType" to leaveType.trim().take(60),
                        "inTime" to safeIn,
                        "outTime" to safeOut,
                        "note" to note.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    attendanceRef,
                    mapOf(
                        "dateKey" to safeDate,
                        "status" to safeStatus,
                        "leaveType" to leaveType.trim().take(60),
                        "inTime" to safeIn,
                        "outTime" to safeOut,
                        "note" to note.trim().take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertAttendance(
        dateKey: String,
        status: String,
        leaveType: String = "",
        inTime: String = "",
        outTime: String = "",
        note: String = ""
    ): Result<Unit> {
        var path = "users/unknown/attendance/$dateKey"
        return try {
            val uid = requireUserId()
            val safeDate = requireValidDate(dateKey)
            val safeStatus = if (status in VALID_ATTENDANCE_STATUSES) status else AttendanceStatuses.PRESENT
            val safeIn = sanitizeOptionalTime(inTime, "In time")
            val safeOut = sanitizeOptionalTime(outTime, "Out time")

            val ref = userDoc(uid).collection("attendance").document(safeDate)
            path = ref.path
            val exists = ref.get().await().exists()
            if (exists) {
                ref.update(
                    mapOf(
                        "status" to safeStatus,
                        "leaveType" to leaveType.trim().take(60),
                        "inTime" to safeIn,
                        "outTime" to safeOut,
                        "note" to note.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "dateKey" to safeDate,
                        "status" to safeStatus,
                        "leaveType" to leaveType.trim().take(60),
                        "inTime" to safeIn,
                        "outTime" to safeOut,
                        "note" to note.trim().take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAttendance(dateKey: String): Result<Unit> {
        var path = "users/unknown/attendance/$dateKey"
        return try {
            val uid = requireUserId()
            val safeDate = requireValidDate(dateKey)
            val ref = userDoc(uid).collection("attendance").document(safeDate)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addOutOnWork(
        date: String,
        mode: String,
        startTime: String,
        endTime: String,
        meetingWith: String,
        location: String,
        remarks: String,
        attachmentName: String
    ): Result<Unit> {
        var path = "users/unknown/out_on_work"
        return try {
            val uid = requireUserId()
            val safeDate = requireValidDate(date)
            val safeMode = if (mode in VALID_OUT_WORK_MODES) mode else "Full Day"
            val safeStart = sanitizeOptionalTime(startTime, "Start time")
            val safeEnd = sanitizeOptionalTime(endTime, "End time")
            val safeMeeting = meetingWith.trim().take(150)
            require(safeMeeting.isNotBlank()) { "Office / Department is required" }

            val docId = "oow_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val ref = userDoc(uid).collection("out_on_work").document(docId)
            path = ref.path
            ref.set(
                mapOf(
                    "date" to safeDate,
                    "mode" to safeMode,
                    "startTime" to safeStart,
                    "endTime" to safeEnd,
                    "meetingWith" to safeMeeting,
                    "location" to location.trim().take(200),
                    "remarks" to remarks.trim().take(500),
                    "attachmentName" to attachmentName.trim().take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val attResult = upsertAttendance(
                dateKey = safeDate,
                status = AttendanceStatuses.OUTSTATION,
                inTime = safeStart,
                outTime = safeEnd,
                note = "Out Work: $safeMeeting"
            )
            if (attResult.isFailure) return attResult
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addExpense(
        date: String,
        title: String,
        category: String,
        amount: Long,
        owner: String,
        note: String
    ): Result<Unit> {
        var path = "users/unknown/expenses"
        return try {
            val uid = requireUserId()
            val safeDate = requireValidDate(date)
            val safeTitle = title.trim().take(150)
            require(safeTitle.isNotBlank()) { "Title is required" }
            require(amount in 1L..100_000_000L) { "Amount must be between ₹1 and ₹10,00,00,000" }
            val docId = "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val ref = userDoc(uid).collection("expenses").document(docId)
            path = ref.path
            ref.set(
                mapOf(
                    "date" to safeDate,
                    "title" to safeTitle,
                    "category" to category.trim().ifBlank { "Office" }.take(60),
                    "amount" to amount,
                    "owner" to if (owner == "headmaster") "headmaster" else "personal",
                    "recovered" to false,
                    "note" to note.trim().take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleExpenseRecovered(expenseId: String, recovered: Boolean): Result<Unit> {
        var path = "users/unknown/expenses/$expenseId"
        return try {
            val uid = requireUserId()
            val safeId = requireSafeDocId(expenseId)
            val ref = userDoc(uid).collection("expenses").document(safeId)
            path = ref.path
            ref.update(
                mapOf(
                    "recovered" to recovered,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        var path = "users/unknown/expenses/$expenseId"
        return try {
            val uid = requireUserId()
            val safeId = requireSafeDocId(expenseId)
            val ref = userDoc(uid).collection("expenses").document(safeId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addFinance(
        date: String,
        title: String,
        amount: Long,
        status: String
    ): Result<Unit> {
        var path = "users/unknown/finances"
        return try {
            val uid = requireUserId()
            val safeDate = requireValidDate(date)
            val safeTitle = title.trim().take(150)
            require(safeTitle.isNotBlank()) { "Description is required" }
            require(amount in 1L..100_000_000L) { "Amount must be between ₹1 and ₹10,00,00,000" }
            val docId = "fin_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val ref = userDoc(uid).collection("finances").document(docId)
            path = ref.path
            ref.set(
                mapOf(
                    "date" to safeDate,
                    "title" to safeTitle,
                    "type" to "income",
                    "amount" to amount,
                    "status" to if (status == "pending") "pending" else "received",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFinanceStatus(financeId: String, newStatus: String): Result<Unit> {
        var path = "users/unknown/finances/$financeId"
        return try {
            val uid = requireUserId()
            val safeId = requireSafeDocId(financeId)
            val ref = userDoc(uid).collection("finances").document(safeId)
            path = ref.path
            ref.update(
                mapOf(
                    "status" to if (newStatus == "pending") "pending" else "received",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFinance(financeId: String): Result<Unit> {
        var path = "users/unknown/finances/$financeId"
        return try {
            val uid = requireUserId()
            val safeId = requireSafeDocId(financeId)
            val ref = userDoc(uid).collection("finances").document(safeId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveReminder(
        editId: String?,
        title: String,
        category: String,
        validFrom: String,
        validUntil: String,
        note: String
    ): Result<Unit> {
        var path = "users/unknown/reminders"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().take(150)
            require(safeTitle.isNotBlank()) { "Title is required" }
            val safeFrom = requireValidDate(validFrom, "Valid From")
            val safeUntil = requireValidDate(validUntil, "Valid Until")
            require(safeUntil >= safeFrom) { "Valid Until cannot be before Valid From" }

            if (editId.isNullOrBlank()) {
                val docId = "rem_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val ref = userDoc(uid).collection("reminders").document(docId)
                path = ref.path
                ref.set(
                    mapOf(
                        "title" to safeTitle,
                        "category" to category.trim().ifBlank { "Document" }.take(60),
                        "validFrom" to safeFrom,
                        "validUntil" to safeUntil,
                        "note" to note.trim().take(400),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val safeId = requireSafeDocId(editId)
                val ref = userDoc(uid).collection("reminders").document(safeId)
                path = ref.path
                ref.update(
                    mapOf(
                        "title" to safeTitle,
                        "category" to category.trim().ifBlank { "Document" }.take(60),
                        "validFrom" to safeFrom,
                        "validUntil" to safeUntil,
                        "note" to note.trim().take(400),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        var path = "users/unknown/reminders/$reminderId"
        return try {
            val uid = requireUserId()
            val safeId = requireSafeDocId(reminderId)
            val ref = userDoc(uid).collection("reminders").document(safeId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addTask(text: String): Result<Unit> {
        var path = "users/unknown/tasks"
        return try {
            val uid = requireUserId()
            val safeText = text.trim().take(300)
            require(safeText.isNotBlank()) { "Task text cannot be empty" }
            val docId = "tsk_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val ref = userDoc(uid).collection("tasks").document(docId)
            path = ref.path
            ref.set(
                mapOf(
                    "text" to safeText,
                    "done" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTask(taskId: String, done: Boolean): Result<Unit> {
        var path = "users/unknown/tasks/$taskId"
        return try {
            val uid = requireUserId()
            val safeId = requireSafeDocId(taskId)
            val ref = userDoc(uid).collection("tasks").document(safeId)
            path = ref.path
            ref.update(
                mapOf(
                    "done" to done,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        var path = "users/unknown/tasks/$taskId"
        return try {
            val uid = requireUserId()
            val safeId = requireSafeDocId(taskId)
            val ref = userDoc(uid).collection("tasks").document(safeId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveColleague(
        editId: String?,
        name: String,
        role: String,
        theme: String,
        photoDataUrl: String,
        bgPhotoDataUrl: String,
        infoFields: List<InfoField>
    ): Result<Unit> {
        var path = "users/unknown/colleagues"
        return try {
            val uid = requireUserId()
            val safeName = name.trim().take(100)
            require(safeName.isNotBlank()) { "Name is required" }
            val safeTheme = if (theme in listOf("sky", "mint", "sunset", "lavender")) theme else "sky"
            val safePhoto = if (photoDataUrl.length <= 200_000) photoDataUrl else ""
            val safeBgPhoto = if (bgPhotoDataUrl.length <= 200_000) bgPhotoDataUrl else ""
            val sanitizedInfo = infoFields
                .filter { it.label.isNotBlank() || it.value.isNotBlank() }
                .take(20)
                .map {
                    mapOf(
                        "label" to it.label.trim().ifBlank { "Info" }.take(60),
                        "value" to it.value.trim().take(300)
                    )
                }

            if (editId.isNullOrBlank()) {
                val docId = "col_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val ref = userDoc(uid).collection("colleagues").document(docId)
                path = ref.path
                ref.set(
                    mapOf(
                        "name" to safeName,
                        "role" to role.trim().take(200),
                        "theme" to safeTheme,
                        "photoDataUrl" to safePhoto,
                        "bgPhotoDataUrl" to safeBgPhoto,
                        "infoFields" to sanitizedInfo,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val safeId = requireSafeDocId(editId)
                val ref = userDoc(uid).collection("colleagues").document(safeId)
                path = ref.path
                ref.update(
                    mapOf(
                        "name" to safeName,
                        "role" to role.trim().take(200),
                        "theme" to safeTheme,
                        "photoDataUrl" to safePhoto,
                        "bgPhotoDataUrl" to safeBgPhoto,
                        "infoFields" to sanitizedInfo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteColleague(colleagueId: String): Result<Unit> {
        var path = "users/unknown/colleagues/$colleagueId"
        return try {
            val uid = requireUserId()
            val safeId = requireSafeDocId(colleagueId)
            val ref = userDoc(uid).collection("colleagues").document(safeId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun buildBackupJson(
        profile: UserProfile,
        attendance: List<AttendanceRecord>,
        outOnWork: List<OutOnWorkLog>,
        expenses: List<ExpenseRecord>,
        finances: List<FinanceRecord>,
        reminders: List<KeyDateRecord>,
        tasks: List<DailyTask>,
        colleagues: List<ColleagueRecord>
    ): String {
        val root = JSONObject()
        root.put("exportedAt", WorkedFormatters.todayDateKey())
        root.put("profile", JSONObject().apply {
            put("displayName", profile.displayName)
            put("bio", profile.bio)
            put("weeklyOff", profile.weeklyOff)
            put("shiftStart", profile.shiftStart)
        })
        root.put("attendance", JSONArray().apply {
            attendance.forEach { a ->
                put(JSONObject().apply {
                    put("dateKey", a.dateKey)
                    put("status", a.status)
                    put("leaveType", a.leaveType)
                    put("inTime", a.inTime)
                    put("outTime", a.outTime)
                    put("note", a.note)
                })
            }
        })
        root.put("outOnWork", JSONArray().apply {
            outOnWork.forEach { o ->
                put(JSONObject().apply {
                    put("date", o.date)
                    put("mode", o.mode)
                    put("startTime", o.startTime)
                    put("endTime", o.endTime)
                    put("meetingWith", o.meetingWith)
                    put("location", o.location)
                    put("remarks", o.remarks)
                })
            }
        })
        root.put("expenses", JSONArray().apply {
            expenses.forEach { e ->
                put(JSONObject().apply {
                    put("date", e.date)
                    put("title", e.title)
                    put("category", e.category)
                    put("amount", e.amount)
                    put("owner", e.owner)
                    put("recovered", e.recovered)
                    put("note", e.note)
                })
            }
        })
        root.put("finances", JSONArray().apply {
            finances.forEach { f ->
                put(JSONObject().apply {
                    put("date", f.date)
                    put("title", f.title)
                    put("amount", f.amount)
                    put("status", f.status)
                })
            }
        })
        root.put("reminders", JSONArray().apply {
            reminders.forEach { r ->
                put(JSONObject().apply {
                    put("title", r.title)
                    put("category", r.category)
                    put("validFrom", r.validFrom)
                    put("validUntil", r.validUntil)
                    put("note", r.note)
                })
            }
        })
        root.put("tasks", JSONArray().apply {
            tasks.forEach { t ->
                put(JSONObject().apply {
                    put("text", t.text)
                    put("done", t.done)
                })
            }
        })
        root.put("colleagues", JSONArray().apply {
            colleagues.forEach { c ->
                put(JSONObject().apply {
                    put("name", c.name)
                    put("role", c.role)
                    put("theme", c.theme)
                })
            }
        })
        return root.toString(2)
    }

    companion object {
        private val SAFE_DOC_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")

        private val VALID_ATTENDANCE_STATUSES = setOf(
            AttendanceStatuses.PRESENT,
            AttendanceStatuses.LEAVE,
            AttendanceStatuses.ABSENT,
            AttendanceStatuses.HOLIDAY,
            AttendanceStatuses.OFFDAY,
            AttendanceStatuses.OUTSTATION,
            AttendanceStatuses.LATE,
            AttendanceStatuses.EARLY_GOING,
            AttendanceStatuses.MISS_PUNCH
        )

        private val VALID_OUT_WORK_MODES = setOf("Half Day", "Full Day", "Quarter Day")

        private val ALLOWED_PROFILE_UPDATE_KEYS = setOf(
            "displayName", "bio", "photoDataUrl", "weeklyOff", "shiftStart",
            "darkMode", "infoFields", "sessionDate", "punchedIn",
            "sessionInTime", "sessionOutTime", "onBreak", "breakStartMs",
            "totalBreakMins", "githubConfigUrl", "githubConfigVersion",
            "sampleDataSeeded"
        )
    }
}
