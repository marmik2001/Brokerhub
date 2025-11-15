// src/components/FilterChips.tsx
import React from "react";

export type FilterKey = "ALL" | "PROFIT" | "LOSS" | "ZERO";

const OPTIONS: { key: FilterKey; label: string }[] = [
  { key: "ALL", label: "All" },
  { key: "PROFIT", label: "Profit" },
  { key: "LOSS", label: "Loss" },
];

type Props = {
  value: FilterKey;
  onChange: (k: FilterKey) => void;
};

const FilterChips: React.FC<Props> = ({ value, onChange }) => {
  return (
    <div className="flex gap-2">
      {OPTIONS.map((opt) => {
        const active = opt.key === value;
        return (
          <button
            key={opt.key}
            onClick={() => onChange(opt.key)}
            className={[
              "text-sm px-3 py-1 rounded-full border transition",
              active
                ? "bg-blue-600 text-white border-blue-600"
                : "bg-white text-gray-700 border-gray-200 hover:bg-gray-50",
            ].join(" ")}
            aria-pressed={active}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
};

export default FilterChips;
