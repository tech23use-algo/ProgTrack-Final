package com.openspace.skillplanner.exception;

public final class ApiExceptions {
    private ApiExceptions() {
    }

    public static class PlanNotFoundException extends RuntimeException {
        public PlanNotFoundException() {
            super("Skill plan was not found.");
        }
    }

    public static class TaskNotFoundException extends RuntimeException {
        public TaskNotFoundException() {
            super("Task was not found in this skill plan.");
        }
    }

    public static class SkillNotAcceptedException extends RuntimeException {
        public SkillNotAcceptedException(String reason) {
            super(reason);
        }
    }

    public static class PlanGenerationFailedException extends RuntimeException {
        public PlanGenerationFailedException() {
            super("A valid learning plan could not be generated. Please try again.");
        }
    }

    public static class UpstreamAuthenticationException extends RuntimeException {
        public UpstreamAuthenticationException() {
            super("AI provider authentication failed.");
        }
    }

    public static class UpstreamUnavailableException extends RuntimeException {
        private final Long retryAfterSeconds;

        public UpstreamUnavailableException(Long retryAfterSeconds) {
            super("AI plan generation is temporarily unavailable. Please try again later.");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public Long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    public static class UpstreamTimeoutException extends RuntimeException {
        public UpstreamTimeoutException() {
            super("AI plan generation timed out. Please try again.");
        }
    }

    public static class GenerationRateLimitException extends RuntimeException {
        private final long retryAfterSeconds;

        public GenerationRateLimitException(long retryAfterSeconds) {
            super("Plan generation limit reached. Please try again later.");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
