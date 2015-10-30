package org.yesworkflow.graph;

import java.util.Map;

import org.yesworkflow.YWStage;
import org.yesworkflow.model.Model;
import org.yesworkflow.model.Program;

public interface Grapher extends YWStage {
    Grapher model(Model model);
    Grapher workflow(Program workflow);
    DotGrapher configure(String key, Object value) throws Exception;
    DotGrapher configure(Map<String, Object> config) throws Exception;
    Grapher graph() throws Exception;
}