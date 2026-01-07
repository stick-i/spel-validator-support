package cn.sticki.spel.validator.support.navigation;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpEL 字段跳转处理器
 * <p>
 * 处理 Ctrl+Click 跳转到字段定义的功能。
 * 当用户在 SpEL 表达式中的 #this.fieldName 上按 Ctrl+Click 时，
 * 跳转到对应的字段定义。
 * <p>
 * 实现说明：
 * <ul>
 *   <li>支持从注入的 SpEL 语言中导航到字段定义</li>
 *   <li>通过 InjectedLanguageManager 获取注入宿主（字符串字面量）</li>
 *   <li>在 plugin.xml 中使用 order="first" 确保优先于 Spring 插件执行</li>
 * </ul>
 */
public class SpelFieldGotoDeclarationHandler implements GotoDeclarationHandler {
    
    private static final Logger LOG = Logger.getInstance(SpelFieldGotoDeclarationHandler.class);
    
    /**
     * 匹配 #this.fieldName 的正则表达式
     */
    private static final Pattern THIS_FIELD_PATTERN = Pattern.compile("#this\\.([a-zA-Z_][a-zA-Z0-9_]*)");
    
    @Override
    @Nullable
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, 
                                                  int offset, 
                                                  Editor editor) {
        if (sourceElement == null) {
            return null;
        }
        
        // 尝试从注入的语言宿主获取原始元素
        PsiLanguageInjectionHost injectionHost = InjectedLanguageManager.getInstance(sourceElement.getProject())
                .getInjectionHost(sourceElement);
        
        // 查找包含的字符串字面量 - 优先使用注入宿主
        PsiLiteralExpression literalExpression;
        if (injectionHost instanceof PsiLiteralExpression) {
            literalExpression = (PsiLiteralExpression) injectionHost;
        } else {
            literalExpression = findContainingLiteral(sourceElement);
        }
        
        if (literalExpression == null) {
            return null;
        }
        
        Object value = literalExpression.getValue();
        if (!(value instanceof String)) {
            return null;
        }
        
        String text = (String) value;
        if (!text.contains("#this.")) {
            return null;
        }
        
        // 查找包含的注解
        PsiAnnotation annotation = findContainingAnnotation(literalExpression);
        if (annotation == null || !SpelValidatorUtil.isSpelConstraintAnnotation(annotation)) {
            return null;
        }
        
        // 获取上下文类
        PsiClass contextClass = SpelValidatorUtil.getContextClass(annotation);
        if (contextClass == null) {
            return null;
        }
        
        // 如果是从注入的语言中调用，直接从源元素文本中查找字段名
        if (injectionHost != null) {
            String sourceText = sourceElement.getText();
            String fieldNameFromSource = findFieldNameInText(sourceText);
            if (fieldNameFromSource != null) {
                PsiField field = SpelValidatorUtil.resolveNestedField(contextClass, fieldNameFromSource);
                if (field != null) {
                    return new PsiElement[]{field};
                }
            }
        }
        
        // 计算点击位置在字符串中的偏移
        int literalStart = literalExpression.getTextRange().getStartOffset();
        int offsetInLiteral = offset - literalStart - 1; // -1 for the opening quote
        
        if (offsetInLiteral < 0 || offsetInLiteral >= text.length()) {
            return null;
        }
        
        // 查找点击位置对应的字段名
        String fieldName = findFieldNameAtOffset(text, offsetInLiteral);
        if (fieldName == null) {
            return null;
        }
        
        // 解析字段
        PsiField field = SpelValidatorUtil.resolveNestedField(contextClass, fieldName);
        if (field != null) {
            return new PsiElement[]{field};
        }
        
        return null;
    }
    
    /**
     * 从文本中直接查找字段名（用于处理注入语言的情况）
     */
    @Nullable
    private String findFieldNameInText(@NotNull String text) {
        // 如果文本本身就是字段名（不包含 #this.）
        if (!text.contains("#this.") && !text.contains(".") && text.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            return text;
        }
        
        // 尝试从 #this.fieldName 模式中提取
        Matcher matcher = THIS_FIELD_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    @Nullable
    private PsiLiteralExpression findContainingLiteral(@NotNull PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof PsiLiteralExpression) {
                return (PsiLiteralExpression) current;
            }
            current = current.getParent();
        }
        return null;
    }
    
    @Nullable
    private PsiAnnotation findContainingAnnotation(@NotNull PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof PsiAnnotation) {
                return (PsiAnnotation) current;
            }
            current = current.getParent();
        }
        return null;
    }
    
    /**
     * 查找给定偏移位置对应的字段名
     */
    @Nullable
    private String findFieldNameAtOffset(@NotNull String text, int offset) {
        Matcher matcher = THIS_FIELD_PATTERN.matcher(text);
        
        while (matcher.find()) {
            int fieldStart = matcher.start(1);
            int fieldEnd = matcher.end(1);
            
            if (offset >= fieldStart && offset <= fieldEnd) {
                return matcher.group(1);
            }
        }
        
        return null;
    }
    
    @Override
    public @Nullable String getActionText(@NotNull DataContext context) {
        return null;
    }
}
