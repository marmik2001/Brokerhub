import React from "react";

type Props = {
  value?: number | null;
  /**
   * If true, renders a currency prefix (₹) using en-IN locale formatting.
   */
  currency?: boolean;
  /**
   * If true, apply positive/negative color styling (green/red).
   * Use this for P&L cells. Default false.
   */
  colorize?: boolean;
  /**
   * Wrap negative numbers in parentheses when true, e.g. (₹ 1,000.00)
   */
  parenNegative?: boolean;
  /**
   * Optional fallback text when value is null/undefined.
   */
  fallback?: string;
  /**
   * Optional className to pass to the container.
   */
  className?: string;
};

const formatNumber = (v: number, currency: boolean) => {
  try {
    if (currency) {
      return (
        "₹ " +
        v.toLocaleString("en-IN", {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        })
      );
    }
    return v.toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  } catch {
    return v.toFixed(2);
  }
};

const applyParenIfNegative = (
  v: number,
  text: string,
  parenNegative?: boolean
) => {
  if (v < 0 && parenNegative) {
    const absText = text.replace("-", "").trim();
    return `(${absText})`;
  }
  return text;
};

/**
 * TableValueCell
 * - formats numbers consistently
 * - optional currency prefix
 * - optional colorization for positive/negative values
 * - optional parentheses for negative numbers
 */
const TableValueCell: React.FC<Props> = ({
  value,
  currency = false,
  colorize = false,
  parenNegative = false,
  fallback = "—",
  className = "",
}) => {
  if (value === null || value === undefined) {
    return <span className={className}>{fallback}</span>;
  }

  const raw = value;
  const formatted = formatNumber(raw, currency);
  const text = applyParenIfNegative(raw, formatted, parenNegative);

  let colorClass = "text-gray-700";
  if (colorize) {
    if (raw > 0) colorClass = "text-green-700";
    else if (raw < 0) colorClass = "text-red-700";
    else colorClass = "text-gray-600";
  }

  return <span className={`${colorClass} ${className}`}>{text}</span>;
};

export default TableValueCell;
