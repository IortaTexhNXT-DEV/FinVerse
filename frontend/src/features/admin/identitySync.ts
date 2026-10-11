import type {
  DirectoryAccount,
  DirectoryStatus,
  IdentityEvent,
  IdentityEventType,
} from '@/api/identity';

/** The events a simulator account can send, with their names. */
export const SIMULATED_EVENTS: readonly { type: IdentityEventType; label: string }[] = [
  { type: 'JOINER', label: 'Send joiner' },
  { type: 'MOVER', label: 'Send mover (details changed)' },
  { type: 'LEAVER', label: 'Send leaver' },
  { type: 'REHIRE', label: 'Send rehire' },
  { type: 'STATUS', label: 'Send status change' },
];

/** The status pill of an outcome: applied and no change are done, refused and failed need review. */
export function outcomeStatus(event: IdentityEvent): string {
  switch (event.status) {
    case 'APPLIED':
      return 'COMPLETED';
    case 'NO_CHANGE':
      return 'CLOSED';
    case 'REFUSED':
      return 'REJECTED';
    default:
      return 'FAILED';
  }
}

/** A new, empty account of the simulator. */
export const EMPTY_ACCOUNT: DirectoryAccount = {
  windowsId: '',
  userId: '',
  email: '',
  firstName: '',
  lastName: '',
  status: 'ACTIVE',
  adGroup: 'BIBS Users',
};

/** What is missing before an account can be saved, in words. */
export function missingAccountFields(account: DirectoryAccount): string[] {
  const missing: string[] = [];
  const required: [keyof DirectoryAccount, string][] = [
    ['windowsId', 'Windows ID'],
    ['userId', 'User ID'],
    ['email', 'E-mail'],
    ['firstName', 'First name'],
    ['lastName', 'Last name'],
  ];
  required.forEach(([key, label]) => {
    if ((account[key] ?? '').trim() === '') {
      missing.push(label);
    }
  });
  return missing;
}

/** The statuses an account of the simulator can take. */
export const SIMULATOR_STATUSES: readonly DirectoryStatus[] = [
  'ACTIVE',
  'INACTIVE',
  'LOCKED',
  'DISABLED',
  'DEACTIVATED',
];
