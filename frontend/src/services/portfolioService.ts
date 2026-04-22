import api from "../api";

/**
 * Lightweight holding type used by the dashboard.
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
 * Lightweight position type used by the dashboard.
 */
export type Position = {
  exchange: string;
  tradingSymbol: string;
  quantity: number;
  averagePrice: number;
  lastPrice: number;
  pnl: number;
  dayChange?: number;
  dayChangePercentage?: number;
};

/**
 * Wrapper for aggregated responses containing full and partial data results.
 */
export type AggregatedResponse<T> = {
  full: T[];
  partial: string[];
};

/**
 * Fetches aggregated holdings for a specific account.
 * GET /api/accounts/{accountId}/aggregate-holdings
 */
export async function fetchAggregateHoldings(
  accountId?: string
): Promise<AggregatedResponse<Holding>> {
  if (!accountId) return { full: [], partial: [] };
  const url = `/accounts/${accountId}/aggregate-holdings`;
  const resp = await api.get<AggregatedResponse<Holding>>(url);
  return resp?.data ?? { full: [], partial: [] };
}

/**
 * Fetches aggregated positions for a specific account.
 * GET /api/accounts/{accountId}/aggregate-positions
 */
export async function fetchAggregatePositions(
  accountId?: string
): Promise<AggregatedResponse<Position>> {
  if (!accountId) return { full: [], partial: [] };
  const url = `/accounts/${accountId}/aggregate-positions`;
  const resp = await api.get<AggregatedResponse<Position>>(url);
  return resp?.data ?? { full: [], partial: [] };
}
