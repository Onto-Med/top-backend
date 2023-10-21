package care.smith.top.backend.model.conceptgraphs;

import care.smith.top.model.ConceptGraphProcess;
import care.smith.top.model.ConceptGraphProcessFinishedSteps;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessOverviewEntity {
  private Process[] processes;

  public Process[] getProcesses() {
    return processes;
  }

  public void setProcesses(Process[] processes) {
    this.processes = processes;
  }

  public List<ConceptGraphProcess> toApiModel() {
    return Arrays.stream(getProcesses()).map(Process::toApiModel).collect(Collectors.toList());
  }
}

class Process {
  private String name;
  private ProcessStep[] finished_steps;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ProcessStep[] getFinished_steps() {
    return finished_steps;
  }

  public void setFinished_steps(ProcessStep[] finished_steps) {
    this.finished_steps = finished_steps;
  }

  public ConceptGraphProcess toApiModel() {
    ConceptGraphProcess process = new ConceptGraphProcess();
    process.setName(getName());
    for (ProcessStep processStep : getFinished_steps()){
      ConceptGraphProcessFinishedSteps finishedSteps = new ConceptGraphProcessFinishedSteps();
      finishedSteps.setName(processStep.getName());
      finishedSteps.setRank(processStep.getRank());
      process.addFinishedStepsItem(finishedSteps);
    }
    return process;
  }
}


class ProcessStep {
  private int rank;
  private String name;

  public int getRank() {
    return rank;
  }

  public void setRank(int rank) {
    this.rank = rank;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}