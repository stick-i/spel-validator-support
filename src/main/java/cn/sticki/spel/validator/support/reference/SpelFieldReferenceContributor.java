package cn.sticki.spel.validator.support.reference;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpEL 字段引用贡献者
 * <p>
 * 本类实现 IntelliJ Platform 的 {@link PsiReferenceContributor}，
 * 为 SpEL 表达式中的字段引用提供解析支持。
 * <p>
 * 功能说明：
 * <ul>
 *   <li>识别 SpEL 表达式中的 #this.fieldName 模式</li>
 *   <li>为每个字段引用创建 {@link SpelFieldReference} 实例</li>
 *   <li>支持嵌套字段路径（如 #this.user.address.city）</li>
 *   <li>为路径中的每一级字段创建独立的引用</li>
 * </ul>
 * <p>
 * 引用创建示例：
 * <pre>
 * 表达式: #this.user.address.city
 * 创建的引用:
 *   - SpelFieldReference("user", contextClass)
 *   - SpelFieldReference("user.address", contextClass)
 *   - SpelFieldReference("user.address.city", contextClass)
 * </pre>
 * <p>
 * 性能优化：
 * <ul>
 *   <li>使用 {@link ReadAction#compute} 确保线程安全</li>
 *   <li>使用正则表达式高效匹配字段引用模式</li>
 * </ul>
 * <p>
 * Requirements: 4.1, 4.2, 4.3, 4.4, 5.1
 *
 * @author Sticki
 * @see PsiReferenceContributor
 * @see SpelFieldReference
 */
public class SpelFieldReferenceContributor extends PsiReferenceContributor {
    
    private static final Logger LOG = Logger.getInstance(SpelFieldReferenceContributor.class);
    
    /**
     * 匹配 #this.fieldName 或 #this.field1.field2 的正则表达式
     * <p>
     * 模式说明：
     * <ul>
     *   <li>#this\. - 匹配 "#this." 前缀</li>
     *   <li>[a-zA-Z_][a-zA-Z0-9_]* - 匹配字段名（以字母或下划线开头）</li>
     *   <li>(?:\.[a-zA-Z_][a-zA-Z0-9_]*)* - 匹配可选的嵌套字段</li>
     * </ul>
     * <p>
     * 捕获组1: 完整的字段路径（如 "field1.field2.field3"）
     */
    private static final Pattern THIS_FIELD_PATTERN = Pattern.compile("#this\\.([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)");
    
    /**
     * 注册引用提供者
     * <p>
     * 此方法在插件加载时被调用，用于注册引用提供者。
     * 注册后，当 IDEA 处理字符串字面量时，会调用我们的引用提供者。
     *
     * @param registrar 引用注册器
     */
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        LOG.warn("SpelFieldReferenceContributor.registerReferenceProviders() called - registering provider");
        
        // 注册引用提供者：匹配字符串字面量
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(PsiLiteralExpression.class),
                new PsiReferenceProvider() {
                    @NotNull
                    @Override
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                                 @NotNull ProcessingContext context) {
                        LOG.warn("getReferencesByElement called for: " + element.getText());
                        return getFieldReferences(element);
                    }
                }
        );
    }
    
    /**
     * 获取元素中的字段引用
     * 使用 ReadAction.compute 确保线程安全
     * 
     * @param element PSI 元素
     * @return 引用数组
     */
    @NotNull
    private PsiReference[] getFieldReferences(@NotNull PsiElement element) {
        try {
            // 使用 ReadAction.compute 确保在读取操作中执行
            return ReadAction.compute(() -> {
                try {
                    return getFieldReferencesInternal(element);
                } catch (Exception e) {
                    LOG.debug("Error getting field references: " + e.getMessage());
                    return PsiReference.EMPTY_ARRAY;
                }
            });
        } catch (Exception e) {
            LOG.warn("Error in ReadAction while getting field references: " + e.getMessage());
            return PsiReference.EMPTY_ARRAY;
        }
    }
    
    /**
     * 内部方法：获取元素中的字段引用
     * 
     * @param element PSI 元素
     * @return 引用数组
     */
    @NotNull
    private PsiReference[] getFieldReferencesInternal(@NotNull PsiElement element) {
        // 检查是否为字符串字面量
        if (!(element instanceof PsiLiteralExpression)) {
            return PsiReference.EMPTY_ARRAY;
        }
        
        PsiLiteralExpression literal = (PsiLiteralExpression) element;
        Object value = literal.getValue();
        
        // 检查是否为字符串类型
        if (!(value instanceof String)) {
            return PsiReference.EMPTY_ARRAY;
        }
        
        String text = (String) value;
        
        // 检查是否包含 #this.
        if (!text.contains("#this.")) {
            return PsiReference.EMPTY_ARRAY;
        }
        
        LOG.warn("Found #this. in text: " + text);
        
        // 检查是否在约束注解中
        if (!isInConstraintAnnotation(element)) {
            LOG.warn("Not in constraint annotation, skipping");
            return PsiReference.EMPTY_ARRAY;
        }
        
        LOG.warn("In constraint annotation, proceeding");
        
        // 获取上下文类
        PsiClass contextClass = getContextClass(element);
        if (contextClass == null) {
            LOG.warn("Context class is null, skipping");
            return PsiReference.EMPTY_ARRAY;
        }
        
        LOG.warn("Context class: " + contextClass.getQualifiedName());
        
        // 解析所有字段引用
        PsiReference[] refs = parseFieldReferences(element, text, contextClass);
        LOG.warn("Created " + refs.length + " references");
        return refs;
    }
    
    /**
     * 检查元素是否在约束注解中
     * 
     * @param element 元素
     * @return 如果在约束注解中返回 true
     */
    private boolean isInConstraintAnnotation(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof PsiAnnotation) {
                return SpelValidatorUtil.isSpelConstraintAnnotation((PsiAnnotation) parent);
            }
            parent = parent.getParent();
        }
        return false;
    }
    
    /**
     * 获取上下文类
     * 
     * @param element 元素
     * @return 上下文类
     */
    private PsiClass getContextClass(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof PsiAnnotation) {
                return SpelValidatorUtil.getContextClass((PsiAnnotation) parent);
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    /**
     * 解析字符串中的所有字段引用
     * 
     * @param element 字符串字面量元素
     * @param text 字符串内容
     * @param contextClass 上下文类
     * @return 引用数组
     */
    @NotNull
    private PsiReference[] parseFieldReferences(@NotNull PsiElement element, 
                                                 @NotNull String text, 
                                                 @NotNull PsiClass contextClass) {
        List<PsiReference> references = new ArrayList<>();
        
        // 使用正则表达式匹配所有 #this.fieldPath 模式
        Matcher matcher = THIS_FIELD_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String fullFieldPath = matcher.group(1); // 完整的字段路径
            int pathStartInText = matcher.start(1);  // 字段路径在文本中的起始位置
            
            // 为字段路径中的每个字段创建引用
            createReferencesForFieldPath(element, fullFieldPath, pathStartInText, contextClass, references);
        }
        
        return references.toArray(PsiReference.EMPTY_ARRAY);
    }
    
    /**
     * 为字段路径中的每个字段创建引用
     * 
     * @param element 字符串字面量元素
     * @param fieldPath 字段路径（如 "user.address.city"）
     * @param pathStartInText 路径在文本中的起始位置
     * @param contextClass 上下文类
     * @param references 引用列表
     */
    private void createReferencesForFieldPath(@NotNull PsiElement element,
                                               @NotNull String fieldPath,
                                               int pathStartInText,
                                               @NotNull PsiClass contextClass,
                                               @NotNull List<PsiReference> references) {
        String[] fieldNames = fieldPath.split("\\.");
        int currentOffset = pathStartInText;
        StringBuilder currentPath = new StringBuilder();
        
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            
            // 构建当前字段的完整路径
            if (currentPath.length() > 0) {
                currentPath.append(".");
            }
            currentPath.append(fieldName);
            
            // 计算字段在字符串字面量中的范围
            // +1 是因为字符串字面量包含开头的引号
            int startOffset = currentOffset + 1;
            int endOffset = startOffset + fieldName.length();
            
            TextRange textRange = new TextRange(startOffset, endOffset);
            
            // 创建字段引用
            SpelFieldReference reference = new SpelFieldReference(
                    element,
                    textRange,
                    currentPath.toString(),
                    contextClass
            );
            
            references.add(reference);
            
            // 更新偏移量（+1 是为了跳过 '.'）
            currentOffset += fieldName.length();
            if (i < fieldNames.length - 1) {
                currentOffset += 1; // 跳过 '.'
            }
        }
    }
}
