package org.yesworkflow.extract;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.sql.SQLException;
import java.util.*;

import org.jooq.Record;
import org.jooq.Result;
import org.yesworkflow.Language;
import org.yesworkflow.LanguageModel;
import org.yesworkflow.YWKeywords;
import org.yesworkflow.YWKeywords.Tag;
import org.yesworkflow.annotations.*;
import org.yesworkflow.config.YWConfiguration;
import org.yesworkflow.db.Column;
import org.yesworkflow.db.Table;
import org.yesworkflow.db.Signature;
import org.yesworkflow.db.YesWorkflowDB;
import org.yesworkflow.exceptions.YWToolUsageException;
import org.yesworkflow.query.QueryEngine;
import org.yesworkflow.query.QueryEngineModel;

import static org.yesworkflow.db.Table.*;
import static org.yesworkflow.db.Column.*;

public class DefaultExtractor implements Extractor {

    static private Language DEFAULT_LANGUAGE = Language.GENERIC;
    static private QueryEngine DEFAULT_QUERY_ENGINE = QueryEngine.SWIPL;

    private YesWorkflowDB ywdb;
    private LanguageModel globalLanguageModel = null;
    private Language lastLanguage = null;
    private QueryEngine queryEngine = DEFAULT_QUERY_ENGINE;
    private BufferedReader sourceReader = null;
    private List<String> sourcePaths;
    private List<Annotation> allAnnotations;
    private List<Annotation> primaryAnnotations;
    private YWKeywords keywordMapping;
    private KeywordMatcher keywordMatcher;
    private String commentListingPath;
    private String factsFile = null;
    private String skeletonFile = null;
    private String skeleton = null;
    private String extractFacts = null;
    private PrintStream stdoutStream = null;
    private PrintStream stderrStream = null;

    private YesWorkflowDB pdb;

    private Long nextAnnotationId = 1L;

    public DefaultExtractor() throws Exception {
        this(YesWorkflowDB.getGlobalInstance(), System.out, System.err);
    }

    public DefaultExtractor(YesWorkflowDB ywdb) throws Exception {
        this(ywdb, System.out, System.err);
    }

    public DefaultExtractor(YesWorkflowDB ywdb,  PrintStream stdoutStream, PrintStream stderrStream) {
        this.ywdb = ywdb;
        try {
            this.pdb = YesWorkflowDB.getPersistentInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.stdoutStream = stdoutStream;
        this.stderrStream = stderrStream;
        this.keywordMapping = new YWKeywords();
        this.keywordMatcher = new KeywordMatcher(keywordMapping.getKeywords());
    }

    @Override
    public DefaultExtractor configure(Map<String,Object> config) throws Exception {
        if (config != null) {
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                configure(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DefaultExtractor configure(String key, Object value) throws Exception {
        if (key.equalsIgnoreCase("sources")) {
            sourcePaths = new LinkedList<String>();
            if (value instanceof String) {
                for (String token : ((String) value).split("\\s")) {
                    if (!token.trim().isEmpty()) {
                        sourcePaths.add(token);
                    }
                }
            } else if (value instanceof List) {
                sourcePaths.addAll((List<? extends String>) value);
            } else {
                throw new Exception("Value of sources property must be one or more strings");
            }
        } else if (key.equalsIgnoreCase("language")) {
            Language language = Language.toLanguage(value);
            globalLanguageModel = new LanguageModel(language);
        } else if (key.equalsIgnoreCase("languageModel")) {
            globalLanguageModel = (LanguageModel)value;
        } else if (key.equalsIgnoreCase("comment")) {
            globalLanguageModel = new LanguageModel();
            globalLanguageModel.singleDelimiter((String)value);
        } else if (key.equalsIgnoreCase("listfile")) {
            commentListingPath = (String)value;
        } else if (key.equalsIgnoreCase("factsfile")) {
            factsFile = (String)value;
        } else if (key.equalsIgnoreCase("skeletonfile")) {
            skeletonFile = (String)value;
        } else if (key.equalsIgnoreCase("queryengine")) {
            queryEngine = QueryEngine.toQueryEngine((String)value);
        }

        return this;
    }

    @Override
    public Language getLanguage() {
        return lastLanguage;
    }

	@Override
	public List<Annotation> getAnnotations() {
		return primaryAnnotations;
	}

	@Override
    public String getSkeleton() {

	    if (skeleton == null) {
            SkeletonBuilder sb = new SkeletonBuilder( getSkeletonCommentDelimiter() + " ");
            for (Annotation annotation : allAnnotations) {
                sb.add(annotation);
            }
            sb.end();
            skeleton = sb.toString();
 	    }

        return skeleton;
    }

	@Override
    public String getFacts(QueryEngineModel queryEngineModel) {
        if (extractFacts == null) {
            extractFacts = new ExtractFacts(ywdb, queryEngineModel, allAnnotations).build().toString();
        }
        return extractFacts;
    }

    @Override
    public DefaultExtractor extract() throws Exception {

        extractCommentsFromSources();
        writeCommentListing();
        extractAnnotations();
        writeSkeletonFile();
        extractCodeBlock();

        System.out.println("extracting");

        System.out.println("COMMENT");
        System.out.println(ywdb.jooq().select()
                .from(Table.COMMENT)
                .fetch());

        System.out.println("ANNOTATION");
        System.out.println(ywdb.jooq().select()
                .from(Table.ANNOTATION)
                .fetch());

        System.out.println("CODE_BLOCK");
        System.out.println(ywdb.jooq().select()
                .from(Table.CODE_BLOCK)
                .fetch());

        System.out.println("SIGNATURE");
        System.out.println(ywdb.jooq().select()
                .from(Table.SIGNATURE)
                .fetch());

        System.out.println("P_CODE_BLOCK");
        System.out.println(pdb.jooq().select()
                .from(Table.CODE_BLOCK)
                .fetch());

        System.out.println("P_SIGNATURE");
        System.out.println(pdb.jooq().select()
                .from(Table.SIGNATURE)
                .fetch());

        System.out.println("P_CODE_SNIPPET");
        System.out.println(pdb.jooq().select()
                .from(Table.CODE_SNIPPET)
                .fetch());

        if (ywdb.getRowCount(ANNOTATION) == 0) {
            stderrStream.println("WARNING: No YW comments found in source code.");
        }

        if (factsFile != null) {
            QueryEngineModel queryEngineModel = new QueryEngineModel(queryEngine);
            writeTextToFileOrStdout(factsFile, getFacts(queryEngineModel));
        }

        return this;
    }

    @SuppressWarnings({ "unchecked" })
    private void extractCodeBlock() throws SQLException {
        Result<Record> begin_rows = ywdb.jooq().select(TAG, VALUE)
                .from(Table.ANNOTATION)
                .orderBy(COMMENT_ID)
                .fetch();

        // the order of @begin, @in, @as must be maintained
        String block_name = null;
        String block_tag = null;
        Signature sig = null;
        for (Record comment : begin_rows) {
            String tag = ywdb.getStringValue(comment, TAG);
            String value = ywdb.getStringValue(comment, VALUE);

            if (tag.equals(Tag.BEGIN.toString()) || tag.equals(Tag.CREATE.toString())) {
                sig = insertSignature(sig, block_tag);
                block_name = value;
                block_tag = tag;

                // get the @begin and @end line numbers
                Result<Record> beginAndEndLineNumber = ywdb.jooq().select(Column.COMMENT.LINE_NUMBER, Column.ANNOTATION.TAG)
                        .from(Table.COMMENT)
                        .join(Table.ANNOTATION)
                        .on(Column.COMMENT.ID.equal(Column.ANNOTATION.COMMENT_ID))
                        .where(Column.ANNOTATION.VALUE.equal(block_name))
                        .orderBy(COMMENT_ID)
                        .fetch();

                Long begin_line = null;
                Long end_line = null;
                for (Record line : beginAndEndLineNumber) {
                    String line_tag = ywdb.getStringValue(line, Column.ANNOTATION.TAG);
                    Long line_number = ywdb.getLongValue(line, Column.COMMENT.LINE_NUMBER);

                    if (line_tag.equals(tag)) begin_line = line_number;
                    else if (line_tag.equals(YWKeywords.Tag.END.toString())) end_line = line_number;
                    else System.out.println("tag: " + line_tag + " is neither BEGIN nor END in extractCodeBlock");
                }

                if(tag.equals(Tag.BEGIN.toString())){
                    pdb.insertCodeBlock(begin_line, end_line, block_name, null);
                    Result<Record> code_rows = ywdb.jooq().select(LINE_NUMBER, LINE_TEXT)
                            .from(Table.SOURCE_LINE)
                            .where(Column.SOURCE_LINE.LINE_NUMBER.between(begin_line, end_line))
                            .orderBy(LINE_NUMBER)
                            .fetch();

                    for(Record code_row : code_rows) {
                        pdb.insertCodeSnippet(pdb.getLongValue(code_row, LINE_NUMBER), pdb.getStringValue(code_row, LINE_TEXT), block_name);
                    }
                }
                if(tag.equals(Tag.CREATE.toString())) ywdb.insertCodeBlock(begin_line, end_line, block_name, null);

            } else if (tag.equals(Tag.IN.toString())){
                sig = insertSignature(sig, block_tag);
                sig = new Signature(block_name).setInputOrOutput(tag.toString()).setVariable(value);
            } else if (tag.equals(Tag.OUT.toString())){
                sig = insertSignature(sig, block_tag);
                sig = new Signature(block_name).setInputOrOutput(tag.toString()).setVariable(value);
            } else if (tag.equals(Tag.AS.toString())){
                sig.setAlias(value);
            } else if (tag.equals(Tag.URI.toString())){
                sig.setURI(value);
            } else if (tag.equals(Tag.END.toString())){
                sig = insertSignature(sig, block_tag);
            }
        }
    }

    private Signature insertSignature(Signature sig, String block_tag) throws SQLException {
        if (sig != null){
            if (block_tag.equals(Tag.BEGIN.toString())) pdb.insertSignature(sig.inputOrOutput, sig.variable, sig.alias, sig.uri, sig.inBlock);
            else if (block_tag.equals(Tag.CREATE.toString())) ywdb.insertSignature(sig.inputOrOutput, sig.variable, sig.alias, sig.uri, sig.inBlock);
            else System.out.println("tag: " + block_tag + " is neither BEGIN nor END in extractCodeBlock");
        }
        return null;
    }

    private void extractCommentsFromSources() throws IOException, YWToolUsageException, SQLException {

        // read source code from reader if provided
        if (sourceReader != null) {

            extractLinesCommentsFromReader(null, sourceReader, globalLanguageModel);

        // otherwise read source code from stdin if source path is empty or just a dash
        } else if (sourcePathsEmptyOrDash(sourcePaths)) {

            Reader reader = new InputStreamReader(System.in);
            extractLinesCommentsFromReader(null, new BufferedReader(reader), globalLanguageModel);

        // else read source code from each file in the list of source paths
        } else {

            for (String path : sourcePaths) {
                Long sourceId = ywdb.insertSource(path);
                LanguageModel languageModel = languageModelForSourceFile(path);
                extractLinesCommentsFromReader(sourceId, fileReaderForPath(path), languageModel);
            }
        }
    }

    private boolean sourcePathsEmptyOrDash(List<String> sourcePaths) {
        return sourcePaths == null ||
                sourcePaths.size() == 0 ||
                sourcePaths.size() == 1 && (sourcePaths.get(0).trim().isEmpty() ||
                                            sourcePaths.get(0).trim().equals("-"));
    }

    private LanguageModel languageModelForSourceFile(String sourcePath) {
        LanguageModel languageModel = null;
        if (globalLanguageModel != null) {
            languageModel = globalLanguageModel;
        } else {
            Language language = LanguageModel.languageForFileName(sourcePath);
            if (language != null) {
                languageModel = new LanguageModel(language);
            }
        }
        return languageModel;
    }

    private void extractLinesCommentsFromReader(Long sourceId, BufferedReader reader, LanguageModel languageModel) throws IOException, SQLException {
        if (languageModel == null)  languageModel = new LanguageModel(DEFAULT_LANGUAGE);
        lastLanguage = languageModel.getLanguage();
        CommentMatcher commentMatcher = new CommentMatcher(ywdb, languageModel);
        commentMatcher.extractComments(sourceId, reader);
    }

    private BufferedReader fileReaderForPath(String path) throws YWToolUsageException {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            throw new YWToolUsageException("ERROR: Input file not found: " + path);
        }

        return reader;
    }

    private void writeCommentListing() throws IOException {
        if (commentListingPath != null) {
            writeTextToFileOrStdout(commentListingPath, DefaultExtractor.commentsAsString(ywdb));
        }
    }

    private void writeSkeletonFile() throws IOException {
        if (skeletonFile != null) {
            writeTextToFileOrStdout(skeletonFile, this.getSkeleton());
        }
    }

    private void writeTextToFileOrStdout(String path, String text) throws IOException {
        PrintStream stream = (path.equals(YWConfiguration.EMPTY_VALUE) || path.equals("-")) ?
                             this.stdoutStream : new PrintStream(path);
        stream.print(text);
        if (stream != this.stdoutStream) {
            stream.close();
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void extractAnnotations() throws Exception {

    	allAnnotations = new LinkedList<Annotation>();
        primaryAnnotations = new LinkedList<Annotation>();
        Annotation primaryAnnotation = null;

        Result<Record> rows = ywdb.jooq().select(ID, SOURCE_ID, LINE_NUMBER, RANK_IN_LINE, COMMENT_TEXT)
                                         .from(Table.COMMENT)
                                         .orderBy(SOURCE_ID, LINE_NUMBER, RANK_IN_LINE)
                                         .fetch();

        for (Record comment : rows) {
            Long sourceId = ywdb.getLongValue(comment, SOURCE_ID);
            Long lineNumber = ywdb.getLongValue(comment, LINE_NUMBER);
            String commentText = (String)comment.getValue(COMMENT_TEXT);
        	List<String> annotationStrings = findCommentsOnLine(commentText, keywordMatcher);
        	Long rankInComment = 1L;

        	for (String annotationString: annotationStrings) {
        		Tag tag = KeywordMatcher.extractInitialKeyword(annotationString, keywordMapping);
                Annotation annotation = null;
                Long id = nextAnnotationId++;

                switch(tag) {

                    case BEGIN:     annotation = new Begin(id, sourceId, lineNumber, annotationString);
                                    break;
                    case CALL:      annotation = new Call(id, sourceId, lineNumber, annotationString);
                                    break;
                    case CREATE:    annotation = new Create(id, sourceId, lineNumber, annotationString);
                                    break;
                    case END:       annotation = new End(id, sourceId, lineNumber, annotationString);
                                    break;
                    case FILE:      annotation = new FileUri(id, sourceId, lineNumber, annotationString, primaryAnnotation);
                                    break;
                    case IN:        annotation = new In(id, sourceId, lineNumber, annotationString);
                                    break;
                    case OUT:       annotation = new Out(id, sourceId, lineNumber, annotationString);
                                    break;
                    case AS:        annotation = new As(id, sourceId, lineNumber, annotationString, primaryAnnotation);
                                    break;
                    case PARAM:     annotation = new Param(id, sourceId, lineNumber, annotationString);
                                    break;
                    case RETURN:    annotation = new Return(id, sourceId, lineNumber, annotationString);
                                    break;
                    case URI:       annotation = new UriAnnotation(id, sourceId, lineNumber, annotationString, primaryAnnotation);
                                    break;
                }

                allAnnotations.add(annotation);

                Long qualifiedAnnotationId = null;
                if (annotation instanceof Qualification) {
                    qualifiedAnnotationId = primaryAnnotation.id;
                } else {
                	primaryAnnotation = annotation;
                    primaryAnnotations.add(annotation);
                }

                ywdb.insertAnnotation(qualifiedAnnotationId, ywdb.getLongValue(comment, ID), rankInComment++,
                                      tag.toString(), annotation.keyword, annotation.name,
                                      annotation.description());

            }
        }
    }
    
    public static List<String> findCommentsOnLine(String line, KeywordMatcher keywordMatcher) {
    	
    	List<String> comments = new LinkedList<String>();
    	StringBuilder buffer = new StringBuilder();
    	StringBuilder currentComment = new StringBuilder();
    	
    	for (int i = 0; i < line.length(); ++i) {
    		
    		char c = line.charAt(i);
    		buffer.append(c);
    	
    		switch(keywordMatcher.matchesKeyword(buffer.toString())) {
    		
	    		case NO_MATCH:
	    			
	    			if (currentComment.length() > 0) currentComment.append(buffer);
	    			buffer.setLength(0);
	    			break;
	    		
	    		case FULL_MATCH:
	    			
	    			if (currentComment.length() > 0) {
	    				comments.add(currentComment.toString().trim());
	    				currentComment.setLength(0);
	    			}
    				currentComment.append(buffer);
    				buffer.setLength(0);
    				break;
	    			
	    		default:
	    			
	    			break;
    		}
    	}
    	
    	if (currentComment.length() > 0) {
    		currentComment.append(buffer);
    		comments.add(currentComment.toString().trim());
    	}
    	
    	return comments;
    }

    @Override
    public DefaultExtractor reader(Reader reader) {
        this.sourceReader = new BufferedReader(reader);
        return this;
    }
    
    private String getSkeletonCommentDelimiter() {
        
        // try to infer language from skeleton file name and return the
        // the single-line comment delimiter if successful
        if (skeletonFile != null) {
            Language language = LanguageModel.languageForFileName(skeletonFile);
            if (language != null) {
                LanguageModel languageModel = new LanguageModel(language);
                if (languageModel.getSingleCommentDelimiters().size() > 0) {
                    return languageModel.getSingleCommentDelimiters().get(0);
                }
            }
        }
        
        // otherwise if a global language was set for the Extractor or a
        // single-line comment delimiter was defined for Extrator, use it
        if (globalLanguageModel != null) {
            if (globalLanguageModel.getSingleCommentDelimiters().size() > 0) {
                return globalLanguageModel.getSingleCommentDelimiters().get(0);
            }
        }
        
        // next fallback to delimiter defined for last language used in extraction
        LanguageModel lastLanguageModel = new LanguageModel(lastLanguage);
        if (lastLanguageModel.getSingleCommentDelimiters().size() > 0) {
            return lastLanguageModel.getSingleCommentDelimiters().get(0) ;
        }
        
        // if all else fails use the default comment delimiter
        return "#";
    }

    @SuppressWarnings("unchecked")
    public static String commentsAsString(YesWorkflowDB ywdb) throws IOException {
        StringBuffer comments = new StringBuffer();
        Result<Record> rows = ywdb.jooq().select(ID, SOURCE_ID, LINE_NUMBER, RANK_IN_LINE, COMMENT_TEXT)
                                         .from(Table.COMMENT)
                                         .orderBy(ID, LINE_NUMBER, RANK_IN_LINE)
                                         .fetch();
        for (Record row : rows) {
            comments.append(row.getValue(COMMENT_TEXT));
            comments.append(CommentMatcher.EOL);
        }
        return comments.toString();
    }
}
