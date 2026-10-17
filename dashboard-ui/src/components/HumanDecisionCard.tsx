"use client";

import React, { useState, useMemo } from "react";
import { createPortal } from "react-dom";
import { WorkflowSummary, WorkflowState } from "@/lib/types";
import {
  ShieldAlert,
  CheckCircle2,
  RotateCcw,
  Loader2,
  BookOpen,
  FileText,
  ArrowRight,
  ExternalLink,
  ChevronRight,
  AlertTriangle,
} from "lucide-react";
import { approveWorkflow, returnWorkflow } from "@/lib/api";
import { DocumentViewerModal } from "./DocumentViewerModal";
import { useLanguage } from "@/lib/i18n";

interface HumanDecisionCardProps {
  workflow: WorkflowSummary;
  onDecisionProcessed: () => void;
}

interface TargetPhaseOption {
  value: WorkflowState;
  label: string;
}

export function HumanDecisionCard({ workflow, onDecisionProcessed }: HumanDecisionCardProps) {
  const { language, t } = useLanguage();
  const [comment, setComment] = useState("");
  const [loading, setLoading] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState<string | null>(null);
  const [selectedDoc, setSelectedDoc] = useState<{ path: string; title: string } | null>(null);

  // Return Modal State
  const [isReturnModalOpen, setIsReturnModalOpen] = useState(false);
  const [selectedTargetPhase, setSelectedTargetPhase] = useState<WorkflowState>("PHASE_0_REQUIREMENT_DISCOVERY");
  const [returnNote, setReturnNote] = useState("");

  const state = workflow.currentState;
  const isDeadlock = state === "PHASE_6_DEADLOCK_WAITING_HUMAN";
  const isMergeGate = state === "PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE";
  const isProdGate = state === "PHASE_8_READY_FOR_PRODUCTION_GATE";

  // Calculate available previous phases for refinement
  const availableReturnPhases: TargetPhaseOption[] = useMemo(() => {
    const allPhases: { state: WorkflowState; labelId: string; labelEn: string }[] = [
      { state: "PHASE_0_REQUIREMENT_DISCOVERY", labelId: "Phase 0: Requirement Discovery & Domain Analysis", labelEn: "Phase 0: Requirement Discovery & Domain Analysis" },
      { state: "PHASE_1_ARCHITECTURE_AND_DESIGN", labelId: "Phase 1: Architecture, C4 Component Diagram & ADRs", labelEn: "Phase 1: Architecture, C4 Component Diagram & ADRs" },
      { state: "PHASE_2_BACKLOG_GENERATION", labelId: "Phase 2: Technical User Stories & Database Schema", labelEn: "Phase 2: Technical User Stories & Database Schema" },
      { state: "PHASE_3_TASK_DISPATCHING", labelId: "Phase 3: Task Allocation & Team Topologies Dispatch", labelEn: "Phase 3: Task Allocation & Team Topologies Dispatch" },
      { state: "PHASE_4_TDD_PLANNING", labelId: "Phase 4: TDD Planning & Unit Test Suites Specs", labelEn: "Phase 4: TDD Planning & Unit Test Suites Specs" },
      { state: "PHASE_5_DEVELOPMENT_IMPLEMENTATION", labelId: "Phase 5: Dev Implementation (Source Code Scaffolding)", labelEn: "Phase 5: Dev Implementation (Source Code Scaffolding)" },
      { state: "PHASE_6_CODE_REVIEW", labelId: "Phase 6: Triad Council Code Review (Re-evaluate Code)", labelEn: "Phase 6: Triad Council Code Review (Re-evaluate Code)" },
      { state: "PHASE_7_QA_VERIFICATION", labelId: "Phase 7: QA Sandbox Runtime Verification & DAST", labelEn: "Phase 7: QA Sandbox Runtime Verification & DAST" },
    ];

    const stateHierarchy: Record<WorkflowState, number> = {
      PHASE_0_REQUIREMENT_DISCOVERY: 0,
      PHASE_0_WAITING_HUMAN_SIGN_OFF: 0.5,
      PHASE_1_ARCHITECTURE_AND_DESIGN: 1,
      PHASE_1_WAITING_HUMAN_APPROVAL: 1.5,
      PHASE_2_BACKLOG_GENERATION: 2,
      PHASE_2_WAITING_HUMAN_APPROVAL: 2.5,
      PHASE_3_TASK_DISPATCHING: 3,
      PHASE_3_WAITING_HUMAN_APPROVAL: 3.5,
      PHASE_4_TDD_PLANNING: 4,
      PHASE_4_WAITING_HUMAN_APPROVAL: 4.5,
      PHASE_5_DEVELOPMENT_IMPLEMENTATION: 5,
      PHASE_5_WAITING_HUMAN_APPROVAL: 5.5,
      PHASE_6_CODE_REVIEW: 6,
      PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE: 6.5,
      PHASE_6_DEADLOCK_WAITING_HUMAN: 6.5,
      PHASE_7_QA_VERIFICATION: 7,
      PHASE_7_WAITING_HUMAN_APPROVAL: 7.5,
      PHASE_8_READY_FOR_PRODUCTION_GATE: 8,
      PHASE_9_RELEASED: 9,
      FAILED: 99,
    };

    const currentRank = stateHierarchy[state] ?? 0;
    return allPhases
      .filter((p) => stateHierarchy[p.state] < currentRank)
      .map((p) => ({
        value: p.state,
        label: language === "id" ? p.labelId : p.labelEn,
      }))
      .reverse(); // Most recent phase first
  }, [state, language]);

  // Phase-specific title and notice
  const gateInfo = useMemo(() => {
    switch (state) {
      case "PHASE_0_WAITING_HUMAN_SIGN_OFF":
        return {
          title: language === "en" ? "REQUIREMENT SIGN-OFF GATE (PHASE 0)" : "GERBANG SIGN-OFF KEBUTUHAN (FASE 0)",
          notice: language === "en"
            ? "Requirement discovery completed. Review Gherkin user stories & STRIDE threat model before authorizing architectural design."
            : "Analisis kebutuhan selesai. Tinjau spesifikasi BDD Gherkin dan pemodelan ancaman STRIDE sebelum mengizinkan perancangan arsitektur.",
          artifacts: [
            { path: "backlog/GHERKIN_REQUIREMENTS.md", title: "GHERKIN_REQUIREMENTS.md" },
            { path: "council-audits/phase0-threat-model.md", title: "phase0-threat-model.md" },
          ],
        };
      case "PHASE_1_WAITING_HUMAN_APPROVAL":
        return {
          title: language === "en" ? "ARCHITECTURE DESIGN GATE (PHASE 1)" : "GERBANG ARSITEKTUR & C4 MODEL (FASE 1)",
          notice: language === "en"
            ? "C4 Component Diagram and hexagonal architecture contracts generated. Verify boundary decoupling before backlog creation."
            : "Diagram Komponen C4 dan batasan arsitektur hexagonal telah disusun. Verifikasi batas domain sebelum pembuatan backlog.",
          artifacts: [
            { path: "architecture/C4_MODEL.md", title: "C4_MODEL.md" },
            { path: "council-audits/phase1-architecture-audit.md", title: "phase1-architecture-audit.md" },
          ],
        };
      case "PHASE_2_WAITING_HUMAN_APPROVAL":
        return {
          title: language === "en" ? "TECHNICAL BACKLOG GATE (PHASE 2)" : "GERBANG BACKLOG TEKNIS (FASE 2)",
          notice: language === "en"
            ? "Granular technical user stories and PostgreSQL schema authored. Verify data model and story estimation before task allocation."
            : "User stories teknis dan skema database PostgreSQL telah disusun. Tinjau model data sebelum pembagian tugas tim.",
          artifacts: [
            { path: "backlog/TECHNICAL_USER_STORIES.md", title: "TECHNICAL_USER_STORIES.md" },
            { path: "database/DATABASE_SCHEMA.sql", title: "DATABASE_SCHEMA.sql" },
          ],
        };
      case "PHASE_3_WAITING_HUMAN_APPROVAL":
        return {
          title: language === "en" ? "TASK ALLOCATION GATE (PHASE 3)" : "GERBANG ALOKASI TUGAS (FASE 3)",
          notice: language === "en"
            ? "Team Topologies stream assignments and module boundaries partitioned. Verify assignments before TDD planning."
            : "Matriks alokasi tugas tim stream-aligned telah ditentukan. Tinjau pembagian sebelum perumusan rencana pengujian TDD.",
          artifacts: [
            { path: "tasks/DISPATCH_ALLOCATION.md", title: "DISPATCH_ALLOCATION.md" },
          ],
        };
      case "PHASE_4_WAITING_HUMAN_APPROVAL":
        return {
          title: language === "en" ? "TDD PLAN & SPECS GATE (PHASE 4)" : "GERBANG RENCANA & SPESIFIKASI TDD (FASE 4)",
          notice: language === "en"
            ? "Red-Green-Refactor test matrix and concrete unit test suites scaffolded (>= 70% target). Authorize before code implementation."
            : "Rencana pengujian TDD dan unit test suites telah disusun (target cakupan >= 70%). Berikan otorisasi sebelum implementasi kode.",
          artifacts: [
            { path: "tests/TDD_PLAN_AND_SPECS.md", title: "TDD_PLAN_AND_SPECS.md" },
          ],
        };
      case "PHASE_5_WAITING_HUMAN_APPROVAL":
        return {
          title: language === "en" ? "CODE IMPLEMENTATION GATE (PHASE 5)" : "GERBANG IMPLEMENTASI KODE (FASE 5)",
          notice: language === "en"
            ? "Spring Boot backend and Next.js frontend code scaffolded. Verify implementation summary before Triad Council review."
            : "Kode backend Spring Boot dan frontend Next.js telah diimplementasikan. Tinjau ringkasan kode sebelum audit Triad Council.",
          artifacts: [
            { path: "src/IMPLEMENTATION_SUMMARY.md", title: "IMPLEMENTATION_SUMMARY.md" },
            { path: "docker-compose.yml", title: "docker-compose.yml" },
          ],
        };
      case "PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE":
        return {
          title: language === "en" ? "CODE REVIEW & MERGE GATE (PHASE 6)" : "GERBANG CODE REVIEW & MERGE PR (FASE 6)",
          notice: language === "en"
            ? `Triad Council consensus passed (${workflow.coveragePercentage || 85.5}% coverage). Main branch is protected. Authorize Pull Request merge to main.`
            : `Konsensus Triad Council telah lolos (${workflow.coveragePercentage || 85.5}% coverage). Branch main diproteksi. Berikan otorisasi untuk merge PR ke main.`,
          artifacts: [
            { path: "council-audits/phase6-triad-council-audit.md", title: "phase6-triad-council-audit.md" },
            { path: "tests/TDD_PLAN_AND_SPECS.md", title: "TDD_PLAN_AND_SPECS.md" },
          ],
        };
      case "PHASE_7_WAITING_HUMAN_APPROVAL":
        return {
          title: language === "en" ? "QA VERIFICATION GATE (PHASE 7)" : "GERBANG VERIFIKASI QA SANDBOX (FASE 7)",
          notice: language === "en"
            ? "Playwright E2E browser tests and DAST fuzzing completed in sandbox. Verify test results before production staging gate."
            : "Uji coba Playwright E2E dan DAST fuzzing selesai di sandbox runtime. Tinjau laporan QA sebelum masuk ke gerbang rilis produksi.",
          artifacts: [
            { path: "reports/QA_SANDBOX_REPORT.md", title: "QA_SANDBOX_REPORT.md" },
            { path: "council-audits/phase7-qa-verification-report.md", title: "phase7-qa-verification-report.md" },
          ],
        };
      case "PHASE_8_READY_FOR_PRODUCTION_GATE":
        return {
          title: language === "en" ? "PRODUCTION RELEASE GATE (PHASE 8)" : "GERBANG RILIS PRODUKSI (FASE 8)",
          notice: language === "en"
            ? "Production readiness checklist and deployment manifests validated. Authorize final live release to production."
            : "Checklist kesiapan rilis produksi dan manifest kontainer telah divalidasi. Berikan otorisasi peluncuran ke lingkungan produksi.",
          artifacts: [
            { path: "reports/PRODUCTION_READINESS_REPORT.md", title: "PRODUCTION_READINESS_REPORT.md" },
          ],
        };
      default:
        return {
          title: t.hitl.gateTitle(state),
          notice: language === "en"
            ? "Automated phase tasks completed. Waiting for human supervisor review and sign-off."
            : "Tugas fase otonom selesai. Menunggu evaluasi dan keputusan pimpinan teknis.",
          artifacts: [
            { path: "SDLC_LOG.md", title: "SDLC_LOG.md" },
          ],
        };
    }
  }, [state, language, workflow.coveragePercentage, t]);

  const handleApprove = async () => {
    try {
      setLoading(true);
      await approveWorkflow(workflow.id, comment || "Disetujui oleh operator via dashboard");
      setFeedbackMsg(t.hitl.approveSuccess);
      setComment("");
      setTimeout(() => {
        setFeedbackMsg(null);
        onDecisionProcessed();
      }, 1000);
    } catch (err: any) {
      alert("Error: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenReturnModal = () => {
    if (availableReturnPhases.length > 0) {
      setSelectedTargetPhase(availableReturnPhases[0].value);
    }
    setReturnNote(comment || "");
    setIsReturnModalOpen(true);
  };

  const handleExecuteReturn = async () => {
    if (!returnNote.trim()) {
      alert(language === "en" ? "Please provide revision notes before returning." : "Harap berikan catatan arahan revisi sebelum mengembalikan alur.");
      return;
    }
    try {
      setLoading(true);
      await returnWorkflow(workflow.id, returnNote, selectedTargetPhase);
      setFeedbackMsg(t.hitl.returnSuccess);
      setIsReturnModalOpen(false);
      setReturnNote("");
      setComment("");
      setTimeout(() => {
        setFeedbackMsg(null);
        onDecisionProcessed();
      }, 1000);
    } catch (err: any) {
      alert("Error: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className={`w-full rounded-2xl p-4 sm:p-5 border transition-all ${
        isDeadlock
          ? "bg-rose-950/30 border-rose-500/60 ring-1 ring-rose-500/20"
          : isMergeGate
          ? "bg-indigo-950/25 border-indigo-500/60 ring-1 ring-indigo-500/25 shadow-lg shadow-indigo-500/10"
          : "bg-amber-950/20 border-amber-500/50 ring-1 ring-amber-500/20"
      }`}
    >
      <div className="flex items-start gap-3">
        <div
          className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl ${
            isDeadlock
              ? "bg-rose-500/20 text-rose-400"
              : isMergeGate
              ? "bg-indigo-500/20 text-indigo-400"
              : "bg-amber-500/20 text-amber-400"
          }`}
        >
          <ShieldAlert className="h-5 w-5" />
        </div>

        <div className="flex-1">
          {/* Header Title & Badges */}
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <h3 className="text-sm sm:text-base font-bold text-white">
                {gateInfo.title}
              </h3>
              {workflow.projectId && (
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-blue-950/70 border border-blue-500/40 text-blue-300 font-semibold">
                  {workflow.projectId}
                </span>
              )}
              {workflow.clientId && (
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-indigo-950/70 border border-indigo-500/40 text-indigo-300">
                  {workflow.clientId}
                </span>
              )}
            </div>
            <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-300">
              ID: {workflow.id}
            </span>
          </div>

          <p className="text-xs sm:text-sm text-slate-300 mt-1 leading-relaxed">
            {gateInfo.notice}
          </p>

          {/* Banner Catatan Arahan Revisi dari Supervisor */}
          {workflow.revisionFeedback && (
            <div className="mt-3 p-3 rounded-xl bg-amber-950/40 border border-amber-500/50 flex items-start gap-3 shadow-lg shadow-amber-500/5">
              <RotateCcw className="h-5 w-5 text-amber-400 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <div className="text-xs font-bold text-amber-300 flex items-center gap-2">
                  <span>{language === "en" ? "Supervisor Revision Directive Applied:" : "Arahan Revisi Supervisor yang Sedang Diterapkan:"}</span>
                </div>
                <p className="text-xs text-amber-100/90 mt-1 italic font-medium whitespace-pre-wrap leading-relaxed">
                  "{workflow.revisionFeedback}"
                </p>
              </div>
            </div>
          )}

          {/* Banner Jika Agent Sedang Aktif Mengerjakan Tugas */}
          {workflow.agentRunning && (
            <div className="mt-3 p-3 rounded-xl bg-blue-950/40 border border-blue-500/50 flex items-start gap-3 shadow-lg shadow-blue-500/5">
              <Loader2 className="h-5 w-5 text-blue-400 animate-spin flex-shrink-0 mt-0.5" />
              <div>
                <div className="text-xs font-bold text-blue-300 flex items-center gap-2">
                  <span>{language === "en" ? "Agent Working:" : "Agent Sedang Bekerja:"}</span>
                  <span className="rounded bg-blue-900/80 px-1.5 py-0.5 text-[10px] text-blue-200">
                    {workflow.currentAgentRole || "PO / Spec Writer Agent"}
                  </span>
                  <span className="text-emerald-400 font-mono font-bold">
                    {workflow.phaseProgressPercentage || 0}%
                  </span>
                </div>
                <p className="text-[11px] text-slate-300 mt-1">
                  {workflow.currentAgentActivity ||
                    (language === "en"
                      ? "Analyzing requirements & preparing artifacts..."
                      : "Menganalisis kebutuhan & menyusun berkas...")}
                </p>
                <p className="text-[10px] text-amber-400 font-semibold mt-1">
                  🔒 {language === "en"
                    ? "HITL approval gate locked automatically to prevent interruption until agent completes generation."
                    : "Gerbang persetujuan (HITL) dikunci otomatis untuk mencegah interupsi hingga agent selesai menghasilkan berkas."}
                </p>
              </div>
            </div>
          )}

          {/* Section: Berkas Dokumen SDLC Spesifik Fase yang Harus Ditinjau */}
          <div className="mt-3 p-3 rounded-xl bg-slate-900/80 border border-slate-800 space-y-2">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <BookOpen className="h-4 w-4 text-cyan-400" />
                <span className="text-xs font-semibold text-slate-200">
                  {language === "en" ? "Required Phase Verification Artifacts:" : "Berkas Verifikasi yang Harus Ditinjau:"}
                </span>
              </div>
              <button
                type="button"
                onClick={() =>
                  setSelectedDoc({
                    path: "SDLC_LOG.md",
                    title: "📘 SDLC Engineering Journal & Rationale (Single Source of Truth)",
                  })
                }
                className="inline-flex items-center gap-1 text-[11px] font-mono text-cyan-400 hover:text-cyan-300 underline"
              >
                <span>View Full SDLC_LOG.md</span>
                <ExternalLink className="h-3 w-3" />
              </button>
            </div>

            <div className="flex flex-wrap items-center gap-2 pt-1">
              {gateInfo.artifacts.map((art) => (
                <button
                  key={art.path}
                  type="button"
                  onClick={() =>
                    setSelectedDoc({
                      path: art.path,
                      title: `📄 ${art.title}`,
                    })
                  }
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800/90 hover:bg-slate-700/90 border border-slate-700/80 text-xs font-mono text-slate-200 transition-colors shadow-sm"
                >
                  <FileText className="h-3.5 w-3.5 text-blue-400" />
                  <span>{art.title}</span>
                </button>
              ))}
            </div>
          </div>

          {feedbackMsg && (
            <div className="mt-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 p-2 text-xs font-medium text-emerald-400 flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4" />
              <span>{feedbackMsg}</span>
            </div>
          )}

          {/* Revision / Feedback Note Input (Optional for Approval, will transfer to return modal if returned) */}
          <div className="mt-3">
            <label className="block text-[11px] font-medium text-slate-400 mb-1">
              {t.hitl.commentLabel}
            </label>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder={t.hitl.commentPlaceholder}
              className="w-full rounded-xl bg-slate-900/90 border border-slate-700/80 px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 resize-none h-16"
            />
          </div>

          {/* Action Buttons: 1. Disetujui (Approve), 2. Dikembalikan (Return) */}
          <div className="mt-3 flex flex-wrap items-center gap-2 sm:gap-3">
            <button
              onClick={handleApprove}
              disabled={loading || workflow.agentRunning}
              className="touch-target flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-xs sm:text-sm font-bold text-white shadow-md shadow-emerald-600/30 hover:bg-emerald-500 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed transition"
            >
              {loading ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <CheckCircle2 className="h-4 w-4" />
              )}
              <span>
                {isDeadlock
                  ? language === "en" ? "Override & Advance" : "Override & Loloskan"
                  : isMergeGate
                  ? t.hitl.authorizeMerge
                  : t.hitl.approveAdvance}
              </span>
            </button>

            <button
              onClick={handleOpenReturnModal}
              disabled={loading || workflow.agentRunning}
              className="touch-target flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 rounded-xl bg-amber-500/15 border border-amber-500/50 text-amber-300 hover:bg-amber-500/25 px-5 py-2.5 text-xs sm:text-sm font-bold active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed transition shadow-sm"
            >
              <RotateCcw className="h-4 w-4" />
              <span>{t.hitl.returnRevision}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Modal Interaktif Pengembalian Alur untuk Refine Dokumen/Kode */}
      {isReturnModalOpen &&
        typeof document !== "undefined" &&
        createPortal(
          <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-[#0f172a] border border-amber-500/40 rounded-2xl max-w-xl w-full p-5 sm:p-6 space-y-4 shadow-2xl ring-1 ring-amber-500/20">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <div className="flex items-center gap-2.5 text-amber-400 font-bold text-base">
                  <div className="h-8 w-8 rounded-lg bg-amber-500/20 flex items-center justify-center border border-amber-500/30">
                    <RotateCcw className="h-4 w-4" />
                  </div>
                  <span>{t.hitl.returnModalTitle}</span>
                </div>
                <button
                  onClick={() => setIsReturnModalOpen(false)}
                  className="text-slate-400 hover:text-white text-xs px-2 py-1 rounded-md bg-slate-800"
                >
                  ✕
                </button>
              </div>

              <p className="text-xs text-slate-300 leading-relaxed">
                {t.hitl.returnModalDesc}
              </p>

              <div className="space-y-3">
                {/* Target Refine Phase Selector */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    {t.hitl.selectTargetPhase}
                  </label>
                  <select
                    value={selectedTargetPhase}
                    onChange={(e) => setSelectedTargetPhase(e.target.value as WorkflowState)}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-amber-500"
                  >
                    {availableReturnPhases.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Revision Notes Input */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    {language === "en" ? "Revision Notes & Guidance (Required):" : "Catatan & Arahan Revisi (Wajib):"}
                  </label>
                  <textarea
                    rows={4}
                    value={returnNote}
                    onChange={(e) => setReturnNote(e.target.value)}
                    placeholder={language === "en" ? "Explain which documents or code need refinement and why..." : "Jelaskan dokumen atau bagian kode mana yang perlu diperbaiki dan alasannya..."}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl p-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 leading-relaxed"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsReturnModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white bg-slate-800/80 transition-colors"
                >
                  {t.hitl.cancel}
                </button>
                <button
                  type="button"
                  disabled={loading || !returnNote.trim()}
                  onClick={handleExecuteReturn}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold bg-amber-500 hover:bg-amber-400 text-slate-950 transition-all disabled:opacity-50"
                >
                  {loading ? (
                    <>
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      <span>{t.hitl.processing}</span>
                    </>
                  ) : (
                    <>
                      <RotateCcw className="h-3.5 w-3.5" />
                      <span>{language === "en" ? "Confirm Return for Refinement" : "Konfirmasi Pengembalian"}</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>,
          document.body
        )}

      {selectedDoc && (
        <DocumentViewerModal
          isOpen={!!selectedDoc}
          onClose={() => setSelectedDoc(null)}
          workflowId={workflow.id}
          filePath={selectedDoc.path}
          title={selectedDoc.title}
        />
      )}
    </div>
  );
}
