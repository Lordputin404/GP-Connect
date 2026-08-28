package com.gumlapolytechnic.gpconnect.data.model

/**
 * The five academic departments that actually exist at Gumla Polytechnic.
 *
 * This enum is the single source of truth for department identity in the app.
 * The enum **name** is the canonical id persisted to Firestore (in
 * `users.department`, `signupRequests.department` and notice audiences), and it
 * is mirrored verbatim by the `departmentIds()` allow-list in `firestore.rules`.
 * Adding or renaming a constant here therefore requires the same edit there.
 *
 * [displayName] is presentation-only and must never be persisted.
 */
enum class Department(val displayName: String, val shortName: String) {
    COMPUTER_APPLICATIONS("Computer Applications", "BCA"),
    MECHANICAL_ENGINEERING("Mechanical Engineering", "Mechanical"),
    CIVIL_ENGINEERING("Civil Engineering", "Civil"),
    MINING_ENGINEERING("Mining Engineering", "Mining"),
    ELECTRICAL_ENGINEERING("Electrical Engineering", "Electrical");

    /** Canonical persisted identifier. */
    val id: String get() = name

    /** Courses this department offers. */
    val courses: List<Course> get() = Course.entries.filter { it.department == this }

    companion object {
        /** Strict lookup by canonical id — returns null for anything else. */
        fun fromIdOrNull(value: String?): Department? =
            entries.firstOrNull { it.name == value }

        /**
         * Tolerant lookup used when reading Firestore. Accepts the canonical id
         * and also the legacy free-text display labels ("Computer Applications",
         * "Mechanical Engineering", …) that pre-existing profiles and notice
         * audiences were written with, so old documents keep resolving.
         */
        fun resolveOrNull(value: String?): Department? {
            val key = value?.normalizeKey() ?: return null
            if (key.isEmpty()) return null
            return entries.firstOrNull { department ->
                key == department.name.normalizeKey() ||
                    key == department.displayName.normalizeKey() ||
                    key == department.shortName.normalizeKey()
            }
        }

        /**
         * Display label for a stored value. Falls back to the raw stored string
         * so an unrecognised legacy value is still shown rather than hidden.
         */
        fun labelFor(value: String?): String =
            resolveOrNull(value)?.displayName ?: value.orEmpty()
    }
}

/**
 * Courses offered by the college. A course is a **separate** concept from a
 * department: a department is an organisational unit that owns staff and
 * student intake, a course is the programme a student is enrolled in.
 * [department] records which department offers the course.
 *
 * Enum names are the canonical persisted ids, and each constant's name/
 * [department] pairing is mirrored by the `courseDepartments()` map in
 * `firestore.rules` — the rules validate the pair, not just the id, so adding or
 * re-homing a course requires the same edit there.
 */
enum class Course(val displayName: String, val department: Department) {
    BCA("BCA", Department.COMPUTER_APPLICATIONS),
    DIPLOMA_MECHANICAL("Diploma in Mechanical Engineering", Department.MECHANICAL_ENGINEERING),
    DIPLOMA_CIVIL("Diploma in Civil Engineering", Department.CIVIL_ENGINEERING),
    DIPLOMA_MINING("Diploma in Mining Engineering", Department.MINING_ENGINEERING),
    DIPLOMA_ELECTRICAL("Diploma in Electrical Engineering", Department.ELECTRICAL_ENGINEERING);

    /** Canonical persisted identifier. */
    val id: String get() = name

    companion object {
        fun fromIdOrNull(value: String?): Course? = entries.firstOrNull { it.name == value }

        /** Tolerant lookup, mirroring [Department.resolveOrNull]. */
        fun resolveOrNull(value: String?): Course? {
            val key = value?.normalizeKey() ?: return null
            if (key.isEmpty()) return null
            return entries.firstOrNull { course ->
                key == course.name.normalizeKey() ||
                    key == course.displayName.normalizeKey() ||
                    key == LEGACY_LABELS[course]?.normalizeKey()
            }
        }

        fun labelFor(value: String?): String = resolveOrNull(value)?.displayName ?: value.orEmpty()

        /** Course labels written by the pre-canonical notice form. */
        private val LEGACY_LABELS = mapOf(
            DIPLOMA_MECHANICAL to "Diploma in Mechanical",
            DIPLOMA_CIVIL to "Diploma in Civil",
            DIPLOMA_MINING to "Diploma in Mining",
            DIPLOMA_ELECTRICAL to "Diploma in Electrical",
        )
    }
}

/** Semesters a diploma/BCA student can be in. Matches the notice form's range. */
val SEMESTER_RANGE = 1..6

private fun String.normalizeKey(): String =
    uppercase().filter { it.isLetterOrDigit() }
