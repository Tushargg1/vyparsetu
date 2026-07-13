import Icon from './Icon';

export default function CatalogGrid({ products, cartByProduct = {}, onSetQty }) {
  return (
    <div className="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-md">
      {products.map((p) => {
        const item = cartByProduct[p.id];
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
            <p className="text-label-sm text-outline">{p.brand}</p>
            <h3 className="text-body-md font-semibold leading-snug line-clamp-2 mb-auto">{p.name}</h3>
            <p className="text-headline-md font-bold text-primary my-1">₹{p.sellingPrice}</p>
            {qty === 0 ? (
              <button
                onClick={() => onSetQty(p, 1, item?.cartItemId)}
                className="w-full bg-primary text-on-primary py-2.5 rounded-xl text-label-md font-bold flex items-center justify-center gap-1"
              >
                <Icon name="add" className="text-[20px]" /> Add
              </button>
            ) : (
              <div className="flex items-center justify-between bg-primary rounded-xl overflow-hidden h-11">
                <button onClick={() => onSetQty(p, qty - 1, item.cartItemId)} className="w-11 h-full text-on-primary text-2xl">−</button>
                <span className="text-on-primary font-bold">{qty}</span>
                <button onClick={() => onSetQty(p, qty + 1, item.cartItemId)} className="w-11 h-full text-on-primary text-2xl">+</button>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
