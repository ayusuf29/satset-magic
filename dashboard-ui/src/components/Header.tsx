"use client";

import React from "react";
import { Activity, Bell, Plus, ShieldCheck, Cpu } from "lucide-react";

interface HeaderProps {
  onOpenNewTaskModal: () => void;
  pendingApprovalsCount: number;
}

export function Header({ onOpenNewTaskModal, pendingApprovalsCount }: HeaderProps) {
  return (
    <header className="sticky top-0 z-40 w-full glass-panel border-b border-slate-800/80 px-4 py-3 sm:px-6">
      <div className="mx-auto flex max-w-7xl items-center justify-between">
        {/* Brand / Logo */}
        <div className="flex items-center gap-3">
          <div className="relative flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-amber-500 shadow-lg shadow-blue-500/20">
            <Cpu className="h-5 w-5 text-white" />
            <span className="absolute -top-1 -right-1 flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
            </span>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base sm:text-lg font-bold tracking-tight text-white">
                Satset <span className="text-blue-400 font-extrabold">Orchestrator</span>
              </h1>
              <span className="hidden sm:inline-flex items-center rounded-full bg-emerald-500/10 px-2 py-0.5 text-xs font-medium text-emerald-400 border border-emerald-500/20">
                Live Engine
              </span>
            </div>
            <p className="text-[11px] text-slate-400 hidden sm:block">
              Multi-Agent Autonomous Command & Governance Hub
            </p>
          </div>
        </div>

        {/* System Telemetry & Quick Action */}
        <div className="flex items-center gap-2 sm:gap-4">
          {/* Telemetry pill (desktop only) */}
          <div className="hidden lg:flex items-center gap-3 rounded-lg bg-slate-900/90 border border-slate-800 px-3 py-1.5 text-xs text-slate-300">
            <div className="flex items-center gap-1.5">
              <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
              <span>HITL Gate: Active</span>
            </div>
            <span className="h-3 w-px bg-slate-700"></span>
            <div className="flex items-center gap-1.5">
              <Activity className="h-3.5 w-3.5 text-blue-400 animate-pulse" />
              <span>ZGC & Virtual Threads: 21</span>
            </div>
          </div>

          {/* Pending HITL Indicator */}
          {pendingApprovalsCount > 0 && (
            <div className="flex items-center gap-1.5 rounded-lg bg-amber-500/10 border border-amber-500/30 px-2.5 py-1 text-xs font-semibold text-amber-400 animate-pulse-fast">
              <Bell className="h-3.5 w-3.5" />
              <span>{pendingApprovalsCount} Approval</span>
            </div>
          )}

          {/* New Workflow Button */}
          <button
            onClick={onOpenNewTaskModal}
            className="touch-target inline-flex items-center justify-center gap-1.5 rounded-xl bg-blue-600 px-3.5 py-2 text-xs sm:text-sm font-semibold text-white shadow-md shadow-blue-600/30 transition hover:bg-blue-500 active:scale-95 focus:outline-none focus:ring-2 focus:ring-blue-400"
          >
            <Plus className="h-4 w-4" />
            <span className="hidden xs:inline">Task Baru</span>
          </button>
        </div>
      </div>
    </header>
  );
}
