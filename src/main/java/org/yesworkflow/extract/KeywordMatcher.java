package org.yesworkflow.extract;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.yesworkflow.YWKeywords;
import org.yesworkflow.YWKeywords.Tag;

/** Simple class for searching a list of comment lines for those
 *  containing YW keywords.  Optionally trims from each line
 *  characters preceding the first YW keyword.
 */
public class KeywordMatcher {
    
    /** storage for the collection of YW keywords to match lines against */
    private Set<String> keywords = new HashSet<String>();
    
    /** Constructs an instance configured to match comment lines against the
     *  provided collection of keywords.
     *  @param keywords The YW keywords against which comment lines are matched.
     */
    public KeywordMatcher(Collection<String> keywords) {
        this.keywords.addAll(keywords);
    }
    
    public static enum MatchExtent {
        NO_MATCH,
        PREFIX_MATCH,
        FULL_MATCH
    }
    
    public MatchExtent matchesKeyword(String s) {
        
        String sl = s.toLowerCase();
        
        // look for a match with single-line comment start delimiter
        for (String keyword : keywords) {
            if (keyword.startsWith(sl)) {
                return (s.length() == keyword.length()) ? MatchExtent.FULL_MATCH : MatchExtent.PREFIX_MATCH;
            }
        }
        return MatchExtent.NO_MATCH;
    }
    
    /** Searches a comment line for YW keywords.  Returns the line if
     *  a keyword is found and null otherwise.  Trims characters
     *  preceding the first keyword in the return value if requested.
     * @param line The comment line to search for YW keywords.
     * @param trim Characters preceding first keyword are trimmed in the return value if true.
     * @return The (optionally trimmed) comment line if it contains a YW keyword, or null otherwise. 
     */
    public String match(String line, boolean trim) {
        int start = findKeyword(line);
        if (start != -1) {
            return trim ? line.substring(start) : line;
        } else {
            return null;
        }
    }
    
    /** Finds the first occurrence of a YW keyword in a comment line.
     *  Returns the index of the start of the keyword or -1 if no keyword is found.
     *  
     *  <p><i>TODO: If the performance of this method proves unsatisfactory, replace it
     *  with an implementation that uses a trie (prefix tree) to represent the set of
     *  keywords.</i></p>
     *  
     *  @param line The comment line to search for YW keywords.
     *  @return The start index of the first keyword found, or -1 if no keyword is found. 
     */
    public int findKeyword(String line) {
        String lineLowerCase = line.toLowerCase();
        int firstKeywordStart = -1;
        for (String keyword : keywords) {
            int start = lineLowerCase.indexOf(keyword);
            if (start == -1) continue;
            if (firstKeywordStart == -1 || start < firstKeywordStart) {
                firstKeywordStart = start;
            }
        }
        return firstKeywordStart;
    }
    
    public static Tag extractInitialKeyword(String s, YWKeywords keywords) {
        String firstToken = new StringTokenizer(s).nextToken();
        return keywords.getTag(firstToken);
    }
}
