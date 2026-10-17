"use client";

import React, { useState, useEffect, useMemo } from "react";
import { createPortal } from "react-dom";
import {
  X,
  Download,
  Copy,
  Check,
  FileText,
  Loader2,
  Printer,
  ExternalLink,
  Maximize2,
  Minimize2,
  Code,
  FileCode,
  Terminal,
} from "lucide-react";
import { fetchArtifactContent } from "@/lib/api";
import { MarkdownViewer } from "./MarkdownViewer";
import { useLanguage } from "@/lib/i18n";

interface DocumentViewerModalProps {
  isOpen: boolean;
  onClose: () => void;
  workflowId: string;
  filePath: string;
  title?: string;
}

export function DocumentViewerModal({
  isOpen,
  onClose,
  workflowId,
  filePath,
  title,
}: DocumentViewerModalProps) {
  const { t, language } = useLanguage();
  const [content, setContent] = useState<string>("");
  const [fileName, setFileName] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(true);
  const [copied, setCopied] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [isMaximized, setIsMaximized] = useState<boolean>(false);
  const [mounted, setMounted] = useState<boolean>(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  // Lock body scroll while modal is open
  useEffect(() => {
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = originalOverflow;
      };
    }
  }, [isOpen]);

  // Handle ESC key to close
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && isOpen) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  useEffect(() => {
    if (!isOpen || !workflowId || !filePath) return;

    let isMounted = true;
    setLoading(true);
    setError(null);

    fetchArtifactContent(workflowId, filePath)
      .then((data) => {
        if (isMounted) {
          setContent(data.content);
          setFileName(data.fileName);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err.message || t.documentViewer.loadError);
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isOpen, workflowId, filePath, t]);

  const fileTypeInfo = useMemo(() => {
    const lower = (fileName || filePath).toLowerCase();
    if (lower.endsWith(".md")) return { isMarkdown: true, label: "MARKDOWN", color: "text-blue-400 bg-blue-500/10 border-blue-500/30" };
    if (lower.endsWith(".java")) return { isMarkdown: false, label: "JAVA 21", color: "text-orange-400 bg-orange-500/10 border-orange-500/30" };
    if (lower.endsWith(".tsx") || lower.endsWith(".ts")) return { isMarkdown: false, label: "TYPESCRIPT REACT", color: "text-sky-400 bg-sky-500/10 border-sky-500/30" };
    if (lower.endsWith(".sql")) return { isMarkdown: false, label: "POSTGRESQL DDL", color: "text-cyan-400 bg-cyan-500/10 border-cyan-500/30" };
    if (lower.endsWith(".json")) return { isMarkdown: false, label: "JSON CONFIG", color: "text-emerald-400 bg-emerald-500/10 border-emerald-500/30" };
    if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return { isMarkdown: false, label: "YAML MANIFEST", color: "text-rose-400 bg-rose-500/10 border-rose-500/30" };
    if (lower.endsWith(".xml")) return { isMarkdown: false, label: "XML POM", color: "text-amber-400 bg-amber-500/10 border-amber-500/30" };
    if (lower.endsWith("dockerfile") || lower.includes("docker")) return { isMarkdown: false, label: "DOCKERFILE", color: "text-indigo-400 bg-indigo-500/10 border-indigo-500/30" };
    return { isMarkdown: false, label: "SOURCE CODE", color: "text-slate-400 bg-slate-500/10 border-slate-500/30" };
  }, [fileName, filePath]);

  const lines = useMemo(() => {
    if (!content) return [];
    return content.split("\n");
  }, [content]);

  if (!isOpen || !mounted) return null;

  const handleCopy = () => {
    navigator.clipboard.writeText(content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownload = () => {
    const blob = new Blob([content], {
      type: fileTypeInfo.isMarkdown ? "text/markdown;charset=utf-8" : "text/plain;charset=utf-8",
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName || (fileTypeInfo.isMarkdown ? "document.md" : "code.txt");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleOpenInNewTab = () => {
    const mimeType = fileTypeInfo.isMarkdown ? "text/plain;charset=utf-8" : "text/plain;charset=utf-8";
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    window.open(url, "_blank");
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[9999] flex items-center justify-center p-2 sm:p-4 md:p-6 bg-black/85 backdrop-blur-md animate-in fade-in duration-150"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className={`w-full flex flex-col glass-panel border border-slate-700 bg-slate-950 shadow-2xl overflow-hidden relative transition-all duration-200 ${
          isMaximized ? "fixed inset-0 h-screen w-screen rounded-none z-[9999]" : "max-w-5xl lg:max-w-6xl h-[92vh] max-h-[960px] rounded-2xl"
        }`}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="flex items-center justify-between px-4 sm:px-6 py-3.5 border-b border-slate-800 bg-slate-900/90 flex-shrink-0">
          <div className="flex items-center gap-3 min-w-0">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600/20 text-blue-400 border border-blue-500/30 flex-shrink-0">
              {fileTypeInfo.isMarkdown ? <FileText className="h-5 w-5" /> : <FileCode className="h-5 w-5 text-emerald-400" />}
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <h3 className="text-sm sm:text-base font-bold text-white truncate">
                  {title || fileName || t.documentViewer.previewTitle}
                </h3>
                <span className={`text-[10px] font-mono px-2 py-0.5 rounded border font-semibold ${fileTypeInfo.color}`}>
                  {fileTypeInfo.label}
                </span>
              </div>
              <p className="text-[11px] font-mono text-slate-400 truncate mt-0.5">
                {filePath}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 flex-shrink-0">
            {/* Open in New Tab Button */}
            <button
              onClick={handleOpenInNewTab}
              disabled={loading || !!error}
              className="touch-target inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 transition"
              title={language === "en" ? "Open in New Tab" : "Buka di Tab Baru"}
            >
              <ExternalLink className="h-4 w-4 text-blue-400" />
              <span className="hidden md:inline">{language === "en" ? "New Tab" : "Tab Baru"}</span>
            </button>

            {/* Copy Button */}
            <button
              onClick={handleCopy}
              disabled={loading || !!error}
              className="touch-target inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 transition"
              title={t.documentViewer.copyTitle}
            >
              {copied ? <Check className="h-4 w-4 text-emerald-400" /> : <Copy className="h-4 w-4" />}
              <span className="hidden sm:inline">{copied ? t.documentViewer.copied : t.documentViewer.copy}</span>
            </button>

            {/* Print / Save as PDF Button */}
            <button
              onClick={() => window.print()}
              disabled={loading || !!error}
              className="touch-target inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600/90 hover:bg-purple-600 text-white text-xs font-semibold shadow-md shadow-purple-600/30 transition"
              title={t.documentViewer.pdfPrintTitle}
            >
              <Printer className="h-4 w-4" />
              <span className="hidden sm:inline">{t.documentViewer.pdfPrint}</span>
            </button>

            {/* Download Button */}
            <button
              onClick={handleDownload}
              disabled={loading || !!error}
              className="touch-target inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-md shadow-blue-600/30 transition"
              title={t.documentViewer.downloadTitle}
            >
              <Download className="h-4 w-4" />
              <span className="hidden sm:inline">{t.documentViewer.download}</span>
            </button>

            {/* Maximize Toggle */}
            <button
              onClick={() => setIsMaximized(!isMaximized)}
              className="touch-target p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition"
              title={isMaximized ? "Restore window" : "Maximize window"}
            >
              {isMaximized ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
            </button>

            {/* Close Button */}
            <button
              onClick={onClose}
              className="touch-target p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition"
              title={t.documentViewer.close}
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto font-sans text-slate-200 text-xs sm:text-sm leading-relaxed bg-[#0a0f1d] select-text">
          {loading ? (
            <div className="h-full flex flex-col items-center justify-center gap-3 text-slate-400 py-20">
              <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
              <p className="text-xs">{t.documentViewer.loading}</p>
            </div>
          ) : error ? (
            <div className="h-full flex flex-col items-center justify-center gap-2 text-rose-400 py-20">
              <p className="font-semibold">{error}</p>
              <p className="text-xs text-slate-500">{t.documentViewer.loadErrorHint}</p>
            </div>
          ) : fileTypeInfo.isMarkdown ? (
            <div className="p-4 sm:p-6 lg:p-8">
              <MarkdownViewer content={content} />
            </div>
          ) : (
            /* IDE Code Viewer */
            <div className="flex min-h-full font-mono text-xs sm:text-sm bg-[#0d1117] text-slate-200">
              {/* Line Numbers Column */}
              <div className="select-none py-4 px-3 sm:px-4 bg-[#090d16] text-slate-600 text-right border-r border-slate-800 flex flex-col font-mono text-xs min-w-[48px] sm:min-w-[60px]">
                {lines.map((_, i) => (
                  <span key={i} className="leading-6">
                    {i + 1}
                  </span>
                ))}
              </div>

              {/* Code Content Column */}
              <div className="flex-1 overflow-x-auto py-4 px-4 sm:px-6">
                <pre className="font-mono leading-6 whitespace-pre">
                  <code>{content}</code>
                </pre>
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="px-4 sm:px-6 py-2.5 border-t border-slate-800/80 bg-slate-950 flex items-center justify-between text-[11px] text-slate-500 flex-shrink-0">
          <div className="flex items-center gap-3">
            <span>{fileTypeInfo.isMarkdown ? t.documentViewer.formatFooter : `${lines.length} lines • ${content.length} characters`}</span>
            <span className="hidden sm:inline text-slate-600">|</span>
            <span className="hidden sm:inline text-slate-400 font-mono">{filePath}</span>
          </div>
          <button
            onClick={onClose}
            className="px-3.5 py-1.5 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 text-xs font-medium"
          >
            {t.documentViewer.close}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
