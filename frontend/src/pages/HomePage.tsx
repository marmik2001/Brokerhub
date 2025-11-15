// src/pages/HomePage.tsx
import React, { useEffect, useState } from "react";
import { useAuth } from "../contexts/AuthContext";
import {
  fetchAggregateHoldings,
  type Holding as RawHolding,
} from "../services/portfolioService";
import StatCard from "../components/StatCard";
import DataTable from "../components/DataTable";
import EmptyState from "../components/EmptyState";

type UIHolding = {
  symbol: string;
  quantity: number;
  avgPrice: number;
  marketPrice: number;
  value: number;
  pnl: number;
};

const HomePage: React.FC = () => {
  const { currentAccount } = useAuth();
  const accountId = currentAccount?.accountId ?? undefined;

  const [tab, setTab] = useState<"dashboard" | "positions" | "feed">(
    "dashboard"
  );
  const [holdings, setHoldings] = useState<UIHolding[]>([]);
  const [positions, setPositions] = useState<UIHolding[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // fetch when tab or selected account changes
    if (tab === "dashboard") {
      setLoading(true);
      fetchAggregateHoldings(accountId)
        .then((h: RawHolding[]) => {
          // map backend Holding -> UIHolding (minimal mapping)
          const mapped: UIHolding[] = (h || []).map((x) => {
            const qty = x.quantity ?? 0;
            const last = x.lastPrice ?? 0;
            const avg = x.averagePrice ?? 0;
            const value = qty * last;
            return {
              symbol: x.tradingSymbol ?? "",
              quantity: qty,
              avgPrice: avg,
              marketPrice: last,
              value,
              pnl: x.pnl ?? 0,
            };
          });
          setHoldings(mapped);
        })
        .finally(() => setLoading(false));
    } else if (tab === "positions") {
      setLoading(true);
      // attempt to call fetchPositions(accountId) if available in your service
      // If not implemented, this should be a no-op or return an empty array.
      fetchAggregateHoldings(accountId)
        .then((p: RawHolding[]) => {
          const mapped = (p || []).map((x) => {
            const qty = x.quantity ?? 0;
            const last = x.lastPrice ?? 0;
            const avg = x.averagePrice ?? 0;
            const value = qty * last;
            return {
              symbol: x.tradingSymbol ?? "",
              quantity: qty,
              avgPrice: avg,
              marketPrice: last,
              value,
              pnl: x.pnl ?? 0,
              account: accountId,
            };
          });
          setPositions(mapped);
        })
        .finally(() => setLoading(false));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, accountId]);

  const totalValue = holdings.reduce((s, h) => s + (h.value ?? 0), 0);
  const totalPnl = holdings.reduce((s, h) => s + (h.pnl ?? 0), 0);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Home</h2>
        <div className="flex gap-2 bg-white p-1 rounded-md border">
          <button
            onClick={() => setTab("dashboard")}
            className={`px-3 py-1 rounded ${
              tab === "dashboard" ? "bg-gray-100" : ""
            }`}
          >
            Dashboard
          </button>
          <button
            onClick={() => setTab("positions")}
            className={`px-3 py-1 rounded ${
              tab === "positions" ? "bg-gray-100" : ""
            }`}
          >
            Positions
          </button>
          <button
            onClick={() => setTab("feed")}
            className={`px-3 py-1 rounded ${
              tab === "feed" ? "bg-gray-100" : ""
            }`}
          >
            Feed
          </button>
        </div>
      </div>

      {tab === "dashboard" && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <StatCard
              title="Total Value"
              value={`₹ ${totalValue.toLocaleString()}`}
            />
            <StatCard
              title="P&L"
              value={`₹ ${totalPnl.toLocaleString()}`}
              sub={totalPnl >= 0 ? "Unrealised gain" : "Unrealised loss"}
            />
            <StatCard title="Allocation" value="See Holdings" />
          </div>

          <div>
            <h3 className="text-lg font-medium mb-2">Holdings</h3>
            {loading ? (
              <div className="p-6 bg-white border rounded">Loading...</div>
            ) : holdings.length === 0 ? (
              <EmptyState
                title="No holdings available"
                description="Connect a broker or add mock data"
              />
            ) : (
              <DataTable
                data={holdings}
                columns={[
                  {
                    header: "Symbol",
                    accessor: (r) => (
                      <div className="font-medium">{r.symbol}</div>
                    ),
                  },
                  { header: "Qty", accessor: (r) => r.quantity },
                  { header: "Avg Price", accessor: (r) => r.avgPrice },
                  { header: "Mkt Price", accessor: (r) => r.marketPrice },
                  { header: "Value", accessor: (r) => r.value },
                  { header: "P&L", accessor: (r) => r.pnl },
                ]}
              />
            )}
          </div>
        </>
      )}

      {tab === "positions" && (
        <>
          <h3 className="text-lg font-medium">Positions (Today)</h3>
          {loading ? (
            <div className="p-6 bg-white border rounded">Loading...</div>
          ) : positions.length === 0 ? (
            <EmptyState title="No positions" />
          ) : (
            <DataTable
              data={positions}
              columns={[
                { header: "Symbol", accessor: (r) => r.symbol },
                { header: "Qty", accessor: (r) => r.quantity },
                { header: "Value", accessor: (r) => r.value },
                { header: "P&L", accessor: (r) => r.pnl },
              ]}
            />
          )}
        </>
      )}

      {tab === "feed" && (
        <div className="bg-white border rounded p-6">
          <EmptyState
            title="Coming soon..."
            description="Activity feed & notifications will be added here."
          />
        </div>
      )}
    </div>
  );
};

export default HomePage;
