package care.smith.top.backend.repository.nlp.custom;

import care.smith.top.backend.model.nlp.DocumentEntity;
import java.util.List;

public interface DocumentCustomRepository {

  /**
   * @param terms list of terms; can also be boolean term description (e.g. [+TERM1, -TERM2])
   * @param fields list of fields to search in
   * @return List of DocumentEntity
   */
  List<DocumentEntity> getESDocumentsByTerms(String[] terms, String[] fields);

  List<DocumentEntity> getESDocumentsByTermsBoolean(
      String[] shouldTerms, String[] mustTerms, String[] notTerms, String[] fields);

  List<DocumentEntity> getESDocumentsByPhrases(String[] phrases, String[] fields);

  List<DocumentEntity> getESDocumentsByPhrasesBoolean(
      String[] shouldPhrases, String[] mustPhrases, String[] notPhrases, String[] fields);
}
