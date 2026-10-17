import { useSearchParams } from 'react-router-dom';

/**
 * The active tab of a record page, kept in the URL (`?tab=documents`), so a notification or a link
 * opens the exact tab and Back returns to it. Unknown values fall back to the first tab.
 */
export function useTabParam<T extends string>(
  ids: readonly T[],
  fallback: T,
): [T, (tab: T) => void] {
  const [params, setParams] = useSearchParams();
  const raw = params.get('tab');
  const active = ids.find((id) => id === raw) ?? fallback;
  const setActive = (tab: T) =>
    setParams(
      (current) => {
        const next = new URLSearchParams(current);
        if (tab === fallback) {
          next.delete('tab');
        } else {
          next.set('tab', tab);
        }
        return next;
      },
      { replace: true },
    );
  return [active, setActive];
}
