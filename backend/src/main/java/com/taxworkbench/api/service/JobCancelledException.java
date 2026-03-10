package com.taxworkbench.api.service;

public class JobCancelledException extends RuntimeException {
    public JobCancelledException(String message) {
        super(message);
    }
}
