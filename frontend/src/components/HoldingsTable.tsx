// src/components/HoldingsTable.tsx
import type { Holding } from "../types/portfolio";

interface HoldingsTableProps {
  holdings: Holding[];
}

const HoldingsTable = ({ holdings }: HoldingsTableProps) => {
  if (holdings.length === 0) {
    return (
      <div className="text-center text-gray-600 mt-10">
        No holdings available.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto bg-white shadow rounded-xl">
      <table className="min-w-full border border-gray-200 rounded-xl">
        <thead className="bg-gray-100">
          <tr>
            <th className="px-4 py-2 text-left text-sm font-semibold text-gray-700">
              Symbol
            </th>
            <th className="px-4 py-2 text-left text-sm font-semibold text-gray-700">
              Quantity
            </th>
            <th className="px-4 py-2 text-left text-sm font-semibold text-gray-700">
              Avg. Price
            </th>
            <th className="px-4 py-2 text-left text-sm font-semibold text-gray-700">
              LTP
            </th>
            <th className="px-4 py-2 text-left text-sm font-semibold text-gray-700">
              P&L
            </th>
          </tr>
        </thead>
        <tbody>
          {holdings.map((h, idx) => (
            <tr
              key={idx}
              className="border-t border-gray-200 hover:bg-gray-50 transition"
            >
              <td className="px-4 py-2 text-sm">{h.symbol}</td>
              <td className="px-4 py-2 text-sm">{h.quantity}</td>
              <td className="px-4 py-2 text-sm">₹{h.avgPrice.toFixed(2)}</td>
              <td className="px-4 py-2 text-sm">₹{h.ltp.toFixed(2)}</td>
              <td
                className={`px-4 py-2 text-sm font-medium ${
                  h.pnl >= 0 ? "text-green-600" : "text-red-600"
                }`}
              >
                ₹{h.pnl.toFixed(2)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default HoldingsTable;
