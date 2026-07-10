package io.nebula.data.mongodb.template;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.ExecutableFindOperation;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.NearQuery;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoTemplateTest {

    @Mock
    private MongoOperations mongoOperations;

    @Mock
    private IndexOperations indexOperations;

    @Mock
    private ExecutableFindOperation.ExecutableFind<TestDocument> executableFind;

    @Mock
    private ExecutableFindOperation.FindWithProjection<TestDocument> findWithProjection;

    @Mock
    private ExecutableFindOperation.TerminatingFindNear<TestDocument> terminatingFindNear;

    private MongoTemplate<TestDocument, String> template;

    @BeforeEach
    void setUp() {
        when(mongoOperations.getCollectionName(TestDocument.class)).thenReturn("documents");
        template = new MongoTemplate<>(mongoOperations, TestDocument.class);
    }

    @Test
    void saveAllUsesUpsertSemantics() {
        TestDocument first = new TestDocument("1", "first");
        TestDocument second = new TestDocument("2", "second");
        when(mongoOperations.save(first, "documents")).thenReturn(first);
        when(mongoOperations.save(second, "documents")).thenReturn(second);

        assertThat(template.saveAll(List.of(first, second))).containsExactly(first, second);

        verify(mongoOperations).save(first, "documents");
        verify(mongoOperations).save(second, "documents");
        verify(mongoOperations, never()).insert(anyCollection(), eq("documents"));
    }

    @Test
    void insertAllUsesSingleBatchInsert() {
        TestDocument first = new TestDocument("1", "first");
        TestDocument second = new TestDocument("2", "second");
        List<TestDocument> documents = List.of(first, second);
        when(mongoOperations.insert(documents, "documents")).thenReturn(documents);

        assertThat(template.insertAll(documents)).containsExactly(first, second);

        verify(mongoOperations).insert(documents, "documents");
        verify(mongoOperations, never()).save(any(), eq("documents"));
    }

    @Test
    void textSearchBuildsTextQuery() {
        when(mongoOperations.find(any(Query.class), eq(TestDocument.class), eq("documents")))
                .thenReturn(List.of());

        template.findByText("nebula framework");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoOperations).find(queryCaptor.capture(), eq(TestDocument.class), eq("documents"));
        assertThat(queryCaptor.getValue().getQueryObject().get("$text")).isNotNull();
        assertThat(queryCaptor.getValue().getSortObject().get("score")).isEqualTo(new Document("$meta", "textScore"));
    }

    @Test
    void aggregationExecutesSupportedPipelineStages() {
        TestProjection projection = new TestProjection("nebula");
        when(mongoOperations.aggregate(any(Aggregation.class), eq("documents"), eq(TestProjection.class)))
                .thenReturn(new AggregationResults<>(List.of(projection), new Document()));

        List<TestProjection> result = template.aggregate(
                List.of(Map.of("$match", Map.of("title", "nebula")), "{ $limit: 1 }"),
                TestProjection.class);

        assertThat(result).containsExactly(projection);
        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongoOperations).aggregate(captor.capture(), eq("documents"), eq(TestProjection.class));
        assertThat(captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT))
                .containsExactly(
                        new Document("$match", new Document("title", "nebula")),
                        new Document("$limit", 1));
    }

    @Test
    void aggregationRejectsUnknownStageType() {
        assertThatThrownBy(() -> template.aggregate(List.of(42), TestProjection.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的聚合阶段类型");
    }

    @Test
    void createsTextAndGeoIndexes() {
        when(mongoOperations.indexOps("documents")).thenReturn(indexOperations);

        template.createTextIndex("title", "body");
        template.createGeoIndex("location");

        ArgumentCaptor<IndexDefinition> captor = ArgumentCaptor.forClass(IndexDefinition.class);
        verify(indexOperations, org.mockito.Mockito.times(2)).createIndex(captor.capture());
        assertThat(captor.getAllValues().get(0).getIndexKeys())
                .containsEntry("title", "text")
                .containsEntry("body", "text");
        assertThat(captor.getAllValues().get(1).getIndexKeys())
                .containsEntry("location", "2dsphere");
    }

    @Test
    void limitingQueryDoesNotMutateCallerQuery() {
        when(mongoOperations.find(any(Query.class), eq(TestDocument.class), eq("documents")))
                .thenReturn(List.of());
        Query original = new Query();

        template.findWithSkipAndLimit(original, 10, 20);

        assertThat(original.getSkip()).isZero();
        assertThat(original.isLimited()).isFalse();
    }

    @Test
    void nearQueryUsesFluentApi() {
        GeoResults<TestDocument> expected = new GeoResults<>(List.of());
        when(mongoOperations.query(TestDocument.class)).thenReturn(executableFind);
        when(executableFind.inCollection("documents")).thenReturn(findWithProjection);
        when(findWithProjection.near(any(NearQuery.class))).thenReturn(terminatingFindNear);
        when(terminatingFindNear.all()).thenReturn(expected);

        GeoResults<TestDocument> result = template.findNear(
                new Point(121.47, 31.23), new Distance(5, Metrics.KILOMETERS));

        assertThat(result).isSameAs(expected);
        verify(terminatingFindNear).all();
    }

    private record TestDocument(String id, String title) {
    }

    private record TestProjection(String title) {
    }
}
