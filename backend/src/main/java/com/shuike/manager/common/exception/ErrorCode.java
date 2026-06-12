package com.shuike.manager.common.exception;

public interface ErrorCode {
    int SUCCESS = 200;
    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    int CONFLICT = 409;
    int INTERNAL_ERROR = 500;
    int INVALID_CREDENTIALS = 1001;
    int ACCOUNT_LOCKED = 1002;
    int PLAN_ALREADY_SUBMITTED = 2001;
    int PLAN_STATUS_INVALID = 2002;
    int FILE_TYPE_NOT_ALLOWED = 3001;
    int FILE_SIZE_EXCEEDED = 3002;
    int AI_SERVICE_UNAVAILABLE = 4001;
    int AI_EVALUATION_FAILED = 4002;
    int AI_PARSE_FAILED = 4003;
    int DUPLICATE_USERNAME = 5001;
}
