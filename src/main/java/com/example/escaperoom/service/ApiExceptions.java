package com.example.escaperoom.service;

public final class ApiExceptions {
    private ApiExceptions() {}

    public static class NotFound extends RuntimeException {
        public NotFound(String msg) { super(msg); }
    }

    public static class Conflict extends RuntimeException {
        public Conflict(String msg) { super(msg); }
    }

    public static class Gone extends RuntimeException {
        public Gone(String msg) { super(msg); }
    }
}
