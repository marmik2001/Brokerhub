import React from "react";
import SearchBar from "../SearchBar";
import FilterChips, { type FilterKey } from "../FilterChips";

type Props = {
  stats: React.ReactNode;
  title: string;
  search: string;
  onSearch: (s: string) => void;
  searchPlaceholder: string;
  searchAriaLabel: string;
  filter: FilterKey;
  onFilterChange: (f: FilterKey) => void;
  allowedFilters: FilterKey[];
  partialTickers: string[];
  children: React.ReactNode;
};

const PortfolioTabLayout: React.FC<Props> = ({
  stats,
  title,
  search,
  onSearch,
  searchPlaceholder,
  searchAriaLabel,
  filter,
  onFilterChange,
  allowedFilters,
  partialTickers,
  children,
}) => {
  return (
    <div className="space-y-6">
      {stats}

      <div className="space-y-3">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 justify-between">
          <div className="flex-1 min-w-0">
            <SearchBar
              initial={search}
              onSearch={onSearch}
              placeholder={searchPlaceholder}
              ariaLabel={searchAriaLabel}
            />
            {partialTickers.length > 0 && (
              <div className="text-sm text-gray-500 mt-1">
                Held privately by other group members: {partialTickers.join(", ")}
              </div>
            )}
          </div>
          <div>
            <FilterChips
              value={filter}
              onChange={onFilterChange}
              allowedKeys={allowedFilters}
            />
          </div>
        </div>

        <h3 className="text-lg font-medium">{title}</h3>
        {children}
      </div>
    </div>
  );
};

export default PortfolioTabLayout;
