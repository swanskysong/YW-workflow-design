package org.yesworkflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that defines the keywords recognized by YW and maps them to the 
 * {@link org.yesworkflow.YWKeywords.Tag YWKeywords.Tag} enumeration.
 * <p>Defines standard keywords for each tag. Supports lookup of of a tag from 
 * a keyword, and reverse lookup of the (standard) keyword for a tag.</p>
 * 
 * <p> Non-default constructors allow multiple, alternative keywords to be 
 * associated with each tag, and disabling of standard keywords.</p>
 */
public class YWKeywords {

    /** Enumeration of unique YW tags, each corresponding to one or more 
     * recognized YW keywords. 
     */
    public enum Tag {
        
        AS,
        BEGIN,
        CALL,
        CREATE,
        END,
        FILE,
        IN,
        OUT,
        PARAM,
        RETURN,
        URI;
    }
    
    /** Definition of the standard keyword for the AS tag. */
    public static final String STANDARD_AS_KEYWORD              = "@as";

    /** Definition of the standard keyword for the BEGIN tag. */
    public static final String STANDARD_BEGIN_KEYWORD           = "@begin";
    
    /** Definition of the standard keyword for the CALL tag. */
    public static final String STANDARD_CALL_KEYWORD            = "@call";

    /** Definition of the standard keyword for the CREATE tag. */
    public static final String STANDARD_CREATE_KEYWORD            = "@create";

    /** Definition of the standard keyword for the END tag. */
    public static final String STANDARD_END_KEYWORD             = "@end";
    
    /** Definition of the standard keyword for the FILE tag. */
    public static final String STANDARD_FILE_KEYWORD            = "@file";

    /** Definition of the standard keyword for the IN tag. */
    public static final String STANDARD_IN_KEYWORD              = "@in";
    
    /** Definition of the standard keyword for the OUT tag. */
    public static final String STANDARD_OUT_KEYWORD             = "@out";

    /** Definition of the standard keyword for the PARAM tag. */
    public static final String STANDARD_PARAM_KEYWORD           = "@param";

    /** Definition of the standard keyword for the RETURN tag. */
    public static final String STANDARD_RETURN_KEYWORD          = "@return";

    /** Definition of the standard keyword for the URI tag. */
    public static final String STANDARD_URI_KEYWORD             = "@uri";
    
    /** Backing for one-to-one mapping from tag to standard keyword */
    private Map<Tag,String> standardkeywordForTag = new HashMap<Tag,String>();

    /** Backing for many-to-one mapping from keyword to tag */
    private Map<String,Tag> tagForKeyword = new HashMap<String,Tag>();
    
    /** Default constructor that assigns the standard keyword to each tag. */
    public YWKeywords() {
        setStandardKeyword(    STANDARD_AS_KEYWORD,     Tag.AS      );
        setStandardKeyword(    STANDARD_BEGIN_KEYWORD,  Tag.BEGIN   );
        setStandardKeyword(    STANDARD_CALL_KEYWORD,   Tag.CALL    );
        setStandardKeyword(    STANDARD_CREATE_KEYWORD, Tag.CREATE  );
        setStandardKeyword(    STANDARD_END_KEYWORD,    Tag.END     );
        setStandardKeyword(    STANDARD_FILE_KEYWORD,   Tag.FILE    );
        setStandardKeyword(    STANDARD_IN_KEYWORD,     Tag.IN      );
        setStandardKeyword(    STANDARD_OUT_KEYWORD,    Tag.OUT     );
        setStandardKeyword(    STANDARD_PARAM_KEYWORD,  Tag.PARAM   );
        setStandardKeyword(    STANDARD_RETURN_KEYWORD, Tag.RETURN  );
        setStandardKeyword(    STANDARD_URI_KEYWORD,    Tag.URI     );
    }

    /** Constructor that assigns alternative keywords to one more tags in addition
     *  to the standard keywords.
     *  @param alternateKeywords A mapping of alternate keywords to tags.
     */
    public YWKeywords(Map<String,Tag> alternateKeywords) {
        this();
        for (Map.Entry<String,Tag> e : alternateKeywords.entrySet()) {
            tagForKeyword.put(e.getKey().toLowerCase(), e.getValue());
        }
    }

    /** Constructor that assigns alternative keywords to one more tags, and 
     *  additionally allows one or more of the standard keywords to be disabled.
     *  @param alternateKeywords A mapping of alternate keywords to tags.
     *  @param disabledKeywords  A list of keywords to disable.
     */
    public YWKeywords(Map<String,Tag> alternateKeywords, List<String> disabledKeywords) {
        this(alternateKeywords);
        for (String keyword : disabledKeywords) {
            tagForKeyword.remove(keyword.toLowerCase());
        }
    }
    
    /** Looks up the standard keyword for a tag.
     *  @param t  The tag of the keyword to look up.
     *  @return The standard keyword for tag t.
     */
    public String getStandardKeyword(Tag t) {
        return standardkeywordForTag.get(t);
    }
    
    /** Looks up the tag to which the given keyword is assigned.
     *  @param k The key word look up.
     *  @return The tag corresponding to k, or null if k is not a keyword.
     */
    public Tag getTag(String k) {
        return tagForKeyword.get(k.toLowerCase());
    }
        
    /** Determines if the given string has been defined as a YW keyword.
     *  @param k  The potential keyword.
     *  @return true if k is a keyword.
     */
    public boolean isKeyword(String k) {
        return tagForKeyword.keySet().contains(k.toLowerCase());
    }
    
    /** Assigns the standard keyword for the given tag. */
    private void setStandardKeyword(String keyword, Tag tag) {
        String lowercaseKeyword = keyword.toLowerCase();
        standardkeywordForTag.put(tag, lowercaseKeyword);
        tagForKeyword.put(lowercaseKeyword, tag);
    }
    
    /** Returns the keywords defined for this configuration of YW */
    public Set<String> getKeywords() {
        return tagForKeyword.keySet();
    }
}
