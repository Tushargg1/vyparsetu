import { useEffect, useState } from 'react';
import { securityApi } from '../lib/api';
import { createPasskey, isWebAuthnSupported } from '../lib/webauthn';
import Button from './Button';
import Icon from './Icon';

/**
 * Account security section for profile screens. Loads the current security status
 * and manages passkey registration/removal plus TOTP setup, confirmation, and disable.
 * Pending ceremonies and TOTP secrets remain only in component memory.
 */
function apiMessage(error, fallback) {
  if (error?.name === 'NotAllowedError') return 'The passkey request was cancelled or timed out.';
  if (error?.name === 'InvalidStateError') return 'This passkey is already registered.';
  return error?.response?.data?.error?.message || error?.message || fallback;
}

function formatDate(value) {
  if (!value) return 'Never used';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value));
}

export default function SecurityMethods() {
  const [status, setStatus] = useState(null);
  const [setup, setSetup] = useState(null);
  const [qrCode, setQrCode] = useState('');
  const [passkeyName, setPasskeyName] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState('status');

  const loadStatus = async () => {
    setBusy('status');
    try {
      setStatus(await securityApi.status());
    } catch (error) {
      setMessage(apiMessage(error, 'Could not load security methods.'));
    } finally {
      setBusy('');
    }
  };

  useEffect(() => { loadStatus(); }, []);

  useEffect(() => {
    let active = true;
    if (!setup?.otpauthUri) {
      setQrCode('');
      return undefined;
    }
    import('qrcode')
      .then(({ default: QRCode }) => QRCode.toDataURL(setup.otpauthUri))
      .then((url) => { if (active) setQrCode(url); })
      .catch(() => { if (active) setMessage('Could not render the QR code. Use the manual secret instead.'); });
    return () => { active = false; };
  }, [setup]);

  const addPasskey = async () => {
    setMessage('');
    if (!isWebAuthnSupported()) {
      setMessage('Passkeys are not supported in this browser or connection.');
      return;
    }
    setBusy('passkey');
    try {
      const ceremony = await securityApi.passkeyOptions();
      const credential = await createPasskey(ceremony.options);
      await securityApi.verifyPasskey({
        ceremonyId: ceremony.ceremonyId,
        credential,
        name: passkeyName.trim() || 'My passkey',
      });
      setPasskeyName('');
      setMessage('Passkey added.');
      setStatus(await securityApi.status());
    } catch (error) {
      setMessage(apiMessage(error, 'Could not add this passkey.'));
    } finally {
      setBusy('');
    }
  };

  const removePasskey = async (id) => {
    setMessage('');
    setBusy(`delete-${id}`);
    try {
      await securityApi.deletePasskey(id);
      setMessage('Passkey removed.');
      setStatus(await securityApi.status());
    } catch (error) {
      setMessage(apiMessage(error, 'Could not remove this passkey.'));
    } finally {
      setBusy('');
    }
  };

  const beginTotpSetup = async () => {
    setMessage('');
    setTotpCode('');
    setBusy('totp-setup');
    try {
      setSetup(await securityApi.setupTotp());
    } catch (error) {
      setMessage(apiMessage(error, 'Could not start authenticator setup.'));
    } finally {
      setBusy('');
    }
  };

  const confirmTotp = async () => {
    if (!/^\d{6}$/.test(totpCode)) {
      setMessage('Enter the 6-digit code from your authenticator app.');
      return;
    }
    setMessage('');
    setBusy('totp-confirm');
    try {
      await securityApi.confirmTotp({ code: totpCode });
      setSetup(null);
      setTotpCode('');
      setMessage('Authenticator verification enabled.');
      setStatus(await securityApi.status());
    } catch (error) {
      setMessage(apiMessage(error, 'The authenticator code could not be verified.'));
    } finally {
      setBusy('');
    }
  };

  const disableTotp = async () => {
    if (!/^\d{6}$/.test(totpCode)) {
      setMessage('Enter the current 6-digit authenticator code to disable verification.');
      return;
    }
    setMessage('');
    setBusy('totp-disable');
    try {
      await securityApi.disableTotp({ code: totpCode });
      setTotpCode('');
      setMessage('Authenticator verification disabled.');
      setStatus(await securityApi.status());
    } catch (error) {
      setMessage(apiMessage(error, 'Could not disable authenticator verification.'));
    } finally {
      setBusy('');
    }
  };

  return (
    <section className="ui-card space-y-lg p-lg sm:p-xl" aria-labelledby="security-methods-title">
      <div>
        <h2 id="security-methods-title" className="text-headline-md text-on-surface">Security methods</h2>
        <p className="mt-xs text-label-md text-on-surface-variant">Use passkeys and an authenticator app to protect your account.</p>
      </div>

      {message && (
        <div className="rounded-xl bg-surface-container p-md text-label-md text-on-surface" role="status">
          {message}
        </div>
      )}

      <div className="space-y-md border-b border-surface-variant pb-lg">
        <div className="flex items-start gap-sm">
          <Icon name="passkey" className="text-primary" />
          <div>
            <h3 className="text-body-lg font-semibold text-on-surface">Passkeys</h3>
            <p className="text-label-sm text-on-surface-variant">Sign in securely with your device lock, fingerprint, or face.</p>
          </div>
        </div>

        <div className="flex flex-col gap-sm sm:flex-row">
          <label className="min-w-0 flex-1">
            <span className="sr-only">Passkey name</span>
            <input
              className="ui-input"
              value={passkeyName}
              maxLength={80}
              onChange={(event) => setPasskeyName(event.target.value)}
              placeholder="Name this passkey, e.g. Office laptop"
            />
          </label>
          <Button onClick={addPasskey} loading={busy === 'passkey'} leadingIcon="add">Add passkey</Button>
        </div>

        {busy === 'status' ? (
          <p className="text-label-md text-on-surface-variant">Loading passkeys…</p>
        ) : status?.passkeys?.length ? (
          <ul className="divide-y divide-surface-variant rounded-xl border border-surface-variant">
            {status.passkeys.map((passkey) => (
              <li key={passkey.id} className="flex flex-col gap-sm p-md sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <p className="truncate text-label-md text-on-surface">{passkey.name || 'Passkey'}</p>
                  <p className="text-label-sm text-on-surface-variant">
                    Added {formatDate(passkey.createdAt)} · Last used {formatDate(passkey.lastUsedAt)}
                  </p>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  leadingIcon="delete"
                  loading={busy === `delete-${passkey.id}`}
                  onClick={() => removePasskey(passkey.id)}
                >Remove</Button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-label-md text-on-surface-variant">No passkeys added yet.</p>
        )}
      </div>

      <div className="space-y-md">
        <div className="flex items-start gap-sm">
          <Icon name="phonelink_lock" className="text-primary" />
          <div>
            <h3 className="text-body-lg font-semibold text-on-surface">Authenticator app</h3>
            <p className="text-label-sm text-on-surface-variant">Use a rotating 6-digit code as a free sign-in method.</p>
          </div>
        </div>

        {status?.totpEnabled ? (
          <div className="space-y-sm">
            <p className="inline-flex rounded-full bg-tertiary-fixed-dim px-sm py-xs text-label-sm text-on-tertiary-fixed">Enabled</p>
            <div className="flex flex-col gap-sm sm:flex-row">
              <input
                className="ui-input sm:max-w-xs"
                aria-label="Current authenticator code"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={totpCode}
                onChange={(event) => setTotpCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                placeholder="6-digit code"
              />
              <Button variant="danger" onClick={disableTotp} loading={busy === 'totp-disable'}>Disable authenticator</Button>
            </div>
          </div>
        ) : setup ? (
          <div className="grid gap-lg rounded-xl bg-surface-container-low p-md sm:grid-cols-2">
            <div className="flex items-center justify-center rounded-xl bg-surface-container-lowest p-sm">
              {qrCode ? <img className="w-full" src={qrCode} alt="Authenticator setup QR code" /> : <span className="text-label-sm">Preparing QR code…</span>}
            </div>
            <div className="min-w-0 space-y-md">
              <div>
                <p className="text-label-md text-on-surface">Scan the QR code in your authenticator app.</p>
                <p className="mt-sm text-label-sm text-on-surface-variant">Or enter this secret manually:</p>
                <code className="mt-xs block break-all rounded-lg bg-surface-container-lowest p-sm text-label-md text-on-surface">{setup.secret}</code>
              </div>
              <input
                className="ui-input"
                aria-label="Authenticator confirmation code"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={totpCode}
                onChange={(event) => setTotpCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                placeholder="Enter the 6-digit code"
              />
              <div className="flex flex-wrap gap-sm">
                <Button onClick={confirmTotp} loading={busy === 'totp-confirm'}>Confirm and enable</Button>
                <Button variant="ghost" onClick={() => { setSetup(null); setTotpCode(''); }}>Cancel</Button>
              </div>
            </div>
          </div>
        ) : (
          <Button variant="secondary" onClick={beginTotpSetup} loading={busy === 'totp-setup'} leadingIcon="qr_code_2">
            Set up authenticator
          </Button>
        )}
      </div>
    </section>
  );
}
