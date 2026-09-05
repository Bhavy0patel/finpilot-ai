import React from 'react';
import { ShieldCheck, Plus, Layers } from 'lucide-react';

export default function Header({ batches = [], selectedBatchId, onSelectBatch, onNewUploadClick, currentBatch }) {
  // Determine AI status based on investigated items in current batch
  const items = currentBatch?.items || [];
  const investigatedItems = items.filter(i => i.aiInvestigation);

  let statusType = 'ready'; // 'ready' | 'live' | 'fallback'
  if (investigatedItems.length > 0) {
    const hasLiveGemini = investigatedItems.some(i => i.aiInvestigation?.source === 'GEMINI');
    statusType = hasLiveGemini ? 'live' : 'fallback';
  }

  return (
    <header className="header-card">
      <div className="brand-group">
        <div className="brand-logo-badge">
          <ShieldCheck size={24} />
        </div>
        <div className="brand-titles">
          <h1>FinPilot AI</h1>
          <p>Autonomous Finance Controller</p>
        </div>
      </div>

      <div className="header-actions">
        {/* Dynamic status pill */}
        {statusType === 'live' && (
          <div className="ai-status-pill pill-live" title="Generated via Google Gemini Live API">
            <span className="pulse-dot dot-live"></span>
            <span>Gemini AI • Live</span>
          </div>
        )}
        {statusType === 'fallback' && (
          <div className="ai-status-pill pill-fallback" title="Deterministic domain fallback mode">
            <span className="pulse-dot dot-fallback"></span>
            <span>Demo / Fallback Mode</span>
          </div>
        )}
        {statusType === 'ready' && (
          <div className="ai-status-pill pill-ready" title="AI Engine is ready to investigate exceptions">
            <span className="pulse-dot dot-ready"></span>
            <span>AI Engine • Ready</span>
          </div>
        )}

        {/* Dynamic Batch Selector */}
        {batches.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Layers size={16} color="#6B7280" />
            <select
              className="batch-select"
              value={selectedBatchId || ''}
              onChange={(e) => onSelectBatch(e.target.value)}
            >
              {batches.map((batch) => (
                <option key={batch.id} value={batch.id}>
                  {batch.batchName || 'Batch'} ({new Date(batch.processedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})
                </option>
              ))}
            </select>
          </div>
        )}

        <button
          onClick={onNewUploadClick}
          style={{
            background: '#111827',
            color: '#FFFFFF',
            border: 'none',
            borderRadius: '999px',
            padding: '8px 18px',
            fontSize: '13px',
            fontWeight: 700,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
          }}
        >
          <Plus size={16} />
          <span>New Upload</span>
        </button>
      </div>
    </header>
  );
}
