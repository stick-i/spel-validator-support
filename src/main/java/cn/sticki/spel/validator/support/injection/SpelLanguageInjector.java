package cn.sticki.spel.validator.support.injection;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * SpEL 语言注入器
 * <p>
 * 本类实现 IntelliJ Platform 的 {@link LanguageInjector} 接口，
 * 为 SpEL Validator 约束注解的属性注入 SpEL 语言支持。
 * <p>
 * 功能说明：
 * <ul>
 *   <li>自动识别 SpEL Validator 的约束注解</li>
 *   <li>检查注解属性是否标注了 @Language("SpEL")</li>
 *   <li>为符合条件的字符串字面量注入 SpEL 语言</li>
 *   <li>注入后，IDEA 会自动提供 SpEL 语法高亮和基础补全</li>
 * </ul>
 * <p>
 * 注入条件（必须同时满足）：
 * <ol>
 *   <li>元素是字符串字面量（PsiLiteralExpression）</li>
 *   <li>字符串位于注解的属性值中（PsiNameValuePair）</li>
 *   <li>属性对应的方法标注了 @Language("SpEL")</li>
 *   <li>注解是 SpEL Validator 的约束注解</li>
 * </ol>
 * <p>
 * 异常处理策略：
 * <ul>
 *   <li>所有扩展点方法都使用 try-catch 捕获异常</li>
 *   <li>使用 Logger 记录错误信息，便于调试</li>
 *   <li>确保异常不会影响 IDEA 的正常功能</li>
 * </ul>
 * <p>
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5
 *
 * @author Sticki
 * @see LanguageInjector
 * @see SpelValidatorUtil#isSpelConstraintAnnotation(PsiAnnotation)
 * @see SpelValidatorUtil#isSpelLanguageAttribute(PsiMethod)
 */
public class SpelLanguageInjector implements LanguageInjector {
    
    private static final Logger LOG = Logger.getInstance(SpelLanguageInjector.class);
    
    /**
     * SpEL 语言的 ID
     * <p>
     * 此 ID 用于在 IntelliJ Platform 中查找 SpEL 语言定义。
     * SpEL 语言支持由 Spring 插件提供，如果 Spring 插件未安装，
     * 则无法找到该语言，语言注入将被跳过。
     */
    private static final String SPEL_LANGUAGE_ID = "SpEL";
    
    /**
     * 获取需要注入的语言
     * <p>
     * 此方法是 {@link LanguageInjector} 接口的实现，
     * 由 IntelliJ Platform 在处理字符串字面量时调用。
     * <p>
     * 方法会检查字符串是否位于 SpEL Validator 约束注解的属性中，
     * 如果是，则注入 SpEL 语言支持。
     *
     * @param host                  语言注入宿主（通常是字符串字面量）
     * @param injectedLanguagePlaces 注入位置的容器，用于添加语言注入
     */
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
