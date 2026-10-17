import { describe, expect, it } from 'vitest';
import { createQueryClient } from './queryClient';

describe('createQueryClient', () => {
  it('marks the workflow headers stale after any successful action', async () => {
    const client = createQueryClient();
    client.setQueryData(['workflow', 'Receipt', '11'], { stage: 'FOR_APPROVAL' });
    client.setQueryData(['receipts', 11], { status: 'ISSUED' });
    await client
      .getMutationCache()
      .build(client, { mutationFn: () => Promise.resolve('posted') })
      .execute(undefined);
    expect(client.getQueryState(['workflow', 'Receipt', '11'])?.isInvalidated).toBe(true);
    expect(client.getQueryState(['receipts', 11])?.isInvalidated).toBe(false);
  });
});
