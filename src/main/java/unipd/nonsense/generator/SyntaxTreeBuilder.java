package unipd.nonsense.generator;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
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
		logger.logTrace("getSyntaxTree: Starting syntax tree generation");

		if(tokens == null || tokens.isEmpty())
		{
			logger.logWarn("getSyntaxTree: No tokens available for analysis");
			return "No tokens available for analysis";
		}

		try
		{
			logger.logTrace("getSyntaxTree: Creating index to token map");
			Map<Integer, T> indexToToken = createIndexTokenMap(tokens);

			logger.logTrace("getSyntaxTree: Building dependency map");
			Map<Integer, List<Integer>> dependencyMap = buildDependencyMap(tokens);

			logger.logTrace("getSyntaxTree: Finding root tokens");
			List<T> rootTokens = findRootTokens(tokens);

			if(rootTokens.isEmpty())
			{
				logger.logWarn("getSyntaxTree: No root tokens found due to invalid syntactic structure");
				return "No root tokens found - invalid syntactic structure";
			}

			StringBuilder treeBuilder = new StringBuilder();

			if(rootTokens.size() == 1)
			{
				logger.logDebug("getSyntaxTree: Single sentence detected with root token: " + rootTokens.get(0).getText());
				T root = rootTokens.get(0);
				int rootIndex = tokens.indexOf(root);

				List<Integer> punctuationTokens = findPunctuationTokens(tokens);

				logger.logDebug("getSyntaxTree: Found " + punctuationTokens.size() + " punctuation tokens");

				for(Integer punctIndex : punctuationTokens)
				{
					if(!isConnectedToTree(punctIndex, rootIndex, dependencyMap))
					{
						int newHead = findAppropriateHeadForPunctuation(punctIndex, rootIndex, tokens);

						logger.logDebug("getSyntaxTree: Reattached punctuation at index " + punctIndex + " to new head at index " + newHead);

						if(!dependencyMap.containsKey(newHead))
							dependencyMap.put(newHead, new ArrayList<>());

						dependencyMap.get(newHead).add(punctIndex);
					}
				}

				buildTreeString(rootIndex, indexToToken, dependencyMap, "", true, treeBuilder);

				logger.logTrace("getSyntaxTree: Successfully built syntax tree for single sentence");

				return treeBuilder.toString();
			}
			else
			{
				logger.logDebug("getSyntaxTree: Multiple sentences detected (" + rootTokens.size() + ")");

				for(int i = 0; i < rootTokens.size(); i++)
				{
					T root = rootTokens.get(i);
					int rootIndex = tokens.indexOf(root);

					logger.logDebug("getSyntaxTree: Processing sentence " + (i + 1) + " with root token: " + root.getText());

					List<Integer> punctuationTokens = findPunctuationTokens(tokens);

					logger.logDebug("getSyntaxTree: Found " + punctuationTokens.size() + " punctuation tokens for sentence " + (i + 1));

					for(Integer punctIndex : punctuationTokens)
					{
						if(!isConnectedToTree(punctIndex, rootIndex, dependencyMap))
						{
							int newHead = findAppropriateHeadForPunctuation(punctIndex, rootIndex, tokens);

							logger.logDebug("getSyntaxTree: Reattached punctuation at index " + punctIndex + " to new head at index " + newHead + " for sentence " + (i + 1));

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

				logger.logTrace("getSyntaxTree: Successfully built syntax trees for all sentences");

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
		logger.logDebug("findRootTokens: Searching for root tokens among " + tokens.size() + " tokens");
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
			logger.logTrace("findRootTokens: No ROOT label tokens found, checking for head index -1");

			for(T token : tokens)
			{
				int headIdx = token.getHeadTokenIndex();

				if(headIdx == -1 && !token.getPosTag().equals("PUNCT"))
				{
					logger.logDebug("findRootTokens: Found root token with index -1: " + token.getText());
					rootTokens.add(token);
				}
			}
		}

		if(rootTokens.isEmpty())
		{
			logger.logTrace("findRootTokens: No -1 head index tokens found, checking for invalid head indices");

			for(T token : tokens)
			{
				int headIdx = token.getHeadTokenIndex();

				if((headIdx >= tokens.size() || headIdx < -1) && !token.getPosTag().equals("PUNCT"))
				{
					logger.logDebug("findRootTokens: Found invalid head index " + headIdx + " for token: " + token.getText());
					rootTokens.add(token);
				}
			}
		}

		logger.logDebug("findRootTokens: Found " + rootTokens.size() + " root tokens");
		return rootTokens;
	}

	private static <T extends SyntaxToken> List<Integer> findPunctuationTokens(List<T> tokens)
	{
		logger.logDebug("findPunctuationTokens: Searching for punctuation tokens");
		List<Integer> punctuationIndices = new ArrayList<>();

		for(int i = 0; i < tokens.size(); ++i)
		{
			if(tokens.get(i).getPosTag().equals("PUNCT"))
			{
				logger.logDebug("findPunctuationTokens: Found punctuation token at index " + i + ": " + tokens.get(i).getText());
				punctuationIndices.add(i);
			}
		}

		logger.logDebug("findPunctuationTokens: Found " + punctuationIndices.size() + " punctuation tokens");

		return punctuationIndices;
	}

	private static <T extends SyntaxToken> Map<Integer, T> createIndexTokenMap(List<T> tokens)
	{
		logger.logDebug("createIndexTokenMap: Creating token index map for " + tokens.size() + " tokens");
		Map<Integer, T> map = new HashMap<>();

		for(int i = 0; i < tokens.size(); ++i)
		{
			map.put(i, tokens.get(i));
			logger.logDebug("createIndexTokenMap: Mapped index " + i + " to token: " + tokens.get(i).getText());
		}

		logger.logDebug("createIndexTokenMap: Token index map created with " + map.size() + " entries");
		return map;
	}

	private static boolean isConnectedToTree(int tokenIndex, int rootIndex, Map<Integer, List<Integer>> dependencyMap)
	{
		logger.logDebug("isConnectedToTree: Checking if token " + tokenIndex + " is connected to root " + rootIndex);


		if(dependencyMap.getOrDefault(rootIndex, Collections.emptyList()).contains(tokenIndex))
		{
			logger.logDebug("isConnectedToTree: Token " + tokenIndex + " is directly connected to root");
			return true;
		}

		Deque<Integer> queue = new ArrayDeque<>();
		queue.add(rootIndex);
		Set<Integer> visited = new HashSet<>();
		visited.add(rootIndex);

		while(!queue.isEmpty())
		{
			int current = queue.poll();
			for(int child : dependencyMap.getOrDefault(current, Collections.emptyList()))
			{
				if(child == tokenIndex)
				{
					logger.logDebug("isConnectedToTree: Token " + tokenIndex + " is connected via path through " + current);
					return true;
				}

				if(!visited.contains(child))
				{
					visited.add(child);
					queue.add(child);
				}
			}
		}

		logger.logDebug("isConnectedToTree: Token " + tokenIndex + " is not connected to the tree");
		return false;
	}

	private static <T extends SyntaxToken> int findAppropriateHeadForPunctuation(int punctIndex, int rootIndex, List<T> tokens)
	{
		logger.logDebug("findAppropriateHeadForPunctuation: Finding head for punctuation at index " + punctIndex);

		if(punctIndex == 0 || tokens.get(punctIndex).getHeadTokenIndex() == -1)
		{
			logger.logDebug("findAppropriateHeadForPunctuation: Punctuation at start or with invalid head, attaching to root at index " + rootIndex);
			return rootIndex;
		}

		for(int i = punctIndex - 1; i >= 0; i--)
		{
			if(!tokens.get(i).getPosTag().equals("PUNCT"))
			{
				logger.logDebug("findAppropriateHeadForPunctuation: Using nearest non-punctuation token at index " + i + " as head");
				return i;
			}
		}

		logger.logDebug("findAppropriateHeadForPunctuation: No appropriate non-punctuation head found, using root as head");
		return rootIndex;
	}

	private static <T extends SyntaxToken> Map<Integer, List<Integer>> buildDependencyMap(List<T> tokens)
	{
		logger.logDebug("buildDependencyMap: Preparing dependency map for " + tokens.size() + " tokens");
		Map<Integer, List<Integer>> dependencyMap = new HashMap<>();

		for(int i = -1; i < tokens.size(); ++i)
		{
			dependencyMap.put(i, new ArrayList<>());
			logger.logDebug("buildDependencyMap: Initialized dependencies for index: " + i);
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
			logger.logDebug("buildDependencyMap: Added dependency from head " + headIdx + " to child " + i + " (" + token.getText() + ")");
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

			logger.logDebug("buildTreeString: Processing token: " + nodeLabel);

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

			List<Integer> childrenIndices = dependencyMap.getOrDefault(current.tokenIndex, Collections.emptyList());
			logger.logDebug("buildTreeString: Found " + childrenIndices.size() + " children for token " + current.tokenIndex);
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

				logger.logDebug("buildTreeString: Adding child " + childIndex + " (" + childToken.getText() + ") to processing stack");
				stack.push(new TreeNodeInfo(childIndex, nextIndent, childIsLast, current.depth + 1));
			}
		}

		logger.logDebug("buildTreeString: Completed tree building for root index " + rootIndex);
	}
}
