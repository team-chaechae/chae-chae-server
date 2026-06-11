export type ApiEnvelope<T> = {
  code?: number;
  message?: string;
  data?: T;
};

export type RequestState<T = unknown> = {
  loading: boolean;
  data: T | null;
  error: string | null;
};

export type ProductForm = {
  name: string;
  category: string;
  price: string;
};

export type OrderForm = {
  productId: string;
  quantity: string;
};

export type SaleForm = {
  userId: string;
  productId: string;
  quantity: string;
};

export type AuthForm = {
  email: string;
  password: string;
};

export type ProductRecord = {
  id: string;
  name: string;
  category: string;
  price: number | null;
  status: string;
  raw: unknown;
};

export type CartItem = ProductRecord & {
  quantity: number;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

function tokenHeaders(): HeadersInit {
  const accessToken = localStorage.getItem('chae.accessToken');
  const userId = localStorage.getItem('chae.userId');

  return {
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    ...(userId ? { 'User-Id': userId } : {}),
  };
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<ApiEnvelope<T>> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...tokenHeaders(),
      ...init?.headers,
    },
  });

  const text = await response.text();
  const payload = text ? safeJsonParse<ApiEnvelope<T>>(text) : {};

  if (!response.ok) {
    const message = payload?.message || `${response.status} ${response.statusText}`;
    throw new Error(message);
  }

  return payload;
}

export function safeJsonParse<T>(value: string): T {
  try {
    return JSON.parse(value) as T;
  } catch {
    return { message: value } as T;
  }
}

export function unwrapList(value: unknown): unknown[] {
  if (Array.isArray(value)) {
    return value;
  }

  if (value && typeof value === 'object') {
    const objectValue = value as Record<string, unknown>;
    for (const key of ['content', 'items', 'products', 'orders', 'sales', 'inventory']) {
      const nestedValue = objectValue[key];
      if (Array.isArray(nestedValue)) {
        return nestedValue;
      }
    }
  }

  return [];
}

export function readField(item: unknown, fields: string[], fallback = '-'): string {
  if (!item || typeof item !== 'object') {
    return fallback;
  }

  const objectValue = item as Record<string, unknown>;
  for (const field of fields) {
    const value = objectValue[field];
    if (value !== undefined && value !== null && value !== '') {
      return String(value);
    }
  }

  return fallback;
}

export function formatMoney(value: unknown): string {
  const numericValue = Number(value);

  if (!Number.isFinite(numericValue)) {
    return '-';
  }

  return numericValue.toLocaleString('ko-KR');
}

export function toProductRecord(item: unknown, index: number): ProductRecord {
  const id = readField(item, ['productId', 'id'], String(index + 1));
  const name = readField(item, ['productName', 'name'], `상품 ${id}`);
  const category = readField(item, ['productCategory', 'category'], '기타');
  const status = readField(item, ['status', 'productStatus'], '판매중');
  const rawPrice = readField(item, ['price'], '');
  const numericPrice = Number(rawPrice);

  return {
    id,
    name,
    category,
    price: Number.isFinite(numericPrice) ? numericPrice : null,
    status,
    raw: item,
  };
}
