import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { whatsappApi } from '../../lib/api';
import Icon from '../../components/Icon';

export default function LinkedNumbers({ retailerId }) {
  const qc = useQueryClient();
  const { data: numbers = [] } = useQuery({
    queryKey: ['linked-numbers', retailerId],
    queryFn: () => whatsappApi.linkedNumbers(retailerId),
  });

  const [phone, setPhone] = useState('');
  const [pendingPhone, setPendingPhone] = useState(null); // awaiting OTP
  const [code, setCode] = useState('');
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);

  const refresh = () => qc.invalidateQueries({ queryKey: ['linked-numbers', retailerId] });

  const sendOtp = async () => {
    if (!/^\d{10,15}$/.test(phone)) {
      setMsg('Enter a valid number');
      return;
    }
    setBusy(true);
    setMsg('');
    try {
      await whatsappApi.addNumber(retailerId, phone);
      setPendingPhone(phone);
      setPhone('');
      setMsg('OTP sent to the number. Enter it below to verify.');
      refresh();
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Could not add number');
    } finally {
      setBusy(false);
    }
  };

  const verify = async () => {
    setBusy(true);
    setMsg('');
    try {
      await whatsappApi.verifyNumber(retailerId, pendingPhone, code);
      setPendingPhone(null);
      setCode('');
      setMsg('Number verified. It can now be used to order.');
      refresh();
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Invalid OTP');
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id) => {
    await whatsappApi.removeNumber(retailerId, id);
    refresh();
  };

  return (
    <div className="bg-surface-container-lowest rounded-xl border border-surface-variant shadow-sm">
      <div className="px-lg py-md border-b border-outline-variant flex items-center gap-sm">
        <Icon name="smartphone" className="text-secondary" />
        <h3 className="text-headline-md text-on-background">Linked WhatsApp numbers</h3>
      </div>

      <div className="p-md space-y-sm">
        {numbers.length === 0 && <p className="text-on-surface-variant px-sm">No numbers linked yet.</p>}
        {numbers.map((n) => (
          <div key={n.id} className="flex items-center justify-between gap-sm p-md rounded-lg border border-surface-variant">
            <div className="flex items-center gap-sm min-w-0">
              <Icon name="call" className="text-on-surface-variant text-[18px]" />
              <span className="text-on-surface font-medium">+91 {n.phone}</span>
            </div>
            <div className="flex items-center gap-sm">
              {n.verified ? (
                <span className="text-label-sm text-on-tertiary-container bg-tertiary-fixed-dim px-2 py-0.5 rounded-md flex items-center gap-1">
                  <Icon name="verified" className="text-[14px]" /> Verified
                </span>
              ) : (
                <span className="text-label-sm text-error bg-error-container px-2 py-0.5 rounded-md">Unverified</span>
              )}
              <button onClick={() => remove(n.id)} className="text-on-surface-variant hover:text-error">
                <Icon name="delete" className="text-[18px]" />
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="px-md pb-md space-y-sm">
        {!pendingPhone ? (
          <div className="flex gap-sm">
            <input
              className="flex-1 border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary"
              placeholder="Add number (digits)"
              inputMode="numeric"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
            <button onClick={sendOtp} disabled={busy} className="bg-primary text-on-primary px-md rounded-lg text-label-md font-semibold disabled:opacity-50">
              Send OTP
            </button>
          </div>
        ) : (
          <div className="flex gap-sm items-center">
            <span className="text-label-sm text-on-surface-variant">+91 {pendingPhone}</span>
            <input
              className="flex-1 border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary"
              placeholder="Enter OTP"
              inputMode="numeric"
              value={code}
              onChange={(e) => setCode(e.target.value)}
            />
            <button onClick={verify} disabled={busy} className="bg-tertiary-container text-on-tertiary-container px-md rounded-lg text-label-md font-semibold disabled:opacity-50">
              Verify
            </button>
            <button onClick={() => { setPendingPhone(null); setCode(''); setMsg(''); }} className="text-on-surface-variant hover:text-on-surface">
              <Icon name="close" className="text-[18px]" />
            </button>
          </div>
        )}
        {msg && <p className="text-label-sm text-on-surface-variant px-1">{msg}</p>}
      </div>
    </div>
  );
}
