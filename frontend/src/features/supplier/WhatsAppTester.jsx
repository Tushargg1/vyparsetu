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
    <div className="bg-surface-container-lowest rounded-xl border border-surface-variant shadow-sm overflow-hidden">
      <div className="px-lg py-md border-b border-outline-variant flex items-center justify-between gap-sm">
        <div className="flex items-center gap-sm">
          <Icon name="sms" className="text-secondary" />
          <h3 className="text-headline-md text-on-background">Test the assistant</h3>
        </div>
        <button onClick={reset} className="text-label-md text-on-surface-variant hover:text-on-surface">Clear</button>
      </div>

      <div className="px-lg py-sm border-b border-surface-variant flex items-center gap-sm">
        <span className="text-label-sm text-on-surface-variant">Customer number</span>
        <input
          className="border border-outline-variant rounded-lg px-3 py-1 text-label-md outline-none focus:border-primary w-40"
          inputMode="numeric"
          value={from}
          onChange={(e) => setFrom(e.target.value)}
        />
        <span className="text-label-sm text-on-surface-variant">Try a new number to see the new-customer flow.</span>
      </div>

      {/* Conversation */}
      <div className="p-md h-80 overflow-y-auto bg-surface-container-low space-y-sm">
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
              className={`max-w-[80%] px-md py-2 rounded-2xl text-label-md whitespace-pre-wrap ${
                m.role === 'customer'
                  ? 'bg-primary text-on-primary rounded-br-sm'
                  : 'bg-surface-container-highest text-on-surface rounded-bl-sm'
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
            className="px-3 py-1 rounded-full border border-outline-variant text-label-sm text-on-surface hover:bg-surface-container disabled:opacity-50">
            {q}
          </button>
        ))}
      </div>

      {/* Composer */}
      <div className="p-md flex gap-sm">
        <input
          className="flex-1 border border-outline-variant rounded-full px-4 py-2 outline-none focus:border-primary"
          placeholder="Type a customer message…"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={onKey}
        />
        <button
          onClick={send}
          disabled={busy || !text.trim()}
          className="bg-primary text-on-primary w-11 h-11 rounded-full flex items-center justify-center disabled:opacity-50"
        >
          <Icon name="send" className="text-[20px]" />
        </button>
      </div>
    </div>
  );
}
