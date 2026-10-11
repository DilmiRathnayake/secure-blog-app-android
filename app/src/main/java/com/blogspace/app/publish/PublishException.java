package com.blogspace.app.publish;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blogspace.app.R;

/** A publish attempt failed. {@link Kind} decides which friendly message the user sees. */
public class PublishException extends Exception {

    public enum Kind {
        NETWORK,
        AUTH,
        FORBIDDEN,
        RATE_LIMIT,
        REJECTED,
        NOT_FOUND,
        SERVER,
        SIMULATED,
        UNKNOWN
    }

    private final Kind kind;
    private final int httpCode;

    public PublishException(@NonNull Kind kind, @Nullable String detail) {
        this(kind, detail, 0, null);
    }

    public PublishException(@NonNull Kind kind, @Nullable String detail, int httpCode, @Nullable Throwable cause) {
        super(detail, cause);
        this.kind = kind;
        this.httpCode = httpCode;
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    public int getHttpCode() {
        return httpCode;
    }

    /** A message suitable for showing to the user. */
    @NonNull
    public String describe(@NonNull Context context) {
        String detail = getMessage() == null ? "" : getMessage();
        switch (kind) {
            case NETWORK:
                return context.getString(R.string.err_network);
            case AUTH:
                return context.getString(R.string.err_auth);
            case FORBIDDEN:
                return context.getString(R.string.err_forbidden, detail);
            case RATE_LIMIT:
                return context.getString(R.string.err_rate_limit);
            case REJECTED:
                return context.getString(R.string.err_rejected, detail);
            case NOT_FOUND:
                return context.getString(R.string.err_not_found);
            case SERVER:
                return context.getString(R.string.err_server, httpCode);
            case SIMULATED:
                return context.getString(R.string.err_simulated);
            default:
                return context.getString(R.string.err_unknown, detail);
        }
    }
}
