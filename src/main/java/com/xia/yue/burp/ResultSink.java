package com.xia.yue.burp;

public interface ResultSink {
    void accept(ScanResult result);

    void error(String message, Throwable throwable);
}
