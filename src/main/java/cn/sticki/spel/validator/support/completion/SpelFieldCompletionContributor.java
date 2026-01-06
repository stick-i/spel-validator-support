package cn.sticki.spel.validator.support.completion;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * SpEL 字段补全贡献者
 * 为 SpEL 表达式中的 #this. 提供字段补全
 * 
 * @author Sticki
 */
public class SpelFieldCompletionContributor extends CompletionContributor {
    
    public SpelFieldCompletionContributor() {
        // 注册补全模式：匹配字符串字面量中的位置
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement()
                        .inside(PsiLiteralExpression.class),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        addFieldCompletions(parameters, result);
                    }
                });
    }
    
    /**
     * 添加字段补全项
     * 
     * @param parameters 补全参数
     * @param result 补全结果集
     */
    private void addFieldCompletions(@NotNull CompletionParameters parameters,
                                     @NotNull CompletionResultSet result) {
        try {
            PsiElement position = parameters.getPosition();
            PsiElement originalPosition = parameters.getOriginalPosition();
            
            // 检查是否在 SpEL 表达式中（约束注解的字符串字面量）
            if (!isInSpelExpression(position)) {
                return;
            }
            
            // 获取补全位置的文本（使用 originalPosition 获取更准确的文本）
            String text = getCompletionText(originalPosition != null ? originalPosition : position);
            if (text == null) {
                return;
            }
            
            // 检查是否为 #this. 或嵌套字段访问
            if (!text.contains("#this.")) {
                return;
            }
            
            // 获取上下文类
            PsiClass contextClass = getContextClass(position);
            if (contextClass == null) {
                return;
            }
            
            // 解析字段路径
            String fieldPath = extractFieldPath(text);
            PsiClass targetClass = resolveTargetClass(contextClass, fieldPath);
            
            if (targetClass == null) {
                return;
            }
            
            // 收集所有字段并创建补全项
            List<PsiField> fields = SpelValidatorUtil.getAllFields(targetClass);
            
            // 使用空前缀匹配器，确保所有结果都能显示
            CompletionResultSet resultWithPrefix = result.withPrefixMatcher("");
            
            for (PsiField field : fields) {
                resultWithPrefix.addElement(createFieldLookupElement(field));
            }
        } catch (Exception e) {
            // 忽略异常，不影响 IDEA 正常功能
        }
    }
    
    /**
     * 检查位置是否在 SpEL 表达式中
     * 
     * @param position 当前位置
     * @return 如果在 SpEL 表达式中返回 true
     */
    private boolean isInSpelExpression(@NotNull PsiElement position) {
        // 向上遍历 PSI 树，查找字符串字面量
        PsiElement parent = position.getParent();
        while (parent != null) {
            if (parent instanceof PsiLiteralExpression) {
                // 检查是否在约束注解中
                return isInConstraintAnnotation(parent);
            }
            parent = parent.getParent();
        }
        return false;
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
     * 获取补全位置的文本
     * 
     * @param position 当前位置
     * @return 文本内容
     */
    private String getCompletionText(@NotNull PsiElement position) {
        PsiElement parent = position.getParent();
        while (parent != null) {
            if (parent instanceof PsiLiteralExpression) {
                Object value = ((PsiLiteralExpression) parent).getValue();
                if (value instanceof String) {
                    String text = (String) value;
                    // 移除 IntelliJ 的补全占位符
                    return text.replace("IntellijIdeaRulezzz ", "");
                }
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    /**
     * 获取上下文类
     * 
     * @param position 当前位置
     * @return 上下文类
     */
    private PsiClass getContextClass(@NotNull PsiElement position) {
        PsiElement parent = position.getParent();
        while (parent != null) {
            if (parent instanceof PsiAnnotation) {
                return SpelValidatorUtil.getContextClass((PsiAnnotation) parent);
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    /**
     * 提取字段路径（从 #this. 之后的部分）
     * 
     * @param text 完整文本
     * @return 字段路径，如果没有嵌套则返回空字符串
     */
    private String extractFieldPath(@NotNull String text) {
        int thisIndex = text.indexOf("#this.");
        if (thisIndex == -1) {
            return "";
        }
        
        // 获取 #this. 之后的部分
        String afterThis = text.substring(thisIndex + 6); // "#this." 长度为 6
        
        // 查找最后一个 . 之前的部分作为字段路径
        int lastDotIndex = afterThis.lastIndexOf('.');
        if (lastDotIndex == -1) {
            // 没有嵌套，返回空字符串
            return "";
        }
        
        // 返回最后一个 . 之前的部分
        return afterThis.substring(0, lastDotIndex);
    }
    
    /**
     * 解析目标类（处理嵌套字段）
     * 
     * @param contextClass 上下文类
     * @param fieldPath 字段路径
     * @return 目标类
     */
    private PsiClass resolveTargetClass(@NotNull PsiClass contextClass, @NotNull String fieldPath) {
        if (fieldPath.isEmpty()) {
            // 没有嵌套，直接返回上下文类
            return contextClass;
        }
        
        // 解析嵌套字段
        PsiField field = SpelValidatorUtil.resolveNestedField(contextClass, fieldPath);
        if (field == null) {
            return null;
        }
        
        // 获取字段类型
        PsiType fieldType = field.getType();
        if (fieldType instanceof PsiClassType) {
            return ((PsiClassType) fieldType).resolve();
        }
        
        return null;
    }
    
    /**
     * 创建字段补全项
     * 
     * @param field 字段
     * @return 补全项
     */
    private LookupElementBuilder createFieldLookupElement(@NotNull PsiField field) {
        String fieldName = field.getName();
        PsiType fieldType = field.getType();
        String typeText = fieldType.getPresentableText();
        
        return LookupElementBuilder.create(fieldName)
                .withTypeText(typeText)
                .withIcon(field.getIcon(0));
    }
}
