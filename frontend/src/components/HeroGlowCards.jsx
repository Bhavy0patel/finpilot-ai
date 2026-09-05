import React from 'react';
import { Activity, ShieldAlert, Sparkles } from 'lucide-react';

export default function HeroGlowCards({ batch }) {
  if (!batch) return null;

  const totalProcessed = (batch.totalPayments || 0) + (batch.unknownCount || 0);
  const matchRate = totalProcessed > 0
    ? Math.round(((batch.matchedCount || 0) / (batch.totalPayments || 1)) * 100)
    : 0;

  const formattedDiscrepancy = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(batch.totalDiscrepancyValue || 0);

  const formattedSettled = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(batch.totalSettledValue || 0);

  const totalExceptions = (batch.amountMismatchCount || 0) +
    (batch.missingSettlementCount || 0) +
    (batch.duplicateCount || 0) +
    (batch.unknownCount || 0);

  return (
    <div className="hero-glow-grid">
      {/* 1. Autonomous Reconciliation Health Card (Superpower Emerald Glow) */}
      <div className="glow-card glow-card-green">
        <div className="glow-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="glow-pill-label">Autonomous Health</span>
          </div>
          <span className="glow-badge-tag">ON TRACK</span>
        </div>

        <div className="glow-body">
          <div className="glow-metric-big">{matchRate}%</div>
          <div className="glow-metric-sub">
            {batch.matchedCount} of {batch.totalPayments} Payments Matched Cleanly
          </div>

          {/* Stylized Ledger Dot Matrix */}
          <div className="dot-matrix-indicator">
            {[...Array(12)].map((_, i) => (
              <div
                key={i}
                className={`dot-matrix-bar ${i < Math.round((matchRate / 100) * 12) ? 'active' : ''}`}
                style={{ height: `${12 + (i % 3) * 6}px` }}
              />
            ))}
          </div>
        </div>

        <div className="glow-footer">
          <span>Settled Volume: <strong>{formattedSettled}</strong></span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Sparkles size={14} /> Deterministic Sync Active
          </span>
        </div>
      </div>

      {/* 2. Discrepancy Risk & Exposure Card (Superpower Coral Sunset Glow) */}
      <div className="glow-card glow-card-coral">
        <div className="glow-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="glow-pill-label">Discrepancy Exposure</span>
          </div>
          <span className="glow-badge-tag" style={{ background: '#FFFFFF', color: '#EA580C' }}>
            {totalExceptions} EXCEPTIONS
          </span>
        </div>

        <div className="glow-body">
          <div className="glow-metric-big">{formattedDiscrepancy}</div>
          <div className="glow-metric-sub">
            Flagged for Autonomous AI Investigation
          </div>

          {/* Exception Distribution Pills */}
          <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '14px', flexWrap: 'wrap' }}>
            {batch.amountMismatchCount > 0 && (
              <span style={{ fontSize: '11px', background: 'rgba(255,255,255,0.25)', padding: '4px 10px', borderRadius: '999px' }}>
                {batch.amountMismatchCount} Amount Mismatch
              </span>
            )}
            {batch.missingSettlementCount > 0 && (
              <span style={{ fontSize: '11px', background: 'rgba(255,255,255,0.25)', padding: '4px 10px', borderRadius: '999px' }}>
                {batch.missingSettlementCount} Missing Payout
              </span>
            )}
            {batch.duplicateCount > 0 && (
              <span style={{ fontSize: '11px', background: 'rgba(255,255,255,0.25)', padding: '4px 10px', borderRadius: '999px' }}>
                {batch.duplicateCount} Duplicates
              </span>
            )}
            {batch.unknownCount > 0 && (
              <span style={{ fontSize: '11px', background: 'rgba(255,255,255,0.25)', padding: '4px 10px', borderRadius: '999px' }}>
                {batch.unknownCount} Unknown Credit
              </span>
            )}
          </div>
        </div>

        <div className="glow-footer">
          <span>AI Resolution Status: <strong>Ready for Audit</strong></span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <ShieldAlert size={14} /> Zero Silent Loss
          </span>
        </div>
      </div>
    </div>
  );
}
