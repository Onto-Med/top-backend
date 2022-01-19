CREATE
    // Directories:
    (imise:Directory    { name: 'IMISE',    type: 'organisation', created_at: datetime(), description: 'Institute for Medical Informatics, Statistics and Epidemiology' }),
    (onto_med:Directory { name: 'Onto-Med', type: 'organisation', created_at: datetime() }),
    (nwg:Directory      { name: 'NWG TOP',  type: 'organisation', created_at: datetime() }),
    (smith:Directory    { name: 'SMITH',    type: 'project',      created_at: datetime() }),

    (onto_med) -[:HAS_SUPER_ORGANISATION]-> (imise),
    (nwg)      -[:HAS_SUPER_ORGANISATION]-> (onto_med),
    (nwg)      -[:IS_PART_OF]-> (smith),


    // Repositories:
    (public_repo:Repository { name: 'Public Repository', created_at: datetime(), is_primary: true },
    (help_repo:Repository   { name: 'HELP',              created_at: datetime(), description: 'Guideline-based Use of Antibiotics in Infectious Medicine' }),

    (public_repo) -[:BELONGS_TO]-> (imise),
    (help_repo)   -[:BELONGS_TO]-> (nwg),


    // Classes:
    (public_weight:Class     { uuid: apoc.create.uuid(), name: 'weight',          created_at: datetime() }),
    (private_weight:Class    { uuid: apoc.create.uuid(), name: 'weight',          created_at: datetime() }),
    (restricted_weight:Class { uuid: apoc.create.uuid(), name: 'weight_gt_100kg', created_at: datetime() }),

    (public_weight)     -[:HAS_ORIGIN]-> (public_repo),
    (private_weight)    -[:HAS_ORIGIN]-> (help_repo),
    (restricted_weight) -[:HAS_ORIGIN]-> (help_repo),
    (private_weight)    -[:IS_FORK_OF]-> (public_weight),


    // Relations
    (class_rel1:ClassRelation { index: 1 }) <-[:HAS_SUPERCLASS]- (public_weight),
    (class_rel2:ClassRelation { index: 1 }) <-[:HAS_SUPERCLASS]- (private_weight),
    (private_weight) -[:HAS_SUBCLASS]-> (class_rel3:ClassRelation { index: 1 }) <-[:HAS_SUPERCLASS]- (restricted_weight),

    (class_rel1) -[:BELONGS_TO]-> (public_repo),
    (class_rel2) -[:BELONGS_TO]-> (help_repo),
    (class_rel3) -[:BELONGS_TO]-> (help_repo),


    // Annotations:
    (public_weight) <-[:ANNOTATES]- (:Annotation { property: 'title',    value: 'Weight',  type: 'string', language: 'en', index: 1 }),
    (public_weight) <-[:ANNOTATES]- (:Annotation { property: 'title',    value: 'Gewicht', type: 'string', language: 'de', index: 2 }),
    (public_weight) <-[:ANNOTATES]- (:Annotation { property: 'unit',     value: 'kg',      type: 'string' }),
    (public_weight) <-[:ANNOTATES]- (:Annotation { property: 'datatype', value: 'decimal', type: 'string' }),

    (restricted_weight) <-[:ANNOTATES]- (:Annotation { property: 'title', value: 'Weight > 100 kg',  type: 'string', language: 'en', index: 1 }),
    (restricted_weight) <-[:ANNOTATES]- (:Annotation { property: 'title', value: 'Gewicht > 100 kg', type: 'string', language: 'de', index: 2 });


// Equivalent class versions (https://github.com/h-omer/neo4j-versioner-core):
CALL graph.versioner.get.current.state(private_weight) YIELD private_version
CALL graph.versioner.get.current.state(public_weight)  YIELD public_version
CREATE (private_version) -[:IS_EQUIVALENT_TO]-> (public_version);
