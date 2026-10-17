import type { ButtonHTMLAttributes, ReactNode } from 'react';

type Variant = 'primary' | 'accent' | 'secondary' | 'ghost' | 'danger';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: 'md' | 'sm';
  icon?: ReactNode;
  busy?: boolean;
}

/** Brand button. `accent` (gold) is reserved for the primary call to action of a screen. */
export function Button({
  variant = 'primary',
  size = 'md',
  icon,
  busy = false,
  children,
  className,
  type = 'button',
  disabled,
  ...rest
}: Readonly<ButtonProps>) {
  const classes = ['btn', `btn-${variant}`, size === 'sm' ? 'btn-sm' : '', className ?? '']
    .filter(Boolean)
    .join(' ');
  return (
    <button type={type} className={classes} disabled={disabled === true || busy} {...rest}>
      {busy ? <span className="spinner" aria-hidden="true" /> : icon}
      {children}
    </button>
  );
}
