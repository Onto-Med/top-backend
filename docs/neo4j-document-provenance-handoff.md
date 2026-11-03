# Handoff: document index provenance from Elasticsearch through concept graphs to Neo4j

## Purpose and current authorization

Continue the explicit document-index integration described in
[index-selection-handoff.md](index-selection-handoff.md), with particular attention to graph hydration.
This document records read-only cross-project findings and user decisions. It is not evidence of a
completed implementation.

The user requested this handoff so another agent can work on the findings. No Python/library changes
were made during this investigation. Present a bounded cross-project plan before editing; confirm the
working scope rather than treating this document as blanket authorization to change sibling projects.

Read repository instructions, README, and git status in every repository involved. Preserve user changes.
Do not access secrets, perform network actions, delete/recreate databases, run migrations, or run costly
builds/container suites without appropriate approval. Lack of production deployment is not permission
to delete development data automatically.

## Agreed decisions

- Keep the existing index-selection contract: exactly one explicit index/alias per document context.
  Require client selection even for a sole-choice source; no first-index or implicit all-index fallback.
- Do **not** introduce content hashes, content-addressed identity, or a broader identity-model redesign.
  These were discussed and deliberately deferred.
- Add source/index provenance to Neo4j document nodes, distinguishing documents by
  `(dataSourceId, concrete index, documentId)` instead of document ID alone.
- Simply adding properties while leaving `docId` as the sole node identity is insufficient.
- Do not duplicate index properties onto every phrase or concept node. Document relationships provide
  document provenance; corpus/pipeline context remains a separate concern.
- The framework is **not deployed in production**. The user says legacy migration/recreation concerns
  need not drive this implementation. Do not design a production historical-data migration as a blocker.
- ZIP pipeline input remains valid without Elasticsearch provenance. Do not invent an Elasticsearch
  destination, import uploaded documents, or fabricate source/index references for ZIP documents.
- No alias restriction has been agreed. Investigate/implement provenance propagation rather than
  silently forbidding aliases, assuming IDs are globally unique, or selecting an arbitrary match.

## Identity and context distinctions

| Context | Meaning |
| --- | --- |
| Source ID | Configured, access-controlled document source; not an endpoint or process name. |
| Selected index/alias | Explicit input choice used to browse, query, or start a server-input pipeline. |
| Concrete index | Elasticsearch hit `_index`; needed to identify the originating stored document. |
| Document retrieval ID | Elasticsearch `_id`; may differ from an ID embedded in `_source`. |
| Corpus/process ID | Graph-analysis context; not an Elasticsearch index or source ID. |
| Marqo index | Embedding/RAG storage; not a document-source choice. |

Pure graph exploration does not inherently require an Elasticsearch selector. Document-specific graph
lookups need exact document identity plus independent graph/corpus context. Graph-derived document
retrieval and highlighting must use the identity to which the stored offsets belong.

A pipeline's selected alias and each document's concrete backing index are different information.
Looking up an ID through an alias during hydration only establishes its location **then**, not where
it was read during the original pipeline run. Alias rollover and duplicate IDs make reconstruction
unsafe. Retain original provenance instead.

## Repository locations and snapshot

- Backend: `C:/Users/fra3066mat/IdeaProjects/top-backend`
- Shared API: `C:/Users/fra3066mat/IdeaProjects/top-api`
- Document-query library: `C:/Users/fra3066mat/IdeaProjects/top-document-query`
- Python pipeline: `C:/Users/fra3066mat/PycharmProjects/concept-graphs`

At handoff creation, backend HEAD was `d23a1c7` (`document querying with explicit index and validation`).
`git status --short` showed only untracked `docs/` before this new file was written. Preserve that
folder. Python status during inspection showed an untracked `NUL`; do not modify/delete it.

Dependency choices are resolved: backend uses `top-api:0.15.0-SNAPSHOT` and
`top-document-query:0.9.7`. The cached artifacts were subsequently inspected and do expose the new
contract/scoped APIs. Earlier notes about stale cached snapshots describe an earlier inspection, not
the final artifact state. Publication was not independently verified over the network.

The API project's POM/schema version discrepancy was previously observed; it is not authorization to
change versions. Inspect current files rather than assuming the initial handoff snapshot remains exact.

## Backend behavior already added

Inspect current source before extending it:

- `api/nlp/DocumentApiDelegateImpl.java`: explicit index argument for browsing, imports, and details;
  scalar/missing/repeated selection validation; browsing via `forIndex`; explicit import destination;
  reference-aware detail lookup.
- `api/nlp/DocumentSelectionExceptionHandler.java` and
  `InvalidDocumentSelectionException.java`: document-controller HTTP 400 `ErrorResponse` handling.
- `service/nlp/DocumentQueryService.java`: discovery maps configured library choices to
  `DataSource.indices`.
- `src/test/java/care/smith/top/backend/nlp/api/DocumentSelectionTest.java`: focused boundary tests;
  existing document delegate tests were updated for signatures.

This does **not** complete response provenance mapping, saved queries/results, pipelines, or graphs.
The current validation/error infrastructure is document-controller-specific; do not assume pipeline
or phrase/cluster controllers already receive the same handling.

## Existing Neo4j representation

Backend Java paths below are under `src/main/java/care/smith/top/backend/`.

```text
(Document {docId, name})
    -[:HAS_PHRASE {offsets}]-> (Phrase {phraseId, phrase, attributes, exemplar})
                                  -[:IN_CONCEPT]-> (Concept {conceptId, labels, corpusId})
                                  -[:NEIGHBOR_OF]-> (Phrase)
```

- `model/neo4j/DocumentNodeEntity.java`: `@Id @Property("docId") String documentId` is currently the
  sole identity. No source/index. Full document text is not stored here.
- `PhraseNodeEntity.java` and `ConceptNodeEntity.java`: generated Neo4j identities. Corpus is stored
  on concepts, not on document nodes.
- `HasPhraseRelationship.java`: stores offsets connecting a document to a phrase.
- `repository/neo4j/DocumentNodeRepository.java`: graph filters return ID-only document models;
  existence lookup matches only `docId`.
- `PhraseNodeRepository.java`, `ConceptClusterNodeRepository.java`, and
  `PhraseDocumentRelationRepository.java`: document matching uses `docId`, optionally combined with
  corpus traversal. Source/index are absent.

Documents with the same ID in different indices/sources can therefore collide in Neo4j. Filtering
concepts by corpus afterward cannot recover document identity that was already conflated.

## Backend hydration findings

`api/nlp/ConceptClusterApiDelegateImpl.java#createConceptClustersForPipelineId` currently chooses an
adapter using **pipeline ID as source ID**. This accidental name coupling must be replaced with the
original pipeline input binding.

`service/nlp/ConceptClusterService.java`:

1. `createSpecificGraphsInNeo4j` retrieves graphs from the Python service, then starts hydration in a
   thread.
2. `createGraphInNeo4j` builds document-to-phrase maps keyed only by document ID.
3. It calls legacy `getDocumentsByIdsBatched(...)` against the supplied adapter.
4. It creates `new DocumentNodeEntity(docId, name)` and attaches ID-matched offsets.

The library already provides `getIndexedDocumentsByIdsBatched(...)`, returning `IndexedDocument`
with `DocumentReference`. Elasticsearch builds this reference from configured source ID, hit `_index`,
and hit `_id`. Reuse indexed reads when appropriate; close streams with try-with-resources to release
PIT cursors. Do not rebuild library scope/provenance functionality.

However, fetching indexed hits at hydration time is **not** sufficient to recover original alias
provenance. If a graph has only an ID, multiple hits cannot safely receive the same phrase offsets.

## Python investigation: exact provenance loss points

Paths below are relative to the Python repository. These are source-inspection findings, not runtime
verification. No cached pickles or private document corpora were loaded.

### 1. Elasticsearch ingestion drops hit metadata

`src/api/services/document_server.py`:

- `get_documents_from_es_server` reads Elasticsearch search/scroll hits.
- Each hit is passed to `check_es_source_for_id`.
- That function returns `_source` with a chosen `id`; it does not retain hit `_index`.
- Chosen ID may be `_source.id`, a configured `other_id`, or fallback `_id`. Do not silently assume
  this ID always equals the Elasticsearch retrieval ID.

`src/api/pipeline_support/document_server.py#load_data_from_document_server` passes the selected
index to ingestion but does not propagate a configured backend source identity into each document.
A minimal interface for that source identity still needs to be agreed.

### 2. Preprocessing only forwards selected metadata

`src/core/data/factory.py`:

- `_build_data_tuples` builds context containing `doc_id`, `doc_index`, `doc_name`, `doc_topic`, and
  `offset_in_doc`.
- **`doc_index` is an integer enumeration position, not an Elasticsearch index. Do not reuse it for
  index provenance.**
- `_process_documents` assigns those context values to spaCy document extensions.
- Chunk extraction/document dictionaries retain ID and offsets, not source/concrete index.
- Saving removes `_data_entries`, serializes processed spaCy documents using
  `DocBin(store_user_data=True)`, and pickles processed state. Original input dictionaries cannot be
  relied on as a later provenance store.

### 3. Offset aggregation conflates equal IDs

`src/core/documents.py#add_offset_to_documents_dicts_by_id` merges offsets by ID alone.
`src/core/data/factory.py` and `src/core/graph/creation.py#GraphCreator._combine_docs` use it.
Adding provenance only at graph serialization is too late: duplicate IDs may already be merged.

### 4. Graph storage/output can carry richer document dictionaries

`src/core/graph/creation.py` constructs NetworkX graphs with document dictionaries on phrase nodes.
Graph artifacts are pickled. `src/api/services/artifact_responses.py#graph_get_specific` exposes node
attributes through `dict(id=n, **v)`. The response layer is not the first provenance bottleneck; richer
metadata must first survive preprocessing, aggregation, and graph creation.

Check both local-pickle and configured vector-store paths before claiming end-to-end coverage.
Marqo document-provenance helpers and tests currently also use ID/offset dictionaries; the word
"provenance" there does not establish Elasticsearch source/index support.

### 5. Stored configuration is not a durable input binding

- `src/api/services/configuration.py#load_configs` reconstructs processing-step configurations.
- `src/api/routes/pipeline.py` returns stored-process name/language/config, not an original
  document-server source/index binding.
- `src/api/pipeline_support/steps.py` can reuse cached step artifacts based on process name and
  `skip_present`; inspected code does not establish input-binding conflict checks.

Do not claim safe resume just because a configuration endpoint exists. Persist and compare input
binding before reusing artifacts; propose representation/location before implementing it.

## Library/shared-contract gap

In document-query:

- `concept_graphs_api/model/PhraseDocumentObject.java` contains only `id` and `offsets`.
- `ConceptGraphEntity.java#toApiModel` maps only those fields to shared `NodeDocuments`.

A coordinated extension is needed to consume graph provenance. Decide the minimal Python-to-Java
wire representation and whether provenance must also be exposed in the shared graph response API.
Do not hand-edit generated models or silently redesign the shared API. Preserve existing ID fields
unless an explicitly reviewed change says otherwise; distinguish business/display IDs from `_id`.

## Suggested bounded implementation sequence

1. Inspect current instructions/status and present a small cross-project plan. Confirm source identity
   and graph-document provenance wire representation, including server-input versus ZIP behavior.
2. In Python, capture trusted hit `_index` and `_id` at ingestion and carry source identity separately
   from endpoint settings. Prevent incoming `_source` fields from overriding trusted provenance.
3. Preserve metadata through spaCy contexts/extensions, cached artifacts, phrase aggregation, graph
   merges, and serialization. Match document references by their full identity, not ID alone.
4. Extend the library graph-document reader; coordinate shared-schema changes only if required.
5. In backend, resolve original pipeline input context, hydrate via exact references, and add
   source/concrete-index properties plus collision-safe node identity. Choose a generated identity
   with tuple uniqueness or an unambiguous deterministic tuple key; do not concatenate unchecked
   components with an ambiguous delimiter.
6. Update all document-specific Neo4j repository queries, graph-to-document mapping, and highlighting
   joins to preserve exact references. Keep corpus separate from source/index.
7. Add persisted process-input conflict checking before cached pipeline reuse as a separately reviewed
   part of this work. Do not claim this is already solved by per-document provenance.

Avoid broad graph/phrase identity redesign. Pure graph endpoints need not acquire an Elasticsearch
index requirement merely because document-specific joins do.

## Acceptance checks

- Equal document IDs in two concrete indices/sources remain distinct through preprocessing, graph
  serialization, Java deserialization, and Neo4j storage/lookup.
- Offsets stay attached to the correct exact document throughout phrase merging.
- An alias ingestion records each hit's original concrete index. Hydration must not silently rebind
  old graph IDs after rollover or fan an ID out across multiple documents.
- Source-field IDs differing from `_id` do not make exact retrieval target the wrong document.
- Saved artifacts retain provenance after reload, including supported vector-store paths.
- Graph-derived Elasticsearch lookups and highlighting preserve references and existing access rules;
  a stored/client-provided reference does not itself grant authorization.
- ZIP ingestion remains selector-free and does not fabricate Elasticsearch references.
- Corpus/process names can differ from source/index names without changing document resolution.
- Resume with conflicting source/index cannot reuse old artifacts silently.

## Verification status and known compilation blockers

An earlier approved backend command was run using installed Maven 3.9.5:

```text
mvn -o -Dtest=DocumentSelectionTest test
```

It failed during **main compilation**, before any tests ran. Four delegate overrides still had old
signatures:

- `ConceptClusterApiDelegateImpl.java`: document-specific lookup.
- `PhraseApiDelegateImpl.java`: document-specific lookup.
- `ConceptPipelineApiDelegateImpl.java`: JSON and multipart starts.

Actual inspected generated signatures:

```text
getPhrasesByDocumentId(documentId, dataSource, index, include, corpusId, text, page)
getConceptClusterByDocumentId(documentId, dataSource, index, include, corpusId, page)
startConceptGraphPipeline(pipelineId, dataSourceId, index, skipPresent, returnStatistics,
                          language, data, labels, dataConfig, embeddingConfig,
                          clusteringConfig, graphConfig)
startConceptGraphPipelineWithJson(index, body, dataSource)
```

Do not merely add unused parameters to make compilation pass. Implement validation/propagation and
independent graph context. The JSON body remains a string. Pipeline payload settings conflicting with
explicit input must fail rather than being overwritten; ZIP is exempt from index requirements.

Formatting and `git diff --check` passed at the earlier backend implementation checkpoint. No Python
checks or new builds/tests were run during this investigation. Recheck current status/results before
reporting anything as passing.

## Starter prompt

> Read docs/neo4j-document-provenance-handoff.md and docs/index-selection-handoff.md. Inspect current
> instructions/status in backend, document-query, API, and the local Python concept-graphs repository.
> Propose the minimal coordinated source/index provenance propagation plan before editing. Keep
> existing IDs/API semantics where possible; no hash-based identity redesign, no guessed alias
> provenance, no production migration work, and no automatic database deletion. Confirm cross-project
> edit scope and verification commands. Report every changed file and distinguish tested behavior
> from unresolved pipeline-binding, alias-authorization, and ZIP graph-hydration questions.
