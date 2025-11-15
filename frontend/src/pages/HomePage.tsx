// src/pages/HomePage.tsx
import React, { useEffect, useState } from "react";
import { useAuth } from "../contexts/AuthContext";
import {
  type Holding,
  fetchAggregateHoldings,
  fetchAggregatePositions,
} from "../services/portfolioService";
import StatCard from "../components/StatCard";
import DataTable from "../components/DataTable";
import EmptyState from "../components/EmptyState";

/**
 * Minimal HomePage that:
 * - fetches aggregate holdings for selected account (currentAccount.accountId)
 * - ensures holdings/positions are never null (use empty arrays fallback)
 * - displays only tradingSymbol, quantity, averagePrice, pnl, lastPrice
 */

const HomePage: React.FC = () => {
  const { currentAccount } = useAuth();
  const accountId = currentAccount?.accountId;

  const [tab, setTab] = useState<"dashboard" | "positions" | "feed">(
    "dashboard"
  );
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [positions, setPositions] = useState<Holding[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // fetch when tab or selected account changes
    if (tab === "dashboard") {
      setLoading(true);
      fetchAggregateHoldings(accountId)
        .then((h) => {
          // ensure array (service should already return empty array, but be defensive)
          setHoldings(h || []);
        })
        .catch(() => setHoldings([]))
        .finally(() => setLoading(false));
    } else if (tab === "positions") {
      setLoading(true);
      fetchAggregatePositions(accountId)
        .then((p) => {
          setPositions(p || []);
        })
        .catch(() => setPositions([]))
        .finally(() => setLoading(false));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, accountId]);

  // compute totals defensively
  const totalValue = holdings.reduce((s, h) => {
    const last = h.lastPrice ?? 0;
    const qty = h.quantity ?? 0;
    return s + last * qty;
  }, 0);

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
              <DataTable<Holding>
                data={holdings}
                columns={[
                  {
                    header: "Symbol",
                    accessor: (r) => (
                      <div className="font-medium">{r.tradingSymbol}</div>
                    ),
                  },
                  { header: "Qty", accessor: (r) => r.quantity },
                  {
                    header: "Avg Price",
                    accessor: (r) => r.averagePrice.toFixed(2),
                  },
                  {
                    header: "Last Price",
                    accessor: (r) => r.lastPrice.toFixed(2),
                  },
                  { header: "P&L", accessor: (r) => r.pnl.toFixed(2) },
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
            <DataTable<Holding>
              data={positions}
              columns={[
                { header: "Symbol", accessor: (r) => r.tradingSymbol },
                { header: "Qty", accessor: (r) => r.quantity },
                {
                  header: "Value",
                  accessor: (r) => (r.lastPrice ?? 0) * (r.quantity ?? 0),
                },
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
