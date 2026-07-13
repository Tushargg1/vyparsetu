import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { productApi, cartApi } from '../../lib/api';
import { useAllCarts, cartItemMap } from '../../hooks/useShop';
import CatalogGrid from '../../components/CatalogGrid';
import Icon from '../../components/Icon';
import PageHeader from '../../components/PageHeader';

export default function BuyProductsPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [q, setQ] = useState('');

  const productsQ = useQuery({
    queryKey: ['all-products', q],
    queryFn: () => productApi.search({ q: q || undefined, page: 0, size: 100 }),
  });
  const products = productsQ.data?.content || [];

  const cartsQ = useAllCarts();
  const byProduct = cartItemMap(cartsQ.data);
  const totalItems = (cartsQ.data || []).reduce((n, c) => n + (c.items?.length || 0), 0);

  const setQty = async (p, qty, cartItemId) => {
    if (qty <= 0 && cartItemId) await cartApi.removeItem(cartItemId);
    else if (qty > 0) await cartApi.addItem({ supplierId: p.supplierId, productId: p.id, quantity: qty });
    qc.invalidateQueries({ queryKey: ['carts-all'] });
  };

  return (
    <div className="space-y-md">
      <PageHeader icon="shopping_basket" title="Buy Products" subtitle="Products from all your distributors." />

      <div className="relative">
        <Icon name="search" className="absolute left-4 top-1/2 -translate-y-1/2 text-outline text-[24px]" />
        <input
          className="w-full pl-12 pr-4 py-3 bg-surface-container-lowest border border-outline-variant rounded-2xl outline-none focus:border-primary"
          placeholder="Search products"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      {productsQ.isLoading && <p className="text-on-surface-variant">Loading…</p>}
      {!productsQ.isLoading && products.length === 0 && <p className="text-on-surface-variant">No products available.</p>}

      <CatalogGrid products={products} cartByProduct={byProduct} onSetQty={setQty} />

      {totalItems > 0 && (
        <div className="fixed bottom-4 right-4 md:right-8 z-30">
          <button onClick={() => navigate('/cart')} className="bg-primary text-on-primary px-lg py-3 rounded-full shadow-lg font-bold flex items-center gap-2">
            <Icon name="shopping_cart" /> Cart ({totalItems})
          </button>
        </div>
      )}
    </div>
  );
}
