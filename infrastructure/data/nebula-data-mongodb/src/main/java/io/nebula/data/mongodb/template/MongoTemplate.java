package io.nebula.data.mongodb.template;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import io.nebula.data.mongodb.repository.MongoRepository;
import org.bson.conversions.Bson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.data.mongodb.core.query.Update;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * MongoDB模板类
 * 实现MongoRepository接口，提供MongoDB操作的具体实现
 * 
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public class MongoTemplate<T, ID extends Serializable> implements MongoRepository<T, ID> {
    
    private final MongoOperations mongoOperations;
    private final Class<T> entityClass;
    private final String collectionName;
    
    public MongoTemplate(MongoOperations mongoOperations, Class<T> entityClass) {
        this.mongoOperations = mongoOperations;
        this.entityClass = entityClass;
        this.collectionName = mongoOperations.getCollectionName(entityClass);
    }
    
    public MongoTemplate(MongoOperations mongoOperations, Class<T> entityClass, String collectionName) {
        this.mongoOperations = mongoOperations;
        this.entityClass = entityClass;
        this.collectionName = collectionName;
    }
    
    // ========== 基础CRUD操作 ==========
    
    @Override
    public Optional<T> findById(ID id) {
        T result = mongoOperations.findById(id, entityClass, collectionName);
        return Optional.ofNullable(result);
    }
    
    @Override
    public List<T> findAll() {
        return mongoOperations.findAll(entityClass, collectionName);
    }
    
    @Override
    public Page<T> findAll(Pageable pageable) {
        Query query = new Query().with(pageable);
        List<T> list = mongoOperations.find(query, entityClass, collectionName);
        long total = mongoOperations.count(new Query(), entityClass, collectionName);
        return new PageImpl<>(list, pageable, total);
    }
    
    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        Query query = new Query(Criteria.where("id").in(ids));
        return mongoOperations.find(query, entityClass, collectionName);
    }
    
    @Override
    public <S extends T> S save(S entity) {
        return mongoOperations.save(entity, collectionName);
    }
    
    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(entity -> mongoOperations.save(entity, collectionName))
                .toList();
    }

    @Override
    public <S extends T> List<S> insertAll(Collection<? extends S> entities) {
        return new ArrayList<>(mongoOperations.insert(entities, collectionName));
    }
    
    @Override
    public <S extends T> S saveAndFlush(S entity) {
        // MongoDB不需要flush操作
        return save(entity);
    }
    
    @Override
    public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {
        // MongoDB不需要flush操作
        return saveAll(entities);
    }
    
    @Override
    public void deleteById(ID id) {
        mongoOperations.remove(Query.query(Criteria.where("id").is(id)), entityClass, collectionName);
    }
    
    @Override
    public void delete(T entity) {
        mongoOperations.remove(entity, collectionName);
    }
    
    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        entities.forEach(this::delete);
    }
    
    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        mongoOperations.remove(Query.query(Criteria.where("id").in(ids)), entityClass, collectionName);
    }
    
    @Override
    public void deleteAll() {
        mongoOperations.remove(new Query(), entityClass, collectionName);
    }
    
    @Override
    public boolean existsById(ID id) {
        return mongoOperations.exists(Query.query(Criteria.where("id").is(id)), entityClass, collectionName);
    }
    
    @Override
    public long count() {
        return mongoOperations.count(new Query(), entityClass, collectionName);
    }
    
    @Override
    public void flush() {
        // MongoDB不需要flush操作
    }
    
    @Override
    public void deleteInBatch(Iterable<T> entities) {
        deleteAll(entities);
    }
    
    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }
    
    @Override
    public void deleteAllByIdInBatch(Iterable<ID> ids) {
        deleteAllById(ids);
    }
    
    @Override
    public T getReference(ID id) {
        return findById(id).orElse(null);
    }
    
    @Override
    public T getReferenceById(ID id) {
        return getReference(id);
    }
    
    // ========== MongoDB特有操作（简化版） ==========
    
    @Override
    public List<T> findByField(String field, Object value) {
        Query query = Query.query(Criteria.where(field).is(value));
        return mongoOperations.find(query, entityClass, collectionName);
    }
    
    @Override
    public Optional<T> findFirstByField(String field, Object value) {
        Query query = Query.query(Criteria.where(field).is(value)).limit(1);
        T result = mongoOperations.findOne(query, entityClass, collectionName);
        return Optional.ofNullable(result);
    }
    
    @Override
    public List<T> findByCriteria(Criteria criteria) {
        Query query = Query.query(criteria);
        return mongoOperations.find(query, entityClass, collectionName);
    }
    
    @Override
    public List<T> find(Query query) {
        return mongoOperations.find(query, entityClass, collectionName);
    }
    
    @Override
    public Optional<T> findFirst(Query query) {
        T result = mongoOperations.findOne(Query.of(query).limit(1), entityClass, collectionName);
        return Optional.ofNullable(result);
    }
    
    @Override
    public Optional<T> findOne(Query query) {
        T result = mongoOperations.findOne(query, entityClass, collectionName);
        return Optional.ofNullable(result);
    }
    
    @Override
    public List<T> findByRegex(String field, String pattern) {
        Query query = Query.query(Criteria.where(field).regex(pattern));
        return mongoOperations.find(query, entityClass, collectionName);
    }
    
    @Override
    public List<T> findByText(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            throw new IllegalArgumentException("searchText 不能为空");
        }
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matching(searchText);
        Query query = TextQuery.queryText(criteria).sortByScore();
        return mongoOperations.find(query, entityClass, collectionName);
    }
    
    @Override
    public List<T> findByRange(String field, Object start, Object end) {
        Query query = Query.query(Criteria.where(field).gte(start).lte(end));
        return mongoOperations.find(query, entityClass, collectionName);
    }
    
    @Override
    public List<T> findByIn(String field, List<?> values) {
        Query query = Query.query(Criteria.where(field).in(values));
        return mongoOperations.find(query, entityClass, collectionName);
    }
    
    @Override
    public List<T> findAllSorted(Sort sort) {
        Query query = new Query().with(sort);
        return mongoOperations.find(query, entityClass, collectionName);
    }
    
    @Override
    public List<T> findWithLimit(Query query, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit 必须大于 0");
        }
        return mongoOperations.find(Query.of(query).limit(limit), entityClass, collectionName);
    }
    
    @Override
    public List<T> findWithSkipAndLimit(Query query, int skip, int limit) {
        if (skip < 0) {
            throw new IllegalArgumentException("skip 不能小于 0");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit 必须大于 0");
        }
        return mongoOperations.find(Query.of(query).skip(skip).limit(limit), entityClass, collectionName);
    }
    
    @Override
    public <R> List<R> aggregate(List<Object> pipeline, Class<R> resultClass) {
        Objects.requireNonNull(pipeline, "pipeline 不能为空");
        Objects.requireNonNull(resultClass, "resultClass 不能为空");

        List<AggregationOperation> operations = pipeline.stream()
                .map(this::toAggregationOperation)
                .toList();
        AggregationResults<R> results = mongoOperations.aggregate(
                Aggregation.newAggregation(operations), collectionName, resultClass);
        return results.getMappedResults();
    }
    
    @Override
    public GeoResults<T> findNear(Point point, Distance distance) {
        NearQuery nearQuery = NearQuery.near(point).maxDistance(distance);
        return mongoOperations.query(entityClass)
                .inCollection(collectionName)
                .near(nearQuery)
                .all();
    }
    
    @Override
    public List<T> findWithin(Point point, Distance distance) {
        Circle circle = new Circle(point, distance);
        Query query = Query.query(Criteria.where("location").withinSphere(circle));
        return mongoOperations.find(query, entityClass, collectionName);
    }
    
    // ========== 更新操作 ==========
    
    @Override
    public long updateField(Query query, String field, Object value) {
        Update update = Update.update(field, value);
        UpdateResult result = mongoOperations.updateMulti(query, update, entityClass, collectionName);
        return result.getModifiedCount();
    }
    
    @Override
    public long updateFields(Query query, java.util.Map<String, Object> updates) {
        Update update = new Update();
        updates.forEach(update::set);
        UpdateResult result = mongoOperations.updateMulti(query, update, entityClass, collectionName);
        return result.getModifiedCount();
    }
    
    @Override
    public long incrementField(Query query, String field, Number value) {
        Update update = new Update().inc(field, value);
        UpdateResult result = mongoOperations.updateMulti(query, update, entityClass, collectionName);
        return result.getModifiedCount();
    }
    
    @Override
    public long addToArray(Query query, String field, Object value) {
        Update update = new Update().addToSet(field, value);
        UpdateResult result = mongoOperations.updateMulti(query, update, entityClass, collectionName);
        return result.getModifiedCount();
    }
    
    @Override
    public long removeFromArray(Query query, String field, Object value) {
        Update update = new Update().pull(field, value);
        UpdateResult result = mongoOperations.updateMulti(query, update, entityClass, collectionName);
        return result.getModifiedCount();
    }
    
    @Override
    public T upsert(T entity) {
        return mongoOperations.save(entity, collectionName);
    }
    
    @Override
    public List<T> upsertAll(List<T> entities) {
        return entities.stream().map(this::upsert).toList();
    }
    
    // ========== 删除操作 ==========
    
    @Override
    public long remove(Query query) {
        DeleteResult result = mongoOperations.remove(query, entityClass, collectionName);
        return result.getDeletedCount();
    }
    
    @Override
    public long removeByField(String field, Object value) {
        Query query = Query.query(Criteria.where(field).is(value));
        return remove(query);
    }
    
    // ========== 查询辅助方法 ==========
    
    @Override
    public boolean exists(Query query) {
        return mongoOperations.exists(query, entityClass, collectionName);
    }
    
    @Override
    public long count(Query query) {
        return mongoOperations.count(query, entityClass, collectionName);
    }
    
    @Override
    public String getCollectionName() {
        return collectionName;
    }
    
    // ========== 索引管理（简化版） ==========
    
    @Override
    public void createIndex(String field) {
        mongoOperations.indexOps(collectionName).createIndex(new Index(field, Sort.Direction.ASC));
    }
    
    @Override
    public void createCompoundIndex(String... fields) {
        Index index = new Index();
        for (String field : fields) {
            index = index.on(field, Sort.Direction.ASC);
        }
        mongoOperations.indexOps(collectionName).createIndex(index);
    }
    
    @Override
    public void createTextIndex(String... fields) {
        requireFields(fields);
        mongoOperations.indexOps(collectionName)
                .createIndex(TextIndexDefinition.builder().onFields(fields).build());
    }
    
    @Override
    public void createGeoIndex(String field) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field 不能为空");
        }
        mongoOperations.indexOps(collectionName)
                .createIndex(new GeospatialIndex(field).typed(GeoSpatialIndexType.GEO_2DSPHERE));
    }
    
    @Override
    public void dropIndex(String indexName) {
        mongoOperations.indexOps(collectionName).dropIndex(indexName);
    }
    
    @Override
    public List<String> getIndexes() {
        return mongoOperations.indexOps(collectionName).getIndexInfo().stream()
                .map(IndexInfo::getName)
                .toList();
    }

    private AggregationOperation toAggregationOperation(Object stage) {
        if (stage instanceof AggregationOperation operation) {
            return operation;
        }
        if (stage instanceof Bson bson) {
            return Aggregation.stage(bson);
        }
        if (stage instanceof String json) {
            return Aggregation.stage(json);
        }
        if (stage instanceof Map<?, ?> map) {
            org.bson.Document document = new org.bson.Document();
            map.forEach((key, value) -> {
                if (!(key instanceof String stringKey)) {
                    throw new IllegalArgumentException("聚合阶段的 Map key 必须是字符串");
                }
                document.put(stringKey, value);
            });
            return Aggregation.stage(document);
        }
        throw new IllegalArgumentException("不支持的聚合阶段类型: "
                + (stage == null ? "null" : stage.getClass().getName()));
    }

    private void requireFields(String... fields) {
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException("fields 不能为空");
        }
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("fields 不能包含空值");
            }
        }
    }
}
