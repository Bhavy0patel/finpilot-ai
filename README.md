# FinPilot AI – Autonomous AI Finance Controller

> **Automated Financial Reconciliation & AI Root-Cause Investigation for Payment Gateways and Bank Settlements.**  
> *Built for the Razorpay AI Buildathon 2026.*

---

## 📌 Problem Statement

Reconciling transaction records between payment gateways (such as Razorpay) and bank nodal settlement statements is an essential operational task for merchants and fintech platforms. Doing this manually with spreadsheets presents several practical challenges:

- **MDR & Processing Fee Deductions:** Payment gateway Merchant Discount Rate (MDR) fees and processing charges cause small variances between the captured payment amount and the credited bank settlement amount.
- **Settlement Timing & Cut-Offs:** Banking cut-offs and settlement cycles (e.g., T+1 or T+2) can leave captured transactions missing from the corresponding bank settlement file.
- **Duplicate Records:** Webhook re-deliveries or repeated customer checkout attempts can create duplicate payment entries on the merchant side.
- **Unlinked Bank Credits:** Direct payouts or transfers can appear in bank statements without a matching gateway transaction identifier.
- **Manual Effort:** Identifying the root cause of each exception in large CSV files requires cross-checking multiple fields, calculating percentage variances, and drafting corrective ledger adjustments.

---

## 💡 Solution

**FinPilot AI** automates this workflow using a two-stage approach:

1. **Deterministic Java Reconciliation Engine:** Processes payment and settlement CSV files, performs exact 1:1 transaction matching using Java `BigDecimal` arithmetic, and categorizes every record into an explicit reconciliation status.
2. **AI Root-Cause Investigator (Gemini 3.6 Flash):** Selectively analyzes only the flagged discrepancy items using Google Gemini. The AI provides structured root-cause explanations, confidence scores, evidence references grounded in transaction numbers, and recommended accounting actions.

---

## ✨ Key Features

- 📁 **CSV Upload Stage:** Upload payment gateway exports (`payments.csv`) and bank settlement files (`settlements.csv`) directly from the browser.
- ⚡ **Deterministic 1:1 Matching:** Compares transaction IDs and settlement amounts with exact mathematical precision using Java `BigDecimal`.
- 🔍 **Anomaly Classification:** Automatically categorizes records into Matched, Amount Mismatch, Missing Settlement, Duplicate Transaction, and Unknown Transaction.
- 🧠 **AI Root-Cause Investigation:** On-demand anomaly diagnosis using Google Gemini 3.6 Flash in structured JSON mode.
- 🎯 **Confidence & Evidence Grounding:** AI responses include confidence scores (0.0 to 1.0), explicit mathematical evidence references, and prescriptive controller actions.
- 💾 **MongoDB Persistence:** Stores each reconciliation batch, item-level statuses, and AI investigation histories in MongoDB (`finpilot_db`).
- 🖥️ **Interactive Dashboard:** React + Vite interface with status overview cards, metrics summary, exception feeds, searchable audit ledger, and root-cause inspection modals.
- 🔄 **Dual-Mode AI Resilience:** Automatically switches between live Gemini analysis (`Gemini AI • Live`) and deterministic domain fallback (`Demo / Fallback Mode`) based on API key availability.

---

## 🔄 How It Works

```
 ┌──────────────────────┐
 │ Upload CSV Datasets  │  (payments.csv + settlements.csv)
 └──────────┬───────────┘
            │
            ▼
 ┌──────────────────────┐
 │ Streaming CSV Parser │  (Apache Commons CSV parses files into Java domain entities)
 └──────────┬───────────┘
            │
            ▼
 ┌──────────────────────┐
 │ Deterministic Engine │  (Exact 1:1 ID matching, BigDecimal difference calculations)
 └──────────┬───────────┘
            │
            ▼
 ┌──────────────────────┐
 │ MongoDB Batch Store  │  (Saves ReconciliationBatch and line items to finpilot_db)
 └──────────┬───────────┘
            │
            ▼
 ┌──────────────────────┐
 │ Filter Discrepancies │  (Isolates non-MATCHED items for AI analysis)
 └──────────┬───────────┘
            │
            ▼
 ┌──────────────────────┐
 │ Gemini Investigation │  (Gemini 3.6 Flash generates structured root-cause analyses)
 └──────────┬───────────┘
            │
            ▼
 ┌──────────────────────┐
 │ Review & Resolution  │  (Dashboard renders live diagnoses and prescribed actions)
 └──────────────────────┘
```

---

## 🏗️ System Architecture

```mermaid
flowchart TD
    subgraph Frontend["Frontend Client (React 18 + Vite)"]
        UI["Dashboard UI"]
        Dropzone["CSV Upload Stage"]
        Feed["Exceptions & AI Feed"]
        Ledger["Records Ledger Table"]
        Modal["AI Diagnosis Modal"]
    end

    subgraph Backend["Backend Service (Spring Boot 3.4.3 / Java 17)"]
        Controller["ReconciliationController (/reconcile/*)"]
        Parser["CsvParserService (CSV Streaming)"]
        Matcher["ReconciliationEngine (BigDecimal Matcher)"]
        AIService["AiInvestigationService (Gemini 3.6 Flash & Fallback)"]
    end

    subgraph External["AI Service"]
        GeminiAPI["Google Gemini API (gemini-3.6-flash)"]
    end

    subgraph Storage["Database (MongoDB)"]
        MongoDB[("MongoDB / Atlas (finpilot_db)")]
    end

    UI --> Dropzone
    Dropzone -->|POST /reconcile/upload| Controller
    Controller --> Parser
    Parser --> Matcher
    Matcher -->|Save Batch| MongoDB

    Feed -->|POST /reconcile/batches/{id}/investigate| Controller
    Controller --> AIService
    AIService -->|Key Present| GeminiAPI
    AIService -->|Key Missing / Offline| AIService
    GeminiAPI -->|Structured JSON Response| AIService
    AIService -->|Update Batch (source: GEMINI/FALLBACK)| MongoDB
    MongoDB --> Controller
    Controller --> UI
    UI --> Feed
    UI --> Ledger
    UI --> Modal
```

---

## 🧰 Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Frontend** | React 18, Vite 6, Lucide React, CSS3 |
| **Backend** | Java 17, Spring Boot 3.4.3, Spring Web (`RestClient`), Spring Data MongoDB |
| **Parsing & Calculations** | Apache Commons CSV 1.11.0, Java `BigDecimal` |
| **AI Integration** | Google Gemini API (`gemini-3.6-flash` via structured JSON `generateContent`) |
| **Database** | MongoDB 6.0+ / MongoDB Atlas (`finpilot_db`) |
| **Testing** | JUnit 5, Mockito, Spring Boot Starter Test |
| **Build Tools** | Apache Maven 3.9+, Node.js 18+ |

---

## 📁 Project Structure

```
finpilot-ai/
├── backend/
│   ├── pom.xml                                  # Maven dependencies & build configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/finpilot/
│       │   │   ├── FinPilotApplication.java       # Application entry point
│       │   │   ├── config/WebConfig.java          # CORS & Web MVC configuration
│       │   │   ├── controller/ReconciliationController.java # REST API endpoints
│       │   │   ├── dto/ReconciliationSummaryDto.java       # Summary data transfer object
│       │   │   ├── model/                         # Domain models & enums
│       │   │   │   ├── AiInvestigation.java       # AI root-cause & source model
│       │   │   │   ├── PaymentTransaction.java    # Gateway payment entity
│       │   │   │   ├── ReconciliationBatch.java   # Top-level batch document
│       │   │   │   ├── ReconciliationItem.java    # Line-item reconciliation model
│       │   │   │   ├── ReconciliationStatus.java  # Reconciliation status enum
│       │   │   │   └── SettlementRecord.java      # Bank settlement entity
│       │   │   ├── repository/ReconciliationBatchRepository.java # Mongo repository
│       │   │   └── service/                       # Core business logic
│       │   │       ├── AiInvestigationService.java # Gemini 3.6 Flash & fallback logic
│       │   │       ├── CsvParserService.java       # CSV streaming parser
│       │   │       └── ReconciliationEngine.java   # Deterministic matcher
│       │   └── resources/
│       │       └── application.properties          # Server port, MongoDB URI, Gemini URL
│       └── test/java/com/finpilot/                 # Automated unit & integration tests
│           ├── controller/ReconciliationControllerTest.java
│           └── service/
│               ├── AiInvestigationServiceTest.java
│               ├── GeminiLiveVerificationTest.java
│               └── ReconciliationEngineTest.java
├── frontend/
│   ├── index.html                               # HTML entry point
│   ├── package.json                             # Frontend dependencies & scripts
│   ├── vite.config.js                           # Vite configuration & backend proxy
│   └── src/
│       ├── main.jsx                             # React bootstrap
│       ├── App.jsx                              # Dashboard state & orchestration
│       ├── App.css                              # UI styles & animations
│       ├── api/client.js                        # Axios/Fetch API client
│       └── components/                          # React UI components
│           ├── Header.jsx                       # Header, batch selector & status pill
│           ├── Sidebar.jsx                      # Navigation sidebar
│           ├── HeroGlowCards.jsx                # Health rate & exposure glow cards
│           ├── MetricsCards.jsx                 # 6-Card KPI summary grid
│           ├── FileDropzone.jsx                 # Dual CSV upload dropzone
│           ├── ExceptionsFeed.jsx               # Anomaly feed with AI insights
│           ├── ReconciliationTable.jsx          # Records ledger with search & filters
│           └── AiDiagnosisCard.jsx              # AI diagnosis modal
└── sample-data/                                 # Test datasets
    ├── payments_sample.csv                      # Gateway transactions with anomalies
    ├── settlements_sample.csv                   # Bank settlement statement
    ├── try_payments.csv                         # 10-transaction test payments
    └── try_settlements.csv                      # 10-transaction test settlements
```

---

## 🚦 Reconciliation Statuses & Anomaly Types

FinPilot AI categorizes every transaction into one of 5 statuses defined in `ReconciliationStatus.java`:

| Status Enum | Description | Typical Financial Scenario |
| :--- | :--- | :--- |
| **`MATCHED`** | Transaction ID and amount match between gateway and bank. | Clean settlement cycle. Zero discrepancy. |
| **`AMOUNT_MISMATCH`** | Transaction ID matches, but payment amount does not equal settled amount. | MDR processing fee deduction (1–3%), processing surcharge, or partial deduction. |
| **`MISSING_SETTLEMENT`** | Transaction is captured in payment gateway, but missing in bank settlement. | Settlement cycle lag (T+1/T+2 days), nodal account cut-off, or bank hold. |
| **`DUPLICATE_TRANSACTION`** | Same Transaction ID appears multiple times in payment records. | Customer double-click at checkout or duplicate webhook delivery from gateway. |
| **`UNKNOWN_TRANSACTION`** | Bank settlement credit received with no matching gateway transaction ID. | Direct NEFT/RTGS transfer or unlinked manual bank credit. |

---

## 📊 Sample Data & Test Scenarios

The `sample-data/` directory includes sample CSV files demonstrating all anomaly types:

### Primary Benchmark Pair (`payments_sample.csv` & `settlements_sample.csv`)

| Txn ID | Gateway Amt | Bank Settled Amt | Difference | Status | Financial Scenario |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `TXN_1001` | ₹1,500.00 | ₹1,500.00 | ₹0.00 | **MATCHED** | Clean UPI payment. |
| `TXN_1002` | ₹2,499.00 | ₹2,499.00 | ₹0.00 | **MATCHED** | Clean Card payment. |
| `TXN_1003` | ₹5,000.00 | ₹4,900.00 | ₹100.00 | **AMOUNT_MISMATCH** | ₹100 MDR fee deducted by payment gateway. |
| `TXN_1004` | ₹1,200.00 | *None* | ₹1,200.00 | **MISSING_SETTLEMENT** | Captured payment pending settlement cycle. |
| `TXN_1005` | ₹850.00 (x2) | ₹850.00 | ₹0.00 | **DUPLICATE_TRANSACTION** | Duplicate payment entry in gateway export. |
| `TXN_9999` | *None* | ₹750.00 | ₹750.00 | **UNKNOWN_TRANSACTION** | Direct bank credit without gateway ID. |
| `TXN_1006` | ₹3,100.00 | ₹3,100.00 | ₹0.00 | **MATCHED** | Clean UPI payment. |

A secondary 10-transaction dataset (`try_payments.csv` and `try_settlements.csv`) is also included for multi-batch testing.

---

## 🤖 AI Investigation & Fallback Behavior

FinPilot AI supports two execution paths for exception analysis:

1. **`Gemini AI • Live` (Live API Mode):**  
   When a valid `GEMINI_API_KEY` is available in the environment, the application sends the anomaly records to Google's **Gemini 3.6 Flash** model in structured JSON mode. The returned analysis is saved to MongoDB and stamped with `source: "GEMINI"`.
2. **`Demo / Fallback Mode` (Deterministic Fallback):**  
   If the API key is not configured, or if the external API is unreachable, the backend applies rule-based domain logic to generate standard financial explanations. The result is stamped with `source: "FALLBACK"`.
3. **Retry & Fast Fallback Handling:**  
   Permanent client/configuration errors (`4xx`, such as invalid model or authentication failure) immediately trigger the fallback without retry delays. Temporary failures (`429 Rate Limit` or `5xx Server Error`) retry up to 3 times before falling back.

The frontend dynamically indicates which mode was used via header pills and item-level badges.

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+** (JDK 17 or higher)
- **Apache Maven 3.9+**
- **Node.js 18+** & **npm**
- **MongoDB 6.0+** running locally on port `27017` (or a MongoDB Atlas connection string)
- *(Optional for Live AI)* A free Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey)

---

### Step 1: Clone the Repository
```bash
git clone https://github.com/your-username/finpilot-ai.git
cd finpilot-ai
```

---

### Step 2: Ensure MongoDB is Running
Start your local MongoDB service:
```bash
# Verify local MongoDB connection (default: mongodb://localhost:27017/finpilot_db)
mongosh --eval "db.adminCommand('ping')"
```

---

### Step 3: Configure Environment Variables & Start Backend

#### Option A: Running with Live Gemini AI
```powershell
# Windows PowerShell
$env:GEMINI_API_KEY="your_actual_gemini_api_key_here"
cd backend
mvn spring-boot:run
```

```bash
# macOS / Linux
export GEMINI_API_KEY="your_actual_gemini_api_key_here"
cd backend
mvn spring-boot:run
```

*(Alternatively, pass the argument directly: `mvn spring-boot:run -Dspring-boot.run.arguments="--gemini.api.key=your_key"`)*

#### Option B: Running in Offline / Demo Fallback Mode
```bash
cd backend
mvn spring-boot:run
```

The Spring Boot backend will start on **`http://localhost:8080`**.

---

### Step 4: Install Dependencies & Start Frontend

In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```

Open your browser and navigate to:
👉 **`http://localhost:5173`**

---

## 🔌 API Endpoints

All endpoints are hosted under `/reconcile` on port `8080`:

| Method | Endpoint | Description | Request Payload | Response |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/reconcile/upload` | Uploads and reconciles payment & settlement CSV files. | `multipart/form-data` (`paymentsFile`, `settlementsFile`, optional `batchName`) | `201 Created` (`ReconciliationBatch`) |
| `POST` | `/reconcile/batches/{batchId}/investigate` | Triggers AI investigation for all unresolved anomalies in a batch. | None (Path variable `batchId`) | `200 OK` (`ReconciliationBatch`) |
| `GET` | `/reconcile/batches` | Retrieves historical reconciliation batch summaries. | None | `200 OK` (`List<ReconciliationSummaryDto>`) |
| `GET` | `/reconcile/batches/{batchId}` | Retrieves full batch details, line items, and nested AI investigations. | None (Path variable `batchId`) | `200 OK` (`ReconciliationBatch`) |
| `GET` | `/reconcile/batches/{batchId}/exceptions` | Retrieves only discrepancy anomaly records for a specific batch. | None (Path variable `batchId`) | `200 OK` (`List<ReconciliationItem>`) |

---

## 🧪 Testing

The backend includes unit and integration tests covering CSV parsing, deterministic reconciliation logic, AI service response handling, and REST endpoints:

```bash
cd backend

# Run standard unit and controller tests
mvn test -Dtest="ReconciliationEngineTest,AiInvestigationServiceTest,ReconciliationControllerTest"

# Run live Gemini API verification test (requires GEMINI_API_KEY in environment)
mvn test -Dtest="GeminiLiveVerificationTest"
```

---

## 🔒 Security & Git Hygiene

- **Environment Variable Key Loading:** The Gemini API key is loaded dynamically from `GEMINI_API_KEY` or runtime arguments and is not hardcoded in source files or `application.properties`.
- **Masked Logging:** Terminal diagnostic logs indicate whether an API key is present using masked references (e.g., `AIzaSy...`) rather than printing raw secrets.
- **Repository `.gitignore`:** Configured to exclude `.env`, `*.key`, `target/`, `node_modules/`, and build artifacts from version control.

---

## 🔮 Future Roadmap

- ⚡ **Webhook Ingestion:** Ingest real-time payment capture and settlement webhook events alongside batch CSV uploads.
- 📑 **Accounting Ledger Integration:** Export formatted journal adjustments directly into accounting software (Tally, Zoho Books, QuickBooks).
- 📈 **Fee Discrepancy Monitoring:** Track recurring MDR variances against contracted gateway rate cards.
- 🛡️ **Automated Dispute Drafting:** Generate structured dispute inquiry summaries for transactions with missing settlement credits.

---

## 🏆 Razorpay AI Buildathon 2026

FinPilot AI was developed for the **Razorpay AI Buildathon 2026** as a functional prototype exploring how deterministic financial validation and generative AI can work together to streamline payment reconciliation.
