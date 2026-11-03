# Backend handoff: explicit document index selection

## Task

Implement the backend integration for the explicit document-index contract already committed in the
sibling `top-api` repository. Work in **top-backend**. Inspect repository instructions, README and
`git status` first; preserve existing changes. Present a bounded implementation plan before editing.

Do not change frontend, Python or document-query code automatically. Reuse the library functionality
already implemented; report concrete blockers and coordinated follow-up instead of broadening scope.
Do not redesign the shared API without agreement. Never hand-edit generated API classes.

## Agreed behavior (not optional compatibility proposals)

- A source can advertise several configured indices/aliases. Each browsing/query context selects
  **one**. No multi-select, implicit search across all choices, first-index fallback or global selection.
- **The client must send index even when the source has only one choice.** The backend must validate
  and propagate it. Do not rely on the library's sole-choice defaults.
- Document browsing, import, exact-document lookups and JSON pipeline starts require scalar `index`.
- `ConceptQuery.index` is required; `PhenotypeQuery` has no document-index requirement.
- Persist concept query selection. Saved-result pages restore original context and have no index override.
- Historical queries without index require explicit migration before conforming to the new contract.
  Do not automatically backfill the current sole/first index or silently rebind historical results.
- Exact identity is `(dataSourceId, concrete index, documentId)`. A selected alias can return documents
  from a concrete backing index. Details use their original reference, not current UI selection.
- Imports have one explicit destination, subject to configured allowlist and existing creation policy.
- Pipeline server input always requires index. **ZIP pipeline ingestion is exempt**: uploaded documents
  are forwarded as pipeline input; the inspected backend path does not import them into Elasticsearch.
  Do not add an Elasticsearch destination or upload-to-index feature as part of this task.
- Source IDs, Elasticsearch indices, pipeline/process IDs and graph corpus IDs are distinct.
  Marqo embedding/RAG indices are not document-source choices.

## Current snapshot: recheck before implementation

Read-only inspection found:

| Project | Current local version/dependency |
| --- | --- |
| top-api project | pom.xml 0.15.0; schema info.version 0.14.1 |
| top-api generator | OpenAPI Generator 7.17.0 |
| top-backend | 0.10.1; depends on top-api 0.14.1 and top-document-query 0.9.5 |
| top-document-query project | 0.9.6; depends on top-api 0.14.1 |

The user says they pushed the API changes and the **publish snapshot workflow completed without errors**.
The local API HEAD inspected here was `fc3819b` ("added explicit document index selection contract").
Neither workflow steps nor published artifact coordinates were fetched or independently verified.
**Do not assume the published version is 0.15.0-SNAPSHOT, or that the backend already consumes it.**
Confirm the actual available coordinate with the user or existing offline artifact metadata. Do not
choose a release/version bump silently. Report the current API pom/schema version discrepancy; it is
not authorization to change either version. Align backend dependencies with artifacts containing the
inspected changes, and check compatibility with the library's older shared API dependency/exclusions.

No relevant local OKF/knowledge folder was found in these two Java repositories. Consulted local
technical context: `../top-document-query/docs/index.md`, `docs/index-selection-impact.md`, current
library/backend source, and the backend README. Prefer current source: some older inventory text still
proposes optional selection or replacing the generic result map, which this task does not authorize.

## Authoritative API contract

Read `../top-api/schemas/top-api.yml`. `../top-api/INDEX_SELECTION.md` contains migration notes but may
still be untracked locally; the handoff does not depend on that document being available.

The schema now contains:

- Optional `DataSource.indices`: unique authorized configured choices, not current selection. No
  endpoints, ports, credentials or complete adapter configuration in discovery.
- Reusable scalar `DocumentIndex` (string, minLength 1) and **one required query parameter**
  `components.parameters.documentIndex`, wire name `index`. There is no separate documentReferenceIndex.
- `DocumentReference`: required nonblank dataSourceId/index/documentId.
- Optional **read-only** `Document.reference`. Preserve existing Document fields. In responses,
  reference.documentId must equal Document.id; populate provenance as backend paths migrate. Imports
  reuse Document but must not require or trust response-only provenance as destination selection.
- Required `ConceptQuery.index`, not Query.index.
- Required index on getDocuments, importDocuments, getSingleDocumentById, getPhrasesByDocumentId,
  getConceptClusterByDocumentId and startConceptGraphPipelineWithJson.
- Optional `corpusId` on document-specific phrase/cluster lookups, with ambiguous omission rejected
  unless an unambiguous persisted pipeline binding resolves it. Unrelated graph endpoints retain
  their existing corpus omission behavior.
- Multipart ConceptPipelineRequest.index: schema-optional only to accommodate ZIP ingestion;
  runtime-required for server input.
- JSON pipeline start: optional dataSource query parameter and required index. The request body is
  still ConceptPipelineConfigRequest **type string**, not a redesigned JSON object.
- Reusable HTTP 400 InvalidDocumentSelection using ErrorResponse (code/message).
- getDocumentsForQuery has **no override index**. getQueryResultIds remains the generic map.
  RAGAnswer.info remains an opaque string.

Inspect the actual generated Java delegate signatures from the selected artifact; do not guess argument
order. Read-only fields are response semantics, not a guarantee that incoming Java objects cannot carry them.

## Existing document-query implementation: reuse, do not rebuild

Under `../top-document-query/src/main/java/care/smith/top/top_document_query/`:

- `adapter/IndexSelection.java`: immutable scopes, configured-choice validation, singleIndex(). V2
  unscoped reads still permit a sole configured index; legacy v1 behavior is also retained. These
  library defaults need not be removed: require explicit selection at the backend boundary.
- `adapter/TextAdapter.java`: forIndex/forIndices, getDocument(DocumentReference, simplified),
  importDocuments(index, documents, language), provenance-preserving indexed read entry points.
- `adapter/elasticsearch/ElasticsearchAdapter.java`: scoped reads/imports/counts; index creation and
  bulk writes use the selected destination; query hits include index/source; PIT batching/pagination.
- `adapter/DocumentReference.java`, `IndexedDocument.java`, `DocumentHit.java`: exact identity and
  wrappers around the existing shared API Document. Map these to API provenance in the backend.
- Exact-reference Elasticsearch lookup validates source identity and searches the permitted scope
  with both document ID and concrete _index filters. It already supports configured aliases; simply
  calling forIndex(concreteIndex) can incorrectly reject a concrete name absent from the allowlist.
- `converter/csv/DocumentCSV.java`, `CSVDataRecord.java`: index/source columns and exact-reference
  reading already exist, including duplicate IDs in different indices. Legacy column APIs remain.
- V1/v2 configuration loading and ordered endpoint support already exist. Full service fallback
  propagation is a separate integration task, not part of scalar selection.

Tests exist for independent scopes, allowlist escape rejection, scoped pages/import creation, duplicate
IDs, alias references, PIT cleanup and CSV provenance. They were inspected, not rerun in this handoff.
Library docs record previously passing local checks and user-reported CI; do not present that as a new run.

## Backend source map and required changes

Paths below are under `src/main/java/care/smith/top/backend/`.

### 1. Document requests and response provenance

- `api/nlp/DocumentApiDelegateImpl.java`: currently accepts no index, obtains an unscoped adapter,
  browses all filter branches with plain Document methods, uses ID-only details and legacy imports.
- `service/nlp/DocumentService.java`: getAdapterForDataSource is cached by source; getAdapterFromQuery
  is cached by query identity. Keep a reusable root adapter and create a scoped view per operation,
  or key scoped caches correctly. Never mutate cached config or store a user's current selection.
- Use the explicit request selection before **every** name/ID/graph-filter/page read branch.
- Use provenance-preserving indexed reads and map wrappers to Document.reference. Inspect
  `util/ApiModelMapper.java` and `util/nlp/DocumentRepresentation.java`: highlighting/rebuilding must
  preserve provenance. Do not silently drop it when mapping or reconstructing documents.
- Imports pass the explicit destination to the library. A permitted not-yet-created destination is
  different from a disallowed name. Reject missing/blank/repeated/list-like input appropriately.
- Exact details should call the library's reference-aware lookup, not merely an ID-only read against
  the currently selected alias. Preserve missing-document behavior without switching indices.

### 2. Query persistence, execution and saved results

- `model/jpa/QueryDao.java`: currently stores source/entity/language but no index; constructor and
  toApiModel need index preservation. Include a reviewed **Liquibase migration**, following README
  conventions; do not apply migrations to real databases without approval.
- Historical DB rows may lack a binding. Plan storage nullability/migration separately from the new
  API's required field. Do not return misleading schema-conforming objects with guessed index values.
  Propose/review the explicit migration or unavailable-result behavior before implementing a new flow.
- `service/nlp/DocumentQueryService.java`: enqueue validation, executeQuery and getTextAdapter must
  validate, persist and restore one index. Scope the adapter supplied to TextFinder.
- textAdapterConfigToDataSource currently exposes no indices: add only authorized configured choices,
  preserving organisation/source access rules. Check discovery and execution authorization separately.
- getDocumentIdsAndOffsets currently reads ID/offset columns into an ID-keyed map. Saved document
  pages reconstruct ID-only reads. Restore original query/result context and use existing CSV
  provenance where needed; do not substitute the current browsing selection.
- Keep the generic resultIds endpoint shape. Do not silently merge colliding IDs; see alias limits below.

### 3. Pipelines and graph lookups

- `api/nlp/ConceptPipelineApiDelegateImpl.java`: createDocumentServerConfigMap currently forwards
  Arrays.stream(config.getIndex()).findFirst(). JSON start derives source lookup from payload name.
  Multipart already has separate pipelineId/dataSourceId. Replace these accidental bindings with the
  explicit source/index context; JSON and multipart server input must behave consistently.
- Preserve the existing string JSON body. Reject conflicting payload versus explicit input settings;
  do not silently overwrite one with another. ZIP ingestion needs no Elasticsearch selector.
- `service/nlp/ConceptGraphsService.java`: retain original process-to-input binding and check resume
  conflicts before cached artifacts can be reused. Inspect existing persistence/config retrieval
  before designing storage. If Python changes are necessary, document the exact interface gap and
  ask for coordination rather than claiming backend-only resume safety.
- `service/nlp/ConceptClusterService.java`: review createSpecificGraphsInNeo4j/createGraphInNeo4j
  hydration so graph-derived IDs resolve against original pipeline source/index, not an assumed source.
- `api/nlp/PhraseApiDelegateImpl.java`, `ConceptClusterApiDelegateImpl.java`: consume original document
  index and separate corpusId. Existing code derives corpus from source; reject ambiguous graph context
  or resolve via persisted binding. Do not invent a source/index-to-pipeline naming convention.

### 4. Validation and errors

Required/minLength schema annotations do not implement live allowlist checks or storage binding.
Reject missing index even with one configured choice; reject empty/whitespace-only and disallowed values.
Do not silently trim document identities. Preserve authentication/authorization and use the documented
ErrorResponse-based 400 cases. Review current exception handlers and pipeline errors (which currently
use other response handling). Return missing resources without silent index fallback.

## Alias limitations: do not silently solve by inventing policy

- One selected alias can cover several backing indices. A generic ID-keyed result map cannot represent
  duplicate IDs faithfully. The API preserves that map; supported alias constraints versus a richer
  result contract need an explicit decision before claiming full support for that case.
- Persisting only an alias name is insufficient for original saved results after rollover. Retain
  concrete provenance/context or reject unavailable history, never resolve old IDs against new targets.
- The library authorizes exact references through the currently permitted scope. After rollover an old
  backing index may no longer be reachable that way. Historical authorization/retention needs an explicit
  policy; do not bypass source access or assume a client-provided reference grants access.
- Resume persistence/binding representation is not fully specified by the API. Inspect existing mechanisms,
  present a minimal proposal and surface coordinated API/Python needs before a breaking redesign.

## Suggested implementation sequence

1. Read instructions/status/source and confirm published dependency coordinates and offline tooling.
2. Inspect generated delegate signatures; report expected compatibility and historical-data decisions.
3. Add focused tests and migrate discovery + document browse/import/details using existing scoped APIs.
4. Add query index persistence with reviewed migration, execute scope and saved-result restoration.
5. Bind both pipeline starts consistently, then graph lookups/resume to the original context.
6. Compile/test with approved local tooling; document unresolved cross-project decisions and frontend work.

No downloads/network actions, credential access, migrations, full builds/tests, global installs, publishing
or pushes without appropriate approval. Ask before costly/container suites. Prefer installed offline tools;
a missing cached artifact is a blocker, not permission to fetch it. Follow Google Java Style and the
README's --skip-reflowing-long-strings formatting convention. Do not reformat unrelated files.

## Acceptance checks

- Missing index fails even for a sole-choice source; explicit allowed choice succeeds; disallowed/blank
  choices fail. Two concurrent selections do not change shared config or contaminate each other.
- Every browse/filter/page branch uses selected scope. Same document ID in different indices yields
  different references; details and highlighting preserve the original one.
- Imports create/write only the explicit permitted destination; response-only reference is not required.
- Discovery exposes authorized choices only, with no credentials or adapter transport configuration.
- Concept query source/index survives DAO round-trip, execution and result reload; phenotype behavior
  is unchanged. Saved pages accept no replacement scope. Historical rows are not silently backfilled.
- Query CSV provenance is not lost; generic result-map limitations are explicit, not hidden collisions.
- JSON and multipart server starts pass the same selected input, independent of process name; conflicts
  fail. ZIP ingestion remains selector-free. Resume cannot silently use a different input.
- Document graph lookups use original identity and independent, unambiguous graph context.
- Backend compiles against the actual new API artifact. Tests cover HTTP boundary validation and
  library integration, not only mocked method signatures. Preserve unrelated tests/features.

## Deliverable

Report every changed file, dependency coordinates, migration/legacy-data decisions, tests and exact
commands/results. Distinguish implemented runtime behavior from unresolved alias/resume decisions and
frontend/Python follow-up. Do not claim end-to-end UI readiness or independently verified publication.

## Starter prompt for a backend agent

> Read docs/index-selection-handoff.md, repository instructions and git status. Inspect the linked API
> contract and existing document-query implementation. Propose the bounded backend integration plan,
> identify dependency and historical-data decisions needing confirmation, then implement incrementally
> with focused tests. Do not modify sibling projects or run network/build/migration actions without approval.
