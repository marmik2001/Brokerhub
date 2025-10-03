// src/services/portfolioService.ts
export interface Holding {
  id: string;
  symbol: string;
  name: string;
  quantity: number;
  avgPrice: number;
  marketPrice: number;
  value: number;
  pnl: number;
  account: string; // account/broker name
  owner: string; // owner loginId
}

const SAMPLE_HOLDINGS: Holding[] = [
  {
    id: "1",
    symbol: "AAPL",
    name: "Apple Inc.",
    quantity: 10,
    avgPrice: 140,
    marketPrice: 170,
    value: 1700,
    pnl: 300,
    account: "Zerodha",
    owner: "testuser",
  },
  {
    id: "2",
    symbol: "INFY",
    name: "Infosys",
    quantity: 20,
    avgPrice: 1000,
    marketPrice: 1250,
    value: 25000,
    pnl: 5000,
    account: "Zerodha",
    owner: "testuser",
  },
  {
    id: "3",
    symbol: "TSLA",
    name: "Tesla",
    quantity: 2,
    avgPrice: 800,
    marketPrice: 780,
    value: 1560,
    pnl: -40,
    account: "Other",
    owner: "family_member",
  },
];

export async function fetchHoldings(filter?: {
  owner?: string;
  account?: string;
}): Promise<Holding[]> {
  await new Promise((r) => setTimeout(r, 300));
  let items = SAMPLE_HOLDINGS.slice();
  if (filter?.owner) items = items.filter((h) => h.owner === filter.owner);
  if (filter?.account)
    items = items.filter((h) => h.account === filter.account);
  return items;
}

export async function fetchPositions(): Promise<Holding[]> {
  // For MVP, reuse holdings as positions snapshot
  return fetchHoldings();
}
