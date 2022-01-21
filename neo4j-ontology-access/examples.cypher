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
    (public_repo:Repository { name: 'Public Repository', created_at: datetime(), is_primary: true }),
    (help_repo:Repository   { name: 'HELP',              created_at: datetime(), description: 'Guideline-based Use of Antibiotics in Infectious Medicine' }),

    (public_repo) -[:BELONGS_TO]-> (imise),
    (help_repo)   -[:BELONGS_TO]-> (nwg)

WITH public_repo, help_repo

// Classes:
CALL graph.versioner.init('Class', { uuid: randomUUID() }, { name: 'weight', version: 1 }) YIELD node AS public_weight
CALL graph.versioner.update(public_weight, { name: 'body_weight', version: 2 }) YIELD node
CALL graph.versioner.init('Class', { uuid: randomUUID() }, { name: 'weight', version: 1 }) YIELD node AS private_weight
CALL graph.versioner.init('Class', { uuid: randomUUID() }, { name: 'weight_gt_100kg', version: 1 }) YIELD node AS restricted_weight

CALL graph.versioner.get.nth.state(public_weight, 1) YIELD node AS public_version
CALL graph.versioner.get.current.state(private_weight) YIELD node AS private_version
CALL graph.versioner.get.current.state(restricted_weight) YIELD node AS restricted_version

// Annotations:
CREATE
    (private_version) -[:IS_EQUIVALENT_TO]-> (public_version),

    (public_version) -[:HAS_ANNOTATION]-> (:Annotation:String:Title    { value: 'Weight',  language: 'en', index: 1 }),
    (public_version) -[:HAS_ANNOTATION]-> (:Annotation:String:Title    { value: 'Gewicht', language: 'de', index: 2 }),
    (public_version) -[:HAS_ANNOTATION]-> (:Annotation:String:Unit     { value: 'kg' }),
    (public_version) -[:HAS_ANNOTATION]-> (:Annotation:String:Datatype { value: 'decimal' }),

    (restricted_version) -[:HAS_ANNOTATION]-> (:Annotation:String:Title { value: 'Weight > 100 kg', language: 'en', index: 1 }),
    (restricted_version) -[:HAS_ANNOTATION]-> (:Annotation:String:Title { value: 'Gewicht > 100 kg', language: 'de', index: 2 })

CREATE
    // (public_weight)     -[:HAS_ORIGIN]-> (public_repo),
    // (private_weight)    -[:HAS_ORIGIN]-> (help_repo),
    // (restricted_weight) -[:HAS_ORIGIN]-> (help_repo),
    (private_weight) -[:IS_FORK_OF]-> (public_weight),


    // Relations
    (class_rel1:ClassRelation { index: 1 }) <-[:HAS_SUPERCLASS]- (public_weight),
    (class_rel2:ClassRelation { index: 1 }) <-[:HAS_SUPERCLASS]- (private_weight),
    (private_weight) -[:HAS_SUBCLASS]-> (class_rel3:ClassRelation { index: 1 }) <-[:HAS_SUPERCLASS]- (restricted_weight),

    (class_rel1) -[:BELONGS_TO]-> (public_repo),
    (class_rel2) -[:BELONGS_TO]-> (help_repo),
    (class_rel3) -[:BELONGS_TO]-> (help_repo)

// example vor versioned annotation
WITH public_weight
CALL graph.versioner.init('Annotation', { value: 'Weight',  language: 'en', index: 1, property: 'title', datatype: 'string' }) YIELD node AS title_en
CALL graph.versioner.relationship.create(public_weight, title_en, 'HAS_ANNOTATION') YIELD relationship RETURN relationship