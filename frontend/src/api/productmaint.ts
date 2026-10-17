import { api, toQuery } from './client';
import type {
  Advisory,
  AdvisoryInput,
  Comparative,
  ComparativeOutput,
  ExpiryRow,
  HomeCounts,
  InsurerChoice,
  InsurerResponse,
  NegotiationRound,
  PackageDates,
  PackageRequest,
  Prefill,
  RenewalResult,
  RequestInput,
  RequestListItem,
  RequestSearch,
  Requirements,
  ResponseHistory,
  ResponseInput,
  Scheme,
  SetupInput,
  Signoff,
} from './productmaintTypes';
import type { PageResponse } from './types';

export type * from './productmaintTypes';

/** Entity type of package requests in the workflow, attachments and messages. */
export const PACKAGE_REQUEST_ENTITY = 'PackageRequest';

const base = '/product-maintenance';
const req = (id: number) => `${base}/requests/${id}`;
const comment = (text?: string) => ({ comment: text });
const joined = (values?: string[]) =>
  values === undefined || values.length === 0 ? undefined : values.join(',');

/** Product Maintenance package requests (BRD-3, `productmaint`). */
export const productMaintApi = {
  search: (companyId: number, s: RequestSearch, page = 0, size = 20) =>
    api.get<PageResponse<RequestListItem>>(
      `${base}/requests${toQuery({
        companyId,
        text: s.text,
        stage: joined(s.stage),
        type: joined(s.type),
        scope: s.scope,
        mine: s.mine,
        expiringWithin: s.expiringWithin,
        page,
        size,
      })}`,
    ),
  get: (id: number) => api.get<PackageRequest>(req(id)),
  create: (input: RequestInput) => api.post<PackageRequest>(`${base}/requests`, input),
  update: (id: number, input: RequestInput) => api.put<PackageRequest>(req(id), input),
  submit: (id: number, text?: string) =>
    api.post<PackageRequest>(`${req(id)}/submit`, comment(text)),
  approve: (id: number, text?: string) =>
    api.post<PackageRequest>(`${req(id)}/approve`, comment(text)),
  recommend: (id: number, text: string) =>
    api.post<PackageRequest>(`${req(id)}/recommend`, { text }),
  approveTsu: (id: number, text?: string) =>
    api.post<PackageRequest>(`${req(id)}/tsu-approve`, comment(text)),
  formPdf: (id: number) => api.getFile(`${req(id)}/form.pdf`),
  prefill: (productCode: string) => api.get<Prefill>(`${base}/prefill${toQuery({ productCode })}`),
  counts: (companyId: number) => api.get<HomeCounts>(`${base}/counts${toQuery({ companyId })}`),

  rounds: (id: number) => api.get<NegotiationRound[]>(`${req(id)}/rounds`),
  revise: (id: number, insurers: string[], notes?: string) =>
    api.post<NegotiationRound>(`${req(id)}/rounds`, { insurers, notes }),
  prepare: (id: number, roundNo: number, insurers: string[], notes?: string) =>
    api.put<NegotiationRound>(`${req(id)}/rounds/${roundNo}`, { insurers, notes }),
  submitSlip: (id: number, roundNo: number, replyBy?: string) =>
    api.post<NegotiationRound>(`${req(id)}/rounds/${roundNo}/quotation-slip/submit`, { replyBy }),
  approveSlip: (id: number, roundNo: number) =>
    api.post<NegotiationRound>(`${req(id)}/rounds/${roundNo}/quotation-slip/approve`),
  slipPdf: (id: number, roundNo: number) =>
    api.getFile(`${req(id)}/rounds/${roundNo}/quotation-slip.pdf`),
  resend: (id: number, roundNo: number, insurerCode: string) =>
    api.post<InsurerResponse>(`${req(id)}/rounds/${roundNo}/insurers/${insurerCode}/resend`),
  comparative: (id: number, roundNo: number) =>
    api.get<Comparative>(`${req(id)}/rounds/${roundNo}/comparative`),
  recordResponse: (id: number, responseId: number, input: ResponseInput) =>
    api.put<InsurerResponse>(`${req(id)}/responses/${responseId}`, input),
  attachResponse: (id: number, responseId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<InsurerResponse>(`${req(id)}/responses/${responseId}/document`, form);
  },
  history: (id: number) => api.get<ResponseHistory[]>(`${req(id)}/responses/history`),
  termsFinal: (id: number, insurers: InsurerChoice[], text?: string) =>
    api.post<PackageRequest>(`${req(id)}/terms-final`, { insurers, comment: text }),
  releaseToMarketing: (id: number, text?: string) =>
    api.post<PackageRequest>(`${req(id)}/release-to-marketing`, comment(text)),
  skipMarketingReview: (id: number, text?: string) =>
    api.post<PackageRequest>(`${req(id)}/skip-marketing-review`, comment(text)),
  acceptTerms: (id: number, text?: string) =>
    api.post<PackageRequest>(`${req(id)}/accept-terms`, comment(text)),

  outputs: (id: number) => api.get<ComparativeOutput[]>(`${req(id)}/comparatives`),
  compileMaster: (id: number) => api.post<ComparativeOutput>(`${req(id)}/comparatives/master`),
  clientView: (id: number, title: string, fields: string[], insurers: string[]) =>
    api.post<ComparativeOutput>(`${req(id)}/comparatives`, { title, fields, insurers }),
  outputFile: (id: number, outputId: number, format: 'pdf' | 'xlsx') =>
    api.getFile(`${req(id)}/comparatives/${outputId}.${format}`),

  requirements: (id: number) => api.get<Requirements>(`${req(id)}/requirements`),
  updateRequirements: (id: number, scheme: Scheme, dates: PackageDates) =>
    api.put<PackageRequest>(`${req(id)}/requirements`, { scheme, dates }),
  submitRequirements: (id: number, text?: string) =>
    api.post<PackageRequest>(`${req(id)}/submit-requirements`, comment(text)),
  packageSlipPdf: (id: number) => api.getFile(`${req(id)}/package-slip.pdf`),
  signoff: (id: number, text?: string) => api.post<Signoff>(`${req(id)}/signoff`, comment(text)),
  setup: (id: number, input: SetupInput) => api.post<PackageRequest>(`${req(id)}/setup`, input),
  returnIncomplete: (id: number, reasonCode: string, text?: string) =>
    api.post<PackageRequest>(`${req(id)}/return-incomplete`, { reasonCode, comment: text }),
  retire: (id: number, text?: string) =>
    api.post<PackageRequest>(`${req(id)}/retire`, comment(text)),

  advisories: (id: number) => api.get<Advisory[]>(`${req(id)}/advisories`),
  pendingAdvisories: (companyId: number) =>
    api.get<Advisory[]>(`${base}/advisories${toQuery({ companyId })}`),
  updateAdvisory: (advisoryId: number, input: AdvisoryInput) =>
    api.put<Advisory>(`${base}/advisories/${advisoryId}`, input),
  sendAdvisory: (advisoryId: number) => api.post<Advisory>(`${base}/advisories/${advisoryId}/send`),

  expiry: (companyId: number, within?: number) =>
    api.get<ExpiryRow[]>(`${base}/expiry${toQuery({ companyId, within })}`),
  renew: (companyId: number, productCodes: string[]) =>
    api.post<RenewalResult[]>(`${base}/expiry/renewal-requests`, { companyId, productCodes }),
};
