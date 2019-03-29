package gov.cms.dpc.aggregation.engine;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClientModule;
import gov.cms.dpc.aggregation.bbclient.TestModule;
import gov.cms.dpc.aggregation.qclient.MockFullQueueClient;
import gov.cms.dpc.common.models.JobModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class AggregationEngineTest {

    private AggregationEngine engine;
    @Inject
    private BlueButtonClient bbc;
    @Inject
    private Config config;

    @BeforeEach
    public void setup() {
        final Injector injector = Guice.createInjector(new TestModule(), new BlueButtonClientModule());
        injector.injectMembers(this);
        this.engine = new AggregationEngine(bbc, new MockFullQueueClient(), config);
    }

    @Test
    public void testSimpleExecution() throws IOException {
        // Create a job and try to work it
        final JobModel model = new JobModel("testProviderId", Arrays.asList("19990000002901", "19990000002902", "19990000002903", "19990000002904", "19990000002905", "19990000002906", "19990000003001", "19990000002907", "19990000003002", "19990000002908", "19990000003003", "19990000002909", "19990000003004", "19990000002910", "19990000003005", "19990000003006", "19990000003007", "19990000003008", "19990000003009", "19990000003010", "19990000002201", "19990000002202", "19990000002203", "19990000002204", "19990000002205", "19990000002206", "19990000002207", "19990000002208", "19990000002209", "19990000002210", "19990000002911", "19990000002912", "19990000002913", "19990000002914", "19990000002915", "19990000003011", "19990000002916", "19990000003012", "19990000002917", "19990000003013", "19990000002918", "19990000003014", "19990000002919", "19990000003015", "19990000002920", "19990000003016", "19990000003017", "19990000003018", "19990000003019"));
        final UUID jobID = UUID.randomUUID();
        this.engine.workJob(jobID, model);
    }
}
