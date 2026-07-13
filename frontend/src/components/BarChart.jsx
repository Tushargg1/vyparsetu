export default function BarChart({ data = [], valueKey = 'value', height = 160, onSelect, selectedIndex = null }) {
  const max = Math.max(1, ...data.map((d) => Number(d[valueKey]) || 0));
  const clickable = typeof onSelect === 'function';
  return (
    <div className="flex items-end gap-2" style={{ height }}>
      {data.map((d, i) => {
        const v = Number(d[valueKey]) || 0;
        const h = Math.round((v / max) * (height - 28));
        const active = selectedIndex === i;
        const dim = selectedIndex != null && !active;
        return (
          <button
            key={i}
            type="button"
            onClick={clickable ? () => onSelect(active ? null : i) : undefined}
            className={`flex-1 flex flex-col items-center justify-end gap-1 min-w-0 ${clickable ? 'cursor-pointer group' : 'cursor-default'}`}
            title={`${d.label}: ${v}`}
          >
            <span className={`text-[10px] whitespace-nowrap ${active ? 'text-primary font-bold' : 'text-on-surface-variant'}`}>
              {v >= 1000 ? `${(v / 1000).toFixed(1)}k` : Math.round(v)}
            </span>
            <div
              className={`w-full rounded-t-md transition-all ${
                active ? 'bg-primary' : dim ? 'bg-secondary/40' : 'bg-secondary'
              } ${clickable ? 'group-hover:opacity-80' : ''}`}
              style={{ height: Math.max(2, h) }}
            />
            <span className={`text-[10px] truncate w-full text-center ${active ? 'text-primary font-semibold' : 'text-on-surface-variant'}`}>
              {d.label}
            </span>
          </button>
        );
      })}
      {data.length === 0 && <p className="text-on-surface-variant text-label-md">No data yet.</p>}
    </div>
  );
}
