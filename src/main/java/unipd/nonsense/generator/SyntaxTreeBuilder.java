package unipd.nonsense.generator;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;
import java.util.Deque;
import java.util.ArrayDeque;

import unipd.nonsense.model.SyntaxToken;

import unipd.nonsense.util.LoggerManager;

import com.google.cloud.language.v1.DependencyEdge;

public class SyntaxTreeBuilder
{
	private static class TreeNodeInfo
	{
		int tokenIndex;
		String indent;
		boolean isLast;
		int depth;

		TreeNodeInfo(int tokenIndex, String indent, boolean isLast, int depth)
		{
			this.tokenIndex = tokenIndex;
			this.indent = indent;
			this.isLast = isLast;
			this.depth = depth;
		}
	}

	private static final LoggerManager logger = new LoggerManager(SyntaxTreeBuilder.class);

	public static <T extends SyntaxToken> String getSyntaxTree(List<T> tokens)
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

			logger.logInfo("getSyntaxTree: Finding root tokens");
			List<T> rootTokens = findRootTokens(tokens);

			if(rootTokens.isEmpty())
			{
				logger.logWarn("getSyntaxTree: No root tokens found due to invalid syntactic structure");
				return "No root tokens found - invalid syntactic structure";
			}

			StringBuilder treeBuilder = new StringBuilder();

			if(rootTokens.size() == 1)
			{
				logger.logDebug("getSyntaxTree: Single sentence detected");
				T root = rootTokens.get(0);
				int rootIndex = tokens.indexOf(root);

				List<Integer> punctuationTokens = findPunctuationTokens(tokens);

				for(Integer punctIndex : punctuationTokens)
				{
					if(!isConnectedToTree(punctIndex, rootIndex, dependencyMap))
					{
						int newHead = findAppropriateHeadForPunctuation(punctIndex, tokens);

						if(!dependencyMap.containsKey(newHead))
							dependencyMap.put(newHead, new ArrayList<>());

						dependencyMap.get(newHead).add(punctIndex);
					}
				}

				buildTreeString(rootIndex, indexToToken, dependencyMap, "", true, treeBuilder);
				return "Syntax Tree:\n" + treeBuilder.toString();
			}
			else
			{
				logger.logDebug("getSyntaxTree: Multiple sentences detected (" + rootTokens.size() + ")");
				for(int i = 0; i < rootTokens.size(); i++)
				{
					T root = rootTokens.get(i);
					int rootIndex = tokens.indexOf(root);

					List<Integer> punctuationTokens = findPunctuationTokens(tokens);
					for(Integer punctIndex : punctuationTokens)
					{
						if(!isConnectedToTree(punctIndex, rootIndex, dependencyMap))
						{
							int newHead = findAppropriateHeadForPunctuation(punctIndex, tokens);

							if(!dependencyMap.containsKey(newHead))
								dependencyMap.put(newHead, new ArrayList<>());

							dependencyMap.get(newHead).add(punctIndex);
						}
					}

					treeBuilder.append("Sentence ").append(i+1).append(":\n");
					buildTreeString(rootIndex, indexToToken, dependencyMap, "", true, treeBuilder);

					if(i < rootTokens.size() - 1)
						treeBuilder.append("\n");

				}

				return treeBuilder.toString();
			}
		}
		catch(Exception e)
		{
			logger.logError("getSyntaxTree: Error generating syntax tree", e);
			return "Error generating syntax tree: " + e.getMessage();
		}
	}

	private static <T extends SyntaxToken> List<T> findRootTokens(List<T> tokens)
	{
		logger.logDebug("findRootTokens: Searching for root tokens");
		List<T> rootTokens = new ArrayList<>();

		for(T token : tokens)
		{
			if(token.getDependencyLabel() == DependencyEdge.Label.ROOT)
			{
				logger.logDebug("findRootTokens: Found root token by dependency label ROOT: " + token.getText());
				rootTokens.add(token);
			}
		}

		if(rootTokens.isEmpty())
		{
			for(T token : tokens)
			{
				int headIdx = token.getHeadTokenIndex();

				if(headIdx == -1)
				{
					logger.logDebug("findRootTokens: Found root token with index -1: " + token.getText());
					rootTokens.add(token);
				}
			}
		}

		if(rootTokens.isEmpty())
		{
			for(T token : tokens)
			{
				int headIdx = token.getHeadTokenIndex();

				if(headIdx >= tokens.size() || headIdx < -1)
				{
					logger.logDebug("findRootTokens: Found invalid head index " + headIdx + " for token: " + token.getText());
					rootTokens.add(token);
				}
			}
		}

		logger.logInfo("findRootTokens: Found " + rootTokens.size() + " root tokens");
		return rootTokens;
	}

	private static <T extends SyntaxToken> List<Integer> findPunctuationTokens(List<T> tokens)
	{
		List<Integer> punctuationIndices = new ArrayList<>();

		for(int i = 0; i < tokens.size(); ++i)
			if(tokens.get(i).getPosTag().equals("PUNCT"))
				punctuationIndices.add(i);

		return punctuationIndices;
	}

	private static <T extends SyntaxToken> Map<Integer, T> createIndexTokenMap(List<T> tokens)
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

	private static boolean isConnectedToTree(int tokenIndex, int rootIndex, Map<Integer, List<Integer>> dependencyMap)
	{
		if(dependencyMap.containsKey(rootIndex) && dependencyMap.get(rootIndex).contains(tokenIndex))
			return true;

		for(List<Integer> children : dependencyMap.values())
			if(children.contains(tokenIndex))
				return true;

		return false;
	}

	private static <T extends SyntaxToken> int findAppropriateHeadForPunctuation(int punctIndex, List<T> tokens)
	{
		if(punctIndex > 0)
			return punctIndex - 1;

		return -1;
	}

	private static <T extends SyntaxToken> Map<Integer, List<Integer>> buildDependencyMap(List<T> tokens)
	{
		logger.logDebug("buildDependencyMap: Preparing dependency map for " + tokens.size() + " tokens");
		Map<Integer, List<Integer>> dependencyMap = new HashMap<>();

		for(int i = -1; i < tokens.size(); ++i)
		{
			dependencyMap.put(i, new ArrayList<>());
			logger.logInfo("buildDependencyMap: Initialized dependencies for index: " + i);
		}

		for(int i = 0; i < tokens.size(); ++i)
		{
			T token = tokens.get(i);
			int headIdx = token.getHeadTokenIndex();

			if(headIdx == i)
			{
				logger.logDebug("buildDependencyMap: Detected self-referencing token at index " + i + ". Setting to -1.");
				headIdx = -1;
			}

			dependencyMap.computeIfAbsent(headIdx, k -> new ArrayList<>()).add(i);
			logger.logInfo("buildDependencyMap: Added dependency from head " + headIdx + " to child " + i);
		}

		logger.logDebug("buildDependencyMap: Dependency map built with " + dependencyMap.size() + " entries");
		return dependencyMap;
	}

	private static <T extends SyntaxToken> void buildTreeString(int rootIndex, Map<Integer, T> indexToToken, Map<Integer, List<Integer>> dependencyMap, String indent, boolean isLast, StringBuilder builder)
	{
		logger.logDebug("buildTreeString: Building tree string for token index: " + rootIndex);

		Deque<TreeNodeInfo> stack = new ArrayDeque<>();
		stack.push(new TreeNodeInfo(rootIndex, "", true, 0));
		Map<Integer, Boolean> visited = new HashMap<>();

		while(!stack.isEmpty())
		{
			TreeNodeInfo current = stack.pop();

			if(visited.containsKey(current.tokenIndex))
			{
				logger.logDebug("buildTreeString: Cycle detected at token index: " + current.tokenIndex);
				continue;
			}

			visited.put(current.tokenIndex, true);

			T token = indexToToken.get(current.tokenIndex);

			if(token == null)
			{
				logger.logDebug("buildTreeString: No token found for index: " + current.tokenIndex);
				continue;
			}

			String nodeLabel = String.format("%s (%s)", token.getText(), token.getPosTag());

			StringBuilder line = new StringBuilder();
			line.append(current.indent);

			if(current.depth > 0)
			{
				if(current.isLast)
					line.append("└─");
				else
					line.append("├─");
			}

			line.append(nodeLabel);

			builder.append(line).append("\n");

			logger.logDebug("buildTreeString: Added node to tree: " + nodeLabel);

			List<Integer> childrenIndices = dependencyMap.getOrDefault(current.tokenIndex, Collections.emptyList());
			Collections.sort(childrenIndices);

			List<Integer> unvisitedChildren = new ArrayList<>();

			for(int childIndex : childrenIndices)
				if(!visited.containsKey(childIndex))
					unvisitedChildren.add(childIndex);

			String nextIndent = current.indent;

			if(current.depth > 0)
				nextIndent += current.isLast ? "  " : "│ ";

			for(int i = childrenIndices.size() - 1; i >= 0; --i)
			{
				int childIndex = childrenIndices.get(i);
				boolean childIsLast = (i == childrenIndices.size() - 1);
				T childToken = indexToToken.get(childIndex);
				stack.push(new TreeNodeInfo(childIndex, nextIndent, childIsLast, current.depth + 1));
			}
		}

		logger.logDebug("buildTreeString: Completed iterative tree building");
	}
}
