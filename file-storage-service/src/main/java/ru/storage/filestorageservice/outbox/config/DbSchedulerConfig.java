package ru.storage.filestorageservice.outbox.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import ru.storage.filestorageservice.outbox.entity.DeleteEventEntity;
import ru.storage.filestorageservice.outbox.enums.DeleteEventStatus;
import ru.storage.filestorageservice.outbox.repository.DeleteEventRepository;
import ru.storage.filestorageservice.outbox.service.DeleteEventService;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Configuration for db-scheduler – a cluster‑friendly persistent task scheduler.
 * <p>
 * The scheduler is used to process outbox deletion events asynchronously:
 * <ul>
 *   <li>A {@link RecurringTask} (dispatcher) periodically fetches a batch of
 *       {@code PENDING} events from the {@code delete_event} table.</li>
 *   <li>For each event, a {@link OneTimeTask} is scheduled to perform the actual
 *       deletion of the corresponding file in MinIO.</li>
 *   <li>The scheduler uses {@code SELECT FOR UPDATE SKIP LOCKED} to guarantee
 *       that each task is executed by at most one instance in a cluster.</li>
 * </ul>
 * </p>
 *
 * @see OneTimeTask
 * @see RecurringTask
 * @see Scheduler
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DbSchedulerConfig {

    private final DeleteEventRepository repository;
    /**
     * Interval (milliseconds) between runs of the dispatcher recurring task.
     */
    private @Value("${outbox.delete.interval-ms}") long intervalMs;
    /**
     * Number of worker threads the scheduler may use to execute tasks in parallel.
     */
    private @Value("${outbox.delete.threads:0}") int configuredThreads;
    /**
     * Maximum number of pending events fetched in one batch by the dispatcher.
     */
    private @Value("${outbox.delete.batch-size}") int batchSize;
    /**
     * How often the scheduler polls the database for due tasks (seconds).
     */
    private @Value("${outbox.delete.polling-interval-seconds}") long pollingIntervalSeconds;

    /**
     * Defines the one‑time task that deletes a single file.
     * <p>
     * The task expects a {@link UUID} as its data – the identifier of the
     * corresponding {@link DeleteEventEntity}. When executed, it calls
     * {@link DeleteEventService#deleteByEventId(UUID)} and handles retries
     * via Spring Retry.
     * </p>
     *
     * @param deleteEventService the service that performs the actual deletion logic
     * @return a configured one‑time task
     */
    @Bean
    public OneTimeTask<UUID> deleteObjectTask(DeleteEventService deleteEventService) {
        log.info("OneTimeTask: deleteByEventId-file-task started");
        return Tasks.oneTime("deleteByEventId-file-task", UUID.class)
            .execute((taskInstance, ctx) -> {
                UUID eventId = taskInstance.getData();
                if (eventId == null) {
                    throw new IllegalArgumentException("Event ID is missing");
                }
                deleteEventService.deleteByEventId(eventId);
            });
    }

    /**
     * Defines the recurring dispatcher task.
     * <p>
     * Every {@code intervalMs} milliseconds, this task fetches a batch of
     * {@code PENDING} events (size = {@code batchSize}) and schedules a
     * one‑time deletion task for each of them with immediate execution.
     * </p>
     *
     * @param scheduler        the db-scheduler instance (injected with {@code @Lazy}
     *                         to avoid circular dependency)
     * @param deleteObjectTask the one‑time task bean used for each deletion
     * @return a configured recurring task
     */
    @Bean
    RecurringTask<Void> deleteObjectDispatcher(@Lazy Scheduler scheduler, OneTimeTask<UUID> deleteObjectTask) {
        return Tasks.recurring("deleteByEventId-object-task", FixedDelay.ofMillis(intervalMs))
            .execute((inst, ctx) -> {
                log.info("Dispatcher is running...");
                List<DeleteEventEntity> pending =
                    repository.findBatchForProcessing(DeleteEventStatus.PENDING.name(), batchSize);
                log.info("Found {} pending events", pending.size());
                if (pending.isEmpty()) return;

                pending.forEach(event -> {
                    log.info("Scheduling deletion for event {}", event.getId());

                    scheduler.schedule(
                        deleteObjectTask.instance(event.getId().toString(), event.getId()),
                        Instant.now());
                });
            });
    }

    /**
     * Creates and starts the db-scheduler instance.
     * <p>
     * The scheduler is backed by the application's {@link DataSource} and uses
     * the table {@code scheduled_tasks} for persistence and coordination.
     * The provided recurring dispatcher task is registered on startup.
     * </p>
     *
     * @param dataSource         the data source used by the scheduler
     * @param dispatcherTask   the recurring task to be started automatically
     * @return a fully configured, started scheduler
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    Scheduler scheduler(
        DataSource dataSource,
        RecurringTask<Void> dispatcherTask,
        OneTimeTask<UUID> deleteObjectTask
    ) {
        int threads = getCountThreads();

        return Scheduler
            .create(dataSource, dispatcherTask, deleteObjectTask)
            .threads(threads)
            .pollingInterval(Duration.ofSeconds(pollingIntervalSeconds))
            .build();
    }

    private int getCountThreads() {
        return configuredThreads > 0 ? configuredThreads : Runtime.getRuntime().availableProcessors() * 4;
    }
}
