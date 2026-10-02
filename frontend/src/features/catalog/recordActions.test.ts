import { describe, expect, it } from 'vitest';
import { actionMessage } from './recordActionMessage';

describe('catalog record action message', () => {
  it('names the record by what the menu is about, not by its code', () => {
    expect(actionMessage('FLEET_REPAIR', 'Accredited repair shops for fleets', 'authorize')).toBe(
      'Accredited repair shops for fleets authorized',
    );
    expect(actionMessage('PRD-001', '', 'authorize')).toBe('PRD-001 authorized');
  });

  it('names a record without a reference by its label, never a bare verb', () => {
    expect(actionMessage('', 'Accredited repair shops for fleets', 'authorize')).toBe(
      'Accredited repair shops for fleets authorized',
    );
    expect(actionMessage(null, 'Flood Exclusion', 'deactivate')).toBe(
      'Flood Exclusion deactivated',
    );
  });
});
