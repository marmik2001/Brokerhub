import React, { useEffect, useMemo, useState } from "react";
import { useAuth } from "../contexts/AuthContext";
import {
  type Holding,
  type Position,
  fetchAggregateHoldings,
  fetchAggregatePositions,
} from "../services/portfolioService";
import HoldingsTab from "./HoldingTab";
import PositionsTab from "./PositionTab";
import FeedTab from "./FeedTab";
import { toast } from "react-hot-toast";
import { type FilterKey } from "../components/FilterChips";

/**
 * HomePage coordinates fetching and top-level tab selection.
 * The actual UI for each tab is delegated to subcomponents.
 */

const HomePage: React.FC = () => {
  const { currentAccount } = useAuth();
  const accountId = currentAccount?.accountId;

  const [tab, setTab] = useState<"dashboard" | "positions" | "feed">(
    "dashboard"
  );

  // raw data states
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [positions, setPositions] = useState<Position[]>([]);
  const [loading, setLoading] = useState(false);

  // search & filter state (shared between tabs)
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<FilterKey>("ALL");

  // fetch on tab change
  useEffect(() => {
    if (tab === "dashboard") {
      setLoading(true);
      fetchAggregateHoldings(accountId)
        .then((h) => setHoldings(h || []))
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

  // derived filtered holdings using search + filter (so parent can compute totals)
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
        return (h.pnl ?? 0) > 0;
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
        <HoldingsTab
          holdings={filteredHoldings}
          loading={loading}
          search={search}
          onSearch={setSearch}
          filter={filter}
          onFilterChange={setFilter}
          totalValue={totalValue}
          totalPnl={totalPnl}
          currentAccount={currentAccount}
        />
      )}

      {tab === "positions" && (
        <PositionsTab
          positions={positions}
          loading={loading}
          search={search}
          onSearch={setSearch}
          filter={filter}
          onFilterChange={setFilter}
        />
      )}

      {tab === "feed" && <FeedTab />}
    </div>
  );
};

export default HomePage;
