package cn.sticki.spel.validator.support.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.Tag;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SpelFieldAnnotator 属性测试
 * 
 * Property 8: 字段存在性检查的准确性
 * 对于任何 SpEL 表达式中的字段引用，如果字段不存在，则应显示错误标记；
 * 如果字段存在，则不应显示错误标记。
 * 
 * Validates: Requirements 6.1, 6.3
 * 
 * @author Sticki
 */
public class SpelFieldAnnotatorPropertyTest extends BasePlatformTestCase {
    
    /**
     * Property 8: 字段存在性检查的准确性 - 存在的字段不应显示错误
     * 
     * 测试策略：生成多种类型的字段，验证存在的字段不会被标记为错误
     * 
     * Validates: Requirements 6.3
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 8: 字段存在性检查的准确性")
    public void testExistingFields_NoErrors() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建测试类，包含多种类型的字段
        myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class TestClass {\n" +
                "  private String stringField;\n" +
                "  private int intField;\n" +
                "  private Double doubleField;\n" +
                "  protected boolean booleanField;\n" +
                "  public Object objectField;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.stringField != null\")\n" +
                "  private String field1;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.intField > 0\")\n" +
                "  private String field2;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.doubleField != null\")\n" +
                "  private String field3;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.booleanField == true\")\n" +
                "  private String field4;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.objectField != null\")\n" +
                "  private String field5;\n" +
                "}"
        );
        
        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出错误级别的高亮
        List<HighlightInfo> errors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("不存在"))
                .collect(Collectors.toList());
        
        // 验证：存在的字段不应该有错误标记
        assertTrue("Existing fields should not have error markers, but found: " + 
                errors.stream().map(HighlightInfo::getDescription).collect(Collectors.joining(", ")),
                errors.isEmpty());
    }
    
    /**
     * Property 8: 字段存在性检查的准确性 - 不存在的字段应显示错误
     * 
     * 测试策略：引用不存在的字段，验证会被标记为错误
     * 
     * Validates: Requirements 6.1
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 8: 字段存在性检查的准确性")
    public void testNonExistentFields_ShowErrors() {
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
    }
    
    /**
     * Property 8: 字段存在性检查的准确性 - 父类字段应被识别
     * 
     * 测试策略：引用父类中定义的字段，验证不会被标记为错误
     * 
     * Validates: Requirements 6.3
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 8: 字段存在性检查的准确性")
    public void testInheritedFields_NoErrors() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建父类
        myFixture.addFileToProject("test/ParentClass.java",
                "package test;\n" +
                "public class ParentClass {\n" +
                "  protected String parentField;\n" +
                "  private int parentPrivateField;\n" +
                "}"
        );
        
        // 创建子类，引用父类字段
        myFixture.configureByText("ChildClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class ChildClass extends ParentClass {\n" +
                "  @SpelNotNull(condition = \"#this.parentField != null\")\n" +
                "  private String childField;\n" +
                "}"
        );
        
        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("不存在"))
                .collect(Collectors.toList());
        
        // 验证：父类字段不应该有错误标记
        assertTrue("Inherited fields should not have error markers, but found: " + 
                fieldErrors.stream().map(HighlightInfo::getDescription).collect(Collectors.joining(", ")),
                fieldErrors.isEmpty());
    }
    
    /**
     * Property 8: 字段存在性检查的准确性 - 多个字段引用混合测试
     * 
     * 测试策略：同时引用存在和不存在的字段，验证只有不存在的字段被标记
     * 
     * Validates: Requirements 6.1, 6.3
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 8: 字段存在性检查的准确性")
    public void testMixedFields_OnlyNonExistentShowErrors() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建测试类
        myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class TestClass {\n" +
                "  private String existingField;\n" +
                "  private int anotherExisting;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.existingField != null && #this.missingField == null\")\n" +
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
        
        // 验证：只有 missingField 应该有错误标记
        assertEquals("Only non-existent field should have error marker", 1, fieldErrors.size());
        assertTrue("Error should be for 'missingField'", 
                fieldErrors.get(0).getDescription().contains("missingField"));
    }
}
