package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.nlp.DocumentEntity;
import care.smith.top.backend.model.nlp.DocumentNodeEntity;
import care.smith.top.backend.model.nlp.PhraseEntity;
import care.smith.top.backend.repository.nlp.DocumentNodeRepository;
import care.smith.top.backend.repository.nlp.DocumentRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Document;
import org.neo4j.cypherdsl.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DocumentService implements ContentService {

    private final DocumentRepository documentRepository;
    private final DocumentNodeRepository documentNodeRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository, DocumentNodeRepository documentNodeRepository) {
        this.documentRepository = documentRepository;
        this.documentNodeRepository = documentNodeRepository;
    }

    @Override
    @Cacheable("documentCount")
    public long count() { return documentNodeRepository.count(); }


    public Document getDocumentByName(String documentName) {
        return documentEntityMapper.apply(
                documentRepository.findDocumentEntityByDocumentName(documentName)
        );
    }

    public List<Document> getDocumentsByTerms(String[] terms, String[] fields) {
        return documentRepository
                .getDocumentsByTerms(terms, fields)
                .stream()
                .map(documentEntityMapper)
                .collect(Collectors.toList());
    }

    @Cacheable("conceptDocumentIds")
    public List<Document> getDocumentsForConcepts(Set<String> conceptIds, Boolean idOnly, Boolean exemplarOnly) {
        if (conceptIds.size() == 0) {
            return List.of();
        }
        HashMap<String, Document> docMap = documentNodeRepository
                .findAll(documentsForConceptsUnion(conceptIds, exemplarOnly))
                .stream()
                .map(idOnly ? documentNodeEntityMapperIdOnly : documentNodeEntityMapper)
                .collect(Collectors.toMap(Document::getId, Function.identity(), (prev, next) -> next, HashMap::new));

        return new ArrayList<>(docMap.values());
    }

    static Statement documentsForConceptsUnion(Set<String> conceptIds, Boolean exemplarOnly) {
        Node document = Cypher.node("Document").named("document");
        Node phrase;
        if (exemplarOnly) {
            phrase = Cypher.node("Phrase").withProperties(Map.of("exemplar", true)).named("phrase");
        } else {
            phrase = Cypher.node("Phrase").named("phrase");
        }
        Node concept = Cypher.node("Concept").named("concept");

        return Cypher
                .match(
                        concept.relationshipFrom(phrase, "IN_CONCEPT"),
                        document.relationshipTo(phrase, "HAS_PHRASE")
                )
                .where(concept.property("conceptId").in(someIdList(conceptIds)))
                .returning(document)
                .build();
    }

    static Expression someIdList(Set<String> someIds) {
        return Cypher.listOf(someIds
                .stream()
                .map(Cypher::literalOf)
                .collect(Collectors.toList())
        );
    }

    private final Function<DocumentEntity, Document> documentEntityMapper = documentEntity -> new Document()
            .id(documentEntity.getDocumentName())
            .text(documentEntity.getDocumentText());

    private final Function<DocumentNodeEntity, Document> documentNodeEntityMapper = documentNodeEntity -> new Document()
            .id(documentNodeEntity.documentId())
            .phrases(documentNodeEntity
                    .documentPhrases()
                    .stream()
                    .map(PhraseEntity::phraseText)
                    .sorted()
                    .collect(Collectors.toList()));

    private final Function<DocumentNodeEntity, Document> documentNodeEntityMapperIdOnly = documentNodeEntity -> new Document()
            .id(documentNodeEntity.documentId());

}
