package cn.sticki.spel.validator.support.injection;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * SpEL 语言注入器
 * 为 SpEL Validator 注解的属性注入 SpEL 语言支持
 * 
 * 异常处理：
 * - 所有扩展点方法都使用 try-catch 捕获异常
 * - 使用 Logger 记录错误信息
 * - 确保异常不影响 IDEA 正常功能
 * 
 * @author Sticki
 */
public class SpelLanguageInjector implements LanguageInjector {
    
    private static final Logger LOG = Logger.getInstance(SpelLanguageInjector.class);
    
    /**
     * SpEL 语言 ID
     */
    private static final String SPEL_LANGUAGE_ID = "SpEL";
    
    @Override
    public void getLanguagesToInject(@NotNull PsiLanguageInjectionHost host, 
                                     @NotNull InjectedLanguagePlaces injectedLanguagePlaces) {
        try {
            injectSpelLanguage(host, injectedLanguagePlaces);
        } catch (Exception e) {
            // 记录异常但不影响 IDEA 正常功能
            LOG.warn("Error in SpelLanguageInjector.getLanguagesToInject: " + e.getMessage(), e);
        }
    }
    
    /**
     * 内部方法：执行 SpEL 语言注入
     * 
     * @param host 语言注入宿主
     * @param injectedLanguagePlaces 注入位置
     */
    private void injectSpelLanguage(@NotNull PsiLanguageInjectionHost host,
                                     @NotNull InjectedLanguagePlaces injectedLanguagePlaces) {
        // 1. 检查 host 是否为字符串字面量
        if (!(host instanceof PsiLiteralExpression)) {
            return;
        }
        
        PsiLiteralExpression literalExpression = (PsiLiteralExpression) host;
        
        // 确保是字符串类型
        Object value = literalExpression.getValue();
        if (!(value instanceof String)) {
            return;
        }
        
        // 2. 向上遍历 PSI 树，找到 PsiNameValuePair（注解属性）
        PsiElement parent = literalExpression.getParent();
        PsiNameValuePair nameValuePair = null;
        
        while (parent != null) {
            if (parent instanceof PsiNameValuePair) {
                nameValuePair = (PsiNameValuePair) parent;
                break;
            }
            parent = parent.getParent();
        }
        
        if (nameValuePair == null) {
            return;
        }
        
        // 3. 获取属性对应的方法定义（PsiMethod）
        PsiReference reference = nameValuePair.getReference();
        if (reference == null) {
            return;
        }
        
        PsiElement resolved = reference.resolve();
        if (!(resolved instanceof PsiMethod)) {
            return;
        }
        
        PsiMethod method = (PsiMethod) resolved;
        
        // 4. 检查属性是否标注了 @Language("SpEL")
        if (!SpelValidatorUtil.isSpelLanguageAttribute(method)) {
            return;
        }
        
        // 5. 获取注解对象
        PsiElement annotationParent = nameValuePair.getParent();
        if (!(annotationParent instanceof PsiAnnotationParameterList)) {
            return;
        }
        
        PsiElement annotation = annotationParent.getParent();
        if (!(annotation instanceof PsiAnnotation)) {
            return;
        }
        
        // 6. 检查注解是否为约束注解
        if (!SpelValidatorUtil.isSpelConstraintAnnotation((PsiAnnotation) annotation)) {
            return;
        }
        
        // 7. 注入 SpEL 语言
        Language spelLanguage = Language.findLanguageByID(SPEL_LANGUAGE_ID);
        if (spelLanguage != null) {
            LOG.debug("Injecting SpEL language for: " + value);
            injectedLanguagePlaces.addPlace(spelLanguage, host.getTextRange(), null, null);
        } else {
            LOG.debug("SpEL language not found, skipping injection");
        }
    }
}
