package cn.sticki.spel.validator.support.reference;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.Tag;

/**
 * SpelFieldReference 属性测试
 * 使用 IntelliJ Platform 测试框架进行基于属性的测试
 * 
 * @author Sticki
 */
public class SpelFieldReferencePropertyTest extends BasePlatformTestCase {
    
    /**
     * Property 5: 字段引用解析的正确性
     * 对于任何 SpEL 表达式中的字段引用 #this.fieldName，
     * 如果字段存在，则引用应解析为对应的 PsiField 对象。
     * 
     * Validates: Requirements 4.3
     * 
     * 测试策略：生成多种字段类型和访问修饰符的组合，验证引用解析的正确性
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 5: 字段引用解析的正确性")
    public void testFieldReferenceResolution_ExistingFields() {
        // 创建测试类，包含多种类型和访问修饰符的字段
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "public class TestClass {\n" +
                "  private String privateField;\n" +
                "  protected int protectedField;\n" +
                "  public double publicField;\n" +
                "  String packageField;\n" +
                "  private static final String CONSTANT = \"value\";\n" +
                "}"
        );
        
        PsiClass testClass = javaFile.getClasses()[0];
        
        // 测试所有字段的引用解析
        String[] fieldNames = {"privateField", "protectedField", "publicField", "packageField", "CONSTANT"};
        
        for (String fieldName : fieldNames) {
            // 创建字段引用
            PsiElement dummyElement = createDummyStringLiteral("#this." + fieldName);
            SpelFieldReference reference = new SpelFieldReference(
                    dummyElement,
                    new TextRange(6, 6 + fieldName.length()), // #this. 后的位置
                    fieldName,
                    testClass
            );
            
            // 解析引用
            PsiElement resolved = reference.resolve();
            
            // 验证：应该解析为对应的 PsiField
            assertNotNull("Field '" + fieldName + "' should be resolved", resolved);
            assertTrue("Resolved element should be PsiField", resolved instanceof PsiField);
            assertEquals("Resolved field name should match", fieldName, ((PsiField) resolved).getName());
        }
    }
    
    /**
     * Property 5 (续): 测试不存在字段的引用解析
     * 
     * Validates: Requirements 4.3
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 5: 字段引用解析的正确性")
    public void testFieldReferenceResolution_NonExistentFields() {
        // 创建测试类
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "public class TestClass {\n" +
                "  private String existingField;\n" +
                "}"
        );
        
        PsiClass testClass = javaFile.getClasses()[0];
        
        // 测试不存在的字段
        String[] nonExistentFields = {"nonExistent", "anotherMissing", "xyz"};
        
        for (String fieldName : nonExistentFields) {
            // 创建字段引用
            PsiElement dummyElement = createDummyStringLiteral("#this." + fieldName);
            SpelFieldReference reference = new SpelFieldReference(
                    dummyElement,
                    new TextRange(6, 6 + fieldName.length()),
                    fieldName,
                    testClass
            );
            
            // 解析引用
            PsiElement resolved = reference.resolve();
            
            // 验证：不存在的字段应该返回 null
            assertNull("Non-existent field '" + fieldName + "' should not be resolved", resolved);
        }
    }
    
    /**
     * Property 5 (续): 测试父类字段的引用解析
     * 
     * Validates: Requirements 4.3
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 5: 字段引用解析的正确性")
    public void testFieldReferenceResolution_InheritedFields() {
        // 创建父类
        myFixture.addFileToProject("test/ParentClass.java",
                "package test;\n" +
                "public class ParentClass {\n" +
                "  protected String parentField;\n" +
                "  private int parentPrivateField;\n" +
                "}"
        );
        
        // 创建子类
        PsiJavaFile childFile = (PsiJavaFile) myFixture.addFileToProject("test/ChildClass.java",
                "package test;\n" +
                "public class ChildClass extends ParentClass {\n" +
                "  private String childField;\n" +
                "}"
        );
        
        PsiClass childClass = childFile.getClasses()[0];
        
        // 测试子类字段
        PsiElement dummyElement1 = createDummyStringLiteral("#this.childField");
        SpelFieldReference childRef = new SpelFieldReference(
                dummyElement1,
                new TextRange(6, 16),
                "childField",
                childClass
        );
        PsiElement resolvedChild = childRef.resolve();
        assertNotNull("Child field should be resolved", resolvedChild);
        assertEquals("childField", ((PsiField) resolvedChild).getName());
        
        // 测试父类字段
        PsiElement dummyElement2 = createDummyStringLiteral("#this.parentField");
        SpelFieldReference parentRef = new SpelFieldReference(
                dummyElement2,
                new TextRange(6, 17),
                "parentField",
                childClass
        );
        PsiElement resolvedParent = parentRef.resolve();
        assertNotNull("Parent field should be resolved from child class", resolvedParent);
        assertEquals("parentField", ((PsiField) resolvedParent).getName());
    }
    
    /**
     * Property 5 (续): 测试空值和边界情况
     * 
     * Validates: Requirements 4.3
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 5: 字段引用解析的正确性")
    public void testFieldReferenceResolution_EdgeCases() {
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java",
                "package test;\n" +
                "public class TestClass {\n" +
                "  private String field;\n" +
                "}"
        );
        
        PsiClass testClass = javaFile.getClasses()[0];
        PsiElement dummyElement = createDummyStringLiteral("#this.field");
        
        // 测试 null contextClass
        SpelFieldReference nullClassRef = new SpelFieldReference(
                dummyElement,
                new TextRange(6, 11),
                "field",
                null
        );
        assertNull("Should return null for null context class", nullClassRef.resolve());
        
        // 测试空字段名
        SpelFieldReference emptyFieldRef = new SpelFieldReference(
                dummyElement,
                new TextRange(6, 6),
                "",
                testClass
        );
        assertNull("Should return null for empty field name", emptyFieldRef.resolve());
    }
    
    /**
     * 创建一个虚拟的字符串字面量元素用于测试
     */
    private PsiElement createDummyStringLiteral(String content) {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("Dummy.java",
                "class Dummy { String s = \"" + content + "\"; }");
        PsiClass dummyClass = file.getClasses()[0];
        PsiField field = dummyClass.getFields()[0];
        return field.getInitializer();
    }
}
