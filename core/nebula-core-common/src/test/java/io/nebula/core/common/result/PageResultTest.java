package io.nebula.core.common.result;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

/**
 * PageResult 类单元测试
 */
class PageResultTest {
    
    @Test
    void testSuccessWithPageInfo() {
        // Given
        List<String> data = List.of("item1", "item2", "item3");
        PageResult.PageInfo pageInfo = PageResult.PageInfo.of(1, 10, 25);
        
        // When
        PageResult<String> result = PageResult.success(data, pageInfo);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo(data);
        assertThat(result.getPageInfo()).isEqualTo(pageInfo);
        assertThat(result.getTimestamp()).isNotNull();
    }
    
    @Test
    void testSuccessWithPageInfoAndMessage() {
        // Given
        List<String> data = List.of("item1", "item2");
        PageResult.PageInfo pageInfo = PageResult.PageInfo.of(2, 5, 15);
        String customMessage = "查询成功";
        
        // When
        PageResult<String> result = PageResult.success(data, pageInfo, customMessage);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(result.getMessage()).isEqualTo(customMessage);
        assertThat(result.getData()).isEqualTo(data);
        assertThat(result.getPageInfo()).isEqualTo(pageInfo);
    }
    
    @Test
    void testSuccessWithPaginationParameters() {
        // Given
        List<String> data = List.of("item1", "item2", "item3", "item4", "item5");
        int pageNumber = 3;
        int pageSize = 5;
        long totalElements = 50;
        
        // When
        PageResult<String> result = PageResult.success(data, pageNumber, pageSize, totalElements);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(data);
        assertThat(result.getPageInfo()).isNotNull();
        assertThat(result.getPageInfo().getPageNumber()).isEqualTo(pageNumber);
        assertThat(result.getPageInfo().getPageSize()).isEqualTo(pageSize);
        assertThat(result.getPageInfo().getTotalElements()).isEqualTo(totalElements);
        assertThat(result.getPageInfo().getTotalPages()).isEqualTo(10); // 50/5 = 10
        assertThat(result.getPageInfo().getNumberOfElements()).isEqualTo(5); // data.size()
        assertThat(result.getPageInfo().isFirst()).isFalse(); // page 3 is not first
        assertThat(result.getPageInfo().isLast()).isFalse(); // page 3 is not last (total 10 pages)
        assertThat(result.getPageInfo().isHasNext()).isTrue();
        assertThat(result.getPageInfo().isHasPrevious()).isTrue();
    }
    
    @Test
    void testEmpty() {
        // Given
        int pageNumber = 1;
        int pageSize = 10;
        
        // When
        PageResult<String> result = PageResult.empty(pageNumber, pageSize);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
        assertThat(result.getPageInfo()).isNotNull();
        assertThat(result.getPageInfo().getPageNumber()).isEqualTo(pageNumber);
        assertThat(result.getPageInfo().getPageSize()).isEqualTo(pageSize);
        assertThat(result.getPageInfo().getTotalElements()).isZero();
        assertThat(result.getPageInfo().getTotalPages()).isZero();
        assertThat(result.getPageInfo().getNumberOfElements()).isZero();
        assertThat(result.getPageInfo().isFirst()).isTrue();
        assertThat(result.getPageInfo().isLast()).isTrue();
        assertThat(result.getPageInfo().isHasNext()).isFalse();
        assertThat(result.getPageInfo().isHasPrevious()).isFalse();
    }
    
    @Test
    void testPageError() {
        // Given
        String errorCode = "PAGE_ERROR";
        String errorMessage = "分页查询失败";
        
        // When
        PageResult<String> result = PageResult.pageError(errorCode, errorMessage);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(errorCode);
        assertThat(result.getMessage()).isEqualTo(errorMessage);
        assertThat(result.getData()).isNull();
        assertThat(result.getPageInfo()).isNull();
    }
    
    @Test
    void testPageInfoCalculations() {
        // Test first page
        PageResult.PageInfo firstPage = PageResult.PageInfo.of(1, 10, 25);
        assertThat(firstPage.isFirst()).isTrue();
        assertThat(firstPage.isLast()).isFalse();
        assertThat(firstPage.isHasNext()).isTrue();
        assertThat(firstPage.isHasPrevious()).isFalse();
        assertThat(firstPage.getTotalPages()).isEqualTo(3); // ceil(25/10) = 3
        
        // Test last page
        PageResult.PageInfo lastPage = PageResult.PageInfo.of(3, 10, 25);
        assertThat(lastPage.isFirst()).isFalse();
        assertThat(lastPage.isLast()).isTrue();
        assertThat(lastPage.isHasNext()).isFalse();
        assertThat(lastPage.isHasPrevious()).isTrue();
        
        // Test middle page
        PageResult.PageInfo middlePage = PageResult.PageInfo.of(2, 10, 25);
        assertThat(middlePage.isFirst()).isFalse();
        assertThat(middlePage.isLast()).isFalse();
        assertThat(middlePage.isHasNext()).isTrue();
        assertThat(middlePage.isHasPrevious()).isTrue();
        
        // Test single page
        PageResult.PageInfo singlePage = PageResult.PageInfo.of(1, 20, 15);
        assertThat(singlePage.isFirst()).isTrue();
        assertThat(singlePage.isLast()).isTrue();
        assertThat(singlePage.isHasNext()).isFalse();
        assertThat(singlePage.isHasPrevious()).isFalse();
        assertThat(singlePage.getTotalPages()).isEqualTo(1);
    }
    
    @Test
    void testPageInfoWithNumberOfElements() {
        // Given
        int pageNumber = 5;
        int pageSize = 10;
        long totalElements = 48;
        int numberOfElements = 8; // Last page has only 8 elements
        
        // When
        PageResult.PageInfo pageInfo = PageResult.PageInfo.of(pageNumber, pageSize, totalElements, numberOfElements);
        
        // Then
        assertThat(pageInfo.getPageNumber()).isEqualTo(pageNumber);
        assertThat(pageInfo.getPageSize()).isEqualTo(pageSize);
        assertThat(pageInfo.getTotalElements()).isEqualTo(totalElements);
        assertThat(pageInfo.getNumberOfElements()).isEqualTo(numberOfElements);
        assertThat(pageInfo.getTotalPages()).isEqualTo(5); // ceil(48/10) = 5
        assertThat(pageInfo.isLast()).isTrue(); // page 5 of 5
        assertThat(pageInfo.isHasNext()).isFalse();
    }
    
    @Test
    void testEdgeCases() {
        // Test zero total elements
        PageResult.PageInfo zeroPage = PageResult.PageInfo.of(1, 10, 0);
        assertThat(zeroPage.getTotalPages()).isZero();
        assertThat(zeroPage.isFirst()).isTrue();
        assertThat(zeroPage.isLast()).isTrue();
        
        // Test exact division
        PageResult.PageInfo exactPage = PageResult.PageInfo.of(2, 10, 20);
        assertThat(exactPage.getTotalPages()).isEqualTo(2);
        assertThat(exactPage.isLast()).isTrue();
        
        // Test single element
        PageResult.PageInfo singleElement = PageResult.PageInfo.of(1, 10, 1);
        assertThat(singleElement.getTotalPages()).isEqualTo(1);
        assertThat(singleElement.isFirst()).isTrue();
        assertThat(singleElement.isLast()).isTrue();
    }
}
