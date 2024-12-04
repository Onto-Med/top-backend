package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.SubjectDao;
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

public abstract class CSVImport {

  protected final Logger LOGGER = LoggerFactory.getLogger(CSVImport.class);
  protected String dataSourceId;
  protected String[] header;
  protected Map<String, Method> fields;
  private CSVReader csvReader;

  protected CSVImport(
      Class<SubjectDao> daoClass,
      String dataSourceId,
      Map<String, String> fieldsMapping,
      Reader reader,
      char separator) {
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
    this.dataSourceId = dataSourceId;
    this.fields =
        Stream.of(daoClass.getDeclaredMethods())
            .filter(m -> m.getParameterCount() == 1)
            .collect(Collectors.toMap(Method::getName, Function.identity()));
  }

  protected CSVImport(
      Class<SubjectDao> daoClass,
      String dataSourceId,
      Map<String, String> fieldsMapping,
      Reader reader) {
    this(daoClass, dataSourceId, fieldsMapping, reader, ';');
  }

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

  protected void setFields(Object dao, String[] values) {
    for (int i = 0; i < header.length; i++)
      if (header[i] != null) setField(dao, header[i], values[i].trim());
  }

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
}
