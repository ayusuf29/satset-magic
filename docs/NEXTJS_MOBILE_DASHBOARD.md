# NEXT.JS MOBILE COMMAND DASHBOARD SPECIFICATION

## 1. Gambaran Antarmuka
Dashboard responsif mobile (PWA-ready) yang dirancang untuk memonitor seluruh aktivitas agen dan memberikan keputusan Human-in-the-Loop secara instan.

## 2. Fitur Utama
1. **Live Agent Status Grid**:
   - Kartu status tiap agen: IDLE, WORKING, WAITING_APPROVAL, ERROR.
   - Current task name, waktu eksekusi, dan live progress bar.
2. **Terminal Log Streamer**:
   - Menampilkan output streaming console via SSE.
3. **Human Decision Modal / Card**:
   - Tombol cepat: `[Approve & Continue]`, `[Reject with Notes]`, `[Override]`.
   - Form input instruksi revisi yang langsung di-push kembali ke antrean agent.
