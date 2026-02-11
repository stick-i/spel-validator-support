package cn.sticki.spel.validator.support.reference;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * 为 SpEL Validator 注解的 message 属性提供国际化 key 引用能力。
 */
public class SpelMessageKeyReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                // 只在注解参数内部注册，避免扫描普通字符串字面量。
                PlatformPatterns.psiElement(PsiLiteralExpression.class)
                        .inside(PsiNameValuePair.class),
                new PsiReferenceProvider() {
                    @Override
                    public @NotNull PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                                          @NotNull ProcessingContext context) {
                        if (!(element instanceof PsiLiteralExpression literalExpression)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        SpelMessageKeyPsiUtil.MessageKeyInfo messageKeyInfo =
                                SpelMessageKeyPsiUtil.extractMessageKeyInfo(literalExpression);
                        if (messageKeyInfo == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        return new PsiReference[]{
                                // soft=false：让 unresolved key 走官方 i18n 检查链路（UnresolvedPropertyKey）。
                                new SpelBracedPropertyReference(
                                        messageKeyInfo.getKey(),
                                        literalExpression,
                                        null,
                                        false,
                                        messageKeyInfo.getKeyRange()
                                )
                        };
                    }
                }
        );
    }

}
