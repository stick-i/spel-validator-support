package cn.sticki.spel.validator.support.spring;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightVariableBuilder;
import com.intellij.spring.el.contextProviders.SpringElContextsExtension;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring SpEL 上下文扩展
 * <p>
 * 为 SpEL Validator 注解中的表达式提供 #this 变量的上下文信息。
 * 这样 Spring 插件就能正确解析 #this 变量，支持跳转、补全等功能。
 */
public class SpelValidatorElContextsExtension extends SpringElContextsExtension {

    @Override
    public @NotNull Collection<? extends PsiVariable> getContextVariables(@NotNull PsiElement context) {
        // 查找包含的注解
        PsiAnnotation annotation = findContainingAnnotation(context);
        if (!SpelValidatorUtil.isSpelConstraintAnnotation(annotation)) {
            return Collections.emptyList();
        }

        // 获取上下文类
        PsiClass contextClass = SpelValidatorUtil.getContextClass(annotation);
        if (contextClass == null || contextClass.getQualifiedName() == null) {
            return Collections.emptyList();
        }

        // 创建 #this 变量的类型
        PsiType thisType = PsiType.getTypeByName(
                contextClass.getQualifiedName(),
                context.getProject(),
                context.getResolveScope()
        );

        // 创建一个轻量级变量表示 #this
        @SuppressWarnings("rawtypes")
        LightVariableBuilder thisVariable = new LightVariableBuilder(
                context.getManager(),
                "this",
                thisType,
                JavaLanguage.INSTANCE
        );

        return Collections.singletonList(thisVariable);
    }

    private PsiAnnotation findContainingAnnotation(@NotNull PsiElement element) {
        // 从注入的语言宿主获取
        PsiLanguageInjectionHost host = getInjectionHost(element);
        if (host != null) {
            element = host;
        }

        PsiElement current = element;
        while (current != null) {
            if (current instanceof PsiAnnotation) {
                return (PsiAnnotation) current;
            }
            current = current.getParent();
        }
        return null;
    }

    private PsiLanguageInjectionHost getInjectionHost(@NotNull PsiElement element) {
        try {
            InjectedLanguageManager manager = InjectedLanguageManager.getInstance(element.getProject());
            return manager.getInjectionHost(element);
        } catch (Exception e) {
            return null;
        }
    }

}
