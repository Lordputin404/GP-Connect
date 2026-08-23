package com.gumlapolytechnic.gpconnect.data.mock

import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole

/**
 * Fictional demo accounts — project rule: no real student personal data.
 */
val DemoStudent = User(
    id = "student-1001",
    name = "Amar Kujur",
    rollNo = "BCA-2023-051",
    course = "BCA",
    semester = 5,
    role = UserRole.STUDENT,
    department = "Computer Applications",
)
