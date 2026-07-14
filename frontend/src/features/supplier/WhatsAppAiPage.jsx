import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { whatsappApi } from '../../lib/api';
import Icon from '../../components/Icon';
import PageHeader from '../../components/PageHeader';
import { LoadingState, EmptyState } from '../../components/StatePanel';
import WhatsAppTester from './WhatsAppTester';

function Toggle({ checked, onChange, disabled, label }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      onClick={() => onChange(!checked)}
      className={`relative h-7 w-12 shrink-0 rounded-full border transition-colors ${
        checked ? 'border-primary bg-primary' : 'border-outline-variant bg-surface-container-high'
      } ${disabled ? 'opacity-40' : ''}`}
    >
      <span className={`absolute left-0.5 top-0.5 h-5.5 w-5.5 rounded-full bg-surface-container-lowest shadow-sm transition-transform ${checked ? 'translate-x-5' : ''}`} />
    </button>
  );
}

function Row({ title, sub, children }) {
  return (
    <div className="flex flex-col gap-sm py-md sm:flex-row sm:items-center sm:justify-between sm:gap-md">
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
          aria-pressed={value === k}
          className={`min-h-10 px-3 py-1.5 ${value === k ? 'bg-primary text-on-primary font-semibold shadow-sm' : 'text-on-surface-variant hover:bg-surface-container-high'}`}
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

  if (isLoading) return <LoadingState label="Loading WhatsApp AI workspace…" />;

  const connected = settings?.connected;

  return (
    <div className="space-y-xl">
      <PageHeader
        icon="forum"
        title="WhatsApp AI"
        subtitle="Manage customer conversations, AI ordering, approvals, and assistant settings in one place."
        action={
          <span className={`inline-flex items-center gap-sm rounded-full px-3 py-1.5 text-label-md font-semibold ${connected ? 'bg-tertiary-fixed-dim text-on-tertiary-container' : 'bg-surface-container text-on-surface-variant'}`}>
            <span className={`h-2 w-2 rounded-full ${connected ? 'bg-on-tertiary-container' : 'bg-outline'}`} />
            {connected ? 'Assistant online' : 'Not connected'}
          </span>
        }
      />

      {/* Connection status */}
      <section className="ui-card overflow-hidden p-lg sm:p-xl">
        {!connected ? (
          <div className="space-y-md">
            <div className="flex items-center gap-sm text-on-surface-variant">
              <span className="w-2.5 h-2.5 rounded-full bg-on-surface-variant" />
              <span className="font-medium">Not connected</span>
            </div>
            <p className="text-label-md text-on-surface-variant">Connect your WhatsApp Business number to start receiving orders.</p>
            <div className="flex flex-col gap-sm sm:max-w-xl sm:flex-row">
              <input
                className="ui-input flex-1"
                placeholder="Business number (digits)"
                inputMode="numeric"
                value={number}
                onChange={(e) => setNumber(e.target.value)}
              />
              <button onClick={connect} disabled={busy} className="ui-button-primary whitespace-nowrap">
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
            <div className="grid grid-cols-2 gap-sm pt-md md:grid-cols-3">
              <Info label="Business Number" value={`+91 ${settings.businessNumber}`} />
              <Info label="AI Status" value={settings.aiEnabled ? 'Enabled' : 'Disabled'} ok={settings.aiEnabled} />
              <Info label="Auto Order" value={settings.autoCreateOrders ? 'ON' : 'OFF'} ok={settings.autoCreateOrders} />
              <Info label="Seller Approval" value={approvalLabel(settings.sellerApprovalMode)} />
              <Info label="Customer Confirm" value={settings.requireConfirmation ? 'ON' : 'OFF'} ok={settings.requireConfirmation} />
              <Info label="Human Takeover" value={settings.humanTakeover ? 'Active' : 'Off'} ok={!settings.humanTakeover} />
            </div>
          </div>
        )}
      </section>

      {connected && (
        <>
          {/* Human takeover */}
          <section className={`rounded-2xl border p-lg shadow-sm ${settings.humanTakeover ? 'border-error/30 bg-error-container/60' : 'border-surface-variant bg-surface-container-lowest'}`}>
            <div className="flex flex-col gap-md sm:flex-row sm:items-center sm:justify-between">
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
                <button onClick={() => takeover(false)} disabled={busy} className="ui-button-primary">Resume AI</button>
              ) : (
                <button onClick={() => takeover(true)} disabled={busy} className="ui-button-secondary">Take over</button>
              )}
            </div>
          </section>

          {/* Live tester */}
          <WhatsAppTester />

          {/* Pending retailer requests */}
          <section className="ui-card overflow-hidden">
            <div className="flex items-center justify-between border-b border-surface-variant bg-surface-container-low/60 px-lg py-md">
              <h3 className="text-headline-md text-on-background">New retailer requests</h3>
              {requests.length > 0 && <span className="text-label-sm bg-error-container text-error px-2 py-0.5 rounded-md">{requests.length} pending</span>}
            </div>
            <div className="divide-y divide-surface-variant">
              {requests.length === 0 && <EmptyState compact icon="group_add" title="No pending requests" description="New retailer approvals will appear here." />}
              {requests.map((r) => (
                <div key={r.id} className="flex flex-wrap items-center justify-between gap-sm px-lg py-md">
                  <div>
                    <div className="font-semibold text-on-surface">{r.shopName || 'New WhatsApp customer'}</div>
                    <div className="text-label-sm text-on-surface-variant">
                      {r.ownerName ? `${r.ownerName} · ` : ''}+91 {r.phone}{r.address ? ` · ${r.address}` : ''}
                    </div>
                  </div>
                  <div className="flex gap-sm">
                    <button onClick={() => reject(r.id)} className="ui-button-secondary min-h-10 px-md py-1.5 text-error hover:border-error hover:bg-error-container">Reject</button>
                    <button onClick={() => approve(r.id)} className="ui-button-primary min-h-10 bg-tertiary-container px-md py-1.5 text-on-tertiary-container">Approve</button>
                  </div>
                </div>
              ))}
            </div>
          </section>

          {/* AI Settings */}
          <section className="ui-card p-lg sm:p-xl">
            <div className="mb-md flex items-center gap-sm">
              <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-secondary-fixed text-secondary"><Icon name="tune" className="text-[20px]" /></span>
              <div>
                <h3 className="text-headline-md text-on-background">AI settings</h3>
                <p className="text-label-sm text-on-surface-variant">Control how the assistant responds and creates orders.</p>
              </div>
            </div>
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
                <Toggle label="AI auto-reply" checked={settings.aiEnabled && settings.autoReply} disabled={busy} onChange={(v) => patch({ aiEnabled: v, autoReply: v })} />
              </Row>

              <Row title="Require customer confirmation" sub='Customer must reply "YES" before an order is placed'>
                <Toggle label="Require customer confirmation" checked={settings.requireConfirmation} disabled={busy} onChange={(v) => patch({ requireConfirmation: v })} />
              </Row>

              <Row title="Create orders automatically" sub="Place orders without manual review">
                <Toggle label="Create orders automatically" checked={settings.autoCreateOrders} disabled={busy} onChange={(v) => patch({ autoCreateOrders: v })} />
              </Row>

              <Row title="Seller approval" sub="When you must approve a new retailer">
                <Segmented
                  value={settings.sellerApprovalMode}
                  onChange={(v) => patch({ sellerApprovalMode: v })}
                  options={[['FIRST_ORDER_ONLY', 'First order'], ['ALWAYS', 'Always'], ['NEVER', 'Never']]}
                />
              </Row>
            </div>
          </section>
        </>
      )}
    </div>
  );
}

function Info({ label, value, ok }) {
  return (
    <div className="rounded-xl border border-surface-variant bg-surface-container-low/70 p-md">
      <div className="text-label-sm text-on-surface-variant">{label}</div>
      <div className={`mt-xs font-semibold ${ok === false ? 'text-error' : ok === true ? 'text-on-tertiary-container' : 'text-on-surface'}`}>{value}</div>
    </div>
  );
}

function approvalLabel(mode) {
  return { FIRST_ORDER_ONLY: 'First order', ALWAYS: 'Always', NEVER: 'Never' }[mode] || mode;
}
