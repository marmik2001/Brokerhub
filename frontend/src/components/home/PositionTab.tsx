import React, { useMemo, useState } from "react";
import DataTable from "../DataTable";
import EmptyState from "../EmptyState";
import { type FilterKey } from "../FilterChips";
import TableValueCell from "../TableValueCell";
import StatCard from "../StatCard";
import { type Position } from "../../services/portfolioService";
import PortfolioTabLayout from "./PortfolioTabLayout";

type Props = {
  positions: Position[];
  partialTickers: string[];
  loading: boolean;
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
  partialTickers,
  loading,
}) => {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<FilterKey>("ALL");

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
    <PortfolioTabLayout
      title="Positions"
      search={search}
      onSearch={setSearch}
      searchPlaceholder="Search positions by symbol or ISIN"
      searchAriaLabel="Search positions"
      filter={filter}
      onFilterChange={setFilter}
      allowedFilters={POSITIONS_FILTERS}
      partialTickers={partialTickers}
      stats={
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
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
        </div>
      }
    >
        {loading ? (
          <div className="p-6 bg-white border rounded">Loading...</div>
        ) : filtered.length === 0 ? (
          <EmptyState title="No positions available" description="Connect a broker!" />
        ) : (
          <DataTable<Position>
            data={filtered}
            columns={[
              {
                header: "Symbol",
                accessor: (r) => r.tradingSymbol,
                sortValue: (r) => r.tradingSymbol,
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
                accessor: (r) => <TableValueCell value={r.lastPrice} currency />,
                sortValue: (r) => r.lastPrice ?? 0,
              },
              {
                header: "Value",
                accessor: (r) => {
                  const last = r.lastPrice ?? r.averagePrice ?? 0;
                  return (
                    <TableValueCell value={last * (r.quantity ?? 0)} currency />
                  );
                },
                sortValue: (r) =>
                  (r.lastPrice ?? r.averagePrice ?? 0) * (r.quantity ?? 0),
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
    </PortfolioTabLayout>
  );
};

export default PositionsTab;
