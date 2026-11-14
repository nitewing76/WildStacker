package com.bgsoftware.wildstacker.database.sql;

public final class QueryResult<T> {

    public interface ThrowableConsumer<T> {
        void accept(T value) throws Exception;
    }

    private ThrowableConsumer<T> onSuccess;
    private ThrowableConsumer<Throwable> onFail;

    public QueryResult<T> onSuccess(ThrowableConsumer<T> onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public QueryResult<T> onFail(ThrowableConsumer<Throwable> onFail) {
        this.onFail = onFail;
        return this;
    }

    public void complete(T value) throws Exception {
        if (this.onSuccess != null) {
            this.onSuccess.accept(value);
        }
    }

    public void fail(Throwable error) {
        if (this.onFail != null) {
            try {
                this.onFail.accept(error);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (error != null) {
            error.printStackTrace();
        }
    }

}
