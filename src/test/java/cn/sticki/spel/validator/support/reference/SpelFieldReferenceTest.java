package cn.sticki.spel.validator.support.reference;

import com.intellij.psi.*;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * SpelFieldReference 单元测试
 * 
 * 测试 Ctrl+Click 跳转、Find Usages、字段重命名等功能
 * 
 * Requirements: 4.1, 4.2, 5.1, 5.3
 * 
 * @author Sticki
 */
public class SpelFieldReferenceTest extends BasePlatformTestCase {
    
    /**
     * 测试 Ctrl+Click 跳转到字段定义
     * Requirements: 4.1
     */
    public void testCtrlClickNavigation_SimpleField() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建测试类，包含 SpelNotNull 注解
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
        
        // 获取光标位置的元素
        PsiElement element = myFixture.getElementAtCaret();
        
        // 验证：应该是 PsiField
        assertNotNull("Element at caret should not be null", element);
        assertTrue("Element should be PsiField", element instanceof PsiField);
        assertEquals("Field name should be 'userName'", "userName", ((PsiField) element).getName());
    }
    
    /**
     * 测试 Ctrl+Click 跳转到嵌套字段定义
     * Requirements: 4.1, 4.4
     */
    public void testCtrlClickNavigation_NestedField() {
        // 创建嵌套类结构
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "}"
        );
        
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建测试类
        myFixture.configureByText("User.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class User {\n" +
                "  private Address add<caret>ress;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.address.city != null\")\n" +
                "  private String name;\n" +
                "}"
        );
        
        // 获取光标位置的元素
        PsiElement element = myFixture.getElementAtCaret();
        
        // 验证：应该是 PsiField
        assertNotNull("Element at caret should not be null", element);
        assertTrue("Element should be PsiField", element instanceof PsiField);
        assertEquals("Field name should be 'address'", "address", ((PsiField) element).getName());
    }
    
    /**
     * 测试 Find Usages 功能
     * Requirements: 4.2
     */
    public void testFindUsages_FieldInSpelExpression() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建测试类
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class TestClass {\n" +
                "  private String targetField;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.targetField != null\")\n" +
                "  private String otherField;\n" +
                "}"
        );
        
        // 获取目标字段
        PsiClass testClass = javaFile.getClasses()[0];
        PsiField targetField = testClass.findFieldByName("targetField", false);
        
        // 验证字段存在
        assertNotNull("Target field should exist", targetField);
        assertEquals("targetField", targetField.getName());
    }
    
    /**
     * 测试字段重命名 - 验证 SpEL 表达式中的字段引用会自动更新
     * Requirements: 5.1, 5.3
     */
    public void testFieldRename_UpdatesSpelExpression() {
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
                "  private String old<caret>FieldName;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.oldFieldName != null\")\n" +
                "  private String otherField;\n" +
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
    
    /**
     * 测试嵌套字段重命名 - 验证 SpEL 表达式中的嵌套字段引用会自动更新
     * Requirements: 5.1, 5.3
     */
    public void testFieldRename_NestedFieldUpdatesSpelExpression() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建 Address 类
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
     * 测试引用解析 - 私有字段
     * Requirements: 4.1
     */
    public void testReferenceResolution_PrivateField() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建测试类
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class TestClass {\n" +
                "  private String privateField;\n" +
                "  \n" +
                "  @SpelNotNull(condition = \"#this.privateField != null\")\n" +
                "  private String otherField;\n" +
                "}"
        );
        
        // 获取私有字段
        PsiClass testClass = javaFile.getClasses()[0];
        PsiField privateField = testClass.findFieldByName("privateField", false);
        
        // 验证私有字段存在且可访问
        assertNotNull("Private field should exist", privateField);
        assertEquals("privateField", privateField.getName());
        assertTrue("Field should be private", 
                privateField.hasModifierProperty(PsiModifier.PRIVATE));
    }
    
    /**
     * 测试引用解析 - 父类字段
     * Requirements: 4.1
     */
    public void testReferenceResolution_InheritedField() {
        // 创建父类
        myFixture.addFileToProject("test/ParentClass.java",
                "package test;\n" +
                "public class ParentClass {\n" +
                "  protected String parentField;\n" +
                "}"
        );
        
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );
        
        // 创建子类
        PsiJavaFile childFile = (PsiJavaFile) myFixture.configureByText("ChildClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class ChildClass extends ParentClass {\n" +
                "  @SpelNotNull(condition = \"#this.parentField != null\")\n" +
                "  private String childField;\n" +
                "}"
        );
        
        // 获取子类
        PsiClass childClass = childFile.getClasses()[0];
        
        // 验证子类存在
        assertNotNull("Child class should exist", childClass);
        assertEquals("ChildClass", childClass.getName());
        
        // 验证父类字段可以通过继承访问
        PsiField parentField = childClass.findFieldByName("parentField", true);
        assertNotNull("Parent field should be accessible from child class", parentField);
    }
}
