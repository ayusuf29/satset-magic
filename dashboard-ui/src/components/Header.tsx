"use client";

import React from "react";
import { Activity, Bell, Plus, ShieldCheck, Cpu, LayoutDashboard, GitBranch, Users, Terminal, Globe } from "lucide-react";
import { TabKey } from "./BottomNav";
import { useLanguage } from "@/lib/i18n";

interface HeaderProps {
  activeTab: TabKey;
  onTabChange: (tab: TabKey) => void;
  onOpenNewTaskModal: () => void;
  pendingApprovalsCount: number;
}

export function Header({
  activeTab,
  onTabChange,
  onOpenNewTaskModal,
  pendingApprovalsCount,
}: HeaderProps) {
  const { language, setLanguage, t } = useLanguage();

  const navTabs: { key: TabKey; label: string; icon: React.ReactNode; badge?: number }[] = [
    { key: "overview", label: t.nav.overview, icon: <LayoutDashboard className="h-3.5 w-3.5" /> },
    { key: "pipeline", label: t.nav.pipeline, icon: <GitBranch className="h-3.5 w-3.5" />, badge: pendingApprovalsCount },
    { key: "agents", label: t.nav.agents, icon: <Users className="h-3.5 w-3.5" /> },
    { key: "logs", label: t.nav.logs, icon: <Terminal className="h-3.5 w-3.5" /> },
  ];

  return (
    <header className="sticky top-0 z-40 w-full glass-panel border-b border-slate-800/80 px-4 py-2.5 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-[1720px] w-full flex-wrap items-center justify-between gap-3">
        {/* Brand / Logo */}
        <div className="flex items-center gap-3">
          <div className="relative flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-amber-500 shadow-lg shadow-blue-500/20">
            <Cpu className="h-4.5 w-4.5 text-white" />
            <span className="absolute -top-1 -right-1 flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
            </span>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base sm:text-lg font-bold tracking-tight text-white">
                {t.header.title} <span className="text-blue-400 font-extrabold">{t.header.titleHighlight}</span>
              </h1>
              <span className="inline-flex items-center rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-medium text-emerald-400 border border-emerald-500/20">
                {t.header.liveEngine}
              </span>
            </div>
            <p className="text-[11px] text-slate-400 hidden md:block">
              {t.header.subtitle}
            </p>
          </div>
        </div>

        {/* Center Navigation Tabs (Responsive for Laptop, Desktop & Tablet) */}
        <nav className="flex items-center gap-1 bg-slate-900/80 p-1 rounded-xl border border-slate-800 order-3 sm:order-2 overflow-x-auto">
          {navTabs.map((tab) => {
            const isActive = activeTab === tab.key;
            return (
              <button
                key={tab.key}
                onClick={() => onTabChange(tab.key)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition whitespace-nowrap ${
                  isActive
                    ? "bg-blue-600 text-white shadow-md shadow-blue-600/30 font-semibold"
                    : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/60"
                }`}
              >
                {tab.icon}
                <span>{tab.label}</span>
                {tab.badge && tab.badge > 0 ? (
                  <span className="flex h-4 w-4 items-center justify-center rounded-full bg-amber-400 text-[10px] font-bold text-slate-950">
                    {tab.badge}
                  </span>
                ) : null}
              </button>
            );
          })}
        </nav>

        {/* System Telemetry, Language Switcher & Quick Action */}
        <div className="flex items-center gap-2 sm:gap-3 order-2 sm:order-3">
          {/* Telemetry pill (desktop / laptop) */}
          <div className="hidden xl:flex items-center gap-2.5 rounded-lg bg-slate-900/90 border border-slate-800 px-3 py-1.5 text-xs text-slate-300">
            <div className="flex items-center gap-1.5">
              <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
              <span>{t.header.hitlGateActive}</span>
            </div>
            <span className="h-3 w-px bg-slate-700"></span>
            <div className="flex items-center gap-1.5">
              <Activity className="h-3.5 w-3.5 text-blue-400 animate-pulse" />
              <span>{t.header.zgcThreads}</span>
            </div>
          </div>

          {/* Pending HITL Indicator */}
          {pendingApprovalsCount > 0 && (
            <div className="flex items-center gap-1.5 rounded-lg bg-amber-500/10 border border-amber-500/30 px-2.5 py-1 text-xs font-semibold text-amber-400 animate-pulse-fast">
              <Bell className="h-3.5 w-3.5" />
              <span>
                {pendingApprovalsCount}{" "}
                {pendingApprovalsCount === 1 ? t.header.approvalSingular : t.header.approvalPlural}
              </span>
            </div>
          )}

          {/* Language Switcher Button */}
          <div
            className="flex items-center gap-1 rounded-xl bg-slate-900/90 border border-slate-800 p-1 pl-2 text-xs shadow-inner"
            role="group"
            aria-label={t.header.switchLanguage}
          >
            <Globe className="h-3.5 w-3.5 text-slate-400 hidden sm:block" />
            <button
              type="button"
              onClick={() => setLanguage("en")}
              className={`px-2 py-1 rounded-lg text-xs font-semibold transition-all duration-150 ${
                language === "en"
                  ? "bg-blue-600 text-white shadow-sm shadow-blue-600/40"
                  : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/40"
              }`}
              title="English (Default)"
              aria-pressed={language === "en"}
            >
              EN
            </button>
            <button
              type="button"
              onClick={() => setLanguage("id")}
              className={`px-2 py-1 rounded-lg text-xs font-semibold transition-all duration-150 ${
                language === "id"
                  ? "bg-blue-600 text-white shadow-sm shadow-blue-600/40"
                  : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/40"
              }`}
              title="Bahasa Indonesia"
              aria-pressed={language === "id"}
            >
              ID
            </button>
          </div>

          {/* New Workflow Button */}
          <button
            onClick={onOpenNewTaskModal}
            className="inline-flex items-center justify-center gap-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 px-3.5 py-1.5 sm:py-2 text-xs sm:text-sm font-semibold text-white shadow-md shadow-blue-600/30 transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-blue-400"
          >
            <Plus className="h-4 w-4" />
            <span>{t.header.newTask}</span>
          </button>
        </div>
      </div>
    </header>
  );
}
