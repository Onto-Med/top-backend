package care.smith.top.backend.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.backend.service.nlp.QueryExpansionService;
import care.smith.top.backend.util.AbstractJpaTest;
import care.smith.top.model.DataSource;
import care.smith.top.model.Organisation;
import care.smith.top.model.QueryExpansionEffectiveConfig;
import care.smith.top.model.QueryExpansionRelation;
import care.smith.top.model.QueryType;
import care.smith.top.top_document_query.adapter.config.QueryExpansionConfig;
import care.smith.top.top_document_query.adapter.config.QueryExpansionRelationConfig;
import care.smith.top.top_document_query.adapter.config.TextAdapterConfig;
import care.smith.top.top_document_query.concept_graphs_api.model.QueryExpansionProfileEntity;
import care.smith.top.top_document_query.concept_graphs_api.model.QueryExpansionProfileRelationEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class QueryApiDelegateImplTest extends AbstractJpaTest {
  @Autowired private QueryApiDelegateImpl queryApi;
  @Autowired private OrganisationApiDelegateImpl organisationApi;

  @Test
  void getDataSources() {
    assertThat(queryApi.getDataSources(null).getBody()).isNotNull().size().isEqualTo(3);
    assertThat(queryApi.getDataSources(QueryType.PHENOTYPE).getBody())
        .isNotNull()
        .size()
        .isEqualTo(2);
  }

  @Test
  void getDataSourceQueryExpansionConfigReturnsEffectiveSemanticRelationIntersection() {
    DocumentQueryService documentQueryService = mock(DocumentQueryService.class);
    QueryExpansionService queryExpansionService = mock(QueryExpansionService.class);
    QueryExpansionApiDelegateImpl delegate = new QueryExpansionApiDelegateImpl();
    ReflectionTestUtils.setField(delegate, "documentQueryService", documentQueryService);
    ReflectionTestUtils.setField(delegate, "queryExpansionService", queryExpansionService);

    TextAdapterConfig adapterConfig = new TextAdapterConfig();
    QueryExpansionConfig queryExpansionConfig = new QueryExpansionConfig();
    queryExpansionConfig.setProfile("medical_de");
    queryExpansionConfig.setRelations(
        Map.of(
            "treated_by", relationConfig("require_context"),
            "not_in_profile", relationConfig("alternatives")));
    adapterConfig.setQueryExpansion(queryExpansionConfig);
    when(documentQueryService.getTextAdapterConfig("documents"))
        .thenReturn(Optional.of(adapterConfig));

    QueryExpansionProfileEntity profile =
        new QueryExpansionProfileEntity();
    profile.setRelations(
        List.of(
            profileRelation(
                "treated_by",
                "Treated by",
                "A diagnosis may be treated by a medication or procedure.",
                List.of("diagnosis"),
                List.of("medication", "procedure")),
            profileRelation(
                "profile_only",
                "Profile only",
                "Not configured by this data source.",
                List.of("diagnosis"),
                List.of("finding"))));
    when(queryExpansionService.getProfile("medical_de")).thenReturn(Optional.of(profile));

    ResponseEntity<QueryExpansionEffectiveConfig> response =
        delegate.getDataSourceQueryExpansionConfig("documents");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    QueryExpansionEffectiveConfig body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getProfile()).isEqualTo("medical_de");
    assertThat(body.getRelations()).hasSize(1);

    QueryExpansionRelation relation = body.getRelations().getFirst();
    assertThat(relation.getId()).isEqualTo("treated_by");
    assertThat(relation.getLabel()).isEqualTo("Treated by");
    assertThat(relation.getDescription())
        .isEqualTo("A diagnosis may be treated by a medication or procedure.");
    assertThat(relation.getSourceCategories()).containsExactly("diagnosis");
    assertThat(relation.getTargetCategories()).containsExactly("medication", "procedure");
  }

  @Test
  void getDataSourceQueryExpansionConfigReturnsNotFoundForMissingQueryExpansionConfig() {
    DocumentQueryService documentQueryService = mock(DocumentQueryService.class);
    QueryExpansionApiDelegateImpl delegate = new QueryExpansionApiDelegateImpl();
    ReflectionTestUtils.setField(delegate, "documentQueryService", documentQueryService);
    when(documentQueryService.getTextAdapterConfig("documents"))
        .thenReturn(Optional.of(new TextAdapterConfig()));

    assertThatThrownBy(() -> delegate.getDataSourceQueryExpansionConfig("documents"))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
  }

  @Test
  void getOrganisationDataSources() {
    Organisation organisation = new Organisation().id("orga");
    DataSource dataSource =
        new DataSource().id("Test_Data_Source_1").queryType(QueryType.PHENOTYPE);

    assertThatThrownBy(
            () -> queryApi.getOrganisationDataSources("does not exist", null).getStatusCode())
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThat(organisationApi.createOrganisation(organisation, null).getStatusCode())
        .isEqualTo(HttpStatus.CREATED);

    assertThat(queryApi.getOrganisationDataSources(organisation.getId(), null).getBody())
        .isNotNull()
        .isEmpty();

    assertThatThrownBy(() -> queryApi.addOrganisationDataSource("does not exist", dataSource))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatThrownBy(() -> queryApi.removeOrganisationDataSource("does not exist", dataSource))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatThrownBy(
            () -> queryApi.removeOrganisationDataSource(organisation.getId(), dataSource))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThat(queryApi.addOrganisationDataSource(organisation.getId(), dataSource).getStatusCode())
        .isEqualTo(HttpStatus.CREATED);

    assertThat(queryApi.getOrganisationDataSources(organisation.getId(), null).getBody())
        .isNotNull()
        .allSatisfy(
            ds -> {
              assertThat(ds.getId()).isEqualTo(dataSource.getId());
              assertThat(ds.getQueryType()).isEqualTo(QueryType.PHENOTYPE);
            });

    assertThat(
            queryApi
                .getOrganisationDataSources(organisation.getId(), QueryType.PHENOTYPE)
                .getBody())
        .isNotNull()
        .allSatisfy(ds -> assertThat(ds.getId()).isEqualTo(dataSource.getId()));

    assertThat(
            queryApi.getOrganisationDataSources(organisation.getId(), QueryType.CONCEPT).getBody())
        .isNotNull()
        .isEmpty();

    assertThat(
            queryApi.removeOrganisationDataSource(organisation.getId(), dataSource).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);

    assertThat(queryApi.getOrganisationDataSources(organisation.getId(), null).getBody())
        .isNotNull()
        .isEmpty();
  }

  private QueryExpansionRelationConfig relationConfig(String strategy) {
    QueryExpansionRelationConfig config = new QueryExpansionRelationConfig();
    config.setStrategy(strategy);
    return config;
  }

  private QueryExpansionProfileRelationEntity profileRelation(
      String id,
      String label,
      String description,
      List<String> sourceCategories,
      List<String> targetCategories) {
    QueryExpansionProfileRelationEntity relation =
        new QueryExpansionProfileRelationEntity();
    relation.setId(id);
    relation.setLabel(label);
    relation.setDescription(description);
    relation.setSourceCategories(sourceCategories);
    relation.setTargetCategories(targetCategories);
    return relation;
  }
}
