import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { directoryApi } from '../../lib/api';
import Icon from '../../components/Icon';
import PageHeader from '../../components/PageHeader';

export default function DistributorsPage() {
  const navigate = useNavigate();
  const { data: distributors = [], isLoading } = useQuery({ queryKey: ['distributors'], queryFn: directoryApi.distributors });
  const [search, setSearch] = useState('');

  const visible = distributors.filter((d) => {
    const q = search.trim().toLowerCase();
    if (!q) return true;
    return [d.businessName, d.city, d.state, d.supplierType].filter(Boolean).join(' ').toLowerCase().includes(q);
  });

  return (
    <div className="space-y-lg">
      <PageHeader
        icon="groups"
        title="Distributors"
        subtitle="Browse suppliers and place an order from their catalog."
      />

      <div className="relative max-w-md">
        <Icon name="search" className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]" />
        <input
          className="w-full border border-outline-variant rounded-xl py-2.5 pl-10 pr-3 outline-none focus:border-primary bg-surface-container-lowest"
          placeholder="Search distributors"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {isLoading && <p className="text-on-surface-variant">Loading…</p>}

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-gutter">
        {visible.map((d) => {
          const place = [d.city, d.state].filter(Boolean).join(', ');
          return (
            <button
              key={d.id}
              onClick={() => navigate(`/distributors/${d.id}`)}
              className="group bg-surface-container-lowest rounded-2xl border border-surface-variant p-lg text-left hover:border-primary hover:shadow-md transition-all"
            >
              <div className="flex items-center gap-md">
                <div className="w-14 h-14 rounded-2xl bg-primary-fixed flex items-center justify-center text-primary shrink-0">
                  <Icon name="storefront" className="text-[28px]" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-headline-md font-bold text-on-surface truncate">{d.businessName}</p>
                  <span className="inline-flex items-center gap-1 text-label-sm text-on-surface-variant bg-surface-container px-2 py-0.5 rounded-md mt-1">
                    {d.supplierType}
                  </span>
                </div>
                <Icon name="chevron_right" className="text-on-surface-variant group-hover:text-primary transition-colors" />
              </div>

              <div className="mt-md pt-md border-t border-surface-variant space-y-1.5">
                {place && (
                  <div className="flex items-center gap-sm text-label-md text-on-surface-variant">
                    <Icon name="location_on" className="text-[18px]" /> {place}
                  </div>
                )}
                {d.phone && (
                  <div className="flex items-center gap-sm text-label-md text-on-surface-variant">
                    <Icon name="call" className="text-[18px]" /> {d.phone}
                  </div>
                )}
                <div className="flex items-center gap-sm text-label-md font-semibold text-primary pt-1">
                  Open catalog <Icon name="arrow_forward" className="text-[18px]" />
                </div>
              </div>
            </button>
          );
        })}
        {!isLoading && visible.length === 0 && (
          <div className="col-span-full bg-surface-container-lowest rounded-2xl border border-surface-variant p-2xl text-center text-on-surface-variant">
            <Icon name="search_off" className="text-[36px] opacity-40" />
            <p className="mt-sm">No distributors found.</p>
          </div>
        )}
      </div>
    </div>
  );
}
