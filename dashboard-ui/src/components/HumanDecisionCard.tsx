"use client";

import React, { useState } from "react";
import { WorkflowSummary } from "@/lib/types";
import { ShieldAlert, CheckCircle2, XCircle, SendHorizontal, Loader2 } from "lucide-react";
import { approveWorkflow, rejectWorkflow } from "@/lib/api";

interface HumanDecisionCardProps {
  workflow: WorkflowSummary;
  onDecisionProcessed: () => void;
}

export function HumanDecisionCard({ workflow, onDecisionProcessed }: HumanDecisionCardProps) {
  const [comment, setComment] = useState("");
  const [loading, setLoading] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState<string | null>(null);

  const isDeadlock = workflow.currentState === "PHASE_6_DEADLOCK_WAITING_HUMAN";
  const isProdGate = workflow.currentState === "PHASE_8_READY_FOR_PRODUCTION_GATE";

  const handleApprove = async () => {
    try {
      setLoading(true);
      await approveWorkflow(workflow.id, comment || "Approved by human operator via mobile dashboard");
      setFeedbackMsg("Persetujuan berhasil diproses. Workflow dilanjutkan!");
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
      alert("Harap berikan catatan revisi sebelum menolak.");
      return;
    }
    try {
      setLoading(true);
      await rejectWorkflow(workflow.id, comment);
      setFeedbackMsg("Penolakan diteruskan. Agent akan melakukan revisi sesuai catatan.");
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
        : "bg-amber-950/20 border-amber-500/50 ring-1 ring-amber-500/20"
    }`}>
      <div className="flex items-start gap-3">
        <div className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl ${
          isDeadlock ? "bg-rose-500/20 text-rose-400" : "bg-amber-500/20 text-amber-400"
        }`}>
          <ShieldAlert className="h-5 w-5" />
        </div>

        <div className="flex-1">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="text-sm sm:text-base font-bold text-white">
              {isDeadlock
                ? "ESKALASI DEADLOCK: Keputusan Arsitek / Manusia Dibutuhkan"
                : isProdGate
                ? "GERBANG PRODUKSI (PHASE 8.5): Verifikasi Rilis Akhir"
                : "SIGN-OFF KEBUTUHAN (PHASE 0): Persetujuan User Stories"}
            </h3>
            <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-300">
              ID: {workflow.id}
            </span>
          </div>

          <p className="text-xs sm:text-sm text-slate-300 mt-1 leading-relaxed">
            {isDeadlock
              ? "Code Review Consortium mendeteksi kegagalan 3x berturut-turut pada implementasi saat ini. Pilih override untuk meloloskan ke QA atau instruksikan arahan revisi spesifik."
              : isProdGate
              ? "Seluruh pengujian DAST (OWASP ZAP) dan Stress Test (k6) telah selesai di Staging Sandbox. Berikan sign-off untuk deploy ke live production."
              : "Spesifikasi bisnis dan kriteria penerimaan Gherkin telah disusun oleh PO/BA agent. Mohon tinjau dan berikan konfirmasi untuk melanjutkan ke perancangan arsitektur."}
          </p>

          {feedbackMsg && (
            <div className="mt-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 p-2 text-xs font-medium text-emerald-400">
              {feedbackMsg}
            </div>
          )}

          {/* Revision / Feedback Note Input */}
          <div className="mt-3">
            <label className="block text-[11px] font-medium text-slate-400 mb-1">
              Catatan / Arahan Revisi Khusus (Opsional jika Approve, Wajib jika Reject):
            </label>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Contoh: Pastikan error response format RFC 7807 dan coverage branch ditambah..."
              className="w-full rounded-xl bg-slate-900/90 border border-slate-700/80 px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 resize-none h-18"
            />
          </div>

          {/* Action Buttons (Touch Friendly) */}
          <div className="mt-3 flex flex-wrap items-center gap-2 sm:gap-3">
            <button
              onClick={handleApprove}
              disabled={loading}
              className="touch-target flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-xs sm:text-sm font-semibold text-white shadow-md shadow-emerald-600/30 hover:bg-emerald-500 active:scale-95 disabled:opacity-50 transition"
            >
              {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
              <span>{isDeadlock ? "Override & Loloskan" : "Setujui & Lanjutkan"}</span>
            </button>

            <button
              onClick={handleReject}
              disabled={loading}
              className="touch-target flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 rounded-xl bg-slate-800 border border-rose-500/40 px-4 py-2.5 text-xs sm:text-sm font-semibold text-rose-300 hover:bg-rose-950/40 active:scale-95 disabled:opacity-50 transition"
            >
              {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <XCircle className="h-4 w-4" />}
              <span>Tolak dengan Catatan</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
