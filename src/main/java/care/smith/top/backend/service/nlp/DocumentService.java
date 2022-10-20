package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.nlp.DocumentEntity;
import care.smith.top.backend.model.nlp.PhraseEntity;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Document;
import care.smith.top.backend.repository.nlp.DocumentRepository;
import org.neo4j.cypherdsl.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DocumentService implements ContentService {

    @Autowired DocumentRepository documentRepository;

    @Override
    @Cacheable("documentCount")
    public long count() { return documentRepository.count(); }


//    @Cacheable("documents")
//    public List<Document> getDocuments(List<String> docIds) {
//        if (docIds == null || docIds.size() == 0) {
//            return documentRepository
//                    .findAll()
//                    .stream()
//                    .map(documentEntityMapper)
//                    .collect(Collectors.toList());
//        }
//        return documentRepository
//                .findAll(documentWithId(docIds))
//                .stream()
//                .map(documentEntityMapper)
//                .collect(Collectors.toList());
//    }
//
//    public List<Document> getDocumentsByPage(List<String> include, int page, int pageSize) {
//        return documentRepository
//                .findAll(PageRequest.of(page, pageSize))// use pageing together with "ongoing and return"
//                .stream()
//                .map(documentEntityMapper)
//                .collect(Collectors.toList());
//    }

    public Document getDocumentById(String documentId) {
        //ToDo: I don't want to return 'null' -> rather some form of 'Empty'-Document
        return documentRepository
                .findOne(documentForId(documentId))
                .map(documentEntityMapper)
                .orElse(null);
    }

    public List<Document> getDocumentsForConcept(String conceptId) {
        return documentRepository
                .findAll(documentsForConcept(conceptId))
                .stream()
                .map(documentEntityMapper)
                .collect(Collectors.toList());
    }

    static Statement documentForId(String documentId) {
        Node document = Cypher.node("Document")
                .withProperties("docId", Cypher.literalOf(documentId)).named("Document");

        return Cypher
                .match(document)
                .returning(document)
                .build();
    }

    static Statement documentsForConcept(String conceptId) {
        Node concept = Cypher.node("Concept")
                .withProperties("conceptId", Cypher.literalOf(conceptId)).named("concept");
        Node document = Cypher.node("Document").named("document");
        Node phrase = Cypher.node("Phrase").named("phrase");

        return Cypher
                .match(
                        concept.relationshipFrom(phrase, "IN_CONCEPT"),
                        document.relationshipTo(phrase, "HAS_PHRASE")
                )
                .returning(document)
                .build();

    }
//    static Statement documentWithId(List<String> docIds) {
//        Node document = Cypher.node("Document").named("document");
//        Property docIdProp = document.property("docId");
//
//        Expression idList = Cypher.listOf(docIds
//                .stream()
//                .map(id -> docIdProp.eq(Cypher.literalOf(id)))
//                .collect(Collectors.toList())
//        );
//
//        return Cypher.match(document)
//                .where(idList.asCondition())
//                .returning(document)
//                .build();
//    }

    private final Function<DocumentEntity, Document> documentEntityMapper = documentEntity -> new Document()
            .id(documentEntity.documentId())
            .text(documentEntity.documentText())
            .phrases(documentEntity
                    .documentPhrases()
                    .stream()
                    .map(PhraseEntity::phraseText)
                    .sorted()
                    .collect(Collectors.toList()));

}
