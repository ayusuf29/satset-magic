"use client";

import React, { useState, useEffect, useRef } from "react";
import { Terminal, Copy, Check, Trash2, ArrowDownCircle, Wifi, WifiOff } from "lucide-react";

export interface LogEntry {
  id: string;
  time: string;
  type: "AGENT_START" | "AGENT_OUTPUT" | "STATE_TRANSITION" | "HUMAN_GATE_TRIGGERED" | "ALERT" | "INFO";
  message: string;
}

interface TerminalLogStreamerProps {
  workflowId?: string;
}

export function TerminalLogStreamer({ workflowId }: TerminalLogStreamerProps) {
  const [logs, setLogs] = useState<LogEntry[]>([
    {
      id: "init-1",
      time: new Date().toLocaleTimeString(),
      type: "INFO",
      message: "Satset Orchestrator Engine initialized. Connected to Java Spring Boot 3.3 runtime.",
    },
    {
      id: "init-2",
      time: new Date().toLocaleTimeString(),
      type: "STATE_TRANSITION",
      message: "Orchestrator ready. Listening for SSE events on /api/workflow/stream",
    },
  ]);

  const [filter, setFilter] = useState<string>("ALL");
  const [autoScroll, setAutoScroll] = useState<boolean>(true);
  const [copied, setCopied] = useState<boolean>(false);
  const [connected, setConnected] = useState<boolean>(false);
  const logContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const apiBase = process.env.NEXT_PUBLIC_ORCHESTRATOR_API_URL || "http://localhost:8080";
    const streamUrl = workflowId
      ? `${apiBase}/api/workflow/${workflowId}/stream`
      : `${apiBase}/api/workflow/stream`;

    let eventSource: EventSource | null = null;

    try {
      eventSource = new EventSource(streamUrl);

      eventSource.onopen = () => {
        setConnected(true);
      };

      eventSource.onerror = () => {
        setConnected(false);
      };

      const handleEvent = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          const newEntry: LogEntry = {
            id: data.id || Math.random().toString(),
            time: new Date(data.timestamp || Date.now()).toLocaleTimeString(),
            type: data.type || "INFO",
            message: `[${data.state || "SYSTEM"}] ${data.message || ""}`,
          };
          setLogs((prev) => [...prev, newEntry]);
        } catch {
          // Plain text fallback
          setLogs((prev) => [
            ...prev,
            {
              id: Math.random().toString(),
              time: new Date().toLocaleTimeString(),
              type: "INFO",
              message: event.data,
            },
          ]);
        }
      };

      eventSource.onmessage = handleEvent;
      eventSource.addEventListener("STATE_TRANSITION", handleEvent);
      eventSource.addEventListener("HUMAN_GATE_TRIGGERED", handleEvent);
      eventSource.addEventListener("AGENT_START", handleEvent);
      eventSource.addEventListener("AGENT_OUTPUT", handleEvent);
      eventSource.addEventListener("AGENT_ERROR", handleEvent);
    } catch {
      setConnected(false);
    }

    return () => {
      if (eventSource) {
        eventSource.close();
      }
    };
  }, [workflowId]);

  useEffect(() => {
    if (autoScroll && logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [logs, autoScroll]);

  const filteredLogs = logs.filter((l) => {
    if (filter === "ALL") return true;
    if (filter === "AGENT") return l.type.startsWith("AGENT");
    if (filter === "GATE") return l.type === "HUMAN_GATE_TRIGGERED";
    if (filter === "TRANSITION") return l.type === "STATE_TRANSITION";
    return true;
  });

  const handleCopy = () => {
    const text = logs.map((l) => `[${l.time}] [${l.type}] ${l.message}`).join("\n");
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleClear = () => {
    setLogs([]);
  };

  return (
    <div className="w-full rounded-2xl glass-panel border border-slate-800 overflow-hidden flex flex-col h-[380px] sm:h-[440px]">
      {/* Terminal Title Bar */}
      <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-2.5 bg-slate-950/80 border-b border-slate-800">
        <div className="flex items-center gap-2">
          <div className="flex gap-1.5">
            <span className="h-3 w-3 rounded-full bg-rose-500/80 inline-block"></span>
            <span className="h-3 w-3 rounded-full bg-amber-500/80 inline-block"></span>
            <span className="h-3 w-3 rounded-full bg-emerald-500/80 inline-block"></span>
          </div>
          <div className="flex items-center gap-1.5 ml-2 text-xs font-mono font-medium text-slate-300">
            <Terminal className="h-3.5 w-3.5 text-blue-400" />
            <span>Agentic Telemetry Stream</span>
          </div>
          {connected ? (
            <span className="inline-flex items-center gap-1 text-[10px] text-emerald-400 ml-2">
              <Wifi className="h-3 w-3" /> Live SSE
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 text-[10px] text-slate-400 ml-2">
              <WifiOff className="h-3 w-3" /> Standby
            </span>
          )}
        </div>

        {/* Filter Pills & Actions */}
        <div className="flex items-center gap-1.5 text-xs">
          <div className="flex rounded-lg bg-slate-900 border border-slate-800 p-0.5 text-[11px]">
            {["ALL", "AGENT", "GATE", "TRANSITION"].map((f) => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`px-2 py-0.5 rounded font-medium transition ${
                  filter === f ? "bg-blue-600 text-white" : "text-slate-400 hover:text-slate-200"
                }`}
              >
                {f}
              </button>
            ))}
          </div>

          <button
            onClick={() => setAutoScroll(!autoScroll)}
            title="Toggle Auto Scroll"
            className={`p-1.5 rounded-lg border transition ${
              autoScroll
                ? "bg-blue-500/10 border-blue-500/30 text-blue-400"
                : "bg-slate-900 border-slate-800 text-slate-400"
            }`}
          >
            <ArrowDownCircle className="h-3.5 w-3.5" />
          </button>

          <button
            onClick={handleCopy}
            title="Copy Logs"
            className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-300 hover:text-white transition"
          >
            {copied ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
          </button>

          <button
            onClick={handleClear}
            title="Clear Terminal"
            className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-rose-400 transition"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      {/* Terminal Log Output Window */}
      <div
        ref={logContainerRef}
        className="flex-1 p-3.5 overflow-y-auto font-mono text-xs sm:text-[13px] bg-[#070b12] space-y-1.5 select-text"
      >
        {filteredLogs.length === 0 ? (
          <div className="text-slate-600 italic py-8 text-center">Belum ada log aktivitas yang cocok dengan filter.</div>
        ) : (
          filteredLogs.map((log) => {
            let typeColor = "text-blue-400";
            if (log.type === "HUMAN_GATE_TRIGGERED") typeColor = "text-amber-400 font-bold";
            if (log.type === "ALERT" || log.type.includes("ERROR")) typeColor = "text-rose-400 font-bold";
            if (log.type === "STATE_TRANSITION") typeColor = "text-purple-400";
            if (log.type.startsWith("AGENT")) typeColor = "text-emerald-400";

            return (
              <div key={log.id} className="leading-relaxed flex items-start gap-2 hover:bg-slate-900/40 rounded px-1">
                <span className="text-slate-500 flex-shrink-0 text-[11px] select-none">[{log.time}]</span>
                <span className={`text-[11px] flex-shrink-0 uppercase font-semibold ${typeColor}`}>
                  [{log.type}]
                </span>
                <span className="text-slate-200 break-words">{log.message}</span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
