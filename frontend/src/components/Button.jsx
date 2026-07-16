import Icon from './Icon';

/**
 * Shared action button. Supports `primary`, `secondary`, `ghost`, and `danger`
 * variants; `sm`, `md`, and `lg` sizes; icons; disabled and loading states.
 */
const VARIANTS = {
  primary: 'bg-primary text-on-primary hover:bg-primary-container',
  secondary: 'border border-secondary text-secondary hover:bg-secondary-fixed',
  ghost: 'text-secondary hover:bg-secondary-fixed/40',
  danger: 'bg-error text-on-error hover:bg-on-error-container',
};

const SIZES = {
  sm: 'min-h-xl px-sm text-label-sm',
  md: 'min-h-2xl px-md text-label-md',
  lg: 'min-h-2xl px-lg text-body-md',
};

export default function Button({
  variant = 'primary', size = 'md', leadingIcon, trailingIcon, loading = false,
  disabled = false, className = '', children, type = 'button', ...props
}) {
  const unavailable = disabled || loading;
  return (
    <button
      type={type}
      disabled={unavailable}
      aria-busy={loading || undefined}
      className={`inline-flex items-center justify-center gap-sm rounded-xl font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-secondary focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 ${VARIANTS[variant]} ${SIZES[size]} ${className}`}
      {...props}
    >
      {loading ? <Icon name="progress_activity" className="animate-spin" /> : leadingIcon && <Icon name={leadingIcon} />}
      <span>{children}</span>
      {!loading && trailingIcon && <Icon name={trailingIcon} />}
    </button>
  );
}
