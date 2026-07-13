import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import Icon from './Icon';

export default function SidebarLayout({ subtitle, nav }) {
  const navigate = useNavigate();
  const { user, clearAuth } = useAuthStore();
  const [open, setOpen] = useState(false);

  const logout = () => {
    clearAuth();
    navigate('/login');
  };

  const SidebarInner = (
    <div className="flex flex-col h-full">
      <div className="mb-xl px-sm">
        <h1 className="text-headline-md font-bold text-primary">VyaparMantra</h1>
        <p className="text-label-sm text-on-surface-variant mt-xs">{subtitle}</p>
      </div>
      <ul className="flex-1 space-y-1 overflow-y-auto">
        {nav.map((n) => (
          <li key={n.to}>
            <NavLink
              to={n.to}
              end={n.end}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-sm px-md py-2.5 rounded-lg text-label-md transition-all ${
                  isActive ? 'bg-primary text-on-primary font-semibold shadow-sm' : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <Icon name={n.icon} filled={isActive} />
                  <span>{n.label}</span>
                </>
              )}
            </NavLink>
          </li>
        ))}
      </ul>
      <div className="mt-auto pt-md border-t border-outline-variant">
        <button onClick={logout} className="flex items-center gap-sm px-md py-2.5 rounded-lg text-error hover:bg-error-container w-full text-label-md">
          <Icon name="logout" /> <span>Logout</span>
        </button>
        <div className="mt-sm px-md py-sm flex items-center gap-sm">
          <div className="w-9 h-9 rounded-full bg-primary text-on-primary flex items-center justify-center font-bold">
            {(user?.name || 'U')[0]}
          </div>
          <div className="min-w-0">
            <p className="text-label-md font-semibold text-on-surface truncate">{user?.name}</p>
            <p className="text-label-sm text-on-surface-variant truncate">{user?.phone}</p>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div className="flex min-h-screen bg-surface-container-low">
      {/* Desktop sidebar */}
      <aside className="hidden md:flex flex-col h-screen py-lg px-md w-[280px] fixed left-0 top-0 bg-surface-container-lowest/80 backdrop-blur-md border-r border-outline-variant z-40">
        {SidebarInner}
      </aside>

      {/* Mobile top bar */}
      <header className="md:hidden fixed top-0 inset-x-0 h-14 bg-surface-container-lowest border-b border-outline-variant flex items-center justify-between px-margin-mobile z-40">
        <button onClick={() => setOpen(true)} className="text-primary p-1"><Icon name="menu" className="text-[28px]" /></button>
        <span className="text-headline-md font-bold text-primary">VyaparMantra</span>
        <button onClick={logout} className="text-error p-1"><Icon name="logout" /></button>
      </header>

      {/* Mobile drawer */}
      {open && (
        <div className="md:hidden fixed inset-0 z-50">
          <div className="absolute inset-0 bg-black/40" onClick={() => setOpen(false)} />
          <div className="absolute left-0 top-0 h-full w-[280px] bg-surface-container-lowest p-lg shadow-xl">
            {SidebarInner}
          </div>
        </div>
      )}

      <main className="flex-1 md:ml-[280px] w-full p-margin-mobile md:p-margin-desktop pt-20 md:pt-margin-desktop">
        <Outlet />
      </main>
    </div>
  );
}
