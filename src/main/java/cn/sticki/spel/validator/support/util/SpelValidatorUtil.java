package cn.sticki.spel.validator.support.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpEL Validator 插件核心工具类
 * 提供注解识别、上下文解析、字段查找等通用功能
 *
 * 性能优化：
 * - 使用 CachedValue 缓存字段列表
 * - 使用 ConcurrentHashMap 缓存字段类型解析结果
 * 
 * @author Sticki
 */
public class SpelValidatorUtil {

    private static final Logger LOG = Logger.getInstance(SpelValidatorUtil.class);
    
    /**
     * SpEL Validator 约束注解的包名
     */
    private static final String CONSTRAINT_PACKAGE = "cn.sticki.spel.validator.constrain";
    
    /**
     * SpEL Constraint 元注解的完全限定名
     */
    private static final String SPEL_CONSTRAINT_ANNOTATION = "cn.sticki.spel.validator.constrain.SpelConstraint";
    
    /**
     * Language 注解的完全限定名
     */
    private static final String LANGUAGE_ANNOTATION = "org.intellij.lang.annotations.Language";

    /**
     * 字段类型解析缓存
     * Key: 类的完全限定名 + "." + 字段路径
     * Value: 解析到的 PsiClass
     */
    private static final ConcurrentHashMap<String, PsiClass> fieldTypeCache = new ConcurrentHashMap<>();

    /**
     * 缓存最大大小，防止内存泄漏
     */
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * 判断注解是否为 SpEL Validator 约束注解
     * 
     * @param annotation 待检查的注解
     * @return 如果是约束注解返回 true，否则返回 false
     */
    public static boolean isSpelConstraintAnnotation(PsiAnnotation annotation) {
        if (annotation == null) {
            return false;
        }

        try {
            // 检查注解的完全限定名
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                return false;
            }

            // 方法1: 检查注解包名是否为 cn.sticki.spel.validator.constrain
            if (qualifiedName.startsWith(CONSTRAINT_PACKAGE)) {
                return true;
            }

            // 方法2: 检查注解上是否标注了 @SpelConstraint 元注解
            PsiClass annotationClass = annotation.resolveAnnotationType();
            if (annotationClass != null) {
                PsiModifierList modifierList = annotationClass.getModifierList();
                if (modifierList != null) {
                    PsiAnnotation[] annotations = modifierList.getAnnotations();
                    for (PsiAnnotation metaAnnotation : annotations) {
                        if (SPEL_CONSTRAINT_ANNOTATION.equals(metaAnnotation.getQualifiedName())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Error checking constraint annotation: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 判断注解属性是否标注了 @Language("SpEL")
     * 
     * @param method 注解属性对应的方法
     * @return 如果标注了 @Language("SpEL") 返回 true，否则返回 false
     */
    public static boolean isSpelLanguageAttribute(PsiMethod method) {
        if (method == null) {
            return false;
        }
        
        try {
            // 获取方法的修饰符列表
            PsiModifierList modifierList = method.getModifierList();
            
            // 查找 @Language 注解
            PsiAnnotation languageAnnotation = modifierList.findAnnotation(LANGUAGE_ANNOTATION);
            if (languageAnnotation == null) {
                return false;
            }
            
            // 检查 @Language 注解的 value 属性是否为 "SpEL"
            PsiAnnotationMemberValue value = languageAnnotation.findAttributeValue("value");
            if (value == null) {
                return false;
            }
            
            // 获取注解值的文本内容（去除引号）
            String languageValue = value.getText().replace("\"", "");
            return "SpEL".equals(languageValue);
        } catch (Exception e) {
            LOG.debug("Error checking SpEL language attribute: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取注解所在的类（#this 的上下文类）
     * 
     * @param annotation 注解对象
     * @return 包含该注解的类，如果找不到返回 null
     */
    public static PsiClass getContextClass(PsiAnnotation annotation) {
        if (annotation == null) {
            return null;
        }
        
        try {
            // 从注解向上遍历 PSI 树，找到包含该注解的类
            PsiElement parent = annotation.getParent();
            
            while (parent != null) {
                // 如果找到类定义，返回该类
                if (parent instanceof PsiClass) {
                    return (PsiClass) parent;
                }
                
                // 继续向上遍历
                parent = parent.getParent();
            }
        } catch (Exception e) {
            LOG.debug("Error getting context class: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 获取类的所有字段（包括父类字段）
     * 使用 CachedValue 缓存结果，提高性能
     * 
     * @param psiClass 类对象
     * @return 所有字段的列表
     */
    public static List<PsiField> getAllFields(PsiClass psiClass) {
        if (psiClass == null) {
            return java.util.Collections.emptyList();
        }

        try {
            // 使用 CachedValuesManager 缓存字段列表
            return CachedValuesManager.getCachedValue(psiClass, () -> {
                List<PsiField> allFields = collectAllFields(psiClass);
                // 依赖 PSI 修改追踪器，当类结构变化时自动失效
                return CachedValueProvider.Result.create(allFields, PsiModificationTracker.MODIFICATION_COUNT);
            });
        } catch (Exception e) {
            LOG.warn("Error getting all fields with cache, falling back to direct collection: " + e.getMessage());
            // 缓存失败时回退到直接收集
            return collectAllFields(psiClass);
        }
    }

    /**
     * 直接收集类的所有字段（包括父类字段）
     *
     * @param psiClass 类对象
     * @return 所有字段的列表
     */
    private static List<PsiField> collectAllFields(PsiClass psiClass) {
        if (psiClass == null) {
            return java.util.Collections.emptyList();
        }
        
        try {
            List<PsiField> allFields = new java.util.ArrayList<>();
            
            // 递归收集当前类及所有父类的字段
            PsiClass currentClass = psiClass;
            while (currentClass != null) {
                // 获取当前类的所有字段
                PsiField[] fields = currentClass.getFields();
                if (fields != null && fields.length > 0) {
                    allFields.addAll(java.util.Arrays.asList(fields));
                }
                
                // 移动到父类
                currentClass = currentClass.getSuperClass();
                
                // 避免收集 Object 类的字段
                if (currentClass != null && "java.lang.Object".equals(currentClass.getQualifiedName())) {
                    break;
                }
            }
            
            return allFields;
        } catch (Exception e) {
            LOG.debug("Error collecting fields: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * 解析嵌套字段路径（如 "user.name"）
     * 使用缓存优化字段类型解析
     * 
     * @param startClass 起始类
     * @param fieldPath 字段路径，用 . 分隔
     * @return 解析到的字段，如果解析失败返回 null
     */
    public static PsiField resolveNestedField(PsiClass startClass, String fieldPath) {
        if (startClass == null || fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }
        
        try {
            // 按 . 分割字段路径
            String[] fieldNames = fieldPath.split("\\.");
            
            PsiClass currentClass = startClass;
            PsiField currentField = null;
            
            // 逐级解析字段
            for (int i = 0; i < fieldNames.length; i++) {
                String fieldName = fieldNames[i];

                if (currentClass == null) {
                    return null;
                }
                
                // 在当前类及其父类中查找字段
                currentField = findFieldInClassHierarchy(currentClass, fieldName);
                if (currentField == null) {
                    return null;
                }
                
                // 如果不是最后一个字段，获取字段类型作为下一级的类
                if (i < fieldNames.length - 1) {
                    currentClass = resolveFieldType(currentClass, fieldName, currentField);
                }
            }

            return currentField;
        } catch (Exception e) {
            LOG.debug("Error resolving nested field '" + fieldPath + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析字段类型，使用缓存优化
     *
     * @param containingClass 包含字段的类
     * @param fieldName       字段名
     * @param field           字段对象
     * @return 字段类型对应的 PsiClass
     */
    private static PsiClass resolveFieldType(PsiClass containingClass, String fieldName, PsiField field) {
        if (field == null) {
            return null;
        }

        try {
            // 构建缓存键
            String cacheKey = containingClass.getQualifiedName() + "." + fieldName;

            // 检查缓存大小，防止内存泄漏
            if (fieldTypeCache.size() > MAX_CACHE_SIZE) {
                fieldTypeCache.clear();
                LOG.debug("Field type cache cleared due to size limit");
            }

            // 尝试从缓存获取
            PsiClass cachedClass = fieldTypeCache.get(cacheKey);
            if (cachedClass != null && cachedClass.isValid()) {
                return cachedClass;
            }

            // 解析字段类型
            PsiType fieldType = field.getType();
            if (fieldType instanceof PsiClassType) {
                PsiClass resolvedClass = ((PsiClassType) fieldType).resolve();
                if (resolvedClass != null) {
                    fieldTypeCache.put(cacheKey, resolvedClass);
                }
                return resolvedClass;
            }

            return null;
        } catch (Exception e) {
            LOG.debug("Error resolving field type: " + e.getMessage());
            return null;
        }
    }

    /**
     * 清除字段类型缓存
     * 用于测试或手动清理
     */
    public static void clearFieldTypeCache() {
        fieldTypeCache.clear();
    }
    
    /**
     * 在类及其父类中查找字段
     * 
     * @param psiClass 类对象
     * @param fieldName 字段名
     * @return 找到的字段，如果找不到返回 null
     */
    private static PsiField findFieldInClassHierarchy(PsiClass psiClass, String fieldName) {
        if (psiClass == null || fieldName == null) {
            return null;
        }

        try {
            PsiClass currentClass = psiClass;
            while (currentClass != null) {
                // 在当前类中查找字段
                PsiField field = currentClass.findFieldByName(fieldName, false);
                if (field != null) {
                    return field;
                }

                // 移动到父类
                currentClass = currentClass.getSuperClass();

                // 避免搜索 Object 类
                if (currentClass != null && "java.lang.Object".equals(currentClass.getQualifiedName())) {
                    break;
                }
            }
        } catch (Exception e) {
            LOG.debug("Error finding field '" + fieldName + "' in class hierarchy: " + e.getMessage());
        }
        
        return null;
    }
}
