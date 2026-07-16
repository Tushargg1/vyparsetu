import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { distributorApi, retailerApi } from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import Icon from '../../components/Icon';
import PageHeader from '../../components/PageHeader';
import SecurityMethods from '../../components/SecurityMethods';
import { LoadingState } from '../../components/StatePanel';

const EMPTY = {
  ownerName: '',
  displayName: '',
  altPhones: '',
  address: '',
  city: '',
  state: '',
  pincode: '',
  locationUrl: '',
};

function Field({ label, value, onChange, placeholder, full, type = 'text', inputMode }) {
  return (
    <label className={`block ${full ? 'md:col-span-2' : ''}`}>
      <span className="text-label-md text-on-surface-variant">{label}</span>
      <input
        type={type}
        inputMode={inputMode}
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="ui-input mt-xs"
      />
    </label>
  );
}

export default function ProfilePage() {
  const qc = useQueryClient();
  const roles = useAuthStore((s) => s.user?.roles || []);
  const isSupplier = roles.includes('SUPPLIER');
  const api = isSupplier ? distributorApi : retailerApi;
  const shopLabel = isSupplier ? 'Business name' : 'Shop name';

  const { data, isLoading } = useQuery({ queryKey: ['my-profile'], queryFn: api.profile });
  const [form, setForm] = useState(EMPTY);
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (data) {
      setForm({
        ownerName: data.ownerName || '',
        displayName: data.displayName || '',
        altPhones: data.altPhones || '',
        address: data.address || '',
        city: data.city || '',
        state: data.state || '',
        pincode: data.pincode || '',
        locationUrl: data.locationUrl || '',
      });
    }
  }, [data]);

  const set = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));

  const save = async () => {
    setBusy(true);
    setMsg('');
    try {
      await api.updateProfile(form);
      setMsg('Profile saved.');
      qc.invalidateQueries({ queryKey: ['my-profile'] });
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Could not save profile');
    } finally {
      setBusy(false);
    }
  };

  const useMyLocation = () => {
    if (!navigator.geolocation) {
      setMsg('Location not supported on this device.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const { latitude, longitude } = pos.coords;
        set('locationUrl')(`https://maps.google.com/?q=${latitude.toFixed(6)},${longitude.toFixed(6)}`);
        setMsg('Location captured. Remember to save.');
      },
      () => setMsg('Could not get your location.'),
    );
  };

  if (isLoading) return <LoadingState label="Loading your profile…" />;

  return (
    <div className="space-y-lg max-w-3xl">
      <PageHeader
        icon="badge"
        title="My profile"
        subtitle={`Your contact details ${isSupplier ? 'are shown to retailers.' : 'are shown to your distributor.'}`}
      />

      <div className="ui-card space-y-lg p-lg sm:p-xl">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
          <Field label="Owner name" value={form.ownerName} onChange={set('ownerName')} placeholder="Your name" />
          <Field label={shopLabel} value={form.displayName} onChange={set('displayName')} placeholder={shopLabel} />
          {data?.phone && (
            <label className="block">
              <span className="text-label-md text-on-surface-variant">Primary number</span>
              <input
                value={`+91 ${data.phone}`}
                disabled
                className="block w-full mt-1 border border-outline-variant rounded-lg py-2 px-3 bg-surface-container text-on-surface-variant"
              />
            </label>
          )}
          <Field
            label="More numbers"
            value={form.altPhones}
            onChange={set('altPhones')}
            placeholder="Comma separated, e.g. 9000000001, 9000000002"
          />
          <Field label="Address" value={form.address} onChange={set('address')} placeholder="Street, area" full />
          <Field label="City" value={form.city} onChange={set('city')} placeholder="City" />
          <Field label="State" value={form.state} onChange={set('state')} placeholder="State" />
          <Field label="Pincode" value={form.pincode} onChange={set('pincode')} placeholder="Pincode" inputMode="numeric" />
          <Field label="Shared location link" value={form.locationUrl} onChange={set('locationUrl')} placeholder="Google Maps link" full />
        </div>

        <div className="flex flex-wrap items-center gap-sm">
          <button onClick={save} disabled={busy} className="ui-button-primary">
            Save profile
          </button>
          <button onClick={useMyLocation} className="ui-button-secondary">
            <Icon name="my_location" className="text-[18px]" />
            Use my location
          </button>
          {form.locationUrl && (
            <a href={form.locationUrl} target="_blank" rel="noreferrer" className="text-primary text-label-md font-semibold hover:underline">
              Preview location
            </a>
          )}
          {msg && <span className="text-label-md text-on-surface-variant">{msg}</span>}
        </div>
      </div>

      <SecurityMethods />
    </div>
  );
}
