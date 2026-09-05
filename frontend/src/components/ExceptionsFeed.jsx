import React from 'react';
import { Sparkles, AlertTriangle, ArrowRight, Loader2, CheckCircle } from 'lucide-react';

export default function ExceptionsFeed({
  items = [],
  onTriggerAiInvestigation,
  isInvestigating = false,
  onInspectItem
}) {
  const exceptions = items.filter((item) => item.status !== 'MATCHED');

  const getStatusColor = (status) => {
    switch (status) {
      case 'AMOUNT_MISMATCH': return '#F59E0B';
      case 'MISSING_SETTLEMENT': return '#EF4444';
      case 'DUPLICATE_TRANSACTION': return '#8B5CF6';
      case 'UNKNOWN_TRANSACTION': return '#64748B';
      default: return '#10B981';
    }
  };

  const formatCurrency = (val, currency = 'INR') => {
    if (val === null || val === undefined) return '—';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency || 'INR',
      maximumFractionDigits: 2,
    }).format(val);
  };

  const investigatedCount = exceptions.filter((e) => !!e.aiInvestigation).length;

  return (
    <div className="table-card">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <h2 style={{ fontSize: '18px', fontWeight: 700, letterSpacing: '-0.02em' }}>
              Exceptions & AI Copilot Feed
            </h2>
            <span style={{
              background: '#FEF3C7',
              color: '#92400E',
              padding: '2px 10px',
              borderRadius: '999px',
              fontSize: '12px',
              fontWeight: 800
            }}>
              {exceptions.length} Anomalies
            </span>
          </div>
          <p style={{ fontSize: '13px', color: '#6B7280', marginTop: '4px' }}>
            {investigatedCount === exceptions.length && exceptions.length > 0
              ? 'All exceptions have been analyzed by Gemini AI.'
              : 'Discrepancies identified by deterministic logic ready for Gemini root-cause diagnosis.'}
          </p>
        </div>

        {/* Action button to trigger AI investigation */}
        {exceptions.length > 0 && (
          <button
            onClick={onTriggerAiInvestigation}
            disabled={isInvestigating}
            style={{
              background: 'linear-gradient(135deg, #10B981, #059669)',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: '999px',
              padding: '10px 22px',
              fontSize: '13px',
              fontWeight: 700,
              cursor: isInvestigating ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              boxShadow: '0 4px 14px rgba(16, 185, 129, 0.3)',
              opacity: isInvestigating ? 0.7 : 1,
              transition: 'all 0.2s ease'
            }}
          >
            {isInvestigating ? (
              <>
                <Loader2 size={16} className="animate-spin" />
                <span>Investigating with Gemini...</span>
              </>
            ) : (
              <>
                <Sparkles size={16} />
                <span>{investigatedCount > 0 ? 'Re-Run AI Investigation' : 'Investigate with Gemini AI'}</span>
              </>
            )}
          </button>
        )}
      </div>

      {exceptions.length === 0 ? (
        <div style={{
          padding: '48px 24px',
          textAlign: 'center',
          background: '#F9FAFB',
          borderRadius: '20px',
          border: '1px solid rgba(0,0,0,0.04)'
        }}>
          <CheckCircle size={36} color="#10B981" style={{ margin: '0 auto 12px' }} />
          <h3 style={{ fontSize: '16px', fontWeight: 700 }}>Zero Exceptions Detected</h3>
          <p style={{ fontSize: '13px', color: '#6B7280', marginTop: '4px' }}>
            All payment gateway transactions match bank settlement reports with 100% accuracy.
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {exceptions.map((item, idx) => {
            const ai = item.aiInvestigation;
            return (
              <div
                key={idx}
                style={{
                  background: '#FFFFFF',
                  borderRadius: '20px',
                  border: '1px solid #E5E7EB',
                  padding: '20px 24px',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.02)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '12px'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <span style={{
                      width: '10px',
                      height: '10px',
                      borderRadius: '50%',
                      backgroundColor: getStatusColor(item.status)
                    }} />
                    <span style={{ fontWeight: 800, fontSize: '15px' }}>
                      {item.transactionId || 'Unknown Txn'}
                    </span>
                    {item.orderId && (
                      <span style={{ fontSize: '12px', color: '#6B7280', background: '#F3F4F6', padding: '2px 8px', borderRadius: '6px' }}>
                        {item.orderId}
                      </span>
                    )}
                    <span style={{ fontSize: '12px', fontWeight: 700, color: getStatusColor(item.status) }}>
                      {item.status.replace('_', ' ')}
                    </span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                    <span style={{ fontSize: '13px', color: '#6B7280' }}>
                      Payment: <strong>{formatCurrency(item.paymentAmount, item.currency)}</strong>
                    </span>
                    <span style={{ fontSize: '13px', color: '#6B7280' }}>
                      Settled: <strong>{formatCurrency(item.settledAmount, item.currency)}</strong>
                    </span>
                    <button
                      onClick={() => onInspectItem(item)}
                      style={{
                        background: '#111827',
                        color: '#FFFFFF',
                        border: 'none',
                        borderRadius: '999px',
                        padding: '6px 14px',
                        fontSize: '12px',
                        fontWeight: 600,
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px'
                      }}
                    >
                      <span>Inspect AI</span>
                      <ArrowRight size={14} />
                    </button>
                  </div>
                </div>

                <div style={{ fontSize: '13px', color: '#4B5563', lineHeight: 1.5 }}>
                  {item.discrepancyReason}
                </div>

                {/* Embedded AI Insight Pill */}
                {ai && (
                  <div style={{
                    background: ai.source === 'GEMINI' ? '#F0FDF4' : '#FFFBEB',
                    border: ai.source === 'GEMINI' ? '1px solid #BBF7D0' : '1px solid #FDE68A',
                    borderRadius: '14px',
                    padding: '14px 16px',
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '12px'
                  }}>
                    <Sparkles
                      size={16}
                      color={ai.source === 'GEMINI' ? '#15803D' : '#B45309'}
                      style={{ marginTop: '2px', flexShrink: 0 }}
                    />
                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                        <strong style={{ fontSize: '13px', color: ai.source === 'GEMINI' ? '#166534' : '#92400E' }}>
                          {ai.source === 'GEMINI' ? '✨ Gemini AI • Live Diagnosis' : '⚙️ Demo / Fallback Analysis'} ({Math.round(ai.confidence * 100)}% Confidence):
                        </strong>
                        <span style={{
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '2px 8px',
                          borderRadius: '999px',
                          background: ai.source === 'GEMINI' ? '#DCFCE7' : '#FEF3C7',
                          color: ai.source === 'GEMINI' ? '#15803D' : '#92400E',
                          border: ai.source === 'GEMINI' ? '1px solid #BBF7D0' : '1px solid #FDE68A'
                        }}>
                          {ai.source === 'GEMINI' ? 'Gemini Live' : 'Fallback Mode'}
                        </span>
                      </div>
                      <p style={{ fontSize: '13px', color: ai.source === 'GEMINI' ? '#14532D' : '#78350F', lineHeight: 1.4 }}>
                        {ai.possibleReason}
                      </p>
                      <div style={{ marginTop: '6px', fontSize: '12px', color: ai.source === 'GEMINI' ? '#15803D' : '#B45309' }}>
                        <strong>Action:</strong> {ai.recommendedAction}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
