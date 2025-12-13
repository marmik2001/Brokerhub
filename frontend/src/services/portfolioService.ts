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

export type AggregatedResponse<T> = {
  full: T[];
  partial: string[];
};

/**
 * Service to fetch aggregated holdings for an account.
 */
export async function fetchAggregateHoldings(
  accountId?: string
): Promise<AggregatedResponse<Holding>> {
  if (!accountId) return { full: [], partial: [] };
  const url = `/accounts/${accountId}/aggregate-holdings`;
  try {
    const resp = await api.get<AggregatedResponse<Holding>>(url);
    return resp?.data ?? { full: [], partial: [] };
  } catch {
    return { full: [], partial: [] };
  }
}

/**
 * Service to fetch aggregated positions for an account.
 */
export async function fetchAggregatePositions(
  accountId?: string
): Promise<AggregatedResponse<Position>> {
  if (!accountId) return { full: [], partial: [] };
  const url = `/accounts/${accountId}/aggregate-positions`;
  try {
    const resp = await api.get<AggregatedResponse<Position>>(url);
    return resp?.data ?? { full: [], partial: [] };
  } catch {
    return { full: [], partial: [] };
  }
}
