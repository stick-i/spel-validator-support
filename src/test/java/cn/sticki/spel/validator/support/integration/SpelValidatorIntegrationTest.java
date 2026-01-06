package cn.sticki.spel.validator.support.integration;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.*;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SpEL Validator 插件集成测试
 * <p>
 * 测试端到端流程，验证所有功能模块的协作
 * <p>
 * Requirements: 8.4, 8.5
 *
 * @author Sticki
 */
public class SpelValidatorIntegrationTest extends BasePlatformTestCase {

    /**
     * 设置测试环境
     * 创建测试所需的注解类和辅助类
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // 创建 SpelConstraint 元注解
        myFixture.addFileToProject("cn/sticki/spel/validator/constrain/SpelConstraint.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "import java.lang.annotation.*;\n" +
                "@Target(ElementType.ANNOTATION_TYPE)\n" +
                "@Retention(RetentionPolicy.RUNTIME)\n" +
                "public @interface SpelConstraint {\n" +
                "}"
        );
        
        // 创建 Language 注解（模拟 IntelliJ 的 @Language 注解）
        myFixture.addFileToProject("org/intellij/lang/annotations/Language.java",
                "package org.intellij.lang.annotations;\n" +
                "import java.lang.annotation.*;\n" +
                "@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})\n" +
                "@Retention(RetentionPolicy.CLASS)\n" +
                "public @interface Language {\n" +
                "  String value();\n" +
                "}"
        );
        
        // 创建 SpelNotNull 约束注解
        myFixture.addFileToProject("cn/sticki/spel/validator/constrain/SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "import org.intellij.lang.annotations.Language;\n" +
                "import java.lang.annotation.*;\n" +
                "@Target({ElementType.FIELD, ElementType.PARAMETER})\n" +
                "@Retention(RetentionPolicy.RUNTIME)\n" +
                "public @interface SpelNotNull {\n" +
                "  @Language(\"SpEL\")\n" +
                "  String condition() default \"\";\n" +
                "  String message() default \"must not be null\";\n" +
                "}"
        );
        
        // 创建 SpelAssert 约束注解
        myFixture.addFileToProject("cn/sticki/spel/validator/constrain/SpelAssert.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "import org.intellij.lang.annotations.Language;\n" +
                "import java.lang.annotation.*;\n" +
                "@Target({ElementType.FIELD, ElementType.PARAMETER})\n" +
                "@Retention(RetentionPolicy.RUNTIME)\n" +
                "public @interface SpelAssert {\n" +
                "  @Language(\"SpEL\")\n" +
                "  String assertTrue() default \"\";\n" +
                "  String message() default \"assertion failed\";\n" +
                "}"
        );
    }

    // ==================== 端到端流程测试 ====================

    /**
     * 测试完整的端到端流程：字段补全 -> 引用解析 -> 错误检查
     * Requirements: 8.4
     */
    public void testEndToEndFlow_FieldCompletionAndValidation() {
        // 创建测试类
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  private Integer age;\n" +
                "  private String email;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.<caret>\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 1. 测试字段补全
        LookupElement[] elements = myFixture.completeBasic();
        assertNotNull("Completion elements should not be null", elements);
        assertTrue("Should have completion elements", elements.length > 0);
        
        List<String> completionItems = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toList());

        assertTrue("Should contain userName field", completionItems.contains("userName"));
        assertTrue("Should contain age field", completionItems.contains("age"));
        assertTrue("Should contain email field", completionItems.contains("email"));
    }

    /**
     * 测试完整流程：存在的字段不显示错误
     * Requirements: 8.4
     */
    public void testEndToEndFlow_ExistingFieldNoError() {
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.userName != null\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("不存在"))
                .collect(Collectors.toList());
        
        // 验证：存在的字段不应该有错误标记
        assertTrue("Existing field should not have error marker", fieldErrors.isEmpty());
    }

    /**
     * 测试完整流程：不存在的字段显示错误
     * Requirements: 8.4
     */
    public void testEndToEndFlow_NonExistentFieldShowsError() {
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.nonExistentField != null\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("nonExistentField"))
                .collect(Collectors.toList());
        
        // 验证：不存在的字段应该有错误标记
        assertFalse("Non-existent field should have error marker", fieldErrors.isEmpty());
    }

    // ==================== 复杂场景测试 ====================

    /**
     * 测试复杂场景：多层继承 + 嵌套字段
     * Requirements: 8.4
     */
    public void testComplexScenario_InheritanceWithNestedFields() {
        // 创建基类
        myFixture.addFileToProject("test/BaseEntity.java",
                "package test;\n" +
                "public class BaseEntity {\n" +
                "  private Long id;\n" +
                "  private String createdBy;\n" +
                "}"
        );
        
        // 创建地址类
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "  private String street;\n" +
                "  private String zipCode;\n" +
                "}"
        );
        
        // 创建用户类（继承基类，包含嵌套字段）
        myFixture.configureByText("User.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class User extends BaseEntity {\n" +
                "  private String userName;\n" +
                "  private Address address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.<caret>\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 测试补全
        LookupElement[] elements = myFixture.completeBasic();
        assertNotNull("Completion elements should not be null", elements);
        
        List<String> completionItems = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toList());

        // 验证：应该包含当前类字段
        assertTrue("Should contain userName", completionItems.contains("userName"));
        assertTrue("Should contain address", completionItems.contains("address"));
        
        // 验证：应该包含父类字段
        assertTrue("Should contain id from parent", completionItems.contains("id"));
        assertTrue("Should contain createdBy from parent", completionItems.contains("createdBy"));
    }

    /**
     * 测试复杂场景：嵌套字段补全
     * Requirements: 8.4
     */
    public void testComplexScenario_NestedFieldCompletion() {
        // 创建地址类
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "  private String street;\n" +
                "}"
        );
        
        // 创建用户类
        myFixture.configureByText("User.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class User {\n" +
                "  private String userName;\n" +
                "  private Address address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.<caret>\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 测试嵌套字段补全
        LookupElement[] elements = myFixture.completeBasic();
        assertNotNull("Completion elements should not be null", elements);
        
        List<String> completionItems = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toList());

        // 验证：应该包含 Address 类的字段
        assertTrue("Should contain city from Address", completionItems.contains("city"));
        assertTrue("Should contain street from Address", completionItems.contains("street"));
        
        // 验证：不应该包含 User 类的字段
        assertFalse("Should not contain userName from User", completionItems.contains("userName"));
    }

    /**
     * 测试复杂场景：嵌套字段错误检查
     * Requirements: 8.4
     */
    public void testComplexScenario_NestedFieldErrorCheck() {
        // 创建地址类
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "}"
        );
        
        // 创建用户类，引用不存在的嵌套字段
        myFixture.configureByText("User.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class User {\n" +
                "  private Address address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.nonExistentField != null\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("nonExistentField"))
                .collect(Collectors.toList());
        
        // 验证：不存在的嵌套字段应该有错误标记
        assertFalse("Non-existent nested field should have error marker", fieldErrors.isEmpty());
    }

    // ==================== 字段重命名测试 ====================

    /**
     * 测试字段重命名：SpEL 表达式中的引用自动更新
     * Requirements: 8.4
     */
    public void testFieldRename_UpdatesSpelExpression() {
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String old<caret>FieldName;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.oldFieldName != null\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 执行重命名操作
        myFixture.renameElementAtCaret("newFieldName");
        
        // 获取重命名后的文件内容
        String fileContent = myFixture.getFile().getText();
        
        // 验证字段已被重命名
        assertTrue("Field declaration should be renamed", 
                fileContent.contains("private String newFieldName;"));
        assertFalse("Old field name should not exist in declaration", 
                fileContent.contains("private String oldFieldName;"));
        
        // 验证 SpEL 表达式中的引用也被更新
        assertTrue("SpEL expression should be updated with new field name", 
                fileContent.contains("#this.newFieldName"));
        assertFalse("Old field name should not exist in SpEL expression", 
                fileContent.contains("#this.oldFieldName"));
    }

    // ==================== 自定义约束注解测试 ====================

    /**
     * 测试自定义约束注解：使用 @SpelConstraint 元注解
     * Requirements: 8.4
     */
    public void testCustomConstraintAnnotation_WithSpelConstraint() {
        // 创建自定义约束注解
        myFixture.addFileToProject("test/CustomConstraint.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelConstraint;\n" +
                "import org.intellij.lang.annotations.Language;\n" +
                "import java.lang.annotation.*;\n" +
                "@Target(ElementType.FIELD)\n" +
                "@Retention(RetentionPolicy.RUNTIME)\n" +
                "@SpelConstraint\n" +
                "public @interface CustomConstraint {\n" +
                "  @Language(\"SpEL\")\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建使用自定义注解的测试类
        myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "public class TestClass {\n" +
                "  private String existingField;\n" +
                "  \n" +
                "  @CustomConstraint(condition = \"#this.<caret>\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 测试补全
        LookupElement[] elements = myFixture.completeBasic();
        assertNotNull("Completion elements should not be null", elements);
        
        List<String> completionItems = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toList());

        // 验证：自定义约束注解也应该支持字段补全
        assertTrue("Should contain existingField", completionItems.contains("existingField"));
    }

    /**
     * 测试自定义约束注解：错误检查
     * Requirements: 8.4
     */
    public void testCustomConstraintAnnotation_ErrorCheck() {
        // 创建自定义约束注解
        myFixture.addFileToProject("test/CustomConstraint.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelConstraint;\n" +
                "import org.intellij.lang.annotations.Language;\n" +
                "import java.lang.annotation.*;\n" +
                "@Target(ElementType.FIELD)\n" +
                "@Retention(RetentionPolicy.RUNTIME)\n" +
                "@SpelConstraint\n" +
                "public @interface CustomConstraint {\n" +
                "  @Language(\"SpEL\")\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建使用自定义注解的测试类，引用不存在的字段
        myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "public class TestClass {\n" +
                "  private String existingField;\n" +
                "  \n" +
                "  @CustomConstraint(condition = \"#this.nonExistentField != null\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("nonExistentField"))
                .collect(Collectors.toList());
        
        // 验证：自定义约束注解中的字段引用也应该被检查
        assertFalse("Custom constraint annotation should check field existence", fieldErrors.isEmpty());
    }

    // ==================== Spring 插件协作测试 ====================

    /**
     * 测试与 Spring 插件的协作：无 Spring 插件时仍能工作
     * Requirements: 8.5
     * <p>
     * 注意：此测试验证插件在没有 Spring 插件时的基本功能
     * 完整的 Spring 协作测试需要在安装了 Spring 插件的环境中进行
     */
    public void testSpringPluginCollaboration_WorksWithoutSpring() {
        // 创建测试类
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.<caret>\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 测试补全功能在没有 Spring 插件时仍然工作
        LookupElement[] elements = myFixture.completeBasic();
        assertNotNull("Completion should work without Spring plugin", elements);
        assertTrue("Should have completion elements", elements.length > 0);
        
        List<String> completionItems = Arrays.stream(elements)
                .map(LookupElement::getLookupString)
                .collect(Collectors.toList());

        assertTrue("Should contain userName field", completionItems.contains("userName"));
    }

    // ==================== 多注解测试 ====================

    /**
     * 测试多个约束注解在同一字段上
     * Requirements: 8.4
     */
    public void testMultipleConstraintAnnotations() {
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "import cn.sticki.spel.validator.constrain.SpelAssert;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  private Integer age;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.userName != null\")\n" +
                "  @SpelAssert(assertTrue = \"#this.age > 0\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("不存在"))
                .collect(Collectors.toList());
        
        // 验证：所有字段都存在，不应该有错误
        assertTrue("All fields exist, should not have error marker", fieldErrors.isEmpty());
    }

    // ==================== 边界情况测试 ====================

    /**
     * 测试空表达式
     * Requirements: 8.4
     */
    public void testEmptyExpression() {
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("不存在"))
                .collect(Collectors.toList());
        
        // 验证：空表达式不应该有错误
        assertTrue("Empty expression should not have error marker", fieldErrors.isEmpty());
    }

    /**
     * 测试非 #this 表达式
     * Requirements: 8.4
     */
    public void testNonThisExpression() {
        myFixture.configureByText("UserDto.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class UserDto {\n" +
                "  private String userName;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"true\")\n" +
                "  private String validatedField;\n" +
                "}"
        );

        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("不存在"))
                .collect(Collectors.toList());
        
        // 验证：非 #this 表达式不应该有字段不存在的错误
        assertTrue("Non-#this expression should not have field error marker", fieldErrors.isEmpty());
    }
}
