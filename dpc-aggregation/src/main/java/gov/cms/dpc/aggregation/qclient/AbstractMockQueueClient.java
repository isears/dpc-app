package gov.cms.dpc.aggregation.qclient;

import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.models.JobModel;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractMockQueueClient implements JobQueue {
    public void submitJob(UUID jobId, JobModel data) {
        // TODO

    }

    public Optional<JobStatus> getJobStatus(UUID jobId) {
        // TODO
        return Optional.of(JobStatus.COMPLETED);
    }

    public Optional<JobModel> getJob(UUID jobID) {
        return Optional.empty();
    }

    public void completeJob(UUID uuid, JobStatus jobStatus, @Nullable List<ResourceType> erringTypes) {
        // TODO
    }

    public void removeJob(UUID uuid) {
        // TODO
    }

    public long queueSize() {
        // TODO
        return -1;
    }
}
