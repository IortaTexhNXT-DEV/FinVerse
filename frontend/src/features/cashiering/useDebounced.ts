import { useEffect, useState } from 'react';

/** The value once it has stopped changing for `delay` ms (live previews while typing). */
export function useDebounced<T>(value: T, delay = 400): T {
  const [settled, setSettled] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setSettled(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return settled;
}
