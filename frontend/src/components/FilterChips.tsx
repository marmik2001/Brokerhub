import React from "react";

interface Chip {
  id: string;
  label: string;
  active?: boolean;
}

interface Props {
  chips: Chip[];
  onToggle: (id: string) => void;
}

/**
 * Small UI-only filter chips component.
 * Parent manages what chips mean (e.g., "All", "Equity", "MF", etc.).
 */
const FilterChips: React.FC<Props> = ({ chips, onToggle }) => {
  return (
    <div className="flex flex-wrap gap-2">
      {chips.map((c) => (
        <button
          key={c.id}
          onClick={() => onToggle(c.id)}
          className={[
            "text-sm px-3 py-1 rounded-full border transition-colors",
            c.active
              ? "bg-blue-600 text-white border-blue-600"
              : "bg-white text-gray-700 border-gray-200 hover:bg-gray-50",
          ].join(" ")}
          aria-pressed={c.active}
        >
          {c.label}
        </button>
      ))}
    </div>
  );
};

export default FilterChips;
