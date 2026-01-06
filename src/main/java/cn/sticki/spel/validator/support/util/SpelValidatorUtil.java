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
 * <p>
 * 本类提供 SpEL Validator IDEA 插件所需的核心工具方法，包括：
 * <ul>
 *   <li>约束注解识别：判断注解是否为 SpEL Validator 的约束注解</li>
 *   <li>@Language 注解检查：判断注解属性是否标注了 @Language("SpEL")</li>
 *   <li>上下文类获取：从注解位置获取 #this 指向的类</li>
 *   <li>字段收集：获取类及其父类的所有字段</li>
 *   <li>嵌套字段解析：解析如 "user.address.city" 的字段路径</li>
 * </ul>
 * <p>
 * 性能优化策略：
 * <ul>
 *   <li>使用 {@link CachedValuesManager} 缓存字段列表，当 PSI 树变化时自动失效</li>
 *   <li>使用 {@link ConcurrentHashMap} 缓存字段类型解析结果，支持并发访问</li>
 *   <li>缓存大小限制为 {@value #MAX_CACHE_SIZE}，防止内存泄漏</li>
 * </ul>
 * <p>
 * 线程安全：本类的所有公共方法都是线程安全的
 * <p>
 * Requirements: 1.1, 1.2, 1.3, 2.1, 3.1, 3.2, 3.3, 3.5, 4.4
 *
 * @author Sticki
 * @see cn.sticki.spel.validator.support.injection.SpelLanguageInjector
 * @see cn.sticki.spel.validator.support.completion.SpelFieldCompletionContributor
 * @see cn.sticki.spel.validator.support.reference.SpelFieldReferenceContributor
 */
public class SpelValidatorUtil {

    private static final Logger LOG = Logger.getInstance(SpelValidatorUtil.class);
    
    /**
     * SpEL Validator 约束注解的包名前缀
     * <p>
     * 所有位于此包下的注解都被视为内置约束注解，包括：
     * SpelAssert, SpelNotNull, SpelNotBlank, SpelNotEmpty, SpelNull,
     * SpelSize, SpelMin, SpelMax, SpelDigits, SpelFuture, SpelPast,
     * SpelFutureOrPresent, SpelPastOrPresent 等
     */
    private static final String CONSTRAINT_PACKAGE = "cn.sticki.spel.validator.constrain";
    
    /**
     * SpelConstraint 元注解的完全限定名
     * <p>
     * 用户自定义的约束注解需要标注此元注解才能被插件识别
     * 这是 SpEL Validator 框架的扩展机制
     */
    private static final String SPEL_CONSTRAINT_ANNOTATION = "cn.sticki.spel.validator.constrain.SpelConstraint";
    
    /**
     * IntelliJ @Language 注解的完全限定名
     * <p>
     * 用于标记注解属性应该注入特定语言支持
     * 当 value 为 "SpEL" 时，表示该属性应该注入 SpEL 语言支持
     */
    private static final String LANGUAGE_ANNOTATION = "org.intellij.lang.annotations.Language";

    /**
     * 字段类型解析缓存
     * <p>
     * 缓存结构：
     * <ul>
     *   <li>Key: 类的完全限定名 + "." + 字段路径（如 "com.example.User.address"）</li>
     *   <li>Value: 解析到的 PsiClass 对象</li>
     * </ul>
     * <p>
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private static final ConcurrentHashMap<String, PsiClass> fieldTypeCache = new ConcurrentHashMap<>();

    /**
     * 缓存最大大小
     * <p>
     * 当缓存大小超过此值时，会清空整个缓存以防止内存泄漏
     * 这是一个简单但有效的缓存淘汰策略
     */
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * 判断注解是否为 SpEL Validator 约束注解
     * <p>
     * 识别逻辑（满足任一条件即可）：
     * <ol>
     *   <li>注解的包名以 {@value #CONSTRAINT_PACKAGE} 开头（内置约束注解）</li>
     *   <li>注解类上标注了 @SpelConstraint 元注解（自定义约束注解）</li>
     * </ol>
     * <p>
     * 使用示例：
     * <pre>{@code
     * // 内置注解
     * @SpelNotNull(condition = "#this.name != null")
     * private String field;
     * 
     * // 自定义注解（需要标注 @SpelConstraint）
     * @SpelConstraint
     * public @interface MyConstraint { ... }
     * }</pre>
     *
     * @param annotation 待检查的注解，可以为 null
     * @return 如果是约束注解返回 true，否则返回 false（包括 null 的情况）
     * @see #CONSTRAINT_PACKAGE
     * @see #SPEL_CONSTRAINT_ANNOTATION
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
     * <p>
     * 此方法用于确定注解的某个属性是否应该注入 SpEL 语言支持。
     * 只有当属性方法上标注了 @Language("SpEL") 时，才会为该属性的字符串值注入 SpEL 语言。
     * <p>
     * 检查逻辑：
     * <ol>
     *   <li>获取方法的修饰符列表</li>
     *   <li>查找 @Language 注解</li>
     *   <li>检查 @Language 的 value 属性是否为 "SpEL"</li>
     * </ol>
     * <p>
     * 使用示例：
     * <pre>{@code
     * public @interface SpelNotNull {
     *   @Language("SpEL")  // 此属性会注入 SpEL 语言
     *   String condition() default "";
     *   
     *   String message() default "";  // 此属性不会注入
     * }
     * }</pre>
     *
     * @param method 注解属性对应的方法（注解的属性在 Java 中表现为方法），可以为 null
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
     * <p>
     * 在 SpEL 表达式中，#this 变量代表当前被校验的对象。
     * 此方法通过向上遍历 PSI 树，找到包含该注解的类定义。
     * <p>
     * PSI 树遍历路径示例：
     * <pre>
     * PsiAnnotation → PsiModifierList → PsiField → PsiClass (目标)
     * </pre>
     * <p>
     * 使用场景：
     * <ul>
     *   <li>字段补全：确定 #this. 后应该补全哪个类的字段</li>
     *   <li>引用解析：确定字段引用应该在哪个类中查找</li>
     *   <li>错误检查：确定字段是否存在于上下文类中</li>
     * </ul>
     *
     * @param annotation 注解对象，可以为 null
     * @return 包含该注解的类，如果找不到或参数为 null 则返回 null
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
     * <p>
     * 此方法递归收集当前类及其所有父类的字段，但不包括 java.lang.Object 的字段。
     * 结果使用 {@link CachedValuesManager} 缓存，当 PSI 树发生变化时自动失效。
     * <p>
     * 字段收集顺序：
     * <ol>
     *   <li>当前类的字段</li>
     *   <li>父类的字段</li>
     *   <li>祖父类的字段</li>
     *   <li>...（直到 Object 类之前）</li>
     * </ol>
     * <p>
     * 性能优化：
     * <ul>
     *   <li>使用 CachedValue 缓存结果</li>
     *   <li>依赖 PsiModificationTracker 自动失效</li>
     *   <li>缓存失败时回退到直接收集</li>
     * </ul>
     *
     * @param psiClass 类对象，可以为 null
     * @return 所有字段的列表（包括私有字段），如果参数为 null 则返回空列表
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
     * 解析嵌套字段路径
     * <p>
     * 此方法支持解析如 "user.address.city" 的嵌套字段路径。
     * 按 "." 分割路径后，逐级解析每个字段的类型，最终返回路径末端的字段。
     * <p>
     * 解析过程示例（路径 "user.address.city"）：
     * <ol>
     *   <li>在 startClass 中查找 "user" 字段</li>
     *   <li>获取 "user" 字段的类型（如 User 类）</li>
     *   <li>在 User 类中查找 "address" 字段</li>
     *   <li>获取 "address" 字段的类型（如 Address 类）</li>
     *   <li>在 Address 类中查找 "city" 字段</li>
     *   <li>返回 "city" 字段</li>
     * </ol>
     * <p>
     * 性能优化：使用 {@link #fieldTypeCache} 缓存字段类型解析结果
     *
     * @param startClass 起始类（#this 指向的类）
     * @param fieldPath  字段路径，用 "." 分隔（如 "user.address.city"）
     * @return 解析到的字段，如果路径中任何一级字段不存在则返回 null
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
