package cn.sticki.spel.validator.support.inspection;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpEL 字段存在性检查标注器
 * 检查 SpEL 表达式中的字段引用是否有效
 * 
 * 功能：
 * - 检查 #this.fieldName 中的字段是否存在
 * - 检查嵌套字段引用（如 #this.user.name）的每一级是否存在
 * - 为不存在的字段显示错误标记
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4
 * 
 * @author Sticki
 */
public class SpelFieldAnnotator implements Annotator {
    
    private static final Logger LOG = Logger.getInstance(SpelFieldAnnotator.class);
    
    /**
     * 匹配 #this.fieldName 或 #this.field1.field2 的正则表达式
     * 捕获组1: 完整的字段路径（如 "field1.field2"）
     */
    private static final Pattern THIS_FIELD_PATTERN = Pattern.compile("#this\\.([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)");
    
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        try {
            // 只处理字符串字面量
            if (!(element instanceof PsiLiteralExpression)) {
                return;
            }
            
            PsiLiteralExpression literal = (PsiLiteralExpression) element;
            Object value = literal.getValue();
            
            // 检查是否为字符串类型
            if (!(value instanceof String)) {
                return;
            }
            
            String text = (String) value;
            
            // 检查是否包含 #this.
            if (!text.contains("#this.")) {
                return;
            }
            
            // 检查是否在约束注解中
            if (!isInConstraintAnnotation(element)) {
                return;
            }
            
            // 获取上下文类
            PsiClass contextClass = getContextClass(element);
            if (contextClass == null) {
                return;
            }
            
            // 检查所有字段引用
            checkFieldReferences(element, text, contextClass, holder);
        } catch (Exception e) {
            // 记录异常但不影响 IDEA 正常功能
            LOG.warn("Error in SpelFieldAnnotator: " + e.getMessage(), e);
        }
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
     * 检查字符串中的所有字段引用
     * 
     * @param element 字符串字面量元素
     * @param text 字符串内容
     * @param contextClass 上下文类
     * @param holder 标注持有者
     */
    private void checkFieldReferences(@NotNull PsiElement element,
                                       @NotNull String text,
                                       @NotNull PsiClass contextClass,
                                       @NotNull AnnotationHolder holder) {
        // 使用正则表达式匹配所有 #this.fieldPath 模式
        Matcher matcher = THIS_FIELD_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String fullFieldPath = matcher.group(1); // 完整的字段路径
            int pathStartInText = matcher.start(1);  // 字段路径在文本中的起始位置
            
            // 检查字段路径中的每个字段
            checkFieldPath(element, fullFieldPath, pathStartInText, contextClass, holder);
        }
    }
    
    /**
     * 检查字段路径中的每个字段是否存在
     * 
     * @param element 字符串字面量元素
     * @param fieldPath 字段路径（如 "user.address.city"）
     * @param pathStartInText 路径在文本中的起始位置
     * @param contextClass 上下文类
     * @param holder 标注持有者
     */
    private void checkFieldPath(@NotNull PsiElement element,
                                 @NotNull String fieldPath,
                                 int pathStartInText,
                                 @NotNull PsiClass contextClass,
                                 @NotNull AnnotationHolder holder) {
        String[] fieldNames = fieldPath.split("\\.");
        int currentOffset = pathStartInText;
        PsiClass currentClass = contextClass;
        StringBuilder currentPath = new StringBuilder();
        
        for (String fieldName : fieldNames) {
            if (currentClass == null) {
                // 如果当前类为 null，无法继续解析
                break;
            }
            
            // 构建当前字段的完整路径
            if (currentPath.length() > 0) {
                currentPath.append(".");
            }
            currentPath.append(fieldName);
            
            // 尝试解析字段
            PsiField field = SpelValidatorUtil.resolveNestedField(contextClass, currentPath.toString());
            
            if (field == null) {
                // 字段不存在，标记错误
                // +1 是因为字符串字面量包含开头的引号
                int startOffset = element.getTextOffset() + currentOffset + 1;
                int endOffset = startOffset + fieldName.length();
                
                // 创建错误标注
                holder.newAnnotation(HighlightSeverity.ERROR, 
                        "字段 '" + fieldName + "' 不存在")
                        .range(new com.intellij.openapi.util.TextRange(startOffset, endOffset))
                        .create();
                
                // 一旦发现错误，停止检查后续字段
                break;
            }
            
            // 获取字段类型，用于解析下一级字段
            PsiType fieldType = field.getType();
            if (fieldType instanceof PsiClassType) {
                currentClass = ((PsiClassType) fieldType).resolve();
            } else {
                // 字段类型不是类类型，无法继续解析嵌套字段
                currentClass = null;
            }
            
            // 更新偏移量（+1 是为了跳过 '.'）
            currentOffset += fieldName.length() + 1;
        }
    }
}
