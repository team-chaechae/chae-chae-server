import { CheckCircle2, Clock3, Heart, Menu, Minus, Plus, RefreshCw, Search, ShieldCheck, ShoppingCart, Store, UserRound } from 'lucide-react';
import type { FormEvent } from 'react';
import type { ApiEnvelope, CartItem, ProductRecord, RequestState } from '../api';
import { formatMoney } from '../api';
import { ProductSkeleton } from './shared';

const mallCategories = ['쓱배송', '세일중', '베스트', '신상품', '전단행사', '브랜드관', '간편식', '신선식품'];
const serviceTiles = [
  { title: '오늘배송', description: '가까운 매장 상품을 빠르게' },
  { title: '새벽배송', description: '아침 식탁 준비를 먼저' },
  { title: '대용량 장보기', description: '많이 사도 한 번에' },
  { title: '신선보장', description: '신선식품 중심 큐레이션' },
];

export function ShopView({
  action,
  cart,
  cartQuantity,
  cartTotal,
  productCatalog,
  productQuery,
  products,
  saleUserId,
  setProductQuery,
  setSaleUserId,
  addToCart,
  checkoutCart,
  loadProducts,
  onOpenConsole,
  sessionUser,
  updateCartQuantity,
}: {
  action: RequestState<ApiEnvelope<unknown>>;
  cart: CartItem[];
  cartQuantity: number;
  cartTotal: number;
  productCatalog: ProductRecord[];
  productQuery: string;
  products: RequestState<unknown>;
  saleUserId: string;
  setProductQuery: (value: string) => void;
  setSaleUserId: (value: string) => void;
  addToCart: (productId: string) => void;
  checkoutCart: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  loadProducts: () => Promise<void>;
  onOpenConsole: () => void;
  sessionUser: string;
  updateCartQuantity: (productId: string, quantityDelta: number) => void;
}) {
  return (
    <div className="mall-page">
      <div className="mall-ribbon">첫 주문 쿠폰팩 · 오늘 담으면 더 좋은 장보기 특가</div>

      <header className="mall-header">
        <div className="mall-header-inner">
          <button className="mall-logo" type="button" onClick={loadProducts}>
            <Store size={26} aria-hidden="true" />
            <span>CHAE MART</span>
          </button>

          <form
            className="mall-search"
            onSubmit={(event) => {
              event.preventDefault();
              void loadProducts();
            }}
          >
            <input
              aria-label="통합 검색"
              placeholder="오늘 장보기 상품을 검색해보세요"
              value={productQuery}
              onChange={(event) => setProductQuery(event.target.value)}
            />
            <button className="mall-search-button" type="submit" disabled={products.loading} title="검색">
              <Search size={22} aria-hidden="true" />
            </button>
          </form>

          <div className="mall-actions">
            <button className="mall-action" type="button" title="로그인 상태">
              <UserRound size={20} aria-hidden="true" />
              <span>{sessionUser || '로그인'}</span>
            </button>
            <button className="mall-action" type="button" title="관심상품">
              <Heart size={20} aria-hidden="true" />
              <span>좋아요</span>
            </button>
            <button className="mall-cart-button" type="button" title="장바구니">
              <ShoppingCart size={21} aria-hidden="true" />
              <strong>{cartQuantity}</strong>
            </button>
            <button className="mall-console-button" type="button" onClick={onOpenConsole}>
              <ShieldCheck size={18} aria-hidden="true" />
              <span>운영</span>
            </button>
          </div>
        </div>

        <nav className="mall-nav" aria-label="쇼핑몰 메뉴">
          <button className="category-button" type="button">
            <Menu size={18} aria-hidden="true" />
            <span>전체 카테고리</span>
          </button>
          {mallCategories.map((category) => (
            <button key={category} type="button">
              {category}
            </button>
          ))}
          <div className="delivery-chip">
            <Clock3 size={16} aria-hidden="true" />
            <span>오늘 18:00 전 도착</span>
          </div>
        </nav>
      </header>

      <main className="mall-main">
        <section className="mall-hero">
          <article className="hero-banner">
            <div>
              <span className="hero-tag">장보기 ZONE</span>
              <h1>오늘 담으면 좋은 신선 장보기</h1>
              <p>자주 사는 상품부터 행사 상품까지 한 번에 담아보세요.</p>
              <button className="hero-button" type="button" onClick={loadProducts}>
                <RefreshCw size={17} aria-hidden="true" />
                <span>상품 보기</span>
              </button>
            </div>
            <div className="hero-visual" aria-hidden="true">
              <div className="basket-shape">
                <span />
                <span />
                <span />
              </div>
            </div>
          </article>

          <div className="promo-stack">
            <article>
              <span>최대 혜택</span>
              <strong>장보기 쿠폰</strong>
              <p>담을수록 커지는 할인</p>
            </article>
            <article>
              <span>오늘 특가</span>
              <strong>신선식품</strong>
              <p>매일 바뀌는 추천 상품</p>
            </article>
          </div>
        </section>

        <section className="service-strip" aria-label="배송 서비스">
          {serviceTiles.map((tile) => (
            <article key={tile.title}>
              <strong>{tile.title}</strong>
              <span>{tile.description}</span>
            </article>
          ))}
        </section>

        <section className="mall-section-head">
          <div>
            <p className="eyebrow">Today Pick</p>
            <h2>지금 세일 중인 상품이에요</h2>
          </div>
          <button className="secondary-button" type="button" onClick={loadProducts}>
            <RefreshCw size={16} aria-hidden="true" />
            <span>새로고침</span>
          </button>
        </section>

        <div className="mall-content">
          <section className="mall-product-grid" aria-label="상품 목록">
            {products.loading ? (
              Array.from({ length: 8 }).map((_, index) => <ProductSkeleton key={index} />)
            ) : productCatalog.length > 0 ? (
              productCatalog.map((product, index) => (
                <article className="mall-product-card" key={`${product.id}-${index}`}>
                  <div className="mall-product-image" aria-hidden="true">
                    <span>{product.category.slice(0, 2).toUpperCase()}</span>
                  </div>
                  <div className="mall-product-meta">
                    <span className="delivery-label">쓱배송</span>
                    <h3>{product.name}</h3>
                    <p>{product.status}</p>
                    <div className="mall-price-row">
                      <strong>{product.price === null ? '가격 미정' : `${formatMoney(product.price)}원`}</strong>
                      <button className="cart-add-button" type="button" onClick={() => addToCart(product.id)} title="장바구니 담기">
                        <Plus size={19} aria-hidden="true" />
                      </button>
                    </div>
                  </div>
                </article>
              ))
            ) : (
              <section className="empty-shop">
                <Store size={36} aria-hidden="true" />
                <strong>{products.error || '상품을 검색해 주세요.'}</strong>
                <button className="secondary-button" type="button" onClick={loadProducts}>
                  <RefreshCw size={16} aria-hidden="true" />
                  <span>상품 불러오기</span>
                </button>
              </section>
            )}
          </section>

          <section className="cart-panel mall-cart-panel" aria-label="장바구니">
            <div className="section-title">
              <ShoppingCart size={18} aria-hidden="true" />
              <h2>장바구니</h2>
            </div>
            <div className="cart-list">
              {cart.length > 0 ? (
                cart.map((item) => (
                  <article className="cart-item" key={item.id}>
                    <div>
                      <strong>{item.name}</strong>
                      <span>{item.price === null ? '가격 미정' : `${formatMoney(item.price)}원`}</span>
                    </div>
                    <div className="stepper">
                      <button className="icon-button" type="button" onClick={() => updateCartQuantity(item.id, -1)} title="수량 감소">
                        <Minus size={16} aria-hidden="true" />
                      </button>
                      <span>{item.quantity}</span>
                      <button className="icon-button" type="button" onClick={() => updateCartQuantity(item.id, 1)} title="수량 증가">
                        <Plus size={16} aria-hidden="true" />
                      </button>
                    </div>
                  </article>
                ))
              ) : (
                <p className="helper-text">담은 상품이 없습니다.</p>
              )}
            </div>
            <form className="stack checkout-form" onSubmit={checkoutCart}>
              <input
                aria-label="주문 사용자 ID"
                placeholder="User ID"
                value={saleUserId}
                onChange={(event) => setSaleUserId(event.target.value)}
                required
              />
              <div className="total-line">
                <span>합계</span>
                <strong>{formatMoney(cartTotal)}원</strong>
              </div>
              <button className="primary-button" type="submit" disabled={action.loading || cart.length === 0}>
                <CheckCircle2 size={16} aria-hidden="true" />
                <span>주문 생성</span>
              </button>
            </form>
          </section>
        </div>
      </main>
    </div>
  );
}
