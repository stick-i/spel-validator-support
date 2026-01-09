package cn.sticki.spel.validator.support.integration;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SpEL 错误检查集成测试
 * <p>
 * 测试当 SpEL 表达式中引用不存在的字段时，是否会有错误提示
 */
public class SpelInspectionIntegrationTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // 定义必要的注解和类
        myFixture.addClass("""
                package org.intellij.lang.annotations;
                public @interface Language {
                    String value();
                }
                """);

        myFixture.addClass("""
                package cn.sticki.spel.validator.constrain;
                import java.lang.annotation.*;
                @Target(ElementType.ANNOTATION_TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                public @interface SpelConstraint {
                }
                """);

        myFixture.addClass("""
                package cn.sticki.spel.validator.constrain;
                import java.lang.annotation.*;
                import org.intellij.lang.annotations.Language;
                @Target({ElementType.FIELD})
                @Retention(RetentionPolicy.RUNTIME)
                @SpelConstraint
                public @interface SpelAssert {
                    @Language("SpEL")
                    String assertTrue() default "";
                }
                """);

        // 定义基础类，防止 Cannot resolve symbol 'String'
        myFixture.addClass("package java.lang; public class String {}");
        myFixture.addClass("package java.lang; public class Integer {}");
        myFixture.addClass("package java.lang; public class Object {}");
    }

    /**
     * 测试引用不存在的字段时显示警告（由 Spring 插件提供）
     */
    public void testInvalidFieldHighlighting() {
        // 启用 Spring 的 SpEL 检查
        myFixture.enableInspections(new com.intellij.spring.el.inspections.SpringElInspection());

        myFixture.configureByText("TestDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                
                public class TestDto {
                    public String userName;
                    private String status;
                
                    @SpelAssert(assertTrue = "#this.nonExistentField")
                    public String getStatus() { return status; }
                }
                """);

        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();

        // 查找与 nonExistentField 相关的警告
        List<HighlightInfo> fieldHighlights = highlights.stream()
                .filter(info -> info.getDescription() != null && info.getDescription().contains("nonExistentField"))
                .collect(Collectors.toList());

        assertFalse("Spring plugin should provide highlighting for non-existent field", fieldHighlights.isEmpty());
        assertEquals(HighlightSeverity.WARNING, fieldHighlights.get(0).getSeverity());
    }

    /**
     * 测试引用不存在的嵌套字段时显示警告
     */
    public void testNestedInvalidFieldHighlighting() {
        // 启用 Spring 的 SpEL 检查
        myFixture.enableInspections(new com.intellij.spring.el.inspections.SpringElInspection());

        myFixture.addClass("""
                package cn.sticki.test;
                public class Address {
                    public String city;
                }
                """);

        myFixture.configureByText("NestedTestDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class NestedTestDto {
                    public Address address;
                
                    @SpelAssert(assertTrue = "#this.address.unknownField")
                    private String status;
                }
                """);

        List<HighlightInfo> highlights = myFixture.doHighlighting();
        List<HighlightInfo> fieldHighlights = highlights.stream()
                .filter(info -> info.getDescription() != null && info.getDescription().contains("unknownField"))
                .collect(Collectors.toList());

        assertFalse("Spring plugin should provide highlighting for non-existent nested field", fieldHighlights.isEmpty());
        assertEquals(HighlightSeverity.WARNING, fieldHighlights.get(0).getSeverity());
    }

}
