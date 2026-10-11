"use client";

import React from "react";
import { LayoutDashboard, GitBranch, Terminal, ShieldAlert, PlusCircle } from "lucide-react";

export type TabKey = "overview" | "pipeline" | "agents" | "logs";

interface BottomNavProps {
  activeTab: TabKey;
  onTabChange: (tab: TabKey) => void;
  onOpenNewTaskModal: () => void;
  pendingApprovalsCount: number;
}

export function BottomNav({
  activeTab,
  onTabChange,
  onOpenNewTaskModal,
  pendingApprovalsCount,
}: BottomNavProps) {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 border-t border-slate-800/90 bg-[#090d16]/95 backdrop-blur-lg md:hidden pb-safe">
      <div className="grid grid-cols-5 items-center h-16 max-w-md mx-auto px-2">
        <button
          onClick={() => onTabChange("overview")}
          className={`flex flex-col items-center justify-center h-full gap-1 transition ${
            activeTab === "overview" ? "text-blue-400" : "text-slate-400 hover:text-slate-200"
          }`}
        >
          <LayoutDashboard className="h-5 w-5" />
          <span className="text-[10px] font-medium">Overview</span>
        </button>

        <button
          onClick={() => onTabChange("pipeline")}
          className={`flex flex-col items-center justify-center h-full gap-1 transition relative ${
            activeTab === "pipeline" ? "text-blue-400" : "text-slate-400 hover:text-slate-200"
          }`}
        >
          <GitBranch className="h-5 w-5" />
          <span className="text-[10px] font-medium">Pipeline</span>
          {pendingApprovalsCount > 0 && (
            <span className="absolute top-2 right-4 flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
            </span>
          )}
        </button>

        {/* Center Quick Action */}
        <button
          onClick={onOpenNewTaskModal}
          className="flex flex-col items-center justify-center -mt-5"
          aria-label="Buat Task Baru"
        >
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-lg shadow-blue-600/50 active:scale-95 transition">
            <PlusCircle className="h-6 w-6" />
          </div>
          <span className="text-[10px] font-medium text-slate-300 mt-1">New</span>
        </button>

        <button
          onClick={() => onTabChange("agents")}
          className={`flex flex-col items-center justify-center h-full gap-1 transition ${
            activeTab === "agents" ? "text-blue-400" : "text-slate-400 hover:text-slate-200"
          }`}
        >
          <ShieldAlert className="h-5 w-5" />
          <span className="text-[10px] font-medium">Teams</span>
        </button>

        <button
          onClick={() => onTabChange("logs")}
          className={`flex flex-col items-center justify-center h-full gap-1 transition ${
            activeTab === "logs" ? "text-blue-400" : "text-slate-400 hover:text-slate-200"
          }`}
        >
          <Terminal className="h-5 w-5" />
          <span className="text-[10px] font-medium">Console</span>
        </button>
      </div>
    </nav>
  );
}
