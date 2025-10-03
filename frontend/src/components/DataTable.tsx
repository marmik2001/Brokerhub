// src/components/DataTable.tsx
import React from "react";

type Column<T> = {
  header: string;
  accessor: (row: T) => React.ReactNode;
  key?: string;
};

const DataTable = <T,>({
  data,
  columns,
}: {
  data: T[];
  columns: Column<T>[];
}) => {
  return (
    <div className="overflow-x-auto bg-white rounded-lg border">
      <table className="min-w-full text-sm">
        <thead className="bg-gray-50">
          <tr>
            {columns.map((c, i) => (
              <th
                key={i}
                className="px-4 py-3 text-left text-xs font-medium text-gray-500"
              >
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                className="px-4 py-8 text-center text-gray-500"
              >
                No rows
              </td>
            </tr>
          ) : (
            data.map((row, rIdx) => (
              <tr
                key={rIdx}
                className={rIdx % 2 === 0 ? "bg-white" : "bg-gray-50"}
              >
                {columns.map((c, cIdx) => (
                  <td key={cIdx} className="px-4 py-3 align-top text-gray-700">
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
