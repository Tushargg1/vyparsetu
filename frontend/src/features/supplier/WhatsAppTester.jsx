import { useState, useRef, useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { whatsappApi } from '../../lib/api';
import Icon from '../../components/Icon';

export default function WhatsAppTester() {
  const qc = useQueryClient();
  const [from, setFrom] = useState('9000000099');
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [messages, setMessages] = useState([]); // {role:'customer'|'ai', text}
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const send = async () => {
    const body = text.trim();
    if (!body || !/^\d{10,15}$/.test(from)) return;
    setMessages((m) => [...m, { role: 'customer', text: body }]);
    setText('');
    setBusy(true);
    try {
      const res = await whatsappApi.simulate(from, body);
      const reply = res?.reply || '(no reply)';
      setMessages((m) => [...m, { role: 'ai', text: reply }]);
      // the flow may have created onboarding requests or orders — refresh those views
      qc.invalidateQueries({ queryKey: ['wa-requests'] });
      qc.invalidateQueries({ queryKey: ['sup-orders-all'] });
      qc.invalidateQueries({ queryKey: ['sup-orders'] });
    } catch (e) {
      const msg = e.response?.data?.error?.message || 'Something went wrong';
      setMessages((m) => [...m, { role: 'ai', text: `⚠ ${msg}` }]);
    } finally {
      setBusy(false);
    }
  };

  const onKey = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  const sendQuick = async (q) => {
    if (busy) return;
    setText(q);
    // send immediately using the chip text
    const body = q.trim();
    if (!/^\d{10,15}$/.test(from)) return;
    setMessages((m) => [...m, { role: 'customer', text: body }]);
    setText('');
    setBusy(true);
    try {
      const res = await whatsappApi.simulate(from, body);
      setMessages((m) => [...m, { role: 'ai', text: res?.reply || '(no reply)' }]);
      qc.invalidateQueries({ queryKey: ['wa-requests'] });
      qc.invalidateQueries({ queryKey: ['sup-orders-all'] });
      qc.invalidateQueries({ queryKey: ['sup-orders'] });
    } catch (e) {
      setMessages((m) => [...m, { role: 'ai', text: `⚠ ${e.response?.data?.error?.message || 'Something went wrong'}` }]);
    } finally {
      setBusy(false);
    }
  };

  const QUICK = ['Hi', 'menu', '2 Maggi, 5 Parle-G', 'order status', 'pending bills', 'price Maggi'];

  const reset = () => setMessages([]);

  return (
    <section className="ui-card overflow-hidden">
      <div className="flex items-center justify-between gap-sm border-b border-surface-variant bg-surface-container-low/60 px-lg py-md">
        <div className="flex items-center gap-sm">
          <Icon name="sms" className="text-secondary" />
          <h3 className="text-headline-md text-on-background">Test the assistant</h3>
        </div>
          <button onClick={reset} className="ui-button-secondary min-h-9 px-3 py-1.5">Clear</button>
      </div>

      <div className="flex flex-col gap-sm border-b border-surface-variant px-lg py-md sm:flex-row sm:items-center">
        <label htmlFor="test-customer-number" className="text-label-sm font-semibold text-on-surface-variant">Customer number</label>
        <input
          id="test-customer-number"
          aria-label="Customer number"
          className="min-h-10 rounded-xl border border-outline-variant bg-surface-container-lowest px-3 py-1 text-label-md outline-none focus:border-primary sm:w-40"
          inputMode="numeric"
          value={from}
          onChange={(e) => setFrom(e.target.value)}
        />
        <span className="text-label-sm text-on-surface-variant sm:ml-auto">Use a new number to preview customer onboarding.</span>
      </div>

      {/* Conversation */}
      <div className="h-96 space-y-sm overflow-y-auto bg-surface-container-low/70 p-md sm:p-lg">
        {messages.length === 0 && (
          <div className="h-full flex flex-col items-center justify-center text-center text-on-surface-variant gap-1">
            <Icon name="forum" className="text-[32px]" />
            <p className="text-label-md">Send a message as a customer.</p>
            <p className="text-label-sm">Try: “2 cartons Maggi, 5 Parle-G” or “Hi”.</p>
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} className={`flex ${m.role === 'customer' ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`max-w-[88%] whitespace-pre-wrap px-md py-2.5 text-label-md shadow-sm sm:max-w-[72%] ${
                m.role === 'customer'
                  ? 'rounded-2xl rounded-br-sm bg-primary text-on-primary'
                  : 'rounded-2xl rounded-bl-sm border border-surface-variant bg-surface-container-lowest text-on-surface'
              }`}
            >
              {m.text}
            </div>
          </div>
        ))}
        <div ref={endRef} />
      </div>

      {/* Quick replies */}
      <div className="px-md pt-md flex gap-sm flex-wrap border-t border-surface-variant">
        {QUICK.map((q) => (
          <button key={q} onClick={() => sendQuick(q)} disabled={busy}
            className="rounded-full border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-label-sm text-on-surface shadow-sm hover:border-primary hover:bg-primary-fixed/40 disabled:opacity-50">
            {q}
          </button>
        ))}
      </div>

      {/* Composer */}
      <div className="flex gap-sm p-md sm:p-lg">
        <input
          aria-label="Customer message"
          className="ui-input flex-1 rounded-full"
          placeholder="Type a customer message…"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={onKey}
        />
        <button
          onClick={send}
          disabled={busy || !text.trim()}
          aria-label="Send message"
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary text-on-primary shadow-sm hover:-translate-y-px hover:shadow-md disabled:opacity-50"
        >
          <Icon name="send" className="text-[20px]" />
        </button>
      </div>
    </section>
  );
}
