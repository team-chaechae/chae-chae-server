import { Boxes, CheckCircle2, CircleDollarSign, ClipboardList, PackagePlus, RefreshCw, Search, ShoppingBag, Truck } from 'lucide-react';
import type { FormEvent } from 'react';
import type { ApiEnvelope, OrderForm, ProductForm, RequestState, SaleForm } from '../api';
import { apiRequest } from '../api';
import { ActionPanel, ProductsTable, ReadOnlyEndpoint, SharedOrderFields, StatusTile } from './shared';

export type ConsoleTab = 'products' | 'inventory' | 'orders' | 'sales' | 'payment';

export function ConsoleView({
  action,
  activeTab,
  orderForm,
  paymentSalesId,
  productForm,
  productQuery,
  productRows,
  products,
  saleForm,
  stockProductId,
  createOrder,
  createProduct,
  createSale,
  loadProducts,
  lookupPayment,
  lookupStock,
  receiveStock,
  runAction,
  setOrderForm,
  setPaymentSalesId,
  setProductForm,
  setProductQuery,
  setSaleForm,
  setStockProductId,
}: {
  action: RequestState<ApiEnvelope<unknown>>;
  activeTab: ConsoleTab;
  orderForm: OrderForm;
  paymentSalesId: string;
  productForm: ProductForm;
  productQuery: string;
  productRows: unknown[];
  products: RequestState<unknown>;
  saleForm: SaleForm;
  stockProductId: string;
  createOrder: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  createProduct: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  createSale: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  loadProducts: () => Promise<void>;
  lookupPayment: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  lookupStock: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  receiveStock: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  runAction: <T>(task: () => Promise<ApiEnvelope<T>>) => Promise<ApiEnvelope<T> | null>;
  setOrderForm: (value: OrderForm) => void;
  setPaymentSalesId: (value: string) => void;
  setProductForm: (value: ProductForm) => void;
  setProductQuery: (value: string) => void;
  setSaleForm: (value: SaleForm) => void;
  setStockProductId: (value: string) => void;
}) {
  return (
    <>
      <header className="topbar">
        <div>
          <p className="eyebrow">API Gateway</p>
          <h1>운영 콘솔</h1>
        </div>
        <button className="ghost-button" type="button" onClick={loadProducts} title="상품 새로고침">
          <RefreshCw size={17} aria-hidden="true" />
          <span>새로고침</span>
        </button>
      </header>

      <section className="status-grid" aria-label="서비스 상태">
        <StatusTile label="Gateway" value="/api -> 8180" />
        <StatusTile label="Products" value={`${productRows.length} loaded`} />
        <StatusTile label="Last action" value={action.loading ? 'running' : action.error ? 'failed' : 'ready'} />
      </section>

      {activeTab === 'products' && (
        <div className="two-column">
          <section className="panel">
            <div className="section-title">
              <ShoppingBag size={18} aria-hidden="true" />
              <h2>상품 검색</h2>
            </div>
            <form
              className="inline-form"
              onSubmit={(event) => {
                event.preventDefault();
                void loadProducts();
              }}
            >
              <input
                aria-label="상품명 검색"
                placeholder="상품명"
                value={productQuery}
                onChange={(event) => setProductQuery(event.target.value)}
              />
              <button className="primary-button" type="submit" disabled={products.loading}>
                <Search size={16} aria-hidden="true" />
                <span>조회</span>
              </button>
            </form>
            <ProductsTable products={products} productRows={productRows} />
          </section>

          <section className="panel">
            <div className="section-title">
              <PackagePlus size={18} aria-hidden="true" />
              <h2>상품 등록</h2>
            </div>
            <form className="stack" onSubmit={createProduct}>
              <input
                aria-label="상품명"
                placeholder="상품명"
                value={productForm.name}
                onChange={(event) => setProductForm({ ...productForm, name: event.target.value })}
                required
              />
              <input
                aria-label="카테고리"
                placeholder="카테고리"
                value={productForm.category}
                onChange={(event) => setProductForm({ ...productForm, category: event.target.value })}
                required
              />
              <input
                aria-label="가격"
                placeholder="가격"
                type="number"
                min="0"
                value={productForm.price}
                onChange={(event) => setProductForm({ ...productForm, price: event.target.value })}
                required
              />
              <button className="primary-button" type="submit" disabled={action.loading}>
                <CheckCircle2 size={16} aria-hidden="true" />
                <span>등록</span>
              </button>
            </form>
          </section>
        </div>
      )}

      {activeTab === 'inventory' && (
        <div className="two-column">
          <ActionPanel title="재고 조회" icon={Boxes} onSubmit={lookupStock}>
            <input
              aria-label="재고 상품 ID"
              placeholder="Product ID"
              value={stockProductId}
              onChange={(event) => setStockProductId(event.target.value)}
              required
            />
          </ActionPanel>
          <ActionPanel title="입고 처리" icon={Truck} onSubmit={receiveStock}>
            <SharedOrderFields form={orderForm} setForm={setOrderForm} />
          </ActionPanel>
        </div>
      )}

      {activeTab === 'orders' && (
        <div className="two-column">
          <ActionPanel title="발주 생성" icon={Truck} onSubmit={createOrder}>
            <SharedOrderFields form={orderForm} setForm={setOrderForm} />
          </ActionPanel>
          <ReadOnlyEndpoint title="발주 검색" endpoint="/api/orders?size=20" />
        </div>
      )}

      {activeTab === 'sales' && (
        <div className="two-column">
          <ActionPanel title="판매 생성" icon={ClipboardList} onSubmit={createSale}>
            <input
              aria-label="사용자 ID"
              placeholder="User ID"
              value={saleForm.userId}
              onChange={(event) => setSaleForm({ ...saleForm, userId: event.target.value })}
              required
            />
            <input
              aria-label="판매 상품 ID"
              placeholder="Product ID"
              value={saleForm.productId}
              onChange={(event) => setSaleForm({ ...saleForm, productId: event.target.value })}
              required
            />
            <input
              aria-label="판매 수량"
              placeholder="Quantity"
              type="number"
              min="1"
              value={saleForm.quantity}
              onChange={(event) => setSaleForm({ ...saleForm, quantity: event.target.value })}
              required
            />
          </ActionPanel>
          <ReadOnlyEndpoint title="판매 검색" endpoint="/api/sales?size=20" />
        </div>
      )}

      {activeTab === 'payment' && (
        <div className="two-column">
          <ActionPanel title="결제 상태" icon={CircleDollarSign} onSubmit={lookupPayment}>
            <input
              aria-label="판매 ID"
              placeholder="Sales ID"
              value={paymentSalesId}
              onChange={(event) => setPaymentSalesId(event.target.value)}
              required
            />
          </ActionPanel>
          <section className="panel">
            <div className="section-title">
              <CircleDollarSign size={18} aria-hidden="true" />
              <h2>Toss 설정</h2>
            </div>
            <button className="secondary-button" type="button" onClick={() => void runAction(() => apiRequest('/api/payment/toss/config'))}>
              <Search size={16} aria-hidden="true" />
              <span>조회</span>
            </button>
          </section>
        </div>
      )}
    </>
  );
}
