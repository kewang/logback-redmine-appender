package tw.kewang.logback.appender;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.taskadapter.redmineapi.IssueManager;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueFactory;

public class RedmineAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private PatternLayoutEncoder encoder;
    private String url;
    private String apiKey;
    private int projectId = -1;
    private RedmineManager redmineManager;
    private IssueManager issueManager;

    @Override
    public void start() {
        if (!checkProperty()) {
            addError("No set url / apiKey / projectId [" + name + "].");

            return;
        }

        if (encoder == null) {
            addError("No encoder set for the appender named [" + name + "].");

            return;
        }

        try {
            encoder.init(System.out);
        } catch (Exception e) {
            addError("Exception", e);
        }

        redmineManager = RedmineManagerFactory.createWithApiKey(url, apiKey);
        issueManager = redmineManager.getIssueManager();

        super.start();
    }

    private boolean checkProperty() {
        return url != null && url.length() != 0 && apiKey != null && apiKey.length() != 0 && projectId != -1;
    }

    @Override
    public void append(ILoggingEvent event) {
        createIssue(event);
    }

    private void createIssue(ILoggingEvent event) {
        Issue issue = IssueFactory.create(projectId, event.getLoggerName());

        issue.setDescription(encoder.getLayout().doLayout(event));

        try {
            issueManager.createIssue(issue);
        } catch (RedmineException e) {
            addError("Exception", e);
        }
    }

    public PatternLayoutEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getProjectId() {
        return projectId;
    }
}