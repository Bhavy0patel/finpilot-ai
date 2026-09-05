/**
 * FinPilot AI API Client
 * Interacts with Spring Boot backend endpoints under /reconcile
 */

const API_BASE = '/reconcile';

/**
 * Uploads payments & settlements CSV files to execute reconciliation.
 */
export async function uploadAndReconcile(paymentsFile, settlementsFile, batchName = '') {
  const formData = new FormData();
  formData.append('paymentsFile', paymentsFile);
  formData.append('settlementsFile', settlementsFile);
  if (batchName && batchName.trim()) {
    formData.append('batchName', batchName.trim());
  }

  const response = await fetch(`${API_BASE}/upload`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Upload failed with status ${response.status}`);
  }

  return response.json();
}

/**
 * Retrieves all historical reconciliation batches (summaries).
 */
export async function getBatches() {
  const response = await fetch(`${API_BASE}/batches`);
  if (!response.ok) {
    throw new Error(`Failed to fetch batches (HTTP ${response.status})`);
  }
  return response.json();
}

/**
 * Retrieves full batch report by ID (including all items).
 */
export async function getBatchById(batchId) {
  const response = await fetch(`${API_BASE}/batches/${batchId}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch batch ${batchId} (HTTP ${response.status})`);
  }
  return response.json();
}

/**
 * Retrieves only non-matched exceptions for a batch.
 */
export async function getBatchExceptions(batchId) {
  const response = await fetch(`${API_BASE}/batches/${batchId}/exceptions`);
  if (!response.ok) {
    throw new Error(`Failed to fetch exceptions for batch ${batchId}`);
  }
  return response.json();
}

/**
 * Triggers autonomous Gemini AI investigation on unresolved exceptions in a batch.
 */
export async function investigateBatch(batchId) {
  const response = await fetch(`${API_BASE}/batches/${batchId}/investigate`, {
    method: 'POST',
  });
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `AI investigation failed with status ${response.status}`);
  }
  return response.json();
}
