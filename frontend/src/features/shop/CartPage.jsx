import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { cartApi, orderApi, directoryApi } from '../../lib/api';
import { useAllCarts } from '../../hooks/useShop';
import Icon from '../../components/Icon';

export default function CartPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const cartsQ = useAllCarts();
  const carts = cartsQ.data || [];
  const { data: distributors = [] } = useQuery({ queryKey: ['distributors'], queryFn: directoryApi.distributors });
  const nameOf = (sid) => distributors.find((d) => d.id === sid)?.businessName || `Distributor #${sid}`;

  const [placingId, setPlacingId] = useState(null);
  const [msg, setMsg] = useState('');

  const setQty = async (supplierId, it, qty) => {
    if (qty <= 0) await cartApi.removeItem(it.cartItemId);
    else await cartApi.addItem({ supplierId, productId: it.productId, quantity: qty });
    qc.invalidateQueries({ queryKey: ['carts-all'] });
  };

  const placeOrder = async (supplierId) => {
    setMsg('');
    setPlacingId(supplierId);
    try {
      const o = await orderApi.place({ supplierId, paymentMode: 'COD', orderSource: 'CART' });
      qc.invalidateQueries({ queryKey: ['carts-all'] });
      setMsg(`Order ${o.orderNumber} placed with ${nameOf(supplierId)}!`);
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Could not place order.');
    } finally {
      setPlacingId(null);
    }
  };

  if (cartsQ.isLoading) return <p className="text-on-surface-variant">Loading…</p>;

  if (carts.length === 0) {
    return (
      <div className="text-center pt-2xl text-on-surface-variant">
        <Icon name="shopping_cart" className="text-[56px]" />
        <p className="mt-md text-body-lg">Your cart is empty.</p>
        <button onClick={() => navigate('/buy')} className="mt-lg bg-primary text-on-primary px-lg py-3 rounded-2xl font-bold">Browse products</button>
      </div>
    );
  }

  return (
    <div className="space-y-lg max-w-2xl">
      <h2 className="text-headline-lg font-bold text-on-background">My Cart</h2>
      {msg && <div className="p-md rounded-xl bg-tertiary-fixed-dim text-on-tertiary-fixed text-label-md">{msg}</div>}

      {carts.map((cart) => (
        <div key={cart.cartId} className="bg-surface-container-lowest rounded-2xl border border-surface-variant overflow-hidden">
          <div className="px-lg py-md border-b border-outline-variant flex items-center gap-2">
            <Icon name="storefront" className="text-primary" />
            <h3 className="text-headline-md">{nameOf(cart.supplierId)}</h3>
          </div>
          <div className="divide-y divide-surface-variant">
            {cart.items.map((it) => (
              <div key={it.cartItemId} className="px-lg py-md flex items-center gap-md">
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-on-surface line-clamp-1">{it.productName}</p>
                  <p className="text-label-sm text-on-surface-variant">₹{it.unitPrice} each</p>
                </div>
                <div className="flex items-center bg-surface-container rounded-xl overflow-hidden h-10">
                  <button onClick={() => setQty(cart.supplierId, it, Number(it.quantity) - 1)} className="w-10 text-primary text-xl">−</button>
                  <span className="w-8 text-center font-bold">{Number(it.quantity)}</span>
                  <button onClick={() => setQty(cart.supplierId, it, Number(it.quantity) + 1)} className="w-10 text-primary text-xl">+</button>
                </div>
              </div>
            ))}
          </div>
          <div className="px-lg py-md flex items-center justify-between bg-surface-container-low">
            <div>
              <p className="text-label-sm text-on-surface-variant">Total</p>
              <p className="text-headline-md font-bold text-primary">₹{cart.estimatedTotal}</p>
            </div>
            <button
              onClick={() => placeOrder(cart.supplierId)}
              disabled={placingId === cart.supplierId}
              className="bg-primary text-on-primary px-lg py-3 rounded-xl font-bold flex items-center gap-1 disabled:opacity-60"
            >
              {placingId === cart.supplierId ? 'Placing…' : 'Place Order'} <Icon name="check" />
            </button>
          </div>
        </div>
      ))}
      <p className="text-center text-label-sm text-on-surface-variant">Pay on delivery (Cash)</p>
    </div>
  );
}
