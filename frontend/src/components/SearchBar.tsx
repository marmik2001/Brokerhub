import React, { useEffect, useState } from "react";
import useDebounce from "../hooks/useDebounce";

type Props = {
  initial?: string;
  onSearch: (term: string) => void;
  placeholder?: string;
  ariaLabel?: string;
};

const SearchBar: React.FC<Props> = ({
  initial = "",
  onSearch,
  placeholder = "Search by symbol or ISIN",
  ariaLabel = "Search",
}) => {
  const [q, setQ] = useState(initial);
  const debounced = useDebounce(q, 300);

  useEffect(() => {
    onSearch(debounced.trim());
  }, [debounced, onSearch]);

  return (
    <div className="flex items-center gap-3">
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder={placeholder}
        className="flex-1 border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
        aria-label={ariaLabel}
      />
      {q && (
        <button
          onClick={() => setQ("")}
          className="text-sm text-gray-600 hover:text-gray-800"
          aria-label="Clear search"
        >
          Clear
        </button>
      )}
    </div>
  );
};

export default SearchBar;
