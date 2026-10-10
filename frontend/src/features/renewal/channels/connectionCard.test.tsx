import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renewalChannelsApi } from '@/api/renewalChannels';
import { renewalWrapper } from '../testWrapper';
import { ConnectionCard } from './ConnectionCard';

afterEach(() => vi.restoreAllMocks());

describe('the connection card of a channel', () => {
  it('names the settings in business words', async () => {
    vi.spyOn(renewalChannelsApi, 'connection').mockResolvedValue({
      settings: {
        channel: 'CCM',
        mode: 'SIMULATOR',
        endpoint: '',
        keySetting: 'CCM key',
        keySet: false,
      },
      check: { channel: 'CCM', live: false, reachable: true, detail: 'CCM simulator' },
    });
    render(renewalWrapper(new Set(['RNW_VIEW']))(<ConnectionCard channel="CCM" />));
    expect(await screen.findByText('Address')).toBeInTheDocument();
    expect(screen.queryByText('Endpoint')).toBeNull();
  });
});
