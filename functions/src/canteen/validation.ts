import {HttpsError} from "firebase-functions/v2/https";
import {NormalizedCartItem} from "./types";

const UUID_V4_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export const BUSINESS_LIMITS = {
  MAX_RAW_ITEMS: 20,
  MAX_QUANTITY_PER_ITEM: 99,
  MAX_UNIT_PRICE_PAISE: 100000,
  MAX_ORDER_TOTAL_PAISE: 500000,
};

export function validateAndNormalizeRequest(data: unknown): { items: NormalizedCartItem[]; idempotencyKey: string } {
  if (!data || typeof data !== "object") {
    throw new HttpsError("invalid-argument", "Request payload must be a valid JSON object.");
  }

  const payload = data as Record<string, unknown>;

  // Validate idempotencyKey
  const idempotencyKey = payload.idempotencyKey;
  if (typeof idempotencyKey !== "string" || idempotencyKey.trim().length === 0 || idempotencyKey.length > 64) {
    throw new HttpsError("invalid-argument", "idempotencyKey must be a non-empty string up to 64 characters.");
  }
  if (!UUID_V4_REGEX.test(idempotencyKey.trim())) {
    throw new HttpsError("invalid-argument", "idempotencyKey must be a valid UUID v4 format.");
  }

  // Validate items array
  const rawItems = payload.items;
  if (!Array.isArray(rawItems)) {
    throw new HttpsError("invalid-argument", "items must be an array.");
  }

  if (rawItems.length === 0) {
    throw new HttpsError("invalid-argument", "Cart cannot be empty.");
  }

  if (rawItems.length > BUSINESS_LIMITS.MAX_RAW_ITEMS) {
    throw new HttpsError("invalid-argument", `Cart cannot exceed ${BUSINESS_LIMITS.MAX_RAW_ITEMS} distinct items.`);
  }

  // Parse and normalize items (merge duplicates)
  const itemMap = new Map<string, number>();

  for (const raw of rawItems) {
    if (!raw || typeof raw !== "object") {
      throw new HttpsError("invalid-argument", "Each item must be an object.");
    }

    const item = raw as Record<string, unknown>;
    const menuItemId = item.menuItemId;
    const quantity = item.quantity;

    if (typeof menuItemId !== "string" || menuItemId.trim().length === 0) {
      throw new HttpsError("invalid-argument", "menuItemId must be a non-empty string.");
    }

    if (typeof quantity !== "number" || !Number.isInteger(quantity) || quantity <= 0) {
      throw new HttpsError("invalid-argument", "quantity must be a positive integer.");
    }

    const trimmedId = menuItemId.trim();
    const existingQty = itemMap.get(trimmedId) || 0;
    itemMap.set(trimmedId, existingQty + quantity);
  }

  const normalizedItems: NormalizedCartItem[] = [];

  for (const [menuItemId, totalQty] of itemMap.entries()) {
    if (totalQty > BUSINESS_LIMITS.MAX_QUANTITY_PER_ITEM) {
      throw new HttpsError(
          "invalid-argument",
          `Total quantity for item ${menuItemId} cannot exceed ${BUSINESS_LIMITS.MAX_QUANTITY_PER_ITEM}.`,
      );
    }
    normalizedItems.push({menuItemId, quantity: totalQty});
  }

  return {
    items: normalizedItems,
    idempotencyKey: idempotencyKey.trim(),
  };
}
