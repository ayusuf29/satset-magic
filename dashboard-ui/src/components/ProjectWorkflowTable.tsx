"use client";

import React, { useState, useMemo, useEffect } from "react";
import { WorkflowSummary, WorkflowState } from "@/lib/types";
import { ProjectFilterStatus } from "./DashboardSummaryKPIs";
import { useLanguage, Translations } from "@/lib/i18n";
import {
  Search,
  ChevronLeft,
  ChevronRight,
  Eye,
  Trash2,
  FolderGit2,
  AlertTriangle,
  Loader2,
  CheckCircle2,
  RefreshCw,
  X,
  Shield,
  Clock,
  Layers,
  ArrowRight,
  Sparkles,
} from "lucide-react";

interface ProjectWorkflowTableProps {
  workflows: WorkflowSummary[];
  selectedWorkflowId: string | null;
  onSelectWorkflow: (id: string) => void;
  onDeleteWorkflow: (id: string) => Promise<void>;
  loading: boolean;
  onRefresh: () => void;
  activeFilter: ProjectFilterStatus;
  onFilterChange: (filter: ProjectFilterStatus) => void;
}

const ITEMS_PER_PAGE = 5;

export function ProjectWorkflowTable({
  workflows,
  selectedWorkflowId,
  onSelectWorkflow,
  onDeleteWorkflow,
  loading,
  onRefresh,
  activeFilter,
  onFilterChange,
}: ProjectWorkflowTableProps) {
  const { language, t } = useLanguage();
  const [searchQuery, setSearchQuery] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [deleteCandidate, setDeleteCandidate] = useState<WorkflowSummary | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // 1. Filter data berdasarkan status KPI & query pencarian
  const filteredWorkflows = useMemo(() => {
    return workflows.filter((w) => {
      // Filter tab status
      if (activeFilter === "RUNNING") {
        const isRunning =
          w.agentRunning ||
          (!w.waitingForHuman && w.currentState !== "PHASE_9_RELEASED");
        if (!isRunning) return false;
      } else if (activeFilter === "WAITING_HITL") {
        if (!w.waitingForHuman) return false;
      } else if (activeFilter === "COMPLETED") {
        if (w.currentState !== "PHASE_9_RELEASED") return false;
      }

      // Filter query pencarian (Project ID, taskName, client, ID, deskripsi)
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchProject = w.projectId?.toLowerCase().includes(q);
        const matchTask = w.taskName?.toLowerCase().includes(q);
        const matchClient = w.clientId?.toLowerCase().includes(q);
        const matchId = w.id?.toLowerCase().includes(q);
        const matchDesc = w.description?.toLowerCase().includes(q);
        if (!matchProject && !matchTask && !matchClient && !matchId && !matchDesc) {
          return false;
        }
      }

      return true;
    });
  }, [workflows, activeFilter, searchQuery]);

  // Reset page ke 1 setiap kali filter atau query search berubah
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, activeFilter]);

  // Kalkulasi pagination (Max 5 per page)
  const totalPages = Math.max(1, Math.ceil(filteredWorkflows.length / ITEMS_PER_PAGE));
  const validCurrentPage = Math.min(currentPage, totalPages);

  const paginatedWorkflows = useMemo(() => {
    const startIndex = (validCurrentPage - 1) * ITEMS_PER_PAGE;
    return filteredWorkflows.slice(startIndex, startIndex + ITEMS_PER_PAGE);
  }, [filteredWorkflows, validCurrentPage]);

  // Eksekusi konfirmasi hapus proyek
  const handleConfirmDelete = async () => {
    if (!deleteCandidate) return;
    try {
      setIsDeleting(true);
      await onDeleteWorkflow(deleteCandidate.id);
      setDeleteCandidate(null);
    } catch (err: any) {
      alert(t.table.deleteFailedAlert(err?.message || err));
    } finally {
      setIsDeleting(false);
    }
  };

  // Helper render status badge
  const renderStatusBadge = (wf: WorkflowSummary) => {
    const isDeadlock = wf.currentState === "PHASE_6_DEADLOCK_WAITING_HUMAN";
    const isWaiting = wf.waitingForHuman;

    if (isDeadlock) {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-rose-500/15 text-rose-300 border border-rose-500/40 shadow-sm shadow-rose-950/50 animate-pulse whitespace-nowrap">
          <AlertTriangle className="h-3 w-3 text-rose-400" />
          DEADLOCK ({language === "en" ? "ESCALATION" : "ESKALASI"})
        </span>
      );
    }

    if (wf.agentRunning) {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-cyan-500/15 text-cyan-300 border border-cyan-500/40 shadow-sm shadow-cyan-950/50 animate-pulse whitespace-nowrap">
          <Loader2 className="h-3 w-3 text-cyan-400 animate-spin" />
          WORKING ({wf.phaseProgressPercentage || 0}%)
        </span>
      );
    }

    if (isWaiting) {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/15 text-amber-300 border border-amber-500/40 shadow-sm shadow-amber-950/50 whitespace-nowrap">
          <Shield className="h-3 w-3 text-amber-400" />
          {language === "en" ? "WAITING HITL" : "MENUNGGU HITL"}
        </span>
      );
    }

    if (wf.currentState === "PHASE_9_RELEASED") {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 whitespace-nowrap">
          <CheckCircle2 className="h-3 w-3 text-emerald-400" />
          {language === "en" ? "RELEASED" : "RELEASED / SELESAI"}
        </span>
      );
    }

    // Default Running Phase
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium bg-blue-500/15 text-blue-300 border border-blue-500/30 whitespace-nowrap">
        <Clock className="h-3 w-3 text-blue-400" />
        {formatPhaseLabel(wf.currentState, t)}
      </span>
    );
  };

  return (
    <section className="space-y-4">
      {/* Header Bar: Title, Search Input, Filter Tabs & Refresh */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3 bg-slate-950/70 border border-slate-800/80 rounded-2xl p-3.5 sm:p-4 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-xl bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
            <Layers className="h-4 w-4" />
          </div>
          <div>
            <h2 className="text-sm sm:text-base font-bold text-white flex items-center gap-2">
              <span>{t.table.title}</span>
              <span className="rounded-full bg-slate-800 px-2 py-0.5 text-xs text-cyan-300 font-mono border border-slate-700">
                {filteredWorkflows.length} {t.kpi.projectsUnit}
              </span>
            </h2>
            <p className="text-[11px] text-slate-400 hidden sm:block">
              {t.table.subtitle}
            </p>
          </div>
        </div>

        {/* Search Bar & Filter Options */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Quick Filter Buttons */}
          <div className="flex items-center bg-slate-900 border border-slate-800 rounded-xl p-0.5 text-xs">
            {(
              [
                { key: "ALL", label: t.table.filterAll },
                { key: "RUNNING", label: t.table.filterRunning },
                { key: "WAITING_HITL", label: t.table.filterWaitingHitl },
                { key: "COMPLETED", label: t.table.filterCompleted },
              ] as const
            ).map((tab) => (
              <button
                key={tab.key}
                onClick={() => onFilterChange(tab.key)}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-medium transition ${
                  activeFilter === tab.key
                    ? "bg-cyan-500 text-slate-950 font-bold shadow-sm"
                    : "text-slate-400 hover:text-slate-200"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Search Box */}
          <div className="relative flex-1 sm:flex-initial">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
            <input
              type="text"
              placeholder={t.table.searchPlaceholder}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="bg-slate-900 border border-slate-800 rounded-xl pl-8 pr-7 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-cyan-500 w-full sm:w-56 md:w-64 transition"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery("")}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
                title={language === "en" ? "Clear search" : "Hapus pencarian"}
              >
                <X className="h-3 w-3" />
              </button>
            )}
          </div>

          {/* Refresh Button */}
          <button
            onClick={onRefresh}
            className="p-2 rounded-xl text-slate-400 hover:text-white bg-slate-900 hover:bg-slate-800 border border-slate-800 transition"
            title={t.table.refresh}
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin text-cyan-400" : ""}`} />
          </button>
        </div>
      </div>

      {/* Main Table Container */}
      <div className="overflow-hidden rounded-2xl border border-slate-800/90 bg-slate-950/60 shadow-xl backdrop-blur-md">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800/90 bg-slate-900/60 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                <th scope="col" className="py-3 px-4 sm:px-6">
                  {t.table.colTask}
                </th>
                <th scope="col" className="py-3 px-4 sm:px-6 w-48 text-center sm:text-left">
                  {t.common.status}
                </th>
                <th scope="col" className="py-3 px-4 sm:px-6 w-44 text-right">
                  {t.table.colActions}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {filteredWorkflows.length === 0 ? (
                <tr>
                  <td colSpan={3} className="py-12 text-center">
                    <div className="space-y-2 max-w-sm mx-auto">
                      <p className="text-sm font-semibold text-slate-300">
                        {t.table.noProjectsFound}
                      </p>
                      <button
                        onClick={() => {
                          onFilterChange("ALL");
                          setSearchQuery("");
                        }}
                        className="text-xs text-cyan-400 hover:underline font-semibold pt-2 inline-block"
                      >
                        {language === "en" ? "Reset Filter & Search" : "Reset Filter & Pencarian"}
                      </button>
                    </div>
                  </td>
                </tr>
              ) : (
                paginatedWorkflows.map((wf) => {
                  const isSelected = wf.id === selectedWorkflowId;
                  const isWaiting = wf.waitingForHuman;

                  return (
                    <tr
                      key={wf.id}
                      className={`transition-all duration-150 ${
                        isSelected
                          ? "bg-cyan-950/30 border-l-4 border-l-cyan-400"
                          : "hover:bg-slate-900/40"
                      }`}
                    >
                      {/* 1. Title Column (Styled visually like attachment) */}
                      <td className="py-3.5 px-4 sm:px-6 align-middle">
                        <div className="flex items-start gap-3">
                          {/* Visual Indicator Icon */}
                          <div
                            className={`flex-shrink-0 mt-0.5 p-2 rounded-xl border ${
                              isWaiting
                                ? "bg-amber-500/10 border-amber-500/30 text-amber-400"
                                : wf.agentRunning
                                ? "bg-cyan-500/10 border-cyan-500/30 text-cyan-400"
                                : wf.currentState === "PHASE_9_RELEASED"
                                ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-400"
                                : "bg-slate-800/60 border-slate-700/60 text-slate-300"
                            }`}
                          >
                            {isWaiting ? (
                              <Shield className="h-4 w-4" />
                            ) : wf.agentRunning ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : wf.currentState === "PHASE_9_RELEASED" ? (
                              <CheckCircle2 className="h-4 w-4" />
                            ) : (
                              <FolderGit2 className="h-4 w-4" />
                            )}
                          </div>

                          {/* Content Block */}
                          <div className="min-w-0 flex-1 space-y-1">
                            {/* Headline & Badges (Matches attachment layout) */}
                            <div className="flex flex-wrap items-center gap-1.5">
                              <span className="font-bold text-xs sm:text-sm text-white tracking-tight hover:text-cyan-300 transition">
                                {isWaiting
                                  ? `${language === "en" ? "REQUIREMENT SIGN-OFF (PHASE 0)" : "SIGN-OFF KEBUTUHAN (PHASE 0)"}: ${wf.taskName}`
                                  : wf.taskName}
                              </span>

                              {/* Project ID Badge */}
                              <span className="inline-flex items-center gap-1 rounded bg-slate-800/90 px-2 py-0.5 text-[11px] font-mono font-bold text-cyan-300 border border-slate-700">
                                {wf.projectId || wf.id}
                              </span>

                              {/* Client ID Badge */}
                              {wf.clientId && (
                                <span className="rounded bg-indigo-950/70 px-2 py-0.5 text-[10px] font-semibold text-indigo-300 border border-indigo-800/50">
                                  {wf.clientId}
                                </span>
                              )}

                              {/* Workflow ID Badge */}
                              <span className="rounded bg-slate-900 px-2 py-0.5 text-[10px] font-mono text-slate-400 border border-slate-800">
                                ID: {wf.id}
                              </span>

                              {isSelected && (
                                <span className="rounded bg-cyan-500/20 px-1.5 py-0.5 text-[10px] font-bold text-cyan-300 border border-cyan-500/40">
                                  🎯 {language === "en" ? "Selected" : "Terpilih"}
                                </span>
                              )}
                            </div>

                            {/* Subtitle / Description */}
                            <p className="text-[11px] sm:text-xs text-slate-400 line-clamp-1 leading-relaxed">
                              {wf.description}
                            </p>

                            {/* Dynamic Live Agent Note if running */}
                            {wf.agentRunning && wf.currentAgentActivity && (
                              <div className="inline-flex items-center gap-1.5 text-[10px] text-cyan-300 bg-cyan-950/50 border border-cyan-800/50 px-2 py-0.5 rounded-md">
                                <Loader2 className="h-2.5 w-2.5 animate-spin text-cyan-400" />
                                <span>
                                  <strong>[{wf.currentAgentRole || "Agent"}]:</strong>{" "}
                                  {wf.currentAgentActivity}
                                </span>
                              </div>
                            )}
                          </div>
                        </div>
                      </td>

                      {/* 2. Status Column */}
                      <td className="py-3.5 px-4 sm:px-6 align-middle">
                        <div className="flex items-center sm:justify-start justify-center">
                          {renderStatusBadge(wf)}
                        </div>
                      </td>

                      {/* 3. Action Buttons (Detail & Delete) */}
                      <td className="py-3.5 px-4 sm:px-6 align-middle text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* Button Detail */}
                          <button
                            onClick={() => {
                              onSelectWorkflow(wf.id);
                              // Smooth scroll ke visual detail pipeline
                              const el = document.getElementById("selected-project-pipeline-detail");
                              if (el) {
                                el.scrollIntoView({ behavior: "smooth", block: "start" });
                              }
                            }}
                            className={`inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-xs font-semibold transition active:scale-95 ${
                              isSelected
                                ? "bg-cyan-500 text-slate-950 font-bold shadow-md shadow-cyan-900/40 ring-1 ring-cyan-300"
                                : "bg-cyan-950/50 hover:bg-cyan-900/60 text-cyan-300 border border-cyan-700/50"
                            }`}
                            title={language === "en" ? "View pipeline & artifacts for this project" : "Tampilkan detail alur & berkas proyek ini"}
                          >
                            <Eye className="h-3.5 w-3.5" />
                            <span>{isSelected ? (language === "en" ? "Viewing" : "Sedang Dilihat") : "Detail"}</span>
                          </button>

                          {/* Button Delete */}
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setDeleteCandidate(wf);
                            }}
                            className="inline-flex items-center gap-1 px-2 py-1.5 rounded-xl text-xs font-semibold bg-rose-950/40 hover:bg-rose-900/60 text-rose-300 hover:text-white border border-rose-800/40 transition active:scale-95"
                            title={language === "en" ? "Delete this project" : "Hapus proyek ini"}
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                            <span className="hidden sm:inline">{t.common.delete}</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar (Max 5 items per page) */}
        {filteredWorkflows.length > 0 && (
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-4 sm:px-6 py-3 bg-slate-900/80 border-t border-slate-800/80 text-xs text-slate-400">
            <div className="flex items-center gap-2">
              <span>
                {t.table.showingRange(
                  (validCurrentPage - 1) * ITEMS_PER_PAGE + 1,
                  Math.min(validCurrentPage * ITEMS_PER_PAGE, filteredWorkflows.length),
                  filteredWorkflows.length
                )}
              </span>
              <span className="text-slate-600 hidden sm:inline">•</span>
              <span className="hidden sm:inline">
                {t.table.pageOf(validCurrentPage, totalPages)}
              </span>
            </div>

            {/* Pagination Controls */}
            <div className="flex items-center gap-1.5">
              <button
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={validCurrentPage <= 1}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg border border-slate-800 bg-slate-950 hover:bg-slate-800 text-slate-300 disabled:opacity-40 disabled:cursor-not-allowed transition"
              >
                <ChevronLeft className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">{t.table.prev}</span>
              </button>

              {/* Page Number Buttons */}
              <div className="flex items-center gap-1">
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => (
                  <button
                    key={pageNum}
                    onClick={() => setCurrentPage(pageNum)}
                    className={`h-7 w-7 rounded-lg text-xs font-semibold transition ${
                      pageNum === validCurrentPage
                        ? "bg-cyan-500 text-slate-950 font-bold shadow-sm"
                        : "bg-slate-950 hover:bg-slate-800 text-slate-400 border border-slate-800"
                    }`}
                  >
                    {pageNum}
                  </button>
                ))}
              </div>

              <button
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={validCurrentPage >= totalPages}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg border border-slate-800 bg-slate-950 hover:bg-slate-800 text-slate-300 disabled:opacity-40 disabled:cursor-not-allowed transition"
              >
                <span className="hidden sm:inline">{t.table.next}</span>
                <ChevronRight className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Confirmation Modal for Project Deletion */}
      {deleteCandidate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-[#0f172a] border border-rose-500/40 rounded-2xl max-w-md w-full p-5 sm:p-6 shadow-2xl shadow-rose-950/60 space-y-4">
            <div className="flex items-start gap-3.5">
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400">
                <Trash2 className="h-6 w-6" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">
                  {t.table.deleteModalTitle}
                </h3>
                <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                  {t.table.deleteModalWarning}
                </p>
              </div>
            </div>

            {/* Target Project Card Info */}
            <div className="rounded-xl bg-slate-900/80 border border-slate-800 p-3 space-y-1.5 text-xs">
              <div className="flex items-center gap-2">
                <span className="font-mono font-bold text-cyan-400">
                  {deleteCandidate.projectId || deleteCandidate.id}
                </span>
                {deleteCandidate.clientId && (
                  <span className="text-[10px] bg-indigo-950 px-1.5 py-0.5 rounded text-indigo-300">
                    {deleteCandidate.clientId}
                  </span>
                )}
              </div>
              <p className="font-semibold text-white line-clamp-1">
                {deleteCandidate.taskName}
              </p>
              <p className="text-[11px] text-slate-400 line-clamp-2">
                {deleteCandidate.description}
              </p>
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setDeleteCandidate(null)}
                disabled={isDeleting}
                className="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
              >
                {t.table.deleteModalCancelBtn}
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-500 text-white shadow-lg shadow-rose-950 transition disabled:opacity-50"
              >
                {isDeleting ? (
                  <>
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    <span>{t.table.deletingBtn}</span>
                  </>
                ) : (
                  <>
                    <Trash2 className="h-3.5 w-3.5" />
                    <span>{t.table.deleteModalConfirmBtn}</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

// Format readable labels for phases
function formatPhaseLabel(state: WorkflowState, t: Translations): string {
  switch (state) {
    case "PHASE_0_REQUIREMENT_DISCOVERY":
      return t.phases.prd.full;
    case "PHASE_0_WAITING_HUMAN_SIGN_OFF":
      return t.phases.signOff.full;
    case "PHASE_1_ARCHITECTURE_AND_DESIGN":
      return t.phases.arch.full;
    case "PHASE_2_BACKLOG_GENERATION":
      return t.phases.backlog.full;
    case "PHASE_3_TASK_DISPATCHING":
      return t.phases.dispatch.full;
    case "PHASE_4_TDD_PLANNING":
      return t.phases.tdd.full;
    case "PHASE_5_DEVELOPMENT_IMPLEMENTATION":
      return t.phases.dev.full;
    case "PHASE_6_CODE_REVIEW":
      return t.phases.review.full;
    case "PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE":
      return t.phases.mergeGate.full;
    case "PHASE_6_DEADLOCK_WAITING_HUMAN":
      return t.phases.deadlock.full;
    case "PHASE_7_QA_VERIFICATION":
      return t.phases.qa.full;
    case "PHASE_8_READY_FOR_PRODUCTION_GATE":
      return t.phases.prodGate.full;
    case "PHASE_9_RELEASED":
      return t.phases.released.full;
    case "FAILED":
      return t.phases.failed.full;
    default:
      return state;
  }
}
