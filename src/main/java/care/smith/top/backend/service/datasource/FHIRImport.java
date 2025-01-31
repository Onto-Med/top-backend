package care.smith.top.backend.service.datasource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import care.smith.top.top_phenotypic_query.adapter.fhir.FHIRUtil;
import care.smith.top.top_phenotypic_query.util.DateUtil;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Timing.TimingRepeatComponent;

public class FHIRImport extends DataImport {

  private final Map<String, Coding> medications = new HashMap<>();
  private final Map<String, String> encounters = new HashMap<>();
  private final boolean mergeEncounters;

  public FHIRImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository) {
    this(
        dataSourceId,
        reader,
        subjectRepository,
        encounterRepository,
        subjectResourceRepository,
        false);
  }

  public FHIRImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      boolean mergeEncounters) {
    super(dataSourceId, reader, subjectRepository, encounterRepository, subjectResourceRepository);
    this.mergeEncounters = mergeEncounters;
  }

  @Override
  public void run() {
    IParser parser = FhirContext.forR4().newJsonParser();
    IBaseResource r = parser.parseResource(reader);
    preprocessResource(r);
    if (mergeEncounters) mergeEncounters();
    importResource(r);
  }

  private void mergeEncounters() {
    encounters.replaceAll((e, v) -> getRootParent(e));
  }

  private String getRootParent(String child) {
    String parent = encounters.get(child);
    if (parent == null) return child;
    return getRootParent(parent);
  }

  private String getEncounterId(Reference r) {
    String id = FHIRUtil.getId(r);
    if (mergeEncounters && encounters.get(id) != null) return encounters.get(id);
    return id;
  }

  private void preprocessResource(IBaseResource r) {
    if (r instanceof Bundle) preprocessBundle((Bundle) r);
    else if (r instanceof Medication) preprocessMedication((Medication) r);
    else if (mergeEncounters && r instanceof Encounter) preprocessEncounter((Encounter) r);
  }

  private void preprocessBundle(Bundle r) {
    for (BundleEntryComponent comp : r.getEntry()) preprocessResource(comp.getResource());
  }

  private void preprocessMedication(Medication r) {
    if (r.hasCode()) medications.put(FHIRUtil.getId(r), r.getCode().getCodingFirstRep());
  }

  private void preprocessEncounter(Encounter r) {
    Reference parent = r.getPartOf();
    if (parent != null
        && parent.getReferenceElement() != null
        && parent.getReferenceElement().getIdPart() != null)
      encounters.put(FHIRUtil.getId(r), FHIRUtil.getId(parent));
  }

  private void importResource(IBaseResource r) {
    if (r instanceof Bundle) importBundle((Bundle) r);
    else if (r instanceof Patient) importPatient((Patient) r);
    else if (r instanceof Encounter) importEncounter((Encounter) r);
    else if (r instanceof Observation) importObservation((Observation) r);
    else if (r instanceof Condition) importCondition((Condition) r);
    else if (r instanceof Procedure) importProcedure((Procedure) r);
    else if (r instanceof MedicationAdministration)
      importMedicationAdministration((MedicationAdministration) r);
    else if (r instanceof MedicationStatement) importMedicationStatement((MedicationStatement) r);
    else if (r instanceof MedicationRequest) importMedicationRequest((MedicationRequest) r);
  }

  private void importBundle(Bundle r) {
    for (BundleEntryComponent comp : r.getEntry()) importResource(comp.getResource());
  }

  private void importPatient(Patient r) {
    SubjectDao dao = new SubjectDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasBirthDate()) dao.birthDate(DateUtil.ofDate(r.getBirthDate()));
    if (r.hasGender()) dao.sex(r.getGender().toCode());
    saveSubject(dao);
  }

  private void importEncounter(Encounter r) {
    EncounterDao dao = new EncounterDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasSubject()) dao.subjectId(FHIRUtil.getId(r.getSubject()));
    if (r.hasClass_()) dao.type(r.getClass_().getCode());
    if (r.hasPeriod()) {
      Period p = r.getPeriod();
      if (p.hasStart()) dao.startDateTime(DateUtil.ofDate(p.getStart()));
      if (p.hasEnd()) dao.endDateTime(DateUtil.ofDate(p.getEnd()));
    }
    saveEncounter(dao);
  }

  private void importObservation(Observation r) {
    SubjectResourceDao dao = new SubjectResourceDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasSubject()) dao.subjectId(FHIRUtil.getId(r.getSubject()));
    if (r.hasEncounter()) dao.encounterId(getEncounterId(r.getEncounter()));
    if (r.hasCode()) setCode(dao, r.getCode().getCodingFirstRep());

    if (r.hasEffectiveDateTimeType())
      dao.dateTime(DateUtil.ofDate(r.getEffectiveDateTimeType().getValue()));
    else if (r.hasEffectiveInstantType())
      dao.dateTime(DateUtil.ofDate(r.getEffectiveInstantType().getValue()));
    else if (r.hasEffectivePeriod()) setPeriod(dao, r.getEffectivePeriod());

    if (r.hasValueBooleanType()) dao.booleanValue(r.getValueBooleanType().getValue());
    else if (r.hasValueCodeableConcept()) {
      Coding c = r.getValueCodeableConcept().getCodingFirstRep();
      dao.textValue(c.getSystem() + "|" + c.getCode());
    } else if (r.hasValueDateTimeType())
      dao.dateTimeValue(DateUtil.ofDate(r.getValueDateTimeType().getValue()));
    else if (r.hasValueIntegerType())
      dao.numberValue(BigDecimal.valueOf(r.getValueIntegerType().getValue()));
    else if (r.hasValueQuantity()) {
      Quantity q = r.getValueQuantity();
      if (q.hasUnit()) dao.unit(q.getUnit());
      if (q.hasValue()) dao.numberValue(q.getValue());
    } else if (r.hasValueStringType()) dao.textValue(r.getValueStringType().asStringValue());

    saveSubjectResource(dao);
  }

  private void importCondition(Condition r) {
    SubjectResourceDao dao = new SubjectResourceDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasSubject()) dao.subjectId(FHIRUtil.getId(r.getSubject()));
    if (r.hasEncounter()) dao.encounterId(getEncounterId(r.getEncounter()));
    if (r.hasCode()) setCode(dao, r.getCode().getCodingFirstRep());

    if (r.hasOnsetDateTimeType())
      dao.dateTime(DateUtil.ofDate(r.getOnsetDateTimeType().getValue()));
    else if (r.hasOnsetPeriod()) setPeriod(dao, r.getOnsetPeriod());
    else if (r.hasRecordedDate()) dao.dateTime(DateUtil.ofDate(r.getRecordedDate()));

    dao.booleanValue(true);

    saveSubjectResource(dao);
  }

  private void importProcedure(Procedure r) {
    SubjectResourceDao dao = new SubjectResourceDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasSubject()) dao.subjectId(FHIRUtil.getId(r.getSubject()));
    if (r.hasEncounter()) dao.encounterId(getEncounterId(r.getEncounter()));
    if (r.hasCode()) setCode(dao, r.getCode().getCodingFirstRep());

    if (r.hasPerformedDateTimeType())
      dao.dateTime(DateUtil.ofDate(r.getPerformedDateTimeType().getValue()));
    else if (r.hasPerformedPeriod()) setPeriod(dao, r.getPerformedPeriod());

    dao.booleanValue(true);

    saveSubjectResource(dao);
  }

  private void importMedicationAdministration(MedicationAdministration r) {
    SubjectResourceDao dao = new SubjectResourceDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasSubject()) dao.subjectId(FHIRUtil.getId(r.getSubject()));
    if (r.hasContext()) dao.encounterId(getEncounterId(r.getContext()));

    if (r.hasEffectiveDateTimeType())
      dao.dateTime(DateUtil.ofDate(r.getEffectiveDateTimeType().getValue()));
    else if (r.hasEffectivePeriod()) setPeriod(dao, r.getEffectivePeriod());

    dao.booleanValue(true);

    if (r.hasMedicationCodeableConcept()) {
      setCode(dao, r.getMedicationCodeableConcept().getCodingFirstRep());
      saveSubjectResource(dao);
    } else if (r.hasMedicationReference()) {
      String medicationId = FHIRUtil.getId(r.getMedicationReference());
      Coding c = medications.get(medicationId);
      if (c != null) {
        setCode(dao, c);
        saveSubjectResource(dao);
      }
    }
  }

  private void importMedicationStatement(MedicationStatement r) {
    SubjectResourceDao dao = new SubjectResourceDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasSubject()) dao.subjectId(FHIRUtil.getId(r.getSubject()));
    if (r.hasContext()) dao.encounterId(getEncounterId(r.getContext()));

    if (r.hasEffectiveDateTimeType())
      dao.dateTime(DateUtil.ofDate(r.getEffectiveDateTimeType().getValue()));
    else if (r.hasEffectivePeriod()) setPeriod(dao, r.getEffectivePeriod());

    dao.booleanValue(true);

    if (r.hasMedicationCodeableConcept()) {
      setCode(dao, r.getMedicationCodeableConcept().getCodingFirstRep());
      saveSubjectResource(dao);
    } else if (r.hasMedicationReference()) {
      String medicationId = FHIRUtil.getId(r.getMedicationReference());
      Coding c = medications.get(medicationId);
      if (c != null) {
        setCode(dao, c);
        saveSubjectResource(dao);
      }
    }
  }

  private void importMedicationRequest(MedicationRequest r) {
    SubjectResourceDao dao = new SubjectResourceDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasSubject()) dao.subjectId(FHIRUtil.getId(r.getSubject()));
    if (r.hasEncounter()) dao.encounterId(getEncounterId(r.getEncounter()));

    if (r.hasDosageInstruction()) {
      Dosage d = r.getDosageInstructionFirstRep();
      if (d.hasTiming()) {
        if (d.getTiming().hasRepeat()) {
          TimingRepeatComponent rep = d.getTiming().getRepeat();
          if (rep.hasBoundsPeriod()) setPeriod(dao, rep.getBoundsPeriod());
        } else if (d.getTiming().hasEvent())
          dao.dateTime(DateUtil.ofDate(d.getTiming().getEvent().get(0).getValue()));
      }
    } else if (r.hasAuthoredOn()) dao.dateTime(DateUtil.ofDate(r.getAuthoredOn()));

    dao.booleanValue(true);

    if (r.hasMedicationCodeableConcept()) {
      setCode(dao, r.getMedicationCodeableConcept().getCodingFirstRep());
      saveSubjectResource(dao);
    } else if (r.hasMedicationReference()) {
      String medicationId = FHIRUtil.getId(r.getMedicationReference());
      Coding c = medications.get(medicationId);
      if (c != null) {
        setCode(dao, c);
        saveSubjectResource(dao);
      }
    }
  }

  private void setCode(SubjectResourceDao dao, Coding c) {
    dao.codeSystem(c.getSystem());
    dao.code(c.getCode());
  }

  private void setPeriod(SubjectResourceDao dao, Period p) {
    if (p.hasStart()) dao.startDateTime(DateUtil.ofDate(p.getStart()));
    if (p.hasEnd()) dao.endDateTime(DateUtil.ofDate(p.getEnd()));
  }
}
