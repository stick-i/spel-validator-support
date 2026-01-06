package cn.sticki.spel.validator.support.reference;

import com.intellij.psi.*;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.Tag;

/**
 * SpelFieldReference 字段重命名属性测试
 * 
 * Property 7: 字段重命名的一致性
 * 对于任何字段重命名操作，SpEL 表达式中对该字段的所有引用应自动更新为新字段名。
 * 
 * Validates: Requirements 5.1
 * 
 * @author Sticki
 */
public class SpelFieldReferenceRenamePropertyTest extends BasePlatformTestCase {
    
    /**
     * Property 7: 字段重命名的一致性
     * 测试简单字段的重命名 - 验证 SpEL 表达式中的引用会自动更新
     * 
     * Validates: Requirements 5.1
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 7: 字段重命名的一致性")
    public void testFieldRename_SimpleField() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建测试类，光标放在要重命名的字段上
        myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class TestClass {\n" +
                "  private String user<caret>Name;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.userName != null\")\n" +
                "  private String email;\n" +
                "}"
        );
        
        // 执行重命名操作
        myFixture.renameElementAtCaret("newUserName");
        
        // 获取重命名后的文件内容
        String fileContent = myFixture.getFile().getText();
        
        // 验证字段已被重命名
        assertTrue("Field declaration should be renamed", 
                fileContent.contains("private String newUserName;"));
        assertFalse("Old field name should not exist in declaration", 
                fileContent.contains("private String userName;"));
        
        // 验证 SpEL 表达式中的引用也被更新
        assertTrue("SpEL expression should be updated with new field name", 
                fileContent.contains("#this.newUserName"));
        assertFalse("Old field name should not exist in SpEL expression", 
                fileContent.contains("#this.userName"));
    }
    
    /**
     * Property 7 (续): 测试嵌套字段的重命名
     * 
     * Validates: Requirements 5.1
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 7: 字段重命名的一致性")
    public void testFieldRename_NestedField() {
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
                "  private String cityName;\n" +
                "}"
        );
        
        // 创建测试类，光标放在要重命名的嵌套字段上
        myFixture.configureByText("User.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class User {\n" +
                "  private Address home<caret>Address;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.homeAddress.cityName != null\")\n" +
                "  private String name;\n" +
                "}"
        );
        
        // 执行重命名操作
        myFixture.renameElementAtCaret("workAddress");
        
        // 获取重命名后的文件内容
        String fileContent = myFixture.getFile().getText();
        
        // 验证字段已被重命名
        assertTrue("Field declaration should be renamed", 
                fileContent.contains("private Address workAddress;"));
        assertFalse("Old field name should not exist in declaration", 
                fileContent.contains("private Address homeAddress;"));
        
        // 验证 SpEL 表达式中的引用也被更新
        assertTrue("SpEL expression should be updated with new field name", 
                fileContent.contains("#this.workAddress.cityName"));
        assertFalse("Old field name should not exist in SpEL expression", 
                fileContent.contains("#this.homeAddress"));
    }
    
    /**
     * Property 7 (续): 测试多个 SpEL 表达式中引用同一字段的重命名
     * 
     * Validates: Requirements 5.1
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 7: 字段重命名的一致性")
    public void testFieldRename_MultipleReferences() {
        // 创建 SpelNotNull 和 SpelAssert 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        myFixture.addFileToProject("cn/sticki/spel/validator/constrain/SpelAssert.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelAssert {\n" +
                "  String assertTrue() default \"\";\n" +
                "}"
        );
        
        // 创建测试类，包含多个引用同一字段的 SpEL 表达式
        myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "import cn.sticki.spel.validator.constrain.SpelAssert;\n" +
                "public class TestClass {\n" +
                "  private String target<caret>Field;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.targetField != null\")\n" +
                "  private String field1;\n" +
                "  \n" +
                "  @SpelAssert(assertTrue = \"#this.targetField.length() > 0\")\n" +
                "  private String field2;\n" +
                "}"
        );
        
        // 执行重命名操作
        myFixture.renameElementAtCaret("renamedField");
        
        // 获取重命名后的文件内容
        String fileContent = myFixture.getFile().getText();
        
        // 验证字段已被重命名
        assertTrue("Field declaration should be renamed", 
                fileContent.contains("private String renamedField;"));
        
        // 验证所有 SpEL 表达式中的引用都被更新
        assertTrue("First SpEL expression should be updated", 
                fileContent.contains("#this.renamedField != null"));
        assertTrue("Second SpEL expression should be updated", 
                fileContent.contains("#this.renamedField.length()"));
        
        // 验证旧字段名不再存在
        assertFalse("Old field name should not exist anywhere", 
                fileContent.contains("targetField"));
    }
    
    /**
     * Property 7 (续): 测试父类字段的重命名
     * 
     * Validates: Requirements 5.1
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 7: 字段重命名的一致性")
    public void testFieldRename_InheritedField() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建父类，光标放在要重命名的字段上
        myFixture.configureByText("ParentClass.java",
                "package test;\n" +
                "public class ParentClass {\n" +
                "  protected String parent<caret>Field;\n" +
                "}"
        );
        
        // 创建子类，引用父类字段
        myFixture.addFileToProject("test/ChildClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class ChildClass extends ParentClass {\n" +
                "  @SpelNotNull(condition = \"#this.parentField != null\")\n" +
                "  private String childField;\n" +
                "}"
        );
        
        // 执行重命名操作
        myFixture.renameElementAtCaret("newParentField");
        
        // 获取父类文件内容
        String parentContent = myFixture.getFile().getText();
        
        // 验证父类字段已被重命名
        assertTrue("Parent field declaration should be renamed", 
                parentContent.contains("protected String newParentField;"));
        assertFalse("Old parent field name should not exist", 
                parentContent.contains("protected String parentField;"));
    }
}
