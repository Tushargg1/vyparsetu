import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { whatsappApi } from '../../lib/api';
import Icon from '../../components/Icon';
import WhatsAppTester from './WhatsAppTester';

function Toggle({ checked, onChange, disabled }) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={() => onChange(!checked)}
      className={`relative w-11 h-6 rounded-full transition-colors shrink-0 ${
        checked ? 'bg-primary' : 'bg-surface-variant'
      } ${disabled ? 'opacity-40' : ''}`}
    >
      <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-surface-container-lowest transition-transform ${checked ? 'translate-x-5' : ''}`} />
    </button>
  );
}

function Row({ title, sub, children }) {
  return (
    <div className="flex items-center justify-between gap-md py-3">
      <div className="min-w-0">
        <div className="text-on-surface font-medium">{title}</div>
        {sub && <div className="text-label-sm text-on-surface-variant">{sub}</div>}
      </div>
      {children}
    </div>
  );
}

function Segmented({ value, options, onChange }) {
  return (
    <div className="flex bg-surface-container rounded-lg overflow-hidden text-label-md shrink-0">
      {options.map(([k, l]) => (
        <button
          key={k}
          onClick={() => onChange(k)}
          className={`px-3 py-1.5 ${value === k ? 'bg-primary text-on-primary font-semibold' : 'text-on-surface-variant'}`}
        >
          {l}
        </button>
      ))}
    </div>
  );
}

export default function WhatsAppAiPage() {
  const qc = useQueryClient();
  const { data: settings, isLoading } = useQuery({ queryKey: ['wa-settings'], queryFn: whatsappApi.settings });
  const { data: requests = [] } = useQuery({ queryKey: ['wa-requests'], queryFn: () => whatsappApi.requests('PENDING') });

  const [number, setNumber] = useState('');
  const [busy, setBusy] = useState(false);

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['wa-settings'] });
    qc.invalidateQueries({ queryKey: ['wa-requests'] });
  };

  const patch = async (body) => {
    setBusy(true);
    try {
      await whatsappApi.updateSettings(body);
      refresh();
    } finally {
      setBusy(false);
    }
  };

  const connect = async () => {
    if (!/^\d{10,15}$/.test(number)) return;
    setBusy(true);
    try {
      await whatsappApi.connect(number);
      setNumber('');
      refresh();
    } finally {
      setBusy(false);
    }
  };

  const disconnect = async () => {
    setBusy(true);
    try {
      await whatsappApi.disconnect();
      refresh();
    } finally {
      setBusy(false);
    }
  };

  const takeover = async (enabled) => {
    setBusy(true);
    try {
      await whatsappApi.takeover(enabled);
      refresh();
    } finally {
      setBusy(false);
    }
  };

  const approve = async (id) => { await whatsappApi.approveRequest(id); refresh(); };
  const reject = async (id) => { await whatsappApi.rejectRequest(id); refresh(); };

  if (isLoading) return <p className="text-on-surface-variant">Loading…</p>;

  const connected = settings?.connected;

  return (
    <div className="space-y-lg">
      <div className="flex items-center gap-sm">
        <Icon name="forum" className="text-secondary text-[28px]" />
        <div>
          <h2 className="text-headline-lg font-bold text-on-background">WhatsApp AI</h2>
          <p className="text-body-md text-on-surface-variant">Let customers order from chat, handled by your AI assistant.</p>
        </div>
      </div>

      {/* Connection status */}
      <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-lg shadow-sm">
        {!connected ? (
          <div className="space-y-md">
            <div className="flex items-center gap-sm text-on-surface-variant">
              <span className="w-2.5 h-2.5 rounded-full bg-on-surface-variant" />
              <span className="font-medium">Not connected</span>
            </div>
            <p className="text-label-md text-on-surface-variant">Connect your WhatsApp Business number to start receiving orders.</p>
            <div className="flex gap-sm max-w-md">
              <input
                className="flex-1 border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary"
                placeholder="Business number (digits)"
                inputMode="numeric"
                value={number}
                onChange={(e) => setNumber(e.target.value)}
              />
              <button onClick={connect} disabled={busy} className="bg-primary text-on-primary px-lg rounded-lg text-label-md font-semibold disabled:opacity-50">
                Connect WhatsApp
              </button>
            </div>
          </div>
        ) : (
          <div className="space-y-sm">
            <div className="flex items-center justify-between gap-sm">
              <div className="flex items-center gap-sm text-tertiary-container">
                <Icon name="check_circle" className="text-[20px]" />
                <span className="font-semibold text-on-surface">Connected</span>
              </div>
              <button onClick={disconnect} disabled={busy} className="text-error text-label-md hover:underline disabled:opacity-50">Disconnect</button>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-sm pt-sm">
              <Info label="Business Number" value={`+91 ${settings.businessNumber}`} />
              <Info label="AI Status" value={settings.aiEnabled ? 'Enabled' : 'Disabled'} ok={settings.aiEnabled} />
              <Info label="Auto Order" value={settings.autoCreateOrders ? 'ON' : 'OFF'} ok={settings.autoCreateOrders} />
              <Info label="Seller Approval" value={approvalLabel(settings.sellerApprovalMode)} />
              <Info label="Customer Confirm" value={settings.requireConfirmation ? 'ON' : 'OFF'} ok={settings.requireConfirmation} />
              <Info label="Human Takeover" value={settings.humanTakeover ? 'Active' : 'Off'} ok={!settings.humanTakeover} />
            </div>
          </div>
        )}
      </div>

      {connected && (
        <>
          {/* Human takeover */}
          <div className={`rounded-xl p-lg shadow-sm border ${settings.humanTakeover ? 'bg-error-container border-error/30' : 'bg-surface-container-lowest border-surface-variant'}`}>
            <div className="flex items-center justify-between gap-md">
              <div>
                <div className="font-semibold text-on-surface flex items-center gap-sm">
                  <Icon name="support_agent" className={settings.humanTakeover ? 'text-error' : 'text-secondary'} />
                  Human Takeover
                </div>
                <p className="text-label-sm text-on-surface-variant mt-xs">
                  {settings.humanTakeover ? 'AI is paused — you are handling chats manually.' : 'AI is replying automatically. Take over to chat yourself.'}
                </p>
              </div>
              {settings.humanTakeover ? (
                <button onClick={() => takeover(false)} disabled={busy} className="bg-primary text-on-primary px-lg py-2 rounded-lg text-label-md font-semibold disabled:opacity-50">Resume AI</button>
              ) : (
                <button onClick={() => takeover(true)} disabled={busy} className="border border-outline-variant text-on-surface px-lg py-2 rounded-lg text-label-md font-semibold disabled:opacity-50">Take Over</button>
              )}
            </div>
          </div>

          {/* Live tester */}
          <WhatsAppTester />

          {/* Pending retailer requests */}
          <div className="bg-surface-container-lowest rounded-xl border border-surface-variant shadow-sm overflow-hidden">
            <div className="px-lg py-md border-b border-outline-variant flex items-center justify-between">
              <h3 className="text-headline-md text-on-background">New retailer requests</h3>
              {requests.length > 0 && <span className="text-label-sm bg-error-container text-error px-2 py-0.5 rounded-md">{requests.length} pending</span>}
            </div>
            <div className="divide-y divide-surface-variant">
              {requests.length === 0 && <p className="p-lg text-on-surface-variant">No pending requests.</p>}
              {requests.map((r) => (
                <div key={r.id} className="flex flex-wrap items-center justify-between gap-sm px-lg py-md">
                  <div>
                    <div className="font-semibold text-on-surface">{r.shopName || 'New WhatsApp customer'}</div>
                    <div className="text-label-sm text-on-surface-variant">
                      {r.ownerName ? `${r.ownerName} · ` : ''}+91 {r.phone}{r.address ? ` · ${r.address}` : ''}
                    </div>
                  </div>
                  <div className="flex gap-sm">
                    <button onClick={() => reject(r.id)} className="px-md py-1.5 rounded-md border border-outline-variant text-error text-label-md font-semibold hover:bg-error-container">Reject</button>
                    <button onClick={() => approve(r.id)} className="px-md py-1.5 rounded-md bg-tertiary-container text-on-tertiary-container text-label-md font-semibold hover:opacity-90">Approve</button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* AI Settings */}
          <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-lg shadow-sm">
            <h3 className="text-headline-md text-on-background mb-sm">AI Settings</h3>
            <div className="divide-y divide-surface-variant">
              <Row title="Language" sub="Reply language for customers">
                <Segmented
                  value={settings.language}
                  onChange={(v) => patch({ language: v })}
                  options={[['HINDI', 'Hindi'], ['ENGLISH', 'English'], ['BOTH', 'Both']]}
                />
              </Row>

              <Row title="Business hours" sub="When the AI actively replies">
                <div className="flex items-center gap-1 text-label-md">
                  <input
                    type="time"
                    value={settings.businessHoursStart}
                    onChange={(e) => patch({ businessHoursStart: e.target.value })}
                    className="border border-outline-variant rounded-md px-2 py-1 outline-none focus:border-primary"
                  />
                  <span className="text-on-surface-variant">–</span>
                  <input
                    type="time"
                    value={settings.businessHoursEnd}
                    onChange={(e) => patch({ businessHoursEnd: e.target.value })}
                    className="border border-outline-variant rounded-md px-2 py-1 outline-none focus:border-primary"
                  />
                </div>
              </Row>

              <Row title="AI auto-reply" sub="Assistant responds to messages automatically">
                <Toggle checked={settings.aiEnabled && settings.autoReply} disabled={busy} onChange={(v) => patch({ aiEnabled: v, autoReply: v })} />
              </Row>

              <Row title="Require customer confirmation" sub='Customer must reply "YES" before an order is placed'>
                <Toggle checked={settings.requireConfirmation} disabled={busy} onChange={(v) => patch({ requireConfirmation: v })} />
              </Row>

              <Row title="Create orders automatically" sub="Place orders without manual review">
                <Toggle checked={settings.autoCreateOrders} disabled={busy} onChange={(v) => patch({ autoCreateOrders: v })} />
              </Row>

              <Row title="Seller approval" sub="When you must approve a new retailer">
                <Segmented
                  value={settings.sellerApprovalMode}
                  onChange={(v) => patch({ sellerApprovalMode: v })}
                  options={[['FIRST_ORDER_ONLY', 'First order'], ['ALWAYS', 'Always'], ['NEVER', 'Never']]}
                />
              </Row>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function Info({ label, value, ok }) {
  return (
    <div>
      <div className="text-label-sm text-on-surface-variant">{label}</div>
      <div className={`font-semibold ${ok === false ? 'text-error' : ok === true ? 'text-tertiary-container' : 'text-on-surface'}`}>{value}</div>
    </div>
  );
}

function approvalLabel(mode) {
  return { FIRST_ORDER_ONLY: 'First order', ALWAYS: 'Always', NEVER: 'Never' }[mode] || mode;
}
