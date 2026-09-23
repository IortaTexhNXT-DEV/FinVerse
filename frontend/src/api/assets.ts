import { api, toQuery } from './client';
import type { RecordStatus } from './types';

export type DepreciationMethod = 'STRAIGHT_LINE' | 'DECLINING_BALANCE';
export type AssetStatus =
  'PENDING_CAPITALIZATION' | 'ACTIVE' | 'FULLY_DEPRECIATED' | 'DISPOSED' | 'TRANSFERRED';
export type MovementType = 'ADDITION' | 'DISPOSAL' | 'TRANSFER';

export interface AssetCategory {
  id: number;
  companyId: number;
  code: string;
  name: string;
  assetAccount: string;
  accumulatedDepreciationAccount: string;
  depreciationExpenseAccount: string;
  depreciationMethod: DepreciationMethod;
  usefulLifeMonths: number;
  residualPercent: number;
  recordStatus: RecordStatus;
  createdBy: string;
  /** Creator or last maintainer; unchanged by authorization. */
  maker?: string;
  authorizedBy?: string;
}

export type AssetCategoryInput = Omit<
  AssetCategory,
  'id' | 'recordStatus' | 'createdBy' | 'maker' | 'authorizedBy'
>;

export interface FixedAsset {
  id: number;
  companyId: number;
  branchId: number;
  categoryId: number;
  categoryCode: string;
  categoryName: string;
  tagNo: string;
  description: string;
  costCenter?: string;
  supplierCode?: string;
  acquisitionDate: string;
  acquisitionCost: number;
  residualValue: number;
  depreciationMethod: DepreciationMethod;
  usefulLifeMonths: number;
  location?: string;
  custodian?: string;
  settlementAccount?: string;
  takeOn: boolean;
  capitalizationDate: string;
  accumulatedDepreciation: number;
  netBookValue: number;
  monthsDepreciated: number;
  lastDepreciationPeriod?: string;
  status: AssetStatus;
  capitalizationBatchNo?: string;
  disposalDate?: string;
  recordStatus: RecordStatus;
  createdBy: string;
  /** Creator or last maintainer; unchanged by authorization. */
  maker?: string;
  authorizedBy?: string;
}

export interface FixedAssetInput {
  companyId: number;
  branchId: number;
  categoryId: number;
  tagNo: string;
  description: string;
  costCenter: string;
  supplierCode?: string;
  acquisitionDate: string;
  acquisitionCost: number;
  depreciationMethod?: DepreciationMethod;
  usefulLifeMonths?: number;
  location?: string;
  custodian?: string;
  settlementAccount?: string;
  takeOn: boolean;
  capitalizationDate?: string;
  openingAccumulatedDepreciation?: number;
  openingMonths?: number;
}

export interface AssetMovement {
  id: number;
  movementType: MovementType;
  movementDate: string;
  fromBranchId?: number;
  toBranchId?: number;
  cost: number;
  accumulatedDepreciation: number;
  netBookValue: number;
  proceeds: number;
  gainLoss: number;
  reference?: string;
  remarks?: string;
  batchNo?: string;
  createdBy: string;
}

export interface DisposalInput {
  disposalDate: string;
  proceeds: number;
  bankAccount?: string;
  reference?: string;
  remarks?: string;
}

export interface TransferInput {
  toBranchId: number;
  transferDate: string;
  location?: string;
  custodian?: string;
  remarks?: string;
}

export interface DepreciationLine {
  assetId: number;
  tagNo: string;
  description: string;
  categoryCode: string;
  branchId: number;
  costCenter?: string;
  months: number;
  amount: number;
  accumulatedAfter: number;
  netBookValueAfter: number;
  batchNo?: string;
}

export interface DepreciationRun {
  id: number;
  period: string;
  periodEnd: string;
  assetCount: number;
  totalDepreciation: number;
  createdBy: string;
  createdAt: string;
}

export interface DepreciationPreview {
  period: string;
  posted: boolean;
  run?: DepreciationRun;
  total: number;
  lines: DepreciationLine[];
}

export interface AssetSearch {
  status?: AssetStatus;
  branchId?: number;
  categoryId?: number;
  q?: string;
}

const REGISTER = '/assets/register';

export const assetsApi = {
  categories: (companyId: number) =>
    api.get<AssetCategory[]>(`/assets/categories${toQuery({ companyId })}`),
  createCategory: (body: AssetCategoryInput) => api.post<AssetCategory>('/assets/categories', body),
  updateCategory: (id: number, body: AssetCategoryInput) =>
    api.put<AssetCategory>(`/assets/categories/${id}`, body),
  authorizeCategory: (id: number) => api.post<AssetCategory>(`/assets/categories/${id}/authorize`),
  search: (companyId: number, filter: AssetSearch) =>
    api.get<FixedAsset[]>(`${REGISTER}${toQuery({ companyId, ...filter })}`),
  create: (body: FixedAssetInput) => api.post<FixedAsset>(REGISTER, body),
  update: (id: number, body: FixedAssetInput) => api.put<FixedAsset>(`${REGISTER}/${id}`, body),
  capitalize: (id: number) => api.post<FixedAsset>(`${REGISTER}/${id}/capitalize`),
  dispose: (id: number, body: DisposalInput) =>
    api.post<AssetMovement>(`${REGISTER}/${id}/dispose`, body),
  transfer: (id: number, body: TransferInput) =>
    api.post<AssetMovement>(`${REGISTER}/${id}/transfer`, body),
  movements: (id: number) => api.get<AssetMovement[]>(`${REGISTER}/${id}/movements`),
  preview: (companyId: number, period: string) =>
    api.get<DepreciationPreview>(`/assets/depreciation/preview${toQuery({ companyId, period })}`),
  postRun: (companyId: number, period: string) =>
    api.post<DepreciationRun>(`/assets/depreciation/runs${toQuery({ companyId, period })}`),
  runs: (companyId: number) =>
    api.get<DepreciationRun[]>(`/assets/depreciation/runs${toQuery({ companyId })}`),
};
