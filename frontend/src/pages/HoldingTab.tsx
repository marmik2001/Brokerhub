import React, { useMemo, useState } from "react";
import StatCard from "../components/StatCard";
import DataTable from "../components/DataTable";
import EmptyState from "../components/EmptyState";
import SearchBar from "../components/SearchBar";
import FilterChips, { type FilterKey } from "../components/FilterChips";
import TableValueCell from "../components/TableValueCell";
import { type Holding } from "../services/portfolioService";

type Props = {
  holdings: Holding[];
  partialTickers: string[];
  loading: boolean;
};

const HOLDINGS_FILTERS: FilterKey[] = ["ALL", "PROFIT", "LOSS"];

const HoldingsTab: React.FC<Props> = ({
  holdings,
  partialTickers,
  loading,
}) => {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<FilterKey>("ALL");

  const filtered = useMemo(() => {
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

  const totalValue = useMemo(() => {
    return filtered.reduce((s, h) => {
      const last = h.lastPrice ?? 0;
      const qty = h.quantity ?? 0;
      return s + last * qty;
    }, 0);
  }, [filtered]);

  const totalPnl = useMemo(() => {
    return filtered.reduce((s, h) => s + (h.pnl ?? 0), 0);
  }, [filtered]);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <StatCard
          title="Total Value"
          value={`₹ ${totalValue.toLocaleString("en-IN")}`}
        />
        <StatCard
          title={`P&L (${
            totalPnl >= 0 ? "Unrealised gain" : "Unrealised loss"
          })`}
          value={
            <TableValueCell
              value={totalPnl}
              currency={true}
              colorize={true}
              parenNegative={true}
            />
          }
        />
      </div>

      <div className="space-y-3">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 justify-between">
          <div className="flex-1 min-w-0">
            <SearchBar
              initial={search}
              onSearch={setSearch}
              placeholder="Search holdings by symbol or ISIN"
              ariaLabel="Search holdings"
            />
            {partialTickers.length > 0 && (
              <div className="text-sm text-gray-500 mt-1">
                Held privately by other group members:{" "}
                {partialTickers.join(", ")}
              </div>
            )}
          </div>
          <div>
            <FilterChips
              value={filter}
              onChange={setFilter}
              allowedKeys={HOLDINGS_FILTERS}
            />
          </div>
        </div>

        <h3 className="text-lg font-medium">Holdings</h3>

        {loading ? (
          <div className="p-6 bg-white border rounded">Loading...</div>
        ) : filtered.length === 0 ? (
          <EmptyState
            title="No holdings available"
            description="Connect a broker!"
          />
        ) : (
          <DataTable<Holding>
            data={filtered}
            columns={[
              {
                header: "Symbol",
                accessor: (r) => (
                  <div className="font-medium">{r.tradingSymbol}</div>
                ),
                sortValue: (r) => r.tradingSymbol,
              },
              {
                header: "ISIN",
                accessor: (r) => r.isin ?? "—",
                sortValue: (r) => r.isin ?? "",
              },
              {
                header: "Qty",
                accessor: (r) => r.quantity,
                sortValue: (r) => r.quantity ?? 0,
              },
              {
                header: "Avg Price",
                accessor: (r) => (
                  <TableValueCell value={r.averagePrice} currency />
                ),
                sortValue: (r) => r.averagePrice ?? 0,
              },
              {
                header: "Last Price",
                accessor: (r) => (
                  <TableValueCell value={r.lastPrice} currency />
                ),
                sortValue: (r) => r.lastPrice ?? 0,
              },
              {
                header: "P&L",
                accessor: (r) => (
                  <TableValueCell
                    value={r.pnl}
                    currency
                    colorize
                    parenNegative
                  />
                ),
                sortValue: (r) => r.pnl ?? 0,
              },
            ]}
          />
        )}
      </div>
    </div>
  );
};

export default HoldingsTab;
