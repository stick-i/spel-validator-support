package cn.sticki.spel.validator.support.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.Tag;

/**
 * SpelFieldReference 嵌套字段属性测试
 * 
 * Property 6: 嵌套字段引用解析的正确性
 * 对于任何嵌套字段引用 #this.field1.field2，
 * 插件应正确解析每一级的字段引用。
 * 
 * Validates: Requirements 4.4
 * 
 * @author Sticki
 */
public class SpelFieldReferenceNestedPropertyTest extends BasePlatformTestCase {
    
    /**
     * Property 6: 嵌套字段引用解析的正确性
     * 测试两级嵌套字段的解析
     * 
     * Validates: Requirements 4.4
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 6: 嵌套字段引用解析的正确性")
    public void testNestedFieldReferenceResolution_TwoLevels() {
        // 创建嵌套类结构
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "  private String street;\n" +
                "  private String zipCode;\n" +
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
        
        // 测试嵌套字段解析：address.city
        String[] nestedFields = {"address.city", "address.street", "address.zipCode"};
        String[] expectedFieldNames = {"city", "street", "zipCode"};
        
        for (int i = 0; i < nestedFields.length; i++) {
            String fieldPath = nestedFields[i];
            String expectedName = expectedFieldNames[i];
            
            PsiElement dummyElement = createDummyStringLiteral("#this." + fieldPath);
            SpelFieldReference reference = new SpelFieldReference(
                    dummyElement,
                    new TextRange(6, 6 + fieldPath.length()),
                    fieldPath,
                    userClass
            );
            
            PsiElement resolved = reference.resolve();
            
            assertNotNull("Nested field '" + fieldPath + "' should be resolved", resolved);
            assertTrue("Resolved element should be PsiField", resolved instanceof PsiField);
            assertEquals("Resolved field name should be '" + expectedName + "'", 
                    expectedName, ((PsiField) resolved).getName());
        }
    }
    
    /**
     * Property 6 (续): 测试三级嵌套字段的解析
     * 
     * Validates: Requirements 4.4
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 6: 嵌套字段引用解析的正确性")
    public void testNestedFieldReferenceResolution_ThreeLevels() {
        // 创建三级嵌套类结构
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
        
        PsiJavaFile userFile = (PsiJavaFile) myFixture.addFileToProject("test/User.java",
                "package test;\n" +
                "public class User {\n" +
                "  private String name;\n" +
                "  private Address address;\n" +
                "}"
        );
        
        PsiClass userClass = userFile.getClasses()[0];
        
        // 测试三级嵌套字段解析：address.country.name
        PsiElement dummyElement = createDummyStringLiteral("#this.address.country.name");
        SpelFieldReference reference = new SpelFieldReference(
                dummyElement,
                new TextRange(6, 26),
                "address.country.name",
                userClass
        );
        
        PsiElement resolved = reference.resolve();
        
        assertNotNull("Three-level nested field should be resolved", resolved);
        assertTrue("Resolved element should be PsiField", resolved instanceof PsiField);
        assertEquals("Resolved field name should be 'name'", "name", ((PsiField) resolved).getName());
        
        // 测试 address.country.code
        PsiElement dummyElement2 = createDummyStringLiteral("#this.address.country.code");
        SpelFieldReference reference2 = new SpelFieldReference(
                dummyElement2,
                new TextRange(6, 26),
                "address.country.code",
                userClass
        );
        
        PsiElement resolved2 = reference2.resolve();
        
        assertNotNull("Three-level nested field 'code' should be resolved", resolved2);
        assertEquals("Resolved field name should be 'code'", "code", ((PsiField) resolved2).getName());
    }
    
    /**
     * Property 6 (续): 测试嵌套字段中间路径不存在的情况
     * 
     * Validates: Requirements 4.4
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 6: 嵌套字段引用解析的正确性")
    public void testNestedFieldReferenceResolution_IntermediateFieldNotExist() {
        // 创建简单类结构
        PsiJavaFile userFile = (PsiJavaFile) myFixture.addFileToProject("test/User.java",
                "package test;\n" +
                "public class User {\n" +
                "  private String name;\n" +
                "}"
        );
        
        PsiClass userClass = userFile.getClasses()[0];
        
        // 测试中间字段不存在的情况：nonExistent.field
        PsiElement dummyElement = createDummyStringLiteral("#this.nonExistent.field");
        SpelFieldReference reference = new SpelFieldReference(
                dummyElement,
                new TextRange(6, 23),
                "nonExistent.field",
                userClass
        );
        
        PsiElement resolved = reference.resolve();
        
        assertNull("Should return null when intermediate field doesn't exist", resolved);
    }
    
    /**
     * Property 6 (续): 测试嵌套字段最后一级不存在的情况
     * 
     * Validates: Requirements 4.4
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 6: 嵌套字段引用解析的正确性")
    public void testNestedFieldReferenceResolution_LastFieldNotExist() {
        // 创建嵌套类结构
        myFixture.addFileToProject("test/Address.java",
                "package test;\n" +
                "public class Address {\n" +
                "  private String city;\n" +
                "}"
        );
        
        PsiJavaFile userFile = (PsiJavaFile) myFixture.addFileToProject("test/User.java",
                "package test;\n" +
                "public class User {\n" +
                "  private Address address;\n" +
                "}"
        );
        
        PsiClass userClass = userFile.getClasses()[0];
        
        // 测试最后一级字段不存在的情况：address.nonExistent
        PsiElement dummyElement = createDummyStringLiteral("#this.address.nonExistent");
        SpelFieldReference reference = new SpelFieldReference(
                dummyElement,
                new TextRange(6, 25),
                "address.nonExistent",
                userClass
        );
        
        PsiElement resolved = reference.resolve();
        
        assertNull("Should return null when last field doesn't exist", resolved);
    }
    
    /**
     * Property 6 (续): 测试嵌套字段类型为基本类型的情况
     * 
     * Validates: Requirements 4.4
     */
    @Tag("Feature: spel-validator-idea-plugin, Property 6: 嵌套字段引用解析的正确性")
    public void testNestedFieldReferenceResolution_PrimitiveTypeField() {
        // 创建类结构，包含基本类型字段
        PsiJavaFile userFile = (PsiJavaFile) myFixture.addFileToProject("test/User.java",
                "package test;\n" +
                "public class User {\n" +
                "  private int age;\n" +
                "}"
        );
        
        PsiClass userClass = userFile.getClasses()[0];
        
        // 测试基本类型字段的嵌套访问（应该失败，因为 int 没有字段）
        PsiElement dummyElement = createDummyStringLiteral("#this.age.something");
        SpelFieldReference reference = new SpelFieldReference(
                dummyElement,
                new TextRange(6, 19),
                "age.something",
                userClass
        );
        
        PsiElement resolved = reference.resolve();
        
        assertNull("Should return null when trying to access field on primitive type", resolved);
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
