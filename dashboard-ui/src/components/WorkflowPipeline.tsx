"use client";

import React from "react";
import { WorkflowState } from "@/lib/types";
import {
  FileText,
  Compass,
  ListTodo,
  Send,
  TestTube2,
  Code2,
  CheckCheck,
  ShieldCheck,
  Rocket,
  AlertTriangle,
} from "lucide-react";

interface WorkflowPipelineProps {
  currentState: WorkflowState;
  reviewLoopCount: number;
  coveragePercentage: number;
}

interface StepMeta {
  state: WorkflowState;
  shortLabel: string;
  name: string;
  icon: React.ReactNode;
}

const PHASES: StepMeta[] = [
  { state: "PHASE_0_REQUIREMENT_DISCOVERY", shortLabel: "0. PRD", name: "Requirements", icon: <FileText className="h-4 w-4" /> },
  { state: "PHASE_1_ARCHITECTURE_AND_DESIGN", shortLabel: "1. Arch", name: "C4 Architecture", icon: <Compass className="h-4 w-4" /> },
  { state: "PHASE_2_BACKLOG_GENERATION", shortLabel: "2. Backlog", name: "User Stories", icon: <ListTodo className="h-4 w-4" /> },
  { state: "PHASE_3_TASK_DISPATCHING", shortLabel: "3. Dispatch", name: "Task Allocator", icon: <Send className="h-4 w-4" /> },
  { state: "PHASE_4_TDD_PLANNING", shortLabel: "4. TDD", name: "Test First Suite", icon: <TestTube2 className="h-4 w-4" /> },
  { state: "PHASE_5_DEVELOPMENT_IMPLEMENTATION", shortLabel: "5. Dev", name: "Coding & Impl", icon: <Code2 className="h-4 w-4" /> },
  { state: "PHASE_6_CODE_REVIEW", shortLabel: "6. Review", name: "Consortium Audit", icon: <CheckCheck className="h-4 w-4" /> },
  { state: "PHASE_7_QA_VERIFICATION", shortLabel: "7. QA", name: "E2E Sandbox", icon: <ShieldCheck className="h-4 w-4" /> },
  { state: "PHASE_8_READY_FOR_PRODUCTION_GATE", shortLabel: "8. Gate", name: "DAST/MAST Gate", icon: <AlertTriangle className="h-4 w-4" /> },
  { state: "PHASE_9_RELEASED", shortLabel: "9. Release", name: "Production Live", icon: <Rocket className="h-4 w-4" /> },
];

function getPhaseIndex(state: WorkflowState): number {
  if (state === "PHASE_0_WAITING_HUMAN_SIGN_OFF") return 0;
  if (state === "PHASE_6_DEADLOCK_WAITING_HUMAN") return 6;
  return PHASES.findIndex((p) => p.state === state);
}

export function WorkflowPipeline({
  currentState,
  reviewLoopCount,
  coveragePercentage,
}: WorkflowPipelineProps) {
  const currentIndex = getPhaseIndex(currentState);
  const isDeadlock = currentState === "PHASE_6_DEADLOCK_WAITING_HUMAN";
  const isWaitingHuman =
    currentState === "PHASE_0_WAITING_HUMAN_SIGN_OFF" ||
    currentState === "PHASE_6_DEADLOCK_WAITING_HUMAN" ||
    currentState === "PHASE_8_READY_FOR_PRODUCTION_GATE";

  return (
    <div className="w-full rounded-2xl glass-panel p-4 sm:p-5">
      {/* Header bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4 pb-3 border-b border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-sm sm:text-base font-semibold text-white">
              Autonomous SDLC Pipeline Tracker
            </h2>
            {isDeadlock && (
              <span className="inline-flex items-center gap-1 rounded-full bg-rose-500/10 px-2 py-0.5 text-[11px] font-bold text-rose-400 border border-rose-500/30 animate-pulse">
                DEADLOCK (3x GAGAL)
              </span>
            )}
            {isWaitingHuman && !isDeadlock && (
              <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2 py-0.5 text-[11px] font-semibold text-amber-400 border border-amber-500/30">
                WAITING SIGN-OFF
              </span>
            )}
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Tahap saat ini: <span className="text-blue-400 font-mono font-medium">{currentState}</span>
          </p>
        </div>

        {/* Telemetry pill */}
        <div className="flex items-center gap-3 text-xs">
          <div className="rounded-lg bg-slate-900 border border-slate-800 px-2.5 py-1 text-slate-300">
            Loop Review: <span className={`font-semibold ${reviewLoopCount >= 3 ? "text-rose-400" : "text-slate-100"}`}>{reviewLoopCount}/3</span>
          </div>
          <div className="rounded-lg bg-slate-900 border border-slate-800 px-2.5 py-1 text-slate-300">
            Coverage: <span className={`font-semibold ${coveragePercentage >= 70 ? "text-emerald-400" : coveragePercentage > 0 ? "text-amber-400" : "text-slate-400"}`}>{coveragePercentage}%</span>
          </div>
        </div>
      </div>

      {/* Horizontal Scroll on Mobile, Grid/Flex on Desktop */}
      <div className="relative">
        <div className="flex items-center gap-2 sm:gap-3 overflow-x-auto pb-3 pt-1 scrollbar-thin no-scrollbar">
          {PHASES.map((phase, idx) => {
            const isCompleted = idx < currentIndex;
            const isCurrent = idx === currentIndex;
            const isFuture = idx > currentIndex;

            let cardBg = "bg-slate-900/60 border-slate-800/80 text-slate-400";
            let iconBg = "bg-slate-800 text-slate-400";

            if (isCompleted) {
              cardBg = "bg-emerald-950/20 border-emerald-500/30 text-emerald-300";
              iconBg = "bg-emerald-500/20 text-emerald-400";
            } else if (isCurrent) {
              if (isDeadlock) {
                cardBg = "bg-rose-950/40 border-rose-500 text-rose-300 ring-2 ring-rose-500/30 animate-pulse";
                iconBg = "bg-rose-500 text-white";
              } else if (isWaitingHuman) {
                cardBg = "bg-amber-950/30 border-amber-500 text-amber-300 ring-2 ring-amber-500/30";
                iconBg = "bg-amber-500 text-slate-900 font-bold";
              } else {
                cardBg = "bg-blue-950/40 border-blue-500 text-blue-200 ring-2 ring-blue-500/30";
                iconBg = "bg-blue-600 text-white";
              }
            }

            return (
              <div
                key={phase.state}
                className={`flex-shrink-0 flex items-center gap-2.5 rounded-xl border px-3 py-2 transition-all ${cardBg}`}
                style={{ minWidth: "135px" }}
              >
                <div className={`flex h-7 w-7 items-center justify-center rounded-lg ${iconBg}`}>
                  {phase.icon}
                </div>
                <div className="overflow-hidden">
                  <div className="text-[11px] font-semibold tracking-tight truncate">
                    {phase.shortLabel}
                  </div>
                  <div className="text-[10px] opacity-75 truncate">{phase.name}</div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
