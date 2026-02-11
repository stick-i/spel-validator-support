package cn.sticki.spel.validator.support.reference;

import com.intellij.codeInsight.highlighting.HighlightedReference;
import com.intellij.lang.properties.references.PropertyReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 为 {xxx.xxx} 形式的 key 提供属性引用。
 * 实现 HighlightedReference 后，样式由 IDEA 官方 reference 高亮链路处理，
 * 避免自定义 Annotator 带来的主题色偏差。
 */
final class SpelBracedPropertyReference extends PropertyReference implements HighlightedReference {

    SpelBracedPropertyReference(@NotNull String key,
                                @NotNull PsiElement element,
                                @Nullable String bundleName,
                                boolean soft,
                                @NotNull TextRange rangeInElement) {
        super(key, element, bundleName, soft, rangeInElement);
    }

}
