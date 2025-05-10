package unipd.nonsense.util;

import java.util.*;

public class SyntaxTreePrinter
{
	public interface TokenElement
	{
		String getText();
		String getPosTag();
		int getHeadIndex();
	}

	public static <T extends TokenElement> String getSyntaxTree(List<T> tokens)
	{
		if(tokens == null || tokens.isEmpty())
			return "No tokens available for analysis";

		try
		{
			Map<Integer, T> indexToToken = createIndexTokenMap(tokens);
			Map<Integer, List<Integer>> dependencyMap = buildDependencyMap(tokens);

			T root = findRootToken(tokens);

			if(root != null)
			{
				StringBuilder treeBuilder = new StringBuilder("Syntax Tree:\n");
				int rootIndex = tokens.indexOf(root);

				buildTreeString(rootIndex, indexToToken, dependencyMap, "", true, treeBuilder);

				return treeBuilder.toString();
			}

			return "Root not found - invalid syntactic structure";

		}
		catch(Exception e)
		{
			return "Error generating syntax tree: " + e.getMessage();
		}
	}

	private static <T extends TokenElement> Map<Integer, T> createIndexTokenMap(List<T> tokens)
	{
		Map<Integer, T> map = new HashMap<>();

		for (int i = 0; i < tokens.size(); ++i)
			map.put(i, tokens.get(i));

		return map;
	}

	private static <T extends TokenElement> Map<Integer, List<Integer>> buildDependencyMap(List<T> tokens)
	{
		Map<Integer, List<Integer>> dependencyMap = new HashMap<>();

		for(int i = -1; i < tokens.size(); ++i)
			dependencyMap.put(i, new ArrayList<>());

		for(int i = 0; i < tokens.size(); i++)
		{
			T token = tokens.get(i);
			int headIdx = token.getHeadIndex();

			dependencyMap.computeIfAbsent(headIdx, k -> new ArrayList<>()).add(i);
		}

		return dependencyMap;
	}

	private static <T extends TokenElement> T findRootToken(List<T> tokens)
	{
			for (T token : tokens)
			{
				int headIdx = token.getHeadIndex();

				if(headIdx == -1)
					return token;
			}

			for(T token : tokens)
			{
				int headIdx = token.getHeadIndex();

				if(headIdx >= tokens.size() || headIdx < -1)
					return token;
			}
		return null;
	}

	private static <T extends TokenElement> void buildTreeString(int tokenIndex, Map<Integer, T> indexToToken, Map<Integer, List<Integer>> dependencyMap, String indent, boolean isLast, StringBuilder builder)
	{
		T token = indexToToken.get(tokenIndex);

		String nodeLabel = String.format("%s (%s)", token.getText(), token.getPosTag());

		builder.append(indent).append(isLast ? "└─ " : "├─ ").append(nodeLabel).append("\n");

		List<Integer> childrenIndices = dependencyMap.getOrDefault(tokenIndex, Collections.emptyList());
		Collections.sort(childrenIndices);

		String childIndent = indent + (isLast ? "   " : "│  ");

		for (int i = 0; i < childrenIndices.size(); i++)
		{
			boolean childIsLast = (i == childrenIndices.size() - 1);

			buildTreeString(childrenIndices.get(i), indexToToken, dependencyMap, childIndent, childIsLast, builder);
		}
	}
}
