import { useQuery } from '@tanstack/react-query';
import { systemApi } from '@/api/system';
import { Modal } from '@/components/ui/Modal';
import { formatDateTime } from '@/utils/format';

/** About iNXT BrokerVerse: product, version, build and vendor. */
export function AboutDialog({ open, onClose }: Readonly<{ open: boolean; onClose: () => void }>) {
  const about = useQuery({ queryKey: ['about'], queryFn: systemApi.about, enabled: open });
  const a = about.data;
  return (
    <Modal title="About iNXT BrokerVerse" open={open} onClose={onClose}>
      <div className="stack" style={{ textAlign: 'center' }}>
        <div className="brand-mark" style={{ margin: '0 auto' }} aria-hidden="true">
          FV
        </div>
        <h2 style={{ margin: 0 }}>{a?.product ?? 'iNXT BrokerVerse'}</h2>
        <p className="muted" style={{ margin: 0 }}>
          Insurance general ledger and finance platform
        </p>
        <dl style={{ margin: 0 }}>
          <dt className="muted">Version</dt>
          <dd style={{ margin: 0, fontWeight: 600 }}>{a?.version ?? '…'}</dd>
          {a?.buildTime !== undefined && (
            <>
              <dt className="muted">Built</dt>
              <dd style={{ margin: 0 }}>{formatDateTime(a.buildTime)}</dd>
            </>
          )}
        </dl>
        <p style={{ margin: 0 }}>
          © {new Date().getFullYear()} {a?.vendor ?? 'IortaTechNXT'}. All rights reserved.
        </p>
      </div>
    </Modal>
  );
}
