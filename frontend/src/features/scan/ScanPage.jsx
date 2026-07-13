import { useEffect, useRef, useState } from 'react';
import { salesApi } from '../../lib/api';
import Icon from '../../components/Icon';

export default function ScanPage() {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const lastScanRef = useRef({ code: '', at: 0 });
  const [scanning, setScanning] = useState(false);
  const [manual, setManual] = useState('');
  const [lines, setLines] = useState([]); // {productId, productName, price, quantity, inStock}
  const [err, setErr] = useState('');
  const [done, setDone] = useState(null);
  const supported = typeof window !== 'undefined' && 'BarcodeDetector' in window;

  useEffect(() => () => stop(), []);

  const stop = () => {
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
    setScanning(false);
  };

  const addByBarcode = async (code) => {
    setErr('');
    try {
      const p = await salesApi.lookup(code);
      setLines((prev) => {
        const i = prev.findIndex((l) => l.productId === p.productId);
        if (i >= 0) {
          const copy = [...prev];
          copy[i] = { ...copy[i], quantity: copy[i].quantity + 1 };
          return copy;
        }
        return [...prev, { productId: p.productId, productName: p.productName, price: Number(p.price), quantity: 1, inStock: Number(p.inStock) }];
      });
    } catch (e) {
      setErr(e.response?.data?.error?.message || `No product found for ${code}`);
    }
  };

  const start = async () => {
    setErr('');
    if (!supported) {
      setErr('Camera scanning is not supported here. Please type the barcode below.');
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setScanning(true);
      // eslint-disable-next-line no-undef
      const detector = new window.BarcodeDetector();
      const tick = async () => {
        if (!streamRef.current) return;
        try {
          const codes = await detector.detect(videoRef.current);
          if (codes.length > 0) {
            const code = codes[0].rawValue;
            const now = Date.now();
            if (code !== lastScanRef.current.code || now - lastScanRef.current.at > 1500) {
              lastScanRef.current = { code, at: now };
              await addByBarcode(code);
            }
          }
        } catch { /* ignore */ }
        requestAnimationFrame(tick);
      };
      requestAnimationFrame(tick);
    } catch {
      setErr('Could not access the camera. Please type the barcode below.');
    }
  };

  const setQty = (productId, qty) => {
    setLines((prev) => prev.flatMap((l) => {
      if (l.productId !== productId) return [l];
      return qty <= 0 ? [] : [{ ...l, quantity: qty }];
    }));
  };

  const total = lines.reduce((s, l) => s + l.price * l.quantity, 0);
  const totalQty = lines.reduce((s, l) => s + l.quantity, 0);

  const placeOrder = async () => {
    setErr('');
    try {
      const res = await salesApi.record(lines.map((l) => ({ productId: l.productId, quantity: l.quantity })));
      stop();
      setDone(res);
      setLines([]);
    } catch (e) {
      setErr(e.response?.data?.error?.message || 'Could not save sale. Check stock.');
    }
  };

  if (done) {
    const dt = done.createdAt ? new Date(done.createdAt) : new Date();
    return (
      <div className="max-w-md mx-auto text-center pt-2xl space-y-md">
        <div className="w-24 h-24 rounded-full bg-tertiary-fixed-dim mx-auto flex items-center justify-center">
          <Icon name="receipt_long" className="text-[52px] text-on-tertiary-fixed" filled />
        </div>
        <h2 className="text-headline-lg">Sale saved!</h2>
        <div className="bg-surface-container-lowest border border-surface-variant rounded-2xl p-lg text-left">
          <p className="text-label-sm text-on-surface-variant">Bill #{done.id} · {dt.toLocaleString()}</p>
          <ul className="mt-sm divide-y divide-surface-variant">
            {done.items.map((it) => (
              <li key={it.productId} className="py-2 flex justify-between text-on-surface">
                <span>{it.productName} × {Number(it.quantity)}</span>
                <span>₹{it.lineTotal}</span>
              </li>
            ))}
          </ul>
          <div className="mt-sm pt-sm border-t border-outline-variant flex justify-between text-headline-md font-bold">
            <span>Total</span><span className="text-primary">₹{done.totalAmount}</span>
          </div>
        </div>
        <button onClick={() => setDone(null)} className="w-full bg-primary text-on-primary py-4 rounded-2xl text-headline-md font-bold">New sale</button>
      </div>
    );
  }

  return (
    <div className="space-y-lg max-w-2xl">
      <div>
        <h2 className="text-headline-lg font-bold text-on-background">Scan & Sell</h2>
        <p className="text-body-md text-on-surface-variant mt-xs">Scan each item you're selling, adjust quantity, then place the order.</p>
      </div>

      {err && <div className="p-md rounded-xl bg-error-container text-on-error-container text-label-md">{err}</div>}

      <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant p-md">
        <div className="aspect-video bg-black rounded-xl overflow-hidden flex items-center justify-center">
          <video ref={videoRef} className={scanning ? 'w-full h-full object-cover' : 'hidden'} muted playsInline />
          {!scanning && <Icon name="barcode_scanner" className="text-[56px] text-outline-variant" />}
        </div>
        <div className="flex gap-sm mt-md">
          {!scanning ? (
            <button onClick={start} className="flex-1 bg-primary text-on-primary py-3 rounded-xl font-bold flex items-center justify-center gap-1">
              <Icon name="photo_camera" /> Start scanning
            </button>
          ) : (
            <button onClick={stop} className="flex-1 border-2 border-outline-variant py-3 rounded-xl font-semibold">Stop camera</button>
          )}
        </div>
        <div className="flex gap-sm mt-sm">
          <input className="flex-1 border border-outline-variant rounded-xl py-2.5 px-3 outline-none focus:border-primary" placeholder="Or type a barcode" inputMode="numeric" value={manual} onChange={(e) => setManual(e.target.value)} />
          <button onClick={() => { if (manual) { addByBarcode(manual); setManual(''); } }} className="bg-secondary text-on-secondary px-md rounded-xl font-semibold">Add</button>
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant">
        <div className="px-lg py-md border-b border-outline-variant flex justify-between items-center">
          <h3 className="text-headline-md">Items ({lines.length})</h3>
          <span className="text-on-surface-variant text-label-md">{totalQty} pcs</span>
        </div>
        {lines.length === 0 ? (
          <p className="p-lg text-on-surface-variant text-center">Scan or add items to start a bill.</p>
        ) : (
          <div className="divide-y divide-surface-variant">
            {lines.map((l) => (
              <div key={l.productId} className="px-lg py-md flex items-center gap-md">
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-on-surface line-clamp-1">{l.productName}</p>
                  <p className="text-label-sm text-on-surface-variant">₹{l.price} · {l.inStock} in stock</p>
                </div>
                <div className="flex items-center bg-surface-container rounded-xl overflow-hidden h-10">
                  <button onClick={() => setQty(l.productId, l.quantity - 1)} className="w-10 text-primary text-xl">−</button>
                  <span className="w-8 text-center font-bold">{l.quantity}</span>
                  <button onClick={() => setQty(l.productId, l.quantity + 1)} className="w-10 text-primary text-xl">+</button>
                </div>
                <span className="w-16 text-right font-bold text-on-surface">₹{(l.price * l.quantity).toFixed(0)}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {lines.length > 0 && (
        <div className="sticky bottom-4">
          <div className="bg-primary text-on-primary rounded-2xl shadow-lg p-md flex items-center justify-between">
            <div>
              <p className="text-label-sm text-primary-fixed-dim">Total bill</p>
              <p className="text-headline-md font-bold">₹{total.toFixed(2)}</p>
            </div>
            <button onClick={placeOrder} className="bg-on-primary text-primary px-lg py-3 rounded-xl font-bold flex items-center gap-1">
              Place Order <Icon name="check" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
