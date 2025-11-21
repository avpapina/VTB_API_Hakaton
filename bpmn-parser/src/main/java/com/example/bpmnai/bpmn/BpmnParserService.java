package com.example.bpmnai.bpmn;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Documentation;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.bpmnai.core.domain.BpmnGateway;
import com.example.bpmnai.core.domain.BpmnProcess;
import com.example.bpmnai.core.domain.BpmnSequenceFlow;
import com.example.bpmnai.core.domain.BpmnTask;

@Service
public class BpmnParserService {

    public void printAnalysisToConsole(BpmnProcess process) {
        System.out.println("=== BPMN Analysis ===");
        System.out.println("Process: " + process.getName() + " (id=" + process.getId() + ")");
        System.out.println("Tasks:");
        process.getTasks().forEach(t -> {
            System.out.println("  - " + t.getName() + " (id=" + t.getId() + ")");
        });
        System.out.println("Gateways:");
        process.getGateways().forEach(g -> {
            System.out.println("  - " + g.getName() + " (id=" + g.getId() + ")");
        });
        System.out.println("Sequence Flows:");
        process.getSequenceFlows().forEach(f -> {
            System.out.println("  - " + f.getSourceRef() + " -> " + f.getTargetRef());
        });
    }

    public BpmnProcess parseBpmnFile(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BpmnModelInstance model = Bpmn.readModelFromStream(inputStream);
            return analyzeBpmn(model);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга BPMN файла: " + e.getMessage(), e);
        }
    }

    private BpmnProcess analyzeBpmn(BpmnModelInstance model) {
        BpmnProcess result = new BpmnProcess();

        // process metadata
        org.camunda.bpm.model.bpmn.instance.Process proc =
                model.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.Process.class)
                        .stream().findFirst().orElse(null);

        if (proc != null) {
            result.setId(proc.getId());
            result.setName(proc.getName() != null ? proc.getName() : proc.getId());
        }

        // tasks (любой Task, включая обычные <bpmn:task>)
        List<BpmnTask> tasks = new ArrayList<>();
        for (Task t : model.getModelElementsByType(Task.class)) {
            BpmnTask bpmnTask = new BpmnTask();
            bpmnTask.setId(t.getId());
            bpmnTask.setName(t.getName() != null ? t.getName() : t.getId());
            bpmnTask.setType("task");

            // description из documentation
            if (t.getDocumentations() != null) {
                for (Documentation doc : t.getDocumentations()) {
                    bpmnTask.setDescription(doc.getTextContent().trim());
                }
            }

            tasks.add(bpmnTask);
        }
        result.setTasks(tasks);

        // gateways
        List<BpmnGateway> gateways = new ArrayList<>();
        for (ExclusiveGateway gw : model.getModelElementsByType(ExclusiveGateway.class)) {
            BpmnGateway g = new BpmnGateway();
            g.setId(gw.getId());
            g.setName(gw.getName() != null ? gw.getName() : gw.getId());
            g.setType("exclusiveGateway");
            gateways.add(g);
        }
        result.setGateways(gateways);

        // sequence flows
        List<BpmnSequenceFlow> bpmnFlows = new ArrayList<>();
        for (SequenceFlow flow : model.getModelElementsByType(SequenceFlow.class)) {
            BpmnSequenceFlow bf = new BpmnSequenceFlow();
            bf.setId(flow.getId());
            bf.setName(flow.getName() != null ? flow.getName() : flow.getId());
            bf.setSourceRef(flow.getSource().getId());
            bf.setTargetRef(flow.getTarget().getId());
            bpmnFlows.add(bf);
        }
        result.setSequenceFlows(bpmnFlows);

        return result;
    }
}
