import React from "react";

interface Props {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
}

/**
 * Simple search input used on the holdings dashboard.
 * Pure presentational component — parent handles debounce and filtering.
 */
const HoldingsSearchBar: React.FC<Props> = ({
  value,
  onChange,
  placeholder,
}) => {
  return (
    <div className="w-full">
      <label className="sr-only">Search holdings</label>
      <div className="relative">
        <input
          type="search"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder ?? "Search by symbol or ISIN"}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          aria-label="Search holdings"
        />
      </div>
    </div>
  );
};

export default HoldingsSearchBar;
