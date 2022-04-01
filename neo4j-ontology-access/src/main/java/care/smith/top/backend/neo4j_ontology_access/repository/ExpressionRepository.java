package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Expression;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpressionRepository
    extends PagingAndSortingRepository<Expression, Long>,
        CypherdslConditionExecutor<Expression>,
        CypherdslStatementExecutor<Expression> {}
