package com.gumlapolytechnic.gpconnect.data.firebase

import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.repository.NoticeDraft
import com.gumlapolytechnic.gpconnect.data.repository.NoticeQuery
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import com.gumlapolytechnic.gpconnect.data.repository.applyQuery
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

/**
 * Firestore-backed notice source. Students and admins share the same
 * notices collection; module-scoped write authority is enforced by the
 * Firestore security rules (firestore.rules in the project root), while this
 * repository applies search/category/module filtering and pinned-first
 * ordering client-side. Read markers live under users/{uid}/noticeReads.
 *
 * Listener errors (for example rules rejecting access) currently surface as
 * an empty snapshot rather than a crash; the error-state UI path stays
 * available for future refinement.
 */
class FirebaseNoticeRepository : NoticeRepository {

    private val firestore get() = FirebaseServices.firestore
    private val auth get() = FirebaseServices.auth

    override fun observeNotices(query: NoticeQuery): Flow<List<Notice>> = callbackFlow {
        val registration: ListenerRegistration =
            firestore.collection(NOTICES).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notices = snapshot?.documents
                    ?.mapNotNull { it.toNotice() }
                    .orEmpty()
                    .applyQuery(query)
                trySend(notices)
            }
        awaitClose { registration.remove() }
    }

    override fun observeNotice(id: String): Flow<Notice?> = callbackFlow {
        val registration: ListenerRegistration =
            firestore.collection(NOTICES).document(id).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toNotice())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getNotice(id: String): Notice? =
        firestore.collection(NOTICES).document(id).get().awaitTask()?.toNotice()

    override suspend fun createNotice(draft: NoticeDraft): Notice {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Cannot create a notice while signed out")
        val now = System.currentTimeMillis()
        val fields = noticeFields(
            title = draft.title,
            body = draft.body,
            category = draft.category,
            isPinned = draft.isPinned,
            audience = draft.audience,
            attachments = draft.attachments,
            author = draft.author,
            createdAt = draft.createdAt,
            updatedAt = now,
            createdBy = uid,
            ownerRole = draft.ownerRole,
            module = draft.module,
        )
        val reference = firestore.collection(NOTICES).add(fields).awaitTask()
        return Notice(
            id = reference.id,
            title = draft.title,
            body = draft.body,
            category = draft.category,
            isPinned = draft.isPinned,
            audience = draft.audience,
            attachments = draft.attachments,
            author = draft.author,
            createdAt = draft.createdAt,
            updatedAt = now,
            createdBy = uid,
            ownerRole = draft.ownerRole,
            module = draft.module,
        )
    }

    override suspend fun updateNotice(notice: Notice) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Signed out")
        val fields = noticeFields(
            title = notice.title,
            body = notice.body,
            category = notice.category,
            isPinned = notice.isPinned,
            audience = notice.audience,
            attachments = notice.attachments,
            author = notice.author,
            createdAt = notice.createdAt,
            updatedAt = System.currentTimeMillis(),
            createdBy = notice.createdBy,
            ownerRole = notice.ownerRole,
            module = notice.module,
        )
        firestore.collection(NOTICES).document(notice.id).set(fields).awaitTask()
    }

    override suspend fun deleteNotice(noticeId: String) {
        firestore.collection(NOTICES).document(noticeId).delete().awaitTask()
    }

    override suspend fun setPinned(noticeId: String, pinned: Boolean) {
        firestore.collection(NOTICES).document(noticeId)
            .update("isPinned", pinned, "updatedAt", System.currentTimeMillis())
            .awaitTask()
    }

    override fun observeReadMarkerIds(): Flow<Set<String>> {
        val uid = auth.currentUser?.uid ?: return flow { emit(emptySet()) }
        return callbackFlow {
            val registration: ListenerRegistration =
                firestore.collection(USERS).document(uid)
                    .collection(READS)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptySet())
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.documents?.map { it.id }?.toSet().orEmpty())
                    }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun markRead(noticeId: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection(USERS).document(uid)
            .collection(READS).document(noticeId)
            .set(mapOf("readAt" to System.currentTimeMillis()))
            .awaitTask()
    }

    private companion object {
        const val NOTICES = "notices"
        const val USERS = "users"
        const val READS = "noticeReads"
    }
}
