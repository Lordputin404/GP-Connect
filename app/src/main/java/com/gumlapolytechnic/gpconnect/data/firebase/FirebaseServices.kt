package com.gumlapolytechnic.gpconnect.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Single access point for Firebase service instances. Only repository
 * implementations in this package may use it — ViewModels and composables
 * must never touch Firebase directly (they depend on the repository
 * interfaces in data/repository).
 *
 * Phase 4A foundation: nothing calls this object yet — the app still runs on
 * the mock repositories. The getters throw only if Firebase failed to
 * initialize, which cannot happen until the real google-services.json is
 * supplied and the Firebase repositories replace the mocks in Phase 4B/4C.
 * Initialization itself is automatic: Firebase's own ContentProvider reads
 * the generated resources at app start; no manual initialization exists
 * anywhere in GPConnectApplication.
 */
object FirebaseServices {
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage get() = FirebaseStorage.getInstance()
}
