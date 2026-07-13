import { useQuery } from '@tanstack/react-query';
import { retailerApi, cartApi } from '../lib/api';

export function useDistributor() {
  return useQuery({ queryKey: ['my-distributor'], queryFn: retailerApi.distributor, retry: false });
}

export function useCart(supplierId) {
  return useQuery({
    queryKey: ['cart', supplierId],
    queryFn: () => cartApi.get(supplierId),
    enabled: !!supplierId,
  });
}

export function useAllCarts() {
  return useQuery({ queryKey: ['carts-all'], queryFn: cartApi.all });
}

/** Builds a map productId -> { cartItemId, quantity, supplierId } from all carts. */
export function cartItemMap(carts = []) {
  const map = {};
  carts.forEach((c) => {
    (c.items || []).forEach((it) => {
      map[it.productId] = { cartItemId: it.cartItemId, quantity: it.quantity, supplierId: c.supplierId };
    });
  });
  return map;
}
