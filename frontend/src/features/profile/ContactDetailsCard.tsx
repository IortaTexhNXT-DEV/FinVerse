import { useMutation } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useState } from 'react';
import { authApi } from '@/api/auth';
import type { ContactDetails } from '@/api/auth';
import { ApiError } from '@/api/client';
import type { UserProfile } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { contactProblems } from './profileRules';

/**
 * The user's own e-mail address and mobile number (UQ17; FR-UA-005): changed here without an
 * access request and written to the access change log.
 */
export function ContactDetailsCard({
  user,
  onSaved,
}: Readonly<{ user: UserProfile; onSaved: (profile: UserProfile) => void }>) {
  const toast = useToast();
  const [form, setForm] = useState<ContactDetails>({
    email: user.email ?? '',
    mobileNo: user.mobileNo ?? '',
  });
  const [touched, setTouched] = useState(false);
  const save = useMutation({
    mutationFn: authApi.updateContact,
    onSuccess: (profile) => {
      toast.success('Contact details saved');
      setTouched(false);
      onSaved(profile);
    },
  });
  const local = touched ? contactProblems(form) : {};
  const server = save.error instanceof ApiError ? save.error.fieldErrors : {};
  const errorOf = (key: keyof ContactDetails) => local[key] ?? server[key];
  const unchanged = form.email === (user.email ?? '') && form.mobileNo === (user.mobileNo ?? '');
  const submit = () => {
    setTouched(true);
    if (Object.keys(contactProblems(form)).length === 0) {
      save.mutate(form);
    }
  };
  return (
    <Card title="Contact details">
      <form
        className="stack"
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        {Object.keys(server).length === 0 && <ErrorAlert error={save.error} />}
        <div className="form-grid">
          <Field label="E-mail address" required error={errorOf('email')}>
            {(id) => (
              <input
                id={id}
                className="input"
                type="email"
                autoComplete="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            )}
          </Field>
          <Field
            label="Mobile number"
            error={errorOf('mobileNo')}
            hint="For example +63 917 123 4567"
          >
            {(id) => (
              <input
                id={id}
                className="input"
                type="tel"
                autoComplete="tel"
                value={form.mobileNo}
                onChange={(e) => setForm({ ...form, mobileNo: e.target.value })}
              />
            )}
          </Field>
        </div>
        <div>
          <Button
            type="submit"
            variant="primary"
            icon={<Save size={16} />}
            busy={save.isPending}
            disabled={unchanged}
          >
            Save Contact Details
          </Button>
        </div>
      </form>
    </Card>
  );
}
