"use client";

import React, { useState, useMemo } from "react";
import { X, Sparkles, Loader2, Play } from "lucide-react";
import { startWorkflow } from "@/lib/api";
import { useLanguage } from "@/lib/i18n";

interface StartWorkflowModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated: () => void;
}

export function StartWorkflowModal({ isOpen, onClose, onCreated }: StartWorkflowModalProps) {
  const { t } = useLanguage();
  const [taskName, setTaskName] = useState("");
  const [description, setDescription] = useState("");
  const [projectId, setProjectId] = useState("");
  const [clientId, setClientId] = useState("");
  const [loading, setLoading] = useState(false);

  const presets = useMemo(() => [
    {
      title: t.modalNew.preset1Title,
      desc: t.modalNew.preset1Desc,
    },
    {
      title: t.modalNew.preset2Title,
      desc: t.modalNew.preset2Desc,
    },
    {
      title: t.modalNew.preset3Title,
      desc: t.modalNew.preset3Desc,
    },
  ], [t]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskName.trim()) return;

    try {
      setLoading(true);
      await startWorkflow(taskName, description, projectId.trim(), clientId.trim());
      setTaskName("");
      setDescription("");
      setProjectId("");
      setClientId("");
      onCreated();
      onClose();
    } catch (err: any) {
      alert("Error: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleApplyPreset = (preset: { title: string; desc: string }) => {
    setTaskName(preset.title);
    setDescription(preset.desc);
    if (!projectId) {
      setProjectId("PRJ-" + preset.title.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 8));
    }
    if (!clientId) {
      setClientId("CLIENT-ENTERPRISE");
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-lg rounded-2xl glass-panel p-5 sm:p-6 border border-slate-700 shadow-2xl relative max-h-[90vh] overflow-y-auto">
        <button
          onClick={onClose}
          className="touch-target absolute top-4 right-4 p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="flex items-center gap-2.5 mb-4">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600/20 text-blue-400 border border-blue-500/30">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-white">{t.modalNew.title}</h3>
            <p className="text-xs text-slate-400">{t.modalNew.subtitle}</p>
          </div>
        </div>

        {/* Quick Presets */}
        <div className="mb-4">
          <label className="block text-xs font-semibold text-slate-300 mb-1.5">{t.modalNew.quickPresets}</label>
          <div className="flex flex-col gap-1.5">
            {presets.map((p) => (
              <button
                key={p.title}
                type="button"
                onClick={() => handleApplyPreset(p)}
                className="text-left p-2 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-blue-500/50 hover:bg-slate-800/60 transition group"
              >
                <div className="text-xs font-medium text-white group-hover:text-blue-400">{p.title}</div>
                <div className="text-[11px] text-slate-400 line-clamp-1">{p.desc}</div>
              </button>
            ))}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">{t.modalNew.projectIdLabel}</label>
              <input
                type="text"
                value={projectId}
                onChange={(e) => setProjectId(e.target.value)}
                placeholder={t.modalNew.projectIdPlaceholder}
                className="w-full rounded-xl bg-slate-900 border border-slate-700 px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">{t.modalNew.clientIdLabel}</label>
              <input
                type="text"
                value={clientId}
                onChange={(e) => setClientId(e.target.value)}
                placeholder={t.modalNew.clientIdPlaceholder}
                className="w-full rounded-xl bg-slate-900 border border-slate-700 px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">{t.modalNew.taskNameLabel}</label>
            <input
              type="text"
              required
              value={taskName}
              onChange={(e) => setTaskName(e.target.value)}
              placeholder={t.modalNew.taskNamePlaceholder}
              className="w-full rounded-xl bg-slate-900 border border-slate-700 px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">{t.modalNew.descLabel}</label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t.modalNew.descPlaceholder}
              className="w-full rounded-xl bg-slate-900 border border-slate-700 px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 resize-none"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="touch-target px-4 py-2 rounded-xl bg-slate-800 text-xs font-medium text-slate-300 hover:bg-slate-700 transition"
            >
              {t.modalNew.cancelBtn}
            </button>
            <button
              type="submit"
              disabled={loading}
              className="touch-target inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-blue-600 text-xs font-semibold text-white shadow-md shadow-blue-600/30 hover:bg-blue-500 active:scale-95 disabled:opacity-50 transition"
            >
              {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4" />}
              <span>{loading ? t.modalNew.launchingBtn : t.modalNew.submitBtn}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
