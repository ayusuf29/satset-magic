# MERMAID & REMOTION ARTIFACT HUB

Direktori ini (`c:\project\satset-magic\mermaid-remotion`) berisi seluruh artefak visualisasi arsitektur dan video presentasi untuk **Satset Autonomous Engineering Platform**:

---

## 1. Struktur Direktori

```text
mermaid-remotion/
├── mermaid/
│   ├── 01_end_to_end_workflow.mmd      # Flowchart lengkap Phase 0 - 9 dengan HITL Code Review & Branch Protection
│   ├── 02_telegram_hitl_sequence.mmd   # Sequence Diagram interaksi Telegram Bot, Orchestrator, & Git Merge
│   └── 03_triad_council_consensus.mmd  # Flowchart proses konsensus Triad Council (Architect, Security, Perf)
├── src/
│   ├── scenes/
│   │   ├── Scene1_Intro.tsx            # Scene 1: Platform Introduction & 5-Team Topology Swarm
│   │   ├── Scene2_Consensus.tsx        # Scene 2: Triad Council Consensus Voting Formula
│   │   ├── Scene3_BranchProtection.tsx # Scene 3: Resolusi GAP HITL & Protected Main Branch
│   │   ├── Scene4_TelegramHITL.tsx     # Scene 4: Mobile Telegram Approval Interactive Mockup
│   │   ├── Scene5_MergeAndRelease.tsx  # Scene 5: Git Merge Commit & Pre-Prod Security/Stress Gate
│   │   └── Scene6_Outro.tsx            # Scene 6: Outro & Enterprise Autonomous Summary
│   ├── MainComposition.tsx             # Sequence timeline orchestrator (1800 frames / 60s @ 30 FPS)
│   ├── Root.tsx                        # Remotion Composition Root
│   ├── index.ts                        # Entry point Remotion registerRoot
│   └── types.ts                        # Shared types, themes, dan frame configs
├── package.json                        # Dependencies Remotion, React 18, TypeScript
├── tsconfig.json                       # Konfigurasi TypeScript
├── remotion.config.ts                  # Konfigurasi rendering Remotion
└── README.md                           # Dokumentasi operasional hub ini
```

---

## 2. Cara Menggunakan File Mermaid

Semua file diagram berekstensi `.mmd` di folder `mermaid/` dapat dibuka dan dirender dengan beberapa cara:

1. **Di IDE / VS Code**: Gunakan ekstensi *Mermaid Preview* atau *Markdown Preview Mermaid Support*.
2. **Mermaid Live Editor**: Buka [mermaid.live](https://mermaid.live) dan salin isi file `.mmd`.
3. **Mermaid CLI (Export ke SVG/PNG)**:
   ```bash
   npx -y @mermaid-js/mermaid-cli -i mermaid/01_end_to_end_workflow.mmd -o mermaid/01_end_to_end_workflow.svg
   npx -y @mermaid-js/mermaid-cli -i mermaid/02_telegram_hitl_sequence.mmd -o mermaid/02_telegram_hitl_sequence.svg
   npx -y @mermaid-js/mermaid-cli -i mermaid/03_triad_council_consensus.mmd -o mermaid/03_triad_council_consensus.svg
   ```

---

## 3. Cara Menjalankan Video Remotion

Proyek Remotion ini dikonfigurasi untuk video beresolusi **1920x1080 (16:9), 30 FPS, durasi 60 detik (1800 frames)**.

### A. Preview Video di Browser (Remotion Studio)
```bash
cd mermaid-remotion
npm install
npm run preview
```
*Browser akan otomatis membuka Remotion Player di `http://localhost:3000` dengan kontrol scrubbing frame-by-frame.*

### B. Render ke File Video MP4
```bash
cd mermaid-remotion
npm run build
```
*Hasil video akan tersimpan di `mermaid-remotion/out/workflow_explainer.mp4`.*

---

## 4. Status Implementasi
> [!NOTE]
> Seluruh file di direktori ini adalah spesifikasi desain dan komponen visual representasi. **Belum ada perubahan yang diimplementasikan ke runtime backend (`orchestrator-service`) atau live database.** Implementasi hanya akan dijalankan saat Anda memberikan persetujuan selanjutnya.
