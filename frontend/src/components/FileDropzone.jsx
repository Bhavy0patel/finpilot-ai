import React, { useState } from 'react';
import { UploadCloud, FileCheck, ArrowRight, Loader2 } from 'lucide-react';

export default function FileDropzone({ onUploadSuccess }) {
  const [paymentsFile, setPaymentsFile] = useState(null);
  const [settlementsFile, setSettlementsFile] = useState(null);
  const [batchName, setBatchName] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleReconcile = async () => {
    if (!paymentsFile || !settlementsFile) {
      setErrorMessage('Please select both payments.csv and settlements.csv files to proceed.');
      return;
    }

    setIsLoading(true);
    setErrorMessage('');

    try {
      await onUploadSuccess(paymentsFile, settlementsFile, batchName);
      setPaymentsFile(null);
      setSettlementsFile(null);
      setBatchName('');
    } catch (err) {
      setErrorMessage(err.message || 'Failed to upload and reconcile files.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="upload-card">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: '18px', fontWeight: 700, letterSpacing: '-0.02em' }}>
            Data Ingestion & Reconciliation
          </h2>
          <p style={{ fontSize: '13px', color: '#6B7280', marginTop: '4px' }}>
            Upload gateway transaction records and bank settlement exports for deterministic pairing.
          </p>
        </div>

        <input
          type="text"
          placeholder="Batch Name (Optional)"
          value={batchName}
          onChange={(e) => setBatchName(e.target.value)}
          style={{
            padding: '8px 16px',
            borderRadius: '999px',
            border: '1px solid #E5E7EB',
            fontSize: '13px',
            fontFamily: 'inherit',
            outline: 'none',
            background: '#F9FAFB',
            width: '220px'
          }}
        />
      </div>

      <div className="upload-grid">
        {/* Slot 1: Payments CSV */}
        <label className={`drop-slot ${paymentsFile ? 'has-file' : ''}`}>
          <input
            type="file"
            accept=".csv"
            style={{ display: 'none' }}
            onChange={(e) => e.target.files?.[0] && setPaymentsFile(e.target.files[0])}
          />
          <div className="slot-icon-wrap">
            {paymentsFile ? <FileCheck size={24} /> : <UploadCloud size={24} />}
          </div>
          <div className="slot-title">
            {paymentsFile ? paymentsFile.name : '1. Payments Gateway CSV'}
          </div>
          <div className="slot-hint">
            {paymentsFile
              ? `${(paymentsFile.size / 1024).toFixed(1)} KB • Click to change`
              : 'Drag & drop payments_sample.csv here'}
          </div>
        </label>

        {/* Slot 2: Settlements CSV */}
        <label className={`drop-slot ${settlementsFile ? 'has-file' : ''}`}>
          <input
            type="file"
            accept=".csv"
            style={{ display: 'none' }}
            onChange={(e) => e.target.files?.[0] && setSettlementsFile(e.target.files[0])}
          />
          <div className="slot-icon-wrap">
            {settlementsFile ? <FileCheck size={24} /> : <UploadCloud size={24} />}
          </div>
          <div className="slot-title">
            {settlementsFile ? settlementsFile.name : '2. Bank Settlements CSV'}
          </div>
          <div className="slot-hint">
            {settlementsFile
              ? `${(settlementsFile.size / 1024).toFixed(1)} KB • Click to change`
              : 'Drag & drop settlements_sample.csv here'}
          </div>
        </label>
      </div>

      {errorMessage && (
        <div style={{
          marginTop: '16px',
          padding: '12px 18px',
          borderRadius: '12px',
          background: '#FEF2F2',
          border: '1px solid #FCA5A5',
          color: '#B91C1C',
          fontSize: '13px',
          fontWeight: 600
        }}>
          {errorMessage}
        </div>
      )}

      <button
        className="btn-reconcile"
        onClick={handleReconcile}
        disabled={isLoading || !paymentsFile || !settlementsFile}
      >
        {isLoading ? (
          <>
            <Loader2 size={18} className="animate-spin" />
            <span>Reconciling & Validating Data...</span>
          </>
        ) : (
          <>
            <span>Run Autonomous Reconciliation</span>
            <ArrowRight size={18} />
          </>
        )}
      </button>
    </div>
  );
}
