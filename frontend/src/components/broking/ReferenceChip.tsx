import { Copy } from 'lucide-react';
import { useToast } from '@/components/ui/toastContext';

/** A business reference (ARN, PRF, slip number) with a copy button. */
export function ReferenceChip({ value, label }: Readonly<{ value: string; label?: string }>) {
  const toast = useToast();
  const copy = () => {
    void navigator.clipboard
      .writeText(value)
      .then(() => toast.success(`${value} copied`))
      .catch(() => undefined);
  };
  return (
    <span className="ref-chip">
      {label && <span className="ref-chip-label">{label}</span>}
      <code>{value}</code>
      <button type="button" className="ref-chip-copy" onClick={copy} aria-label={`Copy ${value}`}>
        <Copy size={12} aria-hidden="true" />
      </button>
    </span>
  );
}
