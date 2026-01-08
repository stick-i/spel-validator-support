package cn.sticki.spel.validator.support.integration;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

/**
 * SpEL 字段引用解析集成测试
 * <p>
 * 测试在 SpEL Validator 注解中点击字段名是否能正确跳转到字段定义
 */
public class SpelReferenceIntegrationTest extends LightJavaCodeInsightFixtureTestCase {

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
     * 测试基础字段跳转
     */
    public void testFieldReference() {
        myFixture.configureByText("TestDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class TestDto {
                    public String userName;
                    @SpelAssert(assertTrue = "#this.user<caret>Name")
                    private String status;
                }
                """);

        PsiElement element = myFixture.getElementAtCaret();
        assertNotNull("Reference should resolve to an element", element);
        assertTrue("Reference should resolve to a PsiField", element instanceof PsiField);
        assertEquals("userName", ((PsiField) element).getName());
    }

    /**
     * 测试嵌套字段跳转
     */
    public void testNestedFieldReference() {
        myFixture.configureByText("NestedDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class NestedDto {
                    public Address address;
                    @SpelAssert(assertTrue = "#this.address.ci<caret>ty")
                    private String status;
                }
                """);

        PsiElement element = myFixture.getElementAtCaret();
        assertNotNull("Reference should resolve to an element", element);
        assertTrue("Reference should resolve to a PsiField", element instanceof PsiField);
        assertEquals("city", ((PsiField) element).getName());
        assertEquals("cn.sticki.test.Address", ((PsiField) element).getContainingClass().getQualifiedName());
    }
}
