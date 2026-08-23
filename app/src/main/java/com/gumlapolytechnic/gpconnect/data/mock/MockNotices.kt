package com.gumlapolytechnic.gpconnect.data.mock

import com.gumlapolytechnic.gpconnect.data.model.Attachment
import com.gumlapolytechnic.gpconnect.data.model.Audience
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.model.NoticeCategory

/**
 * Fictional Gumla Polytechnic demo notices — all names, dates and details are
 * invented for the prototype (project rule: no real student/college data).
 * Dates are computed relative to "now" so the demo always feels current.
 */
object MockNotices {

    private const val DAY_MS = 86_400_000L

    private fun daysAgo(days: Int): Long = System.currentTimeMillis() - days * DAY_MS

    val all: List<Notice> = listOf(
        Notice(
            id = "n-001",
            title = "Semester End Examination Schedule — November 2026",
            body = "The detailed schedule for the Semester End Examinations (November 2026) " +
                "has been published. Examinations begin on 16 November and conclude on " +
                "28 November 2026. Students must carry their college identity card and " +
                "hall ticket to every examination. Seating plans will be displayed on the " +
                "notice board one day before each paper. The complete schedule with room " +
                "allocations is attached.",
            category = NoticeCategory.EXAM,
            isPinned = true,
            audience = Audience.All,
            attachments = listOf(Attachment("Semester_Exam_Schedule.pdf")),
            author = "Examination Cell",
            createdAt = daysAgo(1),
        ),
        Notice(
            id = "n-002",
            title = "College Closed — Durga Puja Holidays",
            body = "All academic and office activities of the institute will remain closed " +
                "from 17 October to 22 October 2026 on account of Durga Puja. The college " +
                "reopens on 23 October 2026 and classes resume as per the regular timetable. " +
                "Students staying in the hostel are advised to register at the warden's " +
                "office before leaving for the holidays.",
            category = NoticeCategory.HOLIDAY,
            isPinned = true,
            audience = Audience.All,
            author = "Office of the Principal",
            createdAt = daysAgo(2),
        ),
        Notice(
            id = "n-003",
            title = "Annual Tech Fest \"Srijan 2026\" — Registrations Open",
            body = "Registrations are now open for Srijan 2026, the annual technical festival " +
                "of Gumla Polytechnic. Events include coding contests, robotics workshops, " +
                "project exhibition and a technical quiz. Students of all departments may " +
                "participate individually or in teams of up to three. Registration forms are " +
                "available with the Student Affairs office until 30 September 2026.",
            category = NoticeCategory.EVENT,
            isPinned = false,
            audience = Audience.All,
            author = "Student Affairs",
            createdAt = daysAgo(3),
        ),
        Notice(
            id = "n-004",
            title = "Internal Assessment II — Timetable and Instructions",
            body = "Internal Assessment II for BCA Semester 5 will be conducted from " +
                "12 October to 14 October 2026 during regular class hours. The syllabus " +
                "includes all units covered until 8 October. Students must be seated ten " +
                "minutes before the scheduled start. Any absence requires prior approval " +
                "from the Head of Department.",
            category = NoticeCategory.EXAM,
            isPinned = false,
            audience = Audience.Course(course = "BCA", semester = 5),
            author = "Department of Computer Applications",
            createdAt = daysAgo(4),
        ),
        Notice(
            id = "n-005",
            title = "Revised Library Timings — Winter Semester",
            body = "With effect from 1 October 2026, the Central Library will remain open " +
                "from 9:00 AM to 6:30 PM on all working days, and from 10:00 AM to 2:00 PM " +
                "on Saturdays. The reading hall will close fifteen minutes before closing " +
                "time. Book return dates falling on holidays are automatically extended to " +
                "the next working day.",
            category = NoticeCategory.LIBRARY,
            isPinned = false,
            audience = Audience.Department(department = "Computer Applications"),
            attachments = listOf(Attachment("Library_Notice.pdf")),
            author = "Central Library",
            createdAt = daysAgo(5),
        ),
        Notice(
            id = "n-006",
            title = "DBMS Assignment 3 — Submission Deadline",
            body = "Assignment 3 of Database Management Systems is due on 10 October 2026. " +
                "Submissions must include the ER diagram, normalized schema and PL/SQL " +
                "scripts. Late submissions attract a penalty of 10% per day. Submit " +
                "handwritten or printed reports to the department office before 4:00 PM.",
            category = NoticeCategory.ASSIGNMENT,
            isPinned = false,
            audience = Audience.Course(course = "BCA", semester = 5),
            author = "Department of Computer Applications",
            createdAt = daysAgo(6),
        ),
        Notice(
            id = "n-007",
            title = "Campus Cleanliness Drive — Volunteer Registration",
            body = "The NSS unit is organising a campus cleanliness drive on Sunday, " +
                "18 October 2026, from 7:00 AM. Volunteers will receive certificates of " +
                "participation. Interested students may register their names with the NSS " +
                "programme officer by 15 October 2026. Gloves and cleaning equipment will " +
                "be provided at the venue.",
            category = NoticeCategory.GENERAL,
            isPinned = false,
            audience = Audience.All,
            author = "NSS Programme Office",
            createdAt = daysAgo(7),
        ),
        Notice(
            id = "n-008",
            title = "Identity Card Reissue — Office Hours",
            body = "Students who have lost or damaged their identity cards can apply for a " +
                "reissue at the administrative office between 11:00 AM and 1:00 PM on " +
                "working days. A copy of the FIR (for lost cards) and a passport-size " +
                "photograph are required. Reissued cards will be distributed after seven " +
                "working days.",
            category = NoticeCategory.GENERAL,
            isPinned = false,
            audience = Audience.All,
            author = "Administrative Office",
            createdAt = daysAgo(9),
        ),
        Notice(
            id = "n-009",
            title = "Inter-Polytechnic Football Tournament — Team Selection",
            body = "Selection trials for the inter-polytechnic football tournament will be " +
                "held on 20 October 2026 at 4:00 PM on the main ground. Players must bring " +
                "their own kit and report to the sports in-charge thirty minutes early. The " +
                "final squad of sixteen players will be announced on the sports notice " +
                "board the following day.",
            category = NoticeCategory.EVENT,
            isPinned = false,
            audience = Audience.All,
            author = "Sports Committee",
            createdAt = daysAgo(10),
        ),
        Notice(
            id = "n-010",
            title = "New Arrivals — Computer Science Section",
            body = "Thirty-two new titles have been added to the Computer Science section of " +
                "the Central Library, including recent editions on database systems, " +
                "operating systems and web technologies. The catalogue is available at the " +
                "issue counter. Books from the new arrivals shelf may be borrowed for the " +
                "standard fourteen-day period.",
            category = NoticeCategory.LIBRARY,
            isPinned = false,
            audience = Audience.Department(department = "Computer Applications"),
            author = "Central Library",
            createdAt = daysAgo(12),
        ),
        Notice(
            id = "n-011",
            title = "Republic Day — Institution Holiday",
            body = "The institute will remain closed on 26 January 2027 on the occasion of " +
                "Republic Day. The flag-hoisting ceremony will be held at 8:00 AM on the " +
                "main lawn, and all students and staff members are cordially invited to " +
                "attend. Classes resume as per the regular timetable from the following " +
                "day.",
            category = NoticeCategory.HOLIDAY,
            isPinned = false,
            audience = Audience.All,
            author = "Office of the Principal",
            createdAt = daysAgo(14),
        ),
        Notice(
            id = "n-012",
            title = "Mini Project Proposal — Submission Guidelines",
            body = "BCA Semester 5 students must submit their mini project proposals on or " +
                "before 25 October 2026. Each proposal should describe the problem " +
                "statement, proposed technology stack and team composition (maximum two " +
                "members). Proposals will be reviewed by the department panel and " +
                "approval status will be communicated through the mentors.",
            category = NoticeCategory.ASSIGNMENT,
            isPinned = false,
            audience = Audience.Course(course = "BCA", semester = 5),
            author = "Department of Computer Applications",
            createdAt = daysAgo(16),
        ),
        Notice(
            id = "n-013",
            title = "Practical Examination Guidelines and Lab Slots",
            body = "Practical examinations for BCA Semester 5 are scheduled from 2 November " +
                "to 6 November 2026. Lab-wise slots have been assigned and are displayed " +
                "on the department notice board. Students must bring their lab records, " +
                " duly signed, and arrive at least fifteen minutes before their slot. " +
                "Re-slotting requests will not be entertained.",
            category = NoticeCategory.EXAM,
            isPinned = false,
            audience = Audience.Course(course = "BCA", semester = 5),
            author = "Examination Cell",
            createdAt = daysAgo(18),
        ),
        Notice(
            id = "n-014",
            title = "Post-Matric Scholarship — Application Forms Available",
            body = "Application forms for the state post-matric scholarship for the current " +
                "academic session are now available at the scholarship cell. Students must " +
                "submit completed forms along with caste, income and residence certificates " +
                "before 31 October 2026. Incomplete applications will be rejected without " +
                "further correspondence.",
            category = NoticeCategory.GENERAL,
            isPinned = false,
            audience = Audience.All,
            author = "Scholarship Cell",
            createdAt = daysAgo(20),
        ),
    )
}
