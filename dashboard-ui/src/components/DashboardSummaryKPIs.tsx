"use client";

import React from "react";
import { FolderGit2, Zap, AlertTriangle, CheckCircle2, ChevronRight, Loader2 } from "lucide-react";
import { WorkflowSummary } from "@/lib/types";
import { useLanguage } from "@/lib/i18n";

export type ProjectFilterStatus = "ALL" | "RUNNING" | "WAITING_HITL" | "COMPLETED";

interface DashboardSummaryKPIsProps {
  workflows: WorkflowSummary[];
  activeFilter: ProjectFilterStatus;
  onFilterChange: (filter: ProjectFilterStatus) => void;
}

export function DashboardSummaryKPIs({
  workflows,
  activeFilter,
  onFilterChange,
}: DashboardSummaryKPIsProps) {
  const { t } = useLanguage();

  // 1. Calculate metrics
  const totalProjects = workflows.length;

  const runningWorkflows = workflows.filter(
    (w) => w.agentRunning || (!w.waitingForHuman && w.currentState !== "PHASE_9_RELEASED")
  );

  const waitingHitlWorkflows = workflows.filter((w) => w.waitingForHuman);

  const completedWorkflows = workflows.filter(
    (w) => w.currentState === "PHASE_9_RELEASED"
  );

  const kpis = [
    {
      key: "ALL" as ProjectFilterStatus,
      title: t.kpi.totalProjects,
      count: totalProjects,
      description: t.kpi.totalProjectsDesc,
      icon: <FolderGit2 className="h-5 w-5 text-blue-400" />,
      gradient: "from-blue-600/20 via-indigo-600/10 to-transparent",
      borderColor: "border-blue-500/30",
      activeRing: "ring-2 ring-blue-500/60 bg-blue-950/40",
      accentText: "text-blue-400",
      badgeText: t.kpi.allSystemsBadge,
      badgeBg: "bg-blue-500/10 text-blue-300 border-blue-500/20",
    },
    {
      key: "RUNNING" as ProjectFilterStatus,
      title: t.kpi.running,
      count: runningWorkflows.length,
      description: t.kpi.runningDesc(workflows.filter((w) => w.agentRunning).length),
      icon: <Zap className="h-5 w-5 text-cyan-400 animate-pulse" />,
      gradient: "from-cyan-600/20 via-blue-600/10 to-transparent",
      borderColor: "border-cyan-500/30",
      activeRing: "ring-2 ring-cyan-500/60 bg-cyan-950/40",
      accentText: "text-cyan-400",
      badgeText: t.kpi.runningBadge,
      badgeBg: "bg-cyan-500/10 text-cyan-300 border-cyan-500/20",
      isLive: workflows.some((w) => w.agentRunning),
    },
    {
      key: "WAITING_HITL" as ProjectFilterStatus,
      title: t.kpi.waitingHitl,
      count: waitingHitlWorkflows.length,
      description: t.kpi.waitingHitlDesc,
      icon: <AlertTriangle className="h-5 w-5 text-amber-400" />,
      gradient: "from-amber-600/20 via-orange-600/10 to-transparent",
      borderColor: "border-amber-500/40",
      activeRing: "ring-2 ring-amber-500/60 bg-amber-950/40",
      accentText: "text-amber-400",
      badgeText: t.kpi.waitingHitlBadge,
      badgeBg: "bg-amber-500/15 text-amber-300 border-amber-500/30",
      isPulsing: waitingHitlWorkflows.length > 0,
    },
    {
      key: "COMPLETED" as ProjectFilterStatus,
      title: t.kpi.completed,
      count: completedWorkflows.length,
      description: t.kpi.completedDesc,
      icon: <CheckCircle2 className="h-5 w-5 text-emerald-400" />,
      gradient: "from-emerald-600/20 via-teal-600/10 to-transparent",
      borderColor: "border-emerald-500/30",
      activeRing: "ring-2 ring-emerald-500/60 bg-emerald-950/40",
      accentText: "text-emerald-400",
      badgeText: t.kpi.completedBadge,
      badgeBg: "bg-emerald-500/10 text-emerald-300 border-emerald-500/20",
    },
  ];

  return (
    <section className="space-y-2">
      <div className="flex items-center justify-between">
        <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
          {t.kpi.sectionTitle}
        </h2>
        <span className="text-[11px] text-slate-400">
          {t.kpi.sectionSubtitle}
        </span>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {kpis.map((kpi) => {
          const isActive = activeFilter === kpi.key;

          return (
            <button
              key={kpi.key}
              onClick={() => onFilterChange(kpi.key)}
              className={`relative text-left rounded-2xl p-4 glass-card border transition-all duration-200 group overflow-hidden ${
                isActive
                  ? `${kpi.activeRing} ${kpi.borderColor}`
                  : `border-slate-800/80 hover:border-slate-700 bg-slate-950/40 hover:bg-slate-900/60`
              }`}
            >
              {/* Background ambient gradient */}
              <div
                className={`absolute inset-0 bg-gradient-to-br ${kpi.gradient} opacity-40 pointer-events-none group-hover:opacity-70 transition-opacity`}
              />

              <div className="relative z-10 flex flex-col justify-between h-full space-y-3">
                {/* Header: Icon & Badge */}
                <div className="flex items-center justify-between gap-2">
                  <div className="p-2 rounded-xl bg-slate-900/90 border border-slate-800 shadow-sm flex items-center justify-center">
                    {kpi.icon}
                  </div>
                  <span
                    className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold border ${kpi.badgeBg}`}
                  >
                    {kpi.isLive && (
                      <span className="h-1.5 w-1.5 rounded-full bg-cyan-400 animate-ping" />
                    )}
                    {kpi.isPulsing && (
                      <span className="h-1.5 w-1.5 rounded-full bg-amber-400 animate-ping" />
                    )}
                    {kpi.badgeText}
                  </span>
                </div>

                {/* Counter & Title */}
                <div>
                  <div className="flex items-baseline gap-2">
                    <span className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white font-mono">
                      {kpi.count}
                    </span>
                    <span className="text-xs text-slate-400 font-medium">
                      {t.kpi.projectsUnit}
                    </span>
                  </div>
                  <h3 className="mt-1 text-sm font-bold text-slate-100 group-hover:text-white transition">
                    {kpi.title}
                  </h3>
                  <p className="mt-0.5 text-[11px] text-slate-400 line-clamp-1">
                    {kpi.description}
                  </p>
                </div>

                {/* Active Indicator bar */}
                <div className="pt-2 border-t border-slate-800/60 flex items-center justify-between text-[11px]">
                  <span
                    className={`font-medium transition ${
                      isActive ? kpi.accentText : "text-slate-400 group-hover:text-slate-300"
                    }`}
                  >
                    {isActive ? t.kpi.filterActive : t.kpi.viewDetails}
                  </span>
                  <ChevronRight
                    className={`h-3.5 w-3.5 text-slate-400 group-hover:translate-x-0.5 transition-transform ${
                      isActive ? kpi.accentText : ""
                    }`}
                  />
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </section>
  );
}
