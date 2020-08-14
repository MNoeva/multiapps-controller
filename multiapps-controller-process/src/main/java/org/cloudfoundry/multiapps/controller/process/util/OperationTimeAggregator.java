package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTimeCalculator.ProcessTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class OperationTimeAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationTimeAggregator.class);

    private ProcessTimeCalculator processTimeCalculator;
    private FlowableFacade flowableFacade;

    @Inject
    public OperationTimeAggregator(FlowableFacade flowableFacade) {
        this.flowableFacade = flowableFacade;
        this.processTimeCalculator = new ProcessTimeCalculator(flowableFacade);
    }

    public void aggregateOperationTime(String correlationId) {
        List<String> historicSubProcesses = flowableFacade.getHistoricSubProcessIds(correlationId);
        historicSubProcesses.add(correlationId);

        Map<String, ProcessTime> processTimesForSubProcesses = historicSubProcesses.stream()
                                                                                   .collect(Collectors.toMap(processId -> processId,
                                                                                                             processTimeCalculator::calculate));
        processTimesForSubProcesses.forEach((key, value) -> logProcessTimeIndividually(value, correlationId, key));

        ProcessTime rootProcessTime = processTimesForSubProcesses.get(correlationId);
        logOverallProcesstime(correlationId, rootProcessTime, processTimesForSubProcesses.values());
    }

    private void logOverallProcesstime(String correlationId, ProcessTime rootProcessTime,
                                       Collection<ProcessTime> subProcessesProcessTimes) {
        long overallDelayBetweenSteps = subProcessesProcessTimes.stream()
                                                                .mapToLong(ProcessTime::getDelayBetweenSteps)
                                                                .sum();
        ProcessTime overallProcessTime = ImmutableProcessTime.copyOf(rootProcessTime)
                                                             .withDelayBetweenSteps(rootProcessTime.getDelayBetweenSteps()
                                                                 + overallDelayBetweenSteps);

        logOverallProcessTime(overallProcessTime, correlationId);
    }

    private void logProcessTimeIndividually(ProcessTime processTime, String correlationId, String processInstanceId) {
        LOGGER.debug(MessageFormat.format("Process time for operation with id \"{0}\", process instance with id \"{1}\", process duration \"{2}\"ms, delay between steps \"{3}\"ms",
                                          correlationId, processInstanceId, processTime.getProcessDuration(),
                                          processTime.getDelayBetweenSteps()));
    }

    private void logOverallProcessTime(ProcessTime processTime, String correlationId) {
        LOGGER.info(MessageFormat.format("Process time for operation with id \"{0}\", operation duration \"{1}\"ms, delay between steps \"{2}\"ms",
                                         correlationId, processTime.getProcessDuration(), processTime.getDelayBetweenSteps()));
    }

}
