package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Date;

/** Example: (:Annotation:String:Title { value: 'Weight', language: 'en', index: 1 }) */
@Node
public class Annotation extends Annotatable {
  @Id @GeneratedValue private Long id;

  private String datatype;
  private String property;
  private String stringValue;
  private Date dateValue;
  private Double decimalValue;
  private Boolean booleanValue;

  @Relationship(type = "HAS_CLASS_VALUE")
  private ClassVersion classValue;

  private String language;
  private Integer index;

  public Annotation() {}

  public Annotation(String property, String stringValue, String language, Integer index) {
    this(property, language, index);
    this.datatype = "string";
    this.stringValue = stringValue;
  }

  public Annotation(String property, Date dateValue, String language, Integer index) {
    this(property, language, index);
    this.datatype = "string";
    this.dateValue = dateValue;
  }

  public Annotation(String property, Double decimalValue, String language, Integer index) {
    this(property, language, index);
    this.datatype = "decimal";
    this.decimalValue = decimalValue;
  }

  public Annotation(String property, Boolean booleanValue, String language, Integer index) {
    this(property, language, index);
    this.datatype = "decimal";
    this.booleanValue = booleanValue;
  }

  public Annotation(String property, ClassVersion classValue, String language, Integer index) {
    this(property, language, index);
    this.datatype = "class";
    this.classValue = classValue;
  }

  private Annotation(String property, String language, Integer index) {
    this.property = property;
    this.language = language;
    this.index = index;
  }

  public Long getId() {
    return id;
  }

  public Object getValue() {
    switch (datatype) {
      case "string":
        return stringValue;
      case "date":
        return dateValue;
      case "decimal":
        return decimalValue;
      case "boolean":
        return booleanValue;
      case "class":
        return classValue;
      default:
        return null;
    }
  }

  public String getDatatype() {
    return datatype;
  }

  public Annotation setDatatype(String datatype) {
    this.datatype = datatype;
    return this;
  }

  public Double getDecimalValue() {
    return decimalValue;
  }

  public Annotation setDecimalValue(Double decimalValue) {
    this.decimalValue = decimalValue;
    return this;
  }

  public String getProperty() {
    return property;
  }

  public Annotation setProperty(String property) {
    this.property = property;
    return this;
  }

  public String getStringValue() {
    return stringValue;
  }

  public Annotation setStringValue(String stringValue) {
    this.stringValue = stringValue;
    return this;
  }

  public Date getDateValue() {
    return dateValue;
  }

  public Annotation setDateValue(Date dateValue) {
    this.dateValue = dateValue;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public Annotation setLanguage(String language) {
    this.language = language;
    return this;
  }

  public Integer getIndex() {
    return index;
  }

  public Annotation setIndex(Integer index) {
    this.index = index;
    return this;
  }

  public Boolean getBooleanValue() {
    return booleanValue;
  }

  public Annotation setBooleanValue(Boolean booleanValue) {
    this.booleanValue = booleanValue;
    return this;
  }

  public ClassVersion getClassValue() {
    return classValue;
  }

  public Annotation setClassValue(ClassVersion classValue) {
    this.classValue = classValue;
    return this;
  }
}
