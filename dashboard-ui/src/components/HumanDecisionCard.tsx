"use client";

import React, { useState } from "react";
import { WorkflowSummary } from "@/lib/types";
import { ShieldAlert, CheckCircle2, XCircle, SendHorizontal, Loader2, ExternalLink, FileText, BookOpen } from "lucide-react";
import { approveWorkflow, rejectWorkflow } from "@/lib/api";
import { DocumentViewerModal } from "./DocumentViewerModal";
import { useLanguage } from "@/lib/i18n";

interface HumanDecisionCardProps {
  workflow: WorkflowSummary;
  onDecisionProcessed: () => void;
}

export function HumanDecisionCard({ workflow, onDecisionProcessed }: HumanDecisionCardProps) {
  const { language, t } = useLanguage();
  const [comment, setComment] = useState("");
  const [loading, setLoading] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState<string | null>(null);
  const [selectedDoc, setSelectedDoc] = useState<{ path: string; title: string } | null>(null);

  const isDeadlock = workflow.currentState === "PHASE_6_DEADLOCK_WAITING_HUMAN";
  const isProdGate = workflow.currentState === "PHASE_8_READY_FOR_PRODUCTION_GATE";
  const isCodeReviewMergeGate = workflow.currentState === "PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE";

  const handleApprove = async () => {
    try {
      setLoading(true);
      await approveWorkflow(workflow.id, comment || "Approved by human operator via dashboard");
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

  const handleReject = async () => {
    if (!comment.trim()) {
      alert(t.hitl.commentRequiredAlert);
      return;
    }
    try {
      setLoading(true);
      await rejectWorkflow(workflow.id, comment);
      setFeedbackMsg(t.hitl.rejectSuccess);
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
    <div className={`w-full rounded-2xl p-4 sm:p-5 border transition-all ${
      isDeadlock
        ? "bg-rose-950/30 border-rose-500/60 ring-1 ring-rose-500/20"
        : isCodeReviewMergeGate
        ? "bg-indigo-950/25 border-indigo-500/60 ring-1 ring-indigo-500/25 shadow-lg shadow-indigo-500/10"
        : "bg-amber-950/20 border-amber-500/50 ring-1 ring-amber-500/20"
    }`}>
      <div className="flex items-start gap-3">
        <div className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl ${
          isDeadlock ? "bg-rose-500/20 text-rose-400" : isCodeReviewMergeGate ? "bg-indigo-500/20 text-indigo-400" : "bg-amber-500/20 text-amber-400"
        }`}>
          <ShieldAlert className="h-5 w-5" />
        </div>

        <div className="flex-1">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <h3 className="text-sm sm:text-base font-bold text-white">
                {isDeadlock
                  ? t.hitl.deadlockTitle
                  : isProdGate
                  ? t.hitl.prodGateTitle
                  : isCodeReviewMergeGate
                  ? t.hitl.mergeGateTitle
                  : t.hitl.reqSignOffTitle}
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
            {isDeadlock
              ? t.hitl.deadlockNotice
              : isProdGate
              ? t.hitl.prodNotice
              : isCodeReviewMergeGate
              ? (language === "en"
                  ? `Council Triad Consensus (Architect, AppSec, Performance) passed with ${workflow.coveragePercentage || 85.5}% test coverage. Main branch is protected from automated commits. Authorize Pull Request merge to main branch.`
                  : `Konsensus Triad Council (Architect 🏛️, AppSec 🛡️, Performance ⚡) telah lolos 3/3 dengan coverage ${workflow.coveragePercentage || 85.5}%. Branch main diproteksi dari bot otonom. Berikan otorisasi Anda untuk menggabungkan Pull Request ke branch main.`)
              : t.hitl.reqNotice}
          </p>

          {/* Banner Jika Agent Sedang Aktif Mengerjakan Tugas (Mencegah Interupsi HITL) */}
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
                  {workflow.currentAgentActivity || (language === "en" ? "Analyzing requirements & preparing artifacts..." : "Menganalisis kebutuhan & menyusun berkas...")}
                </p>
                <p className="text-[10px] text-amber-400 font-semibold mt-1">
                  🔒 {language === "en" ? "HITL approval gate locked automatically to prevent interruption until agent completes generation." : "Gerbang persetujuan (HITL) dikunci otomatis untuk mencegah interupsi hingga agent selesai menghasilkan berkas."}
                </p>
              </div>
            </div>
          )}

          {/* Section: Berkas Dokumen SDLC yang Harus Ditinjau */}
          <div className="mt-3 p-3 rounded-xl bg-slate-900/80 border border-slate-800 space-y-2">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-500/20 text-indigo-400">
                  <BookOpen className="h-4 w-4" />
                </div>
                <div>
                  <span className="text-xs font-semibold text-slate-200">
                    {language === "en" ? "SDLC Engineering Document:" : "Dokumen Rekayasa SDLC:"}
                  </span>
                  <span className="text-[11px] font-mono text-indigo-300 ml-1.5 font-medium">
                    SDLC_LOG.md
                  </span>
                </div>
              </div>
              <button
                type="button"
                onClick={() =>
                  setSelectedDoc({
                    path: "SDLC_LOG.md",
                    title:
                      language === "en"
                        ? "📘 SDLC Engineering Journal & Rationale (Single Source of Truth)"
                        : "📘 SDLC Engineering Journal & Rationale (Single Source of Truth)",
                  })
                }
                className="touch-target inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600/30 hover:bg-indigo-600/50 text-indigo-200 border border-indigo-500/40 text-xs font-semibold shadow-sm transition"
              >
                <BookOpen className="h-3.5 w-3.5" />
                <span>{language === "en" ? "View SDLC Journal (SDLC_LOG.md)" : "Lihat Jurnal SDLC (SDLC_LOG.md)"}</span>
              </button>
            </div>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              {language === "en"
                ? "Review the SDLC engineering journal and agent rationale for this phase before approving or rejecting."
                : "Tinjau jurnal rekayasa SDLC dan pertimbangan agent untuk fase ini sebelum memberikan keputusan setujui atau tolak."}
            </p>

            {/* Live Requirement Summary Accordion */}
            <details className="mt-2 text-xs border-t border-slate-800/80 pt-2 cursor-pointer group">
              <summary className="text-[11px] font-medium text-blue-400 hover:text-blue-300 flex items-center gap-1 select-none">
                <span>▶ {language === "en" ? "Read Requirements Summary" : "Baca Ringkasan Kebutuhan"} ({workflow.taskName})</span>
              </summary>
              <div className="mt-2 p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-[11px] text-slate-300 space-y-2 leading-relaxed">
                <div>
                  <strong className="text-white block mb-0.5">{language === "en" ? "Input Requirements Description:" : "Deskripsi Kebutuhan Masukan:"}</strong>
                  <p className="text-slate-400 whitespace-pre-line">{workflow.description}</p>
                </div>
              </div>
            </details>
          </div>

          {feedbackMsg && (
            <div className="mt-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 p-2 text-xs font-medium text-emerald-400">
              {feedbackMsg}
            </div>
          )}

          {/* Revision / Feedback Note Input */}
          <div className="mt-3">
            <label className="block text-[11px] font-medium text-slate-400 mb-1">
              {t.hitl.commentLabel}
            </label>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder={t.hitl.commentPlaceholder}
              className="w-full rounded-xl bg-slate-900/90 border border-slate-700/80 px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 resize-none h-18"
            />
          </div>

          {/* Action Buttons (Touch Friendly) */}
          <div className="mt-3 flex flex-wrap items-center gap-2 sm:gap-3">
            <button
              onClick={handleApprove}
              disabled={loading || workflow.agentRunning}
              className="touch-target flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-xs sm:text-sm font-semibold text-white shadow-md shadow-emerald-600/30 hover:bg-emerald-500 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed transition"
            >
              {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
              <span>{isDeadlock ? (language === "en" ? "Override & Advance" : "Override & Loloskan") : isCodeReviewMergeGate ? t.hitl.authorizeMerge : t.hitl.approveAdvance}</span>
            </button>

            <button
              onClick={handleReject}
              disabled={loading || workflow.agentRunning}
              className="touch-target flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 rounded-xl bg-slate-800 border border-rose-500/40 px-4 py-2.5 text-xs sm:text-sm font-semibold text-rose-300 hover:bg-rose-950/40 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed transition"
            >
              {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <XCircle className="h-4 w-4" />}
              <span>{isCodeReviewMergeGate ? t.hitl.rejectMerge : t.hitl.rejectRevision}</span>
            </button>
          </div>
        </div>
      </div>

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
