export interface PlaceOrderRequest {
  items: Array<{
    menuItemId: string;
    quantity: number;
  }>;
  idempotencyKey: string;
}

export interface PlaceOrderResponse {
  orderId: string;
}

export interface OrderItemSnapshot {
  menuItemId: string;
  name: string;
  pricePaise: number;
  quantity: number;
  subtotalPaise: number;
}

export interface AuthoritativeMenuItem {
  id: string;
  categoryId: string;
  name: string;
  pricePaise: number;
  isAvailable: boolean;
}

export interface AuthoritativeCategory {
  id: string;
  enabled: boolean;
}

export interface AuthoritativeUser {
  uid: string;
  email: string;
  displayName: string;
  enabled: boolean;
  role: string;
}

export interface NormalizedCartItem {
  menuItemId: string;
  quantity: number;
}
