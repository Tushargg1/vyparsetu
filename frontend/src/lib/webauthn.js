/** Convert WebAuthn's base64url JSON fields and serialize browser credentials. */
function fromBase64url(value) {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/');
  const bytes = Uint8Array.from(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')), (char) => char.charCodeAt(0));
  return bytes.buffer;
}

function toBase64url(value) {
  const bytes = new Uint8Array(value);
  let binary = '';
  bytes.forEach((byte) => { binary += String.fromCharCode(byte); });
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

function publicKeyOptions(options) {
  const parsed = typeof options === 'string' ? JSON.parse(options) : options;
  const nested = typeof parsed.publicKey === 'string' ? JSON.parse(parsed.publicKey) : parsed.publicKey;
  const source = nested || parsed;
  const publicKey = { ...source, challenge: fromBase64url(source.challenge) };
  if (publicKey.user?.id) publicKey.user = { ...publicKey.user, id: fromBase64url(publicKey.user.id) };
  if (publicKey.excludeCredentials) publicKey.excludeCredentials = publicKey.excludeCredentials.map((item) => ({ ...item, id: fromBase64url(item.id) }));
  if (publicKey.allowCredentials) publicKey.allowCredentials = publicKey.allowCredentials.map((item) => ({ ...item, id: fromBase64url(item.id) }));
  return publicKey;
}

function serializeCredential(credential) {
  const response = credential.response;
  const serialized = {
    id: credential.id, rawId: toBase64url(credential.rawId), type: credential.type,
    response: { clientDataJSON: toBase64url(response.clientDataJSON) },
    clientExtensionResults: credential.getClientExtensionResults(),
    authenticatorAttachment: credential.authenticatorAttachment || undefined,
  };
  ['attestationObject', 'authenticatorData', 'signature', 'userHandle'].forEach((field) => {
    if (response[field]) serialized.response[field] = toBase64url(response[field]);
  });
  if (response.getTransports) serialized.response.transports = response.getTransports();
  return serialized;
}

export function isWebAuthnSupported() {
  return typeof window !== 'undefined' && window.isSecureContext && Boolean(window.PublicKeyCredential && navigator.credentials);
}

async function runCeremony(method, options) {
  if (!isWebAuthnSupported()) throw new Error('Passkeys are not supported in this browser or connection.');
  const credential = await navigator.credentials[method]({ publicKey: publicKeyOptions(options) });
  if (!credential) throw new Error('The passkey request did not return a credential.');
  return serializeCredential(credential);
}

export const createPasskey = (options) => runCeremony('create', options);
export const getPasskey = (options) => runCeremony('get', options);
