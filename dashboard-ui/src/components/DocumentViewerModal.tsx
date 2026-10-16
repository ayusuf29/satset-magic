"use client";

import React, { useState, useEffect } from "react";
import { X, Download, Copy, Check, FileText, Loader2, Printer } from "lucide-react";
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
  const { t } = useLanguage();
  const [content, setContent] = useState<string>("");
  const [fileName, setFileName] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(true);
  const [copied, setCopied] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

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

  if (!isOpen) return null;

  const handleCopy = () => {
    navigator.clipboard.writeText(content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownload = () => {
    const blob = new Blob([content], { type: "text/markdown;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName || "document.md";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 md:p-6 bg-black/80 backdrop-blur-md animate-in fade-in duration-150">
      <div className="w-full max-w-5xl lg:max-w-6xl h-[92vh] max-h-[960px] flex flex-col rounded-2xl glass-panel border border-slate-700 bg-slate-950 shadow-2xl overflow-hidden relative">
        {/* Modal Header */}
        <div className="flex items-center justify-between px-4 sm:px-6 py-3.5 border-b border-slate-800 bg-slate-900/90 flex-shrink-0">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600/20 text-blue-400 border border-blue-500/30 flex-shrink-0">
              <FileText className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <h3 className="text-sm sm:text-base font-bold text-white truncate">
                {title || fileName || t.documentViewer.previewTitle}
              </h3>
              <p className="text-[11px] font-mono text-slate-400 truncate">
                {filePath}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 flex-shrink-0">
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
              className="touch-target inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-semibold shadow-md shadow-purple-600/30 transition"
              title={t.documentViewer.pdfPrintTitle}
            >
              <Printer className="h-4 w-4" />
              <span className="hidden sm:inline">{t.documentViewer.pdfPrint}</span>
            </button>

            {/* Download Button (.md) */}
            <button
              onClick={handleDownload}
              disabled={loading || !!error}
              className="touch-target inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-md shadow-blue-600/30 transition"
              title={t.documentViewer.downloadTitle}
            >
              <Download className="h-4 w-4" />
              <span className="hidden sm:inline">{t.documentViewer.download}</span>
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

        {/* Modal Body / Markdown Viewer */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8 font-sans text-slate-200 text-xs sm:text-sm leading-relaxed bg-[#0a0f1d] select-text">
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
          ) : (
            <MarkdownViewer content={content} />
          )}
        </div>

        {/* Modal Footer */}
        <div className="px-4 sm:px-6 py-2.5 border-t border-slate-800/80 bg-slate-950 flex items-center justify-between text-[11px] text-slate-500 flex-shrink-0">
          <span>{t.documentViewer.formatFooter}</span>
          <button
            onClick={onClose}
            className="px-3.5 py-1.5 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 text-xs font-medium"
          >
            {t.documentViewer.close}
          </button>
        </div>
      </div>
    </div>
  );
}
