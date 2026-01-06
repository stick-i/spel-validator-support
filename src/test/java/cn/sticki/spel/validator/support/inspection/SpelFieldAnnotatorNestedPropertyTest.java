package cn.sticki.spel.validator.support.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.Tag;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SpelFieldAnnotator 嵌套字段属性测试
 * 
 * Property 9: 嵌套字段错误检查的准确性
 * 对于任何嵌套字段引用，如果中间某级字段不存在，则应在该字段名下显示错误标记。
 * 
 * Validates: Requirements 6.4
 * 
 * @author Sticki
 */
public class SpelFieldAnnotatorNestedPropertyTest extends BasePlatformTestCase {
    
    /**
     * Property 9: 嵌套字段错误检查的准确性 - 存在的嵌套字段不应显示错误
     * 
     * 测试策略：创建嵌套类结构，验证存在的嵌套字段不会被标记为错误
     * 
     * Validates: Requirements 6.4
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 9: 嵌套字段错误检查的准确性")
    public void testExistingNestedFields_NoErrors() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建嵌套类结构
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "  private String street;\n" +
                "  private Country country;\n" +
                "}"
        );
        
        myFixture.addFileToProject("test/Country.java",
                "package test;\n" +
                "public class Country {\n" +
                "  private String name;\n" +
                "  private String code;\n" +
                "}"
        );
        
        // 创建测试类，引用嵌套字段
        myFixture.configureByText("User.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class User {\n" +
                "  private Address address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.city != null\")\n" +
                "  private String field1;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.street != null\")\n" +
                "  private String field2;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.country.name != null\")\n" +
                "  private String field3;\n" +
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
        assertTrue("Existing nested fields should not have error markers, but found: " + 
                fieldErrors.stream().map(HighlightInfo::getDescription).collect(Collectors.joining(", ")),
                fieldErrors.isEmpty());
    }
    
    /**
     * Property 9: 嵌套字段错误检查的准确性 - 第一级字段不存在应显示错误
     * 
     * 测试策略：引用不存在的第一级字段，验证会被标记为错误
     * 
     * Validates: Requirements 6.4
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 9: 嵌套字段错误检查的准确性")
    public void testNonExistentFirstLevelField_ShowsError() {
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
                "  @SpelNotNull(condition = \"#this.nonExistent.city != null\")\n" +
                "  private String field1;\n" +
                "}"
        );
        
        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("nonExistent"))
                .collect(Collectors.toList());
        
        // 验证：不存在的第一级字段应该有错误标记
        assertFalse("Non-existent first level field should have error marker", fieldErrors.isEmpty());
    }
    
    /**
     * Property 9: 嵌套字段错误检查的准确性 - 第二级字段不存在应显示错误
     * 
     * 测试策略：引用不存在的第二级字段，验证会被标记为错误
     * 
     * Validates: Requirements 6.4
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 9: 嵌套字段错误检查的准确性")
    public void testNonExistentSecondLevelField_ShowsError() {
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
                "  @SpelNotNull(condition = \"#this.address.nonExistentField != null\")\n" +
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
        
        // 验证：不存在的第二级字段应该有错误标记
        assertFalse("Non-existent second level field should have error marker", fieldErrors.isEmpty());
    }
    
    /**
     * Property 9: 嵌套字段错误检查的准确性 - 深层嵌套字段不存在应显示错误
     * 
     * 测试策略：引用不存在的深层嵌套字段，验证会被标记为错误
     * 
     * Validates: Requirements 6.4
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 9: 嵌套字段错误检查的准确性")
    public void testNonExistentDeepNestedField_ShowsError() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建嵌套类结构
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private Country country;\n" +
                "}"
        );
        
        myFixture.addFileToProject("test/Country.java",
                "package test;\n" +
                "public class Country {\n" +
                "  private String name;\n" +
                "}"
        );
        
        // 创建测试类，引用不存在的深层嵌套字段
        myFixture.configureByText("User.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class User {\n" +
                "  private Address address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.country.nonExistent != null\")\n" +
                "  private String field1;\n" +
                "}"
        );
        
        // 执行高亮检查
        List<HighlightInfo> highlights = myFixture.doHighlighting();
        
        // 过滤出字段不存在的错误
        List<HighlightInfo> fieldErrors = highlights.stream()
                .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
                .filter(h -> h.getDescription() != null && h.getDescription().contains("nonExistent"))
                .collect(Collectors.toList());
        
        // 验证：不存在的深层嵌套字段应该有错误标记
        assertFalse("Non-existent deep nested field should have error marker", fieldErrors.isEmpty());
    }
}
