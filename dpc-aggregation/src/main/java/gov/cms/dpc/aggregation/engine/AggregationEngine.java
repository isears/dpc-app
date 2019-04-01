package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.typesafe.config.Config;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import gov.cms.dpc.common.models.JobModel;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.transformer.RetryTransformer;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AggregationEngine implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);
    private static final char DELIM = '\n';

    private final JobQueue queue;
    private final BlueButtonClient bbclient;
    private final FhirContext context;
    private final String exportPath;
    private volatile boolean run = true;
    private Disposable subscribe;

    @Inject
    public AggregationEngine(BlueButtonClient bbclient, JobQueue queue, Config config) {
        this.queue = queue;
        this.bbclient = bbclient;
        this.context = FhirContext.forDstu3();
        this.exportPath = config.getString("exportPath");
    }

    @Override
    public void run() {
        logger.info("Starting to poll for aggregation jobs");
        this.pollQueue();
    }

    public void stop() {
        this.run = false;
        this.subscribe.dispose();
    }

    private void pollQueue() {

        subscribe = Observable.fromCallable(this.queue::workJob)
                .doOnNext(job -> logger.warn("Awaiting job on {}", Thread.currentThread().getName()))
                .filter(Optional::isPresent)
                .repeatWhen(completed -> {
                    logger.warn("No job, retrying in a bit");
                    return completed.delay(2, TimeUnit.SECONDS);
                })
                .map(Optional::get)
                .doOnError(error -> logger.error("Error on things: ", error))
                .subscribe(workPair -> {
                    final JobModel model = (JobModel) workPair.getRight();
                    final UUID jobID = workPair.getLeft();
                    logger.debug("Has job {}. Working.", jobID);
                    List<String> attributedBeneficiaries = model.getPatients();

                    if (!attributedBeneficiaries.isEmpty()) {
                        logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.size());
                        try {
                            this.workJob(jobID, model);
                            this.queue.completeJob(jobID, JobStatus.COMPLETED);
                        } catch (Exception e) {
                            logger.error("Cannot process job {}", jobID, e);
                            this.queue.completeJob(jobID, JobStatus.FAILED);
                        }
                    } else {
                        logger.error("Cannot execute Job {} with no beneficiaries", jobID);
                        this.queue.completeJob(jobID, JobStatus.FAILED);
                    }
                });


//        final Optional<Pair<UUID, Object>> workPair = this.queue.workJob();
//        if (workPair.isEmpty()) {
//            try {
//                logger.debug("No job, waiting 2 seconds");
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        } else {
//            final JobModel model = (JobModel) workPair.get().getRight();
//            final UUID jobID = workPair.get().getLeft();
//            logger.debug("Has job {}. Working.", jobID);
//            List<String> attributedBeneficiaries = model.getPatients();
//
//            if (!attributedBeneficiaries.isEmpty()) {
//                logger.debug("Has {} attributed beneficiaries", attributedBeneficiaries.size());
//                try {
//                    this.workJob(jobID, model);
//                    this.queue.completeJob(jobID, JobStatus.COMPLETED);
//                } catch (Exception e) {
//                    logger.error("Cannot process job {}", jobID, e);
//                    this.queue.completeJob(jobID, JobStatus.FAILED);
//                }
//            } else {
//                logger.error("Cannot execute Job {} with no beneficiaries", jobID);
//                this.queue.completeJob(jobID, JobStatus.FAILED);
//            }
//        }
    }

    void workJob(UUID jobID, JobModel job) throws IOException {
        final IParser parser = context.newJsonParser();
        try (final FileOutputStream writer = new FileOutputStream(String.format("%s/%s.ndjson", exportPath, jobID.toString()))) {

            // Map over the patients and fetch them using a separate thread pool
            Observable.fromIterable(job.getPatients())
                    .flatMap((patient) -> this.handlePatient(patient, parser))
                    .subscribeOn(Schedulers.io())
                    .doOnError(e -> logger.error("Error: ", e))
                    .blockingSubscribe(str -> {
                        try {
                            logger.debug("Writing {} to file on thread {}", str, Thread.currentThread().getName());
                            writer.write(str.getBytes(StandardCharsets.UTF_8));
                            writer.write(DELIM);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            writer.flush();
        }
    }

    private Observable<String> handlePatient(String patient, IParser parser) {
        // Create retry handler
        RetryConfig config = RetryConfig.ofDefaults();
        Retry retry = Retry.of("testName", config);
        RetryTransformer<Patient> retryTransformer = RetryTransformer.of(retry);

        return Observable.fromCallable(() -> this.bbclient.requestPatientFromServer(patient))
                .compose(retryTransformer)
                .doOnNext(p -> logger.debug("Fetching {} on {}", p, Thread.currentThread().getName()))
                .map(parser::encodeResourceToString);

    }
}
