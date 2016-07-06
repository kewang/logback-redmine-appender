package tw.kewang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            for (int i = 0; i < 2; i++) {
                LOG.info("Hello World! + {}", i);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }
}