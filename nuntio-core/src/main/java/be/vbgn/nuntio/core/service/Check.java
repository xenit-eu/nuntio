package be.vbgn.nuntio.core.service;

public interface Check {

    void setFailing(String message);

    void setWarning(String message);

    void setPassing(String message);
}
