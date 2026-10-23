import { WorkflowSummary, WorkflowDetail } from "./types";

export function getApiBaseUrl(): string {
  if (typeof window !== "undefined") {
    const envApi = process.env.NEXT_PUBLIC_ORCHESTRATOR_API_URL;
    if (envApi && !envApi.includes("127.0.0.1") && !envApi.includes("localhost")) {
      return envApi;
    }
    // Auto-detect when accessed on GCP / remote IP (e.g. http://34.128.x.x:3000 -> http://34.128.x.x:8080)
    if (window.location.hostname !== "localhost" && window.location.hostname !== "127.0.0.1") {
      const protocol = window.location.protocol === "https:" ? "https:" : "http:";
      return `${protocol}//${window.location.hostname}:8080`;
    }
  }
  return process.env.NEXT_PUBLIC_ORCHESTRATOR_API_URL || "http://127.0.0.1:8080";
}

export function getWsBaseUrl(): string {
  if (typeof window !== "undefined") {
    const envWs = process.env.NEXT_PUBLIC_WS_URL;
    if (envWs && !envWs.includes("127.0.0.1") && !envWs.includes("localhost")) {
      return envWs;
    }
    // Auto-detect WebSocket when accessed on GCP / remote IP
    if (window.location.hostname !== "localhost" && window.location.hostname !== "127.0.0.1") {
      const wsProto = window.location.protocol === "https:" ? "wss:" : "ws:";
      return `${wsProto}//${window.location.hostname}:8080/ws/agent-events`;
    }
  }
  return process.env.NEXT_PUBLIC_WS_URL || "ws://127.0.0.1:8080/ws/agent-events";
}

const API_BASE = {
  toString: () => getApiBaseUrl(),
};

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

export async function startWorkflow(
  taskName: string,
  description: string,
  projectId?: string,
  clientId?: string
): Promise<WorkflowSummary> {
  const res = await fetch(`${API_BASE}/api/workflow/start`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      projectId: projectId || undefined,
      clientId: clientId || undefined,
      taskName,
      description,
      initialPayload: "{}"
    }),
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

export async function returnWorkflow(
  id: string,
  comment: string,
  targetPhase?: string,
  actor = "HUMAN_OPERATOR"
): Promise<WorkflowSummary> {
  const res = await fetch(`${API_BASE}/api/workflow/${id}/return`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ comment, actor, targetPhase: targetPhase || undefined }),
  });
  if (!res.ok) throw new Error("Gagal mengembalikan alur kerja untuk revisi");
  return await res.json();
}

export async function rejectWorkflow(
  id: string,
  comment: string,
  actor = "HUMAN_OPERATOR"
): Promise<WorkflowSummary> {
  return returnWorkflow(id, comment, undefined, actor);
}

export async function advanceWorkflow(id: string, comment = "Manual advance"): Promise<WorkflowSummary> {
  const res = await fetch(`${API_BASE}/api/workflow/${id}/advance?comment=${encodeURIComponent(comment)}`, {
    method: "POST",
  });
  if (!res.ok) throw new Error("Gagal memajukan fase");
  return await res.json();
}

export async function revertWorkflowPhase(
  id: string,
  targetPhase: string,
  reason = "Reverted by operator",
  actor = "HUMAN_OPERATOR"
): Promise<WorkflowSummary> {
  const res = await fetch(
    `${API_BASE}/api/workflow/${id}/revert-phase?targetPhase=${encodeURIComponent(targetPhase)}&reason=${encodeURIComponent(reason)}&actor=${encodeURIComponent(actor)}`,
    {
      method: "POST",
    }
  );
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.message || "Gagal mengembalikan fase workflow");
  }
  return await res.json();
}

export async function retryWorkflowPhase(
  id: string,
  reason = "Execution retried by operator",
  actor = "HUMAN_OPERATOR"
): Promise<WorkflowSummary> {
  const res = await fetch(
    `${API_BASE}/api/workflow/${id}/retry?reason=${encodeURIComponent(reason)}&actor=${encodeURIComponent(actor)}`,
    {
      method: "POST",
    }
  );
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.message || "Gagal menjalankan ulang fase workflow");
  }
  return await res.json();
}

export async function submitCouncilReview(
  id: string,
  votes: {
    architectApproved: boolean;
    securityApproved: boolean;
    performanceApproved: boolean;
    coveragePercentage: number;
    feedback: string;
  }
): Promise<WorkflowSummary> {
  const res = await fetch(`${API_BASE}/api/workflow/${id}/council-review`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(votes),
  });
  if (!res.ok) throw new Error("Gagal mengirim hasil voting council triad");
  return await res.json();
}

export interface ArtifactMeta {
  name: string;
  path: string;
  type: string;
  sizeBytes?: string;
}

export async function fetchArtifacts(workflowId: string): Promise<ArtifactMeta[]> {
  try {
    const res = await fetch(`${API_BASE}/api/workflow/${workflowId}/artifacts`, { cache: "no-store" });
    if (!res.ok) throw new Error("Failed to load artifacts");
    return await res.json();
  } catch (err) {
    console.warn("Using fallback artifacts:", err);
    return [
      { name: "GHERKIN_REQUIREMENTS.md", path: "backlog/GHERKIN_REQUIREMENTS.md", type: "MARKDOWN" },
      { name: "phase0-threat-model.md", path: "council-audits/phase0-threat-model.md", type: "MARKDOWN" },
      { name: "SDLC_LOG.md", path: "SDLC_LOG.md", type: "MARKDOWN" },
      { name: "raw_input.md", path: "raw_input.md", type: "MARKDOWN" },
    ];
  }
}

export async function fetchArtifactContent(workflowId: string, path: string): Promise<{ fileName: string; path: string; content: string }> {
  const res = await fetch(`${API_BASE}/api/workflow/${workflowId}/artifact-content?path=${encodeURIComponent(path)}`, { cache: "no-store" });
  if (!res.ok) throw new Error("Gagal membaca berkas: " + path);
  return await res.json();
}

export async function deleteWorkflow(id: string): Promise<void> {
  const res = await fetch(`${API_BASE}/api/workflow/${id}`, {
    method: "DELETE",
  });
  if (!res.ok) throw new Error("Gagal menghapus proyek / workflow");
}

export interface ProjectRuntimeStatus {
  projectId: string;
  status: "STOPPED" | "STARTING" | "RUNNING" | "STOPPING" | "ERROR";
  frontendUrl: string;
  backendUrl: string;
  frontendPort: number;
  backendPort: number;
  postgresPort: number;
  redisPort: number;
  kafkaPort: number;
  message: string;
  lastUpdated: string;
}

export async function fetchProjectRuntimeStatus(projectId: string): Promise<ProjectRuntimeStatus> {
  try {
    const res = await fetch(`${API_BASE}/api/v1/projects/${projectId}/runtime/status`, { cache: "no-store" });
    if (!res.ok) throw new Error("Gagal mengambil status runtime proyek");
    return await res.json();
  } catch (err) {
    return {
      projectId,
      status: "STOPPED",
      frontendUrl: "http://localhost:3200",
      backendUrl: "http://localhost:8180",
      frontendPort: 3200,
      backendPort: 8180,
      postgresPort: 5433,
      redisPort: 6380,
      kafkaPort: 9092,
      message: "Runtime offline (fallback)",
      lastUpdated: new Date().toISOString(),
    };
  }
}

export async function startProjectRuntime(projectId: string): Promise<ProjectRuntimeStatus> {
  const res = await fetch(`${API_BASE}/api/v1/projects/${projectId}/runtime/start`, {
    method: "POST",
  });
  if (!res.ok) throw new Error("Gagal menjalankan container stack proyek");
  return await res.json();
}

export async function stopProjectRuntime(projectId: string): Promise<ProjectRuntimeStatus> {
  const res = await fetch(`${API_BASE}/api/v1/projects/${projectId}/runtime/stop`, {
    method: "POST",
  });
  if (!res.ok) throw new Error("Gagal menghentikan container stack proyek");
  return await res.json();
}



