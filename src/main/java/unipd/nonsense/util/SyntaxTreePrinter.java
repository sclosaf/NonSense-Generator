package unipd.nonsense.util;

import java.util.*;

import unipd.nonsense.util.LoggerManager;

public class SyntaxTreePrinter
{
	private static final LoggerManager logger = new LoggerManager(SyntaxTreePrinter.class);

	public interface TokenElement
	{
		String getText();
		String getPosTag();
		int getHeadIndex();
	}

	public static <T extends TokenElement> String getSyntaxTree(List<T> tokens)
	{
		logger.logInfo("getSyntaxTree: Starting syntax tree generation");

		if(tokens == null || tokens.isEmpty())
		{
			logger.logWarn("getSyntaxTree: No tokens available for analysis");
			return "No tokens available for analysis";
		}

		try
		{
			logger.logInfo("getSyntaxTree: Creating index to token map");
			Map<Integer, T> indexToToken = createIndexTokenMap(tokens);

			logger.logInfo("getSyntaxTree: Building dependency map");
			Map<Integer, List<Integer>> dependencyMap = buildDependencyMap(tokens);

			logger.logInfo("getSyntaxTree: Finding root token");
			T root = findRootToken(tokens);

			if(root != null)
			{
				logger.logDebug("getSyntaxTree: Root found at index: " + tokens.indexOf(root));
				StringBuilder treeBuilder = new StringBuilder("Syntax Tree:\n");
				int rootIndex = tokens.indexOf(root);

				logger.logInfo("getSyntaxTree: Building tree string representation");
				buildTreeString(rootIndex, indexToToken, dependencyMap, "", true, treeBuilder);

				logger.logInfo("getSyntaxTree: Syntax tree generated successfully");

				return treeBuilder.toString();
			}

			logger.logWarn("getSyntaxTree: Root not found due to invalid syntactic structure");
			return "Root not found - invalid syntactic structure";

		}
		catch(Exception e)
		{
			logger.logError("getSyntaxTree: Error generating syntax tree", e);
			return "Error generating syntax tree: " + e.getMessage();
		}
	}

	private static <T extends TokenElement> Map<Integer, T> createIndexTokenMap(List<T> tokens)
	{
		logger.logDebug("createIndexTokenMap: Creating token index map for " + tokens.size() + " tokens");
		Map<Integer, T> map = new HashMap<>();

		for(int i = 0; i < tokens.size(); ++i)
		{
			map.put(i, tokens.get(i));
			logger.logInfo("createIndexTokenMap: Mapped index " + i + " to token: " + tokens.get(i).getText());
		}

		logger.logDebug("createIndexTokenMap: Token index map created with " + map.size() + " entries");
		return map;
	}

	private static <T extends TokenElement> Map<Integer, List<Integer>> buildDependencyMap(List<T> tokens)
	{
		logger.logDebug("buildDependencyMap: Preparing dependency map for " + tokens.size() + " tokens");
		Map<Integer, List<Integer>> dependencyMap = new HashMap<>();

		for(int i = -1; i < tokens.size(); ++i)
		{
			dependencyMap.put(i, new ArrayList<>());
			logger.logInfo("buildDependencyMap: Initialized dependencies for index: " + i);
		}

		for(int i = 0; i < tokens.size(); i++)
		{
			T token = tokens.get(i);
			int headIdx = token.getHeadIndex();

			dependencyMap.computeIfAbsent(headIdx, k -> new ArrayList<>()).add(i);
			logger.logInfo("buildDependencyMap: Added dependency from head " + headIdx + " to child " + i);
		}

		logger.logDebug("buildDependencyMap: Dependency map built with " + dependencyMap.size() + " entries");

		return dependencyMap;
	}

	private static <T extends TokenElement> T findRootToken(List<T> tokens)
	{
		logger.logDebug("findRootToken: Searching for root token");

		for (T token : tokens)
		{
			int headIdx = token.getHeadIndex();

			if(headIdx == -1)
			{
				logger.logDebug("findRootToken: Found root token with index -1: " + token.getText());
				return token;
			}
		}

		for(T token : tokens)
		{
			int headIdx = token.getHeadIndex();

			if(headIdx >= tokens.size() || headIdx < -1)
			{
				logger.logDebug("findRootToken: Found invalid head index " + headIdx + " for token: " + token.getText());
				return token;
			}
		}

		logger.logInfo("findRootToken: No valid root token found");

		return null;
	}

	private static <T extends TokenElement> void buildTreeString(int tokenIndex, Map<Integer, T> indexToToken, Map<Integer, List<Integer>> dependencyMap, String indent, boolean isLast, StringBuilder builder)
	{
		logger.logDebug("buildTreeString: Building tree string for token index: " + tokenIndex);

		T token = indexToToken.get(tokenIndex);
		String nodeLabel = String.format("%s (%s)", token.getText(), token.getPosTag());

		builder.append(indent).append(isLast ? "└─ " : "├─ ").append(nodeLabel).append("\n");
		logger.logDebug("buildTreeString: Added node to tree: " + nodeLabel);

		List<Integer> childrenIndices = dependencyMap.getOrDefault(tokenIndex, Collections.emptyList());
		Collections.sort(childrenIndices);

		logger.logDebug("buildTreeString: Found " + childrenIndices.size() + " children for token index: " + tokenIndex);

		String childIndent = indent + (isLast ? "   " : "│  ");

		for (int i = 0; i < childrenIndices.size(); i++)
		{
			boolean childIsLast = (i == childrenIndices.size() - 1);

			logger.logDebug("buildTreeString: Processing child " + i + " of " + tokenIndex + ", isLast: " + childIsLast);

			buildTreeString(childrenIndices.get(i), indexToToken, dependencyMap, childIndent, childIsLast, builder);
		}
	}
}
