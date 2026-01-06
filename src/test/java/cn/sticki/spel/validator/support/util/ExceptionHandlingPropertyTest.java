package cn.sticki.spel.validator.support.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.strings;
import static org.quicktheories.generators.SourceDSL.integers;

/**
 * Property 12: 异常处理的健壮性
 * 对于任何插件内部异常，插件应记录日志但不影响 IDEA 的正常功能（不抛出未捕获异常）。
 * 
 * Validates: Requirements 9.4
 * 
 * 注意：由于 IntelliJ Platform 测试框架的复杂性，
 * 这些属性测试模拟异常处理场景。
 * 
 * @author Sticki
 */
class ExceptionHandlingPropertyTest {
    
    /**
     * Property 12: 异常处理的健壮性
     * 测试工具类方法对 null 输入的处理
     * 
     * Validates: Requirements 9.4
     */
    @Test
    @Tag("Feature: spel-validator-idea-plugin, Property 12: 异常处理的健壮性")
    void testNullInputHandling() {
        // 测试 SpelValidatorUtil 方法对 null 输入不抛出异常
        assertDoesNotThrow(() -> {
            SpelValidatorUtil.isSpelConstraintAnnotation(null);
        }, "isSpelConstraintAnnotation should handle null input");
        
        assertDoesNotThrow(() -> {
            SpelValidatorUtil.isSpelLanguageAttribute(null);
        }, "isSpelLanguageAttribute should handle null input");
        
        assertDoesNotThrow(() -> {
            SpelValidatorUtil.getContextClass(null);
        }, "getContextClass should handle null input");
        
        assertDoesNotThrow(() -> {
            SpelValidatorUtil.getAllFields(null);
        }, "getAllFields should handle null input");
        
        assertDoesNotThrow(() -> {
            SpelValidatorUtil.resolveNestedField(null, "field");
        }, "resolveNestedField should handle null class input");
        
        assertDoesNotThrow(() -> {
            SpelValidatorUtil.resolveNestedField(null, null);
        }, "resolveNestedField should handle all null inputs");
    }
    
    /**
     * Property 12 (续): 测试空字符串和边界输入处理
     * 
     * Validates: Requirements 9.4
     */
    @Test
    @Tag("Feature: spel-validator-idea-plugin, Property 12: 异常处理的健壮性")
    void testEmptyAndBoundaryInputHandling() {
        // 生成各种边界字符串
        Gen<String> boundaryStrings = strings().allPossible().ofLengthBetween(0, 100);
        
        qt()
            .withExamples(100)
            .forAll(boundaryStrings)
            .checkAssert(input -> {
                // 测试 resolveNestedField 对各种字符串输入不抛出异常
                assertDoesNotThrow(() -> {
                    SpelValidatorUtil.resolveNestedField(null, input);
                }, "resolveNestedField should handle input: " + input);
            });
    }
    
    /**
     * Property 12 (续): 测试特殊字符输入处理
     * 
     * Validates: Requirements 9.4
     */
    @Test
    @Tag("Feature: spel-validator-idea-plugin, Property 12: 异常处理的健壮性")
    void testSpecialCharacterInputHandling() {
        // 测试特殊字符输入
        String[] specialInputs = {
            "",
            " ",
            ".",
            "..",
            "...",
            "field.",
            ".field",
            "field..name",
            "field.name.",
            "#this",
            "#this.",
            "#this..",
            "field\nname",
            "field\tname",
            "field\0name",
            "字段名",
            "field🎉name"
        };
        
        for (String input : specialInputs) {
            assertDoesNotThrow(() -> {
                SpelValidatorUtil.resolveNestedField(null, input);
            }, "resolveNestedField should handle special input: " + input);
        }
    }
    
    /**
     * Property 12 (续): 测试缓存清除不抛出异常
     * 
     * Validates: Requirements 9.4
     */
    @Test
    @Tag("Feature: spel-validator-idea-plugin, Property 12: 异常处理的健壮性")
    void testCacheClearingDoesNotThrow() {
        // 生成随机次数的缓存清除操作
        Gen<Integer> clearCountGen = integers().between(1, 100);
        
        qt()
            .withExamples(100)
            .forAll(clearCountGen)
            .checkAssert(count -> {
                for (int i = 0; i < count; i++) {
                    assertDoesNotThrow(() -> {
                        SpelValidatorUtil.clearFieldTypeCache();
                    }, "clearFieldTypeCache should not throw on iteration " + i);
                }
            });
    }
    
    /**
     * Property 12 (续): 测试并发访问不抛出异常
     * 
     * Validates: Requirements 9.4
     */
    @Test
    @Tag("Feature: spel-validator-idea-plugin, Property 12: 异常处理的健壮性")
    void testConcurrentAccessDoesNotThrow() {
        // 生成线程数
        Gen<Integer> threadCountGen = integers().between(2, 10);
        
        qt()
            .withExamples(50)
            .forAll(threadCountGen)
            .checkAssert(threadCount -> {
                CountDownLatch latch = new CountDownLatch(threadCount);
                AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
                
                for (int i = 0; i < threadCount; i++) {
                    new Thread(() -> {
                        try {
                            // 并发调用工具类方法
                            SpelValidatorUtil.isSpelConstraintAnnotation(null);
                            SpelValidatorUtil.getAllFields(null);
                            SpelValidatorUtil.resolveNestedField(null, "field.name");
                            SpelValidatorUtil.clearFieldTypeCache();
                        } catch (Exception e) {
                            exceptionOccurred.set(true);
                        } finally {
                            latch.countDown();
                        }
                    }).start();
                }
                
                try {
                    latch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                org.junit.jupiter.api.Assertions.assertFalse(exceptionOccurred.get(),
                    "No exception should occur during concurrent access");
            });
    }
}
