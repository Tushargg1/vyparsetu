import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { productApi, cartApi, retailerApi } from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { useCart, useDistributor } from '../../hooks/useShop';
import Icon from '../../components/Icon';

export default function ShopPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const [q, setQ] = useState('');
  const [code, setCode] = useState('');
  const [joinErr, setJoinErr] = useState('');

  const distQ = useDistributor();
  const linked = !distQ.isError && distQ.data;
  const supplierId = distQ.data?.id;

  const productsQ = useQuery({
    queryKey: ['products', q],
    queryFn: () => productApi.search({ q: q || undefined, page: 0, size: 50 }),
    enabled: !!supplierId,
  });
  const products = productsQ.data?.content || [];

  const cartQ = useCart(supplierId);
  const cartItems = cartQ.data?.items || [];
  const byProduct = {};
  cartItems.forEach((it) => { byProduct[it.productId] = it; });
  const cartCount = cartItems.length;
  const cartTotal = cartQ.data?.estimatedTotal || 0;

  const setQty = async (productId, qty, cartItemId) => {
    if (qty <= 0 && cartItemId) {
      await cartApi.removeItem(cartItemId);
    } else if (qty > 0) {
      await cartApi.addItem({ supplierId, productId, quantity: qty });
    }
    qc.invalidateQueries({ queryKey: ['cart', supplierId] });
  };

  const join = async () => {
    setJoinErr('');
    try {
      await retailerApi.join(code.trim());
      qc.invalidateQueries({ queryKey: ['my-distributor'] });
    } catch (e) {
      setJoinErr(e.response?.data?.error?.message || 'That code did not work. Please check with your distributor.');
    }
  };

  if (distQ.isLoading) return <p className="text-on-surface-variant">Loading…</p>;

  if (!linked) {
    return (
      <div className="space-y-lg pt-md">
        <div className="text-center">
          <div className="w-20 h-20 rounded-3xl bg-primary-fixed mx-auto flex items-center justify-center mb-md">
            <Icon name="handshake" className="text-[44px] text-primary" />
          </div>
          <h2 className="text-headline-md">Welcome{user?.name ? `, ${user.name}` : ''}!</h2>
          <p className="text-body-md text-on-surface-variant mt-xs">Enter your distributor's code to start ordering.</p>
        </div>
        {joinErr && <div className="p-md rounded-xl bg-error-container text-on-error-container text-label-md text-center">{joinErr}</div>}
        <input
          className="w-full border-2 border-outline-variant rounded-2xl py-4 px-4 text-headline-md text-center tracking-widest outline-none focus:border-primary uppercase"
          placeholder="VS-XXXXXXXX"
          value={code}
          onChange={(e) => setCode(e.target.value)}
        />
        <button onClick={join} className="w-full bg-primary text-on-primary py-4 rounded-2xl text-headline-md font-bold">
          Connect
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-md">
      <div>
        <p className="text-label-md text-on-surface-variant flex items-center gap-1">
          <Icon name="storefront" className="text-[18px]" /> {distQ.data.businessName}
        </p>
        <h2 className="text-headline-md mt-0.5">What do you need today?</h2>
      </div>

      <div className="relative">
        <Icon name="search" className="absolute left-4 top-1/2 -translate-y-1/2 text-outline text-[26px]" />
        <input
          className="w-full pl-12 pr-4 py-4 bg-surface-container-lowest border-2 border-outline-variant rounded-2xl text-body-lg outline-none focus:border-primary"
          placeholder="Search products"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      {productsQ.isLoading && <p className="text-on-surface-variant">Loading products…</p>}
      {!productsQ.isLoading && products.length === 0 && (
        <div className="text-center py-2xl text-on-surface-variant">
          <Icon name="inventory_2" className="text-[48px]" />
          <p className="mt-sm">No products found.</p>
        </div>
      )}

      <div className="grid grid-cols-2 gap-md">
        {products.map((p) => {
          const item = byProduct[p.id];
          const qty = item ? Number(item.quantity) : 0;
          return (
            <div key={p.id} className="bg-surface-container-lowest rounded-2xl border border-surface-variant p-md flex flex-col">
              <div className="aspect-square bg-surface-container rounded-xl mb-sm flex items-center justify-center overflow-hidden">
                {p.imageUrl ? (
                  <img src={p.imageUrl} alt={p.name} className="w-full h-full object-contain" />
                ) : (
                  <Icon name="shopping_basket" className="text-[44px] text-primary-fixed-dim" />
                )}
              </div>
              <h3 className="text-body-md font-semibold leading-snug line-clamp-2 mb-1">{p.name}</h3>
              <p className="text-headline-md font-bold text-primary mb-sm">₹{p.sellingPrice}</p>
              {qty === 0 ? (
                <button
                  onClick={() => setQty(p.id, 1)}
                  className="w-full bg-primary text-on-primary py-3 rounded-xl text-label-md font-bold flex items-center justify-center gap-1"
                >
                  <Icon name="add" className="text-[20px]" /> Add
                </button>
              ) : (
                <div className="flex items-center justify-between bg-primary rounded-xl overflow-hidden h-12">
                  <button onClick={() => setQty(p.id, qty - 1, item.cartItemId)} className="w-12 h-full text-on-primary text-2xl">−</button>
                  <span className="text-on-primary font-bold text-headline-md">{qty}</span>
                  <button onClick={() => setQty(p.id, qty + 1, item.cartItemId)} className="w-12 h-full text-on-primary text-2xl">+</button>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {cartCount > 0 && (
        <div className="fixed bottom-[68px] inset-x-0 z-30 px-margin-mobile">
          <div className="max-w-3xl mx-auto bg-primary text-on-primary rounded-2xl shadow-lg p-md flex items-center justify-between">
            <div>
              <p className="text-[13px] text-primary-fixed-dim">{cartCount} item{cartCount > 1 ? 's' : ''} in cart</p>
              <p className="text-headline-md font-bold">₹{cartTotal}</p>
            </div>
            <button onClick={() => navigate('/cart')} className="bg-on-primary text-primary px-lg py-3 rounded-xl font-bold flex items-center gap-1">
              View Cart <Icon name="arrow_forward" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
