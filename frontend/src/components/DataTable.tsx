import React, { useMemo, useRef, useState } from "react";

type Column<T> = {
  header: string;
  accessor: (row: T) => React.ReactNode;
  key?: string;
  sortValue?: (row: T) => string | number;
  disableSort?: boolean;
};

const DataTable = <T,>({
  data,
  columns,
}: {
  data: T[];
  columns: Column<T>[];
}) => {
  const [sort, setSort] = useState<{
    index: number;
    direction: "asc" | "desc";
  } | null>(null);
  const rowIdsRef = useRef(new WeakMap<object, string>());
  const rowIdCounterRef = useRef(0);

  const getRowKey = (row: T) => {
    if (typeof row === "object" && row !== null) {
      const existing = rowIdsRef.current.get(row as object);
      if (existing) return existing;
      rowIdCounterRef.current += 1;
      const created = `row-${rowIdCounterRef.current}`;
      rowIdsRef.current.set(row as object, created);
      return created;
    }
    return `${typeof row}:${String(row)}`;
  };

  const sortedData = useMemo(() => {
    if (!sort) return data;

    const col = columns[sort.index];
    if (col.disableSort || !col.sortValue) return data;

    const copy = [...data];

    copy.sort((a, b) => {
      const va = col.sortValue!(a);
      const vb = col.sortValue!(b);

      if (va == null && vb == null) return 0;
      if (va == null) return 1;
      if (vb == null) return -1;

      if (typeof va === "number" && typeof vb === "number") {
        return sort.direction === "asc" ? va - vb : vb - va;
      }

      return sort.direction === "asc"
        ? String(va).localeCompare(String(vb))
        : String(vb).localeCompare(String(va));
    });

    return copy;
  }, [data, sort, columns]);

  return (
    <div className="overflow-x-auto bg-white rounded-lg border">
      <table className="min-w-full text-sm">
        <thead className="bg-gray-50">
          <tr>
            {columns.map((c, i) => {
              const sortable = !c.disableSort && !!c.sortValue;
              const active = sort?.index === i;

              return (
                <th
                  key={c.key ?? i}
                  onClick={() => {
                    if (!sortable) return;

                    setSort((prev) => {
                      if (prev?.index === i) {
                        return {
                          index: i,
                          direction: prev.direction === "asc" ? "desc" : "asc",
                        };
                      }
                      return { index: i, direction: "desc" };
                    });
                  }}
                  className={[
                    "px-4 py-3 text-left text-xs font-medium text-gray-500",
                    sortable && "cursor-pointer select-none",
                  ].join(" ")}
                >
                  <span className="flex items-center gap-1">
                    {c.header}
                    {active && (
                      <span className="text-gray-400">
                        {sort!.direction === "asc" ? "▲" : "▼"}
                      </span>
                    )}
                  </span>
                </th>
              );
            })}
          </tr>
        </thead>

        <tbody>
          {sortedData.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                className="px-4 py-8 text-center text-gray-500"
              >
                No rows
              </td>
            </tr>
          ) : (
            sortedData.map((row, rIdx) => (
              <tr
                key={getRowKey(row)}
                className={rIdx % 2 === 0 ? "bg-white" : "bg-gray-50"}
              >
                {columns.map((c, cIdx) => (
                  <td
                    key={c.key ?? cIdx}
                    className="px-4 py-3 align-top text-gray-700"
                  >
                    {c.accessor(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};

export default DataTable;
