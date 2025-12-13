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

const HomePage: React.FC = () => {
  const { currentAccount } = useAuth();
  const accountId = currentAccount?.accountId;

  const [tab, setTab] = useState<"dashboard" | "positions" | "feed">(
    "dashboard"
  );

  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [partialHoldings, setPartialHoldings] = useState<string[]>([]);
  const [positions, setPositions] = useState<Position[]>([]);
  const [partialPositions, setPartialPositions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<FilterKey>("ALL");

  useEffect(() => {
    if (tab === "dashboard") {
      setLoading(true);
      fetchAggregateHoldings(accountId)
        .then((res) => {
          setHoldings(res.full || []);
          setPartialHoldings(res.partial || []);
        })
        .catch(() => {
          setHoldings([]);
          setPartialHoldings([]);
          toast.error("Failed to load holdings");
        })
        .finally(() => setLoading(false));
    } else if (tab === "positions") {
      setLoading(true);
      fetchAggregatePositions(accountId)
        .then((res) => {
          setPositions(res.full || []);
          setPartialPositions(res.partial || []);
        })
        .catch(() => {
          setPositions([]);
          setPartialPositions([]);
        })
        .finally(() => setLoading(false));
    }
  }, [tab, accountId]);

  const filteredHoldings = useMemo(() => {
    const q = (search || "").trim().toLowerCase();
    return holdings.filter((h) => {
      if (q) {
        const sym = (h.tradingSymbol ?? "").toLowerCase();
        const isin = (h.isin ?? "").toLowerCase();
        if (!sym.includes(q) && !isin.includes(q)) return false;
      }

      if (filter === "PROFIT") return (h.pnl ?? 0) > 0;
      if (filter === "LOSS") return (h.pnl ?? 0) < 0;
      return true;
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
          partialTickers={partialHoldings}
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
          partialTickers={partialPositions}
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
