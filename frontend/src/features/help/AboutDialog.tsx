import { useQuery } from '@tanstack/react-query';
import { systemApi } from '@/api/system';
import { Modal } from '@/components/ui/Modal';
import { BRAND } from '@/branding';
import { formatDateTime } from '@/utils/format';

/** About BIBS: product, version, build, platform and vendor. */
export function AboutDialog({ open, onClose }: Readonly<{ open: boolean; onClose: () => void }>) {
  const about = useQuery({ queryKey: ['about'], queryFn: systemApi.about, enabled: open });
  const a = about.data;
  return (
    <Modal title={`About ${BRAND.product}`} open={open} onClose={onClose}>
      <div className="stack about-body">
        <img className="about-client-logo" src={BRAND.clientLogo} alt={BRAND.client} />
        <h2>
          {BRAND.product} – {BRAND.productName}
        </h2>
        <p className="muted">Insurance broking and finance for {BRAND.client}</p>
        <dl className="about-facts">
          <dt className="muted">Version</dt>
          <dd>{a?.version ?? '…'}</dd>
          {a?.buildTime !== undefined && (
            <>
              <dt className="muted">Built</dt>
              <dd>{formatDateTime(a.buildTime)}</dd>
            </>
          )}
          <dt className="muted">Platform</dt>
          <dd>{BRAND.platform}</dd>
        </dl>
        <div className="powered-by">
          <span>Powered by</span>
          <img src={BRAND.vendorLogo} alt={BRAND.vendor} />
        </div>
        <p className="muted">
          © {new Date().getFullYear()} {BRAND.vendor}. All rights reserved.
        </p>
      </div>
    </Modal>
  );
}
