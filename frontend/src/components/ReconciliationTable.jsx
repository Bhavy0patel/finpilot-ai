import React, { useState } from 'react';
import { Sparkles, Search } from 'lucide-react';

export default function ReconciliationTable({ items = [], onInspectItem }) {
  const [activeFilter, setActiveFilter] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');

  const formatCurrency = (val, currency = 'INR') => {
    if (val === null || val === undefined) return '—';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency || 'INR',
      maximumFractionDigits: 2,
    }).format(val);
  };

  const filteredItems = items.filter((item) => {
    // Tab filter
    if (activeFilter === 'exceptions' && item.status === 'MATCHED') return false;
    if (activeFilter === 'matched' && item.status !== 'MATCHED') return false;

    // Search filter
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      const txnMatch = item.transactionId?.toLowerCase().includes(term);
      const orderMatch = item.orderId?.toLowerCase().includes(term);
      const statusMatch = item.status?.toLowerCase().includes(term);
      return txnMatch || orderMatch || statusMatch;
    }
    return true;
  });

  const getStatusBadge = (status) => {
    switch (status) {
      case 'MATCHED':
        return <span className="badge badge-matched">MATCHED</span>;
      case 'AMOUNT_MISMATCH':
        return <span className="badge badge-mismatch">AMOUNT MISMATCH</span>;
      case 'MISSING_SETTLEMENT':
        return <span className="badge badge-missing">MISSING SETTLEMENT</span>;
      case 'DUPLICATE_TRANSACTION':
        return <span className="badge badge-duplicate">DUPLICATE</span>;
      case 'UNKNOWN_TRANSACTION':
        return <span className="badge badge-unknown">UNKNOWN</span>;
      default:
        return <span className="badge">{status}</span>;
    }
  };

  const matchedCount = items.filter(i => i.status === 'MATCHED').length;
  const exceptionsCount = items.length - matchedCount;

  return (
    <div className="table-card">
      <div className="table-header-row">
        <div>
          <h2 style={{ fontSize: '18px', fontWeight: 700, letterSpacing: '-0.02em' }}>
            Reconciliation Records Ledger
          </h2>
          <p style={{ fontSize: '13px', color: '#6B7280', marginTop: '4px' }}>
            Complete audit trail of matched pairs and discrepancy exceptions.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {/* Search box */}
          <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
            <Search size={16} color="#9CA3AF" style={{ position: 'absolute', left: '12px' }} />
            <input
              type="text"
              placeholder="Search ID, Order, Status..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{
                padding: '8px 14px 8px 34px',
                borderRadius: '999px',
                border: '1px solid #E5E7EB',
                fontSize: '13px',
                background: '#FAFAFA',
                outline: 'none',
                width: '210px'
              }}
            />
          </div>

          {/* Filter Tabs */}
          <div className="table-filter-pills">
            <button
              className={`filter-pill ${activeFilter === 'all' ? 'active' : ''}`}
              onClick={() => setActiveFilter('all')}
            >
              All ({items.length})
            </button>
            <button
              className={`filter-pill ${activeFilter === 'exceptions' ? 'active' : ''}`}
              onClick={() => setActiveFilter('exceptions')}
            >
              Exceptions ({exceptionsCount})
            </button>
            <button
              className={`filter-pill ${activeFilter === 'matched' ? 'active' : ''}`}
              onClick={() => setActiveFilter('matched')}
            >
              Matched ({matchedCount})
            </button>
          </div>
        </div>
      </div>

      {filteredItems.length === 0 ? (
        <div style={{ padding: '48px', textAlign: 'center', color: '#6B7280', fontSize: '14px' }}>
          No records match the current filter.
        </div>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <table className="records-table">
            <thead>
              <tr>
                <th>Transaction ID</th>
                <th>Order ID</th>
                <th>Payment Amt</th>
                <th>Settled Amt</th>
                <th>Difference</th>
                <th>Status</th>
                <th>AI Insight</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item, idx) => {
                const isException = item.status !== 'MATCHED';
                const hasAi = !!item.aiInvestigation;

                return (
                  <tr key={idx}>
                    <td>
                      <span style={{ fontWeight: 700, color: '#111827' }}>
                        {item.transactionId || '—'}
                      </span>
                    </td>
                    <td>
                      <span style={{ color: '#4B5563', fontSize: '13px' }}>
                        {item.orderId || '—'}
                      </span>
                    </td>
                    <td>{formatCurrency(item.paymentAmount, item.currency)}</td>
                    <td>{formatCurrency(item.settledAmount, item.currency)}</td>
                    <td>
                      <span style={{
                        color: (item.differenceAmount && item.differenceAmount !== 0) ? '#DC2626' : '#6B7280',
                        fontWeight: (item.differenceAmount && item.differenceAmount !== 0) ? 700 : 500
                      }}>
                        {formatCurrency(item.differenceAmount, item.currency)}
                      </span>
                    </td>
                    <td>{getStatusBadge(item.status)}</td>
                    <td>
                      {hasAi ? (
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px',
                          fontSize: '11px',
                          fontWeight: 700,
                          background: item.aiInvestigation.source === 'GEMINI' ? '#ECFDF5' : '#FFFBEB',
                          color: item.aiInvestigation.source === 'GEMINI' ? '#065F46' : '#92400E',
                          padding: '3px 8px',
                          borderRadius: '999px',
                          border: item.aiInvestigation.source === 'GEMINI' ? '1px solid #A7F3D0' : '1px solid #FDE68A'
                        }}>
                          <Sparkles size={12} color={item.aiInvestigation.source === 'GEMINI' ? '#059669' : '#D97706'} />
                          {item.aiInvestigation.source === 'GEMINI' ? 'Gemini Live' : 'Fallback'} ({Math.round(item.aiInvestigation.confidence * 100)}%)
                        </span>
                      ) : isException ? (
                        <span style={{ fontSize: '12px', color: '#9CA3AF' }}>Pending Analysis</span>
                      ) : (
                        <span style={{ fontSize: '12px', color: '#9CA3AF' }}>Reconciled</span>
                      )}
                    </td>
                    <td>
                      {isException ? (
                        <button
                          className="btn-inspect"
                          onClick={() => onInspectItem(item)}
                        >
                          Inspect
                        </button>
                      ) : (
                        <span style={{ color: '#9CA3AF', fontSize: '12px' }}>—</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
