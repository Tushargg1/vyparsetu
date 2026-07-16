import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import { authApi } from '../../lib/api';
import { API_BASE_URL } from '../../lib/axiosClient';
import { getPasskey, isWebAuthnSupported } from '../../lib/webauthn';
import { useAuthStore } from '../../stores/authStore';

const ROLES = [
  { key: 'RETAILER', label: 'Retailer', desc: 'Purchase and manage stock for your shop', icon: 'store' },
  { key: 'SUPPLIER', label: 'Supplier / Distributor', desc: 'Sell, fulfil orders, and manage inventory', icon: 'inventory_2' },
];

const INITIAL_REGISTRATION = {
  name: '', phone: '', email: '', password: '', shopName: '', businessName: '',
  supplierType: 'DISTRIBUTOR', gstNumber: '', address: '', city: '', state: '',
  pincode: '', inviteCode: '',
};

function homeFor(roles = []) {
  if (roles.includes('SUPPLIER')) return '/supplier';
  if (roles.includes('ADMIN')) return '/admin';
  return '/';
}

function inputClasses(error) {
  return `ui-input w-full ${error ? 'border-error focus:border-error focus:ring-error/20' : ''}`;
}

function TextField({ id, label, error, hint, className = '', ...props }) {
  const descriptionId = error || hint ? `${id}-description` : undefined;
  return (
    <label htmlFor={id} className={`block ${className}`}>
      <span className="mb-xs block text-label-md font-semibold text-on-surface">{label}</span>
      <input id={id} className={inputClasses(error)} aria-invalid={Boolean(error)} aria-describedby={descriptionId} {...props} />
      {(error || hint) && (
        <span id={descriptionId} className={`mt-xs block text-label-sm ${error ? 'text-error' : 'text-on-surface-variant'}`}>
          {error || hint}
        </span>
      )}
    </label>
  );
}


function RoleSelector({ role, onChange }) {
  return (
    <fieldset>
      <legend className="mb-sm text-label-md font-semibold text-on-surface">Choose your workspace</legend>
      <div className="grid gap-sm sm:grid-cols-2">
        {ROLES.map((option) => {
          const active = role === option.key;
          return (
            <button
              key={option.key}
              type="button"
              onClick={() => onChange(option.key)}
              aria-pressed={active}
              className={`flex min-h-20 items-center gap-sm rounded-xl border p-sm text-left transition duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary focus-visible:ring-offset-2 ${
                active
                  ? 'border-secondary bg-secondary-fixed/40 shadow-sm ring-1 ring-secondary'
                  : 'border-surface-variant bg-surface-container-lowest hover:border-outline hover:bg-surface-container-low'
              }`}
            >
              <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${active ? 'bg-secondary text-on-secondary' : 'bg-surface-container text-primary'}`}>
                <Icon name={option.icon} className="text-[22px]" />
              </span>
              <span className="min-w-0">
                <span className="block text-label-md font-bold text-on-surface">{option.label}</span>
                <span className="mt-0.5 block text-label-sm leading-snug text-on-surface-variant">{option.desc}</span>
              </span>
            </button>
          );
        })}
      </div>
    </fieldset>
  );
}

function SocialButton({ provider, configured, checking, busy, onClick }) {
  const label = provider === 'google' ? 'Google' : 'Apple';
  const unavailable = checking || !configured;
  return (
    <Button
      variant="secondary"
      size="lg"
      className="w-full border-outline-variant text-on-surface hover:border-secondary"
      leadingIcon={provider === 'google' ? 'public' : 'ios'}
      disabled={unavailable || busy}
      onClick={() => onClick(provider)}
      aria-label={unavailable && !checking ? `${label} sign-in: Not configured yet` : `Continue with ${label}`}
    >
      {checking ? `Checking ${label}…` : configured ? `Continue with ${label}` : `${label} · Not configured yet`}
    </Button>
  );
}

export default function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const oauthHandled = useRef(false);

  const [tab, setTab] = useState('signin');
  const [role, setRole] = useState('RETAILER');
  const [login, setLogin] = useState({ email: '', password: '', identifier: '' });
  const [registration, setRegistration] = useState(INITIAL_REGISTRATION);
  const [providers, setProviders] = useState({ google: false, apple: false });
  const [providersLoading, setProvidersLoading] = useState(true);
  const [challengeToken, setChallengeToken] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState('');

  const busy = Boolean(actionLoading);
  const setLoginField = (key) => (event) => setLogin((current) => ({ ...current, [key]: event.target.value }));
  const setRegistrationField = (key) => (event) => setRegistration((current) => ({ ...current, [key]: event.target.value }));

  const finishAuthentication = useCallback((response) => {
    if (response?.nextStep !== 'AUTHENTICATED' || !response.accessToken || !response.user) {
      throw new Error('Sign-in could not be completed. Please try again.');
    }
    setChallengeToken('');
    setTotpCode('');
    setAuth({ accessToken: response.accessToken, refreshToken: response.refreshToken, user: response.user });
    navigate(homeFor(response.user.roles));
  }, [navigate, setAuth]);

  const authError = (requestError, fallback) => {
    if (requestError?.name === 'NotAllowedError') return 'The passkey request was cancelled or timed out.';
    return requestError?.response?.data?.error?.message || requestError?.message || fallback;
  };

  useEffect(() => {
    let active = true;
    authApi.oauthProviders()
      .then((result) => {
        if (active) setProviders({ google: Boolean(result?.google), apple: Boolean(result?.apple) });
      })
      .catch(() => {
        if (active) setProviders({ google: false, apple: false });
      })
      .finally(() => {
        if (active) setProvidersLoading(false);
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (oauthHandled.current) return;
    const url = new URL(window.location.href);
    const oauthCode = url.searchParams.get('oauthCode');
    const oauthError = url.searchParams.get('oauthError');
    if (!oauthCode && !oauthError) return;

    oauthHandled.current = true;
    url.searchParams.delete('oauthCode');
    url.searchParams.delete('oauthError');
    window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`);

    if (oauthError) {
      setError(`Social sign-in could not be completed: ${oauthError.replaceAll('_', ' ')}.`);
      return;
    }

    setActionLoading('oauth');
    setError('');
    authApi.oauthExchange({ code: oauthCode })
      .then(finishAuthentication)
      .catch((requestError) => setError(authError(requestError, 'Social sign-in could not be completed.')))
      .finally(() => setActionLoading(''));
  }, [finishAuthentication]);

  const selectTab = (nextTab) => {
    setTab(nextTab);
    setError('');
    setFieldErrors({});
    setChallengeToken('');
    setTotpCode('');
  };

  const passwordLogin = async (event) => {
    event.preventDefault();
    const errors = {};
    if (!/^\S+@\S+\.\S+$/.test(login.email.trim())) errors.email = 'Enter a valid email address.';
    if (!login.password) errors.password = 'Enter your password.';
    setFieldErrors(errors);
    setError('');
    if (Object.keys(errors).length) return;

    setActionLoading('password');
    try {
      const response = await authApi.passwordLogin({ email: login.email.trim(), password: login.password });
      finishAuthentication(response);
    } catch (requestError) {
      setError(authError(requestError, 'Email or password is incorrect.'));
    } finally {
      setActionLoading('');
    }
  };

  const register = async (event) => {
    event.preventDefault();
    const errors = {};
    if (!registration.name.trim()) errors.name = 'Enter your full name.';
    if (!/^[6-9]\d{9}$/.test(registration.phone)) errors.phone = 'Enter a valid Indian 10-digit mobile number.';
    if (!/^\S+@\S+\.\S+$/.test(registration.email.trim())) errors.email = 'Enter a valid email address.';
    if (registration.password.length < 8 || registration.password.length > 72) errors.password = 'Use 8 to 72 characters.';
    if (role === 'RETAILER' && !registration.shopName.trim()) errors.shopName = 'Enter your shop name.';
    if (role === 'SUPPLIER' && !registration.businessName.trim()) errors.businessName = 'Enter your business name.';
    if (registration.gstNumber && !/^[0-9A-Z]{15}$/.test(registration.gstNumber.trim().toUpperCase())) errors.gstNumber = 'GST number must contain 15 letters and digits.';
    if (registration.pincode && !/^\d{6}$/.test(registration.pincode)) errors.pincode = 'Enter a valid 6-digit pincode.';
    setFieldErrors(errors);
    setError('');
    if (Object.keys(errors).length) return;

    setActionLoading('register');
    try {
      const response = await authApi.register({
        name: registration.name.trim(),
        phone: registration.phone,
        email: registration.email.trim(),
        password: registration.password,
        role,
        shopName: role === 'RETAILER' ? registration.shopName.trim() : undefined,
        businessName: role === 'SUPPLIER' ? registration.businessName.trim() : undefined,
        supplierType: role === 'SUPPLIER' ? registration.supplierType : undefined,
        gstNumber: role === 'SUPPLIER' ? registration.gstNumber.trim().toUpperCase() : undefined,
        address: registration.address.trim(),
        city: registration.city.trim(),
        state: registration.state.trim(),
        pincode: registration.pincode,
        inviteCode: role === 'RETAILER' ? registration.inviteCode.trim() : undefined,
      });
      finishAuthentication(response);
    } catch (requestError) {
      setError(authError(requestError, 'Your account could not be created.'));
    } finally {
      setActionLoading('');
    }
  };

  const secureIdentifier = login.identifier.trim() || login.email.trim();

  const signInWithPasskey = async () => {
    setError('');
    if (!secureIdentifier) {
      setFieldErrors({ identifier: 'Enter your email or mobile number.' });
      return;
    }
    if (!isWebAuthnSupported()) {
      setError('Passkeys are not supported in this browser or connection.');
      return;
    }
    setFieldErrors({});
    setActionLoading('passkey');
    try {
      const ceremony = await authApi.passkeyOptions({ identifier: secureIdentifier });
      const credential = await getPasskey(ceremony.options);
      const response = await authApi.verifyPasskey({ ceremonyId: ceremony.ceremonyId, credential });
      finishAuthentication(response);
    } catch (requestError) {
      setError(authError(requestError, 'Could not sign in with this passkey.'));
    } finally {
      setActionLoading('');
    }
  };

  const startTotpSignIn = async () => {
    setError('');
    if (!secureIdentifier) {
      setFieldErrors({ identifier: 'Enter your email or mobile number.' });
      return;
    }
    setFieldErrors({});
    setActionLoading('totp-options');
    try {
      const response = await authApi.totpOptions({ identifier: secureIdentifier });
      setChallengeToken(response.challengeToken);
      setTotpCode('');
    } catch (requestError) {
      setError(authError(requestError, 'Authenticator sign-in is not available for this account.'));
    } finally {
      setActionLoading('');
    }
  };

  const verifyTotp = async (event) => {
    event.preventDefault();
    setError('');
    if (!/^\d{6}$/.test(totpCode)) {
      setFieldErrors({ totpCode: 'Enter the 6-digit code from your authenticator app.' });
      return;
    }
    setFieldErrors({});
    setActionLoading('totp-verify');
    try {
      const response = await authApi.verifyTotp({ challengeToken, code: totpCode });
      finishAuthentication(response);
    } catch (requestError) {
      setError(authError(requestError, 'Invalid authenticator code.'));
    } finally {
      setActionLoading('');
    }
  };

  const demoLogin = async (demoRole) => {
    setError('');
    setFieldErrors({});
    setActionLoading(`demo-${demoRole}`);
    try {
      const response = await authApi.demoLogin(demoRole);
      finishAuthentication(response);
    } catch (requestError) {
      setError(authError(requestError, 'Demo access is currently unavailable.'));
    } finally {
      setActionLoading('');
    }
  };

  const startOAuth = (provider) => {
    if (!providers[provider] || busy) return;
    setActionLoading(`oauth-${provider}`);
    window.location.assign(`${API_BASE_URL}/auth/oauth/${provider}/start?role=${encodeURIComponent(role)}`);
  };

  return (
    <div className="min-h-screen bg-mesh p-md sm:p-lg">
      <main className="mx-auto grid min-h-[calc(100vh-2rem)] w-full max-w-7xl overflow-hidden rounded-[32px] border border-surface-variant bg-surface-container-lowest shadow-xl sm:min-h-[calc(100vh-3rem)] lg:grid-cols-[0.86fr_1.14fr]">
        <aside className="relative hidden overflow-hidden bg-primary p-2xl text-on-primary lg:flex lg:flex-col lg:justify-between">
          <div className="relative z-10 flex items-center gap-sm text-body-lg font-bold">
            <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-on-primary/10">
              <Icon name="storefront" className="text-[24px]" />
            </span>
            VyaparMantra
          </div>

          <div className="relative z-10 max-w-lg py-2xl">
            <span className="inline-flex items-center gap-xs rounded-full bg-on-primary/10 px-3 py-1.5 text-label-sm text-primary-fixed">
              <Icon name="verified" className="text-[16px]" /> Built for Indian businesses
            </span>
            <h2 className="mt-lg text-display-lg font-bold leading-tight">Your business, connected and in control.</h2>
            <p className="mt-md text-body-lg leading-relaxed text-primary-fixed-dim">
              Bring orders, inventory, retailer relationships, and smarter decisions into one dependable workspace.
            </p>
          </div>

          <div className="relative z-10 grid grid-cols-3 gap-sm">
            {[
              ['inventory_2', 'Stock'],
              ['receipt_long', 'Orders'],
              ['auto_awesome', 'Insights'],
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

        <section className="flex justify-center overflow-y-auto p-lg sm:p-xl lg:p-2xl">
          <div className="w-full max-w-2xl self-start py-sm lg:py-lg">
            <header className="mb-xl">
              <div className="mb-lg flex items-center gap-sm lg:hidden">
                <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary text-on-primary shadow-sm">
                  <Icon name="storefront" className="text-[24px]" />
                </span>
                <div>
                  <p className="text-body-lg font-bold text-primary">VyaparMantra</p>
                  <p className="text-label-sm text-on-surface-variant">B2B commerce, made simpler</p>
                </div>
              </div>
              <p className="text-label-md font-semibold text-secondary">WELCOME</p>
              <h1 className="mt-xs text-headline-lg text-on-surface">
                {tab === 'signin' ? 'Sign in to your workspace' : 'Create your business account'}
              </h1>
              <p className="mt-sm text-body-md text-on-surface-variant">
                {tab === 'signin' ? 'Continue securely with your preferred sign-in method.' : 'Set up your workspace and start managing business in minutes.'}
              </p>
            </header>

            <div className="mb-lg grid grid-cols-2 rounded-xl bg-surface-container p-xs" role="tablist" aria-label="Authentication options">
              {[
                ['signin', 'Sign in'],
                ['register', 'Create account'],
              ].map(([key, label]) => (
                <button
                  key={key}
                  type="button"
                  role="tab"
                  aria-selected={tab === key}
                  onClick={() => selectTab(key)}
                  className={`min-h-11 rounded-lg px-md text-label-md font-semibold transition duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary ${
                    tab === key ? 'bg-surface-container-lowest text-primary shadow-sm' : 'text-on-surface-variant hover:text-on-surface'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>

            {error && (
              <div className="mb-lg flex items-start gap-sm rounded-xl border border-error/20 bg-error-container p-md text-on-error-container" role="alert">
                <Icon name="error" className="mt-0.5 shrink-0 text-[20px]" />
                <span className="text-label-md leading-relaxed">{error}</span>
              </div>
            )}

            {actionLoading === 'oauth' && (
              <div className="mb-lg flex items-center gap-sm rounded-xl border border-secondary/20 bg-secondary-fixed/40 p-md text-secondary" role="status">
                <Icon name="progress_activity" className="animate-spin" />
                <span className="text-label-md font-semibold">Completing secure social sign-in…</span>
              </div>
            )}

            <div className="space-y-lg">
              <RoleSelector role={role} onChange={setRole} />

              <section aria-labelledby="social-heading">
                <h2 id="social-heading" className="sr-only">Social sign-in</h2>
                <div className="grid gap-sm sm:grid-cols-2">
                  <SocialButton provider="google" configured={providers.google} checking={providersLoading} busy={busy} onClick={startOAuth} />
                  <SocialButton provider="apple" configured={providers.apple} checking={providersLoading} busy={busy} onClick={startOAuth} />
                </div>
              </section>

              <div className="flex items-center gap-md text-label-sm text-on-surface-variant" aria-hidden="true">
                <span className="h-px flex-1 bg-outline-variant" />
                {tab === 'signin' ? 'or use your password' : 'or register with email'}
                <span className="h-px flex-1 bg-outline-variant" />
              </div>

              {tab === 'signin' ? (
                <div className="space-y-lg" role="tabpanel">
                  <form className="space-y-md" onSubmit={passwordLogin} noValidate>
                    <TextField
                      id="login-email"
                      label="Email address"
                      type="email"
                      autoComplete="email"
                      placeholder="you@business.com"
                      value={login.email}
                      onChange={setLoginField('email')}
                      error={fieldErrors.email}
                    />
                    <TextField
                      id="login-password"
                      label="Password"
                      type="password"
                      autoComplete="current-password"
                      placeholder="Enter your password"
                      value={login.password}
                      onChange={setLoginField('password')}
                      error={fieldErrors.password}
                    />
                    <Button type="submit" size="lg" className="w-full" trailingIcon="arrow_forward" loading={actionLoading === 'password'} disabled={busy && actionLoading !== 'password'}>
                      Sign in securely
                    </Button>
                  </form>

                  <section className="rounded-2xl border border-surface-variant bg-surface-container-low p-md sm:p-lg" aria-labelledby="secure-options-heading">
                    <div className="flex items-start gap-sm">
                      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-secondary-fixed text-secondary">
                        <Icon name="shield_lock" />
                      </span>
                      <div>
                        <h2 id="secure-options-heading" className="text-body-md font-bold text-on-surface">Password-free options</h2>
                        <p className="mt-0.5 text-label-sm text-on-surface-variant">Use the email or phone linked to your account.</p>
                      </div>
                    </div>
                    <div className="mt-md space-y-sm">
                      <TextField
                        id="secure-identifier"
                        label="Email or mobile number"
                        type="text"
                        autoComplete="username webauthn"
                        placeholder="Email or 10-digit mobile number"
                        value={login.identifier}
                        onChange={setLoginField('identifier')}
                        error={fieldErrors.identifier}
                        hint={!login.identifier && login.email ? `Using ${login.email}` : undefined}
                      />
                      <div className="grid gap-sm sm:grid-cols-2">
                        <Button variant="secondary" size="lg" className="w-full" leadingIcon="passkey" loading={actionLoading === 'passkey'} disabled={busy && actionLoading !== 'passkey'} onClick={signInWithPasskey}>
                          Use passkey
                        </Button>
                        <Button variant="ghost" size="lg" className="w-full" leadingIcon="phonelink_lock" loading={actionLoading === 'totp-options'} disabled={busy && actionLoading !== 'totp-options'} onClick={startTotpSignIn}>
                          Use authenticator
                        </Button>
                      </div>
                    </div>

                    {challengeToken && (
                      <form onSubmit={verifyTotp} className="mt-md space-y-sm rounded-xl border border-secondary/20 bg-surface-container-lowest p-md">
                        <div>
                          <h3 className="text-label-md font-bold text-on-surface">Authenticator verification</h3>
                          <p className="mt-0.5 text-label-sm text-on-surface-variant">Enter the 6-digit code from your authenticator app.</p>
                        </div>
                        <TextField
                          id="authenticator-code"
                          label="6-digit code"
                          inputMode="numeric"
                          autoComplete="one-time-code"
                          maxLength={6}
                          placeholder="000000"
                          value={totpCode}
                          onChange={(event) => setTotpCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                          error={fieldErrors.totpCode}
                          className="[&_input]:text-center [&_input]:tracking-[0.35em]"
                        />
                        <div className="flex flex-col gap-sm sm:flex-row">
                          <Button type="submit" className="flex-1" loading={actionLoading === 'totp-verify'} disabled={busy && actionLoading !== 'totp-verify'}>
                            Verify & continue
                          </Button>
                          <Button variant="ghost" className="flex-1" disabled={busy} onClick={() => { setChallengeToken(''); setTotpCode(''); setFieldErrors({}); }}>
                            Cancel
                          </Button>
                        </div>
                      </form>
                    )}
                  </section>
                </div>
              ) : (
                <form className="space-y-lg" onSubmit={register} noValidate role="tabpanel">
                  <section className="space-y-md" aria-labelledby="account-details-heading">
                    <div>
                      <h2 id="account-details-heading" className="text-body-lg font-bold text-on-surface">Account details</h2>
                      <p className="mt-xs text-label-sm text-on-surface-variant">Use details you can access for secure account recovery.</p>
                    </div>
                    <div className="grid gap-md sm:grid-cols-2">
                      <TextField id="register-name" label="Full name" autoComplete="name" placeholder="Your full name" value={registration.name} onChange={setRegistrationField('name')} error={fieldErrors.name} />
                      <TextField id="register-phone" label="Mobile number" type="tel" inputMode="numeric" autoComplete="tel-national" maxLength={10} placeholder="10-digit Indian number" value={registration.phone} onChange={(event) => setRegistration((current) => ({ ...current, phone: event.target.value.replace(/\D/g, '').slice(0, 10) }))} error={fieldErrors.phone} hint="India (+91)" />
                      <TextField id="register-email" label="Email address" type="email" autoComplete="email" placeholder="you@business.com" value={registration.email} onChange={setRegistrationField('email')} error={fieldErrors.email} />
                      <TextField id="register-password" label="Password" type="password" autoComplete="new-password" placeholder="8–72 characters" minLength={8} maxLength={72} value={registration.password} onChange={setRegistrationField('password')} error={fieldErrors.password} hint="Use at least 8 characters." />
                    </div>
                  </section>

                  <section className="space-y-md border-t border-surface-variant pt-lg" aria-labelledby="business-details-heading">
                    <div>
                      <h2 id="business-details-heading" className="text-body-lg font-bold text-on-surface">Business details</h2>
                      <p className="mt-xs text-label-sm text-on-surface-variant">Tell us how your business operates.</p>
                    </div>
                    <div className="grid gap-md sm:grid-cols-2">
                      {role === 'RETAILER' ? (
                        <>
                          <TextField id="register-shop-name" label="Shop name" autoComplete="organization" placeholder="Your shop name" value={registration.shopName} onChange={setRegistrationField('shopName')} error={fieldErrors.shopName} />
                          <TextField id="register-invite" label="Distributor invite code (optional)" placeholder="e.g. VS-XXXX" value={registration.inviteCode} onChange={setRegistrationField('inviteCode')} />
                        </>
                      ) : (
                        <>
                          <TextField id="register-business-name" label="Business name" autoComplete="organization" placeholder="Registered business name" value={registration.businessName} onChange={setRegistrationField('businessName')} error={fieldErrors.businessName} />
                          <label htmlFor="register-supplier-type" className="block">
                            <span className="mb-xs block text-label-md font-semibold text-on-surface">Supplier type</span>
                            <select id="register-supplier-type" className="ui-input w-full bg-surface-container-lowest" value={registration.supplierType} onChange={setRegistrationField('supplierType')}>
                              <option value="DISTRIBUTOR">Distributor</option>
                              <option value="WHOLESALER">Wholesaler</option>
                              <option value="SUPER_STOCKIST">Super Stockist</option>
                            </select>
                          </label>
                          <TextField id="register-gst" label="GST number (optional)" autoCapitalize="characters" maxLength={15} placeholder="15-character GSTIN" value={registration.gstNumber} onChange={setRegistrationField('gstNumber')} error={fieldErrors.gstNumber} className="sm:col-span-2" />
                        </>
                      )}
                    </div>
                  </section>

                  <section className="space-y-md border-t border-surface-variant pt-lg" aria-labelledby="location-details-heading">
                    <div>
                      <h2 id="location-details-heading" className="text-body-lg font-bold text-on-surface">Business location <span className="text-label-sm font-medium text-on-surface-variant">(optional)</span></h2>
                      <p className="mt-xs text-label-sm text-on-surface-variant">Add your location now to make business records easier to manage.</p>
                    </div>
                    <div className="grid gap-md sm:grid-cols-2">
                      <TextField id="register-address" label="Street address" autoComplete="street-address" placeholder="Building, street, area" value={registration.address} onChange={setRegistrationField('address')} className="sm:col-span-2" />
                      <TextField id="register-city" label="City" autoComplete="address-level2" placeholder="City" value={registration.city} onChange={setRegistrationField('city')} />
                      <TextField id="register-state" label="State" autoComplete="address-level1" placeholder="State" value={registration.state} onChange={setRegistrationField('state')} />
                      <TextField id="register-pincode" label="Pincode" inputMode="numeric" autoComplete="postal-code" maxLength={6} placeholder="6-digit pincode" value={registration.pincode} onChange={(event) => setRegistration((current) => ({ ...current, pincode: event.target.value.replace(/\D/g, '').slice(0, 6) }))} error={fieldErrors.pincode} />
                    </div>
                  </section>

                  <div className="rounded-xl bg-surface-container-low p-md text-label-sm leading-relaxed text-on-surface-variant">
                    By creating an account, you confirm these business details are accurate and belong to you.
                  </div>
                  <Button type="submit" size="lg" className="w-full" trailingIcon="arrow_forward" loading={actionLoading === 'register'} disabled={busy && actionLoading !== 'register'}>
                    Create account
                  </Button>
                </form>
              )}

              <section className="border-t border-surface-variant pt-lg" aria-labelledby="demo-heading">
                <div className="mb-sm flex items-center justify-between gap-sm">
                  <div>
                    <h2 id="demo-heading" className="text-body-md font-bold text-on-surface">Explore a ready-to-use workspace</h2>
                    <p className="mt-0.5 text-label-sm text-on-surface-variant">Explore shared sample data without creating an account.</p>
                  </div>
                  <span className="hidden rounded-full bg-tertiary-container px-3 py-1 text-label-sm font-semibold text-on-tertiary-container sm:inline-flex">Instant access</span>
                </div>
                <div className="grid gap-sm sm:grid-cols-2">
                  <Button variant="secondary" size="lg" className="w-full border-outline-variant text-on-surface" leadingIcon="store" loading={actionLoading === 'demo-RETAILER'} disabled={busy && actionLoading !== 'demo-RETAILER'} onClick={() => demoLogin('RETAILER')}>
                    Retailer demo
                  </Button>
                  <Button variant="secondary" size="lg" className="w-full border-outline-variant text-on-surface" leadingIcon="inventory_2" loading={actionLoading === 'demo-SUPPLIER'} disabled={busy && actionLoading !== 'demo-SUPPLIER'} onClick={() => demoLogin('SUPPLIER')}>
                    Distributor demo
                  </Button>
                </div>
              </section>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
