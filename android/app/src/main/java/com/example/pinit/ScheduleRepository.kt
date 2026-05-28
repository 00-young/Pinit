package com.example.pinit

import com.google.firebase.firestore.FirebaseFirestore

class ScheduleRepository {

    private val db = FirebaseFirestore.getInstance()

    // 일정 추가
    fun addSchedule(
        scheduleData: Map<String, Any>,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .add(scheduleData)
            .addOnSuccessListener { doc ->
                onSuccess(doc.id)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // 특정 유저의 일정 조회
    fun getSchedulesByUser(
        userId: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->

                val schedules = result.documents.mapNotNull { doc ->
                    doc.data?.plus("scheduleId" to doc.id)
                }

                onSuccess(schedules)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // 커뮤니티의 전체 일정 조회
    fun getAllSchedules(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .get()
            .addOnSuccessListener { result ->

                val schedules = result.documents.mapNotNull { doc ->
                    doc.data?.plus("scheduleId" to doc.id)
                }

                onSuccess(schedules)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // 특정 일정 조회
    fun getScheduleById(
        scheduleId: String,
        onSuccess: (Map<String, Any>?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .document(scheduleId)
            .get()
            .addOnSuccessListener { doc ->
                onSuccess(doc.data?.plus("scheduleId" to doc.id))
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // 일정 수정
    fun updateSchedule(
        scheduleId: String,
        updateData: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .document(scheduleId)
            .update(updateData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // 일정 삭제
    // 일정 삭제 + 내부 days/items 전체 삭제
    fun deleteSchedule(
        scheduleId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val scheduleRef = db.collection("schedules").document(scheduleId)

        scheduleRef.collection("days")
            .get()
            .addOnSuccessListener { daysSnapshot ->

                val batch = db.batch()
                val dayDocs = daysSnapshot.documents

                if (dayDocs.isEmpty()) {
                    batch.delete(scheduleRef)

                    batch.commit()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e) }

                    return@addOnSuccessListener
                }

                var completedDayCount = 0

                dayDocs.forEach { dayDoc ->

                    dayDoc.reference.collection("items")
                        .get()
                        .addOnSuccessListener { itemsSnapshot ->

                            itemsSnapshot.documents.forEach { itemDoc ->
                                batch.delete(itemDoc.reference)
                            }

                            batch.delete(dayDoc.reference)

                            completedDayCount++

                            if (completedDayCount == dayDocs.size) {
                                batch.delete(scheduleRef)

                                batch.commit()
                                    .addOnSuccessListener { onSuccess() }
                                    .addOnFailureListener { e -> onFailure(e) }
                            }
                        }
                        .addOnFailureListener { e ->
                            onFailure(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Day 추가
    fun addDay(
        scheduleId: String,
        dayId: String,
        dayData: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("days")
            .document(dayId)
            .set(dayData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Day 조회
    fun getDays(
        scheduleId: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("days")
            .orderBy("dayNumber")
            .get()
            .addOnSuccessListener { result ->
                val days = result.documents.mapNotNull { doc ->
                    doc.data?.plus("dayId" to doc.id)
                }
                onSuccess(days)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // 특정 Day 조회
    fun getDayById(
        scheduleId: String,
        dayId: String,
        onSuccess: (Map<String, Any>?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("days")
            .document(dayId)
            .get()
            .addOnSuccessListener { doc ->

                onSuccess(
                    doc.data?.plus("dayId" to doc.id)
                )
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Day + 내부 items 전체 삭제
    fun deleteDay(
        scheduleId: String,
        dayId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        val dayRef = db.collection("schedules")
            .document(scheduleId)
            .collection("days")
            .document(dayId)

        // items 먼저 조회
        dayRef.collection("items")
            .get()
            .addOnSuccessListener { result ->

                val batch = db.batch()

                // items 전부 batch delete
                result.documents.forEach { document ->
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener {

                        // items 삭제 후 day 삭제
                        dayRef.delete()
                            .addOnSuccessListener {
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                onFailure(e)
                            }
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // item 추가
    fun addItem(
        scheduleId: String,
        dayId: String,
        itemData: Map<String, Any>,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("days")
            .document(dayId)
            .collection("items")
            .add(itemData)
            .addOnSuccessListener { doc ->
                onSuccess(doc.id)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Day별 item 조회
    fun getItemsByDay(
        scheduleId: String,
        dayId: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("days")
            .document(dayId)
            .collection("items")
            .orderBy("visitOrder")
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.mapNotNull { doc ->
                    doc.data?.plus("itemId" to doc.id)
                }
                onSuccess(items)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // 특정 item 조회
    fun getItemById(
        scheduleId: String,
        dayId: String,
        itemId: String,
        onSuccess: (Map<String, Any>?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("days")
            .document(dayId)
            .collection("items")
            .document(itemId)
            .get()
            .addOnSuccessListener { doc ->

                onSuccess(
                    doc.data?.plus("itemId" to doc.id)
                )
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // item 수정
    fun updateItem(
        scheduleId: String,
        dayId: String,
        itemId: String,
        updateData: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("days")
            .document(dayId)
            .collection("items")
            .document(itemId)
            .update(updateData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // item 삭제
    fun deleteItem(
        scheduleId: String,
        dayId: String,
        itemId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("schedules")
            .document(scheduleId)
            .collection("days")
            .document(dayId)
            .collection("items")
            .document(itemId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // 게시글 <-> 일정
    fun createPostWithSchedule(
        postData: Map<String, Any>,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("posts")
            .add(postData)
            .addOnSuccessListener { doc ->
                onSuccess(doc.id)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // 일정 + day + item 전체 조회
    fun getScheduleDetail(
        scheduleId: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        val scheduleRef =
            db.collection("schedules")
                .document(scheduleId)

        scheduleRef.get()
            .addOnSuccessListener { scheduleDoc ->

                val scheduleData =
                    scheduleDoc.data?.toMutableMap()

                if (scheduleData == null) {

                    onFailure(Exception("일정이 존재하지 않습니다."))
                    return@addOnSuccessListener
                }

                scheduleData["scheduleId"] =
                    scheduleDoc.id

                // days 조회
                scheduleRef.collection("days")
                    .get()
                    .addOnSuccessListener { daySnapshot ->

                        val dayList =
                            mutableListOf<Map<String, Any>>()

                        val dayDocs =
                            daySnapshot.documents

                        if (dayDocs.isEmpty()) {

                            scheduleData["days"] =
                                emptyList<Map<String, Any>>()

                            onSuccess(scheduleData)
                            return@addOnSuccessListener
                        }

                        var completedDayCount = 0

                        dayDocs.forEach { dayDoc ->

                            val dayData =
                                dayDoc.data?.toMutableMap()
                                    ?: mutableMapOf()

                            dayData["dayId"] =
                                dayDoc.id

                            // items 조회
                            dayDoc.reference
                                .collection("items")
                                .get()
                                .addOnSuccessListener { itemSnapshot ->

                                    val itemList =
                                        mutableListOf<Map<String, Any>>()

                                    itemSnapshot.documents.forEach { itemDoc ->

                                        val itemData =
                                            itemDoc.data?.toMutableMap()
                                                ?: mutableMapOf()

                                        itemData["itemId"] =
                                            itemDoc.id

                                        itemList.add(itemData)
                                    }

                                    dayData["items"] =
                                        itemList

                                    dayList.add(dayData)

                                    completedDayCount++

                                    if (completedDayCount == dayDocs.size) {

                                        scheduleData["days"] =
                                            dayList

                                        onSuccess(scheduleData)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    onFailure(e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // 다른 유저의 특정 day를 내 일정에 복사
    fun copyDayToMySchedule(
        originalScheduleId: String,
        originalDayId: String,
        targetScheduleId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        val targetDaysRef = db.collection("schedules")
            .document(targetScheduleId)
            .collection("days")

        // 1. 내 일정의 현재 day 개수 조회
        targetDaysRef.get()
            .addOnSuccessListener { targetDaySnapshot ->

                val newDayNumber =
                    targetDaySnapshot.size() + 1

                val newDayId =
                    "day$newDayNumber"

                // 2. 원본 day 조회
                db.collection("schedules")
                    .document(originalScheduleId)
                    .collection("days")
                    .document(originalDayId)
                    .get()
                    .addOnSuccessListener { originalDayDoc ->

                        val originalDayData =
                            originalDayDoc.data?.toMutableMap()

                        if (originalDayData == null) {

                            onFailure(
                                Exception("원본 day가 존재하지 않습니다.")
                            )

                            return@addOnSuccessListener
                        }

                        // 새 dayNumber로 변경
                        originalDayData["dayNumber"] =
                            newDayNumber

                        // 3. 새 day 생성
                        targetDaysRef
                            .document(newDayId)
                            .set(originalDayData)
                            .addOnSuccessListener {

                                // 4. 원본 items 조회
                                originalDayDoc.reference
                                    .collection("items")
                                    .get()
                                    .addOnSuccessListener { itemSnapshot ->

                                        val batch = db.batch()

                                        itemSnapshot.documents.forEach { itemDoc ->

                                            val newItemRef =
                                                targetDaysRef
                                                    .document(newDayId)
                                                    .collection("items")
                                                    .document()

                                            batch.set(
                                                newItemRef,
                                                itemDoc.data ?: emptyMap<String, Any>()
                                            )
                                        }

                                        // 5. items 복사
                                        batch.commit()
                                            .addOnSuccessListener {

                                                onSuccess()
                                            }
                                            .addOnFailureListener { e ->

                                                onFailure(e)
                                            }
                                    }
                                    .addOnFailureListener { e ->

                                        onFailure(e)
                                    }
                            }
                            .addOnFailureListener { e ->

                                onFailure(e)
                            }
                    }
                    .addOnFailureListener { e ->

                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->

                onFailure(e)
            }
    }
    // 다른 유저의 전체 일정을 내 schedules에 새로 복사
    fun copyEntireSchedule(
        originalScheduleId: String,
        newUserId: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        val originalScheduleRef =
            db.collection("schedules")
                .document(originalScheduleId)

        // 1. 원본 schedule 조회
        originalScheduleRef.get()
            .addOnSuccessListener { scheduleDoc ->

                val originalScheduleData =
                    scheduleDoc.data?.toMutableMap()

                if (originalScheduleData == null) {

                    onFailure(
                        Exception("원본 일정이 존재하지 않습니다.")
                    )

                    return@addOnSuccessListener
                }

                // 새 userId로 변경
                originalScheduleData["userId"] =
                    newUserId

                originalScheduleData["createdAt"] =
                    System.currentTimeMillis()

                // 2. 새 schedule 생성
                val newScheduleRef =
                    db.collection("schedules")
                        .document()

                newScheduleRef
                    .set(originalScheduleData)
                    .addOnSuccessListener {

                        // 3. 원본 days 조회
                        originalScheduleRef
                            .collection("days")
                            .get()
                            .addOnSuccessListener { daySnapshot ->

                                val dayDocs =
                                    daySnapshot.documents

                                if (dayDocs.isEmpty()) {

                                    onSuccess(newScheduleRef.id)
                                    return@addOnSuccessListener
                                }

                                var completedDayCount = 0

                                dayDocs.forEach { dayDoc ->

                                    val dayData =
                                        dayDoc.data?.toMutableMap()
                                            ?: mutableMapOf()

                                    val originalDayId =
                                        dayDoc.id

                                    val newDayRef =
                                        newScheduleRef
                                            .collection("days")
                                            .document(originalDayId)

                                    // 4. day 복사
                                    newDayRef
                                        .set(dayData)
                                        .addOnSuccessListener {

                                            // 5. items 조회
                                            dayDoc.reference
                                                .collection("items")
                                                .get()
                                                .addOnSuccessListener { itemSnapshot ->

                                                    val batch =
                                                        db.batch()

                                                    itemSnapshot.documents.forEach { itemDoc ->

                                                        val newItemRef =
                                                            newDayRef
                                                                .collection("items")
                                                                .document()

                                                        batch.set(
                                                            newItemRef,
                                                            itemDoc.data
                                                                ?: emptyMap<String, Any>()
                                                        )
                                                    }

                                                    // 6. items 복사
                                                    batch.commit()
                                                        .addOnSuccessListener {

                                                            completedDayCount++

                                                            if (completedDayCount == dayDocs.size) {

                                                                onSuccess(
                                                                    newScheduleRef.id
                                                                )
                                                            }
                                                        }
                                                        .addOnFailureListener { e ->

                                                            onFailure(e)
                                                        }
                                                }
                                                .addOnFailureListener { e ->

                                                    onFailure(e)
                                                }
                                        }
                                        .addOnFailureListener { e ->

                                            onFailure(e)
                                        }
                                }
                            }
                            .addOnFailureListener { e ->

                                onFailure(e)
                            }
                    }
                    .addOnFailureListener { e ->

                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->

                onFailure(e)
            }
    }

}