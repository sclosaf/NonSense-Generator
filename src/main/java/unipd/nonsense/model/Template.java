package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Template
{
    public static enum TemplateType
    {
        SINGULAR,
        PLURAL
    }

    public static enum Placeholder
    {
        NOUN,
        VERB,
        ADJECTIVE
    }

    private String pattern;
    private final TemplateType type;

    public Template(String pattern, TemplateType type)
    {
        if(pattern == null || pattern.isEmpty())
            throw new InvalidGrammaticalElementException();

        this.pattern = pattern;
        this.type = type;
    }

    public String getPattern()
    {
        return pattern;
    }

    public TemplateType getType()
    {
        return type;
    }

    public boolean containsPlaceholder(Placeholder placeholder)
    {
        return pattern.contains("[" + placeholder.name().toLowerCase() + "]");
    }

    public int countPlaceholders(Placeholder placeholder)
    {
        String target = "[" + placeholder.name().toLowerCase() + "]";

        int count = 0;
        int index = pattern.indexOf(target);

        while (index != -1)
        {
            count++;
            index = pattern.indexOf(target, index + target.length());
        }

        return count;
    }

    public void replacePlaceholder(Placeholder placeholder, String replacement)
    {
        String target = "[" + placeholder.name().toLowerCase() + "]";
        List<Integer> indices = new ArrayList<>();
        int index = pattern.indexOf(target);

        while (index != -1)
        {
            indices.add(index);
            index = pattern.indexOf(target, index + target.length());
        }

        if (!indices.isEmpty())
        {
            Random random = new Random();
            int randomIndex = indices.get(random.nextInt(indices.size()));

            StringBuilder sb = new StringBuilder(pattern);
            sb.replace(randomIndex, randomIndex + target.length(), replacement);
            pattern = sb.toString();
        }
    }

    public Template withReplacement(Placeholder placeholder, String replacement)
    {
        String target = "[" + placeholder.name().toLowerCase() + "]";
        List<Integer> indices = new ArrayList<>();
        int index = pattern.indexOf(target);
        String newPattern = pattern;

        while (index != -1) {
            indices.add(index);
            index = newPattern.indexOf(target, index + target.length());
        }

        if (!indices.isEmpty()) {
            Random random = new Random();
            int randomIndexToReplace = indices.get(random.nextInt(indices.size()));
            StringBuilder sb = new StringBuilder(newPattern);
            sb.replace(randomIndexToReplace, randomIndexToReplace + target.length(), replacement);
            newPattern = sb.toString();
        }
        return new Template(newPattern, this.type);
    }

    @Override
    public String toString()
    {
        return pattern;
    }
}