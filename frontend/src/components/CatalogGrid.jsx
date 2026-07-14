import Icon from './Icon';

export default function CatalogGrid({ products, cartByProduct = {}, onSetQty }) {
  return (
    <div className="grid grid-cols-1 gap-md sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {products.map((p) => {
        const item = cartByProduct[p.id];
        const qty = item ? Number(item.quantity) : 0;
        return (
          <article key={p.id} className="ui-card group flex flex-col overflow-hidden p-sm transition hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-md">
            <div className="mb-md flex aspect-[4/3] items-center justify-center overflow-hidden rounded-xl bg-surface-container-low">
              {p.imageUrl ? (
                <img src={p.imageUrl} alt={p.name} className="h-full w-full object-contain p-sm transition duration-200 group-hover:scale-[1.03]" />
              ) : (
                <Icon name="shopping_basket" className="text-[44px] text-primary-fixed-dim" />
              )}
            </div>
            <div className="flex flex-1 flex-col px-sm pb-sm">
              <p className="text-label-sm uppercase tracking-wide text-on-surface-variant">{p.brand}</p>
              <h3 className="mb-auto mt-xs line-clamp-2 text-body-md font-semibold leading-snug text-on-surface">{p.name}</h3>
              <p className="my-sm text-headline-md font-bold text-primary">₹{p.sellingPrice}</p>
            {qty === 0 ? (
              <button
                onClick={() => onSetQty(p, 1, item?.cartItemId)}
                className="ui-button-primary w-full"
              >
                <Icon name="add" className="text-[20px]" /> Add
              </button>
            ) : (
              <div className="flex items-center justify-between bg-primary rounded-xl overflow-hidden h-11">
                <button aria-label={`Decrease ${p.name} quantity`} onClick={() => onSetQty(p, qty - 1, item.cartItemId)} className="h-full w-11 text-2xl text-on-primary hover:bg-on-primary/10">−</button>
                <span className="font-bold text-on-primary">{qty}</span>
                <button aria-label={`Increase ${p.name} quantity`} onClick={() => onSetQty(p, qty + 1, item.cartItemId)} className="h-full w-11 text-2xl text-on-primary hover:bg-on-primary/10">+</button>
              </div>
            )}
            </div>
          </article>
        );
      })}
    </div>
  );
}
