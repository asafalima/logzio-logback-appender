package io.logz.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.io.File;

public class LogzioLogbackAppender extends AppenderBase<ILoggingEvent> {

    private LogzioSender logzioSender;

    // User controlled variables
    private String logzioToken;
    private String logzioType = "java";
    private int drainTimeoutSec = 5;
    private int fileSystemFullPercentThreshold = 98;
    private String bufferDir;
    private String logzioUrl = "https://listener.logz.io:8071";
    private int connectTimeout = 10 * 1000;
    private int socketTimeout = 10 * 1000;

    private boolean debug = false;

    public void setToken(String logzioToken) {
        this.logzioToken = logzioToken;
    }

    public String getToken() {
        return logzioToken;
    }

    public void setLogzioType(String logzioType) {
        this.logzioType = logzioType;
    }

    public String getLogzioType() {
        return logzioType;
    }

    public void setDrainTimeoutSec(int drainTimeoutSec) {

        // Basic protection from running negative or zero timeout
        if (drainTimeoutSec < 1) {
            this.drainTimeoutSec = 1;
            addInfo("Got unsupported drain timeout " + drainTimeoutSec + ". The timeout must be number greater then 1. I have set to 1 as fallback.");
        }
        else {
            this.drainTimeoutSec = drainTimeoutSec;
        }
    }

    public int getDrainTimeoutSec() {
        return drainTimeoutSec;
    }

    public void setFileSystemFullPercentThreshold(int fileSystemFullPercentThreshold) {
        this.fileSystemFullPercentThreshold = fileSystemFullPercentThreshold;
    }

    public int getFileSystemFullPercentThreshold() {
        return fileSystemFullPercentThreshold;
    }

    public void setBufferDir(String bufferDir) {
        this.bufferDir = bufferDir;
    }

    public String getBufferDir() {
        return bufferDir;
    }

    public void setLogzioUrl(String logzioUrl) {
        this.logzioUrl = logzioUrl;
    }

    public String getLogzioUrl() {
        return logzioUrl;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void start() {
        if (logzioToken == null) {
            addError("Logz.io Token is missing! Bailing out..");
            return;
        }

        if (!(fileSystemFullPercentThreshold > 1 && fileSystemFullPercentThreshold <= 100)) {
            if (fileSystemFullPercentThreshold != -1) {
                addError("fileSystemFullPercentThreshold should be a number between 1 and 100, or -1");
                return;
            }
        }

        if (bufferDir != null) {
            File bufferFile = new File(bufferDir);
            if (!bufferFile.mkdirs()) {
                addError("We cant write to your bufferDir location");
                return;
            }
        }
        else {
            bufferDir = System.getProperty("java.io.tmpdir") + "/logzio-logback-buffer";
        }

        try {
            StatusReporter reporter = new StatusReporter();
            logzioSender = new LogzioSender(logzioToken, logzioType, drainTimeoutSec, fileSystemFullPercentThreshold,
                                            bufferDir, logzioUrl, socketTimeout, connectTimeout, debug,
                                            reporter, context.getScheduledExecutorService());
            logzioSender.start();
        }
        catch (IllegalArgumentException e) {
            addError("Something unexpected happened while generating connection to logz.io");
            addError("Exception: " + e.getMessage());
            e.printStackTrace();
            return;  // Not signaling super as up, we have something we cant deal with.
        }

        super.start();
    }

    @Override
    public void stop() {
        logzioSender.stop();
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        logzioSender.send(loggingEvent);
    }

    public class StatusReporter {
        public void error(String msg) {
            addError(msg);
        }
        public void error(String msg, Throwable e) {
            addError(msg, e);
        }
        public void warning(String msg) {
            addWarn(msg);
        }
        public void info(String msg) {
            addInfo(msg);
        }
        public void info(String msg, Throwable e) {
            addInfo(msg, e);
        }
    }
}