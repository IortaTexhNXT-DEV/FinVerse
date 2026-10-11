import { describe, expect, it } from 'vitest';
import { approverLabels } from './approverLabels';

describe('approverLabels', () => {
  it('shows each approver by name, without the user ID', () => {
    const labels = approverLabels([
      { username: 'uamapprover', fullName: 'Ulysses Access Approver' },
      { username: 'approver', fullName: 'Andres Approver' },
    ]);
    expect(labels.get('uamapprover')).toBe('Ulysses Access Approver');
    expect(labels.get('approver')).toBe('Andres Approver');
  });

  it('tells two approvers of the same name apart by their user ID', () => {
    const labels = approverLabels([
      { username: 'a013000101', fullName: 'Maria Santos' },
      { username: 'a013000102', fullName: 'Maria Santos' },
      { username: 'approver', fullName: 'Andres Approver' },
    ]);
    expect(labels.get('a013000101')).toBe('Maria Santos (a013000101)');
    expect(labels.get('a013000102')).toBe('Maria Santos (a013000102)');
    expect(labels.get('approver')).toBe('Andres Approver');
  });
});
