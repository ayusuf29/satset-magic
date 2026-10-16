package com.agentic.platform.service.lifecycle;

public interface PhaseLifecycleObserver {

    /**
     * Pre-task hook: Dipanggil sebelum autonomous agent mulai mengeksekusi tugas pada suatu fase.
     * Digunakan untuk inisialisasi state, mengunci interupsi UI, dan memancarkan telemetri awal.
     */
    void onPreTask(PhaseContext context);

    /**
     * On-task hook: Dipanggil selama fase berjalan untuk melaporkan kemajuan berkala dan aktivitas terkini.
     */
    void onTaskProgress(PhaseContext context, int progressPercentage, String currentActivity);

    /**
     * Post-task hook: Dipanggil setelah fase berhasil diselesaikan.
     * Digunakan untuk mencatat audit journal (SDLC_LOG.md), memancarkan ringkasan hasil, dan evaluasi gerbang.
     */
    void onPostTask(PhaseContext context, PhaseResult result);

    /**
     * Failure hook: Dipanggil jika terjadi unhandled error/exception saat mengeksekusi fase.
     */
    void onTaskFailed(PhaseContext context, Throwable throwable);
}
