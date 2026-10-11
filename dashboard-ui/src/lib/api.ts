import { WorkflowSummary, WorkflowDetail } from "./types";

const API_BASE = process.env.NEXT_PUBLIC_ORCHESTRATOR_API_URL || "http://localhost:8080";

export async function fetchWorkflows(): Promise<WorkflowSummary[]> {
  try {
    const res = await fetch(`${API_BASE}/api/workflow`, { cache: "no-store" });
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    return await res.json();
  } catch (err) {
    console.warn("Backend unavailable, using fallback mock data:", err);
    return [
      {
        id: "wf-live-01",
        taskName: "QRIS & Virtual Account Payment Gateway",
        description: "Integrasi sistem pembayaran instan multi-bank dengan state machine & idempotency key",
        currentState: "PHASE_0_WAITING_HUMAN_SIGN_OFF",
        reviewLoopCount: 0,
        coveragePercentage: 0,
        waitingForHuman: true,
        createdAt: new Date(Date.now() - 3600000).toISOString(),
        updatedAt: new Date().toISOString(),
      },
      {
        id: "wf-live-02",
        taskName: "High-Throughput Order Ingestion Engine",
        description: "Optimasi batch processing pesanan dengan Redis queue dan database connection pool tuning",
        currentState: "PHASE_6_CODE_REVIEW",
        reviewLoopCount: 2,
        coveragePercentage: 68.4,
        waitingForHuman: false,
        createdAt: new Date(Date.now() - 7200000).toISOString(),
        updatedAt: new Date().toISOString(),
      },
    ];
  }
}

export async function fetchWorkflowDetail(id: string): Promise<WorkflowDetail> {
  try {
    const res = await fetch(`${API_BASE}/api/workflow/${id}`, { cache: "no-store" });
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    return await res.json();
  } catch (err) {
    console.warn("Backend unavailable, using fallback detail:", err);
    return {
      id,
      taskName: "QRIS & Virtual Account Payment Gateway",
      description: "Integrasi sistem pembayaran instan multi-bank dengan state machine & idempotency key",
      currentState: "PHASE_0_WAITING_HUMAN_SIGN_OFF",
      reviewLoopCount: 0,
      coveragePercentage: 0,
      waitingForHuman: true,
      createdAt: new Date(Date.now() - 3600000).toISOString(),
      updatedAt: new Date().toISOString(),
      history: [
        {
          id: 1,
          workflowId: id,
          fromState: "PHASE_0_REQUIREMENT_DISCOVERY",
          toState: "PHASE_0_REQUIREMENT_DISCOVERY",
          actor: "SYSTEM",
          message: "Workflow initialized by Product Owner Agent",
          createdAt: new Date(Date.now() - 3600000).toISOString(),
        },
        {
          id: 2,
          workflowId: id,
          fromState: "PHASE_0_REQUIREMENT_DISCOVERY",
          toState: "PHASE_0_WAITING_HUMAN_SIGN_OFF",
          actor: "SPEC_WRITER_AGENT",
          message: "User stories dan skenario Gherkin telah disusun. Menunggu persetujuan sign-off manusia.",
          createdAt: new Date(Date.now() - 1800000).toISOString(),
        },
      ],
    };
  }
}

export async function startWorkflow(taskName: string, description: string): Promise<WorkflowSummary> {
  const res = await fetch(`${API_BASE}/api/workflow/start`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ taskName, description, initialPayload: "{}" }),
  });
  if (!res.ok) throw new Error("Gagal memulai workflow baru");
  return await res.json();
}

export async function approveWorkflow(id: string, comment: string, actor = "HUMAN_OPERATOR"): Promise<WorkflowSummary> {
  const res = await fetch(`${API_BASE}/api/workflow/${id}/approve`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ comment, actor }),
  });
  if (!res.ok) throw new Error("Gagal memproses persetujuan");
  return await res.json();
}

export async function rejectWorkflow(id: string, comment: string, actor = "HUMAN_OPERATOR"): Promise<WorkflowSummary> {
  const res = await fetch(`${API_BASE}/api/workflow/${id}/reject`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ comment, actor }),
  });
  if (!res.ok) throw new Error("Gagal memproses penolakan");
  return await res.json();
}

export async function advanceWorkflow(id: string, comment = "Manual advance"): Promise<WorkflowSummary> {
  const res = await fetch(`${API_BASE}/api/workflow/${id}/advance?comment=${encodeURIComponent(comment)}`, {
    method: "POST",
  });
  if (!res.ok) throw new Error("Gagal memajukan fase");
  return await res.json();
}
