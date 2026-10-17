"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import { WorkflowSummary } from "@/lib/types";
import { fetchWorkflows, advanceWorkflow, deleteWorkflow } from "@/lib/api";
import { Header } from "@/components/Header";
import { TabKey } from "@/components/BottomNav";
import { WorkflowPipeline } from "@/components/WorkflowPipeline";
import { HumanDecisionCard } from "@/components/HumanDecisionCard";
import { AgentStatusGrid } from "@/components/AgentStatusGrid";
import { TerminalLogStreamer } from "@/components/TerminalLogStreamer";
import { StartWorkflowModal } from "@/components/StartWorkflowModal";
import { CouncilTriadCard } from "@/components/CouncilTriadCard";
import { ProjectArtifactsExplorer } from "@/components/ProjectArtifactsExplorer";
import { TeamTopologyArtifactsExplorer } from "@/components/TeamTopologyArtifactsExplorer";
import { DashboardSummaryKPIs, ProjectFilterStatus } from "@/components/DashboardSummaryKPIs";
import { ProjectWorkflowTable } from "@/components/ProjectWorkflowTable";
import { HitlAuditTrailCard } from "@/components/HitlAuditTrailCard";
import { useLanguage } from "@/lib/i18n";
import {
  Play,
  RefreshCw,
  Clock,
  Loader2,
  FolderGit2,
  Search,
  Filter,
  CheckCircle2,
  AlertTriangle,
  Zap,
  Sparkles,
} from "lucide-react";

export default function DashboardPage() {
  const { language, t } = useLanguage();
  const [workflows, setWorkflows] = useState<WorkflowSummary[]>([]);
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabKey>("overview");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [advancingId, setAdvancingId] = useState<string | null>(null);

  // Filter status state (synced with KPIs & Table)
  const [filterStatus, setFilterStatus] = useState<ProjectFilterStatus>("ALL");

  const loadData = useCallback(async () => {
    try {
      const data = await fetchWorkflows();
      setWorkflows(data);
      if (data.length > 0 && !selectedWorkflowId) {
        // Default: pilih yang butuh sign-off manusia atau yang pertama
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
    const interval = setInterval(loadData, 4000); // 4s polling fallback
    return () => clearInterval(interval);
  }, [loadData]);

  const selectedWorkflow =
    workflows.find((w) => w.id === selectedWorkflowId) || workflows[0];
  const pendingApprovals = workflows.filter((w) => w.waitingForHuman);

  // Handle delete workflow & auto-reselect
  const handleDeleteWorkflow = async (id: string) => {
    try {
      await deleteWorkflow(id);
      if (selectedWorkflowId === id) {
        const remaining = workflows.filter((w) => w.id !== id);
        setSelectedWorkflowId(remaining.length > 0 ? remaining[0].id : null);
      }
      await loadData();
    } catch (err: any) {
      alert("Gagal menghapus proyek: " + (err?.message || err));
    }
  };

  const handleAdvance = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      setAdvancingId(id);
      await advanceWorkflow(id, "Manual advance from dashboard");
      await loadData();
    } catch (err: any) {
      alert("Error: " + err.message);
    } finally {
      setAdvancingId(null);
    }
  };

  return (
    <div className="min-h-screen bg-[#090d16] text-slate-100 flex flex-col">
      <Header
        activeTab={activeTab}
        onTabChange={setActiveTab}
        onOpenNewTaskModal={() => setIsModalOpen(true)}
        pendingApprovalsCount={pendingApprovals.length}
      />

      <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-[1720px] w-full mx-auto space-y-6 pb-12">
        {/* 1. Executive Summary KPIs: Total, Running, Waiting HITL, Completed */}
        <DashboardSummaryKPIs
          workflows={workflows}
          activeFilter={filterStatus}
          onFilterChange={(newFilter) => setFilterStatus(newFilter)}
        />

        {/* 2. Top Urgent Alert if Human-in-the-Loop Gate is Active */}
        {pendingApprovals.length > 0 && (
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-amber-400 font-semibold text-xs tracking-wider uppercase">
              <AlertTriangle className="h-4 w-4" />
              <span>{t.hitl.bannerTitle(pendingApprovals.length)}</span>
            </div>
            {pendingApprovals.map((w) => (
              <HumanDecisionCard
                key={w.id}
                workflow={w}
                onDecisionProcessed={loadData}
              />
            ))}
          </div>
        )}

        {/* 3. Tab Views */}
        {activeTab === "overview" && (
          <div className="space-y-6">
            {/* Multi-Project Workflows Table with Max 5 Pagination, Real-Time Search, Detail & Delete */}
            <ProjectWorkflowTable
              workflows={workflows}
              selectedWorkflowId={selectedWorkflowId}
              onSelectWorkflow={(id) => setSelectedWorkflowId(id)}
              onDeleteWorkflow={handleDeleteWorkflow}
              loading={loading}
              onRefresh={loadData}
              activeFilter={filterStatus}
              onFilterChange={(newFilter) => setFilterStatus(newFilter)}
            />

            {/* Active Workflow SDLC Pipeline Tracker for Selected Project */}
            {selectedWorkflow && (
              <section id="selected-project-pipeline-detail" className="space-y-4 scroll-mt-20">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                      {t.pipeline.selectedPipelineTitle}
                    </span>
                    <span className="inline-flex items-center gap-1 font-mono font-bold text-xs text-cyan-400 bg-cyan-950/40 border border-cyan-800/60 px-2 py-0.5 rounded-lg">
                      <FolderGit2 className="h-3 w-3" />
                      {selectedWorkflow.projectId || selectedWorkflow.id}
                    </span>
                    <span className="text-xs text-white font-semibold truncate max-w-sm">
                      {selectedWorkflow.taskName}
                    </span>
                  </div>

                  {selectedWorkflow.agentRunning && (
                    <div className="inline-flex items-center gap-1.5 rounded-full bg-cyan-500/10 border border-cyan-500/30 px-2.5 py-0.5 text-xs font-semibold text-cyan-400 animate-pulse">
                      <Loader2 className="h-3 w-3 animate-spin" />
                      <span>{t.pipeline.agentWorking(selectedWorkflow.phaseProgressPercentage || 0)}</span>
                    </div>
                  )}
                </div>

                <WorkflowPipeline
                  currentState={selectedWorkflow.currentState}
                  reviewLoopCount={selectedWorkflow.reviewLoopCount}
                  coveragePercentage={selectedWorkflow.coveragePercentage}
                />

                {/* Council Triad Governance Widget */}
                <CouncilTriadCard
                  workflow={selectedWorkflow}
                  onReviewSubmitted={loadData}
                />

                {/* Human-in-the-Loop (HITL) Action History & Audit Trail */}
                <HitlAuditTrailCard
                  workflow={selectedWorkflow}
                  onRefresh={loadData}
                />
              </section>
            )}

            {/* Unified 5-Team Topology Status & Project Artifacts Explorer */}
            {selectedWorkflow ? (
              <section>
                <TeamTopologyArtifactsExplorer workflow={selectedWorkflow} />
              </section>
            ) : (
              <section>
                <AgentStatusGrid currentState="PHASE_0_REQUIREMENT_DISCOVERY" />
              </section>
            )}

            {/* Live Streaming Console Scoped Per Project */}
            <section>
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <h2 className="text-sm sm:text-base font-bold text-white">
                    {t.telemetry.title}
                  </h2>
                  <span className="text-xs text-slate-400">
                    {t.telemetry.subtitle}
                  </span>
                </div>
              </div>
              <TerminalLogStreamer
                workflowId={selectedWorkflow?.id}
                workflows={workflows}
                onSelectWorkflowId={(id) => setSelectedWorkflowId(id)}
              />
            </section>
          </div>
        )}

        {/* Tab: Pipeline */}
        {activeTab === "pipeline" && selectedWorkflow && (
          <div className="space-y-6">
            <div className="rounded-2xl glass-panel p-5">
              <h3 className="text-sm font-bold text-white mb-2">
                {t.pipeline.pipelineFlowDetail(selectedWorkflow.taskName, selectedWorkflow.projectId || selectedWorkflow.id)}
              </h3>
              <p className="text-xs text-slate-400 mb-4">
                {selectedWorkflow.description}
              </p>
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

            {/* Artifacts & SDLC Log Explorer in Pipeline Tab */}
            <TeamTopologyArtifactsExplorer workflow={selectedWorkflow} />

            {/* Human-in-the-Loop (HITL) Action History & Audit Trail */}
            <HitlAuditTrailCard
              workflow={selectedWorkflow}
              onRefresh={loadData}
            />
          </div>
        )}

        {/* Tab: Agents */}
        {activeTab === "agents" && selectedWorkflow && (
          <div className="space-y-6">
            <AgentStatusGrid currentState={selectedWorkflow.currentState} />
          </div>
        )}

        {/* Tab: Logs (Live Console Dedicated) */}
        {activeTab === "logs" && (
          <div className="space-y-4">
            <TerminalLogStreamer
              workflowId={selectedWorkflow?.id}
              workflows={workflows}
              onSelectWorkflowId={(id) => setSelectedWorkflowId(id)}
            />
          </div>
        )}
      </main>

      {/* Modal Start Workflow */}
      <StartWorkflowModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onCreated={loadData}
      />
    </div>
  );
}
