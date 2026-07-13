import Icon from './Icon';

/**
 * Consistent page header used across retailer screens.
 * Optional `icon`, `subtitle`, and a right-aligned `action` slot.
 */
export default function PageHeader({ icon, title, subtitle, action }) {
  return (
    <div className="flex items-start justify-between gap-md flex-wrap">
      <div className="flex items-start gap-md">
        {icon && (
          <span className="w-11 h-11 rounded-xl bg-primary-fixed text-primary flex items-center justify-center shrink-0">
            <Icon name={icon} className="text-[24px]" />
          </span>
        )}
        <div>
          <h2 className="text-headline-lg font-bold text-on-background">{title}</h2>
          {subtitle && <p className="text-body-md text-on-surface-variant mt-xs">{subtitle}</p>}
        </div>
      </div>
      {action}
    </div>
  );
}
