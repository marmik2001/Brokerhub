import api from "../api";
/**
 * Lightweight portfolio types used by the dashboard.
 *
 * Note: kept minimal so frontend code remains decoupled from backend DTOs.
 */
export type Holding = {
  exchange: string;
  tradingSymbol: string;
  isin?: string;
  quantity: number;
  averagePrice: number;
  pnl: number;
  lastPrice: number;
  dayChange: number;
  dayChangePercentage: number;
};

/**
 * Service to fetch aggregated holdings for an account.
 *
 * Assumptions:
 * - axios instance is exported from src/api (imported as `api`).
 * - endpoint: GET /api/accounts/{accountId}/aggregate-holdings
 * - If accountId is falsy, this returns an empty array immediately.
 */

export async function fetchAggregateHoldings(
  accountId?: string
): Promise<Holding[]> {
  if (!accountId) return [];
  const url = `/accounts/${accountId}/aggregate-holdings`;
  const resp = await api.get<Holding[]>(url);
  return resp?.data ?? [];
}
