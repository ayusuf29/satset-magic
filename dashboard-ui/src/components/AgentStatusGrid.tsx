"use client";

import React from "react";
import { WorkflowState } from "@/lib/types";
import { Users, Code, Terminal, Activity, ShieldAlert } from "lucide-react";

interface AgentStatusGridProps {
  currentState: WorkflowState;
}

interface TeamCard {
  name: string;
  category: string;
  leadRole: string;
  members: string[];
  icon: React.ReactNode;
  activeInPhases: WorkflowState[];
}

const TEAMS: TeamCard[] = [
  {
    name: "Product & Design",
    category: "Domain 1 & 2",
    leadRole: "PO / BA & UI/UX Specialist",
    members: ["Spec Writer Agent", "UI/UX Pro Max Agent"],
    icon: <Users className="h-5 w-5 text-blue-400" />,
    activeInPhases: ["PHASE_0_REQUIREMENT_DISCOVERY", "PHASE_0_WAITING_HUMAN_SIGN_OFF", "PHASE_2_BACKLOG_GENERATION"],
  },
  {
    name: "Development Team",
    category: "Domain 3, 4 & 7",
    leadRole: "Fullstack Engineers & Review Lead",
    members: ["Spring Boot Worker", "Frontend Next.js Worker", "Senior Reviewer"],
    icon: <Code className="h-5 w-5 text-emerald-400" />,
    activeInPhases: ["PHASE_4_TDD_PLANNING", "PHASE_5_DEVELOPMENT_IMPLEMENTATION", "PHASE_6_CODE_REVIEW", "PHASE_6_DEADLOCK_WAITING_HUMAN"],
  },
  {
    name: "CI/CD & Staging",
    category: "Domain 8 & 9",
    leadRole: "DevOps Runner & Sandbox Platform",
    members: ["Docker Compose Pro", "Ephemeral Staging Runner"],
    icon: <Terminal className="h-5 w-5 text-indigo-400" />,
    activeInPhases: ["PHASE_3_TASK_DISPATCHING", "PHASE_7_QA_VERIFICATION"],
  },
  {
    name: "Production & SRE",
    category: "Domain 10",
    leadRole: "Reliability & Distributed Telemetry",
    members: ["OTel Instrumentor", "Prometheus/Grafana Sentinel"],
    icon: <Activity className="h-5 w-5 text-amber-400" />,
    activeInPhases: ["PHASE_9_RELEASED"],
  },
  {
    name: "Shared Enabling",
    category: "Domain 5, 6 & 9",
    leadRole: "Architect & Pentest Specialists",
    members: ["System Design Architect", "AppSec Auditor", "DAST/MAST Pentest Scout"],
    icon: <ShieldAlert className="h-5 w-5 text-rose-400" />,
    activeInPhases: ["PHASE_1_ARCHITECTURE_AND_DESIGN", "PHASE_8_READY_FOR_PRODUCTION_GATE"],
  },
];

export function AgentStatusGrid({ currentState }: AgentStatusGridProps) {
  return (
    <div className="w-full">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-white flex items-center gap-2">
          <span>5-Team Topology Status</span>
          <span className="text-xs text-slate-400 font-normal hidden sm:inline">(Live Autonomous Allocation)</span>
        </h3>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-3">
        {TEAMS.map((team) => {
          const isActive = team.activeInPhases.includes(currentState);
          const isDeadlock = currentState === "PHASE_6_DEADLOCK_WAITING_HUMAN" && team.name === "Development Team";

          let statusBadge = "IDLE";
          let badgeColor = "bg-slate-800 text-slate-400 border-slate-700";

          if (isDeadlock) {
            statusBadge = "DEADLOCK";
            badgeColor = "bg-rose-500/20 text-rose-400 border-rose-500/40 animate-pulse";
          } else if (isActive) {
            statusBadge = "ACTIVE";
            badgeColor = "bg-emerald-500/20 text-emerald-400 border-emerald-500/40 animate-pulse";
          }

          return (
            <div
              key={team.name}
              className={`rounded-2xl p-4 glass-card border transition-all ${
                isActive
                  ? "border-blue-500/50 bg-slate-900/90 shadow-lg shadow-blue-500/10"
                  : "border-slate-800/80 bg-slate-950/40"
              }`}
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-800/80 border border-slate-700/60">
                    {team.icon}
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-white tracking-tight">{team.name}</h4>
                    <span className="text-[10px] text-slate-400">{team.category}</span>
                  </div>
                </div>

                <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[10px] font-semibold ${badgeColor}`}>
                  {statusBadge}
                </span>
              </div>

              <div className="mt-3 pt-2.5 border-t border-slate-800/60 text-[11px] text-slate-300">
                <div className="font-medium text-slate-200">{team.leadRole}</div>
                <div className="mt-1 flex flex-wrap gap-1">
                  {team.members.map((m) => (
                    <span
                      key={m}
                      className="inline-block rounded bg-slate-800/60 px-1.5 py-0.5 text-[10px] text-slate-400"
                    >
                      {m}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
