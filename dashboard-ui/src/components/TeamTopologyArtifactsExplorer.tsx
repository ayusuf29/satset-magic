"use client";

import React, { useState, useEffect, useMemo } from "react";
import { WorkflowSummary, WorkflowState } from "@/lib/types";
import { fetchArtifacts } from "@/lib/api";
import {
  Users,
  Code,
  Terminal,
  Activity,
  ShieldAlert,
  FileText,
  BookOpen,
  FolderTree,
  Search,
  RefreshCw,
  ExternalLink,
  ShieldCheck,
  CheckCircle2,
  FileCode,
  Layers,
  Sparkles,
  X,
  Database,
  Server,
  FileTerminal,
} from "lucide-react";
import { DocumentViewerModal } from "./DocumentViewerModal";
import { useLanguage } from "@/lib/i18n";

interface TeamTopologyArtifactsExplorerProps {
  workflow: WorkflowSummary;
}

interface ArtifactItem {
  name: string;
  path: string;
  type: string;
}

interface TeamDefinition {
  id: string;
  name: string;
  category: string;
  leadRole: string;
  members: string[];
  icon: React.ReactNode;
  activeInPhases: WorkflowState[];
  accentColor: string;
  borderActiveColor: string;
  bgActiveColor: string;
}

const TEAMS: TeamDefinition[] = [
  {
    id: "product_design",
    name: "Product & Design",
    category: "Domain 1 & 2",
    leadRole: "PO / BA & UI/UX Specialist",
    members: ["Spec Writer Agent", "UI/UX Pro Max Agent"],
    icon: <Users className="h-5 w-5 text-blue-400" />,
    activeInPhases: [
      "PHASE_0_REQUIREMENT_DISCOVERY",
      "PHASE_0_WAITING_HUMAN_SIGN_OFF",
      "PHASE_2_BACKLOG_GENERATION",
    ],
    accentColor: "text-blue-400",
    borderActiveColor: "border-blue-500 ring-2 ring-blue-500/40",
    bgActiveColor: "bg-blue-950/40",
  },
  {
    id: "dev_team",
    name: "Development Team",
    category: "Domain 3, 4 & 7",
    leadRole: "Fullstack Engineers & Review Lead",
    members: ["Spring Boot Worker", "Frontend Next.js Worker", "Senior Reviewer"],
    icon: <Code className="h-5 w-5 text-emerald-400" />,
    activeInPhases: [
      "PHASE_4_TDD_PLANNING",
      "PHASE_5_DEVELOPMENT_IMPLEMENTATION",
      "PHASE_6_CODE_REVIEW",
      "PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE",
      "PHASE_6_DEADLOCK_WAITING_HUMAN",
    ],
    accentColor: "text-emerald-400",
    borderActiveColor: "border-emerald-500 ring-2 ring-emerald-500/40",
    bgActiveColor: "bg-emerald-950/40",
  },
  {
    id: "cicd_staging",
    name: "CI/CD & Staging",
    category: "Domain 8 & 9",
    leadRole: "DevOps Runner & Sandbox Platform",
    members: ["Docker Compose Pro", "Ephemeral Staging Runner"],
    icon: <Terminal className="h-5 w-5 text-indigo-400" />,
    activeInPhases: ["PHASE_3_TASK_DISPATCHING", "PHASE_7_QA_VERIFICATION"],
    accentColor: "text-indigo-400",
    borderActiveColor: "border-indigo-500 ring-2 ring-indigo-500/40",
    bgActiveColor: "bg-indigo-950/40",
  },
  {
    id: "production_sre",
    name: "Production & SRE",
    category: "Domain 10",
    leadRole: "Reliability & Distributed Telemetry",
    members: ["OTel Instrumentor", "Prometheus/Grafana Sentinel"],
    icon: <Activity className="h-5 w-5 text-amber-400" />,
    activeInPhases: ["PHASE_9_RELEASED"],
    accentColor: "text-amber-400",
    borderActiveColor: "border-amber-500 ring-2 ring-amber-500/40",
    bgActiveColor: "bg-amber-950/40",
  },
  {
    id: "shared_enabling",
    name: "Shared Enabling",
    category: "Domain 5, 6 & 9",
    leadRole: "Architect & Pentest Specialists",
    members: ["System Design Architect", "AppSec Auditor", "DAST/MAST Pentest Scout"],
    icon: <ShieldAlert className="h-5 w-5 text-rose-400" />,
    activeInPhases: ["PHASE_1_ARCHITECTURE_AND_DESIGN", "PHASE_8_READY_FOR_PRODUCTION_GATE"],
    accentColor: "text-rose-400",
    borderActiveColor: "border-rose-500 ring-2 ring-rose-500/40",
    bgActiveColor: "bg-rose-950/40",
  },
];

export interface DocumentClassification {
  teamName: string;
  phaseCode: string;
  phaseLabel: string;
  badgeClass: string;
}

export function classifyDocument(filePath: string, language: "en" | "id"): DocumentClassification {
  const p = filePath.toLowerCase().replace(/\\/g, "/");

  // Umum / Lintas Tim
  if (p === "sdlc_log.md" || p.endsWith("/sdlc_log.md")) {
    return {
      teamName: "Umum / Lintas Tim",
      phaseCode: "LINTAS_FASE",
      phaseLabel: language === "en" ? "Cross-Phase: SDLC Journal" : "Lintas Fase: Jurnal SDLC",
      badgeClass: "bg-indigo-500/20 text-indigo-300 border-indigo-500/30",
    };
  }
  if (p === "raw_input.md" || p.endsWith("/raw_input.md")) {
    return {
      teamName: "Product & Design",
      phaseCode: "PHASE_0",
      phaseLabel: language === "en" ? "Phase 0: Raw User Input" : "Fase 0: Kebutuhan Awal User",
      badgeClass: "bg-blue-500/20 text-blue-300 border-blue-500/30",
    };
  }
  if (p === "prompts_and_inferences.md" || p.endsWith("/prompts_and_inferences.md")) {
    return {
      teamName: "Umum / Lintas Tim",
      phaseCode: "GOVERNANCE",
      phaseLabel: language === "en" ? "Governance: Inference Ledger" : "Tata Kelola: Audit Inference",
      badgeClass: "bg-slate-700/40 text-slate-300 border-slate-600/40",
    };
  }

  // Product & Design
  if (p.includes("gherkin_requirements") || p.includes("gherkin")) {
    return {
      teamName: "Product & Design",
      phaseCode: "PHASE_0",
      phaseLabel: language === "en" ? "Phase 0: Requirement Discovery (BDD)" : "Fase 0: Penemuan Kebutuhan (BDD)",
      badgeClass: "bg-blue-500/20 text-blue-300 border-blue-500/30",
    };
  }
  if (p.includes("technical_user_stories") || p.includes("user_stories")) {
    return {
      teamName: "Product & Design",
      phaseCode: "PHASE_2",
      phaseLabel: language === "en" ? "Phase 2: Backlog Generation" : "Fase 2: Pembuatan Backlog User Stories",
      badgeClass: "bg-blue-500/20 text-blue-300 border-blue-500/30",
    };
  }
  if (p.includes("database_schema.sql") || p.endsWith(".sql")) {
    return {
      teamName: "Product & Design",
      phaseCode: "PHASE_2",
      phaseLabel: language === "en" ? "Phase 2: Database Schema (DDL)" : "Fase 2: Skema Database (PostgreSQL 16)",
      badgeClass: "bg-cyan-500/20 text-cyan-300 border-cyan-500/30",
    };
  }

  // Shared Enabling
  if (p.includes("threat-model") || p.includes("threat_model")) {
    return {
      teamName: "Shared Enabling",
      phaseCode: "PHASE_0",
      phaseLabel: language === "en" ? "Phase 0: STRIDE Threat Model" : "Fase 0: Model Ancaman STRIDE",
      badgeClass: "bg-rose-500/20 text-rose-300 border-rose-500/30",
    };
  }
  if (p.includes("c4_diagrams") || p.includes("c4")) {
    return {
      teamName: "Shared Enabling",
      phaseCode: "PHASE_1",
      phaseLabel: language === "en" ? "Phase 1: C4 Architecture Model" : "Fase 1: Arsitektur Model C4",
      badgeClass: "bg-purple-500/20 text-purple-300 border-purple-500/30",
    };
  }
  if (p.includes("phase1-architecture-audit") || p.includes("architecture-audit")) {
    return {
      teamName: "Shared Enabling",
      phaseCode: "PHASE_1",
      phaseLabel: language === "en" ? "Phase 1: Architecture Review" : "Fase 1: Audit Arsitektur Bersama",
      badgeClass: "bg-purple-500/20 text-purple-300 border-purple-500/30",
    };
  }
  if (p.includes("phase8-production-readiness") || p.includes("production-readiness")) {
    return {
      teamName: "Shared Enabling",
      phaseCode: "PHASE_8",
      phaseLabel: language === "en" ? "Phase 8: Production Gate Audit" : "Fase 8: Gerbang Audit Kesiapan Produksi",
      badgeClass: "bg-amber-500/20 text-amber-300 border-amber-500/30",
    };
  }

  // CI/CD & Staging
  if (p.includes("dispatch_allocation") || p.includes("dispatch")) {
    return {
      teamName: "CI/CD & Staging",
      phaseCode: "PHASE_3",
      phaseLabel: language === "en" ? "Phase 3: Task Dispatching" : "Fase 3: Alokasi Task Otonom",
      badgeClass: "bg-violet-500/20 text-violet-300 border-violet-500/30",
    };
  }
  if (p.includes("phase7-qa-verification") || p.includes("qa-verification") || p.includes("qa-report")) {
    return {
      teamName: "CI/CD & Staging",
      phaseCode: "PHASE_7",
      phaseLabel: language === "en" ? "Phase 7: QA Verification & Fuzzing" : "Fase 7: Verifikasi QA & Fuzzing",
      badgeClass: "bg-emerald-500/20 text-emerald-300 border-emerald-500/30",
    };
  }

  // Production & SRE
  if (p.includes("docker-compose") || p.includes("prometheus") || p.includes("grafana")) {
    return {
      teamName: "Production & SRE",
      phaseCode: "PHASE_8",
      phaseLabel: language === "en" ? "Phase 8: Container & Telemetry" : "Fase 8: Manifest Kontainer & Telemetri",
      badgeClass: "bg-amber-500/20 text-amber-300 border-amber-500/30",
    };
  }

  // Development Team
  if (p.includes("tdd_plan") || p.includes("tests/") || p.includes("test/")) {
    return {
      teamName: "Development Team",
      phaseCode: "PHASE_4",
      phaseLabel: language === "en" ? "Phase 4: TDD Test Suites" : "Fase 4: Rencana & Test Suite TDD",
      badgeClass: "bg-emerald-500/20 text-emerald-300 border-emerald-500/30",
    };
  }
  if (p.includes("implementation_summary") || p.includes("src/backend") || p.includes("src/frontend") || p.includes("src/")) {
    return {
      teamName: "Development Team",
      phaseCode: "PHASE_5",
      phaseLabel: language === "en" ? "Phase 5: Implementation Code" : "Fase 5: Implementasi Kode Sumber",
      badgeClass: "bg-emerald-500/20 text-emerald-300 border-emerald-500/30",
    };
  }
  if (p.includes("phase6-triad-council") || p.includes("code-review") || p.includes("council-audit")) {
    return {
      teamName: "Development Team",
      phaseCode: "PHASE_6",
      phaseLabel: language === "en" ? "Phase 6: Triad Code Review" : "Fase 6: Review Kode Triad Council",
      badgeClass: "bg-teal-500/20 text-teal-300 border-teal-500/30",
    };
  }

  // Fallback
  return {
    teamName: "Umum / Lintas Tim",
    phaseCode: "GENERAL",
    phaseLabel: language === "en" ? "General Artifact" : "Berkas Pendukung",
    badgeClass: "bg-slate-700/30 text-slate-300 border-slate-600/30",
  };
}

export function TeamTopologyArtifactsExplorer({ workflow }: TeamTopologyArtifactsExplorerProps) {
  const { language, t } = useLanguage();
  const [artifacts, setArtifacts] = useState<ArtifactItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [selectedTeam, setSelectedTeam] = useState<string | null>(null);
  const [selectedDoc, setSelectedDoc] = useState<{ path: string; title: string } | null>(null);

  const loadArtifacts = async () => {
    if (!workflow?.id) return;
    try {
      setLoading(true);
      const items = await fetchArtifacts(workflow.id);
      setArtifacts(items);
    } catch (err) {
      console.error("Failed to load artifacts:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadArtifacts();
  }, [workflow?.id]);

  // Classify artifacts with Team & Phase metadata
  const classifiedArtifacts = useMemo(() => {
    return artifacts.map((item) => {
      const meta = classifyDocument(item.path, language);
      return {
        ...item,
        meta,
      };
    });
  }, [artifacts, language]);

  // Count artifacts per team
  const countsByTeam = useMemo(() => {
    const counts: Record<string, number> = {
      "Product & Design": 0,
      "Development Team": 0,
      "CI/CD & Staging": 0,
      "Production & SRE": 0,
      "Shared Enabling": 0,
      "Umum / Lintas Tim": 0,
    };
    classifiedArtifacts.forEach((a) => {
      if (counts[a.meta.teamName] !== undefined) {
        counts[a.meta.teamName]++;
      } else {
        counts["Umum / Lintas Tim"]++;
      }
    });
    return counts;
  }, [classifiedArtifacts]);

  // Filter artifacts by selected team and search term
  const filteredArtifacts = useMemo(() => {
    return classifiedArtifacts.filter((item) => {
      const matchesSearch =
        item.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.path.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.meta.phaseLabel.toLowerCase().includes(searchTerm.toLowerCase());

      if (!matchesSearch) return false;

      if (!selectedTeam) return true; // Show all teams
      return item.meta.teamName === selectedTeam;
    });
  }, [classifiedArtifacts, selectedTeam, searchTerm]);

  // Group by team when "Semua Tim" is active
  const groupedByTeam = useMemo(() => {
    const groups: Record<string, typeof filteredArtifacts> = {};
    filteredArtifacts.forEach((item) => {
      if (!groups[item.meta.teamName]) {
        groups[item.meta.teamName] = [];
      }
      groups[item.meta.teamName].push(item);
    });
    return groups;
  }, [filteredArtifacts]);

  const currentState = workflow?.currentState || "PHASE_0_REQUIREMENT_DISCOVERY";

  return (
    <div className="w-full rounded-2xl glass-panel p-4 sm:p-5 border border-slate-800 space-y-5 shadow-xl shadow-black/30">
      {/* 1. Header & Quick Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800/80">
        <div className="flex items-center gap-2.5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600/20 text-blue-400 border border-blue-500/30 flex-shrink-0">
            <FolderTree className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm sm:text-base font-bold text-white flex items-center gap-2">
                {language === "en" ? "5-Team Topology & Project Artifacts" : "5-Team Topology & Berkas Proyek"}
              </h3>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-blue-950/70 border border-blue-500/40 text-blue-300 font-semibold">
                {workflow.projectId || "PRJ-DEFAULT"}
              </span>
            </div>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {language === "en"
                ? "Click any team card below to filter its phase artifacts, or explore all documents grouped by team."
                : "Klik kartu tim di bawah untuk memfilter berkas fase kerjanya, atau jelajahi seluruh dokumen per tim."}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto">
          {/* Quick SDLC Journal Button */}
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
            className="touch-target inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-md shadow-indigo-600/30 transition"
          >
            <BookOpen className="h-4 w-4" />
            <span>{language === "en" ? "View SDLC Journal (SDLC_LOG)" : "Lihat Jurnal SDLC (SDLC_LOG)"}</span>
          </button>

          <button
            type="button"
            onClick={loadArtifacts}
            className="touch-target p-1.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition"
            title={t.artifacts.refresh}
          >
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          </button>
        </div>
      </div>

      {/* 2. Interactive 5-Team Topology Cards Grid */}
      <div>
        <div className="flex items-center justify-between mb-2.5">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <Layers className="h-3.5 w-3.5 text-blue-400" />
              {language === "en" ? "Select Team to Filter Artifacts:" : "Pilih Tim untuk Filter Berkas:"}
            </span>
            {selectedTeam && (
              <span className="text-[11px] px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-300 border border-blue-500/30 font-medium flex items-center gap-1">
                <span>{selectedTeam}</span>
                <button
                  type="button"
                  onClick={() => setSelectedTeam(null)}
                  className="hover:text-white text-blue-400 ml-0.5"
                  title="Hapus filter"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            )}
          </div>

          {/* All Teams Filter Reset Pill */}
          <button
            type="button"
            onClick={() => setSelectedTeam(null)}
            className={`text-xs px-2.5 py-1 rounded-lg border transition font-medium ${
              selectedTeam === null
                ? "bg-blue-600/30 border-blue-500 text-blue-200 shadow-sm"
                : "bg-slate-900/60 border-slate-800 text-slate-400 hover:text-white hover:border-slate-700"
            }`}
          >
            {language === "en" ? "All Teams" : "Semua Tim"} ({artifacts.length})
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-3">
          {TEAMS.map((team) => {
            const isSelected = selectedTeam === team.name;
            const isActive = team.activeInPhases.includes(currentState);
            const isDeadlock =
              currentState === "PHASE_6_DEADLOCK_WAITING_HUMAN" && team.name === "Development Team";

            const isWaitingSignOff =
              (currentState === "PHASE_0_WAITING_HUMAN_SIGN_OFF" && team.name === "Product & Design") ||
              (currentState === "PHASE_8_READY_FOR_PRODUCTION_GATE" && team.name === "Shared Enabling");

            let statusBadge = t.agentTeams.idle.toUpperCase();
            let badgeColor = "bg-slate-800 text-slate-400 border-slate-700";

            if (isDeadlock) {
              statusBadge = "DEADLOCK";
              badgeColor = "bg-rose-500/20 text-rose-400 border-rose-500/40 animate-pulse";
            } else if (isWaitingSignOff) {
              statusBadge = language === "en" ? "WAITING SIGN-OFF" : "MENUNGGU SIGN-OFF";
              badgeColor = "bg-amber-500/20 text-amber-400 border-amber-500/40 animate-pulse font-semibold";
            } else if (isActive) {
              statusBadge = t.agentTeams.running.toUpperCase();
              badgeColor = "bg-emerald-500/20 text-emerald-400 border-emerald-500/40 animate-pulse";
            }

            const teamFileCount = countsByTeam[team.name] || 0;

            return (
              <div
                key={team.id}
                onClick={() => setSelectedTeam(isSelected ? null : team.name)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    setSelectedTeam(isSelected ? null : team.name);
                  }
                }}
                className={`text-left rounded-xl p-3 sm:p-3.5 border transition-all cursor-pointer relative group ${
                  isSelected
                    ? `${team.borderActiveColor} ${team.bgActiveColor} shadow-lg shadow-black/40`
                    : "bg-slate-950/60 border-slate-800/90 hover:border-slate-700 hover:bg-slate-900/40"
                }`}
              >
                {/* Active Indicator Pin */}
                {isSelected && (
                  <div className="absolute -top-1.5 -right-1.5 bg-blue-500 text-white rounded-full p-0.5 shadow-md">
                    <CheckCircle2 className="h-3 w-3" />
                  </div>
                )}

                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <div className="p-1.5 rounded-lg bg-slate-900 border border-slate-800/80">
                      {team.icon}
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-white leading-tight">{team.name}</h4>
                      <span className="text-[10px] text-slate-400">{team.category}</span>
                    </div>
                  </div>
                  <span
                    className={`text-[9px] font-mono px-1.5 py-0.5 rounded border font-semibold ${badgeColor}`}
                  >
                    {statusBadge}
                  </span>
                </div>

                <div className="mt-2 pt-2 border-t border-slate-900 space-y-1.5">
                  <p className="text-[11px] font-medium text-slate-300 truncate">{team.leadRole}</p>
                  <div className="flex flex-wrap gap-1">
                    {team.members.map((member, i) => (
                      <span
                        key={i}
                        className="text-[9px] px-1.5 py-0.5 rounded bg-slate-900 border border-slate-800/80 text-slate-400 truncate max-w-[120px]"
                      >
                        {member}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Team File Count Footer */}
                <div className="mt-2.5 pt-1.5 border-t border-slate-900/80 flex items-center justify-between text-[10px]">
                  <span className="text-slate-400 flex items-center gap-1">
                    📁 {teamFileCount} {language === "en" ? "artifacts" : "berkas"}
                  </span>
                  <span
                    className={`font-semibold ${
                      isSelected ? "text-blue-300 font-bold" : "text-slate-500 group-hover:text-slate-300"
                    }`}
                  >
                    {isSelected ? (language === "en" ? "Active" : "Aktif") : (language === "en" ? "Filter" : "Pilih")} →
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 3. Berkas Workspace List with Search & Phase Labels */}
      <div className="space-y-3 pt-2 border-t border-slate-800/70">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-slate-200">
              {selectedTeam
                ? `${language === "en" ? "Artifacts for" : "Berkas Tim"}: ${selectedTeam}`
                : language === "en"
                ? "All Workspace Artifacts (Grouped by Team)"
                : "Semua Berkas Workspace (Dikelompokkan per Tim)"}
            </span>
            <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-400">
              {filteredArtifacts.length} {language === "en" ? "files" : "berkas"}
            </span>
          </div>

          {/* Search Box */}
          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-slate-500" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder={
                selectedTeam
                  ? `${language === "en" ? "Search in" : "Cari di"} ${selectedTeam}...`
                  : t.artifacts.searchPlaceholder
              }
              className="w-full pl-8 pr-4 py-1.5 rounded-xl bg-slate-950/80 border border-slate-800 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500"
            />
            {searchTerm && (
              <button
                type="button"
                onClick={() => setSearchTerm("")}
                className="absolute right-2.5 top-2.5 text-slate-500 hover:text-white"
              >
                <X className="h-3 w-3" />
              </button>
            )}
          </div>
        </div>

        {/* Content Section */}
        {loading ? (
          <div className="py-12 text-center text-xs text-slate-500 flex flex-col items-center gap-2">
            <RefreshCw className="h-5 w-5 animate-spin text-blue-500" />
            <span>{t.artifacts.loading}</span>
          </div>
        ) : filteredArtifacts.length === 0 ? (
          <div className="py-12 text-center text-xs text-slate-500 rounded-xl bg-slate-950/40 border border-slate-900 space-y-2">
            <p>{t.artifacts.empty}</p>
            {selectedTeam && (
              <button
                type="button"
                onClick={() => setSelectedTeam(null)}
                className="text-blue-400 hover:underline text-xs"
              >
                {language === "en" ? "Clear team filter to view all" : "Reset filter tim untuk melihat semua"}
              </button>
            )}
          </div>
        ) : selectedTeam ? (
          /* Render Flat Grid When A Specific Team Is Selected */
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5 max-h-[420px] overflow-y-auto pr-1">
            {filteredArtifacts.map((item) => (
              <ArtifactCard
                key={item.path}
                item={item}
                onSelect={(doc) => setSelectedDoc(doc)}
                language={language}
              />
            ))}
          </div>
        ) : (
          /* Render Grouped by Team When "Semua Tim" Is Active */
          <div className="space-y-4 max-h-[500px] overflow-y-auto pr-1">
            {Object.entries(groupedByTeam).map(([teamName, items]) => {
              if (items.length === 0) return null;

              return (
                <div key={teamName} className="space-y-2">
                  <div className="flex items-center gap-2 pb-1 border-b border-slate-800/60">
                    <span className="text-xs font-bold text-slate-200 flex items-center gap-1.5">
                      {teamName === "Product & Design" && <Users className="h-3.5 w-3.5 text-blue-400" />}
                      {teamName === "Development Team" && <Code className="h-3.5 w-3.5 text-emerald-400" />}
                      {teamName === "CI/CD & Staging" && <Terminal className="h-3.5 w-3.5 text-indigo-400" />}
                      {teamName === "Production & SRE" && <Activity className="h-3.5 w-3.5 text-amber-400" />}
                      {teamName === "Shared Enabling" && <ShieldAlert className="h-3.5 w-3.5 text-rose-400" />}
                      {teamName === "Umum / Lintas Tim" && <FolderTree className="h-3.5 w-3.5 text-indigo-400" />}
                      <span>{teamName}</span>
                    </span>
                    <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-900 border border-slate-800 text-slate-400 font-semibold">
                      {items.length} {language === "en" ? "files" : "berkas"}
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5">
                    {items.map((item) => (
                      <ArtifactCard
                        key={item.path}
                        item={item}
                        onSelect={(doc) => setSelectedDoc(doc)}
                        language={language}
                      />
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* 4. Document Viewer Modal */}
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

interface ArtifactCardProps {
  item: ArtifactItem & { meta: DocumentClassification };
  onSelect: (doc: { path: string; title: string }) => void;
  language: "en" | "id";
}

function ArtifactCard({ item, onSelect, language }: ArtifactCardProps) {
  const isSdlcLog = item.name === "SDLC_LOG.md";
  const isThreatModel = item.name.includes("threat-model");
  const isGherkin = item.name.includes("GHERKIN");
  const isSql = item.name.endsWith(".sql");
  const isDocker = item.name.includes("docker");

  return (
    <div
      className={`flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded-xl border transition-all gap-2.5 ${
        isSdlcLog
          ? "bg-indigo-950/30 border-indigo-500/40 hover:border-indigo-500/60"
          : "bg-slate-950/60 border-slate-800/90 hover:border-slate-700 hover:bg-slate-900/40"
      }`}
    >
      <div className="flex items-start gap-2.5 min-w-0 pr-1">
        {/* Document Icon */}
        <div
          className={`flex h-8 w-8 items-center justify-center rounded-lg flex-shrink-0 mt-0.5 ${
            isSdlcLog
              ? "bg-indigo-500/20 text-indigo-400"
              : isThreatModel
              ? "bg-rose-500/20 text-rose-400"
              : isGherkin
              ? "bg-blue-500/20 text-blue-400"
              : isSql
              ? "bg-cyan-500/20 text-cyan-400"
              : isDocker
              ? "bg-amber-500/20 text-amber-400"
              : "bg-slate-800 text-slate-300"
          }`}
        >
          {isSdlcLog ? (
            <BookOpen className="h-4 w-4" />
          ) : isThreatModel ? (
            <ShieldCheck className="h-4 w-4" />
          ) : isSql ? (
            <Database className="h-4 w-4" />
          ) : isDocker ? (
            <Server className="h-4 w-4" />
          ) : (
            <FileText className="h-4 w-4" />
          )}
        </div>

        <div className="min-w-0">
          <div className="flex items-center gap-1.5 flex-wrap">
            <p className="text-xs font-semibold text-slate-100 truncate">{item.name}</p>
            {/* Phase Badge */}
            <span
              className={`text-[9px] font-medium px-1.5 py-0.5 rounded border ${item.meta.badgeClass}`}
            >
              {item.meta.phaseLabel}
            </span>
          </div>
          <p className="text-[10px] font-mono text-slate-500 truncate mt-0.5">{item.path}</p>
        </div>
      </div>

      {/* Open Button */}
      <button
        type="button"
        onClick={() =>
          onSelect({
            path: item.path,
            title: isSdlcLog
              ? "📘 SDLC Engineering Journal & Rationale (Single Source of Truth)"
              : `📄 ${item.name}`,
          })
        }
        className="touch-target inline-flex items-center justify-center gap-1 px-2.5 py-1.5 rounded-lg bg-blue-600/20 hover:bg-blue-600/40 text-blue-300 border border-blue-500/30 text-[11px] font-medium transition flex-shrink-0 self-end sm:self-auto"
      >
        <ExternalLink className="h-3 w-3" />
        <span>{language === "en" ? "Open / View" : "Buka / Unduh"}</span>
      </button>
    </div>
  );
}
