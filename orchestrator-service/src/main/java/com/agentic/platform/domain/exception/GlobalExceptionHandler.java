package com.agentic.platform.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ProblemDetail handleWorkflowNotFound(WorkflowNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Workflow Not Found");
        problem.setType(URI.create("https://agentic.platform/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidWorkflowTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidWorkflowTransitionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Workflow Transition");
        problem.setType(URI.create("https://agentic.platform/errors/invalid-transition"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://agentic.platform/errors/bad-request"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource not found: " + ex.getResourcePath());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://agentic.platform/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.web.context.request.async.AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeout(org.springframework.web.context.request.async.AsyncRequestTimeoutException ex) {
        // SSE normal keep-alive timeout lifecycle. Mengizinkan browser reconnect secara natural.
    }

    @ExceptionHandler(org.springframework.web.context.request.async.AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(org.springframework.web.context.request.async.AsyncRequestNotUsableException ex) {
        // SSE client disconnected or aborted connection. Ignore cleanly without logging false 500 error.
    }

    @ExceptionHandler(java.io.IOException.class)
    public void handleIOException(java.io.IOException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("Broken pipe") || msg.contains("Connection reset") || ex.getClass().getSimpleName().contains("ClientAbortException")) {
            log.debug("Client closed stream connection: {}", msg);
            return;
        }
        log.warn("IO Exception encountered: {}", ex.getMessage());
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("Broken pipe") || msg.contains("Connection reset") || ex.getClass().getSimpleName().contains("ClientAbortException") || ex.getClass().getSimpleName().contains("AsyncRequestNotUsableException")) {
            log.debug("Client connection terminated: {}", msg);
            return null;
        }
        log.error("Unhandled exception: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage() != null ? ex.getMessage() : "An unexpected internal error occurred.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://agentic.platform/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
