package com.gumlapolytechnic.gpconnect.data.model

/**
 * Lifecycle status of a canteen order.
 *
 * State flow:
 * [PENDING] → [CONFIRMED] → [PREPARING] → [READY] → [COMPLETED]
 *
 * Any active state ([PENDING], [CONFIRMED], [PREPARING], [READY]) may transition to [CANCELLED]
 * subject to business rules and authorization policies.
 */
enum class OrderStatus {
    /** Placed by student, awaiting canteen admin confirmation. */
    PENDING,

    /** Acknowledged and accepted by canteen staff. */
    CONFIRMED,

    /** Food is currently being prepared in the kitchen. */
    PREPARING,

    /** Prepared and waiting at the canteen counter for student pickup. */
    READY,

    /** Collected by the student and closed. Terminal state. */
    COMPLETED,

    /** Terminated prior to completion. Terminal state. */
    CANCELLED,
    ;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED

    val isActive: Boolean
        get() = !isTerminal

    /**
     * Pure domain helper checking whether transitioning from this status to [target]
     * is allowed by the standard state machine rules.
     */
    fun canTransitionTo(target: OrderStatus): Boolean = when (this) {
        PENDING -> target == CONFIRMED || target == CANCELLED
        CONFIRMED -> target == PREPARING || target == CANCELLED
        PREPARING -> target == READY || target == CANCELLED
        READY -> target == COMPLETED || target == CANCELLED
        COMPLETED, CANCELLED -> false
    }
}