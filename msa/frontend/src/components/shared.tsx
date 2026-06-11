import { CheckCircle2, Search } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import type { FormEvent, ReactNode } from 'react';
import type { OrderForm, RequestState } from '../api';
import { formatMoney, readField } from '../api';

export function StatusTile({ label, value }: { label: string; value: string }) {
  return (
    <article className="status-tile">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

export function ActionPanel({
  title,
  icon: Icon,
  onSubmit,
  children,
}: {
  title: string;
  icon: LucideIcon;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void | Promise<void>;
  children: ReactNode;
}) {
  return (
    <section className="panel">
      <div className="section-title">
        <Icon size={18} aria-hidden="true" />
        <h2>{title}</h2>
      </div>
      <form className="stack" onSubmit={onSubmit}>
        {children}
        <button className="primary-button" type="submit">
          <CheckCircle2 size={16} aria-hidden="true" />
          <span>실행</span>
        </button>
      </form>
    </section>
  );
}

export function SharedOrderFields({
  form,
  setForm,
}: {
  form: OrderForm;
  setForm: (value: OrderForm) => void;
}) {
  return (
    <>
      <input
        aria-label="상품 ID"
        placeholder="Product ID"
        value={form.productId}
        onChange={(event) => setForm({ ...form, productId: event.target.value })}
        required
      />
      <input
        aria-label="수량"
        placeholder="Quantity"
        type="number"
        min="1"
        value={form.quantity}
        onChange={(event) => setForm({ ...form, quantity: event.target.value })}
        required
      />
    </>
  );
}

export function ReadOnlyEndpoint({ title, endpoint }: { title: string; endpoint: string }) {
  return (
    <section className="panel muted-panel">
      <div className="section-title">
        <Search size={18} aria-hidden="true" />
        <h2>{title}</h2>
      </div>
      <code>{endpoint}</code>
    </section>
  );
}

export function ProductsTable({ products, productRows }: { products: RequestState<unknown>; productRows: unknown[] }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>상품명</th>
            <th>카테고리</th>
            <th>가격</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          {products.loading ? (
            <SkeletonRow colSpan={5} />
          ) : productRows.length > 0 ? (
            productRows.map((product, index) => (
              <tr key={`${readField(product, ['productId', 'id'])}-${index}`}>
                <td>{readField(product, ['productId', 'id'])}</td>
                <td>{readField(product, ['productName', 'name'])}</td>
                <td>{readField(product, ['productCategory', 'category'])}</td>
                <td>{formatMoney(readField(product, ['price'], '0'))}</td>
                <td>{readField(product, ['status', 'productStatus'])}</td>
              </tr>
            ))
          ) : (
            <EmptyRow colSpan={5} label={products.error || '조회된 상품이 없습니다.'} />
          )}
        </tbody>
      </table>
    </div>
  );
}

export function ProductSkeleton() {
  return (
    <article className="product-card skeleton-card">
      <span className="loading-block visual" />
      <span className="loading-block short" />
      <span className="loading-block long" />
      <span className="loading-block mid" />
    </article>
  );
}

export function SkeletonRow({ colSpan }: { colSpan: number }) {
  return (
    <tr>
      <td colSpan={colSpan}>
        <span className="loading-line" />
      </td>
    </tr>
  );
}

export function EmptyRow({ colSpan, label }: { colSpan: number; label: string }) {
  return (
    <tr>
      <td colSpan={colSpan} className="empty-cell">
        {label}
      </td>
    </tr>
  );
}
