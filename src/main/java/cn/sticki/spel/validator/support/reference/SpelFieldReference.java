package cn.sticki.spel.validator.support.reference;

import cn.sticki.spel.validator.support.util.SpelValidatorUtil;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * SpEL 字段引用实现
 * <p>
 * 本类继承 {@link PsiReferenceBase}，实现 SpEL 表达式中字段引用的具体行为。
 * 每个 SpelFieldReference 实例代表一个字段引用（如 #this.userName 中的 "userName"）。
 * <p>
 * 支持的功能：
 * <ul>
 *   <li>Ctrl+Click 跳转：点击字段名跳转到字段定义</li>
 *   <li>Find Usages：在查找使用时包含 SpEL 表达式中的引用</li>
 *   <li>字段重命名：重命名字段时自动更新 SpEL 表达式中的引用</li>
 *   <li>代码补全：提供字段补全候选项</li>
 * </ul>
 * <p>
 * 嵌套字段支持：
 * <ul>
 *   <li>支持简单字段引用：#this.userName</li>
 *   <li>支持嵌套字段引用：#this.user.address.city</li>
 *   <li>每一级字段都会创建独立的引用实例</li>
 * </ul>
 * <p>
 * 性能优化：
 * <ul>
 *   <li>使用 {@link ReadAction#compute} 确保线程安全</li>
 *   <li>引用解析在后台线程执行，不阻塞 UI</li>
 * </ul>
 * <p>
 * Requirements: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3
 *
 * @author Sticki
 * @see PsiReferenceBase
 * @see SpelFieldReferenceContributor
 * @see SpelValidatorUtil#resolveNestedField(PsiClass, String)
 */
public class SpelFieldReference extends PsiReferenceBase<PsiElement> {
    
    private static final Logger LOG = Logger.getInstance(SpelFieldReference.class);
    
    /**
     * 字段名或字段路径
     * <p>
     * 对于简单引用，如 #this.userName，值为 "userName"
     * 对于嵌套引用，如 #this.user.address，值为 "user.address"
     * <p>
     * 注意：每个引用实例只负责路径中的一个字段，
     * 但 fieldName 存储的是从 #this 开始到当前字段的完整路径
     */
    private final String fieldName;
    
    /**
     * 上下文类
     * <p>
     * 即 #this 变量指向的类，是字段查找的起始点。
     * 对于嵌套字段，仍然从此类开始逐级解析。
     */
    private final PsiClass contextClass;
    
    /**
     * 构造函数
     * 
     * @param element 引用所在的 PSI 元素
     * @param textRange 引用在元素中的文本范围
     * @param fieldName 字段名
     * @param contextClass 上下文类
     */
    public SpelFieldReference(@NotNull PsiElement element, 
                              @NotNull TextRange textRange,
                              @NotNull String fieldName, 
                              @Nullable PsiClass contextClass) {
        super(element, textRange);
        this.fieldName = fieldName;
        this.contextClass = contextClass;
    }
    
    /**
     * 解析字段引用
     * 返回字段引用指向的 PsiField 对象
     * 使用 ReadAction.compute 确保线程安全
     * 
     * @return 解析到的 PsiField，如果解析失败返回 null
     */
    @Nullable
    @Override
    public PsiElement resolve() {
        if (contextClass == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        
        try {
            // 使用 ReadAction.compute 确保在读取操作中执行，保证线程安全
            return ReadAction.compute(() -> {
                try {
                    // 使用工具类解析嵌套字段
                    return SpelValidatorUtil.resolveNestedField(contextClass, fieldName);
                } catch (Exception e) {
                    LOG.debug("Error resolving field reference '" + fieldName + "': " + e.getMessage());
                    return null;
                }
            });
        } catch (Exception e) {
            LOG.warn("Error in ReadAction while resolving field reference: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 处理字段重命名
     * 当字段被重命名时，更新 SpEL 表达式中的字段引用
     * 
     * @param newElementName 新的字段名
     * @return 更新后的元素
     * @throws IncorrectOperationException 如果重命名操作失败
     */
    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        try {
            PsiElement element = getElement();
            
            // 获取当前元素的文本
            String currentText = element.getText();
            
            // 计算新的文本
            TextRange rangeInElement = getRangeInElement();
            String newText = currentText.substring(0, rangeInElement.getStartOffset()) 
                    + newElementName 
                    + currentText.substring(rangeInElement.getEndOffset());
            
            // 创建新的字符串字面量
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(element.getProject());
            PsiExpression newExpression = factory.createExpressionFromText(newText, element.getContext());
            
            // 替换原元素
            return element.replace(newExpression);
        } catch (Exception e) {
            LOG.debug("Error handling element rename, falling back to default: " + e.getMessage());
            // 如果自定义处理失败，回退到默认实现
            return super.handleElementRename(newElementName);
        }
    }
    
    /**
     * 返回补全候选项
     * 用于代码补全时显示可用的字段列表
     * 使用 ReadAction.compute 确保线程安全
     * 
     * @return 补全候选项数组
     */
    @NotNull
    @Override
    public Object[] getVariants() {
        if (contextClass == null) {
            return new Object[0];
        }
        
        try {
            // 使用 ReadAction.compute 确保在读取操作中执行
            return ReadAction.compute(() -> {
                try {
                    List<Object> variants = new ArrayList<>();
                    
                    // 确定目标类（处理嵌套字段）
                    PsiClass targetClass = resolveTargetClass();
                    if (targetClass == null) {
                        return new Object[0];
                    }
                    
                    // 获取所有字段
                    List<PsiField> fields = SpelValidatorUtil.getAllFields(targetClass);
                    
                    // 为每个字段创建补全项
                    for (PsiField field : fields) {
                        LookupElementBuilder lookupElement = LookupElementBuilder.create(field.getName())
                                .withTypeText(field.getType().getPresentableText())
                                .withIcon(field.getIcon(0));
                        variants.add(lookupElement);
                    }
                    
                    return variants.toArray();
                } catch (Exception e) {
                    LOG.debug("Error getting variants: " + e.getMessage());
                    return new Object[0];
                }
            });
        } catch (Exception e) {
            LOG.warn("Error in ReadAction while getting variants: " + e.getMessage());
            return new Object[0];
        }
    }
    
    /**
     * 解析目标类
     * 如果字段名包含嵌套路径，解析到最后一级字段的类型
     * 
     * @return 目标类
     */
    @Nullable
    private PsiClass resolveTargetClass() {
        if (contextClass == null) {
            return null;
        }
        
        // 如果字段名不包含 .，直接返回上下文类
        int lastDotIndex = fieldName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return contextClass;
        }
        
        // 获取最后一个 . 之前的路径
        String parentPath = fieldName.substring(0, lastDotIndex);
        
        // 解析父路径的字段
        PsiField parentField = SpelValidatorUtil.resolveNestedField(contextClass, parentPath);
        if (parentField == null) {
            return null;
        }
        
        // 获取父字段的类型
        PsiType fieldType = parentField.getType();
        if (fieldType instanceof PsiClassType) {
            return ((PsiClassType) fieldType).resolve();
        }
        
        return null;
    }
    
    /**
     * 获取字段名
     * 
     * @return 字段名
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * 获取上下文类
     * 
     * @return 上下文类
     */
    @Nullable
    public PsiClass getContextClass() {
        return contextClass;
    }
}
