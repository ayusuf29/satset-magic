"use client";

import React, { useState, useEffect } from "react";
import { WorkflowSummary } from "@/lib/types";
import { fetchArtifacts } from "@/lib/api";
import {
  FileText,
  BookOpen,
  Download,
  ExternalLink,
  FolderTree,
  Search,
  RefreshCw,
  ShieldCheck,
  CheckCircle2,
  FileCode,
} from "lucide-react";
import { DocumentViewerModal } from "./DocumentViewerModal";
import { useLanguage } from "@/lib/i18n";

interface ProjectArtifactsExplorerProps {
  workflow: WorkflowSummary;
}

interface ArtifactItem {
  name: string;
  path: string;
  type: string;
}

export function ProjectArtifactsExplorer({ workflow }: ProjectArtifactsExplorerProps) {
  const { language, t } = useLanguage();
  const [artifacts, setArtifacts] = useState<ArtifactItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [searchTerm, setSearchTerm] = useState<string>("");
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

  const filtered = artifacts.filter(
    (a) =>
      a.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      a.path.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="w-full rounded-2xl glass-panel p-4 sm:p-5 border border-slate-800 space-y-4">
      {/* Header & Quick Jurnal SDLC Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800/80">
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600/20 text-blue-400 border border-blue-500/30">
            <FolderTree className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm sm:text-base font-bold text-white flex items-center gap-2">
              {language === "en" ? "Project Workspace Artifacts" : "Berkas Workspace Proyek"} ({workflow.projectId || "PRJ-DEFAULT"})
            </h3>
            <p className="text-[11px] text-slate-400">
              {t.artifacts.subtitle}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Quick Button for SDLC_LOG.md */}
          <button
            onClick={() =>
              setSelectedDoc({
                path: "SDLC_LOG.md",
                title: language === "en" ? "📘 SDLC Engineering Journal & Rationale (Single Source of Truth)" : "📘 SDLC Engineering Journal & Rationale (Single Source of Truth)",
              })
            }
            className="touch-target inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-md shadow-indigo-600/30 transition"
          >
            <BookOpen className="h-4 w-4" />
            <span>{language === "en" ? "View SDLC Journal (Why & Skills)" : "Lihat Jurnal SDLC (Why & Skill Matrix)"}</span>
          </button>

          <button
            onClick={loadArtifacts}
            className="touch-target p-1.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition"
            title={t.artifacts.refresh}
          >
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          </button>
        </div>
      </div>

      {/* Search Input */}
      <div className="relative">
        <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder={t.artifacts.searchPlaceholder}
          className="w-full pl-9 pr-4 py-2 rounded-xl bg-slate-950/80 border border-slate-800 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500"
        />
      </div>

      {/* Artifacts List Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5 max-h-[380px] overflow-y-auto pr-1">
        {loading ? (
          <div className="col-span-full py-8 text-center text-xs text-slate-500">
            {t.artifacts.loading}
          </div>
        ) : filtered.length === 0 ? (
          <div className="col-span-full py-8 text-center text-xs text-slate-500">
            {t.artifacts.empty}
          </div>
        ) : (
          filtered.map((item) => {
            const isSdlcLog = item.name === "SDLC_LOG.md";
            const isThreatModel = item.name.includes("threat-model");
            const isGherkin = item.name.includes("GHERKIN");

            return (
              <div
                key={item.path}
                className={`flex items-center justify-between p-2.5 rounded-xl border transition-all ${
                  isSdlcLog
                    ? "bg-indigo-950/30 border-indigo-500/40 hover:border-indigo-500/60"
                    : "bg-slate-950/60 border-slate-800 hover:border-slate-700"
                }`}
              >
                <div className="flex items-center gap-2 min-w-0 pr-2">
                  <div
                    className={`flex h-8 w-8 items-center justify-center rounded-lg flex-shrink-0 ${
                      isSdlcLog
                        ? "bg-indigo-500/20 text-indigo-400"
                        : isThreatModel
                        ? "bg-rose-500/20 text-rose-400"
                        : isGherkin
                        ? "bg-emerald-500/20 text-emerald-400"
                        : "bg-slate-800 text-slate-300"
                    }`}
                  >
                    {isSdlcLog ? (
                      <BookOpen className="h-4 w-4" />
                    ) : isThreatModel ? (
                      <ShieldCheck className="h-4 w-4" />
                    ) : (
                      <FileText className="h-4 w-4" />
                    )}
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-semibold text-slate-200 truncate">
                      {item.name}
                    </p>
                    <p className="text-[10px] font-mono text-slate-500 truncate">
                      {item.path}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() =>
                    setSelectedDoc({
                      path: item.path,
                      title: isSdlcLog
                        ? "📘 SDLC Engineering Journal & Rationale"
                        : `📄 ${item.name}`,
                    })
                  }
                  className="touch-target flex-shrink-0 inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-blue-600/20 hover:bg-blue-600/40 text-blue-300 border border-blue-500/30 text-xs font-medium transition"
                  title={t.artifacts.openDoc}
                >
                  <ExternalLink className="h-3 w-3" />
                  <span className="hidden sm:inline">{t.artifacts.openDoc}</span>
                </button>
              </div>
            );
          })
        )}
      </div>

      {/* Modal Popup Viewer */}
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
