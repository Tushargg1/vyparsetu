import Icon from './Icon';

// Splits a comma/space separated list of numbers into a clean array.
const splitPhones = (s) =>
  (s || '')
    .split(/[,\n]/)
    .map((x) => x.trim())
    .filter(Boolean);

function Row({ icon, children }) {
  if (children == null || children === '') return null;
  return (
    <div className="flex items-start gap-sm">
      <Icon name={icon} className="text-on-surface-variant text-[18px] mt-0.5 shrink-0" />
      <div className="text-on-surface text-label-md min-w-0">{children}</div>
    </div>
  );
}

/**
 * Shows the contact profile (name, shop, numbers, address, shared location)
 * for a retailer or distributor.
 */
export default function ContactCard({
  title = 'Contact details',
  shopName,
  ownerName,
  phone,
  altPhones,
  address,
  city,
  state,
  pincode,
  locationUrl,
}) {
  const numbers = [phone, ...splitPhones(altPhones)].filter(Boolean);
  const place = [address, city, state, pincode].filter(Boolean).join(', ');

  return (
    <section className="ui-card overflow-hidden">
      <div className="flex items-center gap-sm border-b border-surface-variant bg-surface-container-low/60 px-lg py-md">
        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-secondary-fixed text-secondary">
          <Icon name="contact_page" className="text-[20px]" />
        </span>
        <h3 className="text-body-lg font-semibold text-on-background">{title}</h3>
      </div>

      <div className="p-lg space-y-md">
        {shopName && (
          <Row icon="storefront">
            <div className="font-semibold text-on-surface">{shopName}</div>
          </Row>
        )}
        {ownerName && <Row icon="person">{ownerName}</Row>}

        {numbers.length > 0 && (
          <Row icon="call">
            <div className="flex flex-wrap gap-x-md gap-y-1">
              {numbers.map((n, i) => (
                <a key={i} href={`tel:${n}`} className="text-primary font-medium hover:underline">
                  {n}
                  {i === 0 && numbers.length > 1 ? ' (main)' : ''}
                </a>
              ))}
            </div>
          </Row>
        )}

        {place && <Row icon="location_on">{place}</Row>}

        {locationUrl && (
          <a
            href={locationUrl}
            target="_blank"
            rel="noreferrer"
            className="ui-button-secondary"
          >
            <Icon name="map" className="text-[18px]" />
            Open shared location
          </a>
        )}

        {numbers.length === 0 && !place && !locationUrl && (
          <p className="text-on-surface-variant text-label-md">No contact details saved yet.</p>
        )}
      </div>
    </section>
  );
}
