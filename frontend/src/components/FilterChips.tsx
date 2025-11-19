import React from "react";

/**
 * Allowed filter keys used across tabs.
 * ZERO removed per latest requirement.
 */
export type FilterKey = "ALL" | "PROFIT" | "LOSS" | "LONG" | "SHORT";

/** Default options (used when allowedKeys not provided) */
const DEFAULT_OPTIONS: { key: FilterKey; label: string }[] = [
  { key: "ALL", label: "All" },
  { key: "PROFIT", label: "Profit" },
  { key: "LOSS", label: "Loss" },
  { key: "LONG", label: "Long" },
  { key: "SHORT", label: "Short" },
];

type Props = {
  value: FilterKey;
  onChange: (k: FilterKey) => void;
  /**
   * Optionally supply a subset of allowed filter keys to display.
   * If omitted, DEFAULT_OPTIONS is used.
   */
  allowedKeys?: FilterKey[];
};

const FilterChips: React.FC<Props> = ({ value, onChange, allowedKeys }) => {
  const opts = (allowedKeys ?? DEFAULT_OPTIONS.map((o) => o.key)).map((k) =>
    DEFAULT_OPTIONS.find((o) => o.key === k)
  ) as { key: FilterKey; label: string }[];

  return (
    <div className="flex gap-2 flex-wrap">
      {opts.map((opt) => {
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
            aria-label={`Filter ${opt.label}`}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
};

export default FilterChips;
