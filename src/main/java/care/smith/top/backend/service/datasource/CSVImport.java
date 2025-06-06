package care.smith.top.backend.service.datasource;

import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.ExpectedResultRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import care.smith.top.top_document_query.util.DateUtil;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CSVImport extends DataImport {

  private final Logger LOGGER = LoggerFactory.getLogger(CSVImport.class);
  private final Map<String, Method> fields;
  private String[] header;
  private CSVReader csvReader;

  protected CSVImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      ExpectedResultRepository expectedResultRepository,
      Class<?> daoClass,
      Map<String, String> fieldsMapping,
      char separator) {
    super(
        dataSourceId,
        reader,
        subjectRepository,
        encounterRepository,
        subjectResourceRepository,
        expectedResultRepository);
    try {
      CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
      csvReader = new CSVReaderBuilder(reader).withCSVParser(parser).build();
      header =
          Stream.of(csvReader.readNext())
              .map(h -> fieldsMapping.get(h.trim()))
              .toArray(String[]::new);
    } catch (IOException | CsvValidationException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    this.fields =
        Stream.of(daoClass.getDeclaredMethods())
            .filter(m -> m.getParameterCount() == 1)
            .collect(Collectors.toMap(Method::getName, Function.identity()));
  }

  protected CSVImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      ExpectedResultRepository expectedResultRepository,
      Class<?> daoClass,
      Map<String, String> fieldsMapping) {
    this(
        dataSourceId,
        reader,
        subjectRepository,
        encounterRepository,
        subjectResourceRepository,
        expectedResultRepository,
        daoClass,
        fieldsMapping,
        ';');
  }

  @Override
  public void run() {
    try {
      String[] values;
      while ((values = csvReader.readNext()) != null) run(values);
      csvReader.close();
    } catch (IOException | CsvValidationException e) {
      LOGGER.warn(e.getMessage(), e);
    }
  }

  public abstract void run(String[] values);

  private void setField(Object dao, String name, String value) {
    Method m = fields.get(name);
    Class<?> type = m.getParameterTypes()[0];
    if (BigDecimal.class.equals(type)) invoke(dao, m, new BigDecimal(value));
    else if (Boolean.class.equals(type)) invoke(dao, m, Boolean.valueOf(value));
    else if (LocalDateTime.class.equals(type)) invoke(dao, m, DateUtil.parse(value));
    else invoke(dao, m, value);
  }

  private void invoke(Object dao, Method m, Object value) {
    try {
      m.invoke(dao, value);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      LOGGER.warn(e.getMessage(), e);
    }
  }

  protected void setFields(Object dao, String[] values) {
    for (int i = 0; i < header.length; i++)
      if (header[i] != null && values[i] != null && !values[i].isBlank())
        setField(dao, header[i], values[i].trim());
  }
}
