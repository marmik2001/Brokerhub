import React, { useMemo } from "react";

type Column<T> = {
  header: string;
  accessor: (row: T) => React.ReactNode;
  key?: string;
};

type DefaultSort<T> = {
  accessor: (row: T) => number;
  direction?: "asc" | "desc";
};

const DataTable = <T,>({
  data,
  columns,
  defaultSort,
}: {
  data: T[];
  columns: Column<T>[];
  /**
   * Optional: default numeric sort. Provide an accessor that returns a number for the column
   * you want sorted. If not provided, rows retain original order.
   */
  defaultSort?: DefaultSort<T>;
}) => {
  const sortedData = useMemo(() => {
    if (!defaultSort) return data;
    const { accessor, direction = "desc" } = defaultSort;

    // create a shallow copy so we don't mutate props
    const copy = [...data];
    copy.sort((a, b) => {
      const va = accessor(a) ?? 0;
      const vb = accessor(b) ?? 0;
      if (va === vb) return 0;
      return direction === "asc" ? va - vb : vb - va;
    });
    return copy;
  }, [data, defaultSort]);

  return (
    <div className="overflow-x-auto bg-white rounded-lg border">
      <table className="min-w-full text-sm">
        <thead className="bg-gray-50">
          <tr>
            {columns.map((c, i) => (
              <th
                key={c.key ?? i}
                className="px-4 py-3 text-left text-xs font-medium text-gray-500"
              >
                {c.header}
              </th>
            ))}
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
                key={rIdx}
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
