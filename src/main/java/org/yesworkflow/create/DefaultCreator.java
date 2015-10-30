package org.yesworkflow.create;

import java.io.*;
import java.util.*;
import java.util.List;

import org.jooq.Record;
import org.jooq.Result;
import org.yesworkflow.Language;
import org.yesworkflow.LanguageModel;
import org.yesworkflow.YWKeywords;
import org.yesworkflow.annotations.*;
import org.yesworkflow.db.Column;
import org.yesworkflow.db.Table;
import org.yesworkflow.db.YesWorkflowDB;
import org.yesworkflow.query.QueryEngine;

import static org.yesworkflow.db.Column.*;

public class DefaultCreator implements Creator {

    static private Language DEFAULT_LANGUAGE = Language.GENERIC;
    static private QueryEngine DEFAULT_QUERY_ENGINE = QueryEngine.SWIPL;

    private YesWorkflowDB ywdb;
    private YesWorkflowDB pdb;
    private LanguageModel globalLanguageModel = null;
    private Language lastLanguage = null;
    private QueryEngine queryEngine = DEFAULT_QUERY_ENGINE;
    private List<String> sourcePaths;
    private List<Annotation> allAnnotations;
    private List<Annotation> primaryAnnotations;
    private String commentListingPath;
    private String factsFile = null;
    private String skeletonFile = null;
    private String skeleton = null;
    private String extractFacts = null;
    private PrintStream stdoutStream = null;
    private PrintStream stderrStream = null;

    public DefaultCreator() throws Exception {
        this(YesWorkflowDB.getGlobalInstance(), System.out, System.err);
    }

    public DefaultCreator(YesWorkflowDB ywdb) throws Exception {
        this(ywdb, System.out, System.err);
    }

    public DefaultCreator(YesWorkflowDB ywdb, PrintStream stdoutStream, PrintStream stderrStream) {
        try {
            this.pdb = YesWorkflowDB.getPersistentInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.ywdb = ywdb;
        this.stdoutStream = stdoutStream;
        this.stderrStream = stderrStream;
    }

    @Override
    public DefaultCreator configure(Map<String,Object> config) throws Exception {
        if (config != null) {
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                configure(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DefaultCreator configure(String key, Object value) throws Exception {
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
    public DefaultCreator create() throws Exception {
        fillInCodeBlock();
        return this;
    }

    @SuppressWarnings({ "unchecked" })
    private void fillInCodeBlock() {
        Result<Record> create_rows = ywdb.jooq().select(BEGIN_LINE, END_LINE, NAME)
                .from(Table.CODE_BLOCK)
                .fetch();

        ArrayList<String> notMatchedBlocks = new ArrayList<>();
        String sourceFile = sourcePaths.get(0);
        String createdFile = sourceFile.replace(".py", "_created.py");
        BufferedReader br = null;
        BufferedWriter bw = null;
        int lineCount = 1;
        try {
            br = new BufferedReader(new FileReader(sourceFile));
            bw = new BufferedWriter(new FileWriter(createdFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // for each block to be created
        for (Record create_row : create_rows) {
            String name = ywdb.getStringValue(create_row, NAME);
            Long create_begin_line = ywdb.getLongValue(create_row, BEGIN_LINE);
            Long create_end_line = ywdb.getLongValue(create_row, END_LINE);

            // first, get the signature of the to be created code block
            Result<Record> annoataion_rows = ywdb.jooq().select(INPUT_OR_OUTPUT, VARIABLE)
                    .from(Table.SIGNATURE)
                    .where(Column.SIGNATURE.IN_CODE_BLOCK.equal(name))
                    .fetch();

            ArrayList<String> inputList = new ArrayList<>();
            ArrayList<String> outputList = new ArrayList<>();
            for (Record annoataion_row : annoataion_rows) {
                String tag = ywdb.getStringValue(annoataion_row, INPUT_OR_OUTPUT);
                String value = ywdb.getStringValue(annoataion_row, VARIABLE);
                if(tag.equals(YWKeywords.Tag.IN.toString())) inputList.add(value);
                else if(tag.equals(YWKeywords.Tag.OUT.toString())) outputList.add(value);
            }

            // second, match with existing blocks, the where clause match the name
            if(name.equals("standardize")) name = "standardize_with_mask";
            HashMap<String, String> aliasToVariable = new HashMap<>();
            Result<Record> signatureList = pdb.jooq().select(Column.CODE_BLOCK.BEGIN_LINE, Column.CODE_BLOCK.END_LINE,
                    Column.SIGNATURE.INPUT_OR_OUTPUT, Column.SIGNATURE.ALIAS, Column.SIGNATURE.VARIABLE)
                    .from(Table.CODE_BLOCK)
                    .join(Table.SIGNATURE)
                    .on(Column.CODE_BLOCK.NAME.equal(Column.SIGNATURE.IN_CODE_BLOCK))
                    .where(Column.CODE_BLOCK.NAME.equal(name))
                    .fetch();

            boolean matched = true;
            if(signatureList.isEmpty()) matched = false;
            else {
                // if the code block name is matched then check signature match
                for (Record signature_line : signatureList) {
                    String inputOrOutput = pdb.getStringValue(signature_line, Column.SIGNATURE.INPUT_OR_OUTPUT);
                    String alias = pdb.getStringValue(signature_line, Column.SIGNATURE.ALIAS);
                    String variable = pdb.getStringValue(signature_line, Column.SIGNATURE.VARIABLE);

                    aliasToVariable.put(alias, variable);
                    if (inputOrOutput.equals(YWKeywords.Tag.IN.toString())) {
                        if (!inputList.contains(alias)) {
                            matched = false;
                            break;
                        } else inputList.remove(alias);
                    } else if (inputOrOutput.equals(YWKeywords.Tag.OUT.toString())) {
                        if (!outputList.contains(alias)) {
                            matched = false;
                            break;
                        } else outputList.remove(alias);
                    }
                }
                // if any input and output is left, i.e., not in the match block, fail
                if (!(inputList.isEmpty() && outputList.isEmpty())) matched = false;
            }

            // third, if matched, fetch the actual code and fill in, otherwise, exit
            if(!matched){
                notMatchedBlocks.add(name);
            }else{
                Result<Record> code_rows = pdb.jooq().select(LINE_TEXT)
                        .from(Table.CODE_SNIPPET)
                        .where(Column.CODE_SNIPPET.NAME.equal(name))
                        .orderBy(ORIGINAL_LINE)
                        .fetch();

                StringBuilder code_snippet = new StringBuilder();
                for(Record code_row : code_rows){
                    String line_text = pdb.getStringValue(code_row, LINE_TEXT);
                    code_snippet.append(line_text);
                    code_snippet.append(EOL);
                }

                try{
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (lineCount == create_begin_line) {
                            bw.write(code_snippet.toString());
                            lineCount++;    // make sure line number is consistent with br.readline()
                            int create_block_length = (int) (create_end_line - create_begin_line);
                            while (create_block_length > 0) {
                                br.readLine();
                                lineCount++;
                                create_block_length--;
                            }
                            break;
                        }
                        else {
                            bw.write(line + "\n");
                            lineCount++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if(!notMatchedBlocks.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String name : notMatchedBlocks) sb.append(name + ",");
            System.out.println("Code block: " + sb.toString() + " cannot be created");
        }

        // create file if there is at least one @CREATE block
        if(create_rows.size() != 0) {
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    bw.write(line + "\n");
                }
                if (br != null) br.close();
                if (bw != null) bw.close();
            } catch (IOException e) {}

            File newFile = new File(createdFile);
            System.out.println("File created as " + createdFile);
        }
    }
}
