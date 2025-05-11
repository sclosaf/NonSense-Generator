package unipd.nonsense.exception;

import unipd.nonsense.model.Template.TemplateType;

public class TemplateNotFoundException extends RuntimeException {
    
    private final TemplateType templateType;
    
    public TemplateNotFoundException(TemplateType templateType) {
        super("No templates found for type: " + templateType);
        this.templateType = templateType;
    }
    
    public TemplateType getTemplateType() {
        return templateType;
    }
}