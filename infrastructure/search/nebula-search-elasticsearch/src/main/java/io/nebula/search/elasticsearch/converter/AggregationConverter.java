package io.nebula.search.elasticsearch.converter;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.DoubleTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DoubleTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.AvgAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate;
import io.nebula.search.core.aggregation.DateHistogramAggregation;
import io.nebula.search.core.aggregation.HistogramAggregation;
import io.nebula.search.core.aggregation.MetricAggregation;
import io.nebula.search.core.aggregation.TermsAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 聚合转换器
 * 将 nebula 的 Aggregation 转换为 Elasticsearch Java Client 的 Aggregation
 * 
 * @author nebula
 */
public class AggregationConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(AggregationConverter.class);
    
    /**
     * 转换聚合
     * 
     * @param aggregation nebula 聚合
     * @return Elasticsearch Aggregation
     */
    public static Aggregation convert(io.nebula.search.core.aggregation.Aggregation aggregation) {
        if (aggregation == null) {
            return null;
        }
        
        switch (aggregation.getType()) {
            case TERMS:
                return convertTermsAggregation((TermsAggregation) aggregation);
            case AVG:
            case SUM:
            case MIN:
            case MAX:
                return convertMetricAggregation((MetricAggregation) aggregation);
            case HISTOGRAM:
                return convertHistogramAggregation((HistogramAggregation) aggregation);
            case DATE_HISTOGRAM:
                return convertDateHistogramAggregation((DateHistogramAggregation) aggregation);
            default:
                logger.warn("Unsupported aggregation type: {}", aggregation.getType());
                return null;
        }
    }
    
    /**
     * 转换 Terms 聚合
     */
    private static Aggregation convertTermsAggregation(TermsAggregation termsAgg) {
        // TODO: 子聚合暂不支持，需要在外层 SearchRequest 中处理
        return Aggregation.of(a -> a.terms(t -> {
            t.field(termsAgg.getField());
            
            if (termsAgg.getSize() != null) {
                t.size(termsAgg.getSize());
            }
            
            if (termsAgg.getMinDocCount() != null) {
                t.minDocCount(termsAgg.getMinDocCount().intValue());
            }
            
            return t;
        }));
    }
    
    /**
     * 转换指标聚合
     */
    private static Aggregation convertMetricAggregation(MetricAggregation metricAgg) {
        switch (metricAgg.getType()) {
            case AVG:
                return Aggregation.of(a -> a.avg(av -> av.field(metricAgg.getField())));
            case SUM:
                return Aggregation.of(a -> a.sum(s -> s.field(metricAgg.getField())));
            case MIN:
                return Aggregation.of(a -> a.min(m -> m.field(metricAgg.getField())));
            case MAX:
                return Aggregation.of(a -> a.max(m -> m.field(metricAgg.getField())));
            default:
                logger.warn("Unsupported metric aggregation type: {}", metricAgg.getType());
                return null;
        }
    }
    
    /**
     * 转换 Histogram 聚合
     */
    private static Aggregation convertHistogramAggregation(HistogramAggregation histogramAgg) {
        // TODO: 子聚合暂不支持，需要在外层 SearchRequest 中处理
        return Aggregation.of(a -> a.histogram(h -> {
            h.field(histogramAgg.getField());
            
            if (histogramAgg.getInterval() != null) {
                h.interval(histogramAgg.getInterval());
            }
            
            if (histogramAgg.getMinDocCount() != null) {
                h.minDocCount(histogramAgg.getMinDocCount().intValue());
            }
            
            return h;
        }));
    }
    
    /**
     * 转换 DateHistogram 聚合
     */
    private static Aggregation convertDateHistogramAggregation(DateHistogramAggregation dateHistogramAgg) {
        // TODO: 子聚合暂不支持，需要在外层 SearchRequest 中处理
        return Aggregation.of(a -> a.dateHistogram(dh -> {
            dh.field(dateHistogramAgg.getField());
            
            if (dateHistogramAgg.getCalendarInterval() != null) {
                // 将字符串格式的 calendar interval 转换为 Elasticsearch 的 CalendarInterval 枚举
                String interval = dateHistogramAgg.getCalendarInterval().toLowerCase();
                switch (interval) {
                    case "1m":
                    case "minute":
                        dh.calendarInterval(CalendarInterval.Minute);
                        break;
                    case "1h":
                    case "hour":
                        dh.calendarInterval(CalendarInterval.Hour);
                        break;
                    case "1d":
                    case "day":
                        dh.calendarInterval(CalendarInterval.Day);
                        break;
                    case "1w":
                    case "week":
                        dh.calendarInterval(CalendarInterval.Week);
                        break;
                    case "1M":
                    case "month":
                        dh.calendarInterval(CalendarInterval.Month);
                        break;
                    case "1q":
                    case "quarter":
                        dh.calendarInterval(CalendarInterval.Quarter);
                        break;
                    case "1y":
                    case "year":
                        dh.calendarInterval(CalendarInterval.Year);
                        break;
                    default:
                        logger.warn("Unsupported calendar interval: {}, using Day", interval);
                        dh.calendarInterval(CalendarInterval.Day);
                }
            }
            
            if (dateHistogramAgg.getFormat() != null) {
                dh.format(dateHistogramAgg.getFormat());
            }
            
            if (dateHistogramAgg.getMinDocCount() != null) {
                dh.minDocCount(Integer.valueOf(dateHistogramAgg.getMinDocCount()));
            }
            
            return dh;
        }));
    }
    
    /**
     * 解析聚合结果
     * 
     * @param esAggregates Elasticsearch 聚合结果
     * @return Map 格式的聚合结果
     */
    public static Map<String, Object> parseAggregationResults(Map<String, Aggregate> esAggregates) {
        if (esAggregates == null || esAggregates.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> results = new HashMap<>();
        esAggregates.forEach((name, aggregate) -> {
            if (aggregate.isSterms()) {
                results.put(name, parseStringTermsAggregate(aggregate.sterms()));
            } else if (aggregate.isLterms()) {
                results.put(name, parseLongTermsAggregate(aggregate.lterms()));
            } else if (aggregate.isDterms()) {
                results.put(name, parseDoubleTermsAggregate(aggregate.dterms()));
            } else if (aggregate.isAvg()) {
                results.put(name, parseAvgAggregate(aggregate.avg()));
            } else if (aggregate.isSum()) {
                results.put(name, parseSumAggregate(aggregate.sum()));
            } else if (aggregate.isMin()) {
                results.put(name, parseMinAggregate(aggregate.min()));
            } else if (aggregate.isMax()) {
                results.put(name, parseMaxAggregate(aggregate.max()));
            } else if (aggregate.isHistogram()) {
                results.put(name, parseHistogramAggregate(aggregate.histogram()));
            } else if (aggregate.isDateHistogram()) {
                results.put(name, parseDateHistogramAggregate(aggregate.dateHistogram()));
            }
            // Add other aggregate types as needed
        });
        return results;
    }

    private static Map<String, Object> parseStringTermsAggregate(StringTermsAggregate sterms) {
        Map<String, Object> result = new HashMap<>();
        result.put("doc_count_error_upper_bound", sterms.docCountErrorUpperBound());
        result.put("sum_other_doc_count", sterms.sumOtherDocCount());
        result.put("buckets", sterms.buckets().array().stream()
            .map(AggregationConverter::parseStringTermsBucket)
            .collect(Collectors.toList()));
        return result;
    }

    private static Map<String, Object> parseStringTermsBucket(StringTermsBucket bucket) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", bucket.key().stringValue());
        result.put("doc_count", bucket.docCount());
        if (bucket.aggregations() != null && !bucket.aggregations().isEmpty()) {
            result.put("aggregations", parseAggregationResults(bucket.aggregations()));
        }
        return result;
    }

    private static Map<String, Object> parseLongTermsAggregate(LongTermsAggregate lterms) {
        Map<String, Object> result = new HashMap<>();
        result.put("doc_count_error_upper_bound", lterms.docCountErrorUpperBound());
        result.put("sum_other_doc_count", lterms.sumOtherDocCount());
        result.put("buckets", lterms.buckets().array().stream()
            .map(AggregationConverter::parseLongTermsBucket)
            .collect(Collectors.toList()));
        return result;
    }

    private static Map<String, Object> parseLongTermsBucket(LongTermsBucket bucket) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", bucket.key());
        result.put("doc_count", bucket.docCount());
        if (bucket.aggregations() != null && !bucket.aggregations().isEmpty()) {
            result.put("aggregations", parseAggregationResults(bucket.aggregations()));
        }
        return result;
    }

    private static Map<String, Object> parseDoubleTermsAggregate(DoubleTermsAggregate dterms) {
        Map<String, Object> result = new HashMap<>();
        result.put("doc_count_error_upper_bound", dterms.docCountErrorUpperBound());
        result.put("sum_other_doc_count", dterms.sumOtherDocCount());
        result.put("buckets", dterms.buckets().array().stream()
            .map(AggregationConverter::parseDoubleTermsBucket)
            .collect(Collectors.toList()));
        return result;
    }

    private static Map<String, Object> parseDoubleTermsBucket(DoubleTermsBucket bucket) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", bucket.key());
        result.put("doc_count", bucket.docCount());
        if (bucket.aggregations() != null && !bucket.aggregations().isEmpty()) {
            result.put("aggregations", parseAggregationResults(bucket.aggregations()));
        }
        return result;
    }

    private static Map<String, Object> parseAvgAggregate(AvgAggregate avgAggregate) {
        Map<String, Object> result = new HashMap<>();
        result.put("value", avgAggregate.value());
        return result;
    }

    private static Map<String, Object> parseSumAggregate(SumAggregate sumAggregate) {
        Map<String, Object> result = new HashMap<>();
        result.put("value", sumAggregate.value());
        return result;
    }

    private static Map<String, Object> parseMinAggregate(MinAggregate minAggregate) {
        Map<String, Object> result = new HashMap<>();
        result.put("value", minAggregate.value());
        return result;
    }

    private static Map<String, Object> parseMaxAggregate(MaxAggregate maxAggregate) {
        Map<String, Object> result = new HashMap<>();
        result.put("value", maxAggregate.value());
        return result;
    }

    private static Map<String, Object> parseHistogramAggregate(HistogramAggregate histogramAggregate) {
        Map<String, Object> result = new HashMap<>();
        result.put("buckets", histogramAggregate.buckets().array().stream()
            .map(AggregationConverter::parseHistogramBucket)
            .collect(Collectors.toList()));
        return result;
    }

    private static Map<String, Object> parseHistogramBucket(HistogramBucket bucket) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", bucket.key());
        result.put("doc_count", bucket.docCount());
        if (bucket.aggregations() != null && !bucket.aggregations().isEmpty()) {
            result.put("aggregations", parseAggregationResults(bucket.aggregations()));
        }
        return result;
    }

    private static Map<String, Object> parseDateHistogramAggregate(DateHistogramAggregate dateHistogramAggregate) {
        Map<String, Object> result = new HashMap<>();
        result.put("buckets", dateHistogramAggregate.buckets().array().stream()
            .map(AggregationConverter::parseDateHistogramBucket)
            .collect(Collectors.toList()));
        return result;
    }

    private static Map<String, Object> parseDateHistogramBucket(DateHistogramBucket bucket) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", bucket.keyAsString());
        result.put("key_as_string", bucket.keyAsString());
        result.put("doc_count", bucket.docCount());
        if (bucket.aggregations() != null && !bucket.aggregations().isEmpty()) {
            result.put("aggregations", parseAggregationResults(bucket.aggregations()));
        }
        return result;
    }
}
