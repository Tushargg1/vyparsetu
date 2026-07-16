import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../lib/api';
import { getPasskey, isWebAuthnSupported } from '../../lib/webauthn';
import { useAuthStore } from '../../stores/authStore';
import Button from '../../components/Button';
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
  const [totpCode, setTotpCode] = useState('');
  const [challengeToken, setChallengeToken] = useState('');
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

  const finishAuthentication = (res) => {
    if (res.nextStep !== 'AUTHENTICATED' || !res.accessToken || !res.user) {
      throw new Error('Sign-in could not be completed. Please try again.');
    }
    setChallengeToken('');
    setTotpCode('');
    setAuth({ accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user });
    navigate(homeFor(res.user.roles || []));
  };

  const authError = (e, fallback) => {
    if (e?.name === 'NotAllowedError') return 'The passkey request was cancelled or timed out.';
    return e.response?.data?.error?.message || e.message || fallback;
  };

  const verify = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await authApi.verifyOtp({ identifier: form.phone, code: otp, purpose });
      finishAuthentication(res);
    } catch (e) {
      setError(authError(e, 'Invalid OTP'));
    } finally {
      setLoading(false);
    }
  };

  const signInWithPasskey = async () => {
    setError('');
    if (!isWebAuthnSupported()) {
      setError('Passkeys are not supported in this browser or connection.');
      return;
    }
    setLoading(true);
    try {
      const ceremony = await authApi.passkeyOptions({ identifier: form.phone });
      const credential = await getPasskey(ceremony.options);
      const res = await authApi.verifyPasskey({ ceremonyId: ceremony.ceremonyId, credential });
      finishAuthentication(res);
    } catch (e) {
      setError(authError(e, 'Could not sign in with this passkey.'));
    } finally {
      setLoading(false);
    }
  };

  const startTotpSignIn = async () => {
    setError('');
    setLoading(true);
    try {
      const result = await authApi.totpOptions({ identifier: form.phone });
      setChallengeToken(result.challengeToken);
      setTotpCode('');
      setStep('totp');
    } catch (e) {
      setError(authError(e, 'Authenticator sign-in is not available for this account.'));
    } finally {
      setLoading(false);
    }
  };

  const verifyTotp = async () => {
    setError('');
    if (!/^\d{6}$/.test(totpCode)) {
      setError('Enter the 6-digit code from your authenticator app.');
      return;
    }
    setLoading(true);
    try {
      const res = await authApi.verifyTotp({ challengeToken, code: totpCode });
      finishAuthentication(res);
    } catch (e) {
      setError(authError(e, 'Invalid authenticator code.'));
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
    <div className="bg-mesh min-h-screen p-md sm:p-lg">
      <main className="mx-auto grid min-h-[calc(100vh-2rem)] w-full max-w-6xl overflow-hidden rounded-[32px] border border-surface-variant bg-surface-container-lowest shadow-xl sm:min-h-[calc(100vh-3rem)] lg:grid-cols-[1.05fr_0.95fr]">
        <aside className="relative hidden overflow-hidden bg-primary p-2xl text-on-primary lg:flex lg:flex-col lg:justify-between">
          <div className="relative z-10 flex items-center gap-sm text-body-lg font-bold">
            <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-on-primary/10">
              <Icon name="storefront" className="text-[24px]" />
            </span>
            VyaparMantra
          </div>
          <div className="relative z-10 max-w-lg">
            <span className="inline-flex items-center gap-xs rounded-full bg-on-primary/10 px-3 py-1.5 text-label-sm text-primary-fixed">
              <Icon name="verified" className="text-[16px]" /> Built for Indian businesses
            </span>
            <h2 className="mt-lg text-display-lg font-bold leading-tight">Run your business with clarity and confidence.</h2>
            <p className="mt-md text-body-lg text-primary-fixed-dim">Orders, inventory, retailers, payments, and AI-assisted commerce—together in one calm workspace.</p>
          </div>
          <div className="relative z-10 grid grid-cols-3 gap-sm">
            {[
              ['inventory_2', 'Stock'],
              ['receipt_long', 'Orders'],
              ['auto_awesome', 'AI insights'],
            ].map(([icon, label]) => (
              <div key={label} className="rounded-2xl border border-on-primary/10 bg-on-primary/5 p-md">
                <Icon name={icon} className="text-primary-fixed" />
                <p className="mt-sm text-label-md font-semibold">{label}</p>
              </div>
            ))}
          </div>
          <span className="absolute -right-20 -top-20 h-72 w-72 rounded-full bg-secondary/30 blur-3xl" />
          <span className="absolute -bottom-24 -left-24 h-72 w-72 rounded-full bg-on-tertiary-container/20 blur-3xl" />
        </aside>

        <section className="flex items-center justify-center p-lg sm:p-xl lg:p-2xl">
          <div className="w-full max-w-md">
        <div className="mb-xl flex flex-col items-center lg:hidden">
          <div className="w-16 h-16 bg-gradient-to-br from-primary to-primary-container rounded-2xl flex items-center justify-center mb-md shadow-lg">
            <Icon name="storefront" className="text-on-primary text-[32px]" />
          </div>
          <h1 className="text-headline-lg text-primary">VyaparMantra</h1>
          <p className="text-body-md text-on-surface-variant mt-xs">Empowering Indian B2B Commerce</p>
        </div>

        {error && (
          <div className="mb-md flex items-start gap-sm rounded-xl border border-error-container bg-error-container/60 p-md text-on-error-container" role="alert">
            <Icon name="error" className="mt-0.5 shrink-0 text-[18px]" />
            <span className="text-label-md">{error}</span>
          </div>
        )}

        {step === 'details' ? (
          <>
          <h2 className="mb-md text-body-lg font-semibold text-on-surface">Choose your workspace</h2>
            <div className="grid grid-cols-1 gap-sm mb-xl">
              {ROLES.map((r) => {
                const active = role === r.key;
                return (
                  <button
                    key={r.key}
                    onClick={() => setRole(r.key)}
                    aria-pressed={active}
                    className={`w-full flex items-center p-md rounded-2xl border text-left transition-all ${
                      active ? 'border-secondary bg-secondary-fixed/40 shadow-sm ring-1 ring-secondary' : 'border-surface-variant hover:border-primary/40 hover:bg-surface-container-low'
                    }`}
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

            <h2 className="mb-md text-body-lg font-semibold text-on-surface">Sign in or create an account</h2>
            <div className="flex flex-col gap-md">
              <div className="flex">
                <span className="inline-flex items-center px-4 rounded-l-xl border border-r-0 border-outline-variant bg-surface-container text-on-surface-variant">+91</span>
                <input
                  className="ui-input rounded-l-none"
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
                className="ui-button-primary mt-xs w-full py-3.5"
              >
                <span>Send OTP</span>
                <Icon name="arrow_forward" className="text-[20px]" />
              </button>
              {/^[6-9]\d{9}$/.test(form.phone) && (
                <>
                  <div className="flex items-center gap-sm text-label-sm text-on-surface-variant" aria-hidden="true">
                    <span className="h-px flex-1 bg-outline-variant" />or<span className="h-px flex-1 bg-outline-variant" />
                  </div>
                  <Button
                    variant="secondary"
                    size="lg"
                    className="w-full"
                    leadingIcon="passkey"
                    loading={loading}
                    onClick={signInWithPasskey}
                  >
                    Sign in with passkey
                  </Button>
                  <Button
                    variant="ghost"
                    size="lg"
                    className="w-full"
                    leadingIcon="phonelink_lock"
                    loading={loading}
                    onClick={startTotpSignIn}
                  >
                    Sign in with authenticator
                  </Button>
                </>
              )}
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
        ) : step === 'otp' ? (
          <div className="flex flex-col gap-md">
            <h2 className="text-headline-md font-semibold text-on-surface">Enter your verification code</h2>
            <p className="text-label-sm text-on-surface-variant">Sent to +91 {form.phone} (check the backend console in dev)</p>
            <input
              className="w-full border border-outline-variant rounded-xl py-4 px-4 text-headline-md text-center tracking-[0.5em] outline-none focus:border-primary"
              placeholder="······"
              inputMode="numeric"
              value={otp}
              onChange={(e) => setOtp(e.target.value)}
            />
            <button onClick={verify} disabled={loading} className="ui-button-primary w-full py-3.5">
              Verify & Continue
            </button>
            <button onClick={() => setStep('details')} className="min-h-11 rounded-xl text-secondary text-label-md font-semibold hover:bg-secondary-fixed/40">Change details</button>
          </div>
        ) : (
          <div className="flex flex-col gap-md">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-secondary-fixed text-secondary">
              <Icon name="phonelink_lock" />
            </div>
            <h2 className="text-headline-md font-semibold text-on-surface">Authenticator verification</h2>
            <p className="text-label-sm text-on-surface-variant">Enter the 6-digit code from your authenticator app to finish signing in.</p>
            <input
              className="ui-input py-md text-center text-headline-md tracking-widest"
              aria-label="Authenticator code"
              placeholder="······"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              value={totpCode}
              onChange={(e) => setTotpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              onKeyDown={(e) => { if (e.key === 'Enter') verifyTotp(); }}
            />
            <Button size="lg" className="w-full" loading={loading} onClick={verifyTotp}>
              Verify & Continue
            </Button>
            <Button
              variant="ghost"
              className="w-full"
              onClick={() => { setChallengeToken(''); setTotpCode(''); setStep('details'); }}
            >
              Start over
            </Button>
          </div>
        )}
          </div>
        </section>
      </main>
    </div>
  );
}
