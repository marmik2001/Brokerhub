import React, { useMemo } from "react";
import DataTable from "../components/DataTable";
import EmptyState from "../components/EmptyState";
import SearchBar from "../components/SearchBar";
import FilterChips, { type FilterKey } from "../components/FilterChips";
import TableValueCell from "../components/TableValueCell";
import StatCard from "../components/StatCard";
import { type Position } from "../services/portfolioService";

type Props = {
  positions: Position[]; // raw positions from backend
  loading: boolean;
  search: string;
  onSearch: (s: string) => void;
  filter: FilterKey;
  onFilterChange: (f: FilterKey) => void;
};

const POSITIONS_FILTERS: FilterKey[] = [
  "ALL",
  "PROFIT",
  "LOSS",
  "LONG",
  "SHORT",
];

const PositionsTab: React.FC<Props> = ({
  positions,
  loading,
  search,
  onSearch,
  filter,
  onFilterChange,
}) => {
  // client-side filtering for positions
  const filtered = useMemo(() => {
    const q = (search || "").trim().toLowerCase();
    return positions.filter((p) => {
      if (q) {
        const sym = (p.tradingSymbol ?? "").toLowerCase();
        if (!sym.includes(q)) return false;
      }

      if (filter === "PROFIT") return (p.pnl ?? 0) > 0;
      if (filter === "LOSS") return (p.pnl ?? 0) < 0;
      if (filter === "LONG") return (p.quantity ?? 0) > 0;
      if (filter === "SHORT") return (p.quantity ?? 0) < 0;
      return true;
    });
  }, [positions, search, filter]);

  // totals for stat cards (computed from filtered set)
  const totalValue = useMemo(() => {
    return filtered.reduce((s, p) => {
      const last = p.lastPrice ?? p.averagePrice ?? 0;
      const qty = p.quantity ?? 0;
      return s + last * qty;
    }, 0);
  }, [filtered]);

  const totalPnl = useMemo(() => {
    return filtered.reduce((s, p) => s + (p.pnl ?? 0), 0);
  }, [filtered]);

  return (
    <>
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-3">
        <StatCard
          title="Total Position Value"
          value={`₹ ${totalValue.toLocaleString("en-IN")}`}
        />
        <StatCard
          title={`P&L (${totalPnl >= 0 ? "Net gain" : "Net loss"})`}
          value={
            <TableValueCell
              value={totalPnl}
              currency={true}
              colorize={true}
              parenNegative={true}
            />
          }
        />
        <div className="p-2" /> {/* placeholder to keep grid consistent */}
      </div>

      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 justify-between mb-3">
        <div className="flex-1 min-w-0">
          <SearchBar
            initial={search}
            onSearch={onSearch}
            placeholder="Search positions by symbol or ISIN"
            ariaLabel="Search positions"
          />
        </div>
        <div>
          <FilterChips
            value={filter}
            onChange={onFilterChange}
            allowedKeys={POSITIONS_FILTERS}
          />
        </div>
      </div>

      <h3 className="text-lg font-medium">Positions (Today)</h3>

      {loading ? (
        <div className="p-6 bg-white border rounded">Loading...</div>
      ) : filtered.length === 0 ? (
        <EmptyState title="No positions" />
      ) : (
        <DataTable<Position>
          data={filtered}
          defaultSort={{
            accessor: (r) => {
              const last = r.lastPrice ?? r.averagePrice ?? 0;
              return last * (r.quantity ?? 0);
            },
            direction: "desc",
          }}
          columns={[
            { header: "Symbol", accessor: (r) => r.tradingSymbol },
            { header: "Qty", accessor: (r) => r.quantity },
            {
              header: "Avg Price",
              accessor: (r) => (
                <TableValueCell value={r.averagePrice} currency={true} />
              ),
            },
            {
              header: "Last Price",
              accessor: (r) => (
                <TableValueCell value={r.lastPrice} currency={true} />
              ),
            },
            {
              header: "Value",
              accessor: (r) => {
                const last = r.lastPrice ?? r.averagePrice ?? 0;
                return (
                  <TableValueCell
                    value={last * (r.quantity ?? 0)}
                    currency={true}
                  />
                );
              },
            },
            {
              header: "P&L",
              accessor: (r) => (
                <TableValueCell
                  value={r.pnl}
                  currency={true}
                  colorize={true}
                  parenNegative={true}
                />
              ),
            },
          ]}
        />
      )}
    </>
  );
};

export default PositionsTab;
