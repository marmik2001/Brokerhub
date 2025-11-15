// src/pages/HomePage.tsx
import React, { useEffect, useMemo, useState } from "react";
import { useAuth } from "../contexts/AuthContext";
import {
  type Holding,
  fetchAggregateHoldings,
  fetchAggregatePositions,
} from "../services/portfolioService";
import StatCard from "../components/StatCard";
import DataTable from "../components/DataTable";
import EmptyState from "../components/EmptyState";
import HoldingsSearchBar from "../components/HoldingsSearchBar";
import FilterChips, { type FilterKey } from "../components/FilterChips";
import { toast } from "react-hot-toast";

/**
 * HomePage with search + simple filter chips.
 * - Search filters by tradingSymbol or isin (case-insensitive substring)
 * - Filter chips: All / Profitable / Loss / Zero P&L
 *
 * Filtering is client-side and non-destructive.
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

  // search & filter state
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<FilterKey>("ALL");

  useEffect(() => {
    if (tab === "dashboard") {
      setLoading(true);
      fetchAggregateHoldings(accountId)
        .then((h) => {
          setHoldings(h || []);
        })
        .catch((err) => {
          console.error("Failed to load holdings", err);
          toast.error("Failed to load holdings");
          setHoldings([]);
        })
        .finally(() => setLoading(false));
    } else if (tab === "positions") {
      setLoading(true);
      fetchAggregatePositions(accountId)
        .then((p) => setPositions(p || []))
        .catch(() => setPositions([]))
        .finally(() => setLoading(false));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, accountId]);

  // derived filtered holdings using search + filter
  const filteredHoldings = useMemo(() => {
    const q = (search || "").trim().toLowerCase();
    return holdings.filter((h) => {
      // search match
      if (q) {
        const sym = (h.tradingSymbol ?? "").toLowerCase();
        const isin = (h.isin ?? "").toLowerCase();
        if (!sym.includes(q) && !isin.includes(q)) return false;
      }

      // filter chips
      if (filter === "PROFIT") {
        return (h.pnl ?? 0) >= 0;
      }
      if (filter === "LOSS") {
        return (h.pnl ?? 0) < 0;
      }
      return true; // ALL
    });
  }, [holdings, search, filter]);

  const totalValue = filteredHoldings.reduce((s, h) => {
    const last = h.lastPrice ?? 0;
    const qty = h.quantity ?? 0;
    return s + last * qty;
  }, 0);

  const totalPnl = filteredHoldings.reduce((s, h) => s + (h.pnl ?? 0), 0);

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
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 justify-between mb-3">
              <div className="flex-1 min-w-0">
                <HoldingsSearchBar initial={search} onSearch={setSearch} />
              </div>
              <div>
                <FilterChips value={filter} onChange={setFilter} />
              </div>
            </div>

            <h3 className="text-lg font-medium mb-2">Holdings</h3>
            {loading ? (
              <div className="p-6 bg-white border rounded">Loading...</div>
            ) : filteredHoldings.length === 0 ? (
              // if there are no holdings to display (after filters/search) show a lighter empty state:
              holdings.length === 0 ? (
                <EmptyState
                  title="No holdings available"
                  description="Connect a broker or add mock data"
                />
              ) : (
                <div className="p-6 bg-white border rounded text-center text-gray-600">
                  No holdings match your search / filters.
                </div>
              )
            ) : (
              <DataTable<Holding>
                data={filteredHoldings}
                columns={[
                  {
                    header: "Symbol",
                    accessor: (r) => (
                      <div className="font-medium">{r.tradingSymbol}</div>
                    ),
                  },
                  { header: "ISIN", accessor: (r) => r.isin ?? "—" },
                  { header: "Qty", accessor: (r) => r.quantity },
                  {
                    header: "Avg Price",
                    accessor: (r) => (r.averagePrice ?? 0).toFixed(2),
                  },
                  {
                    header: "Last Price",
                    accessor: (r) => (r.lastPrice ?? 0).toFixed(2),
                  },
                  { header: "P&L", accessor: (r) => (r.pnl ?? 0).toFixed(2) },
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
                { header: "ISIN", accessor: (r) => r.isin ?? "—" },
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
