export type WorkflowState =
  | "PHASE_0_REQUIREMENT_DISCOVERY"
  | "PHASE_0_WAITING_HUMAN_SIGN_OFF"
  | "PHASE_1_ARCHITECTURE_AND_DESIGN"
  | "PHASE_2_BACKLOG_GENERATION"
  | "PHASE_3_TASK_DISPATCHING"
  | "PHASE_4_TDD_PLANNING"
  | "PHASE_5_DEVELOPMENT_IMPLEMENTATION"
  | "PHASE_6_CODE_REVIEW"
  | "PHASE_6_DEADLOCK_WAITING_HUMAN"
  | "PHASE_7_QA_VERIFICATION"
  | "PHASE_8_READY_FOR_PRODUCTION_GATE"
  | "PHASE_9_RELEASED"
  | "FAILED";

export interface WorkflowSummary {
  id: string;
  taskName: string;
  description: string;
  currentState: WorkflowState;
  reviewLoopCount: number;
  coveragePercentage: number;
  lastDecision?: string;
  waitingForHuman: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowTransitionLog {
  id: number;
  workflowId: string;
  fromState: WorkflowState;
  toState: WorkflowState;
  actor: string;
  message: string;
  createdAt: string;
}

export interface WorkflowDetail extends WorkflowSummary {
  payloadJson?: string;
  history: WorkflowTransitionLog[];
}

export interface AgentEvent {
  id: string;
  workflowId: string;
  state: WorkflowState;
  type: string;
  message: string;
  timestamp: string;
}

export interface AgentTeamMember {
  id: string;
  name: string;
  role: string;
  team: "Product & Design" | "Development" | "CI/CD & DevOps" | "Production & SRE" | "Shared & Security";
  status: "IDLE" | "WORKING" | "WAITING_APPROVAL" | "DEADLOCK" | "COMPLETED";
  currentTask: string;
  activePhase: WorkflowState;
}
