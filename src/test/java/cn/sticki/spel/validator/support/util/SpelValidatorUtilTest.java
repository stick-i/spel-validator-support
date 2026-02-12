package cn.sticki.spel.validator.support.util;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * SpelValidatorUtil 单元测试
 * <p>
 * 使用 JUnit 5 断言和 IntelliJ Platform 测试框架
 * 继承 BasePlatformTestCase 提供完整的 PSI 测试环境
 *
 * @author Sticki
 */
public class SpelValidatorUtilTest extends BasePlatformTestCase {

    /**
     * 测试内置约束注解识别
     * Requirements: 1.1, 1.2
     */
    public void testIsSpelConstraintAnnotation_BuiltInAnnotations() {
        // 创建 SpelNotNull 注解类
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );

        // 创建测试类，包含 SpelNotNull 注解
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class TestClass {\n" +
                "  @SpelNotNull(condition = \"true\")\n" +
                "  private String field;\n" +
                "}"
        );

        PsiClass testClass = javaFile.getClasses()[0];
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
        myFixture.configureByText("SpelConstraint.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelConstraint {}"
        );

        // 创建自定义约束注解
        myFixture.configureByText("CustomConstraint.java",
                "package com.example;\n" +
                "import cn.sticki.spel.validator.constrain.SpelConstraint;\n" +
                "@SpelConstraint\n" +
                "public @interface CustomConstraint {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );

        // 创建使用自定义注解的测试类
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import com.example.CustomConstraint;\n" +
                "public class TestClass {\n" +
                "  @CustomConstraint(condition = \"true\")\n" +
                "  private String field;\n" +
                "}"
        );

        PsiClass testClass = javaFile.getClasses()[0];
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
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "public class TestClass {\n" +
                "  @Deprecated\n" +
                "  private String field;\n" +
                "}"
        );

        PsiClass testClass = javaFile.getClasses()[0];
        PsiField field = testClass.getFields()[0];
        PsiAnnotation annotation = field.getAnnotations()[0];

        // 验证：不应该被识别为约束注解
        assertFalse(
                "Non-constraint annotation should not be recognized",
                SpelValidatorUtil.isSpelConstraintAnnotation(annotation)
        );
    }

    /**
     * 测试 Jakarta 版本的 SpelValid 注解识别
     */
    public void testIsSpelValidAnnotation_Jakarta() {
        // 创建 Jakarta 版本的 SpelValid 注解
        myFixture.configureByText("SpelValid.java",
                "package cn.sticki.spel.validator.jakarta;\n" +
                "public @interface SpelValid {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );

        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.jakarta.SpelValid;\n" +
                "@SpelValid(condition = \"true\")\n" +
                "public class TestClass {\n" +
                "  private String field;\n" +
                "}"
        );

        PsiClass testClass = javaFile.getClasses()[0];
        PsiAnnotation annotation = testClass.getAnnotations()[0];

        assertTrue("Jakarta SpelValid should be recognized by isSpelValidAnnotation",
                SpelValidatorUtil.isSpelValidAnnotation(annotation));
        assertFalse("Jakarta SpelValid should NOT be recognized by isSpelConstraintAnnotation",
                SpelValidatorUtil.isSpelConstraintAnnotation(annotation));
        assertTrue("Jakarta SpelValid should be recognized by isSpelValidatorAnnotation",
                SpelValidatorUtil.isSpelValidatorAnnotation(annotation));
    }

    /**
     * 测试 Javax 版本的 SpelValid 注解识别
     */
    public void testIsSpelValidAnnotation_Javax() {
        myFixture.configureByText("SpelValid.java",
                "package cn.sticki.spel.validator.javax;\n" +
                "public @interface SpelValid {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );

        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.javax.SpelValid;\n" +
                "@SpelValid(condition = \"true\")\n" +
                "public class TestClass {\n" +
                "  private String field;\n" +
                "}"
        );

        PsiClass testClass = javaFile.getClasses()[0];
        PsiAnnotation annotation = testClass.getAnnotations()[0];

        assertTrue("Javax SpelValid should be recognized by isSpelValidAnnotation",
                SpelValidatorUtil.isSpelValidAnnotation(annotation));
        assertTrue("Javax SpelValid should be recognized by isSpelValidatorAnnotation",
                SpelValidatorUtil.isSpelValidatorAnnotation(annotation));
    }

    /**
     * 测试非 SpelValid 注解不会被误判
     */
    public void testIsSpelValidAnnotation_NonSpelValid() {
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "@Deprecated\n" +
                "public class TestClass {\n" +
                "  private String field;\n" +
                "}"
        );

        PsiClass testClass = javaFile.getClasses()[0];
        PsiAnnotation annotation = testClass.getAnnotations()[0];

        assertFalse("Non-SpelValid annotation should not be recognized",
                SpelValidatorUtil.isSpelValidAnnotation(annotation));
    }

    /**
     * 测试 isSpelValidatorAnnotation 统一入口覆盖约束注解
     */
    public void testIsSpelValidatorAnnotation_ConstraintAnnotation() {
        myFixture.configureByText("SpelNotNull.java",
                "package cn.sticki.spel.validator.constrain;\n" +
                "public @interface SpelNotNull {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );

        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.constrain.SpelNotNull;\n" +
                "public class TestClass {\n" +
                "  @SpelNotNull(condition = \"true\")\n" +
                "  private String field;\n" +
                "}"
        );

        PsiClass testClass = javaFile.getClasses()[0];
        PsiField field = testClass.getFields()[0];
        PsiAnnotation annotation = field.getAnnotations()[0];

        assertTrue("Constraint annotation should also be recognized by isSpelValidatorAnnotation",
                SpelValidatorUtil.isSpelValidatorAnnotation(annotation));
    }

    /**
     * 测试 SpelValid 注解的 getContextClass（类级注解场景）
     */
    public void testGetContextClass_SpelValidOnClass() {
        myFixture.configureByText("SpelValid.java",
                "package cn.sticki.spel.validator.jakarta;\n" +
                "public @interface SpelValid {\n" +
                "  String condition() default \"\";\n" +
                "}"
        );

        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "import cn.sticki.spel.validator.jakarta.SpelValid;\n" +
                "@SpelValid(condition = \"true\")\n" +
                "public class TestClass {\n" +
                "  private String name;\n" +
                "}"
        );

        PsiClass testClass = javaFile.getClasses()[0];
        PsiAnnotation annotation = testClass.getAnnotations()[0];

        PsiClass contextClass = SpelValidatorUtil.getContextClass(annotation);
        assertNotNull("Context class should not be null for class-level SpelValid", contextClass);
        assertEquals("Context class should be TestClass", "TestClass", contextClass.getName());
    }

    /**
     * 测试获取上下文类
     * Requirements: 3.1
     */
    public void testGetContextClass() {
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
                "  @SpelNotNull(condition = \"true\")\n" +
                "  private String field;\n" +
                "}"
        );

        PsiClass testClass = javaFile.getClasses()[0];
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
        PsiJavaFile parentFile = (PsiJavaFile) myFixture.addFileToProject("test/ParentClass.java",
                "package test;\n" +
                "public class ParentClass {\n" +
                "  private String parentField;\n" +
                "  protected int parentProtectedField;\n" +
                "}"
        );

        // 创建子类
        PsiJavaFile childFile = (PsiJavaFile) myFixture.addFileToProject("test/ChildClass.java",
                "package test;\n" +
                "public class ChildClass extends ParentClass {\n" +
                "  private String childField;\n" +
                "  public double childPublicField;\n" +
                "}"
        );

        PsiClass childClass = childFile.getClasses()[0];

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
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "  private String street;\n" +
                "}"
        );

        PsiJavaFile userFile = (PsiJavaFile) myFixture.addFileToProject("test/User.java",
                "package test;\n" +
                "public class User {\n" +
                "  private String name;\n" +
                "  private Address address;\n" +
                "}"
        );

        PsiClass userClass = userFile.getClasses()[0];

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

        // 测试 isSpelValidAnnotation 的空值处理
        assertFalse("isSpelValidAnnotation should return false for null",
                SpelValidatorUtil.isSpelValidAnnotation(null));

        // 测试 isSpelValidatorAnnotation 的空值处理
        assertFalse("isSpelValidatorAnnotation should return false for null",
                SpelValidatorUtil.isSpelValidatorAnnotation(null));

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

        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "public class TestClass {}");
        PsiClass testClass = javaFile.getClasses()[0];
        assertNull("Should return null for null field path",
                SpelValidatorUtil.resolveNestedField(testClass, null));
        assertNull("Should return null for empty field path",
                SpelValidatorUtil.resolveNestedField(testClass, ""));
    }

}
