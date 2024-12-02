package care.smith.top.backend.service.datasource;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CSVImport {

  protected final Logger LOGGER = LoggerFactory.getLogger(CSVImport.class);
  protected String dataSourceId;
  protected String[] header;
  private CSVReader csvReader;

  protected CSVImport(
      String dataSourceId, Map<String, String> fieldsMapping, Reader reader, char separator) {
    this.dataSourceId = dataSourceId;
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
  }

  protected CSVImport(String dataSourceId, Map<String, String> fieldsMapping, Reader reader) {
    this(dataSourceId, fieldsMapping, reader, ';');
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
}
