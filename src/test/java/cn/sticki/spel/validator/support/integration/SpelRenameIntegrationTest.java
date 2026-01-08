package cn.sticki.spel.validator.support.integration;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

/**
 * SpEL 字段重命名重构集成测试
 * <p>
 * 测试重命名 Java 字段时，SpEL 表达式中的引用是否同步更新
 */
public class SpelRenameIntegrationTest extends LightJavaCodeInsightFixtureTestCase {

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

        myFixture.addClass("""
                package cn.sticki.test;
                public class Address {
                    public String city;
                }
                """);
    }

    /**
     * 测试基础字段重命名
     */
    public void testFieldRename() {
        myFixture.configureByText("TestDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class TestDto {
                    public String user<caret>Name;
                    @SpelAssert(assertTrue = "#this.userName")
                    private String status;
                }
                """);

        // 执行重命名
        myFixture.renameElementAtCaret("newUserName");

        // 验证 SpEL 表达式中的引用已更新
        myFixture.checkResult("""
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class TestDto {
                    public String newUserName;
                    @SpelAssert(assertTrue = "#this.newUserName")
                    private String status;
                }
                """);
    }

    /**
     * 测试嵌套字段重命名
     */
    public void testNestedFieldRename() {
        com.intellij.psi.PsiFile nestedDtoFile = myFixture.configureByText("NestedDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class NestedDto {
                    public Address address;
                    @SpelAssert(assertTrue = "#this.address.city")
                    private String status;
                }
                """);
        
        // 另外创建一个文件，在其中进行重命名
        myFixture.configureByText("Address.java", """
                package cn.sticki.test;
                public class Address {
                    public String ci<caret>ty;
                }
                """);

        // 重命名 Address 中的 city
        myFixture.renameElementAtCaret("newCity");

        // 验证 NestedDto 中的内容已更新
        String text = nestedDtoFile.getText();
        assertTrue("SpEL expression should be updated to newCity", text.contains("#this.address.newCity"));
    }
}
