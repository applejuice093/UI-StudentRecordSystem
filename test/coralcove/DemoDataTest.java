package coralcove;

import coralcove.service.DemoDataService;
import coralcove.service.LodgeService;
import coralcove.service.SettingsService;
import org.junit.Assert;
import org.junit.Test;

public class DemoDataTest extends DbFixture {
    @Test
    public void loadAndResetTouchesOnlySampleRows() {
        DemoDataService demo = new DemoDataService();
        demo.load();
        Assert.assertTrue(SettingsService.demoLoaded());
        Assert.assertEquals(30, new LodgeService().search(null, null, null, null).size());
        try {
            demo.load();
            Assert.fail("second load should be refused");
        } catch (coralcove.util.AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("already loaded"));
        }
    }

    @Test
    public void resetLeavesAnEmptyOperationalSet() {
        DemoDataService demo = new DemoDataService();
        demo.load();
        demo.reset();
        Assert.assertFalse(SettingsService.demoLoaded());
        Assert.assertEquals(0, new LodgeService().search(null, null, null, null).size());
        demo.load();
        Assert.assertEquals(30, new LodgeService().search(null, null, null, null).size());
    }
}
