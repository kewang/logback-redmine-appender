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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class RedmineAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private static final String DEFAULT_TITLE = "Logback Redmine Appender";
    private static final boolean DEFAULT_ONLY_ERROR = true;
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static String GIT_BASE_URL_FORMAT;

    private LayoutWrappingEncoder<ILoggingEvent> encoder;
    private Layout<ILoggingEvent> layout;
    private String url;
    private String apiKey;
    private int projectId = -1;
    private String title = DEFAULT_TITLE;
    private boolean onlyError = DEFAULT_ONLY_ERROR;
    private String gitRepoUrl;
    private String gitCommit;
    private String gitParentDir;
    private boolean gitSupport = false;
    private MessageDigest md;
    private RedmineManager redmineManager;
    private IssueManager issueManager;
    private HashMap<String, Integer> maps;   // <stacktrace hash, issue_id>, like <0d612fc42ec7b7f02cb5affcbb20d531, 25>
    private SimpleDateFormat dateFormat = DEFAULT_DATE_FORMAT;

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

            checkGitIsSupported();

            transformDateFormat();

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

    private void checkGitIsSupported() {
        gitSupport = ((gitRepoUrl != null && gitRepoUrl.length() != 0) && (gitCommit != null && gitCommit.length() != 0)
                && (gitParentDir != null && gitParentDir.length() != 0));

        if (gitSupport) {
            gitRepoUrl = gitRepoUrl.toLowerCase();

            if (gitRepoUrl.indexOf("github") != -1) {
                // https://{repoUrl}/blob/{commit}/{parentDir}/%1$s#L%2$d

                GIT_BASE_URL_FORMAT = "* <a href='" + gitRepoUrl + "/blob/" + gitCommit + "/" + gitParentDir + "/%1$s#L%3$d'>%1$s#L%3$d</a>";
            } else if (gitRepoUrl.indexOf("bitbucket") != -1) {
                // https://{repoUrl}/src/{commit}/{parentDir}/%1$s?fileviewer=file-view-default#%2$s-%3$d"

                GIT_BASE_URL_FORMAT = "* <a href='" + gitRepoUrl + "/src/" + gitCommit + "/" + gitParentDir + "/%1$s?fileviewer=file-view-default#%2$s-%3$d'>%1$s#L%3$d</a>";
            } else if (gitRepoUrl.equalsIgnoreCase("gitlab")) {

            }
        }
    }

    private void transformDateFormat() {
        if (encoder instanceof PatternLayoutEncoder) {
            String encoderPattern = ((PatternLayoutEncoder) encoder).getPattern();
            int startPos = encoderPattern.indexOf("%d{");

            // not found date pattern
            if (startPos == -1) {
                return;
            }

            int endPos = encoderPattern.indexOf("}", startPos);
            String pattern = encoderPattern.substring(startPos + 3, endPos);

            // date pattern is empty
            if (pattern.trim().length() == 0) {
                return;
            }

            dateFormat = new SimpleDateFormat(pattern);
        }
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
            appendToOldIssue(issueId, event.getTimeStamp());
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
        Issue issue = IssueFactory.create(projectId, title + " - " + dateFormat.format(new Date(event.getTimeStamp())));

        String description = transformDescription(event);

        if (gitSupport) {
            String gitNavigation = showGitNavigation(event);

            issue.setDescription(gitNavigation + description);
        } else {
            issue.setDescription(description);
        }

        issue = issueManager.createIssue(issue);

        maps.put(hash, issue.getId());
    }

    private String showGitNavigation(ILoggingEvent event) {
        IThrowableProxy throwable = event.getThrowableProxy();

        StringBuffer sb = new StringBuffer("## link\n");

        for (StackTraceElementProxy stackTrace : throwable.getStackTraceElementProxyArray()) {
            StackTraceElement elem = stackTrace.getStackTraceElement();

            sb.append(convertClassToNavigate(elem)).append("\n");
        }

        sb.append("\n");

        return sb.toString();
    }

    /**
     * Convert com.example.Test(Test.java: 15) class to tw/kewang/logback/appender/AppTest.java#L42
     */
    private String convertClassToNavigate(StackTraceElement elem) {
        String[] classNameArray = elem.getClassName().split("\\.");

        classNameArray[classNameArray.length - 1] = elem.getFileName();

        int lineNumber = elem.getLineNumber();

        String classNavigationString = convertClassToNavigate(classNameArray);

        if (lineNumber > 0) {
            return String.format(GIT_BASE_URL_FORMAT, classNavigationString, elem.getFileName(), lineNumber);
        } else {
            return "* " + classNavigationString + " (unknown source)";
        }
    }

    private String convertClassToNavigate(String[] classNameArray) {
        String s = "";

        for (String className : classNameArray) {
            s += className + "/";
        }

        return s.substring(0, s.length() - 1);
    }

    private String transformDescription(ILoggingEvent event) {
        return "## stacktrace\n```java\n" + layout.doLayout(event) + "```";
    }

    private void appendToOldIssue(int issueId, long timestamp) throws RedmineException {
        Issue issue = issueManager.getIssueById(issueId);

        issue.setNotes(dateFormat.format(new Date(timestamp)) + " happened again");

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

    public String getGitRepoUrl() {
        return gitRepoUrl;
    }

    public void setGitRepoUrl(String gitRepoUrl) {
        this.gitRepoUrl = gitRepoUrl;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public String getGitParentDir() {
        return gitParentDir;
    }

    public void setGitParentDir(String gitParentDir) {
        this.gitParentDir = gitParentDir;
    }

    public boolean hasGitSupport() {
        return gitSupport;
    }

    public void setGitSupport(boolean gitSupport) {
        this.gitSupport = gitSupport;
    }
}