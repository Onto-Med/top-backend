package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.top_document_query.util.DateUtil;
import care.smith.top.top_phenotypic_query.adapter.fhir.FHIRUtil;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
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
import org.hl7.fhir.r4.model.Timing;
import org.hl7.fhir.r4.model.Timing.TimingRepeatComponent;
import org.hl7.fhir.r4.model.codesystems.V3ActCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FHIRImportTest extends ImportTest {
  static final String DATA_SOURCE_ID = "data_source_1";
  static String FHIR;

  @BeforeAll
  static void beforeAll() {
    Patient p1 =
        ((Patient) new Patient().setId("p1"))
            .setGender(AdministrativeGender.MALE)
            .setBirthDate(DateUtil.parseToDate("1974-12-25"));

    Patient p2 =
        ((Patient) new Patient().setId("p2"))
            .setGender(AdministrativeGender.FEMALE)
            .setBirthDate(DateUtil.parseToDate("1982-01-23"));

    Coding amb =
        new Coding(V3ActCode.AMB.getSystem(), V3ActCode.AMB.toCode(), V3ActCode.AMB.getDisplay());
    Coding imp =
        new Coding(V3ActCode.IMP.getSystem(), V3ActCode.IMP.toCode(), V3ActCode.IMP.getDisplay());

    Encounter p1e1 =
        ((Encounter) new Encounter().setId("p1e1"))
            .setSubject(new Reference(p1))
            .setClass_(imp)
            .setPeriod(
                new Period()
                    .setStart(DateUtil.parseToDate("2015-02-01T10:00"))
                    .setEnd(DateUtil.parseToDate("2015-02-05T18:30")));
    Encounter p1e2 =
        ((Encounter) new Encounter().setId("p1e2"))
            .setSubject(new Reference(p1))
            .setClass_(amb)
            .setPartOf(new Reference(p1e1));
    Encounter p1e3 =
        ((Encounter) new Encounter().setId("p1e3"))
            .setSubject(new Reference(p1))
            .setClass_(amb)
            .setPartOf(new Reference(p1e2));

    Encounter p2e1 =
        ((Encounter) new Encounter().setId("p2e1"))
            .setSubject(new Reference(p2))
            .setClass_(imp)
            .setPeriod(
                new Period()
                    .setStart(DateUtil.parseToDate("2016-02-01T10:00"))
                    .setEnd(DateUtil.parseToDate("2016-02-05T18:30")));
    Encounter p2e2 =
        ((Encounter) new Encounter().setId("p2e2"))
            .setSubject(new Reference(p2))
            .setClass_(amb)
            .setPartOf(new Reference(p2e1));
    Encounter p2e3 =
        ((Encounter) new Encounter().setId("p2e3"))
            .setSubject(new Reference(p2))
            .setClass_(amb)
            .setPartOf(new Reference(p2e2));

    Observation p1o =
        ((Observation) new Observation().setId("p1o"))
            .setSubject(new Reference(p1))
            .setEncounter(new Reference(p1e1))
            .setCode(new CodeableConcept(new Coding("http://loinc.org", "711-2", "")))
            .setValue(new Quantity().setValue(new BigDecimal("0.92")).setUnit("x10*9/L"))
            .setEffective(new DateTimeType("2015-02-01T12:00:00"));
    Procedure p1p =
        ((Procedure) new Procedure().setId("p1p"))
            .setSubject(new Reference(p1))
            .setEncounter(new Reference(p1e2))
            .setCode(new CodeableConcept(new Coding("http://snomed.info/sct", "399010004", "")))
            .setPerformed(
                new Period()
                    .setStart(DateUtil.parseToDate("2015-02-02T11:00"))
                    .setEnd(DateUtil.parseToDate("2015-02-02T12:00")));
    Condition p1c =
        ((Condition) new Condition().setId("p1c"))
            .setSubject(new Reference(p1))
            .setEncounter(new Reference(p1e3))
            .setCode(new CodeableConcept(new Coding("http://snomed.info/sct", "39065001", "")))
            .setOnset(new DateTimeType("2015-02-03T12:00:00"));

    Medication m1 =
        ((Medication) new Medication().setId("m1"))
            .setCode(
                new CodeableConcept(new Coding("http://hl7.org/fhir/sid/ndc", "24208-813-20", "")));
    Medication m2 =
        ((Medication) new Medication().setId("m2"))
            .setCode(
                new CodeableConcept(new Coding("http://hl7.org/fhir/sid/ndc", "24208-813-30", "")));

    MedicationAdministration p2ma =
        ((MedicationAdministration) new MedicationAdministration().setId("p2ma"))
            .setSubject(new Reference(p2))
            .setContext(new Reference(p2e1))
            .setMedication(
                new CodeableConcept(new Coding("http://hl7.org/fhir/sid/ndc", "24208-813-10", "")))
            .setEffective(new DateTimeType("2016-02-01T12:00:00"));
    MedicationStatement p2ms =
        ((MedicationStatement) new MedicationStatement().setId("p2ms"))
            .setSubject(new Reference(p2))
            .setContext(new Reference(p2e2))
            .setMedication(new Reference(m1))
            .setEffective(
                new Period()
                    .setStart(DateUtil.parseToDate("2016-02-02T11:00"))
                    .setEnd(DateUtil.parseToDate("2016-02-02T12:00")));
    MedicationRequest p2mr =
        ((MedicationRequest) new MedicationRequest().setId("p2mr"))
            .setSubject(new Reference(p2))
            .setEncounter(new Reference(p2e3))
            .setMedication(new Reference(m2))
            .addDosageInstruction(
                new Dosage()
                    .setTiming(
                        new Timing()
                            .setRepeat(
                                new TimingRepeatComponent()
                                    .setBounds(
                                        new Period()
                                            .setStart(DateUtil.parseToDate("2016-02-03T11:00"))
                                            .setEnd(DateUtil.parseToDate("2016-02-03T12:00"))))));

    Bundle b = new Bundle().setType(Bundle.BundleType.COLLECTION);

    b.addEntry().setResource(p1);
    b.addEntry().setResource(p1e1);
    b.addEntry().setResource(p1e2);
    b.addEntry().setResource(p1e3);

    b.addEntry().setResource(p2);
    b.addEntry().setResource(p2e1);
    b.addEntry().setResource(p2e2);
    b.addEntry().setResource(p2e3);

    b.addEntry().setResource(p1o);
    b.addEntry().setResource(p1p);
    b.addEntry().setResource(p1c);

    b.addEntry().setResource(m1);
    b.addEntry().setResource(m2);

    b.addEntry().setResource(p2ma);
    b.addEntry().setResource(p2ms);
    b.addEntry().setResource(p2mr);

    FHIR = FHIRUtil.toString(b);
  }

  @Test
  void test() {
    Reader reader = new StringReader(FHIR);

    FHIRImport imp =
        new FHIRImport(
            DATA_SOURCE_ID,
            reader,
            subjectRepository,
            encounterRepository,
            subjectResourceRepository);
    imp.run();

    SubjectDao sub1 =
        new SubjectDao(DATA_SOURCE_ID, "Patient/p1", DateUtil.parse("1974-12-25"), "male");
    SubjectDao sub2 =
        new SubjectDao(DATA_SOURCE_ID, "Patient/p2", DateUtil.parse("1982-01-23"), "female");

    EncounterDao enc11 =
        new EncounterDao(
            DATA_SOURCE_ID,
            "Encounter/p1e1",
            sub1,
            "IMP",
            DateUtil.parse("2015-02-01T10:00"),
            DateUtil.parse("2015-02-05T18:30"));
    EncounterDao enc12 = new EncounterDao(DATA_SOURCE_ID, "Encounter/p1e2", sub1).type("AMB");
    EncounterDao enc13 = new EncounterDao(DATA_SOURCE_ID, "Encounter/p1e3", sub1).type("AMB");

    EncounterDao enc21 =
        new EncounterDao(
            DATA_SOURCE_ID,
            "Encounter/p2e1",
            sub2,
            "IMP",
            DateUtil.parse("2016-02-01T10:00"),
            DateUtil.parse("2016-02-05T18:30"));
    EncounterDao enc22 = new EncounterDao(DATA_SOURCE_ID, "Encounter/p2e2", sub2).type("AMB");
    EncounterDao enc23 = new EncounterDao(DATA_SOURCE_ID, "Encounter/p2e3", sub2).type("AMB");

    SubjectResourceDao p1o =
        new SubjectResourceDao(
                DATA_SOURCE_ID, "Observation/p1o", sub1, enc11, "http://loinc.org", "711-2")
            .numberValue(new BigDecimal("0.92"))
            .unit("x10*9/L")
            .dateTime(DateUtil.parse("2015-02-01T12:00"));
    SubjectResourceDao p1p =
        new SubjectResourceDao(
                DATA_SOURCE_ID, "Procedure/p1p", sub1, enc12, "http://snomed.info/sct", "399010004")
            .booleanValue(true)
            .startDateTime(DateUtil.parse("2015-02-02T11:00"))
            .endDateTime(DateUtil.parse("2015-02-02T12:00"));
    SubjectResourceDao p1c =
        new SubjectResourceDao(
                DATA_SOURCE_ID, "Condition/p1c", sub1, enc13, "http://snomed.info/sct", "39065001")
            .booleanValue(true)
            .dateTime(DateUtil.parse("2015-02-03T12:00"));

    SubjectResourceDao p2ma =
        new SubjectResourceDao(
                DATA_SOURCE_ID,
                "MedicationAdministration/p2ma",
                sub2,
                enc21,
                "http://hl7.org/fhir/sid/ndc",
                "24208-813-10")
            .booleanValue(true)
            .dateTime(DateUtil.parse("2016-02-01T12:00"));
    SubjectResourceDao p2ms =
        new SubjectResourceDao(
                DATA_SOURCE_ID,
                "MedicationStatement/p2ms",
                sub2,
                enc22,
                "http://hl7.org/fhir/sid/ndc",
                "24208-813-20")
            .booleanValue(true)
            .startDateTime(DateUtil.parse("2016-02-02T11:00"))
            .endDateTime(DateUtil.parse("2016-02-02T12:00"));
    SubjectResourceDao p2mr =
        new SubjectResourceDao(
                DATA_SOURCE_ID,
                "MedicationRequest/p2mr",
                sub2,
                enc23,
                "http://hl7.org/fhir/sid/ndc",
                "24208-813-30")
            .booleanValue(true)
            .startDateTime(DateUtil.parse("2016-02-03T11:00"))
            .endDateTime(DateUtil.parse("2016-02-03T12:00"));

    assertSubjects(sub1, sub2);
    assertEncounters(enc11, enc12, enc13, enc21, enc22, enc23);
    assertSubjectResources(p1c, p2ma, p2mr, p2ms, p1o, p1p);
  }

  @Test
  void testMergeEncounters() {
    Reader reader = new StringReader(FHIR);

    FHIRImport imp =
        new FHIRImport(
            DATA_SOURCE_ID,
            reader,
            subjectRepository,
            encounterRepository,
            subjectResourceRepository,
            true);
    imp.run();

    SubjectDao sub1 =
        new SubjectDao(DATA_SOURCE_ID, "Patient/p1", DateUtil.parse("1974-12-25"), "male");
    SubjectDao sub2 =
        new SubjectDao(DATA_SOURCE_ID, "Patient/p2", DateUtil.parse("1982-01-23"), "female");

    EncounterDao enc11 =
        new EncounterDao(
            DATA_SOURCE_ID,
            "Encounter/p1e1",
            sub1,
            "IMP",
            DateUtil.parse("2015-02-01T10:00"),
            DateUtil.parse("2015-02-05T18:30"));
    EncounterDao enc12 = new EncounterDao(DATA_SOURCE_ID, "Encounter/p1e2", sub1).type("AMB");
    EncounterDao enc13 = new EncounterDao(DATA_SOURCE_ID, "Encounter/p1e3", sub1).type("AMB");

    EncounterDao enc21 =
        new EncounterDao(
            DATA_SOURCE_ID,
            "Encounter/p2e1",
            sub2,
            "IMP",
            DateUtil.parse("2016-02-01T10:00"),
            DateUtil.parse("2016-02-05T18:30"));
    EncounterDao enc22 = new EncounterDao(DATA_SOURCE_ID, "Encounter/p2e2", sub2).type("AMB");
    EncounterDao enc23 = new EncounterDao(DATA_SOURCE_ID, "Encounter/p2e3", sub2).type("AMB");

    SubjectResourceDao p1o =
        new SubjectResourceDao(
                DATA_SOURCE_ID, "Observation/p1o", sub1, enc11, "http://loinc.org", "711-2")
            .numberValue(new BigDecimal("0.92"))
            .unit("x10*9/L")
            .dateTime(DateUtil.parse("2015-02-01T12:00"));
    SubjectResourceDao p1p =
        new SubjectResourceDao(
                DATA_SOURCE_ID, "Procedure/p1p", sub1, enc11, "http://snomed.info/sct", "399010004")
            .booleanValue(true)
            .startDateTime(DateUtil.parse("2015-02-02T11:00"))
            .endDateTime(DateUtil.parse("2015-02-02T12:00"));
    SubjectResourceDao p1c =
        new SubjectResourceDao(
                DATA_SOURCE_ID, "Condition/p1c", sub1, enc11, "http://snomed.info/sct", "39065001")
            .booleanValue(true)
            .dateTime(DateUtil.parse("2015-02-03T12:00"));

    SubjectResourceDao p2ma =
        new SubjectResourceDao(
                DATA_SOURCE_ID,
                "MedicationAdministration/p2ma",
                sub2,
                enc21,
                "http://hl7.org/fhir/sid/ndc",
                "24208-813-10")
            .booleanValue(true)
            .dateTime(DateUtil.parse("2016-02-01T12:00"));
    SubjectResourceDao p2ms =
        new SubjectResourceDao(
                DATA_SOURCE_ID,
                "MedicationStatement/p2ms",
                sub2,
                enc21,
                "http://hl7.org/fhir/sid/ndc",
                "24208-813-20")
            .booleanValue(true)
            .startDateTime(DateUtil.parse("2016-02-02T11:00"))
            .endDateTime(DateUtil.parse("2016-02-02T12:00"));
    SubjectResourceDao p2mr =
        new SubjectResourceDao(
                DATA_SOURCE_ID,
                "MedicationRequest/p2mr",
                sub2,
                enc21,
                "http://hl7.org/fhir/sid/ndc",
                "24208-813-30")
            .booleanValue(true)
            .startDateTime(DateUtil.parse("2016-02-03T11:00"))
            .endDateTime(DateUtil.parse("2016-02-03T12:00"));

    assertSubjects(sub1, sub2);
    assertEncounters(enc11, enc12, enc13, enc21, enc22, enc23);
    assertSubjectResources(p1c, p2ma, p2mr, p2ms, p1o, p1p);
  }
}
