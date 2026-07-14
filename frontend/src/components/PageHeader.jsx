import Icon from './Icon';

/**
 * Consistent page header used across retailer screens.
 * Optional `icon`, `subtitle`, and a right-aligned `action` slot.
 */
export default function PageHeader({ icon, title, subtitle, action }) {
  return (
    <header className="flex flex-col gap-md border-b border-surface-variant pb-lg sm:flex-row sm:items-start sm:justify-between">
      <div className="flex min-w-0 items-start gap-md">
        {icon && (
          <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary-fixed text-primary shadow-sm">
            <Icon name={icon} className="text-[24px]" />
          </span>
        )}
        <div className="min-w-0">
          <h1 className="text-headline-md font-bold tracking-tight text-on-background sm:text-headline-lg">{title}</h1>
          {subtitle && <p className="mt-xs max-w-2xl text-label-md text-on-surface-variant sm:text-body-md">{subtitle}</p>}
        </div>
      </div>
      {action && <div className="flex shrink-0 items-center">{action}</div>}
    </header>
  );
}
