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
  Folder,
  FolderOpen,
  ChevronRight,
  ChevronDown,
  LayoutGrid,
  ListTree,
  File,
  Maximize2,
  Minimize2,
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
  sizeBytes?: string;
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

  // General / Cross-team
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
  if (p.includes("c4_model") || p.includes("c4")) {
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
  if (p.includes("phase8-production-readiness") || p.includes("production-readiness") || p.includes("production_readiness")) {
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
  if (p.includes("phase7-qa-verification") || p.includes("qa_sandbox") || p.includes("qa-report") || p.includes("qa-verification")) {
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

  return {
    teamName: "Umum / Lintas Tim",
    phaseCode: "GENERAL",
    phaseLabel: language === "en" ? "General Artifact" : "Berkas Pendukung",
    badgeClass: "bg-slate-700/30 text-slate-300 border-slate-600/30",
  };
}

function getFileIcon(path: string) {
  const p = path.toLowerCase();
  if (p.endsWith(".md")) return <FileText className="h-4 w-4 text-blue-400 flex-shrink-0" />;
  if (p.endsWith(".java")) return <FileCode className="h-4 w-4 text-orange-400 flex-shrink-0" />;
  if (p.endsWith(".tsx") || p.endsWith(".ts")) return <Code className="h-4 w-4 text-sky-400 flex-shrink-0" />;
  if (p.endsWith(".sql")) return <Database className="h-4 w-4 text-cyan-400 flex-shrink-0" />;
  if (p.endsWith(".json")) return <FileCode className="h-4 w-4 text-emerald-400 flex-shrink-0" />;
  if (p.endsWith(".yml") || p.endsWith(".yaml")) return <Server className="h-4 w-4 text-rose-400 flex-shrink-0" />;
  if (p.endsWith(".xml")) return <FileText className="h-4 w-4 text-amber-400 flex-shrink-0" />;
  if (p.includes("dockerfile")) return <Terminal className="h-4 w-4 text-indigo-400 flex-shrink-0" />;
  return <File className="h-4 w-4 text-slate-400 flex-shrink-0" />;
}

function getFolderIcon(folderName: string, isOpen: boolean) {
  const lower = folderName.toLowerCase();
  if (lower.startsWith("src") || lower.includes("/src")) {
    return <Code className="h-4 w-4 text-cyan-400 flex-shrink-0" />;
  }
  if (lower.startsWith("test") || lower.includes("/test")) {
    return <CheckCircle2 className="h-4 w-4 text-emerald-400 flex-shrink-0" />;
  }
  if (lower.includes("council") || lower.includes("audit")) {
    return <ShieldCheck className="h-4 w-4 text-rose-400 flex-shrink-0" />;
  }
  if (lower.includes("arch") || lower.includes("c4")) {
    return <Layers className="h-4 w-4 text-purple-400 flex-shrink-0" />;
  }
  if (lower.includes("backlog") || lower.includes("task") || lower.includes("story")) {
    return <BookOpen className="h-4 w-4 text-blue-400 flex-shrink-0" />;
  }
  if (lower.includes("database") || lower.includes("sql") || lower.includes("db")) {
    return <Database className="h-4 w-4 text-teal-400 flex-shrink-0" />;
  }
  if (lower.includes("inference") || lower.includes("prompt")) {
    return <Sparkles className="h-4 w-4 text-amber-400 flex-shrink-0" />;
  }
  return isOpen ? (
    <FolderOpen className="h-4 w-4 text-amber-400 flex-shrink-0" />
  ) : (
    <Folder className="h-4 w-4 text-amber-400 flex-shrink-0" />
  );
}

function formatBytes(bytes?: string | number): string {
  if (!bytes) return "";
  const b = typeof bytes === "string" ? parseInt(bytes, 10) : bytes;
  if (isNaN(b) || b <= 0) return "";
  if (b < 1024) return `${b} B`;
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)} KB`;
  return `${(b / (1024 * 1024)).toFixed(1)} MB`;
}

interface ClassifiedArtifactItem extends ArtifactItem {
  meta: DocumentClassification;
}

interface FileTreeNode {
  id: string;
  name: string;
  path: string;
  isFolder: boolean;
  children: FileTreeNode[];
  artifact?: ClassifiedArtifactItem;
  fileCount: number;
}

function buildFileTree(items: ClassifiedArtifactItem[]): FileTreeNode[] {
  interface RawFolderNode {
    id: string;
    segment: string;
    children: Record<string, RawFolderNode>;
    files: ClassifiedArtifactItem[];
  }

  const root: RawFolderNode = {
    id: "",
    segment: "",
    children: {},
    files: [],
  };

  items.forEach((item) => {
    const normalized = item.path.replace(/\\/g, "/");
    const parts = normalized.split("/").filter(Boolean);
    if (parts.length === 0) return;

    let current = root;
    for (let i = 0; i < parts.length - 1; i++) {
      const seg = parts[i];
      const curPath = parts.slice(0, i + 1).join("/");
      if (!current.children[seg]) {
        current.children[seg] = {
          id: curPath,
          segment: seg,
          children: {},
          files: [],
        };
      }
      current = current.children[seg];
    }
    current.files.push(item);
  });

  function convert(folder: RawFolderNode): FileTreeNode[] {
    const result: FileTreeNode[] = [];

    // Process subfolders
    Object.keys(folder.children).sort().forEach((childKey) => {
      let child = folder.children[childKey];
      let compactedSegment = child.segment;
      let compactedId = child.id;

      // Compact folder chaining (e.g. main/java/com/agentic)
      while (child.files.length === 0 && Object.keys(child.children).length === 1) {
        const onlyChildKey = Object.keys(child.children)[0];
        const nextChild = child.children[onlyChildKey];
        compactedSegment = `${compactedSegment}/${nextChild.segment}`;
        compactedId = nextChild.id;
        child = nextChild;
      }

      const subChildren = convert(child);
      const fileCount = subChildren.reduce((acc, c) => acc + c.fileCount, 0);

      result.push({
        id: compactedId,
        name: compactedSegment,
        path: compactedId,
        isFolder: true,
        children: subChildren,
        fileCount,
      });
    });

    // Process files
    folder.files.sort((a, b) => a.name.localeCompare(b.name)).forEach((file) => {
      result.push({
        id: file.path,
        name: file.name,
        path: file.path,
        isFolder: false,
        children: [],
        artifact: file,
        fileCount: 1,
      });
    });

    return result.sort((a, b) => {
      if (a.isFolder && !b.isFolder) return -1;
      if (!a.isFolder && b.isFolder) return 1;
      return a.name.localeCompare(b.name);
    });
  }

  return convert(root);
}

function collectAllFolderIds(nodes: FileTreeNode[]): string[] {
  const ids: string[] = [];
  function traverse(n: FileTreeNode) {
    if (n.isFolder) {
      ids.push(n.id);
      n.children.forEach(traverse);
    }
  }
  nodes.forEach(traverse);
  return ids;
}

function TreeNodeItem({
  node,
  level,
  openFolders,
  toggleFolder,
  onSelectFile,
  searchTerm,
}: {
  node: FileTreeNode;
  level: number;
  openFolders: Record<string, boolean>;
  toggleFolder: (id: string) => void;
  onSelectFile: (path: string, name: string) => void;
  searchTerm: string;
}) {
  if (node.isFolder) {
    const isOpen = openFolders[node.id] !== false; // default open
    return (
      <div className="select-none">
        <button
          type="button"
          onClick={() => toggleFolder(node.id)}
          style={{ paddingLeft: `${Math.max(level * 16, 6)}px` }}
          className="w-full flex items-center justify-between py-1.5 pr-2.5 rounded-lg hover:bg-slate-800/60 transition group text-left cursor-pointer"
        >
          <div className="flex items-center gap-1.5 min-w-0">
            <span className="p-0.5 text-slate-500 group-hover:text-slate-300">
              {isOpen ? (
                <ChevronDown className="h-3.5 w-3.5" />
              ) : (
                <ChevronRight className="h-3.5 w-3.5" />
              )}
            </span>
            {getFolderIcon(node.name, isOpen)}
            <span className="text-xs font-bold text-slate-200 group-hover:text-white truncate font-mono">
              {node.name}/
            </span>
          </div>
          <span className="text-[10px] font-mono text-slate-500 bg-slate-900/80 px-2 py-0.5 rounded border border-slate-800/80 flex-shrink-0 ml-2">
            {node.fileCount} {node.fileCount === 1 ? "file" : "files"}
          </span>
        </button>

        {isOpen && node.children.length > 0 && (
          <div className="border-l border-slate-800/80 ml-3.5 space-y-0.5 mt-0.5">
            {node.children.map((child) => (
              <TreeNodeItem
                key={child.id}
                node={child}
                level={level + 1}
                openFolders={openFolders}
                toggleFolder={toggleFolder}
                onSelectFile={onSelectFile}
                searchTerm={searchTerm}
              />
            ))}
          </div>
        )}
      </div>
    );
  }

  // File leaf item
  const item = node.artifact;
  const isMatch = searchTerm && node.name.toLowerCase().includes(searchTerm.toLowerCase());

  return (
    <div
      style={{ paddingLeft: `${Math.max(level * 16, 6)}px` }}
      onClick={() => onSelectFile(node.path, node.name)}
      className={`flex items-center justify-between py-1.5 pr-2.5 rounded-lg transition group cursor-pointer ${
        isMatch
          ? "bg-blue-950/40 border border-blue-500/40"
          : "hover:bg-slate-800/50 hover:border-slate-700/50 border border-transparent"
      }`}
    >
      <div className="flex items-center gap-2 min-w-0 flex-1">
        <span className="w-3.5 flex-shrink-0" />
        {getFileIcon(node.path)}
        <span className="text-xs font-medium text-slate-300 group-hover:text-blue-400 transition truncate font-mono">
          {node.name}
        </span>
      </div>

      <div className="flex items-center gap-2 flex-shrink-0 ml-2">
        {item?.sizeBytes && (
          <span className="text-[10px] font-mono text-slate-500">
            {formatBytes(item.sizeBytes)}
          </span>
        )}
        {item?.meta?.badgeClass && (
          <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded border ${item.meta.badgeClass}`}>
            {item.meta.phaseCode}
          </span>
        )}
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onSelectFile(node.path, node.name);
          }}
          className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-slate-700 text-slate-400 hover:text-white transition"
          title="Open Document"
        >
          <ExternalLink className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
}

export function TeamTopologyArtifactsExplorer({ workflow }: TeamTopologyArtifactsExplorerProps) {
  const { language, t } = useLanguage();
  const [artifacts, setArtifacts] = useState<ArtifactItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [viewMode, setViewMode] = useState<"tree" | "topology">("tree");
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [selectedTeam, setSelectedTeam] = useState<string | null>(null);
  const [selectedDoc, setSelectedDoc] = useState<{ path: string; title: string } | null>(null);
  const [openFolders, setOpenFolders] = useState<Record<string, boolean>>({});

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

  const toggleFolder = (folderKey: string) => {
    setOpenFolders((prev) => ({
      ...prev,
      [folderKey]: prev[folderKey] === false ? true : false,
    }));
  };

  // Classify artifacts
  const classifiedArtifacts = useMemo<ClassifiedArtifactItem[]>(() => {
    return artifacts.map((item) => {
      const meta = classifyDocument(item.path, language);
      return {
        ...item,
        meta,
      };
    });
  }, [artifacts, language]);

  // Counts per team
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

  // Filter artifacts
  const filteredArtifacts = useMemo(() => {
    return classifiedArtifacts.filter((item) => {
      const matchesSearch =
        item.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.path.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.meta.phaseLabel.toLowerCase().includes(searchTerm.toLowerCase());

      if (!matchesSearch) return false;

      if (!selectedTeam) return true;
      return item.meta.teamName === selectedTeam;
    });
  }, [classifiedArtifacts, selectedTeam, searchTerm]);

  // Build recursive tree
  const fileTree = useMemo(() => {
    return buildFileTree(filteredArtifacts);
  }, [filteredArtifacts]);

  // Expand all folders containing matches when search term is active
  useEffect(() => {
    if (searchTerm.trim()) {
      const allIds = collectAllFolderIds(fileTree);
      const updates: Record<string, boolean> = {};
      allIds.forEach((id) => (updates[id] = true));
      setOpenFolders((prev) => ({ ...prev, ...updates }));
    }
  }, [searchTerm, fileTree]);

  const handleExpandAll = () => {
    const allIds = collectAllFolderIds(fileTree);
    const updates: Record<string, boolean> = {};
    allIds.forEach((id) => (updates[id] = true));
    setOpenFolders(updates);
  };

  const handleCollapseAll = () => {
    const allIds = collectAllFolderIds(fileTree);
    const updates: Record<string, boolean> = {};
    allIds.forEach((id) => (updates[id] = false));
    setOpenFolders(updates);
  };

  // Group by team for topology view
  const groupedByTeam = useMemo(() => {
    const groups: Record<string, ClassifiedArtifactItem[]> = {};
    filteredArtifacts.forEach((item) => {
      if (!groups[item.meta.teamName]) {
        groups[item.meta.teamName] = [];
      }
      groups[item.meta.teamName].push(item);
    });
    return groups;
  }, [filteredArtifacts]);

  return (
    <div className="w-full rounded-2xl glass-panel p-4 sm:p-5 border border-slate-800 space-y-5 shadow-xl shadow-black/30">
      {/* 1. Header & Navigation Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800/80">
        <div className="flex items-center gap-2.5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600/20 text-blue-400 border border-blue-500/30 flex-shrink-0">
            <FolderTree className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm sm:text-base font-bold text-white flex items-center gap-2">
                {language === "en" ? "Project Artifacts & Code Explorer" : "Eksplorasi Berkas & Kode Proyek"}
              </h3>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-blue-950/70 border border-blue-500/40 text-blue-300 font-semibold">
                {workflow.projectId || "PRJ-DEFAULT"}
              </span>
            </div>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {language === "en"
                ? "Browse SDLC specifications, C4 models, test plans, backend Java, frontend React, and manifests."
                : "Jelajahi spesifikasi SDLC, model C4, rencana uji, Java backend, React frontend, dan manifest."}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto flex-wrap">
          {/* View Mode Switcher */}
          <div className="flex items-center p-1 bg-slate-900 border border-slate-800 rounded-xl">
            <button
              type="button"
              onClick={() => setViewMode("tree")}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                viewMode === "tree"
                  ? "bg-blue-600 text-white shadow-md shadow-blue-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <ListTree className="h-3.5 w-3.5" />
              <span>{language === "en" ? "Workspace Tree" : "Pohon Berkas"}</span>
              <span className={`text-[10px] font-mono px-1.5 py-0.2 rounded font-bold ${viewMode === "tree" ? "bg-blue-700 text-blue-100" : "bg-slate-800 text-slate-400"}`}>
                {filteredArtifacts.length}
              </span>
            </button>
            <button
              type="button"
              onClick={() => setViewMode("topology")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                viewMode === "topology"
                  ? "bg-blue-600 text-white shadow-md shadow-blue-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <LayoutGrid className="h-3.5 w-3.5" />
              <span>{language === "en" ? "Team Topology" : "Topologi Tim"}</span>
            </button>
          </div>

          {/* Quick SDLC Journal Button */}
          <button
            type="button"
            onClick={() =>
              setSelectedDoc({
                path: "SDLC_LOG.md",
                title: "📘 SDLC Engineering Journal & Rationale",
              })
            }
            className="touch-target inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-md shadow-indigo-600/30 transition"
          >
            <BookOpen className="h-4 w-4" />
            <span className="hidden sm:inline">{language === "en" ? "SDLC Journal" : "Jurnal SDLC"}</span>
          </button>

          {/* Refresh Button */}
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

      {/* 2. Search & Filter Bar */}
      <div className="flex items-center justify-between gap-3">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder={
              language === "en"
                ? "Search files by name, path or keyword (e.g. .java, .sql, .md)..."
                : "Cari berkas berdasarkan nama, path atau ekstensi..."
            }
            className="w-full pl-9 pr-8 py-1.5 bg-slate-900 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm("")}
              className="absolute right-2.5 top-2.5 text-slate-500 hover:text-white"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>

        <div className="text-xs text-slate-400 font-mono">
          {filteredArtifacts.length} {language === "en" ? "files in workspace" : "berkas di workspace"}
        </div>
      </div>

      {/* 3A. View Mode: WORKSPACE TREE (True Hierarchical IDE View) */}
      {viewMode === "tree" && (
        <div className="rounded-xl border border-slate-800 bg-[#090d16] font-mono text-xs overflow-hidden shadow-inner">
          {/* Tree Navigation Bar */}
          <div className="px-4 py-2.5 bg-slate-900/90 border-b border-slate-800 flex items-center justify-between flex-wrap gap-2">
            <span className="font-semibold text-slate-300 flex items-center gap-2">
              <Folder className="h-4 w-4 text-blue-400 flex-shrink-0" />
              <span>workspace/projects/{workflow.projectId || "PRJ-DEFAULT"}/</span>
            </span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleExpandAll}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[11px] font-medium border border-slate-700 transition"
                title="Expand All Folders"
              >
                <Maximize2 className="h-3 w-3 text-blue-400" />
                <span>{language === "en" ? "Expand All" : "Buka Semua"}</span>
              </button>
              <button
                type="button"
                onClick={handleCollapseAll}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[11px] font-medium border border-slate-700 transition"
                title="Collapse All Folders"
              >
                <Minimize2 className="h-3 w-3 text-slate-400" />
                <span>{language === "en" ? "Collapse All" : "Tutup Semua"}</span>
              </button>
              <span className="text-[11px] text-slate-500 bg-slate-950 px-2 py-0.5 rounded border border-slate-800 font-mono">
                {filteredArtifacts.length} total files
              </span>
            </div>
          </div>

          {/* Tree Contents */}
          <div className="p-3 space-y-0.5">
            {fileTree.length === 0 ? (
              <div className="py-12 text-center text-slate-500">
                <Folder className="h-8 w-8 mx-auto mb-2 text-slate-600" />
                <p className="text-xs">{t.artifacts.empty}</p>
              </div>
            ) : (
              fileTree.map((node) => (
                <TreeNodeItem
                  key={node.id}
                  node={node}
                  level={0}
                  openFolders={openFolders}
                  toggleFolder={toggleFolder}
                  onSelectFile={(path, name) => setSelectedDoc({ path, title: name })}
                  searchTerm={searchTerm}
                />
              ))
            )}
          </div>
        </div>
      )}

      {/* 3B. View Mode: TEAM TOPOLOGY (Grouped by Folder per Team) */}
      {viewMode === "topology" && (
        <div className="space-y-5">
          {/* Team Cards Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2.5">
            {TEAMS.map((team) => {
              const isSelected = selectedTeam === team.name;
              const count = countsByTeam[team.name] || 0;

              return (
                <button
                  key={team.id}
                  type="button"
                  onClick={() => setSelectedTeam(isSelected ? null : team.name)}
                  className={`text-left p-3 rounded-xl border transition-all ${
                    isSelected
                      ? `${team.borderActiveColor} ${team.bgActiveColor} shadow-lg`
                      : "border-slate-800 bg-slate-900/50 hover:border-slate-700 hover:bg-slate-900"
                  }`}
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="p-1.5 rounded-lg bg-slate-800/80">{team.icon}</div>
                    <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 font-bold">
                      {count}
                    </span>
                  </div>
                  <h4 className="text-xs font-bold text-white truncate">{team.name}</h4>
                  <p className="text-[10px] text-slate-400 truncate mt-0.5">{team.leadRole}</p>
                </button>
              );
            })}
          </div>

          {/* Grouped Artifacts with Directory Subheaders */}
          <div className="space-y-4">
            {Object.entries(groupedByTeam).map(([teamName, items]) => {
              // Group items by directory within team
              const itemsByDir: Record<string, ClassifiedArtifactItem[]> = {};
              items.forEach((item) => {
                const parts = item.path.replace(/\\/g, "/").split("/");
                const dir = parts.length > 1 ? parts.slice(0, parts.length - 1).join("/") : "root";
                if (!itemsByDir[dir]) itemsByDir[dir] = [];
                itemsByDir[dir].push(item);
              });

              return (
                <div key={teamName} className="rounded-xl border border-slate-800/70 bg-slate-900/30 overflow-hidden">
                  <div className="px-3.5 py-2 bg-slate-900/80 border-b border-slate-800 flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-200 flex items-center gap-2">
                      <span className="h-2 w-2 rounded-full bg-blue-500" />
                      {teamName}
                    </span>
                    <span className="text-[11px] font-mono text-slate-500">{items.length} files</span>
                  </div>

                  <div className="divide-y divide-slate-800/40">
                    {Object.entries(itemsByDir).map(([dirPath, dirItems]) => (
                      <div key={dirPath} className="space-y-0.5">
                        <div className="px-3.5 py-1.5 bg-slate-950/60 flex items-center gap-2 text-[11px] font-mono text-slate-400 border-b border-slate-800/40">
                          <Folder className="h-3.5 w-3.5 text-amber-400/80" />
                          <span className="font-semibold text-slate-300">{dirPath}/</span>
                          <span className="text-[10px] text-slate-500">({dirItems.length})</span>
                        </div>

                        {dirItems.map((item) => (
                          <div
                            key={item.path}
                            className="px-3.5 py-2 hover:bg-slate-800/40 transition flex items-center justify-between gap-3 group"
                          >
                            <button
                              type="button"
                              onClick={() => setSelectedDoc({ path: item.path, title: item.name })}
                              className="flex items-center gap-2.5 min-w-0 text-left flex-1"
                            >
                              {getFileIcon(item.path)}
                              <div className="min-w-0">
                                <p className="text-xs font-semibold text-slate-200 group-hover:text-blue-400 transition truncate">
                                  {item.name}
                                </p>
                                <p className="text-[10px] font-mono text-slate-500 truncate">{item.path}</p>
                              </div>
                            </button>

                            <div className="flex items-center gap-2 flex-shrink-0">
                              {item.sizeBytes && (
                                <span className="text-[10px] font-mono text-slate-500">
                                  {formatBytes(item.sizeBytes)}
                                </span>
                              )}
                              <span className={`text-[10px] font-mono px-2 py-0.5 rounded border ${item.meta.badgeClass}`}>
                                {item.meta.phaseCode}
                              </span>
                              <button
                                type="button"
                                onClick={() => setSelectedDoc({ path: item.path, title: item.name })}
                                className="p-1 rounded text-slate-400 hover:text-white hover:bg-slate-800 transition"
                                title="Open file viewer"
                              >
                                <ExternalLink className="h-3.5 w-3.5" />
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Document & Code Viewer Modal */}
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
