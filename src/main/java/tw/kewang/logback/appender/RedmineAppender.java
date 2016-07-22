package tw.kewang.logback.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.taskadapter.redmineapi.IssueManager;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueFactory;

public class RedmineAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private static final String DEFAULT_TITLE = "Logback Redmine Appender";
    private static final boolean DEFAULT_ONLY_ERROR = true;

    private LayoutWrappingEncoder<ILoggingEvent> encoder;
    private Layout<ILoggingEvent> layout;
    private String url;
    private String apiKey;
    private int projectId = -1;
    private String title = DEFAULT_TITLE;
    private boolean onlyError = DEFAULT_ONLY_ERROR;
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

            layout = encoder.getLayout();
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
        if (event.getLevel() != Level.ERROR && onlyError == true) {
            return;
        }

//        createIssue(event);
        showEvent(event);
    }

    private void showEvent(ILoggingEvent event) {
        IThrowableProxy throwable = event.getThrowableProxy();

        for (StackTraceElementProxy stackTrace : throwable.getStackTraceElementProxyArray()) {
            StackTraceElement elem = stackTrace.getStackTraceElement();

            System.out.println("------START");

            convertClassToNavigate(elem);

            System.out.println("------END");
        }
    }

    /**
     * Convert com.example.Test(Test.java: 15) class to com/example/Test.java#L15
     */
    private String convertClassToNavigate(StackTraceElement elem) {
        String[] classNameArray = elem.getClassName().split("\\.");

        classNameArray[classNameArray.length - 1] = elem.getFileName();

        String result = "";

        for (String className : classNameArray) {
            result += className + "/";
        }

        result = result.substring(0, result.length() - 1);

        result += "#L" + elem.getLineNumber();

        return result;
    }

    private void createIssue(ILoggingEvent event) {
        Issue issue = IssueFactory.create(projectId, title + " - " + event.getTimeStamp());

        issue.setDescription(layout.doLayout(event));

        try {
            issueManager.createIssue(issue);
        } catch (RedmineException e) {
            addError("Exception", e);
        }
    }

    public LayoutWrappingEncoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    public void setEncoder(LayoutWrappingEncoder<ILoggingEvent> encoder) {
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

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public boolean isOnlyError() {
        return onlyError;
    }

    public void setOnlyError(boolean onlyError) {
        this.onlyError = onlyError;
    }
}