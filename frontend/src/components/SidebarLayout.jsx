import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { authApi } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import Icon from './Icon';

export default function SidebarLayout({ subtitle, nav }) {
  const navigate = useNavigate();
  const { user, refreshToken, clearAuth } = useAuthStore();
  const [open, setOpen] = useState(false);

  const logout = async () => {
    try {
      if (refreshToken) await authApi.logout(refreshToken);
    } catch {
      // Always clear the local session if the token is stale or the API is unavailable.
    } finally {
      clearAuth();
      navigate('/login', { replace: true });
    }
  };

  const SidebarInner = (
    <div className="flex h-full min-h-0 flex-col">
      <div className="mb-lg flex items-center gap-sm px-sm">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary text-on-primary shadow-sm">
          <Icon name="storefront" className="text-[22px]" />
        </span>
        <div className="min-w-0">
          <div className="text-body-lg font-bold tracking-tight text-primary">VyaparMantra</div>
          <p className="truncate text-label-sm text-on-surface-variant">{subtitle}</p>
        </div>
      </div>
      <nav aria-label="Primary navigation" className="min-h-0 flex-1">
        <ul className="h-full space-y-1 overflow-y-auto pr-xs">
          {nav.map((n) => (
            <li key={n.to}>
              <NavLink
                to={n.to}
                end={n.end}
                onClick={() => setOpen(false)}
                className={({ isActive }) =>
                  `group flex min-h-11 items-center gap-sm rounded-xl px-md py-2.5 text-label-md transition-all ${
                    isActive
                      ? 'bg-primary text-on-primary font-semibold shadow-sm'
                      : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    <Icon name={n.icon} filled={isActive} className="text-[21px] transition-transform group-hover:scale-105" />
                    <span>{n.label}</span>
                  </>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div className="mt-md border-t border-surface-variant pt-md">
        <div className="mb-sm flex items-center gap-sm rounded-xl bg-surface-container-low p-sm">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-fixed font-bold text-primary">
            {(user?.name || 'U')[0]}
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-label-md font-semibold text-on-surface">{user?.name || 'Your account'}</p>
            <p className="truncate text-label-sm text-on-surface-variant">{user?.phone}</p>
          </div>
        </div>
        <button onClick={logout} className="flex min-h-11 w-full items-center gap-sm rounded-xl px-md py-2.5 text-label-md font-semibold text-error hover:bg-error-container">
          <Icon name="logout" className="text-[20px]" /> <span>Log out</span>
        </button>
      </div>
    </div>
  );

  return (
    <div className="flex min-h-screen bg-background">
      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-[272px] flex-col border-r border-surface-variant bg-surface-container-lowest px-md py-lg md:flex">
        {SidebarInner}
      </aside>

      {/* Mobile top bar */}
      <header className="fixed inset-x-0 top-0 z-40 flex h-16 items-center justify-between border-b border-surface-variant bg-surface-container-lowest/95 px-margin-mobile shadow-sm backdrop-blur-md md:hidden">
        <button onClick={() => setOpen(true)} className="ui-icon-button" aria-label="Open navigation menu">
          <Icon name="menu" className="text-[26px]" />
        </button>
        <span className="flex items-center gap-sm text-body-lg font-bold tracking-tight text-primary">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-on-primary">
            <Icon name="storefront" className="text-[18px]" />
          </span>
          VyaparMantra
        </span>
        <button onClick={logout} className="ui-icon-button text-error hover:bg-error-container hover:text-error" aria-label="Log out">
          <Icon name="logout" />
        </button>
      </header>

      {/* Mobile drawer */}
      {open && (
        <div className="fixed inset-0 z-50 md:hidden" role="dialog" aria-modal="true" aria-label="Navigation menu">
          <button className="absolute inset-0 bg-inverse-surface/40 backdrop-blur-sm" onClick={() => setOpen(false)} aria-label="Close navigation menu" />
          <aside className="absolute bottom-0 left-0 top-0 w-[min(320px,88vw)] border-r border-surface-variant bg-surface-container-lowest p-lg shadow-xl">
            <button onClick={() => setOpen(false)} className="ui-icon-button absolute right-sm top-sm" aria-label="Close navigation menu">
              <Icon name="close" />
            </button>
            {SidebarInner}
          </aside>
        </div>
      )}

      <main className="w-full flex-1 px-margin-mobile pb-24 pt-24 md:ml-[272px] md:px-margin-desktop md:pb-margin-desktop md:pt-margin-desktop">
        <div className="mx-auto w-full max-w-[1480px]">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
