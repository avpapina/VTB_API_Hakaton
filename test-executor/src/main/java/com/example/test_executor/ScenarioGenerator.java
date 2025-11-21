package com.example.test_executor;

import java.util.ArrayList;
import java.util.List;

import com.example.bpmnai.core.domain.TestScenario;
import com.example.bpmnai.core.domain.TestStatus;
import com.example.bpmnai.core.domain.TestStep;

public class ScenarioGenerator {

    public static TestScenario generateScenarioFromMatchedTasks(List<MatchedTask> matchedTasks) {
        TestScenario scenario = new TestScenario();
        scenario.setId("scenario_" + System.currentTimeMillis());
        scenario.setName("Auto-generated scenario");
        scenario.setDescription("Scenario generated from matched BPMN/OpenAPI tasks");
        scenario.setStatus(TestStatus.CREATED);

        List<TestStep> steps = new ArrayList<>();
        for (MatchedTask task : matchedTasks) {
            TestStep step = new TestStep();
            step.setId(task.getBpmnTaskId() != null ? task.getBpmnTaskId() : task.getProcessId() + "_" + task.getTaskName());
            step.setName(task.getBpmnTaskName() != null ? task.getBpmnTaskName() : task.getTaskName());
            step.setUrl(task.getOpenApiEndpoint() != null ? task.getOpenApiEndpoint() : task.getEndpointUrl());
            step.setMethod(task.getHttpMethod());
            step.setStatus(TestStatus.CREATED);
            steps.add(step);
        }
        scenario.setSteps(steps);

        return scenario;
    }
}