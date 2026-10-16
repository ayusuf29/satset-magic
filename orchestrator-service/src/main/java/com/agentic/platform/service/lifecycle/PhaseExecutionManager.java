package com.agentic.platform.service.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class PhaseExecutionManager {

    private static final Logger log = LoggerFactory.getLogger(PhaseExecutionManager.class);

    private final List<PhaseLifecycleObserver> observers;

    public PhaseExecutionManager(List<PhaseLifecycleObserver> observers) {
        this.observers = observers;
        log.info("PhaseExecutionManager initialized with {} lifecycle observers: {}",
                observers.size(), observers.stream().map(o -> o.getClass().getSimpleName()).toList());
    }

    public PhaseResult executePhase(PhaseContext context, Function<Consumer<ProgressUpdate>, PhaseResult> taskFunction) {
        log.info("[PRE-TASK] Starting phase lifecycle for: {} | Workflow: {} | Project: {}",
                context.getPhase(), context.getWorkflowId(), context.getProjectId());

        // 1. PRE-TASK HOOK
        notifyPreTask(context);

        long start = System.currentTimeMillis();
        try {
            // 2. ON-TASK HOOK (with progress callback)
            Consumer<ProgressUpdate> progressReporter = update -> {
                notifyProgress(context, update.percentage(), update.activity());
            };

            PhaseResult result = taskFunction.apply(progressReporter);

            long duration = System.currentTimeMillis() - start;
            log.info("[POST-TASK] Phase completed: {} in {}ms | Status: {}", context.getPhase(), duration, result.getStatus());

            // 3. POST-TASK HOOK
            notifyPostTask(context, result);
            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("[FAILED-TASK] Phase failed: {} in {}ms | Error: {}", context.getPhase(), duration, ex.getMessage(), ex);

            notifyFailed(context, ex);
            throw (ex instanceof RuntimeException re) ? re : new RuntimeException(ex);
        }
    }

    public void reportProgress(PhaseContext context, int percentage, String activity) {
        notifyProgress(context, percentage, activity);
    }

    private void notifyPreTask(PhaseContext context) {
        for (PhaseLifecycleObserver observer : observers) {
            try {
                observer.onPreTask(context);
            } catch (Exception ex) {
                log.warn("Observer {} failed onPreTask: {}", observer.getClass().getSimpleName(), ex.getMessage());
            }
        }
    }

    private void notifyProgress(PhaseContext context, int progress, String activity) {
        for (PhaseLifecycleObserver observer : observers) {
            try {
                observer.onTaskProgress(context, progress, activity);
            } catch (Exception ex) {
                log.warn("Observer {} failed onTaskProgress: {}", observer.getClass().getSimpleName(), ex.getMessage());
            }
        }
    }

    private void notifyPostTask(PhaseContext context, PhaseResult result) {
        for (PhaseLifecycleObserver observer : observers) {
            try {
                observer.onPostTask(context, result);
            } catch (Exception ex) {
                log.warn("Observer {} failed onPostTask: {}", observer.getClass().getSimpleName(), ex.getMessage());
            }
        }
    }

    private void notifyFailed(PhaseContext context, Throwable ex) {
        for (PhaseLifecycleObserver observer : observers) {
            try {
                observer.onTaskFailed(context, ex);
            } catch (Exception e) {
                log.warn("Observer {} failed onTaskFailed: {}", observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    public record ProgressUpdate(int percentage, String activity) {}
}
