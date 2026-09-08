import * as admin from "firebase-admin";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {PlaceOrderRequest, PlaceOrderResponse, OrderItemSnapshot, AuthoritativeMenuItem, AuthoritativeCategory, AuthoritativeUser} from "./types";
import {BUSINESS_LIMITS, validateAndNormalizeRequest} from "./validation";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

export const placeCanteenOrder = onCall<PlaceOrderRequest, Promise<PlaceOrderResponse>>(
    {
      enforceAppCheck: false,
    },
    async (request): Promise<PlaceOrderResponse> => {
      // 1. Authentication
      if (!request.auth) {
        throw new HttpsError("unauthenticated", "Authentication required to place an order.");
      }
      const uid = request.auth.uid;

      // 2. Authorization - load trusted user profile
      const userSnap = await db.collection("users").doc(uid).get();
      if (!userSnap.exists) {
        throw new HttpsError("permission-denied", "Your account is not configured for canteen orders.");
      }

      const userData = userSnap.data() as AuthoritativeUser;
      if (userData.enabled !== true) {
        throw new HttpsError("permission-denied", "Your account has been disabled.");
      }
      if (userData.role !== "STUDENT") {
        throw new HttpsError("permission-denied", "Only students may place canteen orders.");
      }

      // 3. Input validation & duplicate merging
      const {items: normalizedItems, idempotencyKey} = validateAndNormalizeRequest(request.data);

      // 4. Firestore transaction for atomic checkout
      const orderRef = db.collection("canteenOrders").doc();
      const idempotencyRef = db.collection("canteenIdempotencyKeys").doc(idempotencyKey);

      try {
        const result = await db.runTransaction(async (transaction) => {
          // --- Check idempotency key ---
          const idempotencySnap = await transaction.get(idempotencyRef);
          if (idempotencySnap.exists) {
            const existingData = idempotencySnap.data()!;
            if (existingData.customerId !== uid) {
              throw new HttpsError("already-exists", "This idempotency key is already associated with another user.");
            }
            // Return existing order for same user
            return {orderId: existingData.orderId, alreadyExists: true};
          }

          // --- Fetch authoritative menu items ---
          const menuRefs = normalizedItems.map((item) => db.collection("canteenMenuItems").doc(item.menuItemId));
          const menuSnaps = await transaction.getAll(...menuRefs);

          const snapshots: OrderItemSnapshot[] = [];
          let totalAmountPaise = 0;

          for (let i = 0; i < normalizedItems.length; i++) {
            const normalized = normalizedItems[i];
            const menuSnap = menuSnaps[i];

            if (!menuSnap.exists) {
              throw new HttpsError("not-found", `Item ${normalized.menuItemId} no longer exists.`);
            }

            const menuData = menuSnap.data() as AuthoritativeMenuItem;
            if (menuData.isAvailable !== true) {
              throw new HttpsError("failed-precondition", `Item ${menuData.name} is currently out of stock.`);
            }

            // Validate price
            const pricePaise = menuData.pricePaise;
            if (
                typeof pricePaise !== "number" ||
                !Number.isInteger(pricePaise) ||
                pricePaise <= 0 ||
                pricePaise > BUSINESS_LIMITS.MAX_UNIT_PRICE_PAISE
            ) {
              throw new HttpsError("internal", `Invalid price data for ${menuData.name}.`);
            }

            // Optional: validate category is enabled
            if (menuData.categoryId) {
              const categorySnap = await transaction.get(db.collection("canteenCategories").doc(menuData.categoryId));
              if (categorySnap.exists) {
                const catData = categorySnap.data() as AuthoritativeCategory;
                if (catData.enabled !== true) {
                  throw new HttpsError("failed-precondition", `Category for ${menuData.name} is currently unavailable.`);
                }
              }
            }

            const subtotalPaise = pricePaise * normalized.quantity;
            totalAmountPaise += subtotalPaise;

            snapshots.push({
              menuItemId: menuSnap.id,
              name: menuData.name,
              pricePaise,
              quantity: normalized.quantity,
              subtotalPaise,
            });
          }

          // --- Total limit check ---
          if (totalAmountPaise > BUSINESS_LIMITS.MAX_ORDER_TOTAL_PAISE) {
            throw new HttpsError("out-of-range", `Order total exceeds maximum allowable limit.`);
          }

          // --- Create order document ---
          const now = admin.firestore.Timestamp.now().toMillis();
          const orderData = {
            customerId: uid,
            customerName: userData.displayName || userData.email,
            customerEmail: userData.email,
            items: snapshots,
            totalAmountPaise,
            status: "PENDING",
            createdAt: now,
            updatedAt: now,
          };

          transaction.set(orderRef, orderData);

          // --- Create idempotency record ---
          transaction.set(idempotencyRef, {
            orderId: orderRef.id,
            customerId: uid,
            createdAt: now,
          });

          return {orderId: orderRef.id, alreadyExists: false};
        });

        return {orderId: result.orderId};
      } catch (error) {
        if (error instanceof HttpsError) {
          throw error;
        }
        console.error("Checkout error:", error);
        throw new HttpsError("internal", "Could not process order. Please try again.");
      }
    }
);