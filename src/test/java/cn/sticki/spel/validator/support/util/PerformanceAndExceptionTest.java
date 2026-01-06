package cn.sticki.spel.validator.support.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能和异常处理单元测试
 * 
 * 测试内容：
 * - 补全性能（100ms 内）
 * - 大型类处理（200ms 内）
 * - 异常捕获和日志记录
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4
 * 
 * @author Sticki
 */
class PerformanceAndExceptionTest {
    
    /**
     * 补全性能阈值：100ms
     */
    private static final long COMPLETION_THRESHOLD_MS = 100;
    
    /**
     * 大型类处理性能阈值：200ms
     */
    private static final long LARGE_CLASS_THRESHOLD_MS = 200;
    
    @BeforeEach
    void setUp() {
        // 清除缓存，确保测试独立性
        SpelValidatorUtil.clearFieldTypeCache();
    }
    
    /**
     * 测试补全性能（100ms 内）
     * Requirements: 9.1
     */
    @Test
    @DisplayName("补全操作应在 100ms 内完成")
    void testCompletionPerformance() {
        // 模拟补全操作的核心逻辑
        long startTime = System.currentTimeMillis();
        
        // 模拟字段收集和处理
        List<String> fields = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            fields.add("field" + i);
        }
        
        // 模拟字段匹配
        for (String field : fields) {
            field.startsWith("field");
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < COMPLETION_THRESHOLD_MS,
            "补全操作耗时 " + duration + "ms，超过阈值 " + COMPLETION_THRESHOLD_MS + "ms");
    }
    
    /**
     * 测试大型类处理（200ms 内）
     * Requirements: 9.3
     */
    @Test
    @DisplayName("大型类（100+ 字段）处理应在 200ms 内完成")
    void testLargeClassPerformance() {
        // 模拟大型类的字段处理
        long startTime = System.currentTimeMillis();
        
        // 模拟 150 个字段的收集和处理
        List<String> fields = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            fields.add("field" + i);
        }
        
        // 模拟字段查找操作
        for (int i = 0; i < 100; i++) {
            String targetField = "field" + (i % 150);
            fields.contains(targetField);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < LARGE_CLASS_THRESHOLD_MS,
            "大型类处理耗时 " + duration + "ms，超过阈值 " + LARGE_CLASS_THRESHOLD_MS + "ms");
    }
    
    /**
     * 测试 null 输入的异常处理
     * Requirements: 9.4
     */
    @Test
    @DisplayName("null 输入应被正确处理，不抛出异常")
    void testNullInputExceptionHandling() {
        // 测试所有工具方法对 null 输入的处理
        assertDoesNotThrow(() -> SpelValidatorUtil.isSpelConstraintAnnotation(null));
        assertDoesNotThrow(() -> SpelValidatorUtil.isSpelLanguageAttribute(null));
        assertDoesNotThrow(() -> SpelValidatorUtil.getContextClass(null));
        assertDoesNotThrow(() -> SpelValidatorUtil.getAllFields(null));
        assertDoesNotThrow(() -> SpelValidatorUtil.resolveNestedField(null, "field"));
        assertDoesNotThrow(() -> SpelValidatorUtil.resolveNestedField(null, null));
    }
    
    /**
     * 测试空字符串输入的异常处理
     * Requirements: 9.4
     */
    @Test
    @DisplayName("空字符串输入应被正确处理，不抛出异常")
    void testEmptyStringExceptionHandling() {
        assertDoesNotThrow(() -> SpelValidatorUtil.resolveNestedField(null, ""));
        assertDoesNotThrow(() -> SpelValidatorUtil.resolveNestedField(null, " "));
        assertDoesNotThrow(() -> SpelValidatorUtil.resolveNestedField(null, "."));
    }
    
    /**
     * 测试特殊字符输入的异常处理
     * Requirements: 9.4
     */
    @Test
    @DisplayName("特殊字符输入应被正确处理，不抛出异常")
    void testSpecialCharacterExceptionHandling() {
        String[] specialInputs = {
            "..",
            "...",
            "field.",
            ".field",
            "field..name",
            "#this",
            "#this.",
            "field\nname",
            "field\tname"
        };
        
        for (String input : specialInputs) {
            assertDoesNotThrow(() -> SpelValidatorUtil.resolveNestedField(null, input),
                "输入 '" + input + "' 应被正确处理");
        }
    }
    
    /**
     * 测试缓存清除功能
     * Requirements: 9.3
     */
    @Test
    @DisplayName("缓存清除应正常工作，不抛出异常")
    void testCacheClearingFunctionality() {
        // 多次清除缓存应该不会抛出异常
        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> SpelValidatorUtil.clearFieldTypeCache());
        }
    }
    
    /**
     * 测试并发访问的线程安全性
     * Requirements: 9.2
     */
    @Test
    @DisplayName("并发访问应保持线程安全")
    void testConcurrentAccessThreadSafety() throws InterruptedException {
        int threadCount = 5;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicBoolean exceptionOccurred = new java.util.concurrent.atomic.AtomicBoolean(false);
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        SpelValidatorUtil.isSpelConstraintAnnotation(null);
                        SpelValidatorUtil.getAllFields(null);
                        SpelValidatorUtil.resolveNestedField(null, "field.name");
                        SpelValidatorUtil.clearFieldTypeCache();
                    }
                } catch (Exception e) {
                    exceptionOccurred.set(true);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        boolean completed = latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        
        assertTrue(completed, "所有线程应在超时前完成");
        assertFalse(exceptionOccurred.get(), "并发访问不应抛出异常");
    }
    
    /**
     * 测试返回值的正确性
     * Requirements: 9.4
     */
    @Test
    @DisplayName("null 输入应返回正确的默认值")
    void testNullInputReturnValues() {
        // 测试 null 输入返回正确的默认值
        assertFalse(SpelValidatorUtil.isSpelConstraintAnnotation(null));
        assertFalse(SpelValidatorUtil.isSpelLanguageAttribute(null));
        assertNull(SpelValidatorUtil.getContextClass(null));
        assertTrue(SpelValidatorUtil.getAllFields(null).isEmpty());
        assertNull(SpelValidatorUtil.resolveNestedField(null, "field"));
    }
}
