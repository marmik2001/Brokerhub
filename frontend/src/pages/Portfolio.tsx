// src/pages/Portfolio.tsx
import { useEffect, useState } from "react";
import KITE_API from "../api";
import HoldingsTable from "../components/HoldingsTable";
import type { Holding } from "../types/portfolio";

const Portfolio = () => {
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchHoldings = async () => {
      try {
        const response = await KITE_API.get("/holdings");

        // Transform backend response → Holding[]
        const transformed: Holding[] = response.data.map((item: any) => ({
          symbol: item.tradingSymbol,
          quantity: item.quantity,
          avgPrice: item.averagePrice,
          ltp: item.lastPrice,
          pnl: item.pnl,
        }));

        setHoldings(transformed);
      } catch (err) {
        setError("Failed to load holdings");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchHoldings();
  }, []);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center text-gray-600">
        Loading portfolio...
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-screen items-center justify-center text-red-600">
        {error}
      </div>
    );
  }

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold mb-4">My Portfolio</h1>
      <HoldingsTable holdings={holdings} />
    </div>
  );
};

export default Portfolio;
