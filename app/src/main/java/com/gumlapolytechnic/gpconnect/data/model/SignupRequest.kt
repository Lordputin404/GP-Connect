package com.gumlapolytechnic.gpconnect.data.model

/** Lifecycle of a signup request. Enum names are persisted verbatim. */
enum class SignupRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
}

/**
 * Data an applicant fills in on the signup form. Passwords are NEVER part of
 * this object: the password goes straight to Firebase Authentication and is
 * never written to Firestore.
 *
 * [course] and [semester] are only meaningful for a STUDENT applicant.
 */
data class SignupSubmission(
    val name: String,
    val email: String,
    val requestedRole: UserRole,
    val department: Department,
    val course: Course? = null,
    val semester: Int? = null,
    val rollNo: String? = null,
)

/**
 * A pending or decided request to join the college app, stored at
 * `signupRequests/{uid}` where `uid` is the applicant's Firebase Auth uid.
 * Using the uid as the document id makes a duplicate request impossible and
 * lets security rules pair the request with `users/{uid}` without a query.
 *
 * The document mirrors the applicant's department so that the department HOD can
 * be granted a server-enforced, department-filtered query over the collection.
 */
data class SignupRequest(
    val uid: String,
    val email: String,
    val name: String,
    val requestedRole: UserRole,
    val department: String,
    val course: String? = null,
    val semester: Int? = null,
    val rollNo: String? = null,
    val status: SignupRequestStatus = SignupRequestStatus.PENDING,
    val createdAt: Long = 0L,
    val decidedAt: Long? = null,
    val decidedBy: String? = null,
    val decisionNote: String? = null,
) {
    val isPending: Boolean get() = status == SignupRequestStatus.PENDING

    /** Resolved department, tolerating legacy display-label values. */
    val departmentOrNull: Department? get() = Department.resolveOrNull(department)

    /** Resolved course, tolerating legacy display-label values. */
    val courseOrNull: Course? get() = Course.resolveOrNull(course)
}

/**
 * Newest-first ordering with pending requests always on top. Applied on the
 * client so the department-filtered query needs no composite Firestore index.
 */
internal fun List<SignupRequest>.sortedForInbox(): List<SignupRequest> = sortedWith(
    compareByDescending<SignupRequest> { it.isPending }.thenByDescending { it.createdAt },
)
