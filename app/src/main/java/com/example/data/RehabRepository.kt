package com.example.data

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class RehabRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val guestStore: GuestRehabStore? = null
) : RehabDataSource {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        guestStore = GuestRehabStore.getInstance(context.applicationContext)
    )

    private fun requireAuthorizedUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("請先登入 Google 帳號才能使用雲端復健紀錄。")
        check(uid == expectedUserId) { "目前登入帳號已變更，請重新整理畫面。" }
        return uid
    }

    override fun observeProfile(userId: String): Flow<RehabProfile?> = flow {
        val uid = requireAuthorizedUserId(userId)
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        RehabProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, null, docRef.path)
                    }
                    throw error
                }
        )
    }

    override fun observeTasks(userId: String): Flow<List<RehabTask>> = flow {
        val uid = requireAuthorizedUserId(userId)
        val colRef = db.collection("users").document(uid).collection("tasks")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RehabTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    override fun observeCheckins(userId: String): Flow<List<RehabCheckin>> = flow {
        val uid = requireAuthorizedUserId(userId)
        val colRef = db.collection("users").document(uid).collection("checkins")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RehabCheckin::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    override suspend fun ensureProfileInitialized(
        userId: String,
        displayName: String,
        todayDate: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val profileRef = db.collection("users").document(uid)
            val snapshot = profileRef.get().await()
            val safeName = displayName.trim().ifBlank { "復健小夥伴" }.take(60)
            if (!snapshot.exists()) {
                val localSnapshot = runCatching { guestStore?.readLocalStateSnapshot() }.getOrNull()
                val localProfile = localSnapshot?.profile
                if (localProfile != null) {
                    val migratedToday = localProfile.todayDate.takeIf { it.length == 10 } ?: todayDate
                    val profilePayload = mapOf(
                        "userId" to uid,
                        "displayName" to localProfile.displayName.trim().ifBlank { safeName }.take(60),
                        "feifeiNickname" to localProfile.feifeiNickname.trim().let {
                            if (it.isBlank() || it == "主治醫師") "主治醫生" else it
                        }.take(30),
                        "todayDate" to migratedToday,
                        "todayCount" to if (migratedToday == todayDate) localProfile.todayCount.coerceIn(0L, 999L) else 0L,
                        "totalCompleted" to localProfile.totalCompleted.coerceAtLeast(0L),
                        "antibodyBalance" to localProfile.antibodyBalance.coerceIn(0L, 9_999_999L),
                        "ownedItemIds" to localProfile.ownedItemIds.distinct().take(20),
                        "equippedItemIds" to localProfile.equippedItemIds.distinct().take(20),
                        "wardPlacements" to localProfile.wardPlacements.take(20),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    val tasksCol = profileRef.collection("tasks")
                    val checkinsCol = profileRef.collection("checkins")
                    val writeOps = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
                    writeOps += profileRef to profilePayload

                    localSnapshot.tasks.forEach { task ->
                        val title = task.title.trim().take(100)
                        if (title.isEmpty()) return@forEach
                        val effort = EffortLevel.fromWire(task.effortLevel).wireValue
                        val category = task.category.trim().take(30).ifEmpty { TaskCategory.DAILY.wireValue }
                        val safeResetHour = if (category == TaskCategory.GAME.wireValue) task.resetHour.coerceIn(0L, 23L) else 0L
                        val safeDeadline = if (category == TaskCategory.HOMEWORK.wireValue) task.deadlineDate.trim().take(10) else ""
                        val taskRef = tasksCol.document()
                        writeOps += taskRef to mapOf(
                            "userId" to uid,
                            "title" to title,
                            "effortLevel" to effort,
                            "category" to category,
                            "completed" to task.completed,
                            "completedDate" to task.completedDate.trim().take(10),
                            "note" to task.note.trim().take(200),
                            "sortOrder" to task.sortOrder,
                            "timeSlotsEnabled" to task.timeSlotsEnabled,
                            "morningDone" to task.morningDone,
                            "noonDone" to task.noonDone,
                            "eveningDone" to task.eveningDone,
                            "morningEnabled" to task.morningEnabled,
                            "noonEnabled" to task.noonEnabled,
                            "eveningEnabled" to task.eveningEnabled,
                            "resetHour" to safeResetHour,
                            "deadlineDate" to safeDeadline,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    }

                    localSnapshot.checkins.forEach { checkin ->
                        val dateKey = checkin.dateKey.trim()
                        if (dateKey.length != 10 || !dateKey.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) return@forEach
                        val energy = EnergyLevel.fromWire(checkin.energyLevel).wireValue
                        val percent = checkin.resolvedPercent().coerceIn(0, 100)
                        val completedTitles = checkin.completedTaskTitles
                            .map { it.trim().take(120) }
                            .filter { it.isNotEmpty() }
                            .distinct()
                            .take(100)
                        val checkinRef = checkinsCol.document(dateKey)
                        writeOps += checkinRef to mapOf(
                            "userId" to uid,
                            "dateKey" to dateKey,
                            "energyLevel" to energy,
                            "energyPercent" to percent,
                            "reflection" to checkin.reflection.trim().take(300),
                            "wardSnapshot" to checkin.wardSnapshot.take(20),
                            "doctorPoseSnapshot" to checkin.doctorPoseSnapshot.take(128),
                            "completedTaskTitles" to completedTitles,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    }

                    writeOps.chunked(400).forEach { chunk ->
                        val chunkBatch = db.batch()
                        chunk.forEach { (ref, payload) -> chunkBatch.set(ref, payload) }
                        chunkBatch.commit().await()
                    }
                    resetExpiredTasksInternal(uid)
                    guestStore?.setSyncedCloudUid(uid)
                } else {
                    val batch = db.batch()
                    val profilePayload = mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "feifeiNickname" to "主治醫生",
                        "todayDate" to todayDate,
                        "todayCount" to 0L,
                        "totalCompleted" to 0L,
                        "antibodyBalance" to 0L,
                        "ownedItemIds" to emptyList<String>(),
                        "equippedItemIds" to emptyList<String>(),
                        "wardPlacements" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(profileRef, profilePayload)

                    val tasksCol = profileRef.collection("tasks")
                    RehabCatalog.starterTasks.forEachIndexed { index, (title, effort, note) ->
                        val taskRef = tasksCol.document()
                        batch.set(
                            taskRef,
                            mapOf(
                                "userId" to uid,
                                "title" to title.take(100),
                                "effortLevel" to effort.wireValue,
                                "category" to TaskCategory.DAILY.wireValue,
                                "completed" to false,
                                "completedDate" to "",
                                "note" to note.take(200),
                                "sortOrder" to index.toLong(),
                                "timeSlotsEnabled" to false,
                                "morningDone" to false,
                                "noonDone" to false,
                                "eveningDone" to false,
                                "morningEnabled" to true,
                                "noonEnabled" to true,
                                "eveningEnabled" to true,
                                "resetHour" to 0L,
                                "deadlineDate" to "",
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                }
            } else {
                val storedDate = snapshot.getString("todayDate") ?: ""
                val updates = mutableMapOf<String, Any>()
                if (storedDate != todayDate) {
                    updates["todayDate"] = todayDate
                    updates["todayCount"] = 0L
                }
                val currentNick = snapshot.getString("feifeiNickname")?.trim().orEmpty()
                if (currentNick.isBlank() || currentNick == "主治醫師") {
                    updates["feifeiNickname"] = "主治醫生"
                }
                if (!snapshot.contains("ownedItemIds")) {
                    updates["ownedItemIds"] = emptyList<String>()
                }
                if (!snapshot.contains("equippedItemIds")) {
                    updates["equippedItemIds"] = emptyList<String>()
                }
                if (!snapshot.contains("wardPlacements")) {
                    val existingEquipped = (snapshot.get("equippedItemIds") as? List<*>)
                        ?.filterIsInstance<String>()
                        .orEmpty()
                    updates["wardPlacements"] = existingEquipped.map { id ->
                        val (dx, dy) = RehabCatalog.defaultWardPosition(id)
                        WardPlacement(id, dx, dy).encode()
                    }
                }
                if (updates.isNotEmpty()) {
                    updates["updatedAt"] = FieldValue.serverTimestamp()
                    profileRef.update(updates).await()
                }
                resetExpiredTasksInternal(uid)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun resetExpiredTasks(userId: String, todayDate: String): Result<Int> {
        val docPath = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val profileRef = db.collection("users").document(uid)
            val profileSnap = profileRef.get().await()
            if (profileSnap.exists() && (profileSnap.getString("todayDate") ?: "") != todayDate) {
                profileRef.update(
                    mapOf(
                        "todayDate" to todayDate,
                        "todayCount" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val count = resetExpiredTasksInternal(uid)
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun resetExpiredTasksInternal(uid: String): Int {
        val tasksCol = db.collection("users").document(uid).collection("tasks")
        val snap = tasksCol.get().await()
        val batch = db.batch()
        var resetCount = 0
        snap.documents.forEach { doc ->
            val category = doc.getString("category") ?: TaskCategory.DAILY.wireValue
            if (category == TaskCategory.HOMEWORK.wireValue) return@forEach
            val completed = doc.getBoolean("completed") ?: false
            val morningDone = doc.getBoolean("morningDone") ?: false
            val noonDone = doc.getBoolean("noonDone") ?: false
            val eveningDone = doc.getBoolean("eveningDone") ?: false
            if (!completed && !morningDone && !noonDone && !eveningDone) return@forEach

            val resetHour = (doc.getLong("resetHour") ?: 0L).coerceIn(0L, 23L)
            val completedDate = doc.getString("completedDate") ?: ""
            val updatedMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            if (shouldResetTask(category, resetHour, completedDate, updatedMillis)) {
                resetCount++
                batch.update(
                    doc.reference,
                    mapOf(
                        "completed" to false,
                        "completedDate" to "",
                        "morningDone" to false,
                        "noonDone" to false,
                        "eveningDone" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        if (resetCount > 0) {
            batch.commit().await()
        }
        return resetCount
    }

    override suspend fun addTask(
        userId: String,
        title: String,
        effortLevel: EffortLevel,
        note: String,
        categoryWire: String,
        resetHour: Long,
        deadlineDate: String,
        timeSlotsEnabled: Boolean
    ): Result<Unit> {
        val trimmedTitle = title.trim()
        val trimmedNote = note.trim()
        val cleanCategory = categoryWire.trim().take(30).ifEmpty { TaskCategory.DAILY.wireValue }
        val safeResetHour = if (cleanCategory == TaskCategory.GAME.wireValue) resetHour.coerceIn(0L, 23L) else 0L
        val safeDeadline = if (cleanCategory == TaskCategory.HOMEWORK.wireValue) deadlineDate.trim().take(10) else ""
        val colPath = "users/$userId/tasks"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(trimmedTitle.isNotEmpty() && trimmedTitle.length <= 100) {
                "微任務名稱需為 1 到 100 個字"
            }
            require(trimmedNote.length <= 200) {
                "備註最多 200 個字"
            }
            val docRef = db.collection("users").document(uid).collection("tasks").document()
            val payload = mapOf(
                "userId" to uid,
                "title" to trimmedTitle,
                "effortLevel" to effortLevel.wireValue,
                "category" to cleanCategory,
                "completed" to false,
                "completedDate" to "",
                "note" to trimmedNote,
                "sortOrder" to -System.currentTimeMillis(),
                "timeSlotsEnabled" to timeSlotsEnabled,
                "morningDone" to false,
                "noonDone" to false,
                "eveningDone" to false,
                "morningEnabled" to true,
                "noonEnabled" to true,
                "eveningEnabled" to true,
                "resetHour" to safeResetHour,
                "deadlineDate" to safeDeadline,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    override suspend fun toggleTaskCompletion(
        userId: String,
        task: RehabTask,
        todayDate: String
    ): Result<Unit> {
        val docPath = "users/$userId/tasks/${task.id}"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(task.id.isNotBlank()) { "任務 ID 不可為空" }
            val profileRef = db.collection("users").document(uid)
            val taskRef = profileRef.collection("tasks").document(task.id)
            val newCompleted = !task.completed
            val effectiveTaskDate = effectiveCompletionDateKey(task.category, task.resetHour)
            val newCompletedDate = if (newCompleted) effectiveTaskDate else ""

            db.runTransaction { transaction ->
                val profileSnap = transaction.get(profileRef)
                val storedDate = profileSnap.getString("todayDate") ?: ""
                val existingTodayCount = if (storedDate == todayDate) {
                    (profileSnap.getLong("todayCount") ?: 0L)
                } else {
                    0L
                }
                val existingTotal = profileSnap.getLong("totalCompleted") ?: 0L

                val wasCountedToday = task.completedDate == effectiveTaskDate || task.completedDate == todayDate
                val nextTodayCount = if (newCompleted) {
                    (existingTodayCount + 1L).coerceIn(0L, 999L)
                } else if (wasCountedToday) {
                    (existingTodayCount - 1L).coerceIn(0L, 999L)
                } else {
                    existingTodayCount
                }

                val nextTotal = if (newCompleted) {
                    existingTotal + 1L
                } else {
                    (existingTotal - 1L).coerceAtLeast(0L)
                }

                val taskUpdates = mutableMapOf<String, Any>(
                    "completed" to newCompleted,
                    "completedDate" to newCompletedDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (task.timeSlotsEnabled) {
                    taskUpdates["morningDone"] = newCompleted && task.morningEnabled
                    taskUpdates["noonDone"] = newCompleted && task.noonEnabled
                    taskUpdates["eveningDone"] = newCompleted && task.eveningEnabled
                }

                transaction.update(taskRef, taskUpdates)
                transaction.update(
                    profileRef,
                    mapOf(
                        "todayDate" to todayDate,
                        "todayCount" to nextTodayCount,
                        "totalCompleted" to nextTotal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun toggleTaskTimeSlot(
        userId: String,
        task: RehabTask,
        slotIndex: Int,
        todayDate: String
    ): Result<Unit> {
        val docPath = "users/$userId/tasks/${task.id}"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(task.id.isNotBlank()) { "任務 ID 不可為空" }
            val profileRef = db.collection("users").document(uid)
            val taskRef = profileRef.collection("tasks").document(task.id)

            db.runTransaction { transaction ->
                val profileSnap = transaction.get(profileRef)
                val taskSnap = transaction.get(taskRef)
                val curMorning = taskSnap.getBoolean("morningDone") ?: task.morningDone
                val curNoon = taskSnap.getBoolean("noonDone") ?: task.noonDone
                val curEvening = taskSnap.getBoolean("eveningDone") ?: task.eveningDone
                val morningEn = taskSnap.getBoolean("morningEnabled") ?: task.morningEnabled
                val noonEn = taskSnap.getBoolean("noonEnabled") ?: task.noonEnabled
                val eveningEn = taskSnap.getBoolean("eveningEnabled") ?: task.eveningEnabled
                val anyConfigEn = morningEn || noonEn || eveningEn
                val effMorningEn = if (anyConfigEn) morningEn else true
                val effNoonEn = if (anyConfigEn) noonEn else true
                val effEveningEn = if (anyConfigEn) eveningEn else true
                val wasCompleted = taskSnap.getBoolean("completed") ?: task.completed

                val nextMorning = if (slotIndex == 0) !curMorning else curMorning
                val nextNoon = if (slotIndex == 1) !curNoon else curNoon
                val nextEvening = if (slotIndex == 2) !curEvening else curEvening
                val nextCompleted = (!effMorningEn || nextMorning) &&
                    (!effNoonEn || nextNoon) &&
                    (!effEveningEn || nextEvening)
                val anyDone = (effMorningEn && nextMorning) ||
                    (effNoonEn && nextNoon) ||
                    (effEveningEn && nextEvening)
                val taskCat = taskSnap.getString("category") ?: task.category
                val taskResetHour = (taskSnap.getLong("resetHour") ?: task.resetHour).coerceIn(0L, 23L)
                val effectiveTaskDate = effectiveCompletionDateKey(taskCat, taskResetHour)
                val nextCompletedDate = if (anyDone || nextCompleted) effectiveTaskDate else ""

                val storedDate = profileSnap.getString("todayDate") ?: ""
                val existingTodayCount = if (storedDate == todayDate) {
                    (profileSnap.getLong("todayCount") ?: 0L)
                } else {
                    0L
                }
                val existingTotal = profileSnap.getLong("totalCompleted") ?: 0L

                val nextTodayCount = when {
                    !wasCompleted && nextCompleted -> (existingTodayCount + 1L).coerceIn(0L, 999L)
                    wasCompleted && !nextCompleted -> (existingTodayCount - 1L).coerceIn(0L, 999L)
                    else -> existingTodayCount
                }
                val nextTotal = when {
                    !wasCompleted && nextCompleted -> existingTotal + 1L
                    wasCompleted && !nextCompleted -> (existingTotal - 1L).coerceAtLeast(0L)
                    else -> existingTotal
                }

                transaction.update(
                    taskRef,
                    mapOf(
                        "morningDone" to nextMorning,
                        "noonDone" to nextNoon,
                        "eveningDone" to nextEvening,
                        "completed" to nextCompleted,
                        "completedDate" to nextCompletedDate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    profileRef,
                    mapOf(
                        "todayDate" to todayDate,
                        "todayCount" to nextTodayCount,
                        "totalCompleted" to nextTotal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun toggleTaskSplitMode(
        userId: String,
        taskId: String,
        enabled: Boolean,
        morningEnabled: Boolean,
        noonEnabled: Boolean,
        eveningEnabled: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(taskId.isNotBlank()) { "任務 ID 不可為空" }
            val hasAny = morningEnabled || noonEnabled || eveningEnabled
            val safeMorning = if (hasAny) morningEnabled else true
            val safeNoon = if (hasAny) noonEnabled else true
            val safeEvening = if (hasAny) eveningEnabled else true
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            val snap = docRef.get().await()
            val completed = snap.getBoolean("completed") ?: false
            val curMorning = snap.getBoolean("morningDone") ?: false
            val curNoon = snap.getBoolean("noonDone") ?: false
            val curEvening = snap.getBoolean("eveningDone") ?: false
            val nextMorning = if (!enabled) false else if (completed) safeMorning else (curMorning && safeMorning)
            val nextNoon = if (!enabled) false else if (completed) safeNoon else (curNoon && safeNoon)
            val nextEvening = if (!enabled) false else if (completed) safeEvening else (curEvening && safeEvening)
            val nextCompleted = if (!enabled) {
                completed
            } else {
                (!safeMorning || nextMorning) && (!safeNoon || nextNoon) && (!safeEvening || nextEvening)
            }
            docRef.update(
                mapOf(
                    "timeSlotsEnabled" to enabled,
                    "morningEnabled" to safeMorning,
                    "noonEnabled" to safeNoon,
                    "eveningEnabled" to safeEvening,
                    "morningDone" to nextMorning,
                    "noonDone" to nextNoon,
                    "eveningDone" to nextEvening,
                    "completed" to nextCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateTaskSortOrders(
        userId: String,
        orderedTaskIds: List<String>
    ): Result<Unit> {
        val colPath = "users/$userId/tasks"
        return try {
            val uid = requireAuthorizedUserId(userId)
            if (orderedTaskIds.isEmpty()) return Result.success(Unit)
            val tasksCol = db.collection("users").document(uid).collection("tasks")
            val batch = db.batch()
            orderedTaskIds.forEachIndexed { idx, taskId ->
                batch.update(
                    tasksCol.document(taskId),
                    mapOf(
                        "sortOrder" to idx.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, colPath)
            Result.failure(e)
        }
    }

    override suspend fun updateTaskScheduleMeta(
        userId: String,
        taskId: String,
        resetHour: Long,
        deadlineDate: String
    ): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(taskId.isNotBlank()) { "任務 ID 不可為空" }
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            docRef.update(
                mapOf(
                    "resetHour" to resetHour.coerceIn(0L, 23L),
                    "deadlineDate" to deadlineDate.trim().take(10),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun shrinkTask(
        userId: String,
        taskId: String,
        newTitle: String,
        newNote: String
    ): Result<Unit> {
        val trimmedTitle = newTitle.trim()
        val trimmedNote = newNote.trim()
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(taskId.isNotBlank()) { "任務 ID 不可為空" }
            require(trimmedTitle.isNotEmpty() && trimmedTitle.length <= 100) {
                "微任務名稱需為 1 到 100 個字"
            }
            require(trimmedNote.length <= 200) {
                "備註最多 200 個字"
            }
            val docRef = db.collection("users").document(uid)
                .collection("tasks").document(taskId)
            docRef.update(
                mapOf(
                    "title" to trimmedTitle,
                    "effortLevel" to EffortLevel.MICRO.wireValue,
                    "note" to trimmedNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateTaskDetails(
        userId: String,
        taskId: String,
        newTitle: String,
        newNote: String,
        effortLevel: EffortLevel,
        resetHour: Long,
        deadlineDate: String
    ): Result<Unit> {
        val trimmedTitle = newTitle.trim()
        val trimmedNote = newNote.trim()
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(taskId.isNotBlank()) { "任務 ID 不可為空" }
            require(trimmedTitle.isNotEmpty() && trimmedTitle.length <= 100) {
                "微任務名稱需為 1 到 100 個字"
            }
            require(trimmedNote.length <= 200) {
                "備註最多 200 個字"
            }
            val docRef = db.collection("users").document(uid)
                .collection("tasks").document(taskId)
            docRef.update(
                mapOf(
                    "title" to trimmedTitle,
                    "note" to trimmedNote,
                    "effortLevel" to effortLevel.wireValue,
                    "resetHour" to resetHour.coerceIn(0L, 23L),
                    "deadlineDate" to deadlineDate.trim().take(10),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(taskId.isNotBlank()) { "任務 ID 不可為空" }
            db.collection("users").document(uid)
                .collection("tasks").document(taskId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun saveCheckin(
        userId: String,
        dateKey: String,
        energyLevel: EnergyLevel,
        energyPercent: Int,
        reflection: String,
        wardSnapshot: List<String>,
        doctorPoseSnapshot: String,
        completedTaskTitles: List<String>
    ): Result<Unit> {
        val trimmedReflection = reflection.trim()
        val safePercent = energyPercent.coerceIn(0, 100)
        val cleanSnapshot = wardSnapshot.take(20)
        val safeDoctorSnapshot = doctorPoseSnapshot.take(128)
        val cleanCompletedTitles = completedTaskTitles
            .map { it.trim().take(120) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(100)
        val docPath = "users/$userId/checkins/$dateKey"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(dateKey.length == 10) { "日期格式需為 YYYY-MM-DD" }
            require(trimmedReflection.length <= 300) { "心情筆記最多 300 個字" }
            val docRef = db.collection("users").document(uid)
                .collection("checkins").document(dateKey)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingTitles = (existingSnap.get("completedTaskTitles") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                val mergedTitles = (existingTitles + cleanCompletedTitles)
                    .map { it.trim().take(120) }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .take(100)
                docRef.update(
                    mapOf(
                        "energyLevel" to energyLevel.wireValue,
                        "energyPercent" to safePercent,
                        "reflection" to trimmedReflection,
                        "wardSnapshot" to cleanSnapshot,
                        "doctorPoseSnapshot" to safeDoctorSnapshot,
                        "completedTaskTitles" to mergedTitles,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "energyLevel" to energyLevel.wireValue,
                        "energyPercent" to safePercent,
                        "reflection" to trimmedReflection,
                        "wardSnapshot" to cleanSnapshot,
                        "doctorPoseSnapshot" to safeDoctorSnapshot,
                        "completedTaskTitles" to cleanCompletedTitles,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateFeifeiNickname(userId: String, nickname: String): Result<Unit> {
        val trimmed = nickname.trim()
        val docPath = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(trimmed.isNotEmpty() && trimmed.length <= 30) {
                "暱稱需為 1 到 30 個字"
            }
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "feifeiNickname" to trimmed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun redeemPharmacyItem(userId: String, item: PharmacyItem): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val profileRef = db.collection("users").document(uid)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(profileRef)
                val owned = (snapshot.get("ownedItemIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                val equipped = (snapshot.get("equippedItemIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()

                if (item.id in owned) {
                    throw IllegalStateException("你已經領取過「${item.name}」囉！")
                }

                val nextOwned = (owned + item.id).distinct().take(20)
                val nextEquipped = (equipped + item.id).distinct().take(20)
                val existingPlacements = (snapshot.get("wardPlacements") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.mapNotNull { WardPlacement.decode(it) }
                    .orEmpty()
                val (dx, dy) = RehabCatalog.defaultWardPosition(item.id)
                val nextPlacements = (existingPlacements.filterNot { it.itemId == item.id } +
                    WardPlacement(item.id, dx, dy)).take(20).map { it.encode() }

                transaction.update(
                    profileRef,
                    mapOf(
                        "ownedItemIds" to nextOwned,
                        "equippedItemIds" to nextEquipped,
                        "wardPlacements" to nextPlacements,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun toggleEquipPharmacyItem(userId: String, itemId: String): Result<Boolean> {
        val docPath = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val profileRef = db.collection("users").document(uid)
            var nowEquipped = false

            db.runTransaction { transaction ->
                val snapshot = transaction.get(profileRef)
                val owned = (snapshot.get("ownedItemIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                val equipped = (snapshot.get("equippedItemIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                val existingPlacements = (snapshot.get("wardPlacements") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.mapNotNull { WardPlacement.decode(it) }
                    .orEmpty()

                if (itemId !in owned) {
                    throw IllegalStateException("需要先在院內藥局領取此道具喔！")
                }

                val isCurrentlyInWard = itemId in equipped || existingPlacements.any { it.itemId == itemId }
                val nextEquipped: List<String>
                val nextPlacements: List<String>
                if (isCurrentlyInWard) {
                    nowEquipped = false
                    nextEquipped = equipped.filterNot { it == itemId }
                    nextPlacements = existingPlacements
                        .filterNot { it.itemId == itemId }
                        .map { it.encode() }
                } else {
                    nowEquipped = true
                    nextEquipped = (equipped + itemId).distinct().take(20)
                    val (dx, dy) = RehabCatalog.defaultWardPosition(itemId)
                    val merged = existingPlacements.ifEmpty {
                        equipped.map { id ->
                            val (px, py) = RehabCatalog.defaultWardPosition(id)
                            WardPlacement(id, px, py)
                        }
                    }
                    nextPlacements = (merged.filterNot { it.itemId == itemId } +
                        WardPlacement(itemId, dx, dy)).take(20).map { it.encode() }
                }

                transaction.update(
                    profileRef,
                    mapOf(
                        "equippedItemIds" to nextEquipped,
                        "wardPlacements" to nextPlacements,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(nowEquipped)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateWardItemPosition(
        userId: String,
        itemId: String,
        xFraction: Float,
        yFraction: Float
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val profileRef = db.collection("users").document(uid)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(profileRef)
                val owned = (snapshot.get("ownedItemIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                if (itemId !in owned) return@runTransaction

                val equipped = (snapshot.get("equippedItemIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                val existingPlacements = (snapshot.get("wardPlacements") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.mapNotNull { WardPlacement.decode(it) }
                    .orEmpty()
                    .ifEmpty {
                        equipped.map { id ->
                            val (px, py) = RehabCatalog.defaultWardPosition(id)
                            WardPlacement(id, px, py)
                        }
                    }

                val updated = WardPlacement(itemId, xFraction, yFraction)
                val nextPlacements = (existingPlacements.filterNot { it.itemId == itemId } + updated)
                    .take(20)
                    .map { it.encode() }
                val nextEquipped = (equipped + itemId).distinct().take(20)

                transaction.update(
                    profileRef,
                    mapOf(
                        "equippedItemIds" to nextEquipped,
                        "wardPlacements" to nextPlacements,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun checkLocalCloudConflict(userId: String): SyncConflictInfo? {
        val store = guestStore ?: return null
        val uid = auth.currentUser?.uid ?: return null
        if (uid != userId) return null
        return try {
            if (store.getSyncedCloudUid() == uid) return null
            val localState = store.readLocalStateSnapshot()
            val localProfile = localState.profile ?: return null

            val profileRef = db.collection("users").document(uid)
            val profileSnap = profileRef.get().await()
            if (!profileSnap.exists()) return null

            val cloudProfile = profileSnap.toObject(
                RehabProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return null
            val cloudTasks = profileRef.collection("tasks").get().await()
                .toObjects(RehabTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val cloudCheckins = profileRef.collection("checkins").get().await()
                .toObjects(RehabCheckin::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            if (isUntouchedStarterLocalState(localProfile, localState.tasks, localState.checkins)) {
                store.overwriteLocalFromCloud(uid, cloudProfile, cloudTasks, cloudCheckins)
                return null
            }

            if (isStateContentEquivalent(
                    localProfile, localState.tasks, localState.checkins,
                    cloudProfile, cloudTasks, cloudCheckins
                )
            ) {
                store.setSyncedCloudUid(uid)
                return null
            }

            SyncConflictInfo(
                userId = uid,
                localSummary = buildSaveSummary(localProfile, localState.tasks, localState.checkins),
                cloudSummary = buildSaveSummary(cloudProfile, cloudTasks, cloudCheckins)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("RehabRepository", "Failed to inspect local vs cloud save conflict", e)
            null
        }
    }

    override suspend fun resolveConflictChooseLocal(userId: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val store = guestStore ?: return Result.success(Unit)
            val localState = store.readLocalStateSnapshot()
            val localProfile = localState.profile
                ?: return Result.failure(IllegalStateException("找不到本機療程存檔。"))

            val profileRef = db.collection("users").document(uid)
            val tasksCol = profileRef.collection("tasks")
            val checkinsCol = profileRef.collection("checkins")

            val cloudProfileSnap = profileRef.get().await()
            val cloudTasksSnap = tasksCol.get().await()
            val cloudCheckinsSnap = checkinsCol.get().await()

            val todayDate = currentTodayDateKey()
            val cleanTodayDate = localProfile.todayDate.takeIf { it.length == 10 } ?: todayDate
            val cleanTodayCount = if (cleanTodayDate == todayDate) {
                localProfile.todayCount.coerceIn(0L, 999L)
            } else {
                0L
            }
            val cleanNickname = normalizeDefaultDoctorName(localProfile.feifeiNickname).take(30)
            val cleanDisplayName = localProfile.displayName.trim()
                .ifBlank { auth.currentUser?.displayName?.trim().orEmpty() }
                .ifBlank { "復健小夥伴" }
                .take(60)
            val cleanOwned = localProfile.ownedItemIds.distinct().take(20)
            val cleanEquipped = localProfile.equippedItemIds.distinct().take(20)
            val cleanPlacements = localProfile.wardPlacements.take(20)

            val batchOps = mutableListOf<(com.google.firebase.firestore.WriteBatch) -> Unit>()

            if (cloudProfileSnap.exists()) {
                val updatePayload = mapOf(
                    "displayName" to cleanDisplayName,
                    "feifeiNickname" to cleanNickname,
                    "todayDate" to cleanTodayDate,
                    "todayCount" to cleanTodayCount,
                    "totalCompleted" to localProfile.totalCompleted.coerceAtLeast(0L),
                    "antibodyBalance" to localProfile.antibodyBalance.coerceIn(0L, 9_999_999L),
                    "ownedItemIds" to cleanOwned,
                    "equippedItemIds" to cleanEquipped,
                    "wardPlacements" to cleanPlacements,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batchOps += { batch -> batch.update(profileRef, updatePayload) }
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanDisplayName,
                    "feifeiNickname" to cleanNickname,
                    "todayDate" to cleanTodayDate,
                    "todayCount" to cleanTodayCount,
                    "totalCompleted" to localProfile.totalCompleted.coerceAtLeast(0L),
                    "antibodyBalance" to localProfile.antibodyBalance.coerceIn(0L, 9_999_999L),
                    "ownedItemIds" to cleanOwned,
                    "equippedItemIds" to cleanEquipped,
                    "wardPlacements" to cleanPlacements,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batchOps += { batch -> batch.set(profileRef, createPayload) }
            }

            cloudTasksSnap.documents.forEach { oldTaskDoc ->
                batchOps += { batch -> batch.delete(oldTaskDoc.reference) }
            }

            localState.tasks.forEach { task ->
                val title = task.title.trim().take(100)
                if (title.isEmpty()) return@forEach
                val effort = EffortLevel.fromWire(task.effortLevel).wireValue
                val category = task.category.trim().take(30).ifEmpty { TaskCategory.DAILY.wireValue }
                val safeResetHour = if (category == TaskCategory.GAME.wireValue) task.resetHour.coerceIn(0L, 23L) else 0L
                val safeDeadline = if (category == TaskCategory.HOMEWORK.wireValue) task.deadlineDate.trim().take(10) else ""
                val newTaskRef = tasksCol.document()
                val taskPayload = mapOf(
                    "userId" to uid,
                    "title" to title,
                    "effortLevel" to effort,
                    "category" to category,
                    "completed" to task.completed,
                    "completedDate" to task.completedDate.trim().take(10),
                    "note" to task.note.trim().take(200),
                    "sortOrder" to task.sortOrder,
                    "timeSlotsEnabled" to task.timeSlotsEnabled,
                    "morningDone" to task.morningDone,
                    "noonDone" to task.noonDone,
                    "eveningDone" to task.eveningDone,
                    "morningEnabled" to task.morningEnabled,
                    "noonEnabled" to task.noonEnabled,
                    "eveningEnabled" to task.eveningEnabled,
                    "resetHour" to safeResetHour,
                    "deadlineDate" to safeDeadline,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batchOps += { batch -> batch.set(newTaskRef, taskPayload) }
            }

            val existingCheckinIds = cloudCheckinsSnap.documents.map { it.id }.toSet()
            val keptDateKeys = mutableSetOf<String>()
            localState.checkins.forEach { checkin ->
                val dateKey = checkin.dateKey.trim()
                if (dateKey.length != 10 || !dateKey.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) return@forEach
                keptDateKeys += dateKey
                val energy = EnergyLevel.fromWire(checkin.energyLevel).wireValue
                val percent = checkin.resolvedPercent().coerceIn(0, 100)
                val completedTitles = checkin.completedTaskTitles
                    .map { it.trim().take(120) }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .take(100)
                val checkinRef = checkinsCol.document(dateKey)
                if (dateKey in existingCheckinIds) {
                    val updatePayload = mapOf(
                        "energyLevel" to energy,
                        "energyPercent" to percent,
                        "reflection" to checkin.reflection.trim().take(300),
                        "wardSnapshot" to checkin.wardSnapshot.take(20),
                        "doctorPoseSnapshot" to checkin.doctorPoseSnapshot.take(128),
                        "completedTaskTitles" to completedTitles,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batchOps += { batch -> batch.update(checkinRef, updatePayload) }
                } else {
                    val createPayload = mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "energyLevel" to energy,
                        "energyPercent" to percent,
                        "reflection" to checkin.reflection.trim().take(300),
                        "wardSnapshot" to checkin.wardSnapshot.take(20),
                        "doctorPoseSnapshot" to checkin.doctorPoseSnapshot.take(128),
                        "completedTaskTitles" to completedTitles,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batchOps += { batch -> batch.set(checkinRef, createPayload) }
                }
            }

            cloudCheckinsSnap.documents
                .filter { it.id !in keptDateKeys }
                .forEach { extraDoc ->
                    batchOps += { batch -> batch.delete(extraDoc.reference) }
                }

            batchOps.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { op -> op(batch) }
                batch.commit().await()
            }

            resetExpiredTasksInternal(uid)
            store.setSyncedCloudUid(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun resolveConflictChooseCloud(userId: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val store = guestStore ?: return Result.success(Unit)
            val profileRef = db.collection("users").document(uid)
            val profileSnap = profileRef.get().await()
            val cloudProfile = profileSnap.toObject(
                RehabProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val cloudTasks = profileRef.collection("tasks").get().await()
                .toObjects(RehabTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val cloudCheckins = profileRef.collection("checkins").get().await()
                .toObjects(RehabCheckin::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            if (cloudProfile != null && (cloudProfile.feifeiNickname.isBlank() || cloudProfile.feifeiNickname == "主治醫師")) {
                profileRef.update(
                    mapOf(
                        "feifeiNickname" to "主治醫生",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            store.overwriteLocalFromCloud(uid, cloudProfile, cloudTasks, cloudCheckins)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.READ, docPath)
            Result.failure(e)
        }
    }

    override suspend fun mirrorCloudBackupIfSynced(
        userId: String,
        profile: RehabProfile?,
        tasks: List<RehabTask>,
        checkins: List<RehabCheckin>
    ) {
        val store = guestStore ?: return
        if (profile == null) return
        if (auth.currentUser?.uid != userId) return
        try {
            if (store.getSyncedCloudUid() == userId) {
                store.overwriteLocalFromCloud(userId, profile, tasks, checkins)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("RehabRepository", "Failed to mirror cloud backup locally", e)
        }
    }

    private fun normalizeDefaultDoctorName(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.isEmpty() || trimmed == "主治醫師") "主治醫生" else trimmed
    }

    private fun isUntouchedStarterLocalState(
        profile: RehabProfile,
        tasks: List<RehabTask>,
        checkins: List<RehabCheckin>
    ): Boolean {
        if (profile.totalCompleted > 0L || profile.todayCount > 0L) return false
        if (profile.ownedItemIds.isNotEmpty() || profile.equippedItemIds.isNotEmpty()) return false
        if (checkins.isNotEmpty()) return false
        val nick = normalizeDefaultDoctorName(profile.feifeiNickname)
        if (nick != "主治醫生") return false
        val starterSet = RehabCatalog.starterTasks.map { Triple(it.first.trim(), it.second.wireValue, it.third.trim()) }.toSet()
        return tasks.all { t ->
            !t.completed &&
                !t.morningDone &&
                !t.noonDone &&
                !t.eveningDone &&
                !t.timeSlotsEnabled &&
                t.deadlineDate.isBlank() &&
                Triple(t.title.trim(), EffortLevel.fromWire(t.effortLevel).wireValue, t.note.trim()) in starterSet
        }
    }

    private fun isStateContentEquivalent(
        localProfile: RehabProfile,
        localTasks: List<RehabTask>,
        localCheckins: List<RehabCheckin>,
        cloudProfile: RehabProfile,
        cloudTasks: List<RehabTask>,
        cloudCheckins: List<RehabCheckin>
    ): Boolean {
        if (normalizeDefaultDoctorName(localProfile.feifeiNickname) !=
            normalizeDefaultDoctorName(cloudProfile.feifeiNickname)
        ) return false
        if (localProfile.totalCompleted != cloudProfile.totalCompleted) return false
        if (localProfile.ownedItemIds.toSet() != cloudProfile.ownedItemIds.toSet()) return false
        if (localProfile.equippedItemIds.toSet() != cloudProfile.equippedItemIds.toSet()) return false
        if (localProfile.wardPlacements.toSet() != cloudProfile.wardPlacements.toSet()) return false

        fun taskSignatures(list: List<RehabTask>) = list.map { t ->
            listOf(
                t.title.trim(),
                EffortLevel.fromWire(t.effortLevel).wireValue,
                t.category.trim().ifEmpty { TaskCategory.DAILY.wireValue },
                t.completed.toString(),
                t.completedDate.trim(),
                t.note.trim(),
                t.timeSlotsEnabled.toString(),
                t.morningDone.toString(),
                t.noonDone.toString(),
                t.eveningDone.toString(),
                t.morningEnabled.toString(),
                t.noonEnabled.toString(),
                t.eveningEnabled.toString(),
                t.resetHour.toString(),
                t.deadlineDate.trim()
            ).joinToString("|")
        }.sorted()

        if (taskSignatures(localTasks) != taskSignatures(cloudTasks)) return false

        fun checkinSignatures(list: List<RehabCheckin>) = list.map { c ->
            listOf(
                c.dateKey.trim(),
                EnergyLevel.fromWire(c.energyLevel).wireValue,
                c.resolvedPercent().toString(),
                c.reflection.trim(),
                c.wardSnapshot.joinToString(","),
                c.doctorPoseSnapshot.trim(),
                c.completedTaskTitles.joinToString(",")
            ).joinToString("|")
        }.sorted()

        return checkinSignatures(localCheckins) == checkinSignatures(cloudCheckins)
    }

    private fun buildSaveSummary(
        profile: RehabProfile,
        tasks: List<RehabTask>,
        checkins: List<RehabCheckin>
    ): SyncSaveSummary {
        val latestDate = checkins.maxOfOrNull { it.dateKey }
            ?: tasks.mapNotNull { it.completedDate.takeIf { d -> d.isNotBlank() } }.maxOrNull()
            ?: profile.todayDate
        return SyncSaveSummary(
            totalCompleted = profile.totalCompleted,
            todayCount = profile.todayCount,
            taskCount = tasks.size,
            completedTaskCount = tasks.count { it.completed },
            checkinCount = checkins.size,
            latestDateKey = latestDate
        )
    }
}
private fun handleFirestoreError(
    e: Throwable,
    operationType: OperationType? = null,
    path: String? = null
): Throwable {
    val auth = Firebase.auth
    val user = auth.currentUser
    val payload = JSONObject().apply {
        put("error", e.message ?: "Unknown Firestore error")
        put("operationType", (operationType ?: OperationType.UNKNOWN).name.lowercase())
        put("path", path ?: JSONObject.NULL)
        put(
            "authInfo",
            JSONObject().apply {
                put("userId", user?.uid ?: JSONObject.NULL)
                put("email", user?.email ?: JSONObject.NULL)
                put("emailVerified", user?.isEmailVerified ?: false)
                put("isAnonymous", user?.isAnonymous ?: false)
                put("tenantId", user?.tenantId ?: JSONObject.NULL)
                put(
                    "providerInfo",
                    user?.providerData?.joinToString(", ") {
                        "${it.providerId}:${it.uid}"
                    } ?: ""
                )
            }
        )
    }
    Log.e("FirestoreError", payload.toString(), e)
    return e
}
enum class OperationType {
    CREATE, READ, WRITE, UPDATE, DELETE, LIST, GET, UNKNOWN
}
