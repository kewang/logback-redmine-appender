package tw.kewang.logback.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
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

import java.security.MessageDigest;
import java.util.HashMap;

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
    private MessageDigest md;
    private RedmineManager redmineManager;
    private IssueManager issueManager;
    private HashMap<String, Integer> maps;   // <stacktrace hash, issue_id>, like <0d612fc42ec7b7f02cb5affcbb20d531, 25>

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

            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            addError("Exception", e);
        }

        redmineManager = RedmineManagerFactory.createWithApiKey(url, apiKey);
        issueManager = redmineManager.getIssueManager();
        maps = new HashMap<String, Integer>();

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

        try {
            createIssue(event);
        } catch (RedmineException e) {
            addError("Exception", e);
        }
    }

    private void createIssue(ILoggingEvent event) throws RedmineException {
        String hash = convertStackTracesToHash(event);

        Integer issueId = maps.get(hash);

        if (issueId == null) {
            createNewIssue(event, hash);
        } else {
            appendToOldIssue(issueId);
        }
    }

    private String convertStackTracesToHash(ILoggingEvent event) {
        IThrowableProxy throwable = event.getThrowableProxy();

        StringBuffer sb = new StringBuffer();

        for (StackTraceElementProxy stackTrace : throwable.getStackTraceElementProxyArray()) {
            StackTraceElement elem = stackTrace.getStackTraceElement();

            sb.append(elem.toString());
        }

        return hash(sb.toString());
    }

    private String hash(String source) {
        md.update(source.getBytes());

        byte byteData[] = md.digest();

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    private void createNewIssue(ILoggingEvent event, String hash) throws RedmineException {
        Issue issue = IssueFactory.create(projectId, title + " - " + event.getTimeStamp());

        issue.setDescription(layout.doLayout(event));

        issue = issueManager.createIssue(issue);

        maps.put(hash, issue.getId());
    }

    private void appendToOldIssue(int issueId) throws RedmineException {
        Issue issue = issueManager.getIssueById(issueId);

        issue.setNotes("TIME: " + System.currentTimeMillis());

        issueManager.update(issue);
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