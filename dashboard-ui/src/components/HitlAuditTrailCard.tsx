"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import { createPortal } from "react-dom";
import { WorkflowSummary, WorkflowTransitionLog, WorkflowState } from "@/lib/types";
import { fetchWorkflowDetail, revertWorkflowPhase } from "@/lib/api";
import { useLanguage } from "@/lib/i18n";
import {
  ShieldCheck,
  CheckCircle2,
  XCircle,
  RotateCcw,
  UserCheck,
  MessageSquare,
  Clock,
  Filter,
  ArrowRight,
  Send,
  Loader2,
  Sparkles,
  ChevronDown,
  AlertTriangle,
  SendHorizontal,
  Bot,
  ExternalLink,
} from "lucide-react";

interface HitlAuditTrailCardProps {
  workflow: WorkflowSummary;
  onRefresh?: () => void;
}

export function HitlAuditTrailCard({ workflow, onRefresh }: HitlAuditTrailCardProps) {
  const { language, t } = useLanguage();
  const [history, setHistory] = useState<WorkflowTransitionLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterMode, setFilterMode] = useState<"HITL_ONLY" | "ALL">("HITL_ONLY");

  // Revert Modal State
  const [isRevertModalOpen, setIsRevertModalOpen] = useState(false);
  const [targetPhase, setTargetPhase] = useState<string>("PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE");
  const [revertReason, setRevertReason] = useState("");
  const [reverting, setReverting] = useState(false);
  const [revertSuccessMsg, setRevertSuccessMsg] = useState<string | null>(null);

  const loadHistory = useCallback(async () => {
    try {
      const detail = await fetchWorkflowDetail(workflow.id);
      if (detail && detail.history) {
        setHistory(detail.history);
      }
    } catch (err) {
      console.error("Failed to load transition history:", err);
    } finally {
      setLoading(false);
    }
  }, [workflow.id]);

  useEffect(() => {
    loadHistory();
    const interval = setInterval(loadHistory, 3000);
    return () => clearInterval(interval);
  }, [loadHistory, workflow.updatedAt]);

  // Determine if a log entry is a Human-in-the-Loop decision
  const isHitlEntry = (log: WorkflowTransitionLog) => {
    const actor = (log.actor || "").toUpperCase();
    const msg = (log.message || "").toLowerCase();
    return (
      actor.includes("HUMAN") ||
      actor.includes("TELEGRAM") ||
      actor.includes("OPERATOR") ||
      msg.includes("sign-off approved") ||
      msg.includes("authorized:") ||
      msg.includes("rejected") ||
      msg.includes("ditolak") ||
      msg.includes("reverted") ||
      msg.includes("dikembalikan")
    );
  };

  const filteredHistory = useMemo(() => {
    const sorted = [...history].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
    if (filterMode === "HITL_ONLY") {
      return sorted.filter(isHitlEntry);
    }
    return sorted;
  }, [history, filterMode]);

  const hitlCount = useMemo(() => history.filter(isHitlEntry).length, [history]);

  // Parse decision category & styling
  const getDecisionMeta = (log: WorkflowTransitionLog) => {
    const msg = log.message || "";
    const lower = msg.toLowerCase();
    const actor = log.actor || "";

    if (lower.includes("reverted") || lower.includes("dikembalikan") || lower.includes("returned")) {
      return {
        label: t.hitl.decisionReverted,
        type: "REVERTED",
        badgeClass: "bg-cyan-500/15 text-cyan-400 border-cyan-500/40 ring-1 ring-cyan-500/20",
        icon: <RotateCcw className="h-3.5 w-3.5 text-cyan-400" />,
        quoteBorder: "border-l-cyan-500 bg-cyan-950/20",
      };
    }

    if (
      lower.includes("reject") ||
      lower.includes("ditolak")
    ) {
      return {
        label: t.hitl.decisionRejected,
        type: "REJECTED",
        badgeClass: "bg-rose-500/15 text-rose-400 border-rose-500/40 ring-1 ring-rose-500/20",
        icon: <XCircle className="h-3.5 w-3.5 text-rose-400" />,
        quoteBorder: "border-l-rose-500 bg-rose-950/25",
      };
    }

    if (
      lower.includes("approved") ||
      lower.includes("authorized") ||
      lower.includes("disetujui") ||
      lower.includes("sign-off approved")
    ) {
      return {
        label: t.hitl.decisionApproved,
        type: "APPROVED",
        badgeClass: "bg-emerald-500/15 text-emerald-400 border-emerald-500/40 ring-1 ring-emerald-500/20",
        icon: <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400" />,
        quoteBorder: "border-l-emerald-500 bg-emerald-950/20",
      };
    }

    return {
      label: t.hitl.decisionAutonomous,
      type: "AUTONOMOUS",
      badgeClass: "bg-slate-500/15 text-slate-300 border-slate-600/40",
      icon: <Bot className="h-3.5 w-3.5 text-slate-400" />,
      quoteBorder: "border-l-slate-600 bg-slate-900/30",
    };
  };

  // Extract author clean name
  const formatActor = (actor: string) => {
    if (actor.startsWith("TELEGRAM:@")) {
      return actor.replace("TELEGRAM:", "");
    }
    if (actor.startsWith("TELEGRAM:")) {
      return "@" + actor.replace("TELEGRAM:", "");
    }
    if (actor === "HUMAN_OPERATOR") {
      return "Lead Supervisor (Dashboard)";
    }
    if (actor === "AUTONOMOUS_ORCHESTRATOR") {
      return "Autonomous Orchestrator";
    }
    return actor;
  };

  // Extract clean comment from formatted message
  const extractCommentAndPrefix = (rawMessage: string) => {
    const delimiters = [
      "Requirement sign-off approved: ",
      "Protected branch merge to main authorized: ",
      "Merge rejected and returned to Dev: ",
      "Deadlock overridden and approved by human: ",
      "Deadlock confirmed. Dev resetting with human guidance: ",
      "Workflow manually reverted: ",
      "Workflow reverted: ",
      "Requirement rejected, clarification requested: ",
      "Dikembalikan untuk revisi oleh ",
      "Disetujui oleh ",
    ];

    for (const d of delimiters) {
      if (rawMessage.includes(d)) {
        const parts = rawMessage.split(d);
        return {
          prefix: d.trim(),
          comment: parts.slice(1).join(d).trim(),
        };
      }
    }

    return {
      prefix: "",
      comment: rawMessage,
    };
  };

  const handleExecuteRevert = async () => {
    try {
      setReverting(true);
      await revertWorkflowPhase(
        workflow.id,
        targetPhase,
        revertReason || "Reverted to Phase by Human Supervisor",
        "HUMAN_OPERATOR"
      );
      setRevertSuccessMsg("Status alur kerja berhasil dikembalikan ke " + targetPhase);
      setTimeout(() => {
        setRevertSuccessMsg(null);
        setIsRevertModalOpen(false);
        setRevertReason("");
        loadHistory();
        if (onRefresh) onRefresh();
      }, 1200);
    } catch (err: any) {
      alert("Error: " + (err?.message || err));
    } finally {
      setReverting(false);
    }
  };

  const phaseOptions = [
    { value: "PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE", label: "Phase 6: Awaiting Human Code Review & Merge (Human Gate)" },
    { value: "PHASE_6_CODE_REVIEW", label: "Phase 6: Triad Council Code Review (Re-run 3 AI Reviewers)" },
    { value: "PHASE_5_DEVELOPMENT_IMPLEMENTATION", label: "Phase 5: Dev Implementation (Engineering & Coding)" },
    { value: "PHASE_4_TDD_PLANNING", label: "Phase 4: TDD Planning & Test Suites Specs" },
    { value: "PHASE_3_TASK_DISPATCHING", label: "Phase 3: Task Allocation & Dispatcher" },
    { value: "PHASE_2_BACKLOG_GENERATION", label: "Phase 2: Backlog & User Stories Generation" },
    { value: "PHASE_1_ARCHITECTURE_AND_DESIGN", label: "Phase 1: Architecture, C4 Model & ADRs" },
    { value: "PHASE_0_WAITING_HUMAN_SIGN_OFF", label: "Phase 0: Awaiting Human Requirement Sign-Off (Human Gate)" },
    { value: "PHASE_0_REQUIREMENT_DISCOVERY", label: "Phase 0: Requirement Discovery & Domain Analysis" },
  ];

  return (
    <div className="rounded-2xl glass-panel p-5 border border-slate-800/80 shadow-xl space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800/80">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-500/15 border border-purple-500/30 text-purple-400 shadow-md shadow-purple-500/10">
            <UserCheck className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm sm:text-base font-bold text-white tracking-tight">
                {t.hitl.historyTitle}
              </h3>
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-500/20 text-purple-300 border border-purple-500/40">
                {hitlCount} Actions
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              {t.hitl.historySubtitle}
            </p>
          </div>
        </div>

        {/* Filter Toggle & Revert Phase Button */}
        <div className="flex items-center gap-2 flex-wrap">
          <div className="inline-flex rounded-xl bg-slate-900/90 p-1 border border-slate-800 text-xs">
            <button
              onClick={() => setFilterMode("HITL_ONLY")}
              className={`px-3 py-1 rounded-lg font-medium transition-all ${
                filterMode === "HITL_ONLY"
                  ? "bg-purple-600 text-white shadow-sm"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              {t.hitl.allDecisions} ({hitlCount})
            </button>
            <button
              onClick={() => setFilterMode("ALL")}
              className={`px-3 py-1 rounded-lg font-medium transition-all ${
                filterMode === "ALL"
                  ? "bg-slate-700 text-white shadow-sm"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              {t.hitl.fullTimeline} ({history.length})
            </button>
          </div>

          <button
            onClick={() => {
              setTargetPhase("PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE");
              setIsRevertModalOpen(true);
            }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-amber-500/15 hover:bg-amber-500/25 border border-amber-500/40 text-amber-300 transition-all hover:scale-[1.02] shadow-sm"
            title="Rollback or jump to another phase"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>{t.hitl.revertPhaseBtn}</span>
          </button>
        </div>
      </div>

      {/* History Timeline */}
      {loading && history.length === 0 ? (
        <div className="flex items-center justify-center py-8 text-slate-500 gap-2">
          <Loader2 className="h-4 w-4 animate-spin text-purple-400" />
          <span className="text-xs">{t.hitl.processing}</span>
        </div>
      ) : filteredHistory.length === 0 ? (
        <div className="py-8 text-center bg-slate-900/40 rounded-xl border border-slate-800/60 p-6 space-y-2">
          <MessageSquare className="h-8 w-8 text-slate-600 mx-auto" />
          <p className="text-xs text-slate-400">{t.hitl.noHistory}</p>
          <p className="text-[11px] text-slate-500">
            Persetujuan, penolakan, otorisasi merge, atau feedback dari dashboard & Telegram akan tercatat otomatis di sini.
          </p>
        </div>
      ) : (
        <div className="space-y-3 max-h-[500px] overflow-y-auto pr-1 divide-y divide-slate-800/40">
          {filteredHistory.map((item, index) => {
            const meta = getDecisionMeta(item);
            const { prefix, comment } = extractCommentAndPrefix(item.message || "");
            const actorDisplay = formatActor(item.actor);
            const dateObj = new Date(item.createdAt);
            const timeStr = dateObj.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
            const dateStr = dateObj.toLocaleDateString([], { month: "short", day: "numeric", year: "numeric" });

            return (
              <div key={item.id || index} className="pt-3 first:pt-0 space-y-2 group">
                {/* Meta Top Line */}
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2 flex-wrap">
                    {/* Decision Badge */}
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${meta.badgeClass}`}
                    >
                      {meta.icon}
                      <span>{meta.label}</span>
                    </span>

                    {/* Actor Badge */}
                    <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-slate-300 bg-slate-800/80 border border-slate-700/60 px-2 py-0.5 rounded-md font-mono">
                      {item.actor.includes("TELEGRAM") ? "💬" : "👤"} {actorDisplay}
                    </span>

                    {/* State Flow Pill */}
                    <div className="inline-flex items-center gap-1 text-[10px] text-slate-400 bg-slate-900/80 px-2 py-0.5 rounded border border-slate-800">
                      <span className="font-mono text-slate-300">
                        {item.fromState ? item.fromState.replace("PHASE_", "P") : "START"}
                      </span>
                      <ArrowRight className="h-3 w-3 text-slate-500" />
                      <span className="font-mono text-cyan-300 font-semibold">
                        {item.toState.replace("PHASE_", "P")}
                      </span>
                    </div>
                  </div>

                  {/* Timestamp */}
                  <div className="flex items-center gap-1 text-[11px] text-slate-400 font-mono">
                    <Clock className="h-3 w-3 text-slate-500" />
                    <span>{dateStr} {timeStr}</span>
                  </div>
                </div>

                {/* Comment / Description Card */}
                <div
                  className={`p-3 rounded-xl border border-slate-800/80 border-l-4 ${meta.quoteBorder} space-y-1`}
                >
                  {prefix && (
                    <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                      <span>{prefix}</span>
                    </div>
                  )}

                  <div className="text-xs text-slate-200 whitespace-pre-wrap leading-relaxed font-sans font-medium selection:bg-purple-500 selection:text-white">
                    {comment || "No detailed note provided."}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Revert Phase Interactive Modal */}
      {isRevertModalOpen &&
        typeof document !== "undefined" &&
        createPortal(
          <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-[#0f172a] border border-amber-500/40 rounded-2xl max-w-xl w-full p-5 sm:p-6 space-y-4 shadow-2xl ring-1 ring-amber-500/20">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <div className="flex items-center gap-2.5 text-amber-400 font-bold text-base">
                  <div className="h-8 w-8 rounded-lg bg-amber-500/20 flex items-center justify-center border border-amber-500/30">
                    <RotateCcw className="h-4 w-4" />
                  </div>
                  <span>{t.hitl.revertModalTitle}</span>
                </div>
                <button
                  onClick={() => setIsRevertModalOpen(false)}
                  className="text-slate-400 hover:text-white text-xs px-2 py-1 rounded-md bg-slate-800"
                >
                  ✕
                </button>
              </div>

              <p className="text-xs text-slate-300 leading-relaxed">
                {t.hitl.revertModalDesc}
              </p>

              <div className="space-y-3">
                {/* Target Phase Selector */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    {t.hitl.targetPhaseLabel}
                  </label>
                  <select
                    value={targetPhase}
                    onChange={(e) => setTargetPhase(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-amber-500"
                  >
                    {phaseOptions.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Reason Input */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    {t.hitl.reasonLabel}
                  </label>
                  <textarea
                    rows={3}
                    value={revertReason}
                    onChange={(e) => setRevertReason(e.target.value)}
                    placeholder={t.hitl.reasonPlaceholder}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl p-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 leading-relaxed"
                  />
                </div>

                {revertSuccessMsg && (
                  <div className="p-2.5 rounded-lg bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 text-xs font-semibold flex items-center gap-2">
                    <CheckCircle2 className="h-4 w-4" />
                    <span>{revertSuccessMsg}</span>
                  </div>
                )}
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsRevertModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white bg-slate-800/80 transition-colors"
                >
                  {t.hitl.cancel}
                </button>
                <button
                  type="button"
                  disabled={reverting}
                  onClick={handleExecuteRevert}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold bg-amber-500 hover:bg-amber-400 text-slate-950 transition-all disabled:opacity-50"
                >
                  {reverting ? (
                    <>
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      <span>{t.hitl.processing}</span>
                    </>
                  ) : (
                    <>
                      <RotateCcw className="h-3.5 w-3.5" />
                      <span>{t.hitl.confirmRevert}</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
}
