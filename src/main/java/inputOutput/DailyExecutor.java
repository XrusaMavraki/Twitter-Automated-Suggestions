package inputOutput;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by xrusa on 22/2/2018.
 */
public class DailyExecutor {
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public void startExecutor() {
        scheduledExecutorService.scheduleAtFixedRate(this::doExecute, 0, 24L, TimeUnit.HOURS);
    }

    private void doExecute() {
       TwitterOutboundService service= TwitterOutboundService.getInstance();
       service.removeOldUsers();
        try {
            service.createContactedUsers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
