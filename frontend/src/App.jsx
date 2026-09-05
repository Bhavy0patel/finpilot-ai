import React, { useState, useEffect } from 'react';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import HeroGlowCards from './components/HeroGlowCards';
import MetricsCards from './components/MetricsCards';
import FileDropzone from './components/FileDropzone';
import ReconciliationTable from './components/ReconciliationTable';
import ExceptionsFeed from './components/ExceptionsFeed';
import AiDiagnosisCard from './components/AiDiagnosisCard';
import { getBatches, getBatchById, uploadAndReconcile, investigateBatch } from './api/client';
import { Loader2 } from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [batches, setBatches] = useState([]);
  const [selectedBatchId, setSelectedBatchId] = useState(null);
  const [currentBatch, setCurrentBatch] = useState(null);
  const [selectedException, setSelectedException] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isInvestigating, setIsInvestigating] = useState(false);

  // Load initial batch history on app launch
  useEffect(() => {
    loadBatches();
  }, []);

  // When selected batch changes, fetch its full details
  useEffect(() => {
    if (selectedBatchId) {
      loadBatchDetails(selectedBatchId);
    }
  }, [selectedBatchId]);

  const loadBatches = async () => {
    setIsLoading(true);
    try {
      const data = await getBatches();
      setBatches(data);
      if (data && data.length > 0) {
        setSelectedBatchId(data[0].id);
        setActiveTab('dashboard');
      } else {
        setActiveTab('upload');
      }
    } catch (err) {
      console.warn('Backend not yet populated or offline:', err.message);
      setActiveTab('upload');
    } finally {
      setIsLoading(false);
    }
  };

  const loadBatchDetails = async (batchId) => {
    try {
      const batchData = await getBatchById(batchId);
      setCurrentBatch(batchData);
    } catch (err) {
      console.error('Failed to load batch details:', err);
    }
  };

  const handleUploadSuccess = async (paymentsFile, settlementsFile, batchName) => {
    const newBatch = await uploadAndReconcile(paymentsFile, settlementsFile, batchName);
    setCurrentBatch(newBatch);
    setSelectedBatchId(newBatch.id);
    await loadBatches();
    setActiveTab('dashboard');
  };

  const handleTriggerAiInvestigation = async () => {
    if (!currentBatch?.id) return;
    setIsInvestigating(true);
    try {
      const updatedBatch = await investigateBatch(currentBatch.id);
      setCurrentBatch(updatedBatch);
    } catch (err) {
      alert('AI Investigation error: ' + err.message);
    } finally {
      setIsInvestigating(false);
    }
  };

  const totalExceptions = currentBatch ? (
    (currentBatch.amountMismatchCount || 0) +
    (currentBatch.missingSettlementCount || 0) +
    (currentBatch.duplicateCount || 0) +
    (currentBatch.unknownCount || 0)
  ) : 0;

  return (
    <div className="app-container">
      {/* 1. Minimalist Floating Sidebar */}
      <Sidebar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        exceptionCount={totalExceptions}
      />

      {/* 2. Main Dashboard Stage */}
      <main className="main-content">
        <Header
          batches={batches}
          selectedBatchId={selectedBatchId}
          onSelectBatch={setSelectedBatchId}
          onNewUploadClick={() => setActiveTab('upload')}
          currentBatch={currentBatch}
        />

        {isLoading ? (
          <div style={{ padding: '80px', textAlign: 'center', color: '#6B7280' }}>
            <Loader2 size={32} className="animate-spin" style={{ margin: '0 auto 12px' }} />
            <p style={{ fontWeight: 600 }}>Loading FinPilot Dashboard...</p>
          </div>
        ) : (
          <>
            {/* View: Upload New Data */}
            {activeTab === 'upload' && (
              <FileDropzone onUploadSuccess={handleUploadSuccess} />
            )}

            {/* View: Master Overview Dashboard */}
            {activeTab === 'dashboard' && currentBatch && (
              <>
                <HeroGlowCards batch={currentBatch} />
                <MetricsCards batch={currentBatch} />
                <ExceptionsFeed
                  items={currentBatch.items || []}
                  onTriggerAiInvestigation={handleTriggerAiInvestigation}
                  isInvestigating={isInvestigating}
                  onInspectItem={setSelectedException}
                />
                <ReconciliationTable
                  items={currentBatch.items || []}
                  onInspectItem={setSelectedException}
                />
              </>
            )}

            {/* View: Exceptions Feed Tab */}
            {activeTab === 'exceptions' && currentBatch && (
              <ExceptionsFeed
                items={currentBatch.items || []}
                onTriggerAiInvestigation={handleTriggerAiInvestigation}
                isInvestigating={isInvestigating}
                onInspectItem={setSelectedException}
              />
            )}

            {/* View: Full Table Tab */}
            {activeTab === 'table' && currentBatch && (
              <ReconciliationTable
                items={currentBatch.items || []}
                onInspectItem={setSelectedException}
              />
            )}

            {/* Empty State when no batch is selected yet */}
            {activeTab !== 'upload' && !currentBatch && (
              <div style={{
                background: '#FFFFFF',
                borderRadius: '24px',
                padding: '60px 24px',
                textAlign: 'center',
                boxShadow: '0 4px 20px rgba(0,0,0,0.03)'
              }}>
                <h3 style={{ fontSize: '18px', fontWeight: 700 }}>No Reconciliation Batches Found</h3>
                <p style={{ color: '#6B7280', fontSize: '14px', marginTop: '6px', marginBottom: '20px' }}>
                  Upload your payment and settlement CSV files to run autonomous reconciliation.
                </p>
                <button
                  onClick={() => setActiveTab('upload')}
                  className="btn-reconcile"
                  style={{ maxWidth: '240px', margin: '0 auto' }}
                >
                  Upload Sample Files Now
                </button>
              </div>
            )}
          </>
        )}
      </main>

      {/* 3. AI Diagnosis Modal / Drawer */}
      {selectedException && (
        <AiDiagnosisCard
          item={selectedException}
          onClose={() => setSelectedException(null)}
          onResolve={() => setSelectedException(null)}
        />
      )}
    </div>
  );
}
