"use client";

import React, { useEffect, useState, useRef } from "react";
import { Copy, Check, Eye, Code, AlertTriangle, Loader2, Sparkles, Maximize2, Minimize2 } from "lucide-react";

interface MermaidDiagramProps {
  chart: string;
  id?: string;
}

export function MermaidDiagram({ chart, id }: MermaidDiagramProps) {
  const [svgHtml, setSvgHtml] = useState<string>("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [viewSource, setViewSource] = useState<boolean>(false);
  const [copied, setCopied] = useState<boolean>(false);
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const cleanChart = chart.trim();

  useEffect(() => {
    let isMounted = true;
    setLoading(true);
    setError(null);

    const renderChart = async () => {
      try {
        const mermaid = (await import("mermaid")).default;
        mermaid.initialize({
          startOnLoad: false,
          theme: "dark",
          securityLevel: "loose",
          fontFamily: "ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
          themeVariables: {
            darkMode: true,
            background: "#090d16",
            primaryColor: "#1e293b",
            primaryTextColor: "#f1f5f9",
            primaryBorderColor: "#3b82f6",
            lineColor: "#60a5fa",
            secondaryColor: "#0f172a",
            tertiaryColor: "#1e1e38",
            noteBkgColor: "#1e293b",
            noteTextColor: "#e2e8f0",
          },
        });

        // Ensure unique ID with sanitization
        const uniqueId = `mermaid-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
        const { svg } = await mermaid.render(uniqueId, cleanChart);

        if (isMounted) {
          setSvgHtml(svg);
          setLoading(false);
        }
      } catch (err: any) {
        if (isMounted) {
          console.warn("Notice: could not render Mermaid diagram:", err);
          setError(err?.message || "Invalid Mermaid syntax or unclosed diagram blocks.");
          setLoading(false);
        }
      }
    };

    renderChart();

    return () => {
      isMounted = false;
    };
  }, [cleanChart]);

  const handleCopy = () => {
    navigator.clipboard.writeText(cleanChart);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div
      ref={containerRef}
      className={`my-4 rounded-2xl border border-slate-800 bg-[#090d16] overflow-hidden shadow-2xl transition-all ${
        isFullscreen ? "fixed inset-4 z-50 flex flex-col bg-slate-950/95 backdrop-blur-xl border-blue-500/40 p-4" : ""
      }`}
    >
      {/* Header Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-2.5 bg-slate-900/90 border-b border-slate-800 text-xs text-slate-300">
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/20 font-mono text-[11px] font-bold">
            <Sparkles className="h-3 w-3" />
            MERMAID DIAGRAM
          </span>
          <span className="text-[11px] text-slate-400 hidden sm:inline font-mono">
            Interactive Architecture Visualizer
          </span>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setViewSource(!viewSource)}
            className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-medium transition border ${
              viewSource
                ? "bg-blue-600 text-white border-blue-500"
                : "bg-slate-800 text-slate-300 hover:text-white border-slate-700"
            }`}
          >
            {viewSource ? (
              <>
                <Eye className="h-3 w-3" />
                <span>Lihat Visual</span>
              </>
            ) : (
              <>
                <Code className="h-3 w-3" />
                <span>Lihat Kode</span>
              </>
            )}
          </button>

          <button
            onClick={handleCopy}
            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 text-[11px] font-medium transition"
            title="Salin sintaks Mermaid"
          >
            {copied ? <Check className="h-3 w-3 text-emerald-400" /> : <Copy className="h-3 w-3" />}
            <span>{copied ? "Tersalin" : "Salin"}</span>
          </button>

          <button
            onClick={() => setIsFullscreen(!isFullscreen)}
            className="p-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 transition"
            title={isFullscreen ? "Kecilkan Tampilan" : "Perbesar Penuh Layar"}
          >
            {isFullscreen ? <Minimize2 className="h-3.5 w-3.5" /> : <Maximize2 className="h-3.5 w-3.5" />}
          </button>
        </div>
      </div>

      {/* Body: Visual Diagram or Raw Code */}
      <div className={`p-4 overflow-auto ${isFullscreen ? "flex-1" : "max-h-[600px]"}`}>
        {loading ? (
          <div className="flex items-center justify-center py-12 gap-2 text-slate-400 text-xs font-mono">
            <Loader2 className="h-4 w-4 animate-spin text-blue-400" />
            <span>Merender diagram visual Mermaid...</span>
          </div>
        ) : error ? (
          <div className="space-y-3">
            <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs flex items-start gap-2.5 font-sans">
              <AlertTriangle className="h-4 w-4 text-amber-400 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">Notice saat merender diagram visual Mermaid:</p>
                <p className="text-[11px] text-amber-400/80 mt-0.5 font-mono">{error}</p>
              </div>
            </div>
            <pre className="p-3.5 bg-slate-950 rounded-xl border border-slate-800 font-mono text-xs text-slate-200 overflow-x-auto leading-relaxed">
              {cleanChart}
            </pre>
          </div>
        ) : viewSource ? (
          <pre className="p-3.5 bg-slate-950 rounded-xl border border-slate-800 font-mono text-xs text-blue-300 overflow-x-auto leading-relaxed">
            {cleanChart}
          </pre>
        ) : (
          <div
            className="mermaid-svg-container flex justify-center items-center [&_svg]:max-w-full [&_svg]:h-auto transition-transform"
            dangerouslySetInnerHTML={{ __html: svgHtml }}
          />
        )}
      </div>
    </div>
  );
}
