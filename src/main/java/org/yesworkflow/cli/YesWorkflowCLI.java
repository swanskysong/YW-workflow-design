package org.yesworkflow.cli;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import org.yesworkflow.annotations.Annotation;
import org.yesworkflow.config.YWConfiguration;
import org.yesworkflow.create.Creator;
import org.yesworkflow.create.DefaultCreator;
import org.yesworkflow.db.YesWorkflowDB;
import org.yesworkflow.exceptions.YWMarkupException;
import org.yesworkflow.exceptions.YWToolUsageException;
import org.yesworkflow.extract.DefaultExtractor;
import org.yesworkflow.extract.Extractor;
import org.yesworkflow.graph.DotGrapher;
import org.yesworkflow.graph.Grapher;
import org.yesworkflow.model.DefaultModeler;
import org.yesworkflow.model.Model;
import org.yesworkflow.model.Modeler;
import org.yesworkflow.recon.DefaultReconstructor;
import org.yesworkflow.recon.Reconstructor;
import org.yesworkflow.recon.Run;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/** 
 * Class that provides the default command-line interface (CLI) for YesWorkflow.
 * The CLI takes one argument (or option) representing the operation to 
 * be carried out (currently <i>extract</i>, <i>model</i>, or <i>graph</i>),
 * along with additional options that specify desired outputs and formats.  
 * Each operation implies and automatically runs the operations that logically 
 * precede it, i.e. the <i>graph</i> command implies the <i>extract</i> and 
 * <i>model</i> operations.</p>
 *
 * <p>The static {@link #main(String[]) main()} method instantiates this class
 * and passes the command line arguments it receives from the OS to 
 * the {@link #runForArgs(String[]) runForArgs()} method.
 * It then uses the {@link ExitCode} returned by 
 * {@link #runForArgs(String[]) runForArgs()} as the process exit code. 
 * 
 * <p>The CLI can be invoked programmatically by instantiating this class and
 * calling {@link #runForArgs(String[]) runForArgs()}. This function takes an argument 
 * of array of String representing command line arguments and options.
 * The {@link org.yesworkflow.extract.Extractor Extractor}, 
 * {@link org.yesworkflow.model.Modeler Modeler}, 
 * and {@link org.yesworkflow.graph.Grapher Grapher} used by the instance may be injected 
 * using the {@link #extractor(Extractor) extractor()}, {@link #modeler(Modeler) modeler()},
 * and {@link #grapher(Grapher) grapher()} methods before calling 
 * {@link #runForArgs(String[]) runForArgs()}.  A 
 * {@link #YesWorkflowCLI(PrintStream, PrintStream) non-default constructor} allows 
 * the output streams used by YesWorkflow to be assigned.</p>
 */
public class YesWorkflowCLI {

    public static final String EOL = System.getProperty("line.separator");
    private static final String PROPERTY_FILE_NAME = "yw.properties";
    private static final String YAML_FILE_NAME = "yw.yaml";
    
    private final YesWorkflowDB ywdb;
    private final PrintStream errStream;
    private final PrintStream outStream;    
    private OptionSet options = null;
    private Extractor extractor = null;
    private Creator creator = null;
    private Modeler modeler = null;
    private Grapher grapher = null;
    private List<Annotation> annotations;
    private Model model = null;
    private YWConfiguration config = null;
    private Reconstructor reconstructor;
    
    /** Method invoked first when the YesWorkflow CLI is run from the 
     * command line. Creates an instance of {@link YesWorkflowCLI},
     * passes the command line arguments to {@link #runForArgs(String[]) runForArgs()}, 
     * and uses the integer value associated with the {@link ExitCode} 
     * returned by {@link #runForArgs(String[]) runForArgs()} as the process
     * exit code.
     * 
     * @param args Arguments provided to the CLI on the command line.
     */
    public static void main(String[] args) {

        ExitCode exitCode;

        try {
            exitCode = new YesWorkflowCLI().runForArgs(args);
        } catch (Exception e) {
            e.printStackTrace();
            exitCode = ExitCode.UNCAUGHT_ERROR;
        }

        System.exit(exitCode.value());
    }

    /** 
     * Default constructor.  Used when YesWorkflow should use the
     * system-provided System.out and System.err streams.
     * @throws Exception 
     */
    public YesWorkflowCLI() throws Exception {
        this(System.out, System.err);
    }

    /** 
     * Constructor that injects custom output streams. Used when 
     * YesWorkflow should use the streams provided as parameters instead 
     * of System.out and System.err.
     * @param outStream The PrintStream to use instead of System.out.
     * @param errStream The PrintStream to use instead of System.err.
     * @throws Exception 
     */
    public YesWorkflowCLI(PrintStream outStream, PrintStream errStream) throws Exception {
        this(YesWorkflowDB.getGlobalInstance(), outStream, errStream);
    }

    public YesWorkflowCLI(YesWorkflowDB ywdb, PrintStream outStream, PrintStream errStream) throws Exception {
        this.ywdb = ywdb;
        this.outStream = outStream;
        this.errStream = errStream;
    }

    public YesWorkflowCLI config(YWConfiguration config) {
        this.config = config;
        return this;
    }
    
    /** Method used to inject the 
     * {@link org.yesworkflow.extract.Extractor Extractor} to be used.
     * @param extractor A configured {@link org.yesworkflow.extract.Extractor Extractor} to use.
     * @return This instance.
     */
    public YesWorkflowCLI extractor(Extractor extractor) {
        this.extractor = extractor;
        return this;
    }

    /** Method used to inject the 
     * {@link org.yesworkflow.model.Modeler Modeler} to be used.
     * @param modeler A configured {@link org.yesworkflow.model.Modeler Modeler} to use.
     * @return This instance.
     */
    public YesWorkflowCLI modeler(Modeler modeler) {
        this.modeler = modeler;
        return this;
    }

    /** Method used to inject the 
     * {@link org.yesworkflow.graph.Grapher Grapher} to be used.
     * @param grapher A configured {@link org.yesworkflow.graph.Grapher Grapher} to use.
     * @return This instance.
     */
    public YesWorkflowCLI grapher(Grapher grapher) {
        this.grapher = grapher;
        return this;
    }

    /** 
     * Method that parses the provided command line arguments and executes the 
     * sequence of YesWorkflow operations requested by them.
     * @param args The command line arguments to parse.
     * @return An {@link ExitCode} indicating either that YesWorkflow 
     * ran successfully, or that an error of the indicated type occurred.
     * @throws Exception if an exception other than 
     * {@link org.yesworkflow.exceptions.YWToolUsageException YWToolUsageException}
     * or {@link org.yesworkflow.exceptions.YWMarkupException YWMarkupException}
     * is thrown while parsing the command line options or executing the YesWorkflow
     * operations. 
     */
    public ExitCode runForArgs(String[] args) throws Exception {

        OptionParser parser = createOptionsParser();

        try {

            // parse the command line arguments and options
            try {
                options = parser.parse(args);
            } catch (OptionException exception) {
                throw new YWToolUsageException("ERROR: " + exception.getMessage());
            }

            // print help and exit if requested
            if (options.has("h")) {
                printCLIHelp(parser);
                return ExitCode.SUCCESS;
            }
            
            // load the configuration files
            if (config == null) {
                config = YWConfiguration.fromYamlFile(YAML_FILE_NAME);
                config.applyPropertyFile(PROPERTY_FILE_NAME);
            }
            
            // apply command-line overrides of config file
            config.applyConfigOptions(options.valuesOf("c"));

            // make sure at least one non-option argument was given
            List<?> nonOptionArguments = options.nonOptionArguments();            
            if (options.nonOptionArguments().size() == 0) {
                throw new YWToolUsageException("ERROR: Command must be first non-option argument to YesWorkflow");
            }            

            // extract YesWorkflow command from first non-option argument
            YWCommand command = null;
            try {
                command = YWCommand.toYWCommand((String) nonOptionArguments.get(0));
            } catch(Exception e) {
                throw new YWToolUsageException("ERROR: Unrecognized YW command: " + nonOptionArguments.get(0));
            }

            // extract source file paths from remaining non-option arguments
            if (nonOptionArguments.size() > 1) {
                List<String> sourceFiles = new LinkedList<String>();
                for (int i = 1; i < nonOptionArguments.size(); ++i) {
                    sourceFiles.add((String) nonOptionArguments.get(i));
                }
                config.applyConfigOption("extract.sources", sourceFiles);
                config.applyConfigOption("create.sources", sourceFiles);
            }
            
            String queryEngine = (String) config.getConfigOptionValue("query.engine");
            if (queryEngine != null) {
                if (config.getConfigOptionValue("extract.queryengine") == null) config.applyConfigOption("extract.queryengine", queryEngine);
                if (config.getConfigOptionValue("model.queryengine") == null) config.applyConfigOption("model.queryengine", queryEngine);
                if (config.getConfigOptionValue("recon.queryengine") == null) config.applyConfigOption("recon.queryengine", queryEngine);
            }
            
            // execute sequence of commands through the requested one
            switch(command) {

                case NOOP:
                    return ExitCode.SUCCESS;
            
                case EXTRACT:
                    extract();
                    return ExitCode.SUCCESS;

                case CREATE:
                    extract();
                    create();
                    return ExitCode.SUCCESS;
    
                case MODEL:
                    extract();
                    model();
                    return ExitCode.SUCCESS;
                    
                case GRAPH:
                    extract();
                    model();
                    graph();
                    return ExitCode.SUCCESS;

                case RECON:
                    extract();
                    model();
                    recon();
                    return ExitCode.SUCCESS;
            }
            
        } catch (YWToolUsageException e) {
            printToolUsageErrors(e.getMessage());
            return ExitCode.CLI_USAGE_ERROR;
        } catch (YWMarkupException e) {
            printMarkupErrors(e.getMessage());
            return ExitCode.MARKUP_ERROR;
        } 

        return ExitCode.SUCCESS;
    }
    
    private void printMarkupErrors(String message) {
        errStream.println("******************* YESWORKFLOW MARKUP ERRORS **************************");
        errStream.print(message);
        errStream.println();
        errStream.println("------------------------------------------------------------------------");
    }

    private void printToolUsageErrors(String message) {
        errStream.println();
        errStream.println(message);
        errStream.println();
        errStream.println("Use the -h option to display help for the YW command-line interface.");
    }
    
    public static final String YW_CLI_USAGE_HELP = 
            "usage: yw <command> [source file(s)] [-c <name=value>]..."                     + EOL;
    
    public static final String YW_CLI_COMMAND_HELP = 
        "Command                    Function"                                               + EOL +
        "-------                    --------"                                               + EOL +
        "extract                    Identify YW comments in script source file(s)"          + EOL +
        "model                      Build workflow model from identified YW comments"       + EOL +
        "recon                      Reconstruct a run from its persisted data products"     + EOL +
        "graph                      Graphically render workflow model of script"            + EOL;

    public static final String YW_CLI_CONFIG_HELP = 
        "Configuration Name         Value"                                                  + EOL +
        "------------------         -----"                                                  + EOL +
        "extract.comment            Single-line comment delimiter in source files"          + EOL +
        "extract.factsfile          File for storing prolog facts about scripts"            + EOL +
        "extract.language           Language used in source files"                          + EOL +
        "extract.listfile           File for storing flat list of extracted YW markup"      + EOL +
        "extract.skeletonfile       File for storing YW-markup skeleton of source files"    + EOL +  
        "extract.sources            List of source files to analyze"                        + EOL +        
        ""                                                                                  + EOL +
        "model.factsfile            File for storing prolog facts describing model"         + EOL +
        "model.workflow             Name of top-level workflow in model"                    + EOL +
        ""                                                                                  + EOL +
        "recon.factsfile            File for storing reconstructed facts about a run"       + EOL +
        ""                                                                                  + EOL +
        "graph.datalabel            Info to display in data nodes: NAME, URI, or BOTH"      + EOL +
        "graph.dotcomments          Include comments in dot file (ON or OFF)"               + EOL +
        "graph.dotfile              Name of GraphViz DOT file to write graph to"            + EOL +
        "graph.edgelabels           SHOW or HIDE labels on edges in process and data views" + EOL +
        "graph.layout               Direction of graph layout: TB, LR, RL, or BT"           + EOL +
        "graph.params               SHOW, HIDE, or REDUCE visibility of parameters"         + EOL +
        "graph.portlayout           Layout mode for workflow ports: HIDE, RELAX or GROUP"   + EOL +
        "graph.subworkflow          Qualified name of (sub)workflow to render"              + EOL +
        "graph.title                Graph title (defaults to workflow name)"                + EOL +
        "graph.titleposition        Where to place graph title: TOP, BOTTOM, or HIDE"       + EOL +
        "graph.view                 Workflow view to render: PROCESS, DATA or COMBINED"     + EOL +
        "graph.workflowbox          SHOW or HIDE box around nodes internal to workflow"     + EOL;
    
    public static final String YW_CLI_EXAMPLES_HELP = 
        "Examples"                                                                          + EOL +
        "--------"                                                                          + EOL +
        "$ yw extract myscript -c extract.comment='#' -c extract.listing=comments.txt"      + EOL +
        "$ yw graph myscript.py -config graph.view=combined -config graph.datalabel=uri"    + EOL +
        "$ yw graph scriptA.py scriptB.py > wf.gv; dot -Tpdf wf.gv -o wf.pdf; open wf.pdf"  + EOL;
    
    private void printCLIHelp(OptionParser parser) throws IOException {
        errStream.println();
        errStream.println(YW_CLI_USAGE_HELP);
        errStream.println(YW_CLI_COMMAND_HELP);
        parser.printHelpOn(errStream);
        errStream.println();
        errStream.println(YW_CLI_CONFIG_HELP);
        errStream.println(YW_CLI_EXAMPLES_HELP);
    }

    private OptionParser createOptionsParser() throws Exception {
        
        OptionParser parser = null;

        parser = new OptionParser() {{
            acceptsAll(asList("c", "config"), "Assign value to configuration option")
                .withRequiredArg()
                .ofType(String.class)
                .describedAs("configuration")
                .describedAs("name=value");

            acceptsAll(asList("h", "help"), "Display this help");
        }};

        return parser;
    }

    private void extract() throws Exception {
    	
        if (extractor == null) {
            extractor =  new DefaultExtractor(this.ywdb, this.outStream, this.errStream);
        }

        annotations = extractor.configure(config.getSection("extract"))
	                           .extract()
	                           .getAnnotations();
    }

    private void create() throws Exception {

        if (creator == null) {
            creator =  new DefaultCreator(this.ywdb, this.outStream, this.errStream);
        }

        creator.configure(config.getSection("create")).create();
    }

    private void model() throws Exception {
        
        if (annotations.size() == 0) {
            throw new YWMarkupException("Cannot create workflow model from source with no YW comments.");
        }
        
        if (modeler == null) {
            modeler = new DefaultModeler(this.ywdb, this.outStream, this.errStream);
         }

        model =  modeler.configure(config.getSection("model"))
                           .annotations(annotations)
                           .model()
                           .getModel();
    }

    private void graph() throws Exception {

        if (grapher == null) {
            grapher = new DotGrapher(this.outStream, this.errStream);
         }
        
        grapher.configure(config.getSection("graph"))
               .model(model)
               .graph();
    }

    private void recon() throws Exception {

        if (reconstructor == null) {
            reconstructor = new DefaultReconstructor(this.outStream, this.errStream);
        }

        String runDirectory = config.getConfigOptionValue("recon.rundir");
        Run run = (runDirectory == null) ? new Run(model) : new Run(model, runDirectory);
        
        reconstructor.configure(config.getSection("recon"))
                     .run(run)
                     .recon();
    }

}
