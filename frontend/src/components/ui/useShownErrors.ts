import { useState } from 'react';
import { visibleErrors } from '@/utils/fieldErrors';

/**
 * Validation on touch or submit: a form computes its errors on every render, but a field shows its
 * error only once the user has changed it or has tried to submit the form.
 */
export function useShownErrors<E extends Partial<Record<string, string>>>(errors: E) {
  const [attempted, setAttempted] = useState(false);
  const [touched, setTouched] = useState<readonly string[]>([]);
  return {
    /** The errors to show now. */
    shown: visibleErrors(errors, touched, attempted),
    /** Marks a field as changed by the user. */
    touch: (field: string) => {
      setTouched((t) => (t.includes(field) ? t : [...t, field]));
    },
    /** Marks a submission attempt; true when the form has no error and can be sent. */
    attempt: (): boolean => {
      setAttempted(true);
      return Object.values(errors).every((e) => e === undefined);
    },
  };
}
