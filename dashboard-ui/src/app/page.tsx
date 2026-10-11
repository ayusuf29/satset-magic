"use client";

import React, { useState, useEffect, useCallback } from "react";
import { WorkflowSummary } from "@/lib/types";
import { fetchWorkflows, advanceWorkflow } from "@/lib/api";
import { Header } from "@/components/Header";
import { BottomNav, TabKey } from "@/components/BottomNav";
import { Sidebar } from "@/components/Sidebar";
import { WorkflowPipeline } from "@/components/WorkflowPipeline";
import { HumanDecisionCard } from "@/components/HumanDecisionCard";
import { AgentStatusGrid } from "@/components/AgentStatusGrid";
import { TerminalLogStreamer } from "@/components/TerminalLogStreamer";
import { StartWorkflowModal } from "@/components/StartWorkflowModal";
import { Play, RefreshCw, AlertCircle, CheckCircle2, Clock, CheckCheck, Loader2 } from "lucide-react";

export default function DashboardPage() {
  const [workflows, setWorkflows] = useState<WorkflowSummary[]>([]);
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabKey>("overview");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [advancingId, setAdvancingId] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      const data = await fetchWorkflows();
      setWorkflows(data);
      if (data.length > 0 && !selectedWorkflowId) {
        // Select the one waiting for human or the first one
        const waiting = data.find((w) => w.waitingForHuman);
        setSelectedWorkflowId(waiting ? waiting.id : data[0].id);
      }
    } catch (err) {
      console.error("Failed to load workflows:", err);
    } finally {
      setLoading(false);
    }
  }, [selectedWorkflowId]);

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 5000); // 5s polling fallback
    return () => clearInterval(interval);
  }, [loadData]);

  const selectedWorkflow = workflows.find((w) => w.id === selectedWorkflowId) || workflows[0];
  const pendingApprovals = workflows.filter((w) => w.waitingForHuman);

  const handleAdvance = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      setAdvancingId(id);
      await advanceWorkflow(id, "Manual advance from mobile dashboard");
      await loadData();
    } catch (err: any) {
      alert("Error: " + err.message);
    } finally {
      setAdvancingId(null);
    }
  };

  return (
    <div className="min-h-screen bg-[#090d16] text-slate-100 flex flex-col md:flex-row">
      {/* Desktop Sidebar */}
      <Sidebar
        activeTab={activeTab}
        onTabChange={setActiveTab}
        onOpenNewTaskModal={() => setIsModalOpen(true)}
        pendingApprovalsCount={pendingApprovals.length}
      />

      {/* Main App Content Area */}
      <div className="flex-1 flex flex-col min-w-0 pb-20 md:pb-8">
        <Header
          onOpenNewTaskModal={() => setIsModalOpen(true)}
          pendingApprovalsCount={pendingApprovals.length}
        />

        <main className="flex-1 p-3.5 sm:p-6 max-w-7xl w-full mx-auto space-y-4 sm:space-y-6">
          {/* Top Urgent Alert if Human-in-the-Loop Gate is Active */}
          {pendingApprovals.length > 0 && (
            <div className="space-y-3">
              {pendingApprovals.map((w) => (
                <HumanDecisionCard
                  key={w.id}
                  workflow={w}
                  onDecisionProcessed={loadData}
                />
              ))}
            </div>
          )}

          {/* Active Workflow SDLC Pipeline Tracker */}
          {selectedWorkflow && (
            <section>
              <WorkflowPipeline
                currentState={selectedWorkflow.currentState}
                reviewLoopCount={selectedWorkflow.reviewLoopCount}
                coveragePercentage={selectedWorkflow.coveragePercentage}
              />
            </section>
          )}

          {/* Tab Views */}
          {activeTab === "overview" && (
            <div className="space-y-6">
              {/* Task Selector Card Grid */}
              <section>
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <h2 className="text-sm sm:text-base font-bold text-white">
                      Daftar Task & Workflows
                    </h2>
                    <span className="rounded-full bg-slate-800 px-2 py-0.5 text-xs text-slate-300">
                      {workflows.length}
                    </span>
                  </div>
                  <button
                    onClick={loadData}
                    className="touch-target p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
                    title="Refresh Status"
                  >
                    <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
                  </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 sm:gap-4">
                  {workflows.map((wf) => {
                    const isSelected = wf.id === selectedWorkflow?.id;
                    const isDeadlock = wf.currentState === "PHASE_6_DEADLOCK_WAITING_HUMAN";
                    const isWaiting = wf.waitingForHuman;

                    return (
                      <div
                        key={wf.id}
                        onClick={() => setSelectedWorkflowId(wf.id)}
                        className={`cursor-pointer rounded-2xl p-4 glass-card border transition-all ${
                          isSelected
                            ? "border-blue-500/80 bg-slate-900/90 ring-1 ring-blue-500/30"
                            : "border-slate-800 hover:border-slate-700 bg-slate-950/40"
                        }`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-[11px] text-blue-400 font-semibold">
                              {wf.id}
                            </span>
                            {isDeadlock ? (
                              <span className="inline-flex items-center gap-1 rounded-full bg-rose-500/10 px-2 py-0.5 text-[10px] font-bold text-rose-400 border border-rose-500/30 animate-pulse">
                                DEADLOCK
                              </span>
                            ) : isWaiting ? (
                              <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2 py-0.5 text-[10px] font-semibold text-amber-400 border border-amber-500/30">
                                WAITING
                              </span>
                            ) : wf.currentState === "PHASE_9_RELEASED" ? (
                              <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-400 border border-emerald-500/30">
                                RELEASED
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 rounded-full bg-blue-500/10 px-2 py-0.5 text-[10px] font-semibold text-blue-400 border border-blue-500/30">
                                RUNNING
                              </span>
                            )}
                          </div>

                          {/* Quick Advance Button (if not waiting human) */}
                          {!wf.waitingForHuman && wf.currentState !== "PHASE_9_RELEASED" && (
                            <button
                              onClick={(e) => handleAdvance(wf.id, e)}
                              disabled={advancingId === wf.id}
                              className="touch-target inline-flex items-center gap-1 rounded-lg bg-blue-600/80 hover:bg-blue-600 px-2.5 py-1 text-[11px] font-semibold text-white shadow transition active:scale-95 disabled:opacity-50"
                            >
                              {advancingId === wf.id ? (
                                <Loader2 className="h-3 w-3 animate-spin" />
                              ) : (
                                <Play className="h-3 w-3" />
                              )}
                              <span>Advance</span>
                            </button>
                          )}
                        </div>

                        <h3 className="mt-2 text-xs sm:text-sm font-bold text-white tracking-tight line-clamp-1">
                          {wf.taskName}
                        </h3>
                        <p className="mt-1 text-[11px] text-slate-400 line-clamp-2">
                          {wf.description}
                        </p>

                        <div className="mt-3 pt-2.5 border-t border-slate-800 flex items-center justify-between text-[11px] text-slate-400">
                          <div className="flex items-center gap-2">
                            <span>Loop: <strong className="text-white">{wf.reviewLoopCount}/3</strong></span>
                            <span>•</span>
                            <span>Cov: <strong className="text-white">{wf.coveragePercentage}%</strong></span>
                          </div>
                          <div className="flex items-center gap-1 text-[10px] text-slate-500">
                            <Clock className="h-3 w-3" />
                            <span>{new Date(wf.updatedAt).toLocaleTimeString()}</span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </section>

              {/* 5-Team Topology Status */}
              <section>
                <AgentStatusGrid currentState={selectedWorkflow?.currentState || "PHASE_0_REQUIREMENT_DISCOVERY"} />
              </section>

              {/* Live Streaming Console */}
              <section>
                <div className="flex items-center justify-between mb-3">
                  <h2 className="text-sm sm:text-base font-bold text-white">
                    Live Telemetry Terminal
                  </h2>
                </div>
                <TerminalLogStreamer workflowId={selectedWorkflow?.id} />
              </section>
            </div>
          )}

          {activeTab === "pipeline" && selectedWorkflow && (
            <div className="space-y-6">
              <div className="rounded-2xl glass-panel p-5">
                <h3 className="text-sm font-bold text-white mb-2">Detail Alur Pipeline: {selectedWorkflow.taskName}</h3>
                <p className="text-xs text-slate-400 mb-4">{selectedWorkflow.description}</p>
                <WorkflowPipeline
                  currentState={selectedWorkflow.currentState}
                  reviewLoopCount={selectedWorkflow.reviewLoopCount}
                  coveragePercentage={selectedWorkflow.coveragePercentage}
                />
              </div>

              {selectedWorkflow.waitingForHuman && (
                <HumanDecisionCard
                  workflow={selectedWorkflow}
                  onDecisionProcessed={loadData}
                />
              )}
            </div>
          )}

          {activeTab === "agents" && selectedWorkflow && (
            <div className="space-y-6">
              <AgentStatusGrid currentState={selectedWorkflow.currentState} />
            </div>
          )}

          {activeTab === "logs" && (
            <div className="space-y-4">
              <TerminalLogStreamer workflowId={selectedWorkflow?.id} />
            </div>
          )}
        </main>
      </div>

      {/* Mobile Bottom Navigation */}
      <BottomNav
        activeTab={activeTab}
        onTabChange={setActiveTab}
        onOpenNewTaskModal={() => setIsModalOpen(true)}
        pendingApprovalsCount={pendingApprovals.length}
      />

      {/* Modal Start Workflow */}
      <StartWorkflowModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onCreated={loadData}
      />
    </div>
  );
}
