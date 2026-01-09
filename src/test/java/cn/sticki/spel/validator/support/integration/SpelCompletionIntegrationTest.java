package cn.sticki.spel.validator.support.integration;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import java.util.List;

/**
 * SpEL 代码补全集成测试
 * <p>
 * 测试在 SpEL Validator 注解中输入 #this. 后是否能够补全字段
 */
public class SpelCompletionIntegrationTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // 定义 @Language 注解（IntelliJ 的注解）
        myFixture.addClass("""
                package org.intellij.lang.annotations;
                
                import java.lang.annotation.*;
                
                @Retention(RetentionPolicy.CLASS)
                @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE})
                public @interface Language {
                    String value();
                    String prefix() default "";
                    String suffix() default "";
                }
                """);

        // 定义 @SpelConstraint 元注解
        myFixture.addClass("""
                package cn.sticki.spel.validator.constrain;
                
                import java.lang.annotation.*;
                
                @Target(ElementType.ANNOTATION_TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                public @interface SpelConstraint {
                }
                """);

        // 定义 @SpelAssert 注解（带有 @Language("SpEL") 标注）
        myFixture.addClass("""
                package cn.sticki.spel.validator.constrain;
                
                import java.lang.annotation.*;
                import org.intellij.lang.annotations.Language;
                
                @Target({ElementType.FIELD, ElementType.PARAMETER})
                @Retention(RetentionPolicy.RUNTIME)
                @SpelConstraint
                public @interface SpelAssert {
                    @Language("SpEL")
                    String assertTrue() default "";
                
                    @Language("SpEL")
                    String assertFalse() default "";
                
                    String message() default "";
                }
                """);
        // 定义 Address 类
        myFixture.addClass("""
                package cn.sticki.test;
                
                public class Address {
                    public String city;
                    public String street;
                }
                """);

        // 定义 BaseDto 类
        myFixture.addClass("""
                package cn.sticki.test;
                
                public class BaseDto {
                    public Long id;
                }
                """);
    }

    /**
     * 测试基本的字段补全
     */
    public void testFieldCompletion() {
        // 创建一个使用 SpEL Validator 注解的 Java 类
        myFixture.configureByText("TestDto.java", """
                package cn.sticki.test;
                
                import cn.sticki.spel.validator.constrain.SpelAssert;
                
                public class TestDto {
                    public String userName;
                    public Integer age;
                    public String email;
                
                    @SpelAssert(assertTrue = "#this.<caret>")
                    private String status;
                }
                """);

        // 触发代码补全
        myFixture.complete(CompletionType.BASIC);

        // 获取补全列表
        List<String> lookupStrings = myFixture.getLookupElementStrings();

        // 验证补全列表包含字段
        assertNotNull("Completion list should not be null", lookupStrings);

        assertTrue("Should contain userName field", lookupStrings.contains("userName"));
        assertTrue("Should contain age field", lookupStrings.contains("age"));
        assertTrue("Should contain email field", lookupStrings.contains("email"));
    }

    /**
     * 测试嵌套字段补全
     */
    public void testNestedFieldCompletion() {
        myFixture.configureByText("NestedDto.java", """
                package cn.sticki.test;
                
                import cn.sticki.spel.validator.constrain.SpelAssert;
                
                public class NestedDto {
                    public Address address;
                
                    @SpelAssert(assertTrue = "#this.address.<caret>")
                    private String status;
                }
                """);

        myFixture.complete(CompletionType.BASIC);
        List<String> lookupStrings = myFixture.getLookupElementStrings();

        assertNotNull("Completion list should not be null", lookupStrings);
        assertTrue("Should contain city field from Address", lookupStrings.contains("city"));
        assertTrue("Should contain street field from Address", lookupStrings.contains("street"));
    }

    /**
     * 测试继承字段补全
     */
    public void testInheritedFieldCompletion() {
        myFixture.configureByText("ChildDto.java", """
                package cn.sticki.test;
                
                import cn.sticki.spel.validator.constrain.SpelAssert;
                
                public class ChildDto extends BaseDto {
                    public String name;
                
                    @SpelAssert(assertTrue = "#this.<caret>")
                    private String status;
                }
                """);

        myFixture.complete(CompletionType.BASIC);
        List<String> lookupStrings = myFixture.getLookupElementStrings();

        assertNotNull("Completion list should not be null", lookupStrings);
        assertTrue("Should contain name field from ChildDto", lookupStrings.contains("name"));
        assertTrue("Should contain id field from BaseDto", lookupStrings.contains("id"));
    }

    /**
     * 测试没有 SpEL 注解时不应该有补全
     */
    public void testNoCompletionWithoutSpelAnnotation() {
        myFixture.configureByText("NormalDto.java", """
                package cn.sticki.test;
                
                public class NormalDto {
                    private String userName;
                
                    // 普通字符串，不是 SpEL 表达式
                    private String value = "#this.<caret>";
                }
                """);

        myFixture.complete(CompletionType.BASIC);
        List<String> lookupStrings = myFixture.getLookupElementStrings();

        // 在普通字符串中不应该有字段补全
        if (lookupStrings != null) {
            assertFalse("Should NOT contain userName in normal string",
                    lookupStrings.contains("userName"));
        }
    }

}
