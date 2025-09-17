package io.nebula.batch.core;

/**
 * 批处理项处理器
 * 
 * @param <I> 输入项类型
 * @param <O> 输出项类型
 */
public interface ItemProcessor<I, O> {
    
    /**
     * 处理单个项
     * 
     * @param item 输入项
     * @return 处理后的输出项，如果返回null则跳过该项
     * @throws Exception 处理异常
     */
    O process(I item) throws Exception;
}
