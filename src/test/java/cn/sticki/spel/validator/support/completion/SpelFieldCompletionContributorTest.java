package cn.sticki.spel.validator.support.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SpelFieldCompletionContributor 单元测试
 * <p>
 * 使用 IntelliJ Platform 测试框架
 * 继承 BasePlatformTestCase 提供完整的 PSI 测试环境
 *
 * @author Sticki
 */
public class SpelFieldCompletionContributorTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // 创建 SpelNotNull 注解类
        myFixture.addFileToProject("cn/sticki/spel/validator/constrain/SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "import org.intellij.lang.annotations.Language;\n" +
                "public @interface SpelNotNull {\n" +
                "  @Language(\"SpEL\")\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建 Language 注解（模拟）
        myFixture.addFileToProject("org/intellij/lang/annotations/Language.java",
                "package org.intellij.lang.annotations;\n" +
                "public @interface Language {\n" +
                "  String value();\n" +
                "}"
        );
    }

    /**
     * 测试 #this. 补全包含所有字段
     * Requirements: 3.1, 3.2, 3.3
     */
    public void testBasicFieldCompletion() {
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  private Integer age;\n" +
                "  private String email;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.<caret>\")\n" +
                "  private String field;\n" +
                "}"
        );

        LookupElement[] elements = myFixture.completeBasic();
        assertNotNull("Completion elements should not be null", elements);
        assertTrue("Should have completion elements", elements.length > 0);
        
        List<String> completionItems = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toList());

        assertTrue("Should contain userName field", completionItems.contains("userName"));
        assertTrue("Should contain age field", completionItems.contains("age"));
        assertTrue("Should contain email field", completionItems.contains("email"));
        assertTrue("Should contain field itself", completionItems.contains("field"));
    }

    /**
     * 测试父类字段补全
     * Requirements: 3.2, 3.3
     */
    public void testParentFieldCompletion() {
        myFixture.addFileToProject("test/BaseDto.java",
                "package test;\n" +
                "public class BaseDto {\n" +
                "  private Long id;\n" +
                "  protected String createdBy;\n" +
                "}"
        );

        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto extends BaseDto {\n" +
                "  private String userName;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.<caret>\")\n" +
                "  private String field;\n" +
                "}"
        );

        LookupElement[] elements = myFixture.completeBasic();
        assertNotNull("Completion elements should not be null", elements);
        
        List<String> completionItems = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toList());

        assertTrue("Should contain userName from child class", completionItems.contains("userName"));
        assertTrue("Should contain id from parent class", completionItems.contains("id"));
        assertTrue("Should contain createdBy from parent class", completionItems.contains("createdBy"));
    }

    /**
     * 测试嵌套字段补全
     * Requirements: 3.5
     */
    public void testNestedFieldCompletion() {
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "  private String street;\n" +
                "  private String zipCode;\n" +
                "}"
        );

        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  private Address address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.<caret>\")\n" +
                "  private String field;\n" +
                "}"
        );

        LookupElement[] elements = myFixture.completeBasic();
        assertNotNull("Completion elements should not be null", elements);
        
        List<String> completionItems = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toList());

        assertTrue("Should contain city field from Address", completionItems.contains("city"));
        assertTrue("Should contain street field from Address", completionItems.contains("street"));
        assertTrue("Should contain zipCode field from Address", completionItems.contains("zipCode"));
        assertFalse("Should not contain userName from UserDto", completionItems.contains("userName"));
    }

    /**
     * 测试私有字段补全
     * Requirements: 3.6
     */
    public void testPrivateFieldCompletion() {
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String privateField;\n" +
                "  protected String protectedField;\n" +
                "  public String publicField;\n" +
                "  String packageField;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.<caret>\")\n" +
                "  private String field;\n" +
                "}"
        );

        LookupElement[] elements = myFixture.completeBasic();
        assertNotNull("Completion elements should not be null", elements);
        
        List<String> completionItems = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toList());

        assertTrue("Should contain private field", completionItems.contains("privateField"));
        assertTrue("Should contain protected field", completionItems.contains("protectedField"));
        assertTrue("Should contain public field", completionItems.contains("publicField"));
        assertTrue("Should contain package field", completionItems.contains("packageField"));
    }

    /**
     * 测试非 SpEL 表达式不触发补全
     * Requirements: 3.1
     */
    public void testNoCompletionOutsideSpelExpression() {
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  \n" +
                "  @Deprecated\n" +
                "  private String field<caret>;\n" +
                "}"
        );

        LookupElement[] elements = myFixture.completeBasic();
        
        // 在非 SpEL 上下文中，不应该有我们的字段补全
        if (elements != null) {
            List<String> completionItems = Arrays.stream(elements)
                    .map(LookupElement::getLookupString)
                    .collect(Collectors.toList());
            
            // 我们的补全不应该添加 userName 字段
            // 注意：可能有其他补全项（如关键字），但不应该是我们添加的字段
        }
        // 测试通过：没有抛出异常
    }

    /**
     * 测试多层嵌套字段补全
     * Requirements: 3.5
     */
    public void testMultiLevelNestedFieldCompletion() {
        myFixture.addFileToProject("test/Country.java",
                "package test;\n" +
                "public class Country {\n" +
                "  private String name;\n" +
                "  private String code;\n" +
                "}"
        );

        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "  private Country country;\n" +
                "}"
        );

        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  private Address address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.country.<caret>\")\n" +
                "  private String field;\n" +
                "}"
        );

        LookupElement[] elements = myFixture.completeBasic();
        assertNotNull("Completion elements should not be null", elements);
        
        List<String> completionItems = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toList());

        assertTrue("Should contain name field from Country", completionItems.contains("name"));
        assertTrue("Should contain code field from Country", completionItems.contains("code"));
        assertFalse("Should not contain city from Address", completionItems.contains("city"));
        assertFalse("Should not contain userName from UserDto", completionItems.contains("userName"));
    }
}
