package care.smith.top.backend.util;

import care.smith.top.model.Entity;
import care.smith.top.model.Repository;
import care.smith.top.top_phenotypic_query.converter.PhenotypeExporter;
import care.smith.top.top_phenotypic_query.converter.PhenotypeImporter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.*;
import java.util.logging.Logger;

public class TopJsonFormat implements PhenotypeExporter, PhenotypeImporter {
  private static final Logger LOGGER = Logger.getLogger(TopJsonFormat.class.getName());
  private final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  @Override
  public String getFileExtension() {
    return "json";
  }

  @Override
  public Entity[] read(InputStream inputStream) {
    try {
      return MAPPER.readValue(inputStream, Entity[].class);
    } catch (IOException e) {
      LOGGER.warning(e.getMessage());
    }
    return new Entity[0];
  }

  @Override
  public Entity[] read(File file) {
    try {
      return read(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      LOGGER.warning(e.getMessage());
    }
    return new Entity[0];
  }

  @Override
  public void write(Entity[] entities, Repository repository, String s, OutputStream outputStream) {
    try {
      MAPPER.writeValue(outputStream, entities);
    } catch (IOException e) {
      LOGGER.warning(e.getMessage());
    }
  }

  @Override
  public void write(Entity[] entities, Repository repository, String s, File file) {
    try {
      write(entities, repository, s, new FileOutputStream(file));
    } catch (FileNotFoundException e) {
      LOGGER.warning(e.getMessage());
    }
  }
}
