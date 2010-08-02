/**
 * 
 */
package edu.illinois.cs.cogcomp.util;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * @author vivek
 * 
 */
public class WhiteSpaceTokenizer
{
	public static java.util.ArrayList<String> tokenize(String line)
	{
		StringTokenizer tokenizer = new StringTokenizer(line);

		ArrayList<String> tokens = new ArrayList<String>();
		while (tokenizer.hasMoreTokens())
			tokens.add(tokenizer.nextToken());

		return tokens;
	}

	public static String[] tokenizeToArray(String line)
	{
		StringTokenizer tokenizer = new StringTokenizer(line);

		String[] tokens = new String[tokenizer.countTokens()];
		int i = 0;
		while (tokenizer.hasMoreTokens())
			tokens[i++] = tokenizer.nextToken();

		return tokens;
	}

	public static int getNumberOfTokens(String input)
	{
		return tokenizeToArray(input).length;
	}
}
