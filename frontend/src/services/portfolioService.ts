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
 * Service to fetch aggregated holdings for an account.
 */
export async function fetchAggregateHoldings(
  accountId?: string
): Promise<Holding[]> {
  if (!accountId) return [];
  const url = `/accounts/${accountId}/aggregate-holdings`;
  try {
    const resp = await api.get<Holding[]>(url);
    return resp?.data ?? [];
  } catch (err) {
    return [];
  }
}

/**
 * Service to fetch aggregated positions for an account.
 */
export async function fetchAggregatePositions(
  accountId?: string
): Promise<Position[]> {
  if (!accountId) return [];
  const url = `/accounts/${accountId}/aggregate-positions`;
  try {
    const resp = await api.get<Position[]>(url);
    return resp?.data ?? [];
  } catch (err) {
    return [];
  }
}
