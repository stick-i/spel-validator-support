package cn.sticki.spel.validator.support.util;

import com.intellij.psi.*;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * SpelValidatorUtil 单元测试
 * 
 * 注意：这些测试需要完整的 IntelliJ Platform 测试环境。
 * 测试将在集成测试阶段使用 BasePlatformTestCase 实现。
 * 
 * @author Sticki
 */
public class SpelValidatorUtilTest extends BasePlatformTestCase {
    
    /**
     * 测试内置约束注解识别
     * Requirements: 1.1, 1.2
     */
    public void testIsSpelConstraintAnnotation_BuiltInAnnotations() {
        // 创建测试类，包含 SpelNotNull 注解
        PsiClass testClass = myFixture.addClass(
            "package test;\n" +
            "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
            "public class TestClass {\n" +
            "  @SpelNotNull(condition = \"true\")\n" +
            "  private String field;\n" +
            "}"
        );
        
        PsiField field = testClass.getFields()[0];
        PsiAnnotation annotation = field.getAnnotations()[0];
        
        // 验证：应该被识别为约束注解
        assertTrue(
            "Built-in SpelNotNull annotation should be recognized",
            SpelValidatorUtil.isSpelConstraintAnnotation(annotation)
        );
    }
    
    /**
     * 测试自定义约束注解识别（带 @SpelConstraint 元注解）
     * Requirements: 1.2, 1.3
     */
    public void testIsSpelConstraintAnnotation_CustomAnnotation() {
        // 创建 @SpelConstraint 元注解
        myFixture.addClass(
            "package cn.sticki.spel.validator.constrain;\n" +
            "public @interface SpelConstraint {}"
        );
        
        // 创建自定义约束注解
        myFixture.addClass(
            "package com.example;\n" +
            "import cn.sticki.spel.validator.constrain.SpelConstraint;\n" +
            "@SpelConstraint\n" +
            "public @interface CustomConstraint {\n" +
            "  String condition() default \"\";\n" +
            "}"
        );
        
        // 创建使用自定义注解的测试类
        PsiClass testClass = myFixture.addClass(
            "package test;\n" +
            "import com.example.CustomConstraint;\n" +
            "public class TestClass {\n" +
            "  @CustomConstraint(condition = \"true\")\n" +
            "  private String field;\n" +
            "}"
        );
        
        PsiField field = testClass.getFields()[0];
        PsiAnnotation annotation = field.getAnnotations()[0];
        
        // 验证：自定义注解应该被识别为约束注解
        assertTrue(
            "Custom annotation with @SpelConstraint should be recognized",
            SpelValidatorUtil.isSpelConstraintAnnotation(annotation)
        );
    }
    
    /**
     * 测试非约束注解不被识别
     * Requirements: 1.1
     */
    public void testIsSpelConstraintAnnotation_NonConstraintAnnotation() {
        // 创建测试类，包含非约束注解
        PsiClass testClass = myFixture.addClass(
            "package test;\n" +
            "public class TestClass {\n" +
            "  @Deprecated\n" +
            "  private String field;\n" +
            "}"
        );
        
        PsiField field = testClass.getFields()[0];
        PsiAnnotation annotation = field.getAnnotations()[0];
        
        // 验证：不应该被识别为约束注解
        assertFalse(
            "Non-constraint annotation should not be recognized",
            SpelValidatorUtil.isSpelConstraintAnnotation(annotation)
        );
    }
    
    /**
     * 测试获取上下文类
     * Requirements: 3.1
     */
    public void testGetContextClass() {
        // 创建测试类
        PsiClass testClass = myFixture.addClass(
            "package test;\n" +
            "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
            "public class TestClass {\n" +
            "  @SpelNotNull(condition = \"true\")\n" +
            "  private String field;\n" +
            "}"
        );
        
        PsiField field = testClass.getFields()[0];
        PsiAnnotation annotation = field.getAnnotations()[0];
        
        // 获取上下文类
        PsiClass contextClass = SpelValidatorUtil.getContextClass(annotation);
        
        // 验证：应该返回 TestClass
        assertNotNull("Context class should not be null", contextClass);
        assertEquals("Context class should be TestClass", "TestClass", contextClass.getName());
    }
    
    /**
     * 测试获取所有字段（包括父类字段）
     * Requirements: 3.2, 3.3
     */
    public void testGetAllFields() {
        // 创建父类
        myFixture.addClass(
            "package test;\n" +
            "public class ParentClass {\n" +
            "  private String parentField;\n" +
            "  protected int parentProtectedField;\n" +
            "}"
        );
        
        // 创建子类
        PsiClass childClass = myFixture.addClass(
            "package test;\n" +
            "public class ChildClass extends ParentClass {\n" +
            "  private String childField;\n" +
            "  public double childPublicField;\n" +
            "}"
        );
        
        // 获取所有字段
        java.util.List<PsiField> allFields = SpelValidatorUtil.getAllFields(childClass);
        
        // 验证：应该包含子类和父类的所有字段
        assertNotNull("Field list should not be null", allFields);
        assertEquals("Should have 4 fields (2 from child + 2 from parent)", 4, allFields.size());
        
        // 验证字段名
        java.util.Set<String> fieldNames = new java.util.HashSet<>();
        for (PsiField field : allFields) {
            fieldNames.add(field.getName());
        }
        
        assertTrue("Should contain childField", fieldNames.contains("childField"));
        assertTrue("Should contain childPublicField", fieldNames.contains("childPublicField"));
        assertTrue("Should contain parentField", fieldNames.contains("parentField"));
        assertTrue("Should contain parentProtectedField", fieldNames.contains("parentProtectedField"));
    }
    
    /**
     * 测试嵌套字段解析
     * Requirements: 3.5, 4.4
     */
    public void testResolveNestedField() {
        // 创建嵌套类结构
        myFixture.addClass(
            "package test;\n" +
            "public class Address {\n" +
            "  private String city;\n" +
            "  private String street;\n" +
            "}"
        );
        
        myFixture.addClass(
            "package test;\n" +
            "public class User {\n" +
            "  private String name;\n" +
            "  private Address address;\n" +
            "}"
        );
        
        PsiClass userClass = myFixture.findClass("test.User");
        
        // 测试简单字段解析
        PsiField nameField = SpelValidatorUtil.resolveNestedField(userClass, "name");
        assertNotNull("Should resolve 'name' field", nameField);
        assertEquals("Field name should be 'name'", "name", nameField.getName());
        
        // 测试嵌套字段解析
        PsiField cityField = SpelValidatorUtil.resolveNestedField(userClass, "address.city");
        assertNotNull("Should resolve 'address.city' field", cityField);
        assertEquals("Field name should be 'city'", "city", cityField.getName());
        
        // 测试不存在的字段
        PsiField nonExistentField = SpelValidatorUtil.resolveNestedField(userClass, "nonExistent");
        assertNull("Should return null for non-existent field", nonExistentField);
        
        // 测试不存在的嵌套字段
        PsiField nonExistentNestedField = SpelValidatorUtil.resolveNestedField(userClass, "address.nonExistent");
        assertNull("Should return null for non-existent nested field", nonExistentNestedField);
    }
    
    /**
     * 测试空值处理
     */
    public void testNullHandling() {
        // 测试 isSpelConstraintAnnotation 的空值处理
        assertFalse("Should return false for null annotation", 
            SpelValidatorUtil.isSpelConstraintAnnotation(null));
        
        // 测试 isSpelLanguageAttribute 的空值处理
        assertFalse("Should return false for null method", 
            SpelValidatorUtil.isSpelLanguageAttribute(null));
        
        // 测试 getContextClass 的空值处理
        assertNull("Should return null for null annotation", 
            SpelValidatorUtil.getContextClass(null));
        
        // 测试 getAllFields 的空值处理
        java.util.List<PsiField> fields = SpelValidatorUtil.getAllFields(null);
        assertNotNull("Should return empty list for null class", fields);
        assertTrue("Should return empty list for null class", fields.isEmpty());
        
        // 测试 resolveNestedField 的空值处理
        assertNull("Should return null for null class", 
            SpelValidatorUtil.resolveNestedField(null, "field"));
        
        PsiClass testClass = myFixture.addClass("public class TestClass {}");
        assertNull("Should return null for null field path", 
            SpelValidatorUtil.resolveNestedField(testClass, null));
        assertNull("Should return null for empty field path", 
            SpelValidatorUtil.resolveNestedField(testClass, ""));
    }
}
