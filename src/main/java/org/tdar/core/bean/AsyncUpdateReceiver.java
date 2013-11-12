package org.tdar.core.bean;

import java.util.ArrayList;
import java.util.List;

import org.tdar.utils.Pair;

/*
 * This interface governs the interactions between asynchronous tasks. It's designed to enable basic communication 
 * between the caller and the processor. It allows for status, completion, and errors to be passed back and forth, 
 * finally, the "details" can be used to pass record specific info to be shared.
 */
public interface AsyncUpdateReceiver {

    final static AsyncUpdateReceiver DEFAULT_RECEIVER = new DefaultReceiver();

    void setPercentComplete(float complete);

    void setStatus(String status);

    void setDetails(List<Pair<Long, String>> details);

    void addDetail(Pair<Long, String> detail);

    String getAsyncErrors();

    String getHtmlAsyncErrors();

    List<Pair<Long, String>> getDetails();

    float getPercentComplete();

    String getStatus();

    void addError(Throwable t);

    void setCompleted();

    public class DefaultReceiver implements AsyncUpdateReceiver {
        private float percentComplete;
        private String status = "initialized";
        private List<Throwable> throwables = new ArrayList<Throwable>();
        private List<Pair<Long, String>> details = new ArrayList<Pair<Long, String>>();

        @Override
        public float getPercentComplete() {
            return percentComplete;
        }

        @Override
        public void setPercentComplete(float percentComplete) {
            this.percentComplete = percentComplete;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public void setStatus(String status) {
            this.status = status;
        }

        public List<Throwable> getThrowables() {
            return throwables;
        }

        @Override
        public void addError(Throwable t) {
            throwables.add(t);
        }

        @Override
        public void setDetails(List<Pair<Long, String>> details) {
            this.details = details;
        }

        @Override
        public List<Pair<Long, String>> getDetails() {
            return details;
        }

        @Override
        public void addDetail(Pair<Long, String> detail) {
            getDetails().add(detail);
        }

        @Override
        public String getAsyncErrors() {
            StringBuilder sb = new StringBuilder();
            for (Throwable throwable : getThrowables()) {
                sb.append(throwable.getLocalizedMessage());
            }
            return sb.toString();
        }

        @Override
        public String getHtmlAsyncErrors() {
            StringBuilder sb = new StringBuilder();
            for (Throwable throwable : getThrowables()) {
                sb.append("<li>").append(throwable.getLocalizedMessage()).append("</li>");
            }
            return sb.toString();
        }

        @Override
        public void update(float percent, String status) {
            setStatus(status);
            setPercentComplete(percent);
        }

        @Override
        public void setCompleted() {
            setPercentComplete(100f);
            setStatus("Complete");
        }

    }

    public void update(float percent, String status);
}
