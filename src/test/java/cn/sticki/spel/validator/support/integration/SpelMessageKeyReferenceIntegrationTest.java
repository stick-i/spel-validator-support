package cn.sticki.spel.validator.support.integration;

import com.intellij.codeInsight.highlighting.HighlightedReference;
import com.intellij.lang.properties.references.PropertyReferenceBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

/**
 * message 属性国际化 key 引用集成测试
 */
public class SpelMessageKeyReferenceIntegrationTest extends LightJavaCodeInsightFixtureTestCase {

    private static final String KEY_TEXT = "test.user.name.required";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

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
                    String message() default "";
                }
                """);

        myFixture.addClass("""
                package cn.sticki.test;
                import java.lang.annotation.*;
                @Target({ElementType.FIELD})
                @Retention(RetentionPolicy.RUNTIME)
                public @interface NonConstraint {
                    String message() default "";
                }
                """);

        myFixture.addClass("package java.lang; public class String {}");
        myFixture.addClass("package java.lang; public class Integer {}");
        myFixture.addClass("package java.lang; public class Object {}");
    }

    public void testMessageKeyReferenceResolve() {
        myFixture.addFileToProject("i18n/messages.properties", KEY_TEXT + "=用户名不能为空\n");

        PsiFile psiFile = myFixture.configureByText("TestDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class TestDto {
                    @SpelAssert(message = "{test.user.name.requ<caret>ired}")
                    private String userName;
                }
                """);

        PropertyReferenceBase reference = findPropertyReferenceAtCaret(psiFile);
        assertNotNull("message key should have a property reference", reference);

        PsiElement resolved = reference.resolve();
        assertNotNull("message key reference should resolve to properties key", resolved);
        assertTrue("resolved element should be a properties key", PropertyReferenceBase.isPropertyPsi(resolved));
        assertEquals("messages.properties", resolved.getContainingFile().getName());
    }

    public void testNoReferenceForOuterSpaces() {
        myFixture.addFileToProject("i18n/messages.properties", KEY_TEXT + "=用户名不能为空\n");

        PsiFile psiFile = myFixture.configureByText("TestDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class TestDto {
                    @SpelAssert(message = " {test.user.name.requ<caret>ired} ")
                    private String userName;
                }
                """);

        assertNoPropertyReferenceAtCaret(psiFile);
    }

    public void testInnerSpacesAreNotTrimmed() {
        myFixture.addFileToProject("i18n/messages.properties", KEY_TEXT + "=用户名不能为空\n");

        PsiFile psiFile = myFixture.configureByText("TestDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class TestDto {
                    @SpelAssert(message = "{ test.user.name.requ<caret>ired }")
                    private String userName;
                }
                """);

        PropertyReferenceBase reference = findPropertyReferenceAtCaret(psiFile);
        assertNotNull("message key with inner spaces should still create a reference", reference);

        // 关键点：不做 trim，key 应保留内部空格
        assertEquals(" test.user.name.required ", reference.getCanonicalText());

        PsiElement resolved = reference.resolve();
        assertNull("key with inner spaces should not resolve to trimmed key", resolved);
    }

    public void testMessageKeyUsesHighlightedReference() {
        myFixture.addFileToProject("i18n/messages.properties", KEY_TEXT + "=用户名不能为空\n");

        PsiFile psiFile = myFixture.configureByText("TestDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class TestDto {
                    @SpelAssert(message = "{test.user.name.requ<caret>ired}")
                    private String userName;
                }
                """);

        PropertyReferenceBase reference = findPropertyReferenceAtCaret(psiFile);
        assertNotNull("recognized key should have a property reference", reference);
        assertTrue("recognized key should use highlighted reference path",
                reference instanceof HighlightedReference);
    }

    public void testNoReferenceForNonMessageAttribute() {
        myFixture.addFileToProject("i18n/messages.properties", KEY_TEXT + "=用户名不能为空\n");

        PsiFile psiFile = myFixture.configureByText("TestDto.java", """
                package cn.sticki.test;
                import cn.sticki.spel.validator.constrain.SpelAssert;
                public class TestDto {
                    @SpelAssert(assertTrue = "{test.user.name.requ<caret>ired}", message = "ok")
                    private String userName;
                }
                """);

        assertNoPropertyReferenceAtCaret(psiFile);
    }

    public void testNoReferenceForNonConstraintAnnotation() {
        myFixture.addFileToProject("i18n/messages.properties", KEY_TEXT + "=用户名不能为空\n");

        PsiFile psiFile = myFixture.configureByText("TestDto.java", """
                package cn.sticki.test;
                import cn.sticki.test.NonConstraint;
                public class TestDto {
                    @NonConstraint(message = "{test.user.name.requ<caret>ired}")
                    private String userName;
                }
                """);

        assertNoPropertyReferenceAtCaret(psiFile);
    }

    private void assertNoPropertyReferenceAtCaret(PsiFile psiFile) {
        PropertyReferenceBase propertyReference = findPropertyReferenceAtCaret(psiFile);
        if (propertyReference == null) {
            return;
        }

        PsiElement resolved = propertyReference.resolve();
        assertTrue("property reference should not resolve when not recognized by plugin",
                resolved == null || !PropertyReferenceBase.isPropertyPsi(resolved));
    }

    private PropertyReferenceBase findPropertyReferenceAtCaret(PsiFile psiFile) {
        PsiElement element = psiFile.findElementAt(myFixture.getCaretOffset());
        while (element != null && !(element instanceof PsiFile)) {
            PsiReference[] references = element.getReferences();
            for (PsiReference reference : references) {
                if (reference instanceof PropertyReferenceBase) {
                    return (PropertyReferenceBase) reference;
                }
            }
            element = element.getParent();
        }
        return null;
    }
}
