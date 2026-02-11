package cn.sticki.spel.validator.support.reference;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNameValuePair;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * SpEL Validator message key PSI 解析工具。
 */
final class SpelMessageKeyPsiUtil {

    private static final String MESSAGE_METHOD_NAME = "message";

    /**
     * 与 ValidatorMessageInterpolator 保持一致：
     * 1. 首尾必须是 { 和 }
     * 2. 不做 trim
     */
    private static final Pattern MESSAGE_KEY_PATTERN = Pattern.compile("^\\{[^{}]+}$");

    private SpelMessageKeyPsiUtil() {
    }

    @Nullable
    static MessageKeyInfo extractMessageKeyInfo(PsiLiteralExpression literalExpression) {
        Object literalValueObj = literalExpression.getValue();
        if (!(literalValueObj instanceof String literalValue)) {
            return null;
        }

        MessageKeyInfo keyInfo = parseMessageKey(literalExpression, literalValue);
        if (keyInfo == null) {
            return null;
        }

        PsiNameValuePair nameValuePair = findContainingNameValuePair(literalExpression);
        if (nameValuePair == null || !isMessageAttribute(nameValuePair)) {
            return null;
        }

        // 只在 SpEL 约束注解的 message 属性上生效，避免影响其他注解的同名字段。
        PsiAnnotation annotation = findContainingAnnotation(nameValuePair);
        if (!SpelValidatorUtil.isSpelConstraintAnnotation(annotation)) {
            return null;
        }

        return keyInfo;
    }

    @Nullable
    private static MessageKeyInfo parseMessageKey(PsiLiteralExpression literalExpression, String literalValue) {
        if (!MESSAGE_KEY_PATTERN.matcher(literalValue).matches()) {
            return null;
        }

        TextRange valueRange = ElementManipulators.getValueTextRange(literalExpression);
        if (valueRange.getLength() < 2) {
            return null;
        }

        String key = literalValue.substring(1, literalValue.length() - 1);
        // range 仅覆盖大括号内部文本，交给 PropertyReference 直接解析 key 本体。
        TextRange keyRange = TextRange.create(
                valueRange.getStartOffset() + 1,
                valueRange.getEndOffset() - 1
        );

        return new MessageKeyInfo(key, keyRange);
    }

    @Nullable
    private static PsiNameValuePair findContainingNameValuePair(PsiLiteralExpression literalExpression) {
        PsiElement parent = literalExpression.getParent();
        // 到注解节点即停止；message 参数不会跨越注解边界。
        while (parent != null && !(parent instanceof PsiAnnotation)) {
            if (parent instanceof PsiNameValuePair) {
                return (PsiNameValuePair) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private static boolean isMessageAttribute(PsiNameValuePair nameValuePair) {
        return MESSAGE_METHOD_NAME.equals(nameValuePair.getName());
    }

    @Nullable
    private static PsiAnnotation findContainingAnnotation(PsiNameValuePair nameValuePair) {
        PsiElement annotationParent = nameValuePair.getParent();
        if (!(annotationParent instanceof PsiAnnotationParameterList)) {
            return null;
        }

        PsiElement annotation = annotationParent.getParent();
        if (annotation instanceof PsiAnnotation) {
            return (PsiAnnotation) annotation;
        }

        return null;
    }

    static final class MessageKeyInfo {

        private final String key;

        private final TextRange keyRange;

        MessageKeyInfo(String key, TextRange keyRange) {
            this.key = key;
            this.keyRange = keyRange;
        }

        String getKey() {
            return key;
        }

        TextRange getKeyRange() {
            return keyRange;
        }
    }

}
