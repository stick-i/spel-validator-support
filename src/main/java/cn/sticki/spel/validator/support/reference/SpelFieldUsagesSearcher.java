package cn.sticki.spel.validator.support.reference;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

/**
 * SpEL 字段使用搜索器
 * <p>
 * 实现 Find Usages 功能，在 SpEL 表达式中查找字段的使用。
 * 当用户在字段定义处执行 "Find Usages" 时，会搜索所有包含 #this.fieldName 的 SpEL 表达式。
 */
public class SpelFieldUsagesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
    
    public SpelFieldUsagesSearcher() {
        super(true); // requireReadAction = true
    }
    
    @Override
    public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters,
                             @NotNull Processor<? super PsiReference> consumer) {
        PsiElement target = queryParameters.getElementToSearch();
        
        // 只处理字段
        if (!(target instanceof PsiField)) {
            return;
        }
        
        PsiField field = (PsiField) target;
        String fieldName = field.getName();
        PsiClass containingClass = field.getContainingClass();
        
        if (containingClass == null || fieldName == null) {
            return;
        }
        
        SearchScope scope = queryParameters.getEffectiveSearchScope();
        if (!(scope instanceof GlobalSearchScope)) {
            return;
        }
        
        // 搜索包含 #this.fieldName 的字符串
        String searchText = "#this." + fieldName;
        
        // 在项目中搜索包含该文本的文件
        ReadAction.run(() -> {
            PsiSearchHelper searchHelper = PsiSearchHelper.getInstance(field.getProject());
            
            searchHelper.processElementsWithWord(
                    (element, offsetInElement) -> {
                        processElement(element, fieldName, containingClass, consumer);
                        return true;
                    },
                    (GlobalSearchScope) scope,
                    searchText,
                    UsageSearchContext.IN_STRINGS,
                    true
            );
        });
    }
    
    private void processElement(@NotNull PsiElement element,
                                @NotNull String fieldName,
                                @NotNull PsiClass targetClass,
                                @NotNull Processor<? super PsiReference> consumer) {
        // 查找字符串字面量
        if (!(element instanceof PsiLiteralExpression)) {
            PsiElement parent = element.getParent();
            if (parent instanceof PsiLiteralExpression) {
                element = parent;
            } else {
                return;
            }
        }
        
        PsiLiteralExpression literal = (PsiLiteralExpression) element;
        Object value = literal.getValue();
        
        if (!(value instanceof String)) {
            return;
        }
        
        String text = (String) value;
        if (!text.contains("#this." + fieldName)) {
            return;
        }
        
        // 检查是否在约束注解中
        PsiAnnotation annotation = findContainingAnnotation(literal);
        if (annotation == null || !SpelValidatorUtil.isSpelConstraintAnnotation(annotation)) {
            return;
        }
        
        // 获取上下文类并验证是否匹配
        PsiClass contextClass = SpelValidatorUtil.getContextClass(annotation);
        if (contextClass == null || !isClassOrSuperclass(contextClass, targetClass)) {
            return;
        }
        
        // 获取已创建的引用
        PsiReference[] references = literal.getReferences();
        for (PsiReference ref : references) {
            if (ref instanceof SpelFieldReference) {
                SpelFieldReference spelRef = (SpelFieldReference) ref;
                // 检查引用的字段名是否匹配（支持嵌套路径的最后一级）
                String refFieldName = spelRef.getFieldName();
                if (refFieldName.equals(fieldName) || refFieldName.endsWith("." + fieldName)) {
                    PsiElement resolved = ref.resolve();
                    if (resolved != null && resolved.equals(targetClass.findFieldByName(fieldName, true))) {
                        consumer.process(ref);
                    }
                }
            }
        }
    }
    
    /**
     * 检查 contextClass 是否是 targetClass 或其子类
     */
    private boolean isClassOrSuperclass(@NotNull PsiClass contextClass, @NotNull PsiClass targetClass) {
        if (contextClass.equals(targetClass)) {
            return true;
        }
        
        // 检查 targetClass 是否是 contextClass 的父类
        PsiClass superClass = contextClass.getSuperClass();
        while (superClass != null) {
            if (superClass.equals(targetClass)) {
                return true;
            }
            superClass = superClass.getSuperClass();
        }
        
        return false;
    }
    
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
}
