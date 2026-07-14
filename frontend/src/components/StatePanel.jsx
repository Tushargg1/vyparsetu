import Icon from './Icon';

export function LoadingState({ label = 'Loading…', compact = false }) {
  return (
    <div className={`ui-state-panel ${compact ? 'min-h-28' : 'min-h-52'}`} role="status" aria-live="polite">
      <span className="relative flex h-10 w-10 items-center justify-center rounded-full bg-primary-fixed text-primary">
        <Icon name="progress_activity" className="animate-spin text-[22px]" />
      </span>
      <p className="text-label-md text-on-surface-variant">{label}</p>
    </div>
  );
}

export function EmptyState({ icon = 'inbox', title, description, action, compact = false }) {
  return (
    <div className={`ui-state-panel ${compact ? 'min-h-36' : 'min-h-56'}`}>
      <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-surface-container text-primary">
        <Icon name={icon} className="text-[26px]" />
      </span>
      <div className="max-w-md text-center">
        <h3 className="text-body-lg font-semibold text-on-surface">{title}</h3>
        {description && <p className="mt-xs text-label-md text-on-surface-variant">{description}</p>}
      </div>
      {action}
    </div>
  );
}

export function ErrorState({ title = 'Something went wrong', description, action }) {
  return (
    <div className="ui-state-panel min-h-52 border-error-container bg-error-container/30" role="alert">
      <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-error-container text-error">
        <Icon name="error" className="text-[26px]" />
      </span>
      <div className="max-w-md text-center">
        <h3 className="text-body-lg font-semibold text-on-surface">{title}</h3>
        {description && <p className="mt-xs text-label-md text-on-surface-variant">{description}</p>}
      </div>
      {action}
    </div>
  );
}
