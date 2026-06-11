import {
  CheckCircle2,
  CircleDollarSign,
  ClipboardList,
  Loader2,
  LogIn,
  PackagePlus,
  ShieldCheck,
  ShoppingBag,
  Store,
  Truck,
  Boxes,
} from 'lucide-react';
import { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  ApiEnvelope,
  AuthForm,
  CartItem,
  OrderForm,
  ProductForm,
  RequestState,
  SaleForm,
  apiRequest,
  toProductRecord,
  unwrapList,
} from './api';
import { ConsoleTab, ConsoleView } from './components/ConsoleView';
import { ShopView } from './components/ShopView';

type AppMode = 'shop' | 'console';

const consoleTabs: Array<{ key: ConsoleTab; label: string; icon: typeof Boxes }> = [
  { key: 'products', label: '상품', icon: ShoppingBag },
  { key: 'inventory', label: '재고', icon: Boxes },
  { key: 'orders', label: '발주', icon: Truck },
  { key: 'sales', label: '판매', icon: ClipboardList },
  { key: 'payment', label: '결제', icon: CircleDollarSign },
];

const initialProducts: RequestState<unknown> = { loading: false, data: null, error: null };
const initialAction: RequestState<ApiEnvelope<unknown>> = { loading: false, data: null, error: null };

export function App() {
  const [mode, setMode] = useState<AppMode>('shop');
  const [activeTab, setActiveTab] = useState<ConsoleTab>('products');
  const [products, setProducts] = useState<RequestState<unknown>>(initialProducts);
  const [action, setAction] = useState<RequestState<ApiEnvelope<unknown>>>(initialAction);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [productQuery, setProductQuery] = useState('');
  const [stockProductId, setStockProductId] = useState('');
  const [paymentSalesId, setPaymentSalesId] = useState('');
  const [authForm, setAuthForm] = useState<AuthForm>({ email: '', password: '' });
  const [productForm, setProductForm] = useState<ProductForm>({ name: '', category: '', price: '' });
  const [orderForm, setOrderForm] = useState<OrderForm>({ productId: '', quantity: '1' });
  const [saleForm, setSaleForm] = useState<SaleForm>({ userId: '', productId: '', quantity: '1' });
  const [sessionUser, setSessionUser] = useState(() => localStorage.getItem('chae.userId') || '');

  const productRows = useMemo(() => unwrapList(products.data), [products.data]);
  const productCatalog = useMemo(() => productRows.map(toProductRecord), [productRows]);
  const cartTotal = cart.reduce((sum, item) => sum + (item.price ?? 0) * item.quantity, 0);
  const cartQuantity = cart.reduce((sum, item) => sum + item.quantity, 0);

  useEffect(() => {
    let ignore = false;

    async function loadInitialProducts() {
      setProducts({ loading: true, data: null, error: null });

      try {
        const result = await apiRequest<unknown>('/api/products?size=20');
        if (!ignore) {
          setProducts({ loading: false, data: result.data ?? result, error: null });
        }
      } catch (error) {
        if (!ignore) {
          setProducts({
            loading: false,
            data: null,
            error: error instanceof Error ? error.message : '상품 목록을 가져오지 못했습니다.',
          });
        }
      }
    }

    void loadInitialProducts();

    return () => {
      ignore = true;
    };
  }, []);

  async function runAction<T>(task: () => Promise<ApiEnvelope<T>>) {
    setAction({ loading: true, data: null, error: null });

    try {
      const result = await task();
      setAction({ loading: false, data: result as ApiEnvelope<unknown>, error: null });
      return result;
    } catch (error) {
      setAction({
        loading: false,
        data: null,
        error: error instanceof Error ? error.message : '요청 처리에 실패했습니다.',
      });
      return null;
    }
  }

  async function loadProducts() {
    setProducts({ loading: true, data: null, error: null });

    const searchParams = new URLSearchParams();
    if (productQuery.trim()) {
      searchParams.set('productName', productQuery.trim());
    }
    searchParams.set('size', '20');

    try {
      const result = await apiRequest<unknown>(`/api/products?${searchParams.toString()}`);
      setProducts({ loading: false, data: result.data ?? result, error: null });
    } catch (error) {
      setProducts({
        loading: false,
        data: null,
        error: error instanceof Error ? error.message : '상품 목록을 가져오지 못했습니다.',
      });
    }
  }

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const result = await runAction(() =>
      apiRequest<Record<string, unknown>>('/api/users/auth/login', {
        method: 'POST',
        body: JSON.stringify({ user: authForm }),
      }),
    );

    const data = result?.data;
    if (data && typeof data === 'object') {
      const objectData = data as Record<string, unknown>;
      const accessToken = readToken(objectData, ['accessToken', 'token', 'access_token']);
      const refreshToken = readToken(objectData, ['refreshToken', 'refresh_token']);
      const userId = readToken(objectData, ['userId', 'id', 'email']);

      if (accessToken) {
        localStorage.setItem('chae.accessToken', accessToken);
      }
      if (refreshToken) {
        localStorage.setItem('chae.refreshToken', refreshToken);
      }
      if (userId) {
        localStorage.setItem('chae.userId', userId);
        setSessionUser(userId);
      }
    }
  }

  async function createProduct(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(() =>
      apiRequest('/api/products', {
        method: 'POST',
        body: JSON.stringify({
          product: {
            name: productForm.name,
            category: productForm.category,
            price: Number(productForm.price),
          },
        }),
      }),
    );
    await loadProducts();
  }

  async function receiveStock(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(() =>
      apiRequest('/api/inventory/warehouse/receive', {
        method: 'POST',
        body: JSON.stringify({
          inventory: [{ productId: Number(orderForm.productId), quantity: Number(orderForm.quantity) }],
        }),
      }),
    );
  }

  async function createOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(() =>
      apiRequest('/api/orders', {
        method: 'POST',
        body: JSON.stringify({
          order: { productId: Number(orderForm.productId), quantity: Number(orderForm.quantity) },
        }),
      }),
    );
  }

  async function createSale(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(() =>
      apiRequest('/api/sales', {
        method: 'POST',
        body: JSON.stringify({
          userId: Number(saleForm.userId),
          salesItems: [{ productId: Number(saleForm.productId), quantity: Number(saleForm.quantity) }],
        }),
      }),
    );
  }

  async function checkoutCart(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (cart.length === 0) {
      setAction({ loading: false, data: null, error: '장바구니가 비어 있습니다.' });
      return;
    }

    const result = await runAction(() =>
      apiRequest('/api/sales', {
        method: 'POST',
        body: JSON.stringify({
          userId: Number(saleForm.userId),
          salesItems: cart.map((item) => ({ productId: Number(item.id), quantity: item.quantity })),
        }),
      }),
    );

    if (result) {
      setCart([]);
    }
  }

  async function lookupStock(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(() => apiRequest(`/api/inventory/stock/${stockProductId}`));
  }

  async function lookupPayment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(() => apiRequest(`/api/payment/sales/${paymentSalesId}/status`));
  }

  function addToCart(productId: string) {
    const product = productCatalog.find((item) => item.id === productId);
    if (!product) {
      return;
    }

    setCart((items) => {
      const existing = items.find((item) => item.id === product.id);
      if (existing) {
        return items.map((item) => (item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item));
      }
      return [...items, { ...product, quantity: 1 }];
    });
  }

  function updateCartQuantity(productId: string, quantityDelta: number) {
    setCart((items) =>
      items
        .map((item) => (item.id === productId ? { ...item, quantity: item.quantity + quantityDelta } : item))
        .filter((item) => item.quantity > 0),
    );
  }

  if (mode === 'shop') {
    return (
      <ShopView
        action={action}
        cart={cart}
        cartQuantity={cartQuantity}
        cartTotal={cartTotal}
        productCatalog={productCatalog}
        productQuery={productQuery}
        products={products}
        saleUserId={saleForm.userId}
        sessionUser={sessionUser}
        setProductQuery={setProductQuery}
        setSaleUserId={(userId) => setSaleForm({ ...saleForm, userId })}
        addToCart={addToCart}
        checkoutCart={checkoutCart}
        loadProducts={loadProducts}
        onOpenConsole={() => setMode('console')}
        updateCartQuantity={updateCartQuantity}
      />
    );
  }

  return (
    <main className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <PackagePlus size={22} aria-hidden="true" />
          </div>
          <div>
            <strong>Chae Chae</strong>
            <span>MSA Console</span>
          </div>
        </div>

        <div className="mode-switch" role="group" aria-label="화면 전환">
          <button className="mode-button" type="button" onClick={() => setMode('shop')}>
            <Store size={16} aria-hidden="true" />
            <span>쇼핑몰</span>
          </button>
          <button className="mode-button active" type="button" onClick={() => setMode('console')}>
            <ShieldCheck size={16} aria-hidden="true" />
            <span>운영</span>
          </button>
        </div>

        <nav className="nav-list" aria-label="운영 작업">
          {consoleTabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.key}
                className={activeTab === tab.key ? 'nav-item active' : 'nav-item'}
                type="button"
                onClick={() => setActiveTab(tab.key)}
                title={tab.label}
              >
                <Icon size={18} aria-hidden="true" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </nav>

        <AuthPanel authForm={authForm} actionLoading={action.loading} savedUser={sessionUser} setAuthForm={setAuthForm} onLogin={login} />
      </aside>

      <section className="workspace">
        <ConsoleView
          action={action}
          activeTab={activeTab}
          orderForm={orderForm}
          paymentSalesId={paymentSalesId}
          productForm={productForm}
          productQuery={productQuery}
          productRows={productRows}
          products={products}
          saleForm={saleForm}
          stockProductId={stockProductId}
          createOrder={createOrder}
          createProduct={createProduct}
          createSale={createSale}
          loadProducts={loadProducts}
          lookupPayment={lookupPayment}
          lookupStock={lookupStock}
          receiveStock={receiveStock}
          runAction={runAction}
          setOrderForm={setOrderForm}
          setPaymentSalesId={setPaymentSalesId}
          setProductForm={setProductForm}
          setProductQuery={setProductQuery}
          setSaleForm={setSaleForm}
          setStockProductId={setStockProductId}
        />

        <section className="response-panel" aria-live="polite">
          <div className="section-title">
            {action.loading ? <Loader2 className="spin" size={18} aria-hidden="true" /> : <CheckCircle2 size={18} aria-hidden="true" />}
            <h2>응답</h2>
          </div>
          <pre>{action.error || JSON.stringify(action.data ?? { message: '요청 결과가 여기에 표시됩니다.' }, null, 2)}</pre>
        </section>
      </section>
    </main>
  );
}

function AuthPanel({
  authForm,
  actionLoading,
  savedUser,
  setAuthForm,
  onLogin,
}: {
  authForm: AuthForm;
  actionLoading: boolean;
  savedUser: string;
  setAuthForm: (value: AuthForm) => void;
  onLogin: (event: FormEvent<HTMLFormElement>) => Promise<void>;
}) {
  return (
    <section className="auth-panel" aria-label="로그인">
      <div className="section-title">
        <ShieldCheck size={18} aria-hidden="true" />
        <h2>인증</h2>
      </div>
      <form className="stack" onSubmit={onLogin}>
        <input
          aria-label="이메일"
          placeholder="email@example.com"
          value={authForm.email}
          onChange={(event) => setAuthForm({ ...authForm, email: event.target.value })}
        />
        <input
          aria-label="비밀번호"
          placeholder="Password123!"
          type="password"
          value={authForm.password}
          onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })}
        />
        <button className="primary-button" type="submit" disabled={actionLoading}>
          <LogIn size={16} aria-hidden="true" />
          <span>로그인</span>
        </button>
      </form>
      <p className="session-line">{savedUser ? `User-Id: ${savedUser}` : '세션 없음'}</p>
    </section>
  );
}

function readToken(data: Record<string, unknown>, fields: string[]): string {
  for (const field of fields) {
    const value = data[field];
    if (typeof value === 'string' || typeof value === 'number') {
      return String(value);
    }
  }

  return '';
}
