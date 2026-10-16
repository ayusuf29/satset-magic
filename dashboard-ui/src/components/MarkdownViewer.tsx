"use client";

import React, { useState } from "react";
import { Copy, Check, Eye, Code, FileText, AlertCircle, Info, ShieldAlert, Sparkles } from "lucide-react";

interface MarkdownViewerProps {
  content: string;
}

export function MarkdownViewer({ content }: MarkdownViewerProps) {
  const [viewMode, setViewMode] = useState<"rendered" | "raw">("rendered");
  const [copiedBlockId, setCopiedBlockId] = useState<string | null>(null);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedBlockId(id);
    setTimeout(() => setCopiedBlockId(null), 2000);
  };

  if (viewMode === "raw") {
    return (
      <div className="space-y-3">
        <div className="flex items-center justify-between pb-2 border-b border-slate-800">
          <span className="text-xs font-mono text-slate-400">Tampilan Kode Sumber (.md mentah)</span>
          <button
            onClick={() => setViewMode("rendered")}
            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-blue-600/20 text-blue-300 border border-blue-500/30 text-xs font-medium hover:bg-blue-600/40 transition"
          >
            <Eye className="h-3.5 w-3.5" />
            <span>Kembali ke Pratinjau Terformat</span>
          </button>
        </div>
        <pre className="whitespace-pre-wrap font-mono text-xs text-slate-200 bg-slate-950 p-4 rounded-xl border border-slate-800 overflow-x-auto leading-relaxed">
          {content}
        </pre>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* View Switcher bar */}
      <div className="flex items-center justify-between pb-3 border-b border-slate-800/80">
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/20 text-xs font-semibold">
            <Sparkles className="h-3.5 w-3.5" />
            Markdown Reader Aktif
          </span>
          <span className="text-[11px] text-slate-400 hidden sm:inline">
            Rendering dokumen visual dengan syntax highlighting & tabel GFM
          </span>
        </div>

        <button
          onClick={() => setViewMode("raw")}
          className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium border border-slate-700 transition"
          title="Lihat teks sumber mentah markdown"
        >
          <Code className="h-3.5 w-3.5" />
          <span>Lihat Raw (.md)</span>
        </button>
      </div>

      {/* Rendered Markdown Body */}
      <div className="space-y-3.5 text-slate-200 font-sans text-xs sm:text-sm leading-relaxed">
        {renderMarkdownSections(content, copyToClipboard, copiedBlockId)}
      </div>
    </div>
  );
}

function renderMarkdownSections(
  markdown: string,
  copyToClipboard: (text: string, id: string) => void,
  copiedBlockId: string | null
): React.ReactNode {
  const lines = markdown.split("\n");
  const elements: React.ReactNode[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    // Fenced Code Block
    if (line.trim().startsWith("```")) {
      const lang = line.trim().replace(/^```/, "").trim();
      const codeLines: string[] = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith("```")) {
        codeLines.push(lines[i]);
        i++;
      }
      i++; // skip closing ```
      const blockContent = codeLines.join("\n");
      const blockId = `code-${i}-${lang}`;

      elements.push(
        <div key={blockId} className="my-3 rounded-xl border border-slate-800 bg-slate-950/90 overflow-hidden shadow-lg">
          <div className="flex items-center justify-between px-3.5 py-1.5 bg-slate-900 border-b border-slate-800 text-[11px] text-slate-400 font-mono">
            <span className="font-semibold text-blue-400 uppercase">{lang || "CODE"}</span>
            <button
              onClick={() => copyToClipboard(blockContent, blockId)}
              className="inline-flex items-center gap-1 text-[10px] text-slate-400 hover:text-white px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 transition"
            >
              {copiedBlockId === blockId ? <Check className="h-3 w-3 text-emerald-400" /> : <Copy className="h-3 w-3" />}
              <span>{copiedBlockId === blockId ? "Tersalin" : "Salin"}</span>
            </button>
          </div>
          <pre className="p-3.5 overflow-x-auto font-mono text-xs text-slate-200 leading-relaxed">
            {codeLines.map((cl, idx) => {
              // Highlight Gherkin keywords
              if (lang.toLowerCase() === "gherkin" || cl.trim().startsWith("Given") || cl.trim().startsWith("When") || cl.trim().startsWith("Then") || cl.trim().startsWith("Scenario:") || cl.trim().startsWith("Feature:")) {
                return (
                  <div key={idx} className="leading-5">
                    {renderGherkinLine(cl)}
                  </div>
                );
              }
              return <div key={idx} className="leading-5">{cl || "\u00A0"}</div>;
            })}
          </pre>
        </div>
      );
      continue;
    }

    // Markdown Table
    if (line.trim().startsWith("|") && line.trim().endsWith("|")) {
      const tableLines: string[] = [];
      while (i < lines.length && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
        tableLines.push(lines[i]);
        i++;
      }

      if (tableLines.length >= 2) {
        elements.push(renderTable(tableLines, `table-${i}`));
        continue;
      }
    }

    // GitHub Alerts / Blockquotes (> [!NOTE], > [!WARNING], > etc)
    if (line.trim().startsWith(">")) {
      const quoteLines: string[] = [];
      while (i < lines.length && lines[i].trim().startsWith(">")) {
        quoteLines.push(lines[i].replace(/^>\s?/, ""));
        i++;
      }
      elements.push(renderCalloutOrQuote(quoteLines, `quote-${i}`));
      continue;
    }

    // Headings
    if (line.startsWith("# ")) {
      elements.push(
        <h1 key={`h1-${i}`} className="text-lg sm:text-xl font-bold text-white tracking-tight pb-2 mt-4 border-b border-slate-800 flex items-center gap-2">
          <span className="h-5 w-1 rounded-full bg-blue-500"></span>
          <span>{renderInline(line.replace(/^#\s+/, ""))}</span>
        </h1>
      );
      i++;
      continue;
    }

    if (line.startsWith("## ")) {
      elements.push(
        <h2 key={`h2-${i}`} className="text-base sm:text-lg font-bold text-slate-100 tracking-tight mt-5 mb-2 pb-1.5 border-b border-slate-800/60 flex items-center gap-2">
          <span className="h-4 w-1 rounded-full bg-indigo-500"></span>
          <span>{renderInline(line.replace(/^##\s+/, ""))}</span>
        </h2>
      );
      i++;
      continue;
    }

    if (line.startsWith("### ")) {
      elements.push(
        <h3 key={`h3-${i}`} className="text-sm sm:text-base font-semibold text-blue-300 mt-4 mb-1.5 flex items-center gap-1.5">
          <span>{renderInline(line.replace(/^###\s+/, ""))}</span>
        </h3>
      );
      i++;
      continue;
    }

    if (line.startsWith("#### ")) {
      elements.push(
        <h4 key={`h4-${i}`} className="text-xs sm:text-sm font-semibold text-emerald-300 mt-3 mb-1">
          {renderInline(line.replace(/^####\s+/, ""))}
        </h4>
      );
      i++;
      continue;
    }

    // Horizontal Rule
    if (/^(\*\*\*|---|___)$/.test(line.trim())) {
      elements.push(<hr key={`hr-${i}`} className="my-4 border-slate-800" />);
      i++;
      continue;
    }

    // Bullet Lists or Checkboxes
    if (/^\s*[-*+]\s+/.test(line)) {
      const listItems: { text: string; indent: number; checked?: boolean }[] = [];
      while (i < lines.length && /^\s*[-*+]\s+/.test(lines[i])) {
        const raw = lines[i];
        const matchIndent = raw.match(/^\s*/);
        const indent = matchIndent ? matchIndent[0].length : 0;
        let text = raw.replace(/^\s*[-*+]\s+/, "");
        let checked: boolean | undefined = undefined;

        if (text.startsWith("[x] ") || text.startsWith("[X] ")) {
          checked = true;
          text = text.substring(4);
        } else if (text.startsWith("[ ] ")) {
          checked = false;
          text = text.substring(4);
        }

        listItems.push({ text, indent, checked });
        i++;
      }

      elements.push(
        <ul key={`ul-${i}`} className="my-2.5 space-y-1.5 pl-2">
          {listItems.map((li, idx) => (
            <li
              key={idx}
              className={`flex items-start gap-2 ${li.indent > 0 ? "ml-4 text-slate-300" : "text-slate-200"}`}
            >
              {li.checked !== undefined ? (
                <input
                  type="checkbox"
                  readOnly
                  checked={li.checked}
                  className="mt-1 rounded bg-slate-800 border-slate-700 text-blue-500 focus:ring-0 cursor-default"
                />
              ) : (
                <span className="h-1.5 w-1.5 rounded-full bg-blue-400 mt-2 flex-shrink-0" />
              )}
              <div className="flex-1 leading-relaxed">{renderInline(li.text)}</div>
            </li>
          ))}
        </ul>
      );
      continue;
    }

    // Numbered Lists
    if (/^\s*\d+\.\s+/.test(line)) {
      const numItems: string[] = [];
      while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
        numItems.push(lines[i].replace(/^\s*\d+\.\s+/, ""));
        i++;
      }

      elements.push(
        <ol key={`ol-${i}`} className="my-2.5 space-y-1.5 list-decimal list-inside pl-2 text-slate-200">
          {numItems.map((item, idx) => (
            <li key={idx} className="leading-relaxed pl-1">
              {renderInline(item)}
            </li>
          ))}
        </ol>
      );
      continue;
    }

    // Regular Paragraph
    if (line.trim().length > 0) {
      elements.push(
        <p key={`p-${i}`} className="text-slate-300 leading-relaxed">
          {renderInline(line)}
        </p>
      );
    }

    i++;
  }

  return elements;
}

function renderTable(tableLines: string[], key: string): React.ReactNode {
  const headerLine = tableLines[0];
  const dataLines = tableLines.slice(2); // line 1 is separator |---|---|

  const parseCells = (row: string) =>
    row
      .split("|")
      .slice(1, -1)
      .map((c) => c.trim());

  const headers = parseCells(headerLine);

  return (
    <div key={key} className="my-3.5 overflow-x-auto rounded-xl border border-slate-800 bg-slate-950/80 shadow-md">
      <table className="w-full text-left border-collapse text-xs">
        <thead>
          <tr className="bg-slate-900/90 border-b border-slate-800 text-slate-200 font-semibold">
            {headers.map((h, idx) => (
              <th key={idx} className="py-2.5 px-3.5 border-r border-slate-800/80 last:border-r-0">
                {renderInline(h)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800/60">
          {dataLines.map((row, rIdx) => {
            const cells = parseCells(row);
            return (
              <tr key={rIdx} className="hover:bg-slate-900/40 transition">
                {cells.map((c, cIdx) => (
                  <td key={cIdx} className="py-2 px-3.5 border-r border-slate-800/60 last:border-r-0 text-slate-300">
                    {renderInline(c)}
                  </td>
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function renderCalloutOrQuote(quoteLines: string[], key: string): React.ReactNode {
  const first = quoteLines[0] || "";
  let alertType: "note" | "tip" | "important" | "warning" | "caution" | null = null;
  let remaining = [...quoteLines];

  if (first.includes("[!NOTE]")) {
    alertType = "note";
    remaining[0] = remaining[0].replace(/\[!NOTE\]/, "").trim();
  } else if (first.includes("[!TIP]")) {
    alertType = "tip";
    remaining[0] = remaining[0].replace(/\[!TIP\]/, "").trim();
  } else if (first.includes("[!IMPORTANT]")) {
    alertType = "important";
    remaining[0] = remaining[0].replace(/\[!IMPORTANT\]/, "").trim();
  } else if (first.includes("[!WARNING]")) {
    alertType = "warning";
    remaining[0] = remaining[0].replace(/\[!WARNING\]/, "").trim();
  } else if (first.includes("[!CAUTION]")) {
    alertType = "caution";
    remaining[0] = remaining[0].replace(/\[!CAUTION\]/, "").trim();
  }

  if (alertType) {
    const config = {
      note: { border: "border-blue-500/50", bg: "bg-blue-950/30", text: "text-blue-300", title: "NOTE", icon: <Info className="h-4 w-4 text-blue-400" /> },
      tip: { border: "border-emerald-500/50", bg: "bg-emerald-950/30", text: "text-emerald-300", title: "TIP", icon: <Sparkles className="h-4 w-4 text-emerald-400" /> },
      important: { border: "border-purple-500/50", bg: "bg-purple-950/30", text: "text-purple-300", title: "IMPORTANT", icon: <AlertCircle className="h-4 w-4 text-purple-400" /> },
      warning: { border: "border-amber-500/50", bg: "bg-amber-950/30", text: "text-amber-300", title: "WARNING", icon: <ShieldAlert className="h-4 w-4 text-amber-400" /> },
      caution: { border: "border-rose-500/50", bg: "bg-rose-950/30", text: "text-rose-300", title: "CAUTION", icon: <ShieldAlert className="h-4 w-4 text-rose-400" /> },
    }[alertType];

    return (
      <div key={key} className={`my-3 p-3.5 rounded-xl border ${config.border} ${config.bg} space-y-1.5`}>
        <div className="flex items-center gap-1.5 font-bold text-xs uppercase tracking-wider">
          {config.icon}
          <span className={config.text}>{config.title}</span>
        </div>
        <div className="text-slate-300 text-xs sm:text-[13px] space-y-1 pl-5">
          {remaining.filter(Boolean).map((l, idx) => (
            <p key={idx}>{renderInline(l)}</p>
          ))}
        </div>
      </div>
    );
  }

  return (
    <blockquote key={key} className="my-2.5 pl-3.5 border-l-2 border-slate-700 italic text-slate-400 space-y-1">
      {quoteLines.map((l, idx) => (
        <p key={idx}>{renderInline(l)}</p>
      ))}
    </blockquote>
  );
}

function renderGherkinLine(line: string): React.ReactNode {
  const trimmed = line.trim();
  if (trimmed.startsWith("Feature:")) {
    return <span className="text-purple-400 font-bold">{line}</span>;
  }
  if (trimmed.startsWith("Scenario:") || trimmed.startsWith("Scenario Outline:")) {
    return <span className="text-amber-400 font-bold">{line}</span>;
  }
  if (trimmed.startsWith("Given ")) {
    return (
      <span>
        <span className="text-cyan-400 font-bold">Given </span>
        <span className="text-slate-200">{line.replace(/^\s*Given\s/, "")}</span>
      </span>
    );
  }
  if (trimmed.startsWith("When ")) {
    return (
      <span>
        <span className="text-amber-300 font-bold">When </span>
        <span className="text-slate-200">{line.replace(/^\s*When\s/, "")}</span>
      </span>
    );
  }
  if (trimmed.startsWith("Then ")) {
    return (
      <span>
        <span className="text-emerald-400 font-bold">Then </span>
        <span className="text-slate-200">{line.replace(/^\s*Then\s/, "")}</span>
      </span>
    );
  }
  if (trimmed.startsWith("And ")) {
    return (
      <span>
        <span className="text-blue-400 font-bold">And </span>
        <span className="text-slate-200">{line.replace(/^\s*And\s/, "")}</span>
      </span>
    );
  }
  return <span className="text-slate-300">{line}</span>;
}

function renderInline(text: string): React.ReactNode {
  // Simple token parser for **bold**, *italic*, `code`, and [link](url)
  const parts: React.ReactNode[] = [];
  const regex = /(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`|\[[^\]]+\]\([^)]+\))/g;
  let lastIndex = 0;
  let match;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.substring(lastIndex, match.index));
    }

    const token = match[0];
    if (token.startsWith("**") && token.endsWith("**")) {
      parts.push(
        <strong key={match.index} className="font-bold text-white">
          {token.slice(2, -2)}
        </strong>
      );
    } else if (token.startsWith("*") && token.endsWith("*")) {
      parts.push(
        <em key={match.index} className="italic text-slate-300">
          {token.slice(1, -1)}
        </em>
      );
    } else if (token.startsWith("`") && token.endsWith("`")) {
      parts.push(
        <code key={match.index} className="font-mono text-[11px] px-1.5 py-0.5 rounded bg-slate-900 border border-slate-800 text-blue-300">
          {token.slice(1, -1)}
        </code>
      );
    } else if (token.startsWith("[") && token.includes("](") && token.endsWith(")")) {
      const textMatch = token.match(/\[([^\]]+)\]/);
      const urlMatch = token.match(/\(([^)]+)\)/);
      const label = textMatch ? textMatch[1] : token;
      const url = urlMatch ? urlMatch[1] : "#";
      parts.push(
        <span key={match.index} className="text-blue-400 underline decoration-blue-500/40 hover:text-blue-300 font-mono text-[11px]">
          {label}
        </span>
      );
    }

    lastIndex = regex.lastIndex;
  }

  if (lastIndex < text.length) {
    parts.push(text.substring(lastIndex));
  }

  return parts.length > 0 ? parts : text;
}
