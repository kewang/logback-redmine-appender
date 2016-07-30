package tw.kewang.logback.appender;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppTest {
    private static final Logger LOG = LoggerFactory.getLogger(AppTest.class);

    @Test
    public void testApp() {
        String abc = null;

        for (int i = 0; i < 3; i++) {
            try {
                if (abc.length() == 0) {
                    LOG.info("No String");
                }
            } catch (Exception e) {
                LOG.error("Caught Exception: ", e);
                LOG.info("You can't see me.");
            }
        }

        for (int i = 0; i < 3; i++) {
            try {
                if (abc.length() == 0) {
                    LOG.info("No String");
                }
            } catch (Exception e) {
                LOG.error("Caught Exception: ", e);
                LOG.info("You can't see me.");
            }
        }
    }

    @Test
    public void testLinkStackTrace() {
        String abc = null;

        try {
            if (abc.length() == 0) {
                LOG.info("No String");
            }
        } catch (Exception e) {
            LOG.error("Caught Exception: ", e);
            LOG.info("You can't see me.");
        }
    }
}
