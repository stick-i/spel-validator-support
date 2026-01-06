package cn.sticki.spel.validator.support.injection;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.lang.Language;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * SpEL 语言注入器
 * 为 SpEL Validator 注解的属性注入 SpEL 语言支持
 * 
 * @author Sticki
 */
public class SpelLanguageInjector implements LanguageInjector {
    
    /**
     * SpEL 语言 ID
     */
    private static final String SPEL_LANGUAGE_ID = "SpEL";
    
    @Override
    public void getLanguagesToInject(@NotNull PsiLanguageInjectionHost host, 
                                     @NotNull InjectedLanguagePlaces injectedLanguagePlaces) {
        try {
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
                injectedLanguagePlaces.addPlace(spelLanguage, host.getTextRange(), null, null);
            }
        } catch (Exception e) {
            // 忽略异常，不影响 IDEA 正常功能
        }
    }
}
