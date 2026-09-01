import {expect} from "chai";
import "mocha";
import {validateAndNormalizeRequest, BUSINESS_LIMITS} from "../../canteen/validation";
import {HttpsError} from "firebase-functions/v2/https";

describe("validateAndNormalizeRequest", () => {
  const validIdempotencyKey = "a8f3b2c1-9d2e-4f5a-8b3c-1d2e3f4a5b6c";

  describe("Authentication & basic validation", () => {
    it("rejects null payload", () => {
      expect(() => validateAndNormalizeRequest(null)).to.throw(HttpsError, "Request payload must be a valid JSON object.");
    });

    it("rejects non-object payload", () => {
      expect(() => validateAndNormalizeRequest("string")).to.throw(HttpsError, "Request payload must be a valid JSON object.");
    });

    it("rejects missing items array", () => {
      expect(() => validateAndNormalizeRequest({idempotencyKey: validIdempotencyKey})).to.throw(HttpsError, "items must be an array.");
    });

    it("rejects empty items array", () => {
      expect(() => validateAndNormalizeRequest({items: [], idempotencyKey: validIdempotencyKey})).to.throw(HttpsError, "Cart cannot be empty.");
    });

    it("rejects more than 20 raw entries", () => {
      const items = Array.from({length: 21}, (_, i) => ({menuItemId: `item${i}`, quantity: 1}));
      expect(() => validateAndNormalizeRequest({items, idempotencyKey: validIdempotencyKey})).to.throw(HttpsError, "Cart cannot exceed 20 distinct items.");
    });
  });

  describe("idempotencyKey validation", () => {
    it("rejects missing idempotencyKey", () => {
      expect(() => validateAndNormalizeRequest({items: [{menuItemId: "a", quantity: 1}], idempotencyKey: ""})).to.throw(HttpsError, "idempotencyKey must be a non-empty string up to 64 characters.");
    });

    it("rejects invalid UUID format", () => {
      expect(() => validateAndNormalizeRequest({items: [{menuItemId: "a", quantity: 1}], idempotencyKey: "not-a-uuid"})).to.throw(HttpsError, "idempotencyKey must be a valid UUID v4 format.");
    });

    it("accepts valid UUID v4", () => {
      const result = validateAndNormalizeRequest({items: [{menuItemId: "a", quantity: 1}], idempotencyKey: validIdempotencyKey});
      expect(result.idempotencyKey).to.equal(validIdempotencyKey);
    });
  });

  describe("item validation", () => {
    it("rejects item without menuItemId", () => {
      expect(() => validateAndNormalizeRequest({items: [{quantity: 1}], idempotencyKey: validIdempotencyKey})).to.throw(HttpsError, "menuItemId must be a non-empty string.");
    });

    it("rejects empty menuItemId", () => {
      expect(() => validateAndNormalizeRequest({items: [{menuItemId: "", quantity: 1}], idempotencyKey: validIdempotencyKey})).to.throw(HttpsError, "menuItemId must be a non-empty string.");
    });

    it("rejects quantity 0", () => {
      expect(() => validateAndNormalizeRequest({items: [{menuItemId: "a", quantity: 0}], idempotencyKey: validIdempotencyKey})).to.throw(HttpsError, "quantity must be a positive integer.");
    });

    it("rejects negative quantity", () => {
      expect(() => validateAndNormalizeRequest({items: [{menuItemId: "a", quantity: -1}], idempotencyKey: validIdempotencyKey})).to.throw(HttpsError, "quantity must be a positive integer.");
    });

    it("rejects non-integer quantity", () => {
      expect(() => validateAndNormalizeRequest({items: [{menuItemId: "a", quantity: 1.5}], idempotencyKey: validIdempotencyKey})).to.throw(HttpsError, "quantity must be a positive integer.");
    });

    it("rejects quantity > 99", () => {
      expect(() => validateAndNormalizeRequest({items: [{menuItemId: "a", quantity: 100}], idempotencyKey: validIdempotencyKey})).to.throw(HttpsError, "Total quantity for item a cannot exceed 99.");
    });
  });

  describe("duplicate merging", () => {
    it("merges duplicate menuItemIds", () => {
      const payload = {
        items: [
          {menuItemId: "A", quantity: 2},
          {menuItemId: "A", quantity: 3},
          {menuItemId: "B", quantity: 1},
        ],
        idempotencyKey: validIdempotencyKey,
      };
      const result = validateAndNormalizeRequest(payload);
      expect(result.items).to.have.lengthOf(2);
      const itemA = result.items.find(item => item.menuItemId === "A");
      const itemB = result.items.find(item => item.menuItemId === "B");
      expect(itemA!.quantity).to.equal(5);
      expect(itemB!.quantity).to.equal(1);
    });

    it("rejects merged quantity exceeding 99", () => {
      const payload = {
        items: [
          {menuItemId: "A", quantity: 60},
          {menuItemId: "A", quantity: 60},
        ],
        idempotencyKey: validIdempotencyKey,
      };
      expect(() => validateAndNormalizeRequest(payload)).to.throw(HttpsError, "Total quantity for item A cannot exceed 99.");
    });
  });

  describe("pricing calculation safeguards", () => {
    it("MAX_UNIT_PRICE_PAISE is 100000", () => {
      expect(BUSINESS_LIMITS.MAX_UNIT_PRICE_PAISE).to.equal(100000);
    });

    it("MAX_ORDER_TOTAL_PAISE is 500000", () => {
      expect(BUSINESS_LIMITS.MAX_ORDER_TOTAL_PAISE).to.equal(500000);
    });

    it("MAX_QUANTITY_PER_ITEM is 99", () => {
      expect(BUSINESS_LIMITS.MAX_QUANTITY_PER_ITEM).to.equal(99);
    });
  });
});