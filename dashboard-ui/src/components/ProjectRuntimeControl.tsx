"use client";

import React, { useState, useEffect } from "react";
import { Play, Square, ExternalLink, RefreshCw, CheckCircle2, AlertCircle, Server } from "lucide-react";
import { fetchProjectRuntimeStatus, startProjectRuntime, stopProjectRuntime, ProjectRuntimeStatus } from "../lib/api";

interface ProjectRuntimeControlProps {
  projectId: string;
}

export function ProjectRuntimeControl({ projectId }: ProjectRuntimeControlProps) {
  const [status, setStatus] = useState<ProjectRuntimeStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);

  const loadStatus = async () => {
    try {
      const data = await fetchProjectRuntimeStatus(projectId);
      setStatus(data);
    } catch (err: any) {
      console.warn("Could not load runtime status:", err.message);
    }
  };

  useEffect(() => {
    loadStatus();
    // Poll every 3s if starting
    const interval = setInterval(() => {
      if (status?.status === "STARTING" || status?.status === "STOPPING") {
        loadStatus();
      }
    }, 3000);
    return () => clearInterval(interval);
  }, [projectId, status?.status]);

  const handleStart = async () => {
    setLoading(true);
    setFeedback("Menginisialisasi Docker Compose stack di background...");
    try {
      const res = await startProjectRuntime(projectId);
      setStatus(res);
      setFeedback("Stack container sedang di-build dan dinyalakan...");
    } catch (err: any) {
      setFeedback(`Gagal menjalankan stack: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleStop = async () => {
    setLoading(true);
    setFeedback("Menghentikan container stack...");
    try {
      const res = await stopProjectRuntime(projectId);
      setStatus(res);
      setFeedback("Container stack dihentikan.");
    } catch (err: any) {
      setFeedback(`Gagal menghentikan stack: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const isRunning = status?.status === "RUNNING";
  const isStarting = status?.status === "STARTING";
  const isStopping = status?.status === "STOPPING";

  return (
    <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-xl">
      <div className="flex items-center gap-3.5">
        <div className={`p-2.5 rounded-xl border ${
          isRunning
            ? "bg-emerald-950/60 border-emerald-500/40 text-emerald-400"
            : isStarting
            ? "bg-amber-950/60 border-amber-500/40 text-amber-400 animate-pulse"
            : "bg-slate-800/80 border-slate-700 text-slate-400"
        }`}>
          <Server className="w-5 h-5" />
        </div>

        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-white tracking-wide">Autonomous Runtime Stack</span>
            <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${
              isRunning
                ? "bg-emerald-950 text-emerald-400 border border-emerald-700/60"
                : isStarting
                ? "bg-amber-950 text-amber-400 border border-amber-700/60 animate-pulse"
                : "bg-slate-950 text-slate-400 border border-slate-800"
            }`}>
              {status?.status || "STOPPED"}
            </span>
          </div>

          <p className="text-xs text-slate-400 mt-1">
            {status?.message || "Container stack offline. Klik jalankan untuk spin up Postgres, Redis, Kafka, Spring Boot & Next.js."}
          </p>

          {status && (
            <div className="flex flex-wrap items-center gap-2 mt-2 text-[10px] font-mono text-slate-400">
              <span className="bg-slate-950 px-2 py-0.5 rounded border border-slate-800">
                FE: <strong className="text-blue-400">{status.frontendPort}</strong>
              </span>
              <span className="bg-slate-950 px-2 py-0.5 rounded border border-slate-800">
                BE: <strong className="text-emerald-400">{status.backendPort}</strong>
              </span>
              <span className="bg-slate-950 px-2 py-0.5 rounded border border-slate-800">
                PG: <strong className="text-purple-400">{status.postgresPort}</strong>
              </span>
              <span className="bg-slate-950 px-2 py-0.5 rounded border border-slate-800">
                REDIS: <strong className="text-red-400">{status.redisPort}</strong>
              </span>
              <span className="bg-slate-950 px-2 py-0.5 rounded border border-slate-800">
                KAFKA: <strong className="text-amber-400">{status.kafkaPort}</strong>
              </span>
            </div>
          )}
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2.5">
        {isRunning && (
          <a
            href={status?.frontendUrl || `http://localhost:3200`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 px-3.5 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white rounded-xl text-xs font-bold shadow-lg shadow-blue-500/20 transition group"
          >
            <span>Buka Tab Frontend ({status?.frontendPort || 3200})</span>
            <ExternalLink className="w-3.5 h-3.5 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition" />
          </a>
        )}

        {!isRunning && !isStarting && (
          <button
            onClick={handleStart}
            disabled={loading}
            className="flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white rounded-xl text-xs font-bold shadow-lg shadow-blue-600/20 transition"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>Jalankan App Stack</span>
          </button>
        )}

        {isStarting && (
          <button
            disabled
            className="flex items-center gap-1.5 px-4 py-2 bg-amber-600/80 text-white rounded-xl text-xs font-bold animate-pulse cursor-not-allowed"
          >
            <RefreshCw className="w-3.5 h-3.5 animate-spin" />
            <span>Sedang Membangun Stack...</span>
          </button>
        )}

        {(isRunning || isStarting) && (
          <button
            onClick={handleStop}
            disabled={loading || isStopping}
            className="flex items-center gap-1.5 px-3 py-2 bg-slate-950 hover:bg-red-950/60 text-red-400 border border-red-900/40 hover:border-red-700/60 rounded-xl text-xs font-bold transition"
          >
            <Square className="w-3 h-3 fill-current" />
            <span>Hentikan Stack</span>
          </button>
        )}

        <button
          onClick={loadStatus}
          title="Refresh Status"
          className="p-2 bg-slate-950 hover:bg-slate-800 text-slate-400 hover:text-white rounded-xl border border-slate-800 transition"
        >
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
