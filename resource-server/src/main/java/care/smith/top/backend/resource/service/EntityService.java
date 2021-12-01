package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.LocalisableText;
import care.smith.top.data.Keys;
import care.smith.top.data.tables.records.AnnotationRecord;
import care.smith.top.data.tables.records.ClassRecord;
import care.smith.top.data.tables.records.ClassVersionRecord;
import care.smith.top.data.tables.records.PropertyRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import static care.smith.top.data.Tables.*;

@Service
public class EntityService {
  @Autowired DSLContext context;

  public Entity loadEntity(
      String organisationName, String repositoryName, UUID id, Integer version) {
    ClassVersionRecord record = loadEntityRecord(organisationName, repositoryName, id, version);
    if (record == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    return mapToEntity(record);
  }

  public void deleteEntity(
      String organisationName, String repositoryName, UUID id, Integer version) {
    ClassVersionRecord record = loadEntityRecord(organisationName, repositoryName, id, version);
    if (record == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    record.setHiddenAt(OffsetDateTime.now());
    record.update();
  }

  private Entity mapToEntity(ClassVersionRecord record) {
    Entity entity = new Entity();

    // TODO: implement mapping from DB...
    ClassRecord classRecord = record.fetchParent(Keys.CLASS_VERSION__FK_CLASS_TO_CLASS_VERSION);
    if (classRecord == null)
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          String.format("Class with ID '%d' does not exist.", record.getClassId()));

    entity.setId(classRecord.getUuid());
    entity.setCreatedAt(record.getCreatedAt());
    entity.setVersion(record.getVersion());

    /**** Examples on how to get annotation "title" ***
     * TODO: select one approach and delete the other one
     */

    /** Via fetchChildren(): * */
    Result<AnnotationRecord> annotations =
        record.fetchChildren(Keys.ANNOTATION__FK_CLASS_VERSION_TO_ANNOTATION);
    PropertyRecord titleProperty = context.fetchOne(PROPERTY, PROPERTY.NAME.eq("title"));

    if (titleProperty != null)
      entity.setTitles(
          annotations.stream()
              .filter(a -> a.getPropertyId().equals(titleProperty.getPropertyId()))
              .map(
                  a -> {
                    LocalisableText title = new LocalisableText();
                    title.setLang(a.getLanguage());
                    title.setText(a.getStringValue());
                    return title;
                  })
              .collect(Collectors.toList()));

    /** Via fresh query * */
    Result<AnnotationRecord> titles =
        context.fetch(
            ANNOTATION,
            ANNOTATION
                .CLASS_VERSION_ID
                .eq(record.getClassId())
                .and(ANNOTATION.property().NAME.eq("title")));

    entity.setTitles(
        titles.stream()
            .map(
                t -> {
                  LocalisableText title = new LocalisableText();
                  title.setLang(t.getLanguage());
                  title.setText(t.getStringValue());
                  return title;
                })
            .collect(Collectors.toList()));

    return entity;
  }

  private ClassVersionRecord loadEntityRecord(
      String organisationName, String repositoryName, UUID id, Integer version) {
    // TODO: filter by organisationName and repositoryName
    return context.fetchOne(
        CLASS_VERSION, CLASS_VERSION.class_().UUID.eq(id).and(CLASS_VERSION.VERSION.eq(version)));
  }
}
