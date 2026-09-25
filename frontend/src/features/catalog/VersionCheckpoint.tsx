import { useMutation, useQueryClient } from '@tanstack/react-query';
import { BadgeCheck, Undo2 } from 'lucide-react';
import { useState } from 'react';
import { productCatalogApi } from '@/api/productCatalog';
import type { VersionDetail } from '@/api/productCatalog';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { VALIDATION_CHECKLIST } from './versionForm';

/** The checklist confirmed at validation, from its JSON. */
function confirmedItems(json: string | undefined): string[] {
  if (!json) {
    return [];
  }
  try {
    const parsed: unknown = JSON.parse(json);
    return Array.isArray(parsed) ? parsed.map(String) : [];
  } catch {
    return [];
  }
}

/**
 * The post-set-up validation checkpoint of a package version (PMADD06): the validator (TSU Head
 * or Business Administrator, never the maker or submitter) confirms the checklist and releases the
 * version, or returns it to MBS with a reason. After release it shows who validated it, when, the
 * checklist and the test premium.
 */
export function VersionCheckpoint({ detail }: Readonly<{ detail: VersionDetail }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const s = detail.summary;
  const [checked, setChecked] = useState<string[]>([]);
  const [returning, setReturning] = useState(false);
  const refresh = (message: string) => {
    void queryClient.invalidateQueries({ queryKey: ['catalog'] });
    toast.success(message);
  };
  const validate = useMutation({
    mutationFn: () => productCatalogApi.validateVersion(s.productCode, s.versionNo, checked),
    onSuccess: () => refresh(`Version ${s.versionNo} released`),
    onError: (e) => toast.error(e.message),
  });
  const giveBack = useMutation({
    mutationFn: (reason: string) =>
      productCatalogApi.returnVersion(s.productCode, s.versionNo, reason),
    onSuccess: () => {
      setReturning(false);
      refresh(`Version ${s.versionNo} returned to MBS`);
    },
  });
  const ownVersion = user?.username === s.maker || user?.username === s.submittedBy;
  const mayValidate = s.status === 'FOR_VALIDATION' && can('PRODUCT_VALIDATE') && !ownVersion;
  if (s.validatedBy) {
    return (
      <Card title="Validation">
        <p>
          Validated by <strong>{s.validatedBy}</strong> on {formatDateTime(s.validatedAt)}
          {detail.testPremium !== undefined && (
            <>
              {' '}
              · Test premium <Amount value={detail.testPremium} />
            </>
          )}
        </p>
        <ul>
          {confirmedItems(detail.validationChecklist).map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </Card>
    );
  }
  if (!mayValidate) {
    return null;
  }
  const complete = VALIDATION_CHECKLIST.every((item) => checked.includes(item));
  return (
    <Card
      title="Validation Checklist"
      actions={
        <div className="row">
          <Button variant="secondary" icon={<Undo2 size={16} />} onClick={() => setReturning(true)}>
            Return to MBS
          </Button>
          <Button
            variant="accent"
            icon={<BadgeCheck size={16} />}
            disabled={!complete}
            busy={validate.isPending}
            onClick={() => validate.mutate()}
          >
            Validate and Release
          </Button>
        </div>
      }
    >
      <p className="muted">
        Confirm each item; the test premium is computed on release. You cannot validate a version
        you set up or submitted.
      </p>
      {VALIDATION_CHECKLIST.map((item) => (
        <label className="checkbox" key={item}>
          <input
            type="checkbox"
            checked={checked.includes(item)}
            onChange={(e) =>
              setChecked(e.target.checked ? [...checked, item] : checked.filter((c) => c !== item))
            }
          />
          {item}
        </label>
      ))}
      {returning && (
        <ActionDialog
          title={`Return Version ${s.versionNo} to MBS`}
          reasonLov="RETURN_REASON"
          confirmLabel="Return"
          busy={giveBack.isPending}
          error={giveBack.error}
          onClose={() => setReturning(false)}
          onConfirm={(note) =>
            giveBack.mutate(
              [note.reasonCode, note.comment].filter((v) => v !== undefined && v !== '').join(': '),
            )
          }
        />
      )}
    </Card>
  );
}
