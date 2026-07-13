import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { productApi, cartApi, directoryApi } from '../../lib/api';
import { useCart } from '../../hooks/useShop';
import CatalogGrid from '../../components/CatalogGrid';
import Icon from '../../components/Icon';
import ContactCard from '../../components/ContactCard';

export default function DistributorCatalogPage() {
  const { id } = useParams();
  const supplierId = Number(id);
  const navigate = useNavigate();
  const qc = useQueryClient();

  const { data: distributors = [] } = useQuery({ queryKey: ['distributors'], queryFn: directoryApi.distributors });
  const distributor = distributors.find((d) => d.id === supplierId);

  const productsQ = useQuery({
    queryKey: ['dist-products', supplierId],
    queryFn: () => productApi.search({ supplierId, page: 0, size: 100 }),
  });
  const products = productsQ.data?.content || [];

  const cartQ = useCart(supplierId);
  const byProduct = {};
  (cartQ.data?.items || []).forEach((it) => { byProduct[it.productId] = it; });
  const count = cartQ.data?.items?.length || 0;
  const total = cartQ.data?.estimatedTotal || 0;

  const setQty = async (p, qty, cartItemId) => {
    if (qty <= 0 && cartItemId) await cartApi.removeItem(cartItemId);
    else if (qty > 0) await cartApi.addItem({ supplierId, productId: p.id, quantity: qty });
    qc.invalidateQueries({ queryKey: ['cart', supplierId] });
    qc.invalidateQueries({ queryKey: ['carts-all'] });
  };

  return (
    <div className="space-y-md">
      <button onClick={() => navigate('/distributors')} className="flex items-center gap-1 text-primary text-label-md">
        <Icon name="arrow_back" /> Distributors
      </button>
      <h2 className="text-headline-lg font-bold text-on-background">{distributor?.businessName || 'Distributor'}</h2>

      {distributor && (
        <ContactCard
          title="Distributor contact"
          shopName={distributor.businessName}
          ownerName={distributor.ownerName}
          phone={distributor.phone}
          altPhones={distributor.altPhones}
          address={distributor.address}
          city={distributor.city}
          state={distributor.state}
          pincode={distributor.pincode}
          locationUrl={distributor.locationUrl}
        />
      )}

      {productsQ.isLoading && <p className="text-on-surface-variant">Loading…</p>}
      {!productsQ.isLoading && products.length === 0 && <p className="text-on-surface-variant">No products listed yet.</p>}

      <CatalogGrid products={products} cartByProduct={byProduct} onSetQty={setQty} />

      {count > 0 && (
        <div className="fixed bottom-4 right-4 md:right-8 z-30">
          <button onClick={() => navigate('/cart')} className="bg-primary text-on-primary px-lg py-3 rounded-full shadow-lg font-bold flex items-center gap-2">
            <Icon name="shopping_cart" /> ₹{total} · View Cart
          </button>
        </div>
      )}
    </div>
  );
}
