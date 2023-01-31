package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.nlp.DocumentEntity;
import care.smith.top.backend.model.nlp.PhraseEntity;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Document;
import care.smith.top.backend.repository.nlp.DocumentRepository;
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

    @Autowired DocumentRepository documentRepository;

    @Override
    @Cacheable("documentCount")
    public long count() { return documentRepository.count(); }


    public Document getDocumentById(String documentId) {
        //ToDo: I don't want to return 'null' -> rather some form of 'Empty'-Document
        return documentRepository
                .findOne(documentForId(documentId))
                .map(documentEntityMapper)
                .orElse(null);
    }

    @Cacheable("conceptDocumentIds")
    public List<Document> getDocumentsForConcepts(Set<String> conceptIds, Boolean idOnly) {
        if (conceptIds.size() == 0) {
            return List.of();
        }
        HashMap<String, Document> docMap = documentRepository
                .findAll(documentsForConceptsUnion(conceptIds))
                .stream()
                .map(idOnly ? documentEntityMapperIdOnly : documentEntityMapper)
                .collect(Collectors.toMap(Document::getId, Function.identity(), (prev, next) -> next, HashMap::new));

        return new ArrayList<>(docMap.values());
    }

    static Statement documentForId(String documentId) {
        Node document = Cypher.node("Document")
                .withProperties("docId", Cypher.literalOf(documentId)).named("Document");

        return Cypher
                .match(document)
                .returning(document)
                .build();
    }

    static Statement documentsForConceptsUnion(Set<String> conceptIds) {
        Node document = Cypher.node("Document").named("document");
        Node phrase = Cypher.node("Phrase").named("phrase");
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
            .id(documentEntity.documentId())
            .text(documentEntity.documentText())
            .phrases(documentEntity
                    .documentPhrases()
                    .stream()
                    .map(PhraseEntity::phraseText)
                    .sorted()
                    .collect(Collectors.toList()));

    private final Function<DocumentEntity, Document> documentEntityMapperIdOnly = documentEntity -> new Document()
            .id(documentEntity.documentId());

}
