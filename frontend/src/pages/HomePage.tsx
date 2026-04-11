import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import {
  type Holding,
  type Position,
  fetchAggregateHoldings,
  fetchAggregatePositions,
} from "../services/portfolioService";
import HoldingsTab from "../components/home/HoldingTab";
import PositionsTab from "../components/home/PositionTab";
import FeedTab from "../components/home/FeedTab";
import { toast } from "react-hot-toast";
import { parseApiError } from "../utils/apiError";

type Section = "holding" | "positions" | "feed";

const HomePage: React.FC = () => {
  const location = useLocation();
  const { currentAccount } = useAuth();
  const accountId = currentAccount?.accountId;
  const sectionFromState = (location.state as { section?: Section } | null)
    ?.section;
  const section: Section = sectionFromState ?? "holding";

  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [partialHoldings, setPartialHoldings] = useState<string[]>([]);
  const [positions, setPositions] = useState<Position[]>([]);
  const [partialPositions, setPartialPositions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (section === "holding") {
      setLoading(true);
      fetchAggregateHoldings(accountId)
        .then((res) => {
          setHoldings(res.full || []);
          setPartialHoldings(res.partial || []);
        })
        .catch((err: unknown) => {
          setHoldings([]);
          setPartialHoldings([]);
          const { message } = parseApiError(err);
          toast.error(message || "Failed to load holdings");
        })
        .finally(() => setLoading(false));
    } else if (section === "positions") {
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
  }, [section, accountId]);

  return (
    <div className="pt-4">
      {section === "holding" && (
        <HoldingsTab
          holdings={holdings}
          partialTickers={partialHoldings}
          loading={loading}
        />
      )}

      {section === "positions" && (
        <PositionsTab
          positions={positions}
          partialTickers={partialPositions}
          loading={loading}
        />
      )}

      {section === "feed" && <FeedTab />}
    </div>
  );
};

export default HomePage;
