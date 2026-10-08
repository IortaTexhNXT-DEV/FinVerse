/**
 * An on / off filter of a work list toolbar ("Overdue only"), drawn as a control of the toolbar's
 * height with its checkbox inside, so it lines up with the search box and the buttons.
 */
export function FilterToggle({
  label,
  checked,
  onChange,
}: Readonly<{ label: string; checked: boolean; onChange: (checked: boolean) => void }>) {
  return (
    <label className={checked ? 'filter-toggle checked' : 'filter-toggle'}>
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      <span>{label}</span>
    </label>
  );
}
