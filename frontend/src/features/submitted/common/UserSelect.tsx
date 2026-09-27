import { useQuery } from '@tanstack/react-query';
import { submittedApi } from '@/api/submitted';

/** A choice of the users holding a permission of the module, by name. */
export function UserSelect({
  id,
  permission,
  value,
  onChange,
  placeholder = 'Select…',
}: Readonly<{
  id: string;
  permission: string;
  value: string;
  onChange: (username: string) => void;
  placeholder?: string;
}>) {
  const users = useQuery({
    queryKey: ['submitted', 'users', permission],
    queryFn: () => submittedApi.users(permission),
  });
  return (
    <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">{placeholder}</option>
      {(users.data ?? []).map((u) => (
        <option key={u.username} value={u.username}>
          {u.displayName}
        </option>
      ))}
    </select>
  );
}
