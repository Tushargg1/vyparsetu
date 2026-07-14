import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import Icon from '../../components/Icon';

const ROLES = [
  { key: 'RETAILER', label: 'Retailer', desc: 'Buy goods for your shop', icon: 'store' },
  { key: 'SUPPLIER', label: 'Supplier / Distributor', desc: 'Manage inventory & orders', icon: 'inventory_2' },
];

function homeFor(roles) {
  if (roles.includes('SUPPLIER')) return '/supplier';
  if (roles.includes('ADMIN')) return '/admin';
  return '/';
}

export default function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const [role, setRole] = useState('RETAILER');
  const [form, setForm] = useState({ phone: '', name: '', shopName: '', businessName: '', supplierType: 'DISTRIBUTOR', inviteCode: '' });
  const [otp, setOtp] = useState('');
  const [purpose, setPurpose] = useState('LOGIN');
  const [step, setStep] = useState('details');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const sendOtp = async () => {
    setError('');
    if (!/^[6-9]\d{9}$/.test(form.phone)) {
      setError('Enter a valid 10-digit mobile number');
      return;
    }
    setLoading(true);
    try {
      // Try to register; if the phone already exists, fall back to login OTP.
      try {
        await authApi.register({
          name: form.name || 'User',
          phone: form.phone,
          role,
          shopName: form.shopName,
          businessName: form.businessName,
          supplierType: role === 'SUPPLIER' ? form.supplierType : undefined,
          inviteCode: role === 'RETAILER' ? form.inviteCode : undefined,
        });
        setPurpose('REGISTER');
      } catch (e) {
        const code = e.response?.data?.error?.code;
        if (code === 'PHONE_TAKEN') {
          await authApi.sendOtp({ identifier: form.phone, channel: 'SMS', purpose: 'LOGIN' });
          setPurpose('LOGIN');
        } else {
          throw e;
        }
      }
      setStep('otp');
    } catch (e) {
      setError(e.response?.data?.error?.message || 'Could not send OTP');
    } finally {
      setLoading(false);
    }
  };

  const verify = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await authApi.verifyOtp({ identifier: form.phone, code: otp, purpose });
      setAuth({ accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user });
      navigate(homeFor(res.user.roles || []));
    } catch (e) {
      setError(e.response?.data?.error?.message || 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  };

  const devLogin = async (devRole) => {
    setError('');
    setLoading(true);
    try {
      const res = await authApi.devLogin(devRole);
      setAuth({ accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user });
      navigate(homeFor(res.user.roles || []));
    } catch (e) {
      setError(e.response?.data?.error?.message || 'Test login unavailable');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-mesh min-h-screen flex items-center justify-center p-md">
      <main className="w-full max-w-md bg-surface-container-lowest rounded-[32px] shadow-[0_8px_30px_rgb(0,0,0,0.06)] border border-surface-container-high/50 p-lg md:p-xl">
        <div className="flex flex-col items-center mb-xl">
          <div className="w-16 h-16 bg-gradient-to-br from-primary to-primary-container rounded-2xl flex items-center justify-center mb-md shadow-lg">
            <Icon name="storefront" className="text-on-primary text-[32px]" />
          </div>
          <h1 className="text-headline-lg text-primary">VyaparMantra</h1>
          <p className="text-body-md text-on-surface-variant mt-xs">Empowering Indian B2B Commerce</p>
        </div>

        {error && (
          <div className="mb-md p-sm rounded-xl bg-error-container text-on-error-container text-label-md">{error}</div>
        )}

        {step === 'details' ? (
          <>
            <h2 className="text-[20px] font-semibold text-on-surface mb-md">Choose Your Role</h2>
            <div className="grid grid-cols-1 gap-sm mb-xl">
              {ROLES.map((r) => {
                const active = role === r.key;
                return (
                  <button
                    key={r.key}
                    onClick={() => setRole(r.key)}
                    className={`w-full flex items-center p-md rounded-2xl border text-left transition-all ${
                      active ? 'border-2 border-secondary shadow-md' : 'border-surface-container-high hover:border-primary/30'
                    } bg-surface-container-lowest`}
                  >
                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center mr-md ${active ? 'bg-secondary text-on-secondary' : 'bg-surface-container-high text-primary'}`}>
                      <Icon name={r.icon} className="text-[24px]" />
                    </div>
                    <div>
                      <div className={`text-label-md font-bold ${active ? 'text-secondary' : 'text-on-surface'}`}>{r.label}</div>
                      <div className="text-label-sm text-on-surface-variant mt-0.5">{r.desc}</div>
                    </div>
                  </button>
                );
              })}
            </div>

            <h2 className="text-[20px] font-semibold text-on-surface mb-md">Login or Register</h2>
            <div className="flex flex-col gap-md">
              <div className="flex">
                <span className="inline-flex items-center px-4 rounded-l-xl border border-r-0 border-outline-variant bg-surface-container text-on-surface-variant">+91</span>
                <input
                  className="block w-full border border-outline-variant bg-surface-container-lowest rounded-r-xl focus:ring-2 focus:ring-primary/20 focus:border-primary py-3 px-4 text-body-md outline-none"
                  placeholder="10-digit mobile number"
                  inputMode="numeric"
                  value={form.phone}
                  onChange={set('phone')}
                />
              </div>
              <input className="w-full border border-outline-variant rounded-xl py-3 px-4 text-body-md outline-none focus:border-primary" placeholder="Your name (new users)" value={form.name} onChange={set('name')} />
              {role === 'RETAILER' && (
                <>
                  <input className="w-full border border-outline-variant rounded-xl py-3 px-4 text-body-md outline-none focus:border-primary" placeholder="Shop name" value={form.shopName} onChange={set('shopName')} />
                  <input className="w-full border border-outline-variant rounded-xl py-3 px-4 text-body-md outline-none focus:border-primary" placeholder="Distributor code (e.g. VS-XXXX)" value={form.inviteCode} onChange={set('inviteCode')} />
                </>
              )}
              {role === 'SUPPLIER' && (
                <>
                  <input className="w-full border border-outline-variant rounded-xl py-3 px-4 text-body-md outline-none focus:border-primary" placeholder="Business name" value={form.businessName} onChange={set('businessName')} />
                  <select className="w-full border border-outline-variant rounded-xl py-3 px-4 text-body-md outline-none focus:border-primary bg-surface-container-lowest" value={form.supplierType} onChange={set('supplierType')}>
                    <option value="DISTRIBUTOR">Distributor</option>
                    <option value="WHOLESALER">Wholesaler</option>
                    <option value="SUPER_STOCKIST">Super Stockist</option>
                  </select>
                </>
              )}
              <button
                onClick={sendOtp}
                disabled={loading}
                className="w-full bg-gradient-to-r from-primary to-secondary text-on-primary py-4 rounded-xl font-bold tracking-wide shadow-lg disabled:opacity-60 flex items-center justify-center gap-2"
              >
                <span>Send OTP</span>
                <Icon name="arrow_forward" className="text-[20px]" />
              </button>
            </div>

            {import.meta.env.DEV && (
              <div className="mt-lg pt-md border-t border-outline-variant">
                <p className="text-label-sm text-on-surface-variant mb-sm text-center">Quick test login (dev)</p>
                <div className="grid grid-cols-3 gap-sm">
                  <button onClick={() => devLogin('RETAILER')} disabled={loading} className="py-2 rounded-xl border border-outline-variant text-label-md hover:bg-surface-container disabled:opacity-60">Retailer</button>
                  <button onClick={() => devLogin('SUPPLIER')} disabled={loading} className="py-2 rounded-xl border border-outline-variant text-label-md hover:bg-surface-container disabled:opacity-60">Distributor</button>
                  <button onClick={() => devLogin('ADMIN')} disabled={loading} className="py-2 rounded-xl border border-outline-variant text-label-md hover:bg-surface-container disabled:opacity-60">Admin</button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="flex flex-col gap-md">
            <h2 className="text-[20px] font-semibold text-on-surface">Enter OTP</h2>
            <p className="text-label-sm text-on-surface-variant">Sent to +91 {form.phone} (check the backend console in dev)</p>
            <input
              className="w-full border border-outline-variant rounded-xl py-4 px-4 text-headline-md text-center tracking-[0.5em] outline-none focus:border-primary"
              placeholder="······"
              inputMode="numeric"
              value={otp}
              onChange={(e) => setOtp(e.target.value)}
            />
            <button onClick={verify} disabled={loading} className="w-full bg-gradient-to-r from-primary to-secondary text-on-primary py-4 rounded-xl font-bold shadow-lg disabled:opacity-60">
              Verify & Continue
            </button>
            <button onClick={() => setStep('details')} className="text-secondary text-label-md">Change details</button>
          </div>
        )}
      </main>
    </div>
  );
}
