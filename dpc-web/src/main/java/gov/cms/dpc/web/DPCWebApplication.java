package gov.cms.dpc.web;

import ca.mestevens.java.configuration.bundle.TypesafeConfigurationBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.fhir.FHIRModule;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClientModule;
import gov.cms.dpc.queue.JobQueueModule;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DPCWebApplication extends Application<DPWebConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DPCWebApplication().run(args);
    }

    @Override
    public String getName() {
        return "DPC Web API";
    }

    @Override
    public void initialize(final Bootstrap<DPWebConfiguration> bootstrap) {
        // This is required for Guice to load correctly. Not entirely sure why
        // https://github.com/dropwizard/dropwizard/issues/1772
        JerseyGuiceUtils.reset();
        GuiceBundle<DPWebConfiguration> guiceBundle = GuiceBundle.defaultBuilder(DPWebConfiguration.class)
                .modules(new DPCAppModule(), new JobQueueModule(), new FHIRModule(), new BlueButtonClientModule())
                .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new TypesafeConfigurationBundle());
    }

    @Override
    public void run(final DPWebConfiguration configuration,
                    final Environment environment) {
        // Not used yet
    }
}
