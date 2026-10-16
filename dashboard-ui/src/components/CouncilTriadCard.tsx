"use client";

import React, { useState } from "react";
import { WorkflowSummary } from "@/lib/types";
import { Shield, Cpu, Activity, CheckCircle2, XCircle, Clock, Vote, AlertTriangle } from "lucide-react";
import { submitCouncilReview } from "@/lib/api";
import { useLanguage } from "@/lib/i18n";

interface CouncilTriadCardProps {
  workflow: WorkflowSummary;
  onReviewSubmitted: () => void;
}

export function CouncilTriadCard({ workflow, onReviewSubmitted }: CouncilTriadCardProps) {
  const { language, t } = useLanguage();
  const [archVote, setArchVote] = useState(workflow.architectApproved ?? true);
  const [secVote, setSecVote] = useState(workflow.securityApproved ?? true);
  const [perfVote, setPerfVote] = useState(workflow.performanceApproved ?? true);
  const [coverage, setCoverage] = useState(workflow.coveragePercentage || 85.0);
  const [feedback, setFeedback] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [showVotingModal, setShowVotingModal] = useState(false);

  const isCodeReviewPhase = workflow.currentState === "PHASE_6_CODE_REVIEW";

  const handleCastVotes = async () => {
    try {
      setSubmitting(true);
      await submitCouncilReview(workflow.id, {
        architectApproved: archVote,
        securityApproved: secVote,
        performanceApproved: perfVote,
        coveragePercentage: Number(coverage),
        feedback: feedback || "Triad Council manual evaluation",
      });
      setShowVotingModal(false);
      onReviewSubmitted();
    } catch (err: any) {
      alert("Error: " + err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="rounded-2xl p-4 sm:p-5 glass-card border border-indigo-500/20 bg-gradient-to-br from-indigo-950/20 via-slate-900/60 to-slate-950/80 shadow-xl">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
            <Vote className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm sm:text-base font-bold text-white flex items-center gap-2">
              {t.council.title}
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 font-medium">
                {t.council.badge}
              </span>
            </h3>
            <p className="text-[11px] text-slate-400">
              {t.council.subtitle}
            </p>
          </div>
        </div>

        {isCodeReviewPhase && (
          <button
            onClick={() => setShowVotingModal(true)}
            className="touch-target inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-lg shadow-indigo-600/30 transition active:scale-95"
          >
            <Vote className="h-4 w-4" />
            <span>{t.council.openVoteModal}</span>
          </button>
        )}
      </div>

      {/* 3 Pillars Visual Matrix */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        {/* Pillar 1: System Architect */}
        <div className="rounded-xl p-3 bg-slate-900/70 border border-slate-800 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2 text-sky-400 font-semibold text-xs">
                <Cpu className="h-4 w-4" />
                <span>{t.council.architectRole}</span>
              </div>
              {workflow.architectApproved ? (
                <span className="inline-flex items-center gap-1 text-[10px] text-emerald-400 font-medium bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
                  <CheckCircle2 className="h-3 w-3" /> Consensus OK
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 text-[10px] text-amber-400 font-medium bg-amber-500/10 px-2 py-0.5 rounded-full border border-amber-500/20">
                  <Clock className="h-3 w-3" /> Reviewing C4
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-300 leading-relaxed">
              {t.council.architectDesc}
            </p>
          </div>
          <div className="mt-2.5 pt-2 border-t border-slate-800 text-[10px] text-slate-500 flex justify-between">
            <span>Role: system-design-architect</span>
            <span className="text-sky-400 font-mono">Phase 0 - 8.5</span>
          </div>
        </div>

        {/* Pillar 2: AppSec Auditor */}
        <div className="rounded-xl p-3 bg-slate-900/70 border border-slate-800 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2 text-rose-400 font-semibold text-xs">
                <Shield className="h-4 w-4" />
                <span>{t.council.securityRole}</span>
              </div>
              {workflow.securityApproved ? (
                <span className="inline-flex items-center gap-1 text-[10px] text-emerald-400 font-medium bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
                  <CheckCircle2 className="h-3 w-3" /> SAST Passed
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 text-[10px] text-rose-400 font-medium bg-rose-500/10 px-2 py-0.5 rounded-full border border-rose-500/20">
                  <AlertTriangle className="h-3 w-3" /> Threat Modeling
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-300 leading-relaxed">
              {t.council.securityDesc}
            </p>
          </div>
          <div className="mt-2.5 pt-2 border-t border-slate-800 text-[10px] text-slate-500 flex justify-between">
            <span>Role: appsec-auditor</span>
            <span className="text-rose-400 font-mono">Veto Power</span>
          </div>
        </div>

        {/* Pillar 3: Perf & DAST Scout */}
        <div className="rounded-xl p-3 bg-slate-900/70 border border-slate-800 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2 text-amber-400 font-semibold text-xs">
                <Activity className="h-4 w-4" />
                <span>{t.council.perfRole}</span>
              </div>
              {workflow.performanceApproved ? (
                <span className="inline-flex items-center gap-1 text-[10px] text-emerald-400 font-medium bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
                  <CheckCircle2 className="h-3 w-3" /> SLA Verified
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 text-[10px] text-amber-400 font-medium bg-amber-500/10 px-2 py-0.5 rounded-full border border-amber-500/20">
                  <Clock className="h-3 w-3" /> k6 Benchmarking
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-300 leading-relaxed">
              {t.council.perfDesc}
            </p>
          </div>
          <div className="mt-2.5 pt-2 border-t border-slate-800 text-[10px] text-slate-500 flex justify-between">
            <span>Role: dast-pentest-scout</span>
            <span className="text-amber-400 font-mono">k6 / DAST Gate</span>
          </div>
        </div>
      </div>

      {/* Voting Modal */}
      {showVotingModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="w-full max-w-md rounded-2xl glass-panel p-5 border border-slate-700 shadow-2xl relative space-y-4">
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              <Vote className="h-4 w-4 text-indigo-400" />
              {t.council.voteModalTitle}
            </h4>
            <p className="text-xs text-slate-400">
              {t.council.voteModalDesc}
            </p>

            <div className="space-y-2.5">
              <label className="flex items-center justify-between p-2.5 rounded-xl bg-slate-900 border border-slate-800 cursor-pointer">
                <span className="text-xs font-medium text-slate-200">🏛️ {t.council.architectRole} Approval</span>
                <input
                  type="checkbox"
                  checked={archVote}
                  onChange={(e) => setArchVote(e.target.checked)}
                  className="rounded border-slate-700 text-indigo-600 focus:ring-indigo-500 h-4 w-4"
                />
              </label>

              <label className="flex items-center justify-between p-2.5 rounded-xl bg-slate-900 border border-slate-800 cursor-pointer">
                <span className="text-xs font-medium text-slate-200">🛡️ {t.council.securityRole} Approval</span>
                <input
                  type="checkbox"
                  checked={secVote}
                  onChange={(e) => setSecVote(e.target.checked)}
                  className="rounded border-slate-700 text-rose-600 focus:ring-rose-500 h-4 w-4"
                />
              </label>

              <label className="flex items-center justify-between p-2.5 rounded-xl bg-slate-900 border border-slate-800 cursor-pointer">
                <span className="text-xs font-medium text-slate-200">⚡ {t.council.perfRole} Approval</span>
                <input
                  type="checkbox"
                  checked={perfVote}
                  onChange={(e) => setPerfVote(e.target.checked)}
                  className="rounded border-slate-700 text-amber-600 focus:ring-amber-500 h-4 w-4"
                />
              </label>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">{t.council.coverageInput}</label>
                <input
                  type="number"
                  value={coverage}
                  onChange={(e) => setCoverage(Number(e.target.value))}
                  min={0}
                  max={100}
                  className="w-full rounded-xl bg-slate-900 border border-slate-700 px-3 py-1.5 text-xs text-white"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">{t.council.notesInput}</label>
                <textarea
                  rows={2}
                  value={feedback}
                  onChange={(e) => setFeedback(e.target.value)}
                  placeholder={t.council.notesPlaceholder}
                  className="w-full rounded-xl bg-slate-900 border border-slate-700 px-3 py-1.5 text-xs text-white resize-none"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setShowVotingModal(false)}
                className="px-3 py-1.5 rounded-xl bg-slate-800 text-xs font-medium text-slate-300 hover:bg-slate-700"
              >
                {t.common.cancel}
              </button>
              <button
                type="button"
                disabled={submitting}
                onClick={handleCastVotes}
                className="px-4 py-1.5 rounded-xl bg-indigo-600 text-xs font-semibold text-white hover:bg-indigo-500 shadow-md transition disabled:opacity-50"
              >
                {submitting ? t.council.submitting : t.council.submitVote}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
