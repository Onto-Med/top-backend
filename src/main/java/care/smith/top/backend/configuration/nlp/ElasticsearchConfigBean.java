package care.smith.top.backend.configuration.nlp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchConfigBean {

  @Value("${spring.elasticsearch.index.name}")
  private String indexName;

//  @Value("${top.documents.ellipsis:15}")
//  private static Integer wordEllipsis;

  public String getIndexName() {
    return indexName;
  }

  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

//  public static Integer wordEllipsis() { return wordEllipsis; }
}
