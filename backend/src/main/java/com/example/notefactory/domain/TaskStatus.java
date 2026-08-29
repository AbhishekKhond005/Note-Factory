package com.example.notefactory.domain;

public enum TaskStatus {
    PENDING,
    QUEUED,
    RUNNING,
    VALIDATING,
    RETRYING,
    COMPLETE,
    FAILED,
    CANCELLED
}
