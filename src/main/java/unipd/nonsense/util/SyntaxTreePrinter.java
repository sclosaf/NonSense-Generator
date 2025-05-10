package unipd.nonsense.util;

import com.google.cloud.language.v1.Token;
import java.util.*;

public class SyntaxTreePrinter
{
	public static String getSyntaxTree(List<Token> tokens)
	{
		Map<Token, Integer> tokenToIndex = createTokenIndexMap(tokens);
		Token root = findRootToken(tokens, tokenToIndex);

		if(root != null)
		{
			StringBuilder treeBuilder = new StringBuilder("\nAlbero sintattico:\n");
			buildTreeString(root, tokenToIndex, tokens, "", true, treeBuilder);
			return treeBuilder.toString();
        }
		else
			return "No radix found for the tree";
	}

	private static Map<Token, Integer> createTokenIndexMap(List<Token> tokens)
	{
		Map<Token, Integer> map = new HashMap<>();

		for(int i = 0; i < tokens.size(); ++i)
			map.put(tokens.get(i), i);

		return map;
	}

	private static Token findRootToken(List<Token> tokens, Map<Token, Integer> tokenToIndex)
	{
		for(Token token : tokens)
		{
			int headIdx = token.getDependencyEdge().getHeadTokenIndex();

			if (headIdx == -1 || headIdx == tokenToIndex.get(token))
				return token;
		}

		return null;
	}

	private static void buildTreeString(Token token, Map<Token, Integer> tokenToIndex, List<Token> tokens, String indent, boolean isLast, StringBuilder builder)
	{
		String nodeLabel = String.format("%s (%s)", token.getText().getContent(), token.getPartOfSpeech().getTag());
		builder.append(indent).append(isLast ? "└─ " : "├─ ").append(nodeLabel).append("\n");

		String childIndent = indent + (isLast ? "   " : "│  ");

		List<Token> children = getChildren(token, tokenToIndex, tokens);

		for (int i = 0; i < children.size(); ++i)
			buildTreeString(children.get(i), tokenToIndex, tokens, childIndent, i == children.size() - 1, builder);
	}

	private static List<Token> getChildren(Token parent, Map<Token, Integer> tokenToIndex, List<Token> tokens)
	{
		List<Token> children = new ArrayList<>();
		int parentIndex = tokenToIndex.get(parent);

		for (Token token : tokens)
			if (token != parent && token.getDependencyEdge().getHeadTokenIndex() == parentIndex)
				children.add(token);

		return children;
	}
}
