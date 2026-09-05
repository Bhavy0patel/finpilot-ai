import React from 'react';
import { ArrowUpRight, CheckCircle2, AlertCircle, Percent, Receipt, Landmark } from 'lucide-react';

export default function MetricsCards({ batch }) {
  if (!batch) return null;

  const totalExceptions = (batch.amountMismatchCount || 0) +
    (batch.missingSettlementCount || 0) +
    (batch.duplicateCount || 0) +
    (batch.unknownCount || 0);

  const matchRate = (batch.totalPayments > 0)
    ? Math.round(((batch.matchedCount || 0) / batch.totalPayments) * 100)
    : 0;

  const formatCurrency = (val) =>
    new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(val || 0);

  const metrics = [
    {
      title: 'Total Payments',
      value: batch.totalPayments || 0,
      sub: formatCurrency(batch.totalPaymentValue),
      icon: Receipt,
      color: '#3B82F6',
    },
    {
      title: 'Total Settlements',
      value: batch.totalSettlements || 0,
      sub: formatCurrency(batch.totalSettledValue),
      icon: Landmark,
      color: '#6366F1',
    },
    {
      title: 'Matched',
      value: batch.matchedCount || 0,
      sub: 'Clean 1:1 Pairs',
      icon: CheckCircle2,
      color: '#10B981',
    },
    {
      title: 'Match Rate',
      value: `${matchRate}%`,
      sub: `${batch.matchedCount} of ${batch.totalPayments} reconciled`,
      icon: Percent,
      color: '#059669',
    },
    {
      title: 'Exceptions',
      value: totalExceptions,
      sub: 'Action Required',
      icon: AlertCircle,
      color: '#F97316',
    },
    {
      title: 'Discrepancy ₹',
      value: formatCurrency(batch.totalDiscrepancyValue),
      sub: 'Unresolved Value',
      icon: ArrowUpRight,
      color: '#EF4444',
    },
  ];

  return (
    <div className="metrics-grid">
      {metrics.map((m, i) => {
        const Icon = m.icon;
        return (
          <div key={i} className="metric-card">
            <div>
              <div className="metric-card-top">
                <span className="metric-card-title">{m.title}</span>
                <div className="metric-card-icon" style={{ color: m.color }}>
                  <Icon size={18} />
                </div>
              </div>
              <div className="metric-card-value">{m.value}</div>
            </div>
            <div className="metric-card-sub">{m.sub}</div>
          </div>
        );
      })}
    </div>
  );
}
