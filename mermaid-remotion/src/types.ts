export interface WorkflowMeta {
  workflowId: string;
  projectName: string;
  taskTitle: string;
  targetBranch: string;
  featureBranch: string;
}

export const DEFAULT_META: WorkflowMeta = {
  workflowId: 'wf-98b7c4a1',
  projectName: 'PRJ-ECOMMERCE-CHECKOUT',
  taskTitle: 'Idempotent Payment Webhook Handling',
  targetBranch: 'main',
  featureBranch: 'feat/order-idempotency',
};

export const VIDEO_CONFIG = {
  fps: 30,
  width: 1920,
  height: 1080,
  totalFrames: 1800, // 60s
  scenes: {
    intro: { from: 0, duration: 300 }, // 0s - 10s
    consensus: { from: 300, duration: 360 }, // 10s - 22s
    protection: { from: 660, duration: 390 }, // 22s - 35s
    telegram: { from: 1050, duration: 360 }, // 35s - 47s
    merge: { from: 1410, duration: 240 }, // 47s - 55s
    outro: { from: 1650, duration: 150 }, // 55s - 60s
  },
};

export const THEME = {
  bg: '#090d16',
  surface: '#131b2e',
  surfaceLight: '#1e293b',
  border: '#334155',
  primary: '#3b82f6',
  indigo: '#6366f1',
  success: '#10b981',
  danger: '#f43f5e',
  warning: '#f97316',
  text: '#f8fafc',
  textMuted: '#94a3b8',
};
