package org.yesworkflow.create;

import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.yesworkflow.Language;
import org.yesworkflow.YWStage;
import org.yesworkflow.annotations.Annotation;
import org.yesworkflow.query.QueryEngineModel;

public interface Creator extends YWStage {
    Creator configure(String key, Object value) throws Exception;
    Creator configure(Map<String, Object> config) throws Exception;
    Creator create() throws Exception;
}

