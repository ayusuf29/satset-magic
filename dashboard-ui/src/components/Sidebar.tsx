"use client";

import React from "react";
import { TabKey } from "./BottomNav";
import { LayoutDashboard, GitBranch, Users, Terminal, Cpu, PlusCircle } from "lucide-react";

interface SidebarProps {
  activeTab: TabKey;
  onTabChange: (tab: TabKey) => void;
  onOpenNewTaskModal: () => void;
  pendingApprovalsCount: number;
}

export function Sidebar({
  activeTab,
  onTabChange,
  onOpenNewTaskModal,
  pendingApprovalsCount,
}: SidebarProps) {
  const navItems = [
    { key: "overview" as TabKey, label: "Overview", icon: <LayoutDashboard className="h-4 w-4" /> },
    { key: "pipeline" as TabKey, label: "SDLC Pipeline", icon: <GitBranch className="h-4 w-4" />, badge: pendingApprovalsCount },
    { key: "agents" as TabKey, label: "Agent Teams", icon: <Users className="h-4 w-4" /> },
    { key: "logs" as TabKey, label: "Live Console", icon: <Terminal className="h-4 w-4" /> },
  ];

  return (
    <aside className="hidden md:flex flex-col w-60 border-r border-slate-800/80 glass-panel p-4 flex-shrink-0 min-h-screen">
      <div className="flex items-center gap-2.5 px-2 py-3 mb-6">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600 text-white shadow-md shadow-blue-500/20">
          <Cpu className="h-4 w-4" />
        </div>
        <div>
          <div className="text-sm font-bold text-white tracking-tight">Satset Hub</div>
          <div className="text-[10px] text-slate-400">Enterprise Engine</div>
        </div>
      </div>

      <div className="mb-4">
        <button
          onClick={onOpenNewTaskModal}
          className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-3.5 py-2.5 text-xs font-semibold text-white shadow-md shadow-blue-600/30 hover:brightness-110 active:scale-95 transition"
        >
          <PlusCircle className="h-4 w-4" />
          <span>Mulai Task Baru</span>
        </button>
      </div>

      <nav className="flex-1 space-y-1">
        {navItems.map((item) => {
          const isActive = activeTab === item.key;
          return (
            <button
              key={item.key}
              onClick={() => onTabChange(item.key)}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-medium transition ${
                isActive
                  ? "bg-blue-600/20 border border-blue-500/30 text-blue-300"
                  : "text-slate-400 hover:text-slate-200 hover:bg-slate-900/60"
              }`}
            >
              <div className="flex items-center gap-2.5">
                {item.icon}
                <span>{item.label}</span>
              </div>
              {item.badge && item.badge > 0 ? (
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-amber-500 text-[10px] font-bold text-slate-900">
                  {item.badge}
                </span>
              ) : null}
            </button>
          );
        })}
      </nav>

      {/* Footer Info */}
      <div className="pt-4 border-t border-slate-800 text-[11px] text-slate-500 px-2 space-y-1">
        <div>Java 21 • Next.js 14</div>
        <div>Postgres State Checkpointer</div>
        <div className="text-emerald-400">● 10 Skills Mounted</div>
      </div>
    </aside>
  );
}
