package cn.sticki.spel.validator.support.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SpelFieldAnnotator 单元测试
 * 
 * 测试字段存在性错误检查功能
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4
 * 
 * @author Sticki
 */
public class SpelFieldAnnotatorTest extends BasePlatformTestCase {
    
    /**
     * 测试不存在字段的错误标记
     * Requirements: 6.1, 6.2
     */
    public void testNonExistentField_ShowsError() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建测试类，引用不存在的字段
        myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class TestClass {\n" +
                "  private String existingField;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.nonExistentField != null\")\n" +
                "  private String field1;\n" +
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
        assertTrue("Error message should mention the field name", 
                fieldErrors.get(0).getDescription().contains("nonExistentField"));
    }
    
    /**
     * 测试嵌套字段的错误检查 - 第一级字段不存在
     * Requirements: 6.4
     */
    public void testNestedField_FirstLevelNotExist_ShowsError() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建嵌套类
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "}"
        );
        
        // 创建测试类，引用不存在的第一级字段
        myFixture.configureByText("User.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class User {\n" +
                "  private Address address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.wrongField.city != null\")\n" +
                "  private String field1;\n" +
                "}"
        );
        
        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("wrongField"))
                .collect(Collectors.toList());
        
        // 验证：不存在的第一级字段应该有错误标记
        assertFalse("Non-existent first level field should have error marker", fieldErrors.isEmpty());
    }
    
    /**
     * 测试嵌套字段的错误检查 - 第二级字段不存在
     * Requirements: 6.4
     */
    public void testNestedField_SecondLevelNotExist_ShowsError() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建嵌套类
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "}"
        );
        
        // 创建测试类，引用不存在的第二级字段
        myFixture.configureByText("User.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class User {\n" +
                "  private Address address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.wrongField != null\")\n" +
                "  private String field1;\n" +
                "}"
        );
        
        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("wrongField"))
                .collect(Collectors.toList());
        
        // 验证：不存在的第二级字段应该有错误标记
        assertFalse("Non-existent second level field should have error marker", fieldErrors.isEmpty());
    }
    
    /**
     * 测试正确字段不显示错误
     * Requirements: 6.3
     */
    public void testExistingField_NoError() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建测试类，引用存在的字段
        myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class TestClass {\n" +
                "  private String existingField;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.existingField != null\")\n" +
                "  private String field1;\n" +
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
     * 测试正确的嵌套字段不显示错误
     * Requirements: 6.3, 6.4
     */
    public void testExistingNestedField_NoError() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建嵌套类
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "}"
        );
        
        // 创建测试类，引用存在的嵌套字段
        myFixture.configureByText("User.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class User {\n" +
                "  private Address address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.city != null\")\n" +
                "  private String field1;\n" +
                "}"
        );
        
        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("不存在"))
                .collect(Collectors.toList());
        
        // 验证：存在的嵌套字段不应该有错误标记
        assertTrue("Existing nested field should not have error marker", fieldErrors.isEmpty());
    }
    
    /**
     * 测试非约束注解中的字段引用不检查
     * Requirements: 6.1
     */
    public void testNonConstraintAnnotation_NoCheck() {
        // 创建一个普通注解（非约束注解）
        myFixture.configureByText("MyAnnotation.java",
                "package test;\n" +
                "public @interface MyAnnotation {\n" +
                "  String value() default \"\";\n" +
                "}"
        );
        
        // 创建测试类，在普通注解中引用不存在的字段
        myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "public class TestClass {\n" +
                "  private String existingField;\n" +
                "  \n" +
                "  @MyAnnotation(value = \"#this.nonExistentField != null\")\n" +
                "  private String field1;\n" +
                "}"
        );
        
        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("不存在"))
                .collect(Collectors.toList());
        
        // 验证：非约束注解中的字段引用不应该被检查
        assertTrue("Non-constraint annotation should not be checked", fieldErrors.isEmpty());
    }
    
    /**
     * 测试自定义约束注解中的字段检查
     * Requirements: 6.1
     */
    public void testCustomConstraintAnnotation_ChecksField() {
        // 创建 SpelConstraint 元注解
        myFixture.addFileToProject("cn/sticki/spel/validator/constrain/SpelConstraint.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "import java.lang.annotation.*;\n" +
                "@Target(ElementType.ANNOTATION_TYPE)\n" +
                "@Retention(RetentionPolicy.RUNTIME)\n" +
                "public @interface SpelConstraint {\n" +
                "}"
        );
        
        // 创建自定义约束注解
        myFixture.addFileToProject("test/CustomConstraint.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelConstraint;\n" +
                "@SpelConstraint\n" +
                "public @interface CustomConstraint {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建测试类，在自定义约束注解中引用不存在的字段
        myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "public class TestClass {\n" +
                "  private String existingField;\n" +
                "  \n" +
                "  @CustomConstraint(condition = \"#this.nonExistentField != null\")\n" +
                "  private String field1;\n" +
                "}"
        );
        
        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("nonExistentField"))
                .collect(Collectors.toList());
        
        // 验证：自定义约束注解中的字段引用应该被检查
        assertFalse("Custom constraint annotation should check field existence", fieldErrors.isEmpty());
    }
}
